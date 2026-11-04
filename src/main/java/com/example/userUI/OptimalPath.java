package com.example.userUI;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OptimalPath {

   static void OptimalPathFinder(Integer districtId, WebEngine webEngine,GeoJsonConverter converter) throws Exception{
       HttpClient client = HttpClient.newHttpClient();
       HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/getoptimalpath?districtId="+ districtId)).GET().build();
       HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
       String geoJson = response.body();
       geoJson = converter.convertPath(response.body());
       webEngine.executeScript("addOptimalPath(" + geoJson + ")");

    }
}
