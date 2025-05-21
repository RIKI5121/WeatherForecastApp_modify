import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/*たつき */
/**
 * 天気予報アプリ - 本体
 * このアプリケーションは、気象庁のWeb APIから大阪府の天気予報データを取得して表示する
 *
 * @author n.katayama
 * @version 1.0
 */

// WeatherApiClientクラス: API通信を担当
class WeatherApiClient {
    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json";

    public String fetchWeatherData() throws IOException, URISyntaxException {
        URI uri = new URI(TARGET_URL);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }
            return responseBody.toString();
        } else {
            throw new IOException("Failed to fetch data: Response code " + responseCode);
        }
    }
}

// WeatherDataParserクラス: データ解析を担当
class WeatherDataParser {
    public List<String[]> parseWeatherData(String jsonData) {
        List<String[]> weatherList = new ArrayList<>();
        JSONArray rootArray = new JSONArray(jsonData);
        JSONObject timeStringObject = rootArray.getJSONObject(0)
                .getJSONArray("timeSeries").getJSONObject(0);

        JSONArray timeDefinesArray = timeStringObject.getJSONArray("timeDefines");
        JSONArray weathersArray = timeStringObject.getJSONArray("areas")
                .getJSONObject(0).getJSONArray("weathers");

        for (int i = 0; i < timeDefinesArray.length(); i++) {
            String dateTime = timeDefinesArray.getString(i);
            String weather = weathersArray.getString(i);
            weatherList.add(new String[] { dateTime, weather });
        }

        return weatherList;
    }
}

// WeatherForecastAppクラス: メイン処理
public class WeatherForecastApp {
    public static void main(String[] args) {
        WeatherApiClient apiClient = new WeatherApiClient();
        WeatherDataParser dataParser = new WeatherDataParser();

        try {
            // 天気データを取る
            String jsonData = apiClient.fetchWeatherData();

            // 天気データを解析
            List<String[]> weatherList = dataParser.parseWeatherData(jsonData);

            // 天気データを表示
            for (String[] weather : weatherList) {
                LocalDateTime dateTime = LocalDateTime.parse(
                        weather[0], DateTimeFormatter.ISO_DATE_TIME);
                String dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                String weatherStr = convertToAnimalStyle(weather[1]);
                System.out.println(dateStr + " " + weatherStr);
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("エラーが発生しました: " + e.getMessage());
        }
    }

    public static String convertToAnimalStyle(String weather) {
        String phrase;
        String colorCode = "";  // 背景色のコード
    
        if (weather.contains("晴")) {
            phrase = "猫「晴れだにゃ～」"; // 猫
            colorCode = "\u001B[43m"; // 背景色を黄色に
        } else if (weather.contains("曇")) {
            phrase = "牛「曇りモ〜」"; // 牛
            colorCode = "\u001B[48;5;235m"; // 背景色を灰色に

        } else if (weather.contains("雨")) {
            phrase = "カエル「雨ゲロゲロ～」"; // カエル
            colorCode = "\u001B[44m"; // 背景色を青に
        } else {
            phrase = weather + "（よくわからない天気だね）";
            colorCode = "\u001B[47m"; // 背景色を白に
        }
    
        // 色をリセット
        String resetCode = "\u001B[0m";
        return colorCode + phrase + resetCode;  // 色付きのテキストを返す
    }
}