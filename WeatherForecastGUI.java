import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.json.*;

// 背景画像付きパネルクラス
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

public class WeatherForecastGUI {

    private static final Map<String, String> REGION_CODES = Map.of(
            "北海道", "010000",
            "宮城", "040000",
            "東京", "130000",
            "愛知", "230000",
            "大阪", "270000",
            "広島", "340000");

    private static String currentRegion = "大阪";
    private static List<String> forecastList = new ArrayList<>();
    private static int forecastIndex = 0;

    private static JTextPane textPane;
    private static JComboBox<String> regionComboBox;
    private static JButton loadButton;
    private static JButton nextLineButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("お天気ネコちゃん");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1500, 1000);

        BackgroundPanel backgroundPanel = new BackgroundPanel("/img/background.png");

        regionComboBox = new JComboBox<>(REGION_CODES.keySet().toArray(new String[0]));
        regionComboBox.setSelectedItem(currentRegion);
        regionComboBox.setFont(new Font("Meiryo", Font.BOLD, 24));

        loadButton = new JButton("お天気を読み込むニャ");
        nextLineButton = new JButton("次の天気を見せるニャ");
        nextLineButton.setEnabled(false);

        Font buttonFont = new Font("Meiryo", Font.BOLD, 30);
        loadButton.setFont(buttonFont);
        nextLineButton.setFont(buttonFont);

        textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setForeground(Color.BLACK);
        textPane.setFont(new Font("Serif", Font.BOLD, 24));
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("地域選択:"));
        topPanel.add(regionComboBox);
        topPanel.add(loadButton);
        topPanel.add(nextLineButton);

        loadButton.addActionListener(_ -> {
            currentRegion = (String) regionComboBox.getSelectedItem();
            forecastList = fetchForecastList(currentRegion);
            forecastIndex = 0;
            textPane.setText("");
            appendWithHighlight("「" + currentRegion + "」の天気情報を読み込んだニャ！\nボタンを押すと順番に表示するニャ～\n");
            nextLineButton.setEnabled(true);
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex < forecastList.size()) {
                String line = forecastList.get(forecastIndex);
                appendWithHighlight(line);
                forecastIndex++;
            } else {
                appendWithHighlight("もう全部出したニャ。\n");
                nextLineButton.setEnabled(false);
            }
        });

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

                // 結果表示のヘッダー
                result.add("=== " + today.format(DateTimeFormatter.ofPattern("M月d日")) + "・" +
                        tomorrow.format(DateTimeFormatter.ofPattern("d日")) + "の" + region + "の天気と気温ニャ ===");

                // 各日付ごとの天気を取り出して1日単位で表示
                LocalDateTime[] days = { today, tomorrow };
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");

                for (LocalDateTime day : days) {
                    String dateStr = day.format(fmt);
                    String weather = null;
                    String tempHigh = null;
                    String tempLow = null;

                    // 最初の timeSeries から天気予報を取得（テキストベース）
                    if (!weatherMap.isEmpty()) {
                        for (Map.Entry<LocalDateTime, String> entry : weatherMap.entrySet()) {
                            if (entry.getKey().toLocalDate().equals(day.toLocalDate())) {
                                weather = entry.getValue();
                                break; // 最初の一致だけ表示
                            }
                        }
                    }

                    // tempMap から気温（複数あれば最高/最低を抽出）
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
                        tempHigh = String.valueOf(temps.stream().max(Integer::compare).orElse(0));
                        tempLow = String.valueOf(temps.stream().min(Integer::compare).orElse(0));
                    }

                    // 猫語風の天気表現で表示
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

    // 猫語風の天気表現メソッド（ここで使う）
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