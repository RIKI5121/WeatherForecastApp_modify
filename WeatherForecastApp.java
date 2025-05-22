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
                JSONObject timeSeriesObj = rootArray.getJSONObject(0)
                        .getJSONArray("timeSeries").getJSONObject(0);

                List<String> timeDefines = new ArrayList<>();
                List<String> weathers = new ArrayList<>();
                List<String> pops = new ArrayList<>();
                List<String> temps = new ArrayList<>();

                JSONArray timeDefinesArray = timeSeriesObj.getJSONArray("timeDefines");
                JSONArray areasArray = timeSeriesObj.getJSONArray("areas");
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                // "pops"と"temps"が別の構造になっている場合があるので、適切に確認
                JSONArray popsArray = areasArray.getJSONObject(0).optJSONArray("pops");
                JSONArray tempsArray = areasArray.getJSONObject(0).optJSONArray("temps");

                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    timeDefines.add(timeDefinesArray.getString(i));
                    weathers.add(weathersArray.getString(i));
                    pops.add(popsArray != null ? popsArray.getString(i) : "N/A"); // popsがnullの場合、N/Aをセット
                    temps.add(tempsArray != null ? tempsArray.getString(i) : "N/A"); // tempsがnullの場合、N/Aをセット
                }

                // 今日の日付を取得
                LocalDateTime today = LocalDateTime.now();
                String todayDate = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                // 今日と明日の6時、12時、18時、24時の天気を表示
                int[] targetHours = { 6, 12, 18, 24 }; // 対象の時間
                for (int hour : targetHours) {
                    String targetTime = todayDate + " " + (hour < 10 ? "0" + hour : hour) + ":00";
                    displayWeatherAt(targetTime, timeDefines, weathers, pops, temps);
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
    }
}