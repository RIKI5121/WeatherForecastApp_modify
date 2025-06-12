import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class WeatherForecastFrame extends JFrame {
    private final ForecastPanel forecastPanel;
    private final JTextPane textPane;
    private final JTextField regionInput;
    private final JButton showInputButton, prevLineButton, nextLineButton;
    private final List<String> forecastList = new ArrayList<>();
    private final List<String> weatherDescriptions = new ArrayList<>();
    private int forecastIndex = 0;

    public WeatherForecastFrame() {
        super("天気予報アプリ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        inputPanel.setBackground(new Color(230, 230, 250));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "地域入力"));

        regionInput = new JTextField(15);
        regionInput.setFont(new Font("Meiryo", Font.PLAIN, 18));
        inputPanel.add(new JLabel("都道府県:"));
        inputPanel.add(regionInput);

        showInputButton = new JButton("表示");
        showInputButton.setFont(new Font("Meiryo", Font.BOLD, 18));
        inputPanel.add(showInputButton);

        prevLineButton = new JButton("前の天気を表示");
        prevLineButton.setFont(new Font("Meiryo", Font.BOLD, 18));
        prevLineButton.setEnabled(false);
        inputPanel.add(prevLineButton);

        nextLineButton = new JButton("次の天気を表示");
        nextLineButton.setFont(new Font("Meiryo", Font.BOLD, 18));
        nextLineButton.setEnabled(false);
        inputPanel.add(nextLineButton);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        forecastPanel = new ForecastPanel();
        textPane = forecastPanel.getTextPane();
        mainPanel.add(forecastPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        showInputButton.addActionListener(_ -> onShowInput());
        nextLineButton.addActionListener(_ -> onNextLine());
        prevLineButton.addActionListener(_ -> onPrevLine());
    }

    private void onShowInput() {
        String inputRegion = regionInput.getText().trim();
        if (!WeatherForecastUtil.REGION_CODES.containsKey(inputRegion)) {
            JOptionPane.showMessageDialog(this, "正しい都道府県名を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }
        forecastList.clear();
        weatherDescriptions.clear();
        List<String> detailedForecasts = WeatherForecastUtil.fetchDetailedForecast(inputRegion);
        if (detailedForecasts.isEmpty()) {
            textPane.setText("天気情報が取得できませんでした。");
            nextLineButton.setEnabled(false);
            prevLineButton.setEnabled(false);
            forecastPanel.setBackgroundImage(null);
            forecastPanel.repaint();
            return;
        }
        for (String forecast : detailedForecasts) {
            forecastList.add(forecast);
            String weatherLine = null;
            for (String line : forecast.split("\r?\n")) { // OS依存の改行混在対策
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
        forecastIndex = 0;
        WeatherForecastUtil.setStyledText(textPane, forecastList.get(0));
        forecastPanel.setBackgroundImage(WeatherForecastUtil.getBackgroundImageForWeather(weatherDescriptions.get(0)));
        forecastPanel.setTextColor(Color.BLACK);
        forecastPanel.repaint();
        forecastIndex = 1;
        nextLineButton.setEnabled(forecastList.size() > 1);
        prevLineButton.setEnabled(false);
    }

    private void onNextLine() {
        if (forecastIndex < forecastList.size()) {
            WeatherForecastUtil.setStyledText(textPane, forecastList.get(forecastIndex));
            String weatherLine = "";
            for (String line : forecastList.get(forecastIndex).split("\r?\n")) { // OS依存の改行混在対策
                if (line.startsWith("天気: ")) {
                    String weatherValue = line.substring(4).trim();
                    if (!weatherValue.isEmpty()) {
                        weatherLine = weatherValue.split("[ 　\t]")[0];
                    }
                    break;
                }
            }
            forecastPanel.setBackgroundImage(WeatherForecastUtil.getBackgroundImageForWeather(weatherLine));
            forecastPanel.setTextColor(Color.BLACK);
            forecastPanel.repaint();
            forecastIndex++;
            prevLineButton.setEnabled(forecastIndex > 1);
            nextLineButton.setEnabled(forecastIndex < forecastList.size());
        } else {
            textPane.setText("これ以上の天気情報はありません。");
            forecastPanel.setTextColor(Color.BLACK);
            nextLineButton.setEnabled(false);
            forecastPanel.setBackgroundImage(null);
            forecastPanel.repaint();
        }
    }

    private void onPrevLine() {
        if (forecastIndex > 1) {
            forecastIndex--;
            WeatherForecastUtil.setStyledText(textPane, forecastList.get(forecastIndex - 1));
            String weatherLine = "";
            for (String line : forecastList.get(forecastIndex - 1).split("\r?\n")) { // OS依存の改行混在対策
                if (line.startsWith("天気: ")) {
                    String weatherValue = line.substring(4).trim();
                    if (!weatherValue.isEmpty()) {
                        weatherLine = weatherValue.split("[ 　\t]")[0];
                    }
                    break;
                }
            }
            forecastPanel.setBackgroundImage(WeatherForecastUtil.getBackgroundImageForWeather(weatherLine));
            forecastPanel.setTextColor(Color.BLACK);
            forecastPanel.repaint();
            prevLineButton.setEnabled(forecastIndex > 1);
            nextLineButton.setEnabled(forecastIndex < forecastList.size());
        }
    }
}
