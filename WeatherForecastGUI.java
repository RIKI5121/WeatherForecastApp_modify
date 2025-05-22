import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.*;

public class WeatherForecastGUI {

    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("大阪のお天気（ネコ風）");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Serif", Font.PLAIN, 16));

        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("最新の天気を取得するニャ");
        refreshButton.addActionListener(_ -> {
            textArea.setText("取得中ニャ……");
            String forecast = fetchForecast();
            textArea.setText(forecast);
        });

        frame.add(refreshButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static String fetchForecast() {
        StringBuilder output = new StringBuilder();
        HttpURLConnection connection = null;

        try {
            URI uri = new URI(TARGET_URL);
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                }

                JSONArray rootArray = new JSONArray(responseBody.toString());
                JSONObject timeSeriesObj = rootArray.getJSONObject(0)
                        .getJSONArray("timeSeries").getJSONObject(0);

                JSONArray timeDefinesArray = timeSeriesObj.getJSONArray("timeDefines");
                JSONArray areasArray = timeSeriesObj.getJSONArray("areas");
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    String dateStr = timeDefinesArray.getString(i);
                    String weather = weathersArray.getString(i);
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                    String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));

                    output.append(formatCatStyle(formattedDate, weather)).append("\n");
                }

            } else {
                output.append("データ取得に失敗しましたニャ…");
            }

        } catch (Exception e) {
            output.append("エラーが発生したニャ: ").append(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return output.toString();
    }

    private static String formatCatStyle(String date, String weather) {
        String tail;

        if (weather.contains("雨")) {
            tail = "っぽいニャ～";
        } else if (weather.contains("くもり") && weather.contains("晴")) {
            tail = "になりそうニャ";
        } else if (weather.contains("晴")) {
            tail = "みたいニャ～";
        } else if (weather.contains("くもり")) {
            tail = "かもニャ";
        } else {
            tail = "ニャ";
        }

        String spokenWeather = weather.replaceAll("\\s+", "、");
        return date + " の大阪のお天気は「" + spokenWeather + "」" + tail;
    }
}