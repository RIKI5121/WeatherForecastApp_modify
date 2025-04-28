import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 天気予報アプリ - 本体
 * このアプリケーションは、気象庁のWeb APIから大阪府の天気予報データを取得して表示します
 *
 * @author n.katayama
 * @version 1.0
 */
public class WeatherForecastApp {
    /**
     * 気象庁の天気予報APIのエンドポイントURL
     * 大阪府の天気予報データを提供します
     */
    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json"; // 天気予報APIのURL（大阪府）

    /**
     * メイン処理: 天気予報の取得と表示を実行します
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        try {
            // Web APIを呼び出す
            // HttpURLConnectionを使ってGETリクエストを送信する
            URL url = new URL(TARGET_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // レスポンスコードがOKかどうかを確認する
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // レスポンスボディを取得する
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                }

                // JSONデータを取得する
                JSONArray rootArray = new JSONArray(responseBody.toString());
                JSONObject timeStringObject = rootArray.getJSONObject(0)
                        .getJSONArray("timeSeries").getJSONObject(0);

                List<String> timeDefines = new ArrayList<>();
                List<String> weather = new ArrayList<>();

                // 日時と天気情報を抽出する
                JSONArray timeDefinesArray = timeStringObject.getJSONArray("timeDefines");
                JSONArray weathersArray = timeStringObject.getJSONArray("areas")
                        .getJSONObject(0).getJSONArray("weathers");

                // 日時と天気予報のリストを作成する
                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    timeDefines.add(timeDefinesArray.getString(i));
                    weather.add(weathersArray.getString(i));
                }

                // 日時と天気予報を表示する
                for (int i = 0; i < Math.min(timeDefines.size(), weather.size()); i++) {
                    LocalDateTime dateTime = LocalDateTime.parse(
                            timeDefines.get(i),
                            DateTimeFormatter.ISO_DATE_TIME);
                    System.out.println(
                            dateTime.format(
                                    DateTimeFormatter.ofPattern("yyyy/MM/dd")) + " " + weather.get(i));
                }
            } else {
                // レスポンスコードがOKでない場合のエラー処理
                System.out.println("データ取得に失敗しました!");
            }
        } catch (IOException e) {
            // IO例外を処理する
            e.printStackTrace();
        }
    }
}