package com.fidesmo.mifaretutorial;

import org.json.simple.JSONValue;
import spark.Request;
import spark.Response;
import spark.Route;
import org.json.simple.JSONObject;

import java.util.ArrayList;

import static spark.Spark.*;

public class MifareSP {

    // Just store POST data within a ArrayList for now
    public static ArrayList<String> things = new ArrayList<String>();

    private static MifareRobot cardClient;

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

        // GET routes
        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                System.out.println("GET detected on root");
                return "<h2>Welcome to the MIFARE tutorial Service Provider</h2>";
            }
        });

        get(new Route("/describe/:serviceId") {
            @Override
            public Object handle(Request request, Response response) {
                String serviceId = request.params("serviceId");
                System.out.println("DESCRIBE endpoint - ServiceID: " + serviceId);

                JSONObject description = new JSONObject();
                description.put("title", Constants.serviceTitle);
                description.put("confirmationRequired", Constants.requiresConfirmation);
                System.out.println("Getting response ready: " + description.toString());

                response.type("application/json");
                response.status(200);
                return description.toJSONString();
            }
        });

        put(new Route("/deliver") {
            @Override
            public Object handle(Request request, Response response) {
                System.out.println("DELIVER endpoint - Request: " + request.toString());
                System.out.println("DELIVER endpoint - Request headers: " + request.headers());
                System.out.println("DELIVER endpoint - Request body: " + request.body());
                String sessionId = "";
                try {
                    //JSONArray array = (JSONArray) parser.parse(request.body());
                    //System.out.println("JSON Array received at DELIVER endpoint " + array);
                    // TODO parse the body not as an array, but as an object
                    //JSONObject miniMap = (JSONObject) array.get(0);
                    JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                    String serviceId = (String) jsonParameters.get("serviceId");
                    System.out.println("DELIVER endpoint - ServiceID: " + serviceId);
                    sessionId = (String) jsonParameters.get("sessionId");
                    System.out.println("DELIVER endpoint - SessionID: " + sessionId);
                } catch (/*org.json.simple.parser.ParseException*/ Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Error when parsing DELIVER body");
                }

                // TODO store sessionId in a map

                // launch first operation: GET CARD - this command breaks this, must do in other thread
                cardClient.getCard(sessionId);

                response.status(200);
                return "";
            }
        });

        // handles response from the initial Get MIFARE Card message
        post(new Route("/gotcard") {
            @Override
            public Object handle(Request request, Response response) {
                System.out.println("GOT CARD endpoint - Request: " + request.toString());
                System.out.println("GOT CARD endpoint - Request headers: " + request.headers());
                System.out.println("GOT CARD endpoint - Request body: " + request.body());

                // TODO parse response, launch initialization or read sector
                JSONObject jsonParameters = (JSONObject) JSONValue.parse(request.body());
                String operationId = (String) jsonParameters.get("operationId");
                System.out.println("GOT CARD endpoint - operationID: " + operationId);
                String statusCode = (String) jsonParameters.get("statusCode");
                System.out.println("GOT CARD endpoint - Status Code: " + statusCode);
                String uid = (String) jsonParameters.get("uid");
                System.out.println("GOT CARD endpoint - UID: " + uid);
                Boolean newCard = (Boolean) jsonParameters.get("newCard");
                System.out.println("GOT CARD endpoint - New card created?: " + newCard);

                response.status(200);
                return "";
            }
        });

    }

}
