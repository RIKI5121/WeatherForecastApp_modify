import javax.swing.*;
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
            backgroundImage = new ImageIcon(getClass().getResource("/img/background.png")).getImage();

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("大阪のお天気（ネコ風）");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        // 背景画像パネル（画像ファイル名は "background.jpg" など）
        BackgroundPanel backgroundPanel = new BackgroundPanel("background.jpg");

        JTextArea textArea = new JTextArea();
        textArea.setOpaque(false); // 背景透明化
        textArea.setForeground(Color.BLACK); // 文字色
        textArea.setFont(new Font("Serif", Font.BOLD, 24));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JButton loadButton = new JButton("お天気を読み込むニャ");
        JButton nextLineButton = new JButton("次の天気を見せるニャ");
        nextLineButton.setEnabled(false);

        loadButton.addActionListener(_ -> {
            forecastList = fetchForecastList();
            forecastIndex = 0;
            textArea.setText("読み込み完了ニャ！\nボタンを押すと順番に表示するニャ～\n");
            nextLineButton.setEnabled(true);
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex < forecastList.size()) {
                textArea.append(forecastList.get(forecastIndex) + "\n");
                forecastIndex++;
            } else {
                textArea.append("もう全部出したニャ。\n");
                nextLineButton.setEnabled(false);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false); // 背景透明化
        buttonPanel.add(loadButton);
        buttonPanel.add(nextLineButton);

        backgroundPanel.add(scrollPane, BorderLayout.CENTER);
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.setContentPane(backgroundPanel);
        frame.setVisible(true);
    }    

    private static List<String> fetchForecastList() {
        List<String> result = new ArrayList<>();
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

                JSONArray timeDefinesArray = timeSeriesObj.getJSONArray("timeDefines");
                JSONArray areasArray = timeSeriesObj.getJSONArray("areas");
                JSONArray weathersArray = areasArray.getJSONObject(0).getJSONArray("weathers");

                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    String dateStr = timeDefinesArray.getString(i);
                    String weather = weathersArray.getString(i);
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                    String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                    result.add(formatCatStyle(formattedDate, weather));
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