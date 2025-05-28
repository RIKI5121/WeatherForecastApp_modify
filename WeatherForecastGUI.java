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

    // 地域ごとの気象庁コード（https://www.jma.go.jp/bosai/common/const/area.json 等から抜粋）
    private static final Map<String, String> REGION_CODES = Map.of(
            "北海道", "010000",
            "宮城", "040000",
            "東京", "130000",
            "愛知", "230000",
            "大阪", "270000",
            "広島", "340000");

    private static String currentRegion = "大阪"; // デフォルト地域
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

        // 地域選択コンボボックス
        regionComboBox = new JComboBox<>(REGION_CODES.keySet().toArray(new String[0]));
        regionComboBox.setSelectedItem(currentRegion);
        regionComboBox.setFont(new Font("Meiryo", Font.BOLD, 24));

        loadButton = new JButton("お天気を読み込むニャ");
        nextLineButton = new JButton("次の天気を見せるニャ");
        nextLineButton.setEnabled(false);

        Font buttonFont = new Font("Meiryo", Font.BOLD, 30);
        loadButton.setFont(buttonFont);
        nextLineButton.setFont(buttonFont);
        loadButton.setMargin(new Insets(10, 20, 10, 20));
        nextLineButton.setMargin(new Insets(10, 20, 10, 20));

        textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setForeground(Color.BLACK);
        textPane.setFont(new Font("Serif", Font.BOLD, 24));
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // ボタンパネル（地域選択 + 読込ボタン + 次へボタン）
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("地域選択:"));
        topPanel.add(regionComboBox);
        topPanel.add(loadButton);
        topPanel.add(nextLineButton);

        loadButton.addActionListener(e -> {
            currentRegion = (String) regionComboBox.getSelectedItem();
            forecastList = fetchForecastList(currentRegion);
            forecastIndex = 0;
            textPane.setText("");
            appendWithHighlight("「" + currentRegion + "」の天気情報を読み込んだニャ！\nボタンを押すと順番に表示するニャ～\n");
            nextLineButton.setEnabled(true);
        });

        nextLineButton.addActionListener(e -> {
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

    // 地域を指定して天気情報を取得
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

        int[] targetHours = { 0, 6, 12, 18 };
        List<LocalDateTime> targetTimes = new ArrayList<>();

        for (int h : targetHours)
            targetTimes.add(today.withHour(h));
        for (int h : targetHours)
            targetTimes.add(tomorrow.withHour(h));

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
                JSONObject timeSeriesObj = rootArray.getJSONObject(0)
                        .getJSONArray("timeSeries").getJSONObject(0);

                JSONArray timeDefinesArray = timeSeriesObj.getJSONArray("timeDefines");
                JSONArray areasArray = timeSeriesObj.getJSONArray("areas");
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                Map<LocalDateTime, String> weatherMap = new HashMap<>();
                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    LocalDateTime dt = LocalDateTime.parse(timeDefinesArray.getString(i),
                            DateTimeFormatter.ISO_DATE_TIME);
                    if (i < weathersArray.length()) {
                        weatherMap.put(dt, weathersArray.getString(i));
                    }
                }

                result.add("=== " + today.format(DateTimeFormatter.ofPattern("M月d日")) + "・" +
                        tomorrow.format(DateTimeFormatter.ofPattern("d日")) + "の" + region + "の天気（6時間ごと）ニャ ===");

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d H時");

                for (LocalDateTime targetTime : targetTimes) {
                    String dateStr = targetTime.format(fmt);
                    String weather = weatherMap.get(targetTime);
                    if (weather == null || weather.isEmpty()) {
                        result.add(dateStr + " の" + region + "のお天気はデータがないニャ…");
                    } else {
                        result.add(formatCatStyle(dateStr, weather, region));
                    }
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