package com.example.userUI;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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
    private WebEngine engine;

    public MainWindow(OrderRepository OrderRepository, DistrictRepository DistrictRepository, ObjectMapper mapper) {
        this.OrderRepository = OrderRepository;
        this.DistrictRepository = DistrictRepository;
        this.converter = new GeoJsonConverter(mapper);
    }

    void connect(WebView webView, Label label, TableView tableView) {
        WebEngine engine = webView.getEngine();
        long ordersCount = OrderRepository.countAllOrders();
        label.setText("Orders from last update: " + Long.toString(ordersCount));
        engine.executeScript("clearData()");


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
        engine = webView.getEngine();
        TableView<CouriersEntity> tableView = new TableView<>();
        createCouriersTable(tableView);

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

    void createCouriersTable(TableView<CouriersEntity> tableView) {

        TableColumn<CouriersEntity, Integer> id = new TableColumn<>("ID");
        TableColumn<CouriersEntity, String> name = new TableColumn<>("Name");
        TableColumn<CouriersEntity, String> surname = new TableColumn<>("Surname");
        TableColumn<CouriersEntity, String> phone = new TableColumn<>("Phone");
        TableColumn<CouriersEntity, String> workingTime = new TableColumn<>("Working Time");
        TableColumn<CouriersEntity, String> workDistrict = new TableColumn<>("Work District");

        id.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        name.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        surname.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSurname()));
        phone.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPhone()));
        workingTime.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getWorkingTime()));
        workDistrict.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getWorkDistrict()));

        TableColumn<CouriersEntity, Void> action = new TableColumn<>("Action");

        action.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<Integer> comboBox = new ComboBox<>();
            private final Button button = new Button("Make route");
            private final HBox box = new HBox(10, comboBox, button);

            {
                comboBox.setPromptText("Start order");

                comboBox.setOnShowing(event -> {
                    try {
                        Integer courierId = getTableRow().getItem().getId();
                        String json = Requests.getCourierOrderIds(courierId);
                        comboBox.getItems().setAll(converter.parseCourierOrderIds(json));

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                button.setOnAction(event -> {
                    try {
                        Integer courierId = getTableRow().getItem().getId();
                        Integer startOrderId = comboBox.getValue();

                        if (startOrderId == null) return;

                        OptimalPathFinder(courierId, startOrderId, engine, converter);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                comboBox.getItems().clear();
                comboBox.setValue(null);
                setGraphic(empty ? null : box);
            }
        });

        action.setPrefWidth(260);
        action.setMinWidth(260);
        action.setMaxWidth(260);
        action.setResizable(false);
        action.setReorderable(false);

        tableView.getColumns().addAll(id, name, surname, phone, workingTime, workDistrict, action);

        try {
            String json = Requests.getCouriers();
            tableView.getItems().setAll(converter.parseCouriers(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}