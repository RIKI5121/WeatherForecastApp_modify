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
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        inputPanel.setBackground(new Color(230, 230, 250));
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
                textPane.setText(forecastList.get(forecastIndex[0]));
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
            JSONObject weatherRoot = rootArray.getJSONObject(0);
            JSONArray timeSeriesArray = weatherRoot.getJSONArray("timeSeries");

            JSONObject weatherPart = null, popPart = null, windPart = null, tempPart = null;

            for (int i = 0; i < timeSeriesArray.length(); i++) {
                JSONObject ts = timeSeriesArray.getJSONObject(i);
                JSONArray areas = ts.getJSONArray("areas");
                for (int j = 0; j < areas.length(); j++) {
                    JSONObject area = areas.getJSONObject(j);
                    if (weatherPart == null && area.has("weathers"))
                        weatherPart = ts;
                    if (popPart == null && area.has("pops"))
                        popPart = ts;
                    if (windPart == null && area.has("winds"))
                        windPart = ts;
                    if (tempPart == null && (area.has("temps") || area.has("tempMin") || area.has("tempMax")))
                        tempPart = ts;
                }
            }

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

                StringBuilder popInfo = new StringBuilder();
                for (int j = 0; j < popTimes.size() && j < pops.size(); j++) {
                    String popTime = popTimes.get(j);
                    if (popTime.substring(0, 10).equals(dateTime.substring(0, 10))) {
                        popInfo.append(String.format("降水確率（%s〜%s）: %s%%\n",
                                popTime.substring(11, 16), getEndTime(popTime), pops.get(j)));
                    }
                }

                String windInfo = "";
                for (int j = 0; j < windTimes.size() && j < winds.size(); j++) {
                    if (windTimes.get(j).substring(0, 10).equals(dateTime.substring(0, 10))) {
                        windInfo = "風向き: " + winds.get(j);
                        break;
                    }
                }

                String tempInfo = "";
                for (int j = 0; j < tempTimes.size() && j < temps.size(); j++) {
                    if (tempTimes.get(j).substring(0, 10).equals(dateTime.substring(0, 10))) {
                        tempInfo = "気温: " + temps.get(j) + "℃\n";
                        break;
                    }
                }

                String line = String.format("【%s】 \n天気: %s\n%s%s%s",
                        formatDateTime(dateTime), weather,
                        tempInfo,
                        popInfo.toString(),
                        windInfo.isEmpty() ? "" : windInfo + "\n");

                result.add(line);
            }

        } catch (Exception e) {
            result.add("天気情報の取得に失敗しました: " + e.getMessage());
        }

        return result;
    }

    private static String getEndTime(String startTime) {
        try {
            int hour = Integer.parseInt(startTime.substring(11, 13));
            int endHour = (hour + 6) % 24;
            return String.format("%02d:00", endHour);
        } catch (Exception e) {
            return "--:--";
        }
    }

    private static List<String> getStringList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.optString(i, "--"));
            }
        }
        return list;
    }

    private static String formatDateTime(String isoDateTime) {
        try {
            String datePart = isoDateTime.substring(0, 10);
            String timePart = isoDateTime.substring(11, 16);
            LocalDate date = LocalDate.parse(datePart);
            String jpDayOfWeek = switch (date.getDayOfWeek()) {
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