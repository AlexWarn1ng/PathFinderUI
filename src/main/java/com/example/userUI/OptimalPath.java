package com.example.userUI;

import javafx.scene.web.WebEngine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OptimalPath {

    static void OptimalPathFinder(Integer CourierId, Integer startOrderId, WebEngine webEngine, GeoJsonConverter converter) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/getoptimalpath?CourierId=" + CourierId + "&startOrderId=" + startOrderId)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }

        webEngine.executeScript("addOptimalPath(" + response.body() + ")");
    }
}