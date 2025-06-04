import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import org.json.*;

public class WeatherForecastGUI {
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
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
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

        // ボタンの動作（例）
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
                // 今日の天気だけを表示
                textPane.setText(forecastList.get(0));
                forecastIndex[0] = 1; // 次は2件目を表示
                nextLineButton.setEnabled(forecastList.size() > 1);
            }
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex[0] < forecastList.size()) {
                String text = forecastList.get(forecastIndex[0]);
                textPane.setText(text); // 前の天気は削除して新しい天気だけを表示
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
            JSONArray weatherTimeSeries = weatherRoot.getJSONArray("timeSeries");
            JSONObject weatherPart = weatherTimeSeries.getJSONObject(0);

            List<String> weatherTimes = getStringList(weatherPart.getJSONArray("timeDefines"));
            JSONArray weatherAreas = weatherPart.getJSONArray("areas");
            JSONObject weatherArea = weatherAreas.getJSONObject(0);
            List<String> weathers = getStringList(weatherArea.getJSONArray("weathers"));

            int maxWeatherCount = Math.min(3, Math.min(weatherTimes.size(), weathers.size()));
            for (int i = 0; i < maxWeatherCount; i++) {
                String date = weatherTimes.get(i).substring(0, 10);
                result.add("【天気】" + date + ": " + weathers.get(i));
            }

        } catch (IOException | JSONException e) {
            result.add("詳細天気情報の取得に失敗しました: " + e.getMessage());
        }
        return result;
    }

    private static List<String> getStringList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }
}