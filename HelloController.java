package com.example.gid2;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.event.ActionEvent; // Import this if not already present
import javafx.scene.Node;
import java.awt.Desktop;
import java.net.URI;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class HelloController {

    @FXML
    private Label welcomeText, firm_name, contact_person, product_category_name, mobile_number, address,
            business_details, office_number;
    @FXML
    private VBox vbox;
    @FXML
    private ComboBox<String> product_category_nameComboBox, city_nameComboBox;
    //    @FXML
//    private Button deletButton, printButton, searchButton, RefreshButton, onalldataButtonCLick,
//            onPrintButtonClick, onRefreshButtonClick;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Hyperlink email_address, website;
    //    @FXML
//    private ScrollPane Scrollpanel;
//    @FXML
//    private AnchorPane anchorPane;
    @FXML
    private ImageView imageView1; // Add your ImageView here
    private final String[] imageUrls = {
            "1.jpg",
            "2.jpg",
            "3.jpg",
            // Add more image URLs as needed
    };
    private int currentImageIndex = 0;
    @FXML
    public void initialize() {
        // Existing initialization logic
        System.out.println("Initializing HelloController...");
        fetchCategoriesFromDatabase();
        fetchAllFirmNames(); // Load all firms initially
        email_address.setOnAction(event -> handleEmailClick());
        website.setOnAction(event -> handleWebsiteClick());

        product_category_nameComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                clearCityAndResetUI();
                fetchFirmNamesByCategory(newValue);
                fetchCitiesFromDatabase(newValue);
            }
        });

        // Start the image rotation when the application initializes
        startImageRotation();
    }

    private void startImageRotation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> changeImage()));
        timeline.setCycleCount(Timeline.INDEFINITE); // Repeat indefinitely
        timeline.play();
    }

    private void changeImage() {
        currentImageIndex = (currentImageIndex + 1) % imageUrls.length; // Cycle through images
        imageView1.setImage(new Image(getClass().getResourceAsStream(imageUrls[currentImageIndex])));
    }
    private void clearCityAndResetUI() {
        // Clear the city combo box and firm results
        city_nameComboBox.getItems().clear();
        city_nameComboBox.getItems().add("All city");
        city_nameComboBox.setValue("All city");
        vbox.getChildren().clear();
        // Clear all company detail labels
        firm_name.setText("");
        contact_person.setText("");
        product_category_name.setText("");
        mobile_number.setText("");
        address.setText("");
        business_details.setText("");
        email_address.setText("");
        website.setText("");
        office_number.setText("");
    }

    private void fetchFirmNamesByCategory(String selectedCategory) {
        vbox.getChildren().clear(); // Clear existing firm names

        // Show the progress indicator while loading
        progressIndicator.setVisible(true);

        // Create a Task to fetch data in the background
        Task<List<String>> fetchTask = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> firmNames = new ArrayList<>();
                String query = "SELECT firm_name FROM user_cddata WHERE product_category_name = ? ORDER BY firm_name ASC";

                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(query)) {

                    stmt.setString(1, selectedCategory); // Set the selected category
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        firmNames.add(rs.getString("firm_name"));
                    }
                } catch (SQLException e) {
                    System.err.println("Error fetching firm names: " + e.getMessage());
                }

                return firmNames; // Return the list of firm names
            }
        };
        // When the task completes, update the VBox with the fetched firm names
        fetchTask.setOnSucceeded(event -> {
            List<String> firmNames = fetchTask.getValue(); // Get the result from the task
            for (String firmName : firmNames) {
                Label label = new Label(firmName);
                label.setOnMouseClicked(event1 -> {
                    if (event1.getButton() == MouseButton.PRIMARY) {
                        fetchCompanyDetails(firmName); // Fetch details when clicked
                    }
                });
                vbox.getChildren().add(label); // Add to VBox
            }
            // Hide the progress indicator
            progressIndicator.setVisible(false);
        });
        // When the task fails, show an error message (optional)
        fetchTask.setOnFailed((WorkerStateEvent _) -> {
            Throwable exception = fetchTask.getException();
            showAlert("Error fetching firm names: " + exception.getMessage());
            // Hide the progress indicator in case of error
            progressIndicator.setVisible(false);
        });

        // Start the task on a background thread
        new Thread(fetchTask).start();
    }

    private void fetchAllFirmNames() {
        vbox.getChildren().clear();
        String query = "SELECT firm_name FROM user_cddata ORDER BY firm_name ASC";

        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String firm_nameValue = rs.getString("firm_name");
                    Label label = new Label(firm_nameValue);
                    label.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            fetchCompanyDetails(firm_nameValue);
                        }
                    });
                    vbox.getChildren().add(label);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching firm names: " + e.getMessage());
        }
    }
    private void fetchCategoriesFromDatabase() {
        List<String> categories = new ArrayList<>();
        categories.add("Select Category"); // Add "Select Category" as the first item

        String query = "SELECT DISTINCT product_category_name FROM user_cddata ORDER BY product_category_name ASC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("product_category_name"));
            }

            // Clear previous items and add all categories to ComboBox
            product_category_nameComboBox.getItems().setAll(categories);
            product_category_nameComboBox.setValue("Select Category"); // Set default value to "Select Category"

        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
    }

    private void fetchCitiesFromDatabase(String selectedProductCategoryName) {
        List<String> cities = new ArrayList<>();
        String query = "SELECT DISTINCT city_name FROM user_cddata WHERE product_category_name = ? ORDER BY city_name ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, selectedProductCategoryName);
            ResultSet rs = stmt.executeQuery();

            // Clear the ComboBox and add "All city" as the first option
            city_nameComboBox.getItems().clear();
            city_nameComboBox.getItems().add("All city");  // Default option to select all cities

            while (rs.next()) {
                cities.add(rs.getString("city_name"));
            }

            // Add the cities to the ComboBox after "All city"
            city_nameComboBox.getItems().addAll(cities);
            city_nameComboBox.setValue("All city");  // Set default to "All city"

        } catch (SQLException e) {
            System.err.println("Error fetching cities: " + e.getMessage());
        }
    }

    @FXML
    private void onSearchButtonClick() {
        String category = product_category_nameComboBox.getValue();
        String city = city_nameComboBox.getValue();

        if (category == null || category.isEmpty() || "Select Category".equals(category)) {
            showAlert("Please select a product category.");
            return;
        }

        // Reset the firm list and details when a new search is initiated
        vbox.getChildren().clear();
        fetchDataBasedOnSearch(category, city);
    }

    private void fetchDataBasedOnSearch(String category, String city) {
        vbox.getChildren().clear(); // Clear previous results
        String query = city != null && !city.isEmpty() && !"All city".equals(city)
                ? "SELECT firm_name FROM user_cddata WHERE product_category_name = ? AND city_name = ? ORDER BY firm_name ASC"
                : "SELECT firm_name FROM user_cddata WHERE product_category_name = ? ORDER BY firm_name ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, category);
            if (city != null && !city.isEmpty() && !"All city".equals(city)) {
                stmt.setString(2, city);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String firmName = rs.getString("firm_name");
                Label label = new Label(firmName);
                label.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        fetchCompanyDetails(firmName);
                    }
                });
                vbox.getChildren().add(label); // Add firm names to VBox
            }
        } catch (SQLException e) {
            System.err.println("Error fetching search results: " + e.getMessage());
        }
    }

    private void fetchCompanyDetails(String firmName) {
        String query = "SELECT * FROM user_cddata WHERE firm_name = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, firmName);
            System.out.println("Executing query: " + query);  // Debugging output
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("Data fetched successfully for firm: " + firmName);  // Debugging output

                contact_person.setText("Contact Person: " + (rs.getString("contact_person") != null ? rs.getString("contact_person") : "N/A"));
                product_category_name.setText(rs.getString("product_category_name") != null ? rs.getString("product_category_name") : "N/A");
                mobile_number.setText("Mobile Number: " + (rs.getString("mobile_number") != null ? rs.getString("mobile_number") : "N/A"));
                business_details.setText(rs.getString("business_details") != null ? rs.getString("business_details") : "N/A");

// Address, City, and Pincode
                String addressText = rs.getString("address") != null ? rs.getString("address") : "N/A";
                String city = rs.getString("city_name") != null ? rs.getString("city_name") : "N/A";
                String pincode = rs.getString("pincode") != null ? rs.getString("pincode") : "N/A";
                address.setText(addressText + "\n" + city + " - " + pincode);

                email_address.setText("Email: " + (rs.getString("email_address") != null ? rs.getString("email_address") : "N/A"));
                website.setText("Website: " + (rs.getString("website") != null ? rs.getString("website") : "N/A"));
                office_number.setText("Office Number: " + (rs.getString("office_number") != null ? rs.getString("office_number") : "N/A"));
                firm_name.setText(firmName);  // Display the firm name

            } else {
                System.err.println("No details found for firm name: " + firmName);  // Debugging output
                showAlert("No details found for firm name: " + firmName);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching company details: " + e.getMessage());
            showAlert("Error fetching company details: " + e.getMessage());
        }
    }
    @FXML
    private void handleEmailClick() {
        String email = email_address.getText().replace("Email: ", "");
        if (!email.isEmpty()) {
            openHyperlink(email, "mailto:");
        } else {
            showAlert("Email link not available.");
        }
    }
    @FXML
    private void handleWebsiteClick() {
        String websiteUrl = website.getText().replace("Website: ", "");
        if (!websiteUrl.isEmpty()) {
            openHyperlink(websiteUrl, "");
        } else {
            showAlert("Website link not available.");
        }
    }

    private void openHyperlink(String link, String prefix) {
        if (!link.isEmpty()) {
            try {
                System.out.println("Opening link: " + prefix + link);  // Debugging output
                Desktop.getDesktop().browse(new URI(prefix + link));
            } catch (Exception e) {
                showAlert("Error opening link: " + e.getMessage());
                System.err.println("Error opening link: " + e.getMessage());  // Debugging output
            }
        } else {
            showAlert("Link not available.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onalldataButtonCLick() {
        vbox.getChildren().clear();
        // Add a ProgressIndicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50); // Customize size
        vbox.getChildren().add(progressIndicator);

        // Create a Task for background data fetching
        Task<Void> loadDataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    if (connection != null) {
                        try (Statement stmt = connection.createStatement()) {
                            String query = "SELECT firm_name FROM user_cddata ORDER BY firm_name ASC";
                            ResultSet rs = stmt.executeQuery(query);

                            while (rs.next()) {
                                String firm_nameValue = rs.getString("firm_name");

                                // Update the UI in Platform.runLater
                                Platform.runLater(() -> {
                                    Label label = new Label(firm_nameValue);
                                    label.setOnMouseClicked(event -> {
                                        if (event.getButton() == MouseButton.PRIMARY) {
                                            fetchCompanyDetails(firm_nameValue);
                                        }
                                    });
                                    vbox.getChildren().add(label);
                                });
                            }
                        }
                    } else {
                        throw new SQLException("Connection is null.");
                    }
                } catch (SQLException e) {
                    Platform.runLater(() ->
                            System.err.println("Error fetching all data: " + e.getMessage())
                    );
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                vbox.getChildren().remove(progressIndicator); // Remove ProgressIndicator
            }

            @Override
            protected void failed() {
                super.failed();
                vbox.getChildren().remove(progressIndicator); // Remove ProgressIndicator
                System.err.println("Error loading data: " + getException().getMessage());
            }
        };

        // Start the Task in a new Thread
        Thread thread = new Thread(loadDataTask);
        thread.setDaemon(true);
        thread.start();
    }
    @FXML
    protected void onCloseButtonClick() {
        if (welcomeText != null && welcomeText.getScene() != null) {
            Stage stage = (Stage) welcomeText.getScene().getWindow();
            stage.close();
        } else {
            System.out.println("Error: welcomeText is null.");
        }
    }
    @FXML
    protected void onAboutUsButtonClick(ActionEvent event) {
        System.out.println("About Us button clicked.");

        // Creating the Alert dialog
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About Us");
        alert.setHeaderText("D.P. Infonet");
        alert.setContentText(
                "304, Rajat Complex, Sardar Nagar Main Road,\n" +
                        "Rajkot- 360001, Gujarat, India.\n\n" +
                        "Email: dpinfonetrajkot@gmail.com\n" +
                        "Phone: +91 94262 54611"
        );

        // Load custom CSS for styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("alert.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        // Set the alert to modal mode
        alert.initModality(Modality.APPLICATION_MODAL);
        // Get the primary stage from the event source
        Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        alert.initOwner(primaryStage); // Sets the owner to the primary stage

        alert.showAndWait(); // Displays the popup and blocks interaction with the main application
    }
    @FXML
    private void onPrintButtonClick(ActionEvent event) {
        // Create a dialog box for print options
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Print Options");
        dialog.setHeaderText("Select Print Option");

        // Set up the buttons
        ButtonType printByCategoryAndCityButton = new ButtonType("Print by Category and City", ButtonBar.ButtonData.OK_DONE);
        ButtonType printByCategoryButton = new ButtonType("Print by Category", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(printByCategoryAndCityButton, printByCategoryButton, cancelButton);

        // Initialize ComboBoxes for category and city
        ComboBox<String> productCategoryComboBox = new ComboBox<>();
        ComboBox<String> cityComboBox = new ComboBox<>();
        productCategoryComboBox.getItems().add("Select Category");
        cityComboBox.getItems().add("All city");

        // Fetch categories first and fill product_category_nameComboBox
        fetchCategoriesFromDatabase(productCategoryComboBox);

        // Set the event for category selection to populate the city ComboBox
        productCategoryComboBox.setOnAction(e -> {
            String selectedCategory = productCategoryComboBox.getValue();
            if (selectedCategory != null && !selectedCategory.equals("Select Category")) {
                fetchCitiesFromDatabase(selectedCategory, cityComboBox); // Fetch cities based on selected category
            }
        });

        // Set ComboBoxes into the dialog content
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(new Label("Select Category:"), productCategoryComboBox, new Label("Select City:"), cityComboBox);
        dialog.getDialogPane().setContent(vbox);

        // Handle the selected button action
        dialog.setResultConverter(dialogButton -> {
            String selectedCategoryValue = productCategoryComboBox.getValue();
            String selectedCityValue = cityComboBox.getValue();

            if (dialogButton == printByCategoryAndCityButton) {
                // Fetch data and call print method for Category and City filter
                printByCategoryAndCity(selectedCategoryValue, selectedCityValue);
            } else if (dialogButton == printByCategoryButton) {
                // Fetch data and call print method for Category filter
                printByCategory(selectedCategoryValue);
            }

            return null;
        });

        // Get the primary stage from the event source
        Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        dialog.initOwner(primaryStage); // Set the owner of the dialog

        // Show the dialog
        dialog.showAndWait();
    }
    // Helper method to fetch categories from the database and update ComboBox
    private void fetchCategoriesFromDatabase(ComboBox<String> categoryComboBox) {
        List<String> categories = new ArrayList<>();
        categories.add("Select Category"); // Add "Select Category" as the first item

        String query = "SELECT DISTINCT product_category_name FROM user_cddata ORDER BY product_category_name ASC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("product_category_name"));
            }
            // Update the ComboBox with categories
            categoryComboBox.getItems().setAll(categories);
            categoryComboBox.setValue("Select Category");

        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
    }
    // Helper method to fetch cities based on selected category and update ComboBox
    private void fetchCitiesFromDatabase(String selectedCategory, ComboBox<String> cityComboBox) {
        List<String> cities = new ArrayList<>();
        String query = "SELECT DISTINCT city_name FROM user_cddata WHERE product_category_name = ? ORDER BY city_name ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, selectedCategory);
            ResultSet rs = stmt.executeQuery();

            // Clear and add "All city" option
            cityComboBox.getItems().clear();
            cityComboBox.getItems().add("All city");

            while (rs.next()) {
                cities.add(rs.getString("city_name"));
            }

            // Add the cities to the ComboBox
            cityComboBox.getItems().addAll(cities);
            cityComboBox.setValue("All city");

        } catch (SQLException e) {
            System.err.println("Error fetching cities: " + e.getMessage());
        }
    }

    // Helper method to validate category and city
    private boolean isValidCategoryAndCity(String category, String city) {
        return category != null && !category.isEmpty() && !category.equals("Select Category") && city != null && !city.isEmpty();
    }

    // Helper method to validate category
    private boolean isValidCategory(String category) {
        return category != null && !category.isEmpty() && !category.equals("Select Category");
    }

    // Method to show the alert box with error messages
    private void showAlert1(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid Input");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void printByCategory(String category) {
        if (!isValidCategory(category)) {
            showAlert1("Please select a category.");
            return;
        }

        List<String> allFirmDetails = new ArrayList<>();
        String query = "SELECT * FROM user_cddata WHERE product_category_name = ? ORDER BY firm_name ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Extract details for each firm
                String firmNameText = rs.getString("firm_name");
                String contactPersonText = rs.getString("contact_person");
                String mobileNumberText = rs.getString("mobile_number");
                String addressText = rs.getString("address");

// Split the address into parts by commas
                String[] parts = addressText.split(",");
                StringBuilder formattedAddress = new StringBuilder();

// Loop through the parts and add a newline after every second part, but limit to 4 parts
                int maxParts = 4;  // Maximum parts to display
                for (int i = 0; i < parts.length && i < maxParts; i++) {
                    formattedAddress.append(parts[i].trim());
                    if ((i + 1) % 2 == 0 && i != maxParts - 1) {  // After every second comma, but not the last part
                        formattedAddress.append(",\n");
                    } else if (i < maxParts - 1) {
                        formattedAddress.append(", ");
                    }
                }
                addressText = formattedAddress.toString();

                String city_nameText = rs.getString("city_name");
                String pincodeText = rs.getString("pincode");
                String formattedCityPincode = city_nameText + " - " + pincodeText;

                // Format the details without an empty line after the address
                String details = String.format(
                        "%s\n%s\n%s\n%s\n%s",
                        firmNameText,
                        contactPersonText,
                        mobileNumberText,
                        addressText.trim(),
                        formattedCityPincode
                );

                // Add to the list of all firms
                allFirmDetails.add(details);
            }

            // Print all stickers for firms in the selected category
            StickerPrinter.printSticker(allFirmDetails);

        } catch (SQLException e) {
            showAlert1("Error fetching category data: " + e.getMessage());
            System.err.println("Error fetching category data: " + e.getMessage());
        }
    }

    private void printByCategoryAndCity(String category, String city) {
        if (!isValidCategoryAndCity(category, city) || city.equals("All city")) {
            showAlert1("Please select both a category and a valid city.");
            return;
        }

        List<String> allFirmDetails = new ArrayList<>();
        String query = "SELECT * FROM user_cddata WHERE product_category_name = ? AND city_name = ? ORDER BY firm_name ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, category);
            stmt.setString(2, city);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Extract details for each firm
                String firmNameText = rs.getString("firm_name");
                String contactPersonText = rs.getString("contact_person");
                String mobileNumberText = rs.getString("mobile_number");
                String addressText = rs.getString("address");

// Split the address into parts by commas
                String[] parts = addressText.split(",");
                StringBuilder formattedAddress = new StringBuilder();

// Loop through the parts and add a newline after every second part, but limit to 4 parts
                int maxParts = 4;  // Maximum parts to display
                for (int i = 0; i < parts.length && i < maxParts; i++) {
                    formattedAddress.append(parts[i].trim());
                    if ((i + 1) % 2 == 0 && i != maxParts - 1) {  // After every second comma, but not the last part
                        formattedAddress.append(",\n");
                    } else if (i < maxParts - 1) {
                        formattedAddress.append(", ");
                    }
                }
                addressText = formattedAddress.toString();

                String city_nameText = rs.getString("city_name");
                String pincodeText = rs.getString("pincode");
                String formattedCityPincode = city_nameText + " - " + pincodeText;

                // Format the details without an empty line after the address
                String details = String.format(
                        "%s\n%s\n%s\n%s\n%s",
                        firmNameText,
                        contactPersonText,
                        mobileNumberText,
                        addressText.trim(),
                        formattedCityPincode
                );

                // Add to the list of all firms
                allFirmDetails.add(details);
            }

            // Print all stickers for firms in the selected category and city
            StickerPrinter.printSticker(allFirmDetails);

        } catch (SQLException e) {
            showAlert1("Error fetching data for category and city: " + e.getMessage());
            System.err.println("Error fetching data for category and city: " + e.getMessage());
        }
    }

    @FXML
    private void onPrintAllButtonClick() {
        // Create a TextArea to display the company details
        TextArea companyDetailsArea = new TextArea();
        companyDetailsArea.setWrapText(true); // Ensure text wraps in the TextArea
        companyDetailsArea.setEditable(false); // Make it non-editable

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<String> allCompanyDetails = new ArrayList<>();
                String query = "SELECT * FROM user_cddata ORDER BY firm_name ASC";

                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        // Extract and process the data
                        String firmNameText = rs.getString("firm_name");
                        String contactPersonText = rs.getString("contact_person");
                        String mobileNumberText = rs.getString("mobile_number");
                        String addressText = rs.getString("address");

                        // Split the address into parts by commas
                        String[] parts = addressText.split(",");
                        StringBuilder formattedAddress = new StringBuilder();

                        // Loop through the parts and add a newline after every second part, but limit to 4 parts
                        int maxParts = 4;  // Maximum parts to display
                        for (int i = 0; i < parts.length && i < maxParts; i++) {
                            formattedAddress.append(parts[i].trim());
                            if ((i + 1) % 2 == 0 && i != maxParts - 1) {  // After every second comma, but not the last part
                                formattedAddress.append(",\n");
                            } else if (i < maxParts - 1) {
                                formattedAddress.append(", ");
                            }
                        }
                        addressText = formattedAddress.toString();

                        String city_nameText = rs.getString("city_name");
                        String pincodeText = rs.getString("pincode");
                        String formattedCityPincode = city_nameText + " - " + pincodeText;

                        // Format the details without an empty line after the address
                        String details = String.format(
                                "%s\n%s\n%s\n%s\n%s",
                                firmNameText,
                                contactPersonText,
                                mobileNumberText,
                                addressText.trim(),
                                formattedCityPincode
                        );

                        allCompanyDetails.add(details);
                    }

                    // After collecting all the details, update the UI to show them
                    Platform.runLater(() -> {
                        // Combine all the company details into a single string with additional newlines
                        String allDetailsText = String.join("\n\n", allCompanyDetails);  // Adds extra newline between records
                        companyDetailsArea.setText(allDetailsText); // Display all details in the TextArea
                    });

                    // Proceed to print the stickers after collecting all the details
                    StickerPrinter.printSticker(allCompanyDetails);

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        new Thread(task).start();
    }
    @FXML
    private void onRefreshButtonClick() {
        try {
            // Clear any displayed details in the VBox
            vbox.getChildren().clear();

            // Reset ComboBox selections to default values
            product_category_nameComboBox.setValue("Select Category"); // Reset category selection

            // Reset the city ComboBox to display "All city"
            city_nameComboBox.getItems().clear();
            city_nameComboBox.getItems().add("All city");
            city_nameComboBox.setValue("All city");

            // Clear displayed details for the selected company
            clearCityAndResetUI();

            onalldataButtonCLick();
            // Hide the progress indicator if it was visible
            progressIndicator.setVisible(false);

            System.out.println("Refresh button clicked - fields cleared and ComboBoxes reset.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}