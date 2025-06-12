import javax.swing.*;
import javax.swing.text.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.List;

import org.json.*;

public class WeatherForecastUtil {
    public static final Map<String, String> REGION_CODES = Map.ofEntries(
            Map.entry("北海道", "016000"), Map.entry("青森県", "020000"), Map.entry("岩手県", "030000"),
            Map.entry("宮城県", "040000"), Map.entry("秋田県", "050000"), Map.entry("山形県", "060000"),
            Map.entry("福島県", "070000"), Map.entry("茨城県", "080000"), Map.entry("栃木県", "090000"),
            Map.entry("群馬県", "100000"), Map.entry("埼玉県", "110000"), Map.entry("千葉県", "120000"),
            Map.entry("東京都", "130000"), Map.entry("神奈川県", "140000"), Map.entry("新潟県", "150000"),
            Map.entry("富山県", "160000"), Map.entry("石川県", "170000"), Map.entry("福井県", "180000"),
            Map.entry("山梨県", "190000"), Map.entry("長野県", "200000"), Map.entry("岐阜県", "210000"),
            Map.entry("静岡県", "220000"), Map.entry("愛知県", "230000"), Map.entry("三重県", "240000"),
            Map.entry("滋賀県", "250000"), Map.entry("京都府", "260000"), Map.entry("大阪府", "270000"),
            Map.entry("兵庫県", "280000"), Map.entry("奈良県", "290000"), Map.entry("和歌山県", "300000"),
            Map.entry("鳥取県", "310000"), Map.entry("島根県", "320000"), Map.entry("岡山県", "330000"),
            Map.entry("広島県", "340000"), Map.entry("山口県", "350000"), Map.entry("徳島県", "360000"),
            Map.entry("香川県", "370000"), Map.entry("愛媛県", "380000"), Map.entry("高知県", "390000"),
            Map.entry("福岡県", "400000"), Map.entry("佐賀県", "410000"), Map.entry("長崎県", "420000"),
            Map.entry("熊本県", "430000"), Map.entry("大分県", "440000"), Map.entry("宮崎県", "450000"),
            Map.entry("鹿児島県", "460100"), Map.entry("沖縄県", "471000"));

    public static List<String> fetchDetailedForecast(String region) {
        List<String> result = new ArrayList<>();
        try {
            String code = REGION_CODES.get(region);
            if (code == null) {
                result.add("指定された地域は存在しません。");
                return result;
            }

            URI uri = URI.create("https://www.jma.go.jp/bosai/forecast/data/forecast/" + code + ".json");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");

            StringBuilder jsonText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonText.append(line);
                }
            }

            JSONArray rootArray = new JSONArray(jsonText.toString());
            JSONArray timeSeriesArray = rootArray.getJSONObject(0).getJSONArray("timeSeries");

            JSONObject weatherPart = findTimeSeriesWithKey(timeSeriesArray, "weathers");
            JSONObject popPart = findTimeSeriesWithKey(timeSeriesArray, "pops");
            JSONObject windPart = findTimeSeriesWithKey(timeSeriesArray, "winds");
            JSONObject tempPart = findTimeSeriesWithKey(timeSeriesArray, "temps");

            if (weatherPart == null || popPart == null || windPart == null) {
                result.add("必要な天気情報が取得できませんでした。");
                return result;
            }

            List<String> weatherTimes = getStringList(weatherPart.getJSONArray("timeDefines"));
            List<String> weathers = getStringList(
                    weatherPart.getJSONArray("areas").getJSONObject(0).getJSONArray("weathers"));

            List<String> popTimes = getStringList(popPart.getJSONArray("timeDefines"));
            List<String> pops = getStringList(popPart.getJSONArray("areas").getJSONObject(0).getJSONArray("pops"));

            List<String> windTimes = getStringList(windPart.getJSONArray("timeDefines"));
            List<String> winds = getStringList(windPart.getJSONArray("areas").getJSONObject(0).getJSONArray("winds"));

            List<String> tempTimes = new ArrayList<>();
            List<String> temps = new ArrayList<>();
            if (tempPart != null) {
                tempTimes = getStringList(tempPart.getJSONArray("timeDefines"));
                JSONObject tempArea = tempPart.getJSONArray("areas").getJSONObject(0);
                if (tempArea.has("temps")) {
                    temps = getStringList(tempArea.getJSONArray("temps"));
                } else {
                    List<String> minTemps = getStringList(tempArea.optJSONArray("tempMin"));
                    List<String> maxTemps = getStringList(tempArea.optJSONArray("tempMax"));
                    for (int i = 0; i < Math.max(minTemps.size(), maxTemps.size()); i++) {
                        String min = (i < minTemps.size()) ? minTemps.get(i) : "--";
                        String max = (i < maxTemps.size()) ? maxTemps.get(i) : "--";
                        temps.add(String.format("最低 %s℃ / 最高 %s℃", min, max));
                    }
                }
            }

            for (int i = 0; i < weatherTimes.size() && i < weathers.size(); i++) {
                String dateTime = weatherTimes.get(i);
                String weather = weathers.get(i);

                String popInfo = buildPopInfo(dateTime, popTimes, pops);
                String windInfo = buildWindInfo(dateTime, windTimes, winds);
                String tempInfo = buildTempInfo(dateTime, tempTimes, temps);

                String line = String.format("日時: %s\n天気: %s\n%s%s%s",
                        formatDateTime(dateTime), weather, tempInfo, popInfo,
                        windInfo.isEmpty() ? "" : windInfo + "\n");

                result.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.add("天気情報の取得中にエラーが発生しました。");
        }

        return result;
    }

    private static JSONObject findTimeSeriesWithKey(JSONArray timeSeriesArray, String key) {
        for (int i = 0; i < timeSeriesArray.length(); i++) {
            JSONObject ts = timeSeriesArray.getJSONObject(i);
            JSONArray areas = ts.getJSONArray("areas");
            if (areas.length() > 0 && areas.getJSONObject(0).has(key)) {
                return ts;
            }
        }
        return null;
    }

    private static String buildPopInfo(String date, List<String> times, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < times.size() && j < values.size(); j++) {
            if (times.get(j).startsWith(date.substring(0, 10))) {
                String start = times.get(j).substring(11, 16);
                String end = (j + 1 < times.size() && times.get(j + 1).startsWith(date.substring(0, 10)))
                        ? times.get(j + 1).substring(11, 16)
                        : "00:00";
                sb.append(String.format("降水確率（%s〜%s）: %s%%\n", start, end, values.get(j)));
            }
        }
        return sb.toString();
    }

    private static String buildWindInfo(String date, List<String> times, List<String> values) {
        for (int j = 0; j < times.size() && j < values.size(); j++) {
            if (times.get(j).startsWith(date.substring(0, 10))) {
                return "風向き: " + values.get(j);
            }
        }
        return "";
    }

    private static String buildTempInfo(String date, List<String> times, List<String> values) {
        for (int j = 0; j < times.size() && j < values.size(); j++) {
            String tempDate = times.get(j).substring(0, 10);
            String tempHour = times.get(j).substring(11, 13);
            if (tempDate.equals(date.substring(0, 10)) && tempHour.equals("09")) {
                return "最高気温: " + values.get(j) + "℃\n";
            }
        }
        return "";
    }

    public static List<String> getStringList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray == null)
            return list;
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.optString(i, ""));
        }
        return list;
    }

    public static String formatDateTime(String dateTime) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateTime);
            ZonedDateTime zdt = odt.atZoneSameInstant(ZoneId.systemDefault());
            LocalDate date = zdt.toLocalDate();
            String[] days = { "月", "火", "水", "木", "金", "土", "日" };
            return String.format("%d年%d月%d日（%s曜日）", date.getYear(), date.getMonthValue(),
                    date.getDayOfMonth(), days[date.getDayOfWeek().getValue() - 1]);
        } catch (Exception e) {
            return dateTime;
        }
    }

    public static BufferedImage getBackgroundImageForWeather(String weather) {
        String path;
        if (weather == null) {
            path = "backgrounds/default.jpg";
        } else if (weather.contains("晴")) {
            path = "backgrounds/sunny.jpg";
        } else if (weather.contains("くもり") || weather.contains("曇")) {
            path = "backgrounds/cloudy.jpg";
        } else if (weather.contains("雨")) {
            path = "backgrounds/rainy.jpg";
        } else if (weather.contains("雪")) {
            path = "backgrounds/snowy.jpg";
        } else {
            path = "backgrounds/default.jpg";
        }

        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("背景画像の読み込み失敗: " + path);
            return null;
        }
    }

    public static void setStyledText(JTextPane textPane, String text) {
        textPane.setText("");
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        StyleConstants.setFontSize(bold, 22);
        StyleConstants.setForeground(bold, Color.BLACK);
        StyleConstants.setBackground(bold, new Color(255, 255, 255, 180));

        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setFontSize(normal, 20);
        StyleConstants.setForeground(normal, Color.BLACK);
        StyleConstants.setBackground(normal, new Color(255, 255, 255, 180));

        String[] lines = text.split("\n", 2);
        try {
            doc.insertString(doc.getLength(), lines[0] + "\n", bold);
            if (lines.length > 1) {
                doc.insertString(doc.getLength(), lines[1], normal);
            }
        } catch (BadLocationException e) {
            textPane.setText(text);
        }
    }
}