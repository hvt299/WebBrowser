import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleMiniWebBrowser extends Application {

    private TextField urlField, zoomField;
    private TextArea resultArea;
    private ComboBox<String> methodBox;
    private WebView webView;
    private WebEngine webEngine;
    private Button backButton, nextButton, refreshButton, goButton, zoomInButton, zoomOutButton;

    @Override
    public void start(Stage stage) {
        // Tạo các thành phần giao diện người dùng
        urlField = new TextField("https://www.example.com");
        zoomField = new TextField();
        resultArea = new TextArea();
        methodBox = new ComboBox<>();
        methodBox.getItems().addAll("GET", "POST", "HEAD");
        methodBox.setValue("GET");

        webView = new WebView();
        webEngine = webView.getEngine();
        webView.setZoom(1.0);
        zoomField.setEditable(false);
        zoomField.setText(String.format("%.0f%%", webView.getZoom() * 100));

        // Nút Back
        backButton = new Button("Back");
        backButton.setOnAction(e -> handleBack());

        // Nút Next
        nextButton = new Button("Next");
        nextButton.setOnAction(e -> handleNext());

        // Nút Refresh
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> handleRefresh());

        // Nút Go
        goButton = new Button("Go");
        goButton.setOnAction(e -> handleRequest());
        
        zoomInButton = new Button("Zoom In");
        zoomInButton.setOnAction(event -> {
            if (webView.getZoom() < 3.0) { // Giới hạn zoom tối đa là 300%
                webView.setZoom(webView.getZoom() + 0.1);
                zoomField.setText(String.format("%.0f%%", webView.getZoom() * 100));
            }
        });
        zoomOutButton = new Button("Zoom Out");
        zoomOutButton.setOnAction(event -> {
            if (webView.getZoom() > 0.6) { // Giới hạn zoom tối thiểu là 50%
                webView.setZoom(webView.getZoom() - 0.1);
                zoomField.setText(String.format("%.0f%%", webView.getZoom() * 100));
            }
        });

        // Tạo thanh công cụ điều khiển
        ToolBar toolBar = new ToolBar(backButton, nextButton, refreshButton, new Label("URL:"), urlField, methodBox, goButton, zoomField, zoomInButton, zoomOutButton);

        // Sắp xếp giao diện
        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(webView);
        root.setBottom(resultArea);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Simple Mini Web Browser");
        stage.show();

        // Theo dõi thay đổi trong lịch sử duyệt web để cập nhật trạng thái của các nút
        webEngine.getHistory().currentIndexProperty().addListener((obs, oldValue, newValue) -> updateNavigationButtons());

        // Lắng nghe thay đổi URL để cập nhật thanh URL mỗi khi trang mới được tải
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                urlField.setText(newValue); // Cập nhật URL trong thanh địa chỉ
            }
        });
    }

    // Xử lý khi nhấn nút Go
    private void handleRequest() {
        String url = urlField.getText();
        String method = methodBox.getValue();

        if (url.isEmpty()) {
            resultArea.setText("Please enter a valid URL.");
            return;
        }

        // Kiểm tra nếu URL có http:// hoặc https://, nếu không có thì thêm vào
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        // Xử lý theo các loại request GET, POST, HEAD
        switch (method) {
            case "GET":
            	sendGetRequest(url);
                webEngine.load(url);  // Gửi yêu cầu GET và tải trang
                break;
            case "POST":
                sendPostRequest(url);
                break;
            case "HEAD":
                sendHeadRequest(url);
                break;
        }
    }
    
    // Hàm đếm số lượng thẻ HTML
    private int countTags(String html, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + "\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    // Gửi yêu cầu GET
    private void sendGetRequest(String url) {
    	try {
    		 // Tạo HttpClient
            HttpClient client = HttpClient.newHttpClient();
            URI uri = URI.create(url);

            // Tạo HttpRequest
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(uri);
            HttpRequest request = requestBuilder.build();

            // Gửi yêu cầu GET
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Đếm số lượng thẻ
            int pCount = countTags(responseBody, "p");
            int divCount = countTags(responseBody, "div");
            int spanCount = countTags(responseBody, "span");
            int imgCount = countTags(responseBody, "img");

            // Hiển thị kết quả phân tích
            resultArea.setText("Response Length: " + responseBody.length() + "\n" +
                    "Number of <p> tags: " + pCount + "\n" +
                    "Number of <div> tags: " + divCount + "\n" +
                    "Number of <span> tags: " + spanCount + "\n" +
                    "Number of <img> tags: " + imgCount + "\n");

            // Hiển thị trang HTML trong WebView
            webEngine.loadContent(responseBody);
    	} catch (Exception e) {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    // Gửi yêu cầu POST
    private void sendPostRequest(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())  // Gửi yêu cầu POST với payload rỗng
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Hiển thị thông tin phản hồi
            resultArea.setText("POST Request sent. Response Code: " + response.statusCode());
            webEngine.loadContent(response.body());  // Tải nội dung trả về nếu có
        } catch (Exception e) {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    // Gửi yêu cầu HEAD
    private void sendHeadRequest(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())  // Gửi yêu cầu HEAD
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            // Hiển thị thông tin header phản hồi
            resultArea.setText("HEAD Request sent.\nResponse Code: " + response.statusCode() + "\nHeaders: " + response.headers().map().toString());
        } catch (Exception e) {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    // Hàm xử lý nút Back
    private void handleBack() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() > 0) {
            history.go(-1); // Đi lùi trong lịch sử duyệt web
        }
    }

    // Hàm xử lý nút Next
    private void handleNext() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() < history.getEntries().size() - 1) {
            history.go(1); // Đi tới trong lịch sử duyệt web
        }
    }

    // Hàm xử lý nút Refresh
    private void handleRefresh() {
        webEngine.reload(); // Tải lại trang hiện tại
    }

    // Cập nhật trạng thái các nút Back và Next dựa trên lịch sử duyệt web
    private void updateNavigationButtons() {
        WebHistory history = webEngine.getHistory();
        int currentIndex = history.getCurrentIndex();

        // Vô hiệu hóa nút Back nếu không thể quay lại
        backButton.setDisable(currentIndex <= 0);

        // Vô hiệu hóa nút Next nếu không thể đi tới
        nextButton.setDisable(currentIndex >= history.getEntries().size() - 1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
