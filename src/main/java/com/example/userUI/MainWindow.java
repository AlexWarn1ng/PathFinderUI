package com.example.userUI;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.example.userUI.OptimalPath.OptimalPathFinder;

@Service
public class MainWindow {

    private final DistrictRepository DistrictRepository;
    private final OrderRepository OrderRepository;
    private final GeoJsonConverter converter;
    private long lastCountOfReadedOrders = 0;

    public MainWindow(OrderRepository OrderRepository, DistrictRepository DistrictRepository, ObjectMapper mapper) {
        this.OrderRepository = OrderRepository;
        this.DistrictRepository = DistrictRepository;
        this.converter = new GeoJsonConverter(mapper);
    }

    void connect(WebView webView, Label label, TableView tableView ) {
        WebEngine engine = webView.getEngine();
        long newCount = OrderRepository.countAllOrders() - lastCountOfReadedOrders;
        label.setText("New orders from last update: " + Long.toString(newCount));
        engine.executeScript("clearData()");


        try{OptimalPathFinder( 1, engine, converter);} catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (DistrictEntity district : DistrictRepository.findAll()) {
            try {
                engine.executeScript(
                        "addDistrict(" + converter.convert(district) + ")"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (OrderEntity order : OrderRepository.findAll()) {
            tableView.getItems().add(order);
            try {
                if (order.getCoords() == null) continue;

                engine.executeScript("addOrder(" + converter.convert(order) + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void window() {
        Button connectButton = new Button("Reload map");
        Button createButton = new Button("Create");
        connectButton.setFocusTraversable(false);
        createButton.setFocusTraversable(false);
        Label label = new Label();
        label.setText("Click 'Reload map' to update map data");
        ToolBar toolBar = new ToolBar(connectButton, createButton);
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        TableView tableView = new TableView(); createTable(tableView);

        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        connectButton.setDisable(false);
                    }
                }
        );
        engine.load(getClass().getResource("/map.html").toExternalForm());

        connectButton.setOnAction(value -> {
            connect(webView, label, tableView);
        });

        Stage stage = new Stage();
        VBox vBox = new VBox(toolBar, label, webView, tableView);
        Scene scene = new Scene(vBox, 1000, 800);
        stage.setScene(scene);
        stage.setTitle("userUI");
        stage.show();

    }

    void createTable(TableView<OrderEntity> tableView) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        TableColumn<OrderEntity, Integer> id = new TableColumn<>("ID");
        TableColumn<OrderEntity, Integer> district = new TableColumn<>("District");
        TableColumn<OrderEntity, Point> coords = new TableColumn<>("Coordinates");
        TableColumn<OrderEntity, Integer> urgency = new TableColumn<>("Urgency");
        TableColumn<OrderEntity, Integer> status = new TableColumn<>("Status");
        TableColumn<OrderEntity, String> createdAt = new TableColumn<>("Created At");
        TableColumn<OrderEntity, String> deadline = new TableColumn<>("Deadline");

        id.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        district.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDistrictId()));
        coords.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCoords()));
        urgency.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getUrgency()));
        status.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStatus()));


        deadline.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDeadline().format(formatter))
        );

        createdAt.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCreatedAt().format(formatter))
        );

        tableView.getColumns().addAll(id, district, coords, deadline, urgency, status, createdAt);
    }

}