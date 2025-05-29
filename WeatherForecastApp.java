import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherForecastApp {

    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json";

    public static void main(String[] args) {
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

                List<String> timeDefines = new ArrayList<>();
                List<String> weathers = new ArrayList<>();

                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    timeDefines.add(timeDefinesArray.getString(i));
                    weathers.add(weathersArray.getString(i));
                }

                // 出力処理
                for (int i = 0; i < timeDefines.size(); i++) {
                    LocalDateTime dateTime = LocalDateTime.parse(timeDefines.get(i), DateTimeFormatter.ISO_DATE_TIME);
                    String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                    String weather = weathers.get(i);
                    System.out.println(formatWeatherText(formattedDate, weather));
                }

            } else {
                System.out.println("データの取得に失敗しました。");
            }
        } catch (URISyntaxException e) {
            System.out.println("URIの構文が無効です: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("通信エラーが発生しました。");
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // 通常の天気情報表示文
    private static String formatWeatherText(String date, String weather) {
        String spokenWeather = weather.replaceAll("\\s+", "、");
        return date + " の大阪の天気は「" + spokenWeather + "」です。";
    }
}