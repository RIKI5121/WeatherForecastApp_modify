import javax.swing.*;
import javax.swing.text.*;
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

    private static String currentRegion = "大阪府";
    private static List<String> forecastList = new ArrayList<>();
    private static int forecastIndex = 0;

    private static JTextPane textPane;
    private static JButton nextLineButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("お天気ネコちゃん");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600, 1000);

        JPanel backgroundPanel = new JPanel();
        backgroundPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel controlPanel = new JPanel();
        controlPanel.setOpaque(false);

        nextLineButton = new JButton("決定ニャ");
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

        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        JTextField regionInput = new JTextField(20); // ← 検索欄を広く
        regionInput.setFont(new Font("Meiryo", Font.PLAIN, 20));

        // 地域を打った後、エンターキーで切り替えれる
        regionInput.addActionListener(_ -> {
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
        Style style = textPane.addStyle("CatStyle", null);
        StyleConstants.setForeground(style, new Color(30, 30, 30));
        StyleConstants.setFontSize(style, 24);
        StyleConstants.setBold(style, true);

        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static List<String> fetchForecastList(String region) {
        List<String> result = new ArrayList<>();
        try {
            String code = REGION_CODES.get(region);
            URI uri = URI.create("https://www.jma.go.jp/bosai/forecast/data/overview_forecast/" + code + ".json");
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

            JSONObject json = new JSONObject(jsonText.toString());
            String text = json.getString("text");

            for (String line : text.split("\n")) {
                if (!line.isBlank()) {
                    result.add(formatCatStyle(line));
                }
            }

        } catch (IOException | JSONException e) {
            result.add("天気情報の取得に失敗したニャ: " + e.getMessage());
        }
        return result;
    }

    private static String formatCatStyle(String input) {
        return input
                .replaceAll("です", "だニャ")
                .replaceAll("ます", "まス")
                .replaceAll("。", "ニャ。")
                .replaceAll("でしょう", "かもしれニャい")
                .replaceAll("ください", "してほしいニャ")
                .replaceAll("あります", "あるニャ")
                .replaceAll("予想", "にゃん予想")
                .replaceAll("影響", "影響ニャ")
                .replaceAll("可能性", "にゃん可能性")
                .replaceAll("おそれ", "おそれニャ")
                .replaceAll("見込み", "見込みニャ") + "～";
    }
}