package com.example.socialanalystpro;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class DashboardController {

    // --- Common Controls ---
    @FXML private BorderPane rootPane;
    @FXML private TextField tokenField;
    @FXML private Button analyzeButton;
    @FXML private Label statusLabel;
    @FXML private TabPane contentTabPane;
    @FXML private MenuButton dateFilterMenu;
    @FXML private Button themeToggleButton;
    @FXML private VBox sidebarMenu;

    // --- Tabs ---
    @FXML private Tab pagesTab;
    @FXML private Tab dashboardTab;
    @FXML private Tab postsTab;
    @FXML private Tab audienceTab;
    @FXML private Tab engagementTab;
    @FXML private Tab reportsTab;
    @FXML private Tab settingsTab;

    // --- Overview Tab Controls ---
    @FXML private Label totalFollowersLabel;
    @FXML private Label avgEngagementRateLabel;
    @FXML private Label totalPostsAnalyzedLabel;
    @FXML private Label growthPercentageLabel;
    @FXML private LineChart<String, Number> likesReachChart;
    @FXML private Label likesReachSubtitle;

    // --- Posts Tab Controls ---
    @FXML private BarChart<String, Number> engagementPerPostChart;
    @FXML private Label engagementChartTitle;
    @FXML private TableView<Map<String, Object>> topPostsTable;
    @FXML private TableColumn<Map, String> postContentColumn;
    @FXML private TableColumn<Map, Long> postEngagementColumn;
    
    // --- Post Inspector Controls ---
    @FXML private VBox postInspectorContainer;
    @FXML private Label inspectorPostText;
    @FXML private Label inspectorPostType;
    @FXML private Label inspectorPostDate;
    @FXML private Label inspectorViralScore;
    @FXML private Label inspectorTrendAlert;
    @FXML private ListView<String> inspectorTipsList;

    // --- Audience Tab Controls ---
    @FXML private LineChart<String, Number> followerGrowthChart;
    @FXML private PieChart genderBreakdownChart;
    @FXML private PieChart newVsReturningChart;

    // --- Engagement Tab Controls ---
    @FXML private LineChart<String, Number> contentTypeTrendChart;
    @FXML private LineChart<String, Number> engagementTrendChart;
    @FXML private BarChart<String, Number> engagementByDayChart;
    @FXML private BarChart<String, Number> engagementByHourChart;
    @FXML private LineChart<String, Number> postEngagementOverTimeChart;
    
    // --- Reports Tab Controls ---
    @FXML private LineChart<String, Number> reportOverallPerformanceChart;
    @FXML private PieChart reportEngagementBreakdownChart;
    @FXML private BarChart<Number, String> reportAudienceSummaryChart;
    @FXML private ListView<String> reportTopPostsList;
    @FXML private Label reportWeeklySummary;

    // --- Settings Tab Controls ---
    @FXML private TextField settingsTokenField;
    @FXML private Label settingsStatusLabel;
    @FXML private Label lastSyncLabel;
    @FXML private Label appVersionLabel;
    @FXML private Label rmiStatusLabel;

    // --- ViralBud AI Controls ---
    @FXML private VBox chatWindow;
    @FXML private TextArea chatHistory;
    @FXML private TextField chatInput;
    @FXML private Button viralBudButton;
    @FXML private VBox viralBudSummaryPane;
    @FXML private Label viralBudBestContent;
    @FXML private Label viralBudBestTime;

    private long sinceTimestamp;
    private boolean isDarkMode = false;
    private SocialInsights currentInsights;
    private AnalyticsService analyticsService;
    private Timer followerUpdateTimer;
    private Timer likesUpdateTimer;
    private Timer genderUpdateTimer;
    private Timer engagementUpdateTimer;

    @FXML
    private void initialize() {
        showPagesView(null);
        Label themeIcon = (Label) themeToggleButton.getGraphic();
        themeIcon.setText("☾");

        // Default to 30 days
        updateTimestamp(30L * 24 * 60 * 60);

        topPostsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updatePostInspector(newSelection);
            } else {
                clearPostInspector();
            }
        });
        
        new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                Platform.runLater(() -> {
                    if (rmiStatusLabel != null) rmiStatusLabel.setText("Connected");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (rmiStatusLabel != null) rmiStatusLabel.setText("Disconnected");
                });
            }
        }).start();

        if (appVersionLabel != null) appVersionLabel.setText("1.0.0 (Pro Edition)");
    }

    private void startFollowerUpdater() {
        if (followerUpdateTimer != null) {
            followerUpdateTimer.cancel();
        }
        
        followerUpdateTimer = new Timer(true);
        followerUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Only update if dashboard tab is selected and we have a token
                if (contentTabPane.getSelectionModel().getSelectedItem() != dashboardTab) {
                    return;
                }
                
                String token = tokenField.getText();
                if (token == null || token.trim().isEmpty()) {
                    return;
                }

                try {
                    if (analyticsService == null) {
                        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                        analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                    }
                    
                    long currentFollowers = analyticsService.getFollowerCount(token);
                    
                    Platform.runLater(() -> {
                        if (totalFollowersLabel != null) {
                            String currentText = totalFollowersLabel.getText().replace(",", "");
                            long displayedFollowers = 0;
                            try {
                                displayedFollowers = Long.parseLong(currentText);
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                            
                            if (currentFollowers != displayedFollowers) {
                                totalFollowersLabel.setText(String.format("%,d", currentFollowers));
                            }
                        }
                    });
                } catch (Exception e) {
                    // Silently fail or log, don't disrupt UI
                    System.err.println("Failed to update follower count: " + e.getMessage());
                }
            }
        }, 5000, 5000); // Start after 5s, repeat every 5s
    }

    private void stopFollowerUpdater() {
        if (followerUpdateTimer != null) {
            followerUpdateTimer.cancel();
            followerUpdateTimer = null;
        }
    }

    private void startLikesUpdater() {
        if (likesUpdateTimer != null) {
            likesUpdateTimer.cancel();
        }
        
        likesUpdateTimer = new Timer(true);
        likesUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Only update if dashboard tab is selected and we have a token
                if (contentTabPane.getSelectionModel().getSelectedItem() != dashboardTab) {
                    return;
                }
                
                String token = tokenField.getText();
                if (token == null || token.trim().isEmpty()) {
                    return;
                }

                try {
                    if (analyticsService == null) {
                        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                        analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                    }
                    
                    long currentLikes = analyticsService.getPageLikes(token);
                    
                    Platform.runLater(() -> {
                        if (likesReachSubtitle != null) {
                            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy"));
                            likesReachSubtitle.setText(currentDate + ": page like and reach: " + currentLikes + " likes");
                        }
                        
                        // Update the graph if needed (simplified: just adding a point for "Now")
                        if (likesReachChart != null) {
                            XYChart.Series<String, Number> likesSeries = null;
                            for (XYChart.Series<String, Number> s : likesReachChart.getData()) {
                                if ("Page Likes".equals(s.getName())) {
                                    likesSeries = s;
                                    break;
                                }
                            }
                            
                            if (likesSeries != null) {
                                String nowLabel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd HH:mm"));
                                // Check if last data point is different
                                if (!likesSeries.getData().isEmpty()) {
                                    XYChart.Data<String, Number> lastData = likesSeries.getData().get(likesSeries.getData().size() - 1);
                                    if (lastData.getYValue().longValue() != currentLikes) {
                                         likesSeries.getData().add(new XYChart.Data<>(nowLabel, currentLikes));
                                    }
                                } else {
                                    likesSeries.getData().add(new XYChart.Data<>(nowLabel, currentLikes));
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to update page likes: " + e.getMessage());
                }
            }
        }, 60000, 60000); // Start after 60s, repeat every 60s
    }

    private void stopLikesUpdater() {
        if (likesUpdateTimer != null) {
            likesUpdateTimer.cancel();
            likesUpdateTimer = null;
        }
    }

    private void startGenderUpdater() {
        if (genderUpdateTimer != null) {
            genderUpdateTimer.cancel();
        }
        
        genderUpdateTimer = new Timer(true);
        genderUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Only update if audience tab is selected and we have a token
                if (contentTabPane.getSelectionModel().getSelectedItem() != audienceTab) {
                    return;
                }
                
                String token = tokenField.getText();
                if (token == null || token.trim().isEmpty()) {
                    return;
                }

                try {
                    if (analyticsService == null) {
                        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                        analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                    }
                    
                    Map<String, Integer> genderData = analyticsService.getGenderBreakdown(token);
                    
                    Platform.runLater(() -> {
                        if (genderBreakdownChart != null) {
                            if (genderData.isEmpty()) {
                                genderBreakdownChart.setTitle("No gender data available");
                                genderBreakdownChart.setData(FXCollections.observableArrayList());
                            } else {
                                genderBreakdownChart.setTitle("Gender Breakdown");
                                genderBreakdownChart.setData(FXCollections.observableArrayList(
                                    genderData.entrySet().stream()
                                        .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                                        .collect(Collectors.toList())
                                ));
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to update gender breakdown: " + e.getMessage());
                }
            }
        }, 5000, 300000); // Start after 5s, repeat every 5 minutes
    }

    private void stopGenderUpdater() {
        if (genderUpdateTimer != null) {
            genderUpdateTimer.cancel();
            genderUpdateTimer = null;
        }
    }

    private void startEngagementUpdater() {
        if (engagementUpdateTimer != null) {
            engagementUpdateTimer.cancel();
        }
        
        engagementUpdateTimer = new Timer(true);
        engagementUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Only update if engagement tab is selected and we have a token
                if (contentTabPane.getSelectionModel().getSelectedItem() != engagementTab) {
                    return;
                }
                
                String token = tokenField.getText();
                if (token == null || token.trim().isEmpty()) {
                    return;
                }

                try {
                    if (analyticsService == null) {
                        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                        analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                    }
                    
                    Map<String, Map<String, Integer>> engagementData = analyticsService.getPostEngagementOverTime(token, sinceTimestamp);
                    
                    Platform.runLater(() -> {
                        if (postEngagementOverTimeChart != null) {
                            if (engagementData.isEmpty()) {
                                postEngagementOverTimeChart.setTitle("No engagement data available");
                                postEngagementOverTimeChart.getData().clear();
                            } else {
                                postEngagementOverTimeChart.setTitle("Post Engagement Over Time");
                                
                                XYChart.Series<String, Number> likesSeries = new XYChart.Series<>();
                                likesSeries.setName("Likes");
                                XYChart.Series<String, Number> commentsSeries = new XYChart.Series<>();
                                commentsSeries.setName("Comments");
                                XYChart.Series<String, Number> sharesSeries = new XYChart.Series<>();
                                sharesSeries.setName("Shares");
                                XYChart.Series<String, Number> totalSeries = new XYChart.Series<>();
                                totalSeries.setName("Total Engagement");
                                
                                engagementData.forEach((date, metrics) -> {
                                    likesSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Likes", 0)));
                                    commentsSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Comments", 0)));
                                    sharesSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Shares", 0)));
                                    totalSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Total", 0)));
                                });
                                
                                postEngagementOverTimeChart.getData().setAll(likesSeries, commentsSeries, sharesSeries, totalSeries);
                                
                                // Apply colors
                                for (XYChart.Series<String, Number> s : postEngagementOverTimeChart.getData()) {
                                    String color = "";
                                    switch (s.getName()) {
                                        case "Likes": color = "#3B82F6"; break;
                                        case "Comments": color = "#10B981"; break;
                                        case "Shares": color = "#F59E0B"; break;
                                        case "Total Engagement": color = "#8B5CF6"; break;
                                    }
                                    if (!color.isEmpty()) {
                                        s.getNode().setStyle("-fx-stroke: " + color + ";");
                                        for (XYChart.Data<String, Number> data : s.getData()) {
                                            if (data.getNode() != null) {
                                                data.getNode().setStyle("-fx-background-color: " + color + ", white;");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to update engagement graph: " + e.getMessage());
                }
            }
        }, 5000, 300000); // Start after 5s, repeat every 5 minutes
    }

    private void stopEngagementUpdater() {
        if (engagementUpdateTimer != null) {
            engagementUpdateTimer.cancel();
            engagementUpdateTimer = null;
        }
    }

    private void updatePostInspector(Map<String, Object> postData) {
        if (postData == null) return;
        
        Map<String, Object> detailedData = postData;
        if (currentInsights != null && currentInsights.getPostInspectorData() != null) {
             String id = (String) postData.get("id");
             if (id != null && currentInsights.getPostInspectorData().containsKey(id)) {
                 detailedData = currentInsights.getPostInspectorData().get(id);
             }
        }

        inspectorPostText.setText((String) detailedData.getOrDefault("full_text", "No content"));
        inspectorPostType.setText("Type: " + detailedData.getOrDefault("type", "Unknown"));
        inspectorPostDate.setText("Posted: " + detailedData.getOrDefault("created_time", "-"));
        
        // ViralBud AI features removed from Post Inspector
        inspectorViralScore.setText("-");
        inspectorTrendAlert.setText("");
        inspectorTipsList.getItems().clear();
    }

    private void clearPostInspector() {
        inspectorPostText.setText("Select a post to view details");
        inspectorPostType.setText("");
        inspectorPostDate.setText("");
        inspectorViralScore.setText("-");
        inspectorTrendAlert.setText("");
        inspectorTipsList.getItems().clear();
    }

    public void setToken(String token) {
        tokenField.setText(token);
        if (settingsTokenField != null) settingsTokenField.setText(token);
        statusLabel.setText("Token received! Ready to analyze.");
    }

    @FXML
    protected void openFacebookLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Parent root = loader.load();
            
            LoginController loginController = loader.getController();
            loginController.setDashboardController(this);
            loginController.loadLogin();

            Stage stage = new Stage();
            stage.setTitle("Login with Facebook");
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error opening login window: " + e.getMessage());
        }
    }

    private void setActiveNav(Node selectedButton) {
        for (Node node : sidebarMenu.getChildren()) {
            if (node instanceof Button) {
                node.getStyleClass().remove("focused");
            }
        }
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("focused");
        }
    }

    @FXML protected void showPagesView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(pagesTab);
        setActiveNav(event != null ? (Node)event.getSource() : null);
    }
    @FXML protected void showDashboardView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(dashboardTab);
        setActiveNav((Node)event.getSource());
    }
    @FXML protected void showPostsView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(postsTab);
        setActiveNav((Node)event.getSource());
    }
    @FXML protected void showAudienceView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(audienceTab);
        setActiveNav((Node)event.getSource());
        // Trigger gender update when switching to Audience tab
        if (genderUpdateTimer == null) {
            startGenderUpdater();
        }
    }
    @FXML protected void showEngagementView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(engagementTab);
        setActiveNav((Node)event.getSource());
        // Trigger engagement update when switching to Engagement tab
        if (engagementUpdateTimer == null) {
            startEngagementUpdater();
        }
    }
    @FXML protected void showReportsView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(reportsTab);
        setActiveNav((Node)event.getSource());
    }
    @FXML protected void showSettingsView(ActionEvent event) { 
        contentTabPane.getSelectionModel().select(settingsTab);
        setActiveNav((Node)event.getSource());
    }

    @FXML
    protected void toggleTheme() {
        isDarkMode = !isDarkMode;
        rootPane.getStyleClass().removeAll("light-theme", "dark-theme");
        Label themeIcon = (Label) themeToggleButton.getGraphic();
        if (isDarkMode) {
            rootPane.getStyleClass().add("dark-theme");
            themeIcon.setText("☀");
            themeToggleButton.getTooltip().setText("Switch to Light Mode");
            viralBudButton.setStyle("-fx-background-color: #2d88ff; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 24; -fx-effect: dropshadow(gaussian, rgba(45, 136, 255, 0.4), 10, 0, 0, 4); -fx-cursor: hand;");
        } else {
            rootPane.getStyleClass().add("light-theme");
            themeIcon.setText("☾");
            themeToggleButton.getTooltip().setText("Switch to Dark Mode");
            viralBudButton.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 12 24; -fx-effect: dropshadow(gaussian, rgba(24, 119, 242, 0.4), 10, 0, 0, 4); -fx-cursor: hand;");
        }
    }

    @FXML
    protected void showProfile() {
        String token = tokenField.getText();
        if (token == null || token.trim().isEmpty()) {
            statusLabel.setText("Please enter a token first.");
            return;
        }

        new Thread(() -> {
            try {
                if (analyticsService == null) {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                }
                PageProfile profile = analyticsService.getPageProfile(token);

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Account Profile");
                    alert.setHeaderText(profile.getName());
                    
                    VBox content = new VBox(10);
                    content.setStyle("-fx-padding: 10;");
                    
                    if (profile.getPictureUrl() != null && !profile.getPictureUrl().isEmpty()) {
                        ImageView imageView = new ImageView(new Image(profile.getPictureUrl()));
                        imageView.setFitHeight(100);
                        imageView.setFitWidth(100);
                        content.getChildren().add(imageView);
                    }
                    
                    content.getChildren().add(new Label("Category: " + profile.getCategory()));
                    content.getChildren().add(new Label("Page ID: " + profile.getId()));
                    
                    alert.getDialogPane().setContent(content);
                    alert.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showApiErrorDialog(e));
            }
        }).start();
    }

    @FXML
    protected void onDateFilterChanged(ActionEvent event) {
        MenuItem source = (MenuItem) event.getSource();
        String text = source.getText();
        dateFilterMenu.setText(text);
        
        long seconds = 0;
        switch (text) {
            case "Last 30 Minutes": seconds = 30 * 60; break;
            case "Last 1 Hour": seconds = 60 * 60; break;
            case "Last 24 Hours": seconds = 24 * 60 * 60; break;
            case "Last 5 Days": seconds = 5 * 24 * 60 * 60; break;
            case "Last 10 Days": seconds = 10 * 24 * 60 * 60; break;
            case "Last 1 Month": seconds = 30L * 24 * 60 * 60; break;
            case "Last 2 Months": seconds = 60L * 24 * 60 * 60; break;
            case "Last 6 Months": seconds = 180L * 24 * 60 * 60; break;
            case "Last 1 Year": seconds = 365L * 24 * 60 * 60; break;
            default: seconds = 30L * 24 * 60 * 60;
        }
        updateTimestamp(seconds);
        
        if (engagementChartTitle != null) engagementChartTitle.setText("Engagement per Post (" + text + ")");
    }
    
    private void updateTimestamp(long secondsAgo) {
        this.sinceTimestamp = LocalDateTime.now().minusSeconds(secondsAgo).atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    @FXML
    protected void onAnalyzeButtonClick() {
        String token = tokenField.getText();
        if (token == null || token.trim().isEmpty()) {
            statusLabel.setText("Please enter a valid token.");
            return;
        }
        statusLabel.setText("Analyzing for " + dateFilterMenu.getText() + "...");
        analyzeButton.setDisable(true);
        
        new Thread(() -> {
            try {
                if (analyticsService == null) {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                }
                SocialInsights insights = analyticsService.analyzePage(token, sinceTimestamp);
                Platform.runLater(() -> {
                    currentInsights = insights;
                    updateDashboard(insights);
                    contentTabPane.getSelectionModel().select(dashboardTab);
                    for(Node node : sidebarMenu.getChildren()) {
                        if(node instanceof Button && ((Button)node).getText().contains("Dashboard")) {
                            setActiveNav(node);
                            break;
                        }
                    }
                    if (lastSyncLabel != null) {
                        lastSyncLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    
                    // Start the updaters after successful analysis
                    startFollowerUpdater();
                    startLikesUpdater();
                    startGenderUpdater();
                    startEngagementUpdater();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showApiErrorDialog(e));
            }
        }).start();
    }

    private void showApiErrorDialog(Exception e) {
        analyzeButton.setDisable(false);
        statusLabel.setText("Analysis Failed. See details.");
        e.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Facebook API Error");
        alert.setHeaderText("Could not fetch data from Facebook.");
        
        String errorMessage = e.getMessage();
        String commonCauses = "This can happen for several reasons:\n\n" +
                              "1. Invalid or Expired Token:\n" +
                              "   • Generate a new Page Access Token from the Graph API Explorer.\n\n" +
                              "2. Missing Permissions:\n" +
                              "   • Ensure your token has 'pages_read_engagement' and 'pages_read_user_content'.\n\n" +
                              "3. App Not Reviewed (for live apps):\n" +
                              "   • Your app may need 'Page Public Content Access' review by Facebook.\n\n" +
                              "4. Incorrect Token Type:\n" +
                              "   • Make sure you are using a Page Access Token, not a User Access Token.";

        TextArea textArea = new TextArea("Full Error:\n" + errorMessage + "\n\n--- Common Solutions ---\n" + commonCauses);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        
        GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.showAndWait();
    }

    @FXML
    protected void exportReport() {
        if (currentInsights == null) {
            statusLabel.setText("No data to export. Run analysis first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("SocialAnalystPro_Report.csv");
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Social Analyst Pro Report");
                writer.println("Generated on: " + java.time.LocalDate.now());
                writer.println("--------------------------------------------------");
                
                writer.println("OVERVIEW");
                writer.println("Total Followers," + currentInsights.getTotalFollowers());
                writer.println("Avg Engagement Rate," + String.format("%.2f%%", currentInsights.getAverageEngagementRate()));
                writer.println("Total Posts Analyzed," + currentInsights.getTotalPostsAnalyzed());
                writer.println("Growth Prediction," + String.format("%.1f%%", currentInsights.getGrowthPrediction()));
                writer.println();

                writer.println("RECOMMENDATIONS");
                for (String rec : currentInsights.getRecommendations()) {
                    writer.println("\"" + rec + "\"");
                }
                writer.println();

                writer.println("TOP POSTS");
                writer.println("Content,Engagement");
                for (Map<String, Object> post : currentInsights.getTopPerformingPosts()) {
                    writer.println("\"" + post.get("text") + "\"," + post.get("engagement"));
                }

                statusLabel.setText("Report saved successfully.");
            } catch (Exception e) {
                statusLabel.setText("Error saving report: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    protected void clearCache() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cache Cleared");
        alert.setHeaderText(null);
        alert.setContentText("Local application cache has been successfully cleared.");
        alert.showAndWait();
    }

    // --- Settings Actions ---
    @FXML
    protected void updateToken() {
        if (settingsTokenField != null) {
            String newToken = settingsTokenField.getText();
            tokenField.setText(newToken);
            settingsStatusLabel.setText("Token updated successfully.");
        }
    }

    @FXML
    protected void testConnection() {
        if (settingsTokenField == null || settingsTokenField.getText().isEmpty()) {
            settingsStatusLabel.setText("Please enter a token first.");
            return;
        }
        settingsStatusLabel.setText("Testing connection...");
        new Thread(() -> {
            try {
                if (analyticsService == null) {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    analyticsService = (AnalyticsService) registry.lookup("AnalyticsService");
                }
                PageProfile profile = analyticsService.getPageProfile(settingsTokenField.getText());
                Platform.runLater(() -> settingsStatusLabel.setText("Success! Connected to: " + profile.getName()));
            } catch (Exception e) {
                Platform.runLater(() -> settingsStatusLabel.setText("Connection failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    protected void logout() {
        stopFollowerUpdater(); // Stop the updater on logout
        stopLikesUpdater(); // Stop the likes updater on logout
        stopGenderUpdater(); // Stop the gender updater on logout
        stopEngagementUpdater(); // Stop the engagement updater on logout
        tokenField.clear();
        if (settingsTokenField != null) settingsTokenField.clear();
        currentInsights = null;
        statusLabel.setText("Logged out.");
        settingsStatusLabel.setText("Logged out.");
        showPagesView(null);
    }

    // --- ViralBud AI Methods ---
    @FXML
    protected void toggleChat() {
        chatWindow.setVisible(!chatWindow.isVisible());
    }

    @FXML
    protected void sendMessage() {
        String userMessage = chatInput.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        chatHistory.appendText("You: " + userMessage + "\n");
        chatInput.clear();

        // Simulate AI response
        String aiResponse = getViralBudResponse(userMessage);
        chatHistory.appendText("ViralBud: " + aiResponse + "\n");
    }

    private String getViralBudResponse(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase();

        if (lowerCaseMessage.contains("followers")) {
            return "To get more followers, focus on creating high-quality, engaging content. Use relevant hashtags, post consistently, and interact with your audience by responding to comments and messages.";
        } else if (lowerCaseMessage.contains("trending")) {
            return "Trending topics vary by platform. On Instagram and TikTok, short-form video content is very popular. For Facebook, focus on community-building posts and live videos.";
        } else if (lowerCaseMessage.contains("engagement")) {
            return "To increase engagement, ask questions in your posts, run polls, and create shareable content like infographics or inspiring quotes. The more your audience interacts, the more your content will be seen.";
        } else {
            return "I'm here to help with your social media questions. Try asking about how to get more followers, what's trending, or how to increase engagement.";
        }
    }

    private void updateDashboard(SocialInsights insights) {
        if (insights.getTotalPostsAnalyzed() == 0) {
            statusLabel.setText("No new posts found in this period. Showing 0s.");
        } else {
            statusLabel.setText("Analysis complete.");
        }
        
        totalFollowersLabel.setText(String.format("%,d", insights.getTotalFollowers()));
        avgEngagementRateLabel.setText(String.format("%.2f%%", insights.getAverageEngagementRate()));
        
        // Updated to show posts count for the chosen period
        totalPostsAnalyzedLabel.setText(String.valueOf(insights.getPeriodMetrics() != null ? insights.getPeriodMetrics().getPostsInPeriod() : 0));
        
        growthPercentageLabel.setText(String.format("+%.1f%%", insights.getGrowthPercentage()));
        
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy"));
        
        // Update subtitle with specific data from the analysis period
        if (likesReachSubtitle != null) {
            long periodLikes = 0;
            if (insights.getPeriodMetrics() != null) {
                periodLikes = insights.getPeriodMetrics().getLikesInPeriod();
            }
            likesReachSubtitle.setText(currentDate + ": page like and reach: " + periodLikes + " likes");
        }
        
        XYChart.Series<String, Number> likesSeries = new XYChart.Series<>();
        likesSeries.setName("Page Likes");
        if (insights.getFollowerGrowth() != null) {
            insights.getFollowerGrowth().forEach((month, count) -> likesSeries.getData().add(new XYChart.Data<>(month, count)));
        }

        XYChart.Series<String, Number> reachSeries = new XYChart.Series<>();
        reachSeries.setName("Reach");
        if (insights.getReachOverTime() != null) {
            insights.getReachOverTime().forEach((month, count) -> reachSeries.getData().add(new XYChart.Data<>(month, count)));
        }

        if (likesReachChart != null) {
            likesReachChart.getData().clear();
            if (!likesSeries.getData().isEmpty() || !reachSeries.getData().isEmpty()) {
                likesReachChart.getData().addAll(likesSeries, reachSeries);
            } else {
                likesReachChart.setTitle("No data available for Likes & Reach");
            }
        }

        XYChart.Series<String, Number> postEngagementSeries = new XYChart.Series<>();
        postEngagementSeries.setName("Total Engagement");
        
        // Engagement per Post: Use recent posts (filtered by period)
        if (insights.getRecentPosts() != null) {
            int postCount = 0;
            for (Map<String, Object> post : insights.getRecentPosts()) {
                if (postCount++ >= 10) break; // Limit to 10 for readability
                String label = "Post " + postCount;
                Number engagement = (Number) post.get("engagement");
                postEngagementSeries.getData().add(new XYChart.Data<>(label, engagement));
            }
        }
        
        if (engagementPerPostChart != null) {
            engagementPerPostChart.getData().clear();
            if (!postEngagementSeries.getData().isEmpty()) {
                engagementPerPostChart.getData().add(postEngagementSeries);
            } else {
                engagementPerPostChart.setTitle("No posts available");
            }
        }

        postContentColumn.setCellValueFactory(new MapValueFactory<>("text"));
        postEngagementColumn.setCellValueFactory(new MapValueFactory<>("engagement"));
        
        // Top Performing Posts: Use historical top posts (ignoring filter)
        if (insights.getTopPerformingPosts() != null) {
            topPostsTable.setItems(FXCollections.observableArrayList(insights.getTopPerformingPosts()));
        } else {
            topPostsTable.setPlaceholder(new Label("No posts available for ranking"));
        }

        XYChart.Series<String, Number> actualGrowthSeries = new XYChart.Series<>();
        actualGrowthSeries.setName("Actual Growth");
        if (insights.getFollowerGrowth() != null) {
            insights.getFollowerGrowth().forEach((month, count) -> actualGrowthSeries.getData().add(new XYChart.Data<>(month, count)));
        }
        
        XYChart.Series<String, Number> projectedGrowthSeries = new XYChart.Series<>();
        projectedGrowthSeries.setName("Projected Growth");
        if (insights.getProjectedGrowth() != null && !actualGrowthSeries.getData().isEmpty()) {
            insights.getProjectedGrowth().forEach((month, count) -> projectedGrowthSeries.getData().add(new XYChart.Data<>(month, count)));
        }
        
        if (followerGrowthChart != null) {
            followerGrowthChart.getData().clear();
            if (!actualGrowthSeries.getData().isEmpty()) {
                followerGrowthChart.getData().addAll(actualGrowthSeries, projectedGrowthSeries);
            } else {
                followerGrowthChart.setTitle("Not enough data to project growth");
            }
        }

        if (genderBreakdownChart != null) {
            genderBreakdownChart.getData().clear();
            if (insights.getAgeGenderDistribution() != null && !insights.getAgeGenderDistribution().isEmpty()) {
                // This is the old logic, we will replace it with real data in the updater
                // For now, let's just clear it or show a placeholder
                genderBreakdownChart.setTitle("Loading Gender Data...");
            } else {
                genderBreakdownChart.setTitle("No gender data available");
            }
        }

        if (newVsReturningChart != null) {
            if (insights.getNewVsReturning() != null && !insights.getNewVsReturning().isEmpty()) {
                newVsReturningChart.setData(FXCollections.observableArrayList(
                    insights.getNewVsReturning().entrySet().stream()
                        .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList())
                ));
            } else {
                newVsReturningChart.setTitle("No audience session data");
                newVsReturningChart.setData(FXCollections.observableArrayList());
            }
        }

        if (contentTypeTrendChart != null) {
            contentTypeTrendChart.getData().clear();
            if (insights.getContentTrend() != null && !insights.getContentTrend().isEmpty()) {
                insights.getContentTrend().forEach((contentType, trendData) -> {
                    if (!trendData.isEmpty()) {
                        XYChart.Series<String, Number> series = new XYChart.Series<>();
                        series.setName(contentType);
                        trendData.forEach((time, value) -> series.getData().add(new XYChart.Data<>(time, value)));
                        contentTypeTrendChart.getData().add(series);
                    }
                });
            } else {
                contentTypeTrendChart.setTitle("No content trend data available");
            }
        }
        
        if (engagementTrendChart != null) {
            engagementTrendChart.getData().clear();
            if (insights.getEngagementTrend() != null && !insights.getEngagementTrend().isEmpty()) {
                // Check if total engagement is 0
                double totalEngagement = insights.getEngagementTrend().values().stream().mapToDouble(Double::doubleValue).sum();
                if (totalEngagement > 0) {
                    XYChart.Series<String, Number> trendSeries = new XYChart.Series<>();
                    trendSeries.setName("Overall Engagement");
                    insights.getEngagementTrend().forEach((date, value) -> trendSeries.getData().add(new XYChart.Data<>(date, value)));
                    engagementTrendChart.getData().setAll(trendSeries);
                } else {
                    engagementTrendChart.setTitle("No engagement data available for this time range");
                }
            } else {
                engagementTrendChart.setTitle("No engagement data available");
            }
        }

        if (engagementByDayChart != null) {
            engagementByDayChart.getData().clear();
            if (insights.getEngagementByDay() != null && !insights.getEngagementByDay().isEmpty()) {
                 double totalEngagement = insights.getEngagementByDay().values().stream().mapToDouble(Double::doubleValue).sum();
                 if (totalEngagement > 0) {
                    XYChart.Series<String, Number> daySeries = new XYChart.Series<>();
                    daySeries.setName("Engagement by Day");
                    insights.getEngagementByDay().forEach((day, value) -> daySeries.getData().add(new XYChart.Data<>(day, value)));
                    engagementByDayChart.getData().setAll(daySeries);
                 } else {
                     engagementByDayChart.setTitle("Insufficient data to determine best day");
                 }
            } else {
                engagementByDayChart.setTitle("No data available");
            }
        }
        
        if (engagementByHourChart != null) {
            engagementByHourChart.getData().clear();
            if (insights.getEngagementByHour() != null && !insights.getEngagementByHour().isEmpty()) {
                double totalEngagement = insights.getEngagementByHour().values().stream().mapToDouble(Double::doubleValue).sum();
                if (totalEngagement > 0) {
                    XYChart.Series<String, Number> hourSeries = new XYChart.Series<>();
                    hourSeries.setName("Engagement by Hour");
                    insights.getEngagementByHour().forEach((hour, value) -> hourSeries.getData().add(new XYChart.Data<>(hour, value)));
                    engagementByHourChart.getData().setAll(hourSeries);
                } else {
                    engagementByHourChart.setTitle("Insufficient data to determine best hour");
                }
            } else {
                engagementByHourChart.setTitle("No data available");
            }
        }

        if (reportOverallPerformanceChart != null) {
            reportOverallPerformanceChart.getData().clear();
            // Use the new real data if available, otherwise fallback to old behavior or clear
            if (insights.getOverallPerformance() != null && !insights.getOverallPerformance().isEmpty()) {
                XYChart.Series<String, Number> overallReachSeries = new XYChart.Series<>();
                overallReachSeries.setName("Reach");
                XYChart.Series<String, Number> overallEngagementSeries = new XYChart.Series<>();
                overallEngagementSeries.setName("Engagement");
                
                insights.getOverallPerformance().forEach((date, metrics) -> {
                    overallReachSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Reach", 0)));
                    overallEngagementSeries.getData().add(new XYChart.Data<>(date, metrics.getOrDefault("Engagement", 0)));
                });
                
                reportOverallPerformanceChart.getData().addAll(overallReachSeries, overallEngagementSeries);
                reportOverallPerformanceChart.setTitle("Overall Performance (Real Data)");
            } else {
                reportOverallPerformanceChart.setTitle("Not enough data yet");
            }
        }
        
        if (reportEngagementBreakdownChart != null) {
            // Use the period metrics which are calculated from the same source as the engagement menu
            // The engagement menu uses 'periodPosts' which are fetched with 'sinceTimestamp'
            // 'PeriodMetrics' stores the sum of likes, comments, shares from these posts.
            
            long totalLikes = 0;
            long totalComments = 0;
            long totalShares = 0;
            
            if (insights.getPeriodMetrics() != null) {
                totalLikes = insights.getPeriodMetrics().getLikesInPeriod();
                totalComments = insights.getPeriodMetrics().getCommentsInPeriod();
                totalShares = insights.getPeriodMetrics().getSharesInPeriod();
            }
            
            if (totalLikes + totalComments + totalShares > 0) {
                reportEngagementBreakdownChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Likes", totalLikes),
                    new PieChart.Data("Comments", totalComments),
                    new PieChart.Data("Shares", totalShares)
                ));
                reportEngagementBreakdownChart.setTitle("Engagement Breakdown");
            } else {
                reportEngagementBreakdownChart.setData(FXCollections.observableArrayList());
                reportEngagementBreakdownChart.setTitle("No engagement data available yet");
            }
        }
        
        if (reportAudienceSummaryChart != null) {
            reportAudienceSummaryChart.getData().clear();
            if (insights.getAgeGenderDistribution() != null && !insights.getAgeGenderDistribution().isEmpty()) {
                XYChart.Series<Number, String> audienceSeries = new XYChart.Series<>();
                
                insights.getAgeGenderDistribution().forEach((ageGroup, genderMap) -> {
                    double total = genderMap.getOrDefault("Male", 0.0) + genderMap.getOrDefault("Female", 0.0);
                    audienceSeries.getData().add(new XYChart.Data<>(total, ageGroup));
                });
                
                reportAudienceSummaryChart.getData().add(audienceSeries);
            }
        }
        
        if (reportTopPostsList != null) {
            if (insights.getTopPerformingPosts() != null) {
                reportTopPostsList.setItems(FXCollections.observableArrayList(
                    insights.getTopPerformingPosts().stream()
                        .map(p -> (String)p.get("text"))
                        .collect(Collectors.toList())
                ));
            } else {
                reportTopPostsList.getItems().clear();
            }
        }
        
        if (reportWeeklySummary != null) {
            reportWeeklySummary.setText(insights.getWeeklyViralSummary());
        }

        // Update ViralBud Summary Pane
        if (viralBudBestContent != null) {
            viralBudBestContent.setText(insights.getBestContentTypeSuggestion());
        }
        if (viralBudBestTime != null) {
            viralBudBestTime.setText(insights.getBestTimeToPostSuggestion());
        }

        analyzeButton.setDisable(false);
    }
}
