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
import org.json.*;

// 背景画像付きパネルクラス
class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        try {
            // 画像パスをちゃんと使う
            backgroundImage = new ImageIcon(getClass().getResource(imagePath)).getImage();
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

    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json";

    private static List<String> forecastList = new ArrayList<>();
    private static int forecastIndex = 0;

    private static JTextPane textPane; // JTextArea → JTextPane に変更

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("大阪のお天気（ネコ風）");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1500, 1000);

        BackgroundPanel backgroundPanel = new BackgroundPanel("/img/background.png");

        textPane = new JTextPane();
        textPane.setOpaque(false); // 背景透明化
        textPane.setForeground(Color.BLACK);
        textPane.setFont(new Font("Serif", Font.BOLD, 24));
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JButton loadButton = new JButton("お天気を読み込むニャ");
        JButton nextLineButton = new JButton("次の天気を見せるニャ");
        nextLineButton.setEnabled(false);

        Font buttonFont = new Font("Meiryo", Font.BOLD, 30);
        loadButton.setFont(buttonFont);
        nextLineButton.setFont(buttonFont);
        loadButton.setMargin(new Insets(10, 20, 10, 20));
        nextLineButton.setMargin(new Insets(10, 20, 10, 20));

        loadButton.addActionListener(_ -> {
            forecastList = fetchForecastList();
            forecastIndex = 0;
            textPane.setText("");
            appendWithHighlight("読み込み完了ニャ！\nボタンを押すと順番に表示するニャ～\n", Color.ORANGE);
            nextLineButton.setEnabled(true);
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex < forecastList.size()) {
                String line = forecastList.get(forecastIndex);
                Color color = line.contains("データがない") ? Color.GRAY
                        : line.contains("雨") ? Color.BLUE.darker()
                                : line.contains("晴") ? Color.ORANGE.darker() : Color.BLACK;
                appendWithHighlight(line, color);
                forecastIndex++;
            } else {
                appendWithHighlight("もう全部出したニャ。\n", Color.MAGENTA);
                nextLineButton.setEnabled(false);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(loadButton);
        buttonPanel.add(nextLineButton);

        backgroundPanel.add(scrollPane, BorderLayout.CENTER);
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.setContentPane(backgroundPanel);
        frame.setVisible(true);
    }

    private static void appendWithHighlight(String text, Color color) {
        StyledDocument doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("Style_" + color.toString(), null);
        StyleConstants.setForeground(style, color);

        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static List<String> fetchForecastList() {
        List<String> result = new ArrayList<>();
        HttpURLConnection connection = null;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = today.plusDays(1);

        int[] targetHours = { 0, 6, 12, 18 };
        List<LocalDateTime> targetTimes = new ArrayList<>();

        for (int h : targetHours) {
            targetTimes.add(today.withHour(h));
        }
        for (int h : targetHours) {
            targetTimes.add(tomorrow.withHour(h));
        }

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

                JSONArray timeDefinesArray = timeSeriesObj.getJSONArray("timeDefines");
                JSONArray areasArray = timeSeriesObj.getJSONArray("areas");
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                java.util.Map<LocalDateTime, String> weatherMap = new java.util.HashMap<>();
                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    LocalDateTime dt = LocalDateTime.parse(timeDefinesArray.getString(i),
                            DateTimeFormatter.ISO_DATE_TIME);
                    if (i < weathersArray.length()) {
                        weatherMap.put(dt, weathersArray.getString(i));
                    }
                }

                result.add("=== " + today.format(DateTimeFormatter.ofPattern("M月d日")) + "・" +
                        tomorrow.format(DateTimeFormatter.ofPattern("d日")) + "の大阪の天気（6時間ごと）ニャ ===");

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d H時");

                for (LocalDateTime targetTime : targetTimes) {
                    String dateStr = targetTime.format(fmt);
                    String weather = weatherMap.get(targetTime);
                    if (weather == null || weather.isEmpty()) {
                        result.add(dateStr + " の大阪のお天気はデータがないニャ…");
                    } else {
                        result.add(formatCatStyle(dateStr, weather));
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

    private static String formatCatStyle(String date, String weather) {
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
        return date + " の大阪のお天気は「" + spokenWeather + "」" + tail;
    }
}
//