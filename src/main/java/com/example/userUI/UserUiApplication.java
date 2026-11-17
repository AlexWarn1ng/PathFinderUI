package com.example.userUI;

import javafx.application.Platform;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UserUiApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(UserUiApplication.class, args);
        MainWindow mainWindow = context.getBean(MainWindow.class);
        Platform.startup(() -> mainWindow.window());
    }
}
