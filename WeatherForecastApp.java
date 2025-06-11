import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
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

        ForecastPanel forecastPanel = new ForecastPanel();
        JTextPane textPane = forecastPanel.getTextPane();

        mainPanel.add(forecastPanel, BorderLayout.CENTER);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        List<String> forecastList = new ArrayList<>();
        List<String> weatherDescriptions = new ArrayList<>();
        final int[] forecastIndex = { 0 };

        showInputButton.addActionListener(_ -> {
            String inputRegion = regionInput.getText().trim();
            if (!REGION_CODES.containsKey(inputRegion)) {
                JOptionPane.showMessageDialog(frame, "正しい都道府県名を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }
            forecastList.clear();
            weatherDescriptions.clear();

            List<String> detailedForecasts = fetchDetailedForecast(inputRegion);
            if (detailedForecasts.isEmpty()) {
                textPane.setText("天気情報が取得できませんでした。");
                nextLineButton.setEnabled(false);
                forecastPanel.setBackgroundImage(null);
                forecastPanel.repaint();
                return;
            }

            for (String forecast : detailedForecasts) {
                forecastList.add(forecast);
                String weatherLine = null;
                for (String line : forecast.split("\n")) {
                    if (line.startsWith("天気: ")) {
                        String weatherValue = line.substring(4).trim();
                        if (!weatherValue.isEmpty()) {
                            weatherLine = weatherValue.split("[ 　\t]")[0];
                        } else {
                            weatherLine = "";
                        }
                        break;
                    }
                }
                weatherDescriptions.add(weatherLine != null ? weatherLine : "");
            }

            forecastIndex[0] = 0;
            textPane.setText(forecastList.get(0));
            forecastPanel.setBackgroundImage(getBackgroundImageForWeather(weatherDescriptions.get(0)));
            forecastPanel.repaint();

            forecastIndex[0] = 1;
            nextLineButton.setEnabled(forecastList.size() > 1);
        });

        nextLineButton.addActionListener(_ -> {
            if (forecastIndex[0] < forecastList.size()) {
                textPane.setText(forecastList.get(forecastIndex[0]));
                String weatherLine = "";
                for (String line : forecastList.get(forecastIndex[0]).split("\n")) {
                    if (line.startsWith("天気: ")) {
                        String weatherValue = line.substring(4).trim();
                        if (!weatherValue.isEmpty()) {
                            weatherLine = weatherValue.split("[ 　\t]")[0];
                        }
                        break;
                    }
                }
                forecastPanel.setBackgroundImage(getBackgroundImageForWeather(weatherLine));
                forecastPanel.repaint();
                forecastIndex[0]++;
            } else {
                textPane.setText("これ以上の天気情報はありません。");
                nextLineButton.setEnabled(false);
                forecastPanel.setBackgroundImage(null);
                forecastPanel.repaint();
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
                    String tempDate = tempTimes.get(j).substring(0, 10);
                    String tempHour = tempTimes.get(j).substring(11, 13);
                    if (tempDate.equals(dateTime.substring(0, 10)) && tempHour.equals("09")) {
                        tempInfo = "最高気温: " + temps.get(j) + "℃\n";
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
            e.printStackTrace();
            result.add("天気情報の取得中にエラーが発生しました。");
        }
        return result;
    }

    private static List<String> getStringList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray == null)
            return list;
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.optString(i, ""));
        }
        return list;
    }

    private static String getEndTime(String startTime) {
        try {
            LocalTime start = LocalTime.parse(startTime.substring(11));
            LocalTime end = start.plusHours(6);
            return end.toString().substring(0, 5);
        } catch (Exception e) {
            return "--:--";
        }
    }

    private static String formatDateTime(String dateTime) {
        try {
            LocalDateTime dt = LocalDateTime.parse(dateTime);
            return dt.format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm"));
        } catch (Exception e) {
            return dateTime;
        }
    }

    private static BufferedImage getBackgroundImageForWeather(String weather) {
        String imgPath;
        if (weather != null && (weather.contains("くもり") || weather.contains("曇"))) {
            imgPath = "backgrounds/cloudy.jpg";
        } else if (weather != null && (weather.contains("晴れ") || weather.contains("晴"))) {
            imgPath = "backgrounds/sunny.jpg";
        } else if (weather != null && weather.contains("雨")) {
            imgPath = "backgrounds/rainy.jpg";
        } else if (weather != null && weather.contains("雪")) {
            imgPath = "backgrounds/snowy.jpg";
        } else {
            imgPath = "backgrounds/default.jpg";
        }

        try {
            return ImageIO.read(new File(imgPath));
        } catch (IOException e) {
            System.err.println("背景画像の読み込みに失敗しました: " + imgPath);
            return null;
        }
    }

    // 天気予報欄にだけ背景画像を描画するパネル
    static class ForecastPanel extends JPanel {
        private BufferedImage backgroundImage;
        private JTextPane textPane;

        public ForecastPanel() {
            setLayout(new BorderLayout());
            textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setFont(new Font("Meiryo", Font.PLAIN, 20));
            textPane.setOpaque(false);
            textPane.setMargin(new Insets(10, 10, 10, 10));

            JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY), "天気予報"));
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);

            add(scrollPane, BorderLayout.CENTER);
        }

        public JTextPane getTextPane() {
            return textPane;
        }

        public void setBackgroundImage(BufferedImage img) {
            this.backgroundImage = img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}
