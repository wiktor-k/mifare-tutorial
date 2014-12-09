package com.fidesmo.mifaretutorial;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Client to send operations to a MIFARE Classic card, using the Fidesmo API
 */
public class MifareRobot {

    // Sends a Get MIFARE Card request
    public int getCard(String sessionId, HashMap<String, String> pendingOperations) {
        String url = "https://api.fidesmo.com/mifare/get";
        String callbackUrl = Constants.rootUrl + Constants.getCardCallbackUrl;
        JSONObject payload = new JSONObject();
        payload.put("sectors", 4);

        return sendOperationAndProcessResponse(url, callbackUrl, payload, sessionId, pendingOperations);
    }

    // Initialize the first sector of our virtual MIFARE card
    public int initializeCard(String sessionId, HashMap<String, String> pendingOperations) {
        String url = "https://api.fidesmo.com/mifare/initialize";
        String callbackUrl = Constants.rootUrl + Constants.initializeCallbackUrl;

        // Encode the JSON structure with the trailer data for one sector
        JSONObject payload = new JSONObject();
        JSONArray trailerData = new JSONArray();
        trailerData.add(Utils.encodeSectorTrailer(1, Constants.keyA, Constants.keyB, Constants.accessBits));
        payload.put("trailers", trailerData);
        payload.put("overwriteInvalidAccessBits", true);

        return sendOperationAndProcessResponse(url, callbackUrl, payload, sessionId, pendingOperations);
    }

    // Read block 1 of sector 1 of the virtual MIFARE card
    public int readBlock(String sessionId, HashMap<String, String> pendingOperations) {
        String url = "https://api.fidesmo.com/mifare/read";
        String callbackUrl = Constants.rootUrl + Constants.readBlockCallbackUrl;

        // Encode the JSON structure with the trailer data for one sector
        JSONObject payload = new JSONObject();
        JSONArray blockData = new JSONArray();
        blockData.add(Utils.encodeBlockToRead(1, 1));
        payload.put("blocks", blockData);

        return sendOperationAndProcessResponse(url, callbackUrl, payload, sessionId, pendingOperations);
    }

    // Write a hex-encoded value into block 1 of sector 1 of the virtual MIFARE card
    public int writeCounterInCard(long counter, String checksum, String sessionId, HashMap<String, String> pendingOperations) {
        String url = "https://api.fidesmo.com/mifare/write";
        String callbackUrl = Constants.rootUrl + Constants.writeBlockCallbackUrl;
        JSONObject payload = new JSONObject();
        JSONArray blockData = new JSONArray();
        blockData.add(Utils.encodeIntInBlock(counter, 1, 1));
        payload.put("blocks", blockData);
        payload.put("checksum", checksum);

        return sendOperationAndProcessResponse(url, callbackUrl, payload, sessionId, pendingOperations);
    }

    // Deletes the virtual MIFARE card
    public int deleteCard(String sessionId, HashMap<String, String> pendingOperations) {
        String url = "https://api.fidesmo.com/mifare/delete";
        String callbackUrl = Constants.rootUrl + Constants.deleteCardCallbackUrl;

        return sendOperationAndProcessResponse(url, callbackUrl, null, sessionId, pendingOperations);
    }

    // Sends the message finalizing the delivery process
    public void serviceDeliveryCompleted(boolean successful, long counter, String sessionId) {
        String url = "https://api.fidesmo.com/service/completed";

        JSONObject payload = new JSONObject();
        // output different messages depending on the delivery's success or failure
        if (successful) {
            payload.put("success", true);
            // counter == 0 means that the service's virtual card has been removed
            if (counter == 0) {
                payload.put("message", "CTULHU HAS ABANDONED THIS CARD.\nThe service has been deleted");
            } else {
                payload.put("message", "CTULHU IS PLEASED.\nYou have invoked Him " + counter + " times");
            }
        } else {
            payload.put("success", false);
            payload.put("message", "CTULHU IS VERY ANGRY.\nYour invocation failed.");
        }

        sendOperationAndProcessResponse(url, null, payload, sessionId, null);
    }

    // Creates a connection with the common headers used in all Fidesmo API operations,
    // adds the message payload if necessary and process the response.
    // If successful, extracts the operation ID from a response and inserts it into the map of pending operations
    // Returns the HTTP Response Code
    public int sendOperationAndProcessResponse(String url, String callbackUrl, JSONObject payload, String sessionId, HashMap<String, String> pendingOperations) {
        int responseCode = 0;
        try {
            // prepare the operation request
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("sessionId", sessionId);
            connection.setRequestProperty("app_id", Constants.applicationId);
            connection.setRequestProperty("app_key", Constants.applicationKeys);
            connection.setRequestProperty("Content-Type", "application/json");

            if (callbackUrl != null) {
                connection.setRequestProperty("callbackUrl", callbackUrl);
            }

            if (payload != null) {
                OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
                osw.write(payload.toJSONString());
                osw.flush();
                osw.close();
            }

            // send it and process the response
            responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) return responseCode;

            // we need to extract the operationId if the "pendingOperations" map is provided
            if (pendingOperations != null) {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = inputReader.readLine()) != null) {
                    response.append(inputLine);
                }
                inputReader.close();

                JSONObject jsonResponse = (JSONObject) JSONValue.parse(response.toString());
                String operationId = (String) jsonResponse.get("operationId");
                pendingOperations.put(operationId, sessionId);
            }
            return responseCode;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseCode;
    }

}
