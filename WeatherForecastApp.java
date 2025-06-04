import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.List;
import org.json.*;

public class WeatherForecastApp {
    private static final Map<String, String> REGION_CODES = Map.ofEntries(
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("天気予報アプリ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null); // 画面中央に表示

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245)); // 明るいグレー背景

        // 入力エリアパネル（上部）
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        inputPanel.setBackground(new Color(230, 230, 250)); // 薄いラベンダー色
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "地域入力"));

        JTextField regionInput = new JTextField(15);
        regionInput.setFont(new Font("Meiryo", Font.PLAIN, 18));
        inputPanel.add(new JLabel("都道府県:"));
        inputPanel.add(regionInput);

        JButton showInputButton = new JButton("表示");
        showInputButton.setFont(new Font("Meiryo", Font.BOLD, 18));
        inputPanel.add(showInputButton);

        JButton nextLineButton = new JButton("次の天気を表示");
        nextLineButton.setFont(new Font("Meiryo", Font.BOLD, 18));
        nextLineButton.setEnabled(false);
        inputPanel.add(nextLineButton);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // テキスト表示エリア
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Meiryo", Font.PLAIN, 20));
        textPane.setBackground(Color.WHITE);
        textPane.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "天気予報"));

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        // ボタンの動作
        List<String> forecastList = new ArrayList<>();
        final int[] forecastIndex = { 0 };

        showInputButton.addActionListener(_ -> {
            String inputRegion = regionInput.getText().trim();
            if (!REGION_CODES.containsKey(inputRegion)) {
                JOptionPane.showMessageDialog(frame, "正しい都道府県名を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }
            forecastList.clear();
            forecastList.addAll(fetchDetailedForecast(inputRegion));
            forecastIndex[0] = 0;
            textPane.setText("");
            if (forecastList.isEmpty()) {
                textPane.setText("天気情報が取得できませんでした。");
                nextLineButton.setEnabled(false);
            } else {
                textPane.setText(forecastList.get(0));
                forecastIndex[0] = 1;
                nextLineButton.setEnabled(forecastList.size() > 1);
            }
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex[0] < forecastList.size()) {
                String text = forecastList.get(forecastIndex[0]);
                textPane.setText(text);
                forecastIndex[0]++;
            } else {
                textPane.setText("これ以上の天気情報はありません。");
                nextLineButton.setEnabled(false);
            }
        });
    }

    private static List<String> fetchDetailedForecast(String region) {
        List<String> result = new ArrayList<>();
        try {
            String code = REGION_CODES.get(region);
            if (code == null) {
                result.add("指定された地域コードがありません: " + region);
                return result;
            }

            URI uri = URI.create("https://www.jma.go.jp/bosai/forecast/data/forecast/" + code + ".json");
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            StringBuilder jsonText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonText.append(line);
                }
            }

            JSONArray rootArray = new JSONArray(jsonText.toString());
            if (rootArray.length() < 1) {
                result.add("APIのレスポンスデータが不足しています。");
                return result;
            }

            JSONObject weatherRoot = rootArray.getJSONObject(0);
            JSONArray timeSeriesArray = weatherRoot.getJSONArray("timeSeries");

            JSONObject weatherPart = null;
            JSONObject popPart = null;
            JSONObject windPart = null;

            for (int i = 0; i < timeSeriesArray.length(); i++) {
                JSONObject ts = timeSeriesArray.getJSONObject(i);
                JSONArray areas = ts.getJSONArray("areas");
                for (int j = 0; j < areas.length(); j++) {
                    JSONObject areaObj = areas.getJSONObject(j);
                    if (weatherPart == null && areaObj.has("weathers")) {
                        weatherPart = ts;
                    }
                    if (popPart == null && areaObj.has("pops")) {
                        popPart = ts;
                    }
                    if (windPart == null && areaObj.has("winds")) {
                        windPart = ts;
                    }
                    if (weatherPart != null && popPart != null && windPart != null) {
                        break;
                    }
                }
            }

            if (weatherPart == null || popPart == null || windPart == null) {
                result.add("必要な天気情報（天気・降水確率・風向き）が見つかりません。");
                return result;
            }

            List<String> weatherTimes = getStringList(weatherPart.getJSONArray("timeDefines"));
            JSONObject weatherArea = weatherPart.getJSONArray("areas").getJSONObject(0);
            List<String> weathers = getStringList(weatherArea.getJSONArray("weathers"));

            List<String> popTimes = getStringList(popPart.getJSONArray("timeDefines"));
            JSONObject popArea = popPart.getJSONArray("areas").getJSONObject(0);
            List<String> pops = getStringList(popArea.getJSONArray("pops"));

            List<String> windTimes = getStringList(windPart.getJSONArray("timeDefines"));
            JSONObject windArea = windPart.getJSONArray("areas").getJSONObject(0);
            List<String> winds = getStringList(windArea.getJSONArray("winds"));

            for (int i = 0; i < weatherTimes.size() && i < weathers.size(); i++) {
                String dateTime = weatherTimes.get(i);
                String weather = weathers.get(i);

                StringBuilder popInfo = new StringBuilder();
                for (int j = 0; j < popTimes.size() && j < pops.size(); j++) {
                    String popTime = popTimes.get(j);
                    String popValue = pops.get(j);

                    if (popTime.substring(0, 10).equals(dateTime.substring(0, 10))) {
                        popInfo.append(String.format("降水確率: %s%% [%s-%s]\n",
                                popTime.substring(11, 16), getEndTime(popTime), popValue));
                    }
                }

                String windInfo = "";
                for (int j = 0; j < windTimes.size() && j < winds.size(); j++) {
                    String windTime = windTimes.get(j);
                    if (windTime.substring(0, 10).equals(dateTime.substring(0, 10))) {
                        windInfo = "風向き: " + winds.get(j);
                        break;
                    }
                }

                String line = String.format("【%s】 \n天気: %s\n%s%s",
                        formatDateTime(dateTime), weather, popInfo.toString(),
                        windInfo.isEmpty() ? "" : windInfo + "\n");

                result.add(line);
            }

        } catch (IOException | JSONException e) {
            result.add("詳細天気情報の取得に失敗しました: " + e.getMessage());
        }
        return result;
    }

    private static String getEndTime(String startTime) {
        try {
            int hour = Integer.parseInt(startTime.substring(11, 13));
            int endHour = (hour + 6) % 24; // 6時間後
            return String.format("%02d:00", endHour);
        } catch (Exception e) {
            return "--:--";
        }
    }

    private static List<String> getStringList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }

    // 追加：日時を「2025年06月06日（土） 00:00」形式に変換
    private static String formatDateTime(String isoDateTime) {
        try {
            // 例: 2025-06-06T00:00:00+09:00 → 先頭10文字＋時間部分11〜16文字を取得
            String datePart = isoDateTime.substring(0, 10); // 2025-06-06
            String timePart = isoDateTime.substring(11, 16); // 00:00

            // LocalDateに変換
            LocalDate date = LocalDate.parse(datePart);

            // 曜日を日本語の短縮形で取得（例：月、火、水…）
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String jpDayOfWeek = switch (dayOfWeek) {
                case MONDAY -> "月";
                case TUESDAY -> "火";
                case WEDNESDAY -> "水";
                case THURSDAY -> "木";
                case FRIDAY -> "金";
                case SATURDAY -> "土";
                case SUNDAY -> "日";
            };

            return String.format("%d年%02d月%02d日（%s） %s",
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    jpDayOfWeek, timePart);

        } catch (Exception e) {
            return isoDateTime;
        }
    }
}