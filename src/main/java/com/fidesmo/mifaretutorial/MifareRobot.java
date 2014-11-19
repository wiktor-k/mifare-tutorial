package com.fidesmo.mifaretutorial;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * Client used to send operations to a MIFARE Classic card, using the Fidesmo API
 */
public class MifareRobot {

    // Sends a Get MIFARE Card request
    public void getCard(String sessionId) {

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut("https://api.fidesmo.com/mifare/get");
        httpPut.addHeader("app_id", Constants.applicationId);
        httpPut.addHeader("app_key", Constants.applicationKeys);
        httpPut.addHeader("callbackUrl", Constants.rootUrl + "/gotcard");
        httpPut.addHeader("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("sectors", 4);

        CloseableHttpResponse response = null;

        try {
            StringEntity payloadEntity = new StringEntity(payload.toJSONString());
            httpPut.setEntity(payloadEntity);
            response = client.execute(httpPut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // check the synchronous response
        try {
            int status = response.getStatusLine().getStatusCode();
            System.out.println("MifareRobot.getCard: synchronous status code is " + status);
            if (status > 300) {
                System.out.println("MifareRobot.getCard: must terminate this session");
                // TODO remove session from database
            }
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
