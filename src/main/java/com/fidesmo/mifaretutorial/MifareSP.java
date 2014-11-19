package com.fidesmo.mifaretutorial;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import spark.Request;
import spark.Response;
import spark.Route;
import org.json.simple.JSONObject;
import java.util.HashMap;

import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class MifareSP {

    // Session state is kept in this Map
    private static HashMap<String, SessionState> sessionMap = new HashMap<>();
    // Map linking pending operations to sessions
    private static HashMap<String, String> pendingOperations = new HashMap<>();

    private static MifareRobot cardClient = new MifareRobot();

    public static void main(String[] args) {

        //Heroku assigns different port each time, hence reading it from process.
        ProcessBuilder process = new ProcessBuilder();
        Integer port;
        if (process.environment().get("PORT") != null) {
            port = Integer.parseInt(process.environment().get("PORT"));
        } else {
            port = 8080;
        }
        setPort(port);

        // Endpoint to test that the webapp is up and running
        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                System.out.println("GET detected on root");
                return "<h2>Welcome to the MIFARE tutorial Service Provider</h2>";
            }
        });

        // Endpoint for the service description. Description for service "invoke" will be uploaded
        // to Fidesmo server as a Service Recipe, so only requests for  service "delete"
        // or for unknown serviceIds should reach this endpoint.
        get(new Route("/describe/:serviceId") {
            @Override
            public Object handle(Request request, Response response) {
                String serviceId = request.params("serviceId");
                System.out.println("DESCRIBE endpoint - ServiceID: " + serviceId);
                JSONObject description = new JSONObject();

                // verify the serviceId
                if (serviceId.equals(Constants.invokeServiceId)){
                    description.put("title", Constants.invokeServiceTitle);
                } else if (serviceId.equals(Constants.deleteServiceId)) {
                    description.put("title", Constants.deleteServiceTitle);
                } else {
                    response.status(HTTP_NOT_FOUND);
                    return "HTTP 404 - Service not found";
                }
                description.put("confirmationRequired", Constants.requiresConfirmation);
                response.type("application/json");
                response.status(HTTP_OK);
                return description.toJSONString();
            }
        });

        // Endpoint for the Service Delivery Request
        put(new Route("/deliver") {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String serviceId = (String) jsonParameters.get("serviceId");
                System.out.println("DELIVER endpoint - ServiceID: " + serviceId);

                // verify the serviceId
                if (!serviceId.equals(Constants.invokeServiceId) && !serviceId.equals(Constants.deleteServiceId)) {
                    response.status(HTTP_NOT_FOUND);
                    return "HTTP 404 - Service not found";
                }

                // create an entry in the session map for this new session
                String sessionId = (String) jsonParameters.get("sessionId");
                sessionMap.put(sessionId, new SessionState());

                // launch first operation for each of the two services
                if (serviceId.equals(Constants.invokeServiceId)){
                    // first operation is GET CARD
                    if (cardClient.getCard(sessionId, pendingOperations) != HTTP_OK) handleFailure(sessionId);
                } else if (serviceId.equals(Constants.deleteServiceId)) {
                    // first operation is DELETE CARD
                    if (cardClient.deleteCard(sessionId, pendingOperations) != HTTP_OK) handleFailure(sessionId);
                }
                response.status(HTTP_OK);
                return "";
            }
        });

        // handles response from the initial Get MIFARE Card message
        post(new Route(Constants.getCardCallbackUrl) {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");

                // retrieve the sessionId using the operationId
                String sessionId = pendingOperations.get(operationId);
                System.out.println("GOT CARD endpoint - stored sessionID: " + sessionId);
                // remove operationId from the map
                pendingOperations.remove(operationId);

                int statusCode = ((Long)jsonParameters.get("statusCode")).intValue();
                if (statusCode != HTTP_OK)
                {
                    handleFailure(sessionId);
                } else {
                    Boolean newCard = (Boolean) jsonParameters.get("newCard");
                    // if a new virtual card has been created, it is necessary to initialize its keys and counter
                    // if not, proceed reading the counter.
                    if (newCard) {
                        if (cardClient.initializeCard(sessionId, pendingOperations) != HTTP_OK) handleFailure(sessionId);
                    } else {
                        if (cardClient.readBlock(sessionId, pendingOperations)!= HTTP_OK) handleFailure(sessionId);
                    }
                }
                response.status(HTTP_OK);
                return "";
            }
        });

        // handles response from the Initialize MIFARE Card message
        post(new Route(Constants.initializeCallbackUrl) {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");
                String sessionId = pendingOperations.get(operationId);
                System.out.println("CARD INITIALIZED endpoint - stored sessionID: " + sessionId);
                pendingOperations.remove(operationId);

                int statusCode = ((Long)jsonParameters.get("statusCode")).intValue();
                if (statusCode != HTTP_OK)
                {
                    handleFailure(sessionId);
                } else {
                    sessionMap.get(sessionId).setFirstTime(true);
                    if (cardClient.readBlock(sessionId, pendingOperations)!= HTTP_OK) handleFailure(sessionId);
                }
                response.status(HTTP_OK);
                return "";
            }
        });

        // handles response from the Read MIFARE Data message
        post(new Route(Constants.readBlockCallbackUrl) {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");
                String sessionId = pendingOperations.get(operationId);
                pendingOperations.remove(operationId);
                System.out.println("BLOCK READ endpoint - sessionID: " + sessionId);
                int statusCode = ((Long)jsonParameters.get("statusCode")).intValue();
                if (statusCode != HTTP_OK)
                {
                    handleFailure(sessionId);
                } else {
                    // parse the counter value from the read block, increment
                    // unless it is the first time the card is used, then initialize to one
                    // store counter and checksum in the session state map
                    JSONArray blocks = (JSONArray) jsonParameters.get("blocks");
                    long counter = Utils.decodeCounter(blocks);
                    sessionMap.get(sessionId).setCounter(counter);
                    String checksum = (String) jsonParameters.get("checksum");
                    sessionMap.get(sessionId).setChecksum(checksum);
                    if (sessionMap.get(sessionId).isFirstTime()) {
                        sessionMap.get(sessionId).setCounter(1);
                        if (cardClient.writeCounterInCard(1, checksum, sessionId, pendingOperations) != HTTP_OK) handleFailure(sessionId);
                    } else {
                        counter ++;
                        if (cardClient.writeCounterInCard(counter, checksum, sessionId, pendingOperations) != HTTP_OK) handleFailure(sessionId);
                    }
                }
                response.status(HTTP_OK);
                return "";
            }
        });

        // handles the response from the Write MIFARE Data message
        post(new Route(Constants.writeBlockCallbackUrl) {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");
                String sessionId = pendingOperations.get(operationId);
                pendingOperations.remove(operationId);
                System.out.println("BLOCK WRITE endpoint - sessionID: " + sessionId);
                int statusCode = ((Long)jsonParameters.get("statusCode")).intValue();
                if (statusCode != HTTP_OK)
                {
                    handleFailure(sessionId);
                } else {
                    // we are done - send the Service Delivery Completed message
                    cardClient.serviceDeliveryCompleted(true, sessionMap.get(sessionId).getCounter(), sessionId);
                    // Time to remove this session from the map - it is finished!
                    sessionMap.remove(sessionId);
                }
                response.status(HTTP_OK);
                return "";
            }
        });

        // handles the response from the Delete  MIFARE Card message
        post(new Route(Constants.deleteCardCallbackUrl) {
            @Override
            public Object handle(Request request, Response response) {
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");
                String sessionId = pendingOperations.get(operationId);
                pendingOperations.remove(operationId);
                System.out.println("DELETE CARD endpoint - sessionID: " + sessionId);
                int statusCode = ((Long)jsonParameters.get("statusCode")).intValue();
                if (statusCode != HTTP_OK)
                {
                    handleFailure(sessionId);
                } else {
                    cardClient.serviceDeliveryCompleted(true, 0, sessionId);
                    sessionMap.remove(sessionId);
                }
                response.status(HTTP_OK);
                return "";
            }
        });

    }

    // If any operation failed, report a failed service delivery to the user
    static void handleFailure(String sessionId) {
        // Remove this session's entry, if it exists
        sessionMap.remove(sessionId);
        cardClient.serviceDeliveryCompleted(false, 0, sessionId);
    }

}
