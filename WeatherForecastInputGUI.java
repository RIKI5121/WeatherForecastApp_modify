import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import org.json.*;

class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        try {
            if (imagePath != null) {
                backgroundImage = new ImageIcon(getClass().getResource(imagePath)).getImage();
            }
        } catch (Exception e) {
            System.err.println("背景画像の読み込みに失敗したニャ: " + e.getMessage());
        }
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

public class WeatherForecastInputGUI {

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

    private static String currentRegion = "大阪";
    private static List<String> forecastList = new ArrayList<>();
    private static int forecastIndex = 0;

    private static JTextPane textPane;
    private static JButton nextLineButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastInputGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("お天気ネコちゃん");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600, 1000);

        BackgroundPanel backgroundPanel = new BackgroundPanel("/img/background.png");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel controlPanel = new JPanel();
        controlPanel.setOpaque(false);

        nextLineButton = new JButton("次の天気を見せるニャ");
        nextLineButton.setFont(new Font("Meiryo", Font.BOLD, 28));
        nextLineButton.setEnabled(false);
        nextLineButton.addActionListener(_ -> {
            if (forecastIndex < forecastList.size()) {
                appendWithHighlight(forecastList.get(forecastIndex));
                forecastIndex++;
            } else {
                appendWithHighlight("もう全部出したニャ。\n");
                nextLineButton.setEnabled(false);
            }
        });

        // --- 入力欄と表示ボタンの追加 ---
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        JTextField regionInput = new JTextField(10);
        regionInput.setFont(new Font("Meiryo", Font.PLAIN, 20));
        JButton showInputButton = new JButton("この地域を表示ニャ");
        showInputButton.setFont(new Font("Meiryo", Font.BOLD, 28));

        showInputButton.addActionListener(_ -> {
            String inputRegion = regionInput.getText().trim();
            if (REGION_CODES.containsKey(inputRegion)) {
                currentRegion = inputRegion;
                forecastList = fetchForecastList(currentRegion);
                forecastIndex = 0;
                textPane.setText("");
                appendWithHighlight("「" + currentRegion + "」の天気情報を読み込んだニャ！\n順番に表示するニャ～\n");
                nextLineButton.setEnabled(true);
            } else {
                appendWithHighlight("「" + inputRegion + "」は知らニャい地域ニャ…正しい都道府県名を入力してニャ！");
            }
        });

        inputPanel.add(new JLabel("地域入力:"));
        inputPanel.add(regionInput);
        inputPanel.add(showInputButton);

        controlPanel.add(inputPanel);
        controlPanel.add(nextLineButton);

        textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setForeground(Color.BLACK);
        textPane.setFont(new Font("Serif", Font.BOLD, 24));
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        topPanel.add(controlPanel, BorderLayout.NORTH);

        backgroundPanel.add(topPanel, BorderLayout.NORTH);
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        frame.setContentPane(backgroundPanel);
        frame.setVisible(true);
    }

    private static void appendWithHighlight(String text) {
        StyledDocument doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("BlackTextStyle", null);
        StyleConstants.setForeground(style, Color.BLACK);
        StyleConstants.setBackground(style, new Color(255, 255, 180));
        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static List<String> fetchForecastList(String region) {
        List<String> result = new ArrayList<>();
        HttpURLConnection connection = null;
        String code = REGION_CODES.get(region);
        if (code == null) {
            result.add("地域コードが見つからニャい…");
            return result;
        }

        String targetUrl = "https://www.jma.go.jp/bosai/forecast/data/forecast/" + code + ".json";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = today.plusDays(1);

        try {
            URI uri = new URI(targetUrl);
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
                Map<LocalDateTime, String> weatherMap = new HashMap<>();
                JSONArray timeSeriesArray = rootArray.getJSONObject(0).getJSONArray("timeSeries");

                JSONObject weatherSeries = timeSeriesArray.getJSONObject(0);
                JSONArray weatherTimeDefines = weatherSeries.getJSONArray("timeDefines");
                JSONArray weatherAreas = weatherSeries.getJSONArray("areas");
                JSONArray weathersArray = weatherAreas.getJSONObject(0).getJSONArray("weathers");

                for (int i = 0; i < weatherTimeDefines.length() && i < weathersArray.length(); i++) {
                    LocalDateTime dt = LocalDateTime.parse(weatherTimeDefines.getString(i),
                            DateTimeFormatter.ISO_DATE_TIME);
                    weatherMap.put(dt, weathersArray.getString(i));
                }

                Map<LocalDateTime, String> tempMap = new HashMap<>();
                for (int tsIndex = 0; tsIndex < timeSeriesArray.length(); tsIndex++) {
                    JSONObject tsObj = timeSeriesArray.getJSONObject(tsIndex);
                    JSONArray timeDefines = tsObj.getJSONArray("timeDefines");
                    JSONArray areas = tsObj.getJSONArray("areas");

                    if (areas.length() == 0 || !areas.getJSONObject(0).has("temps"))
                        continue;

                    JSONArray temps = areas.getJSONObject(0).getJSONArray("temps");
                    for (int i = 0; i < timeDefines.length() && i < temps.length(); i++) {
                        String tempStr = temps.getString(i);
                        if (tempStr == null || tempStr.isEmpty())
                            continue;

                        LocalDateTime dt = LocalDateTime.parse(timeDefines.getString(i),
                                DateTimeFormatter.ISO_DATE_TIME);
                        tempMap.put(dt, tempStr);
                    }
                }

                result.add("=== " + today.format(DateTimeFormatter.ofPattern("M月d日")) + "・" +
                        tomorrow.format(DateTimeFormatter.ofPattern("d日")) + "の" + region + "の天気と気温ニャ ===");

                LocalDateTime[] days = { today, tomorrow };
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");

                for (LocalDateTime day : days) {
                    String dateStr = day.format(fmt);
                    String weather = null;
                    String tempHigh = null;
                    String tempLow = null;

                    for (Map.Entry<LocalDateTime, String> entry : weatherMap.entrySet()) {
                        if (entry.getKey().toLocalDate().equals(day.toLocalDate())) {
                            weather = entry.getValue();
                            break;
                        }
                    }

                    List<Integer> temps = new ArrayList<>();
                    for (Map.Entry<LocalDateTime, String> entry : tempMap.entrySet()) {
                        if (entry.getKey().toLocalDate().equals(day.toLocalDate())) {
                            try {
                                temps.add(Integer.parseInt(entry.getValue()));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    if (!temps.isEmpty()) {
                        tempHigh = String.valueOf(Collections.max(temps));
                        tempLow = String.valueOf(Collections.min(temps));
                    }

                    String line = formatCatStyle(dateStr, weather, region);
                    if (tempHigh != null && tempLow != null) {
                        line += " 最高気温 " + tempHigh + "℃、最低気温 " + tempLow + "℃ ニャ～";
                    }

                    result.add(line);
                }

            } else {
                result.add("データ取得に失敗したニャ…");
            }

        } catch (Exception e) {
            result.add("エラーが起きたニャ: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    private static String formatCatStyle(String date, String weather, String region) {
        if (weather == null)
            weather = "不明";
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
        return date + " の " + region + " のお天気は「" + spokenWeather + "」" + tail;
    }
}