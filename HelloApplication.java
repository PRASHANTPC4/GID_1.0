package com.example.gid2;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load the FXML file for the main application
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DD2.fxml"));
            Scene mainScene = new Scene(fxmlLoader.load());

            // Set the title and other stage properties
            stage.setTitle("Fullscreen Application");
            stage.setResizable(false);

            // Exit the application when ESC key is pressed
            mainScene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    Platform.exit();
                }
            });

            // Show the image in fullscreen mode for 3 seconds
            showImageInFullscreen(stage, mainScene);

            // Show the stage
            stage.show();

            // Fetch data from the database
//            fetchDataFromDatabase();

        } catch (IOException e) {
            System.err.println("Failed to load FXML file: " + e.getMessage());
        }
    }

    private void showImageInFullscreen(Stage stage, Scene mainScene) {
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("SVUM.jpg")));
        ImageView imageView = new ImageView(image);

        // Make the ImageView responsive
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.fitWidthProperty().bind(stage.widthProperty());
        imageView.fitHeightProperty().bind(stage.heightProperty());

        // Create a StackPane to center the image
        StackPane stackPane = new StackPane(imageView);
        stackPane.setStyle("-fx-background-color: black;"); // Set background color
        Scene imageScene = new Scene(stackPane);

        // Set the image scene and go fullscreen
        stage.setScene(imageScene);
        stage.setFullScreen(true);

        // Timeline to switch to the main scene after the specified seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            stage.setScene(mainScene);
            stage.setFullScreen(true);
        }));
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void fetchDataFromDatabase() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            if (connection != null) {
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT * FROM user_cddata");
                    while (rs.next()) {
                        String data = rs.getString("cname");
                        System.out.println(data != null ? "Fetched Data: " + data : "No data found in 'cname' column.");
                    }
                }
            } else {
                System.err.println("Connection is null.");
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General Exception: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
