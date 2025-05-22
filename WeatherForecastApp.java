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

/**
 * 天気予報アプリ - ネコ版
 * 気象庁のWeb APIから大阪府の天気予報を取得して、ネコがしゃべっているように表示します。
 */
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
                JSONObject report = rootArray.getJSONObject(0); // 最新の天気情報を取得
                JSONArray timeSeries = report.getJSONArray("timeSeries");

                // 時間ごとのデータを格納するリスト
                List<String> timeDefines = new ArrayList<>();
                List<String> weathers = new ArrayList<>();
                List<String> pops = new ArrayList<>();
                List<String> temps = new ArrayList<>();

                // 1つ目の時系列データ：天気
                JSONObject weatherData = timeSeries.getJSONObject(0);
                JSONArray timeDefinesArray = weatherData.getJSONArray("timeDefines");
                JSONArray areasArray = weatherData.getJSONArray("areas");

                // 各エリアの天気を取得
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                // 2つ目の時系列データ：降水確率
                JSONObject popData = timeSeries.getJSONObject(1);
                JSONArray popsArray = popData.getJSONArray("areas").getJSONObject(0).getJSONArray("pops");

                // 3つ目の時系列データ：気温
                JSONObject tempData = timeSeries.getJSONObject(2);
                JSONArray tempsArray = tempData.getJSONArray("areas").getJSONObject(0).getJSONArray("temps");

                // データをリストに追加
                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    timeDefines.add(timeDefinesArray.getString(i));
                    weathers.add(weathersArray.getString(i));
                    pops.add(popsArray.getString(i));
                    temps.add(tempsArray.getString(i));
                }

                // 今日の日付を取得
                LocalDateTime today = LocalDateTime.now();
                String todayDate = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                // 明日の日付を取得
                LocalDateTime tomorrow = today.plusDays(1);
                String tomorrowDate = tomorrow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                // 今日と明日の6時間ごとの天気を表示
                int[] targetHours = { 6, 12, 18, 24 }; // 対象の時間
                for (int hour : targetHours) {
                    String targetTimeToday = todayDate + " " + (hour < 10 ? "0" + hour : hour) + ":00";
                    displayWeatherAt(targetTimeToday, timeDefines, weathers, pops, temps);
                }

                // 明日の6時間ごとの天気を表示
                for (int hour : targetHours) {
                    String targetTimeTomorrow = tomorrowDate + " " + (hour < 10 ? "0" + hour : hour) + ":00";
                    displayWeatherAt(targetTimeTomorrow, timeDefines, weathers, pops, temps);
                }

            } else {
                System.out.println("データ取得に失敗しましたニャ…");
            }
        } catch (URISyntaxException e) {
            System.out.println("URIの構文が無効ですニャ: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("通信エラーが発生したニャ: ");
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /**
     * 指定した時刻の天気を表示
     */
    private static void displayWeatherAt(String targetTime, List<String> timeDefines, List<String> weathers,
            List<String> pops, List<String> temps) {
        for (int i = 0; i < timeDefines.size(); i++) {
            String time = timeDefines.get(i);
            if (time.startsWith(targetTime)) {
                String weather = weathers.get(i);
                String pop = pops.get(i);
                String temp = temps.get(i);
                System.out.println(formatCatStyle(targetTime, weather, pop, temp));
                return;

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
        }
        System.out.println(targetTime + "  天気データがありませんニャ");
    }


    /**
     * ネコがしゃべっているような形式に整形する
     */
    private static String formatCatStyle(String date, String weather, String pop, String temp) {
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

        // 読みやすくするため、スペースを読点に変換
        String spokenWeather = weather.replaceAll("\\s+", "、");

        return date + "  大阪のお天気は「" + spokenWeather + "」" + tail + "\n降水確率: " + pop + "  気温: " + temp + "°C";

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