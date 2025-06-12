import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class WeatherForecastFrame extends JFrame {
    private final WeatherForecastPanel forecastPanel;
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

        forecastPanel = new WeatherForecastPanel();
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
            forecastPanel.setBackgroundImage(null);
            forecastPanel.repaint();
            nextLineButton.setEnabled(false);
            prevLineButton.setEnabled(false);
            return;
        }

        for (String forecast : detailedForecasts) {
            forecastList.add(forecast);
            weatherDescriptions.add(extractWeather(forecast));
        }

        forecastIndex = 0;
        showForecastAtIndex(forecastIndex);
        updateButtonStates();

        updateButtonStates();
    }

    private void onNextLine() {
        if (forecastIndex < forecastList.size()) {
            showForecastAtIndex(forecastIndex);
            forecastIndex++;
            updateButtonStates();
        } else {
            textPane.setText("これ以上の天気情報はありません。");
            forecastPanel.setBackgroundImage(null);
            forecastPanel.setTextColor(Color.BLACK);
            forecastPanel.repaint();
            nextLineButton.setEnabled(false);
        }
    }

    private void onPrevLine() {
        if (forecastIndex > 1) {
            forecastIndex--;
            showForecastAtIndex(forecastIndex - 1);
            updateButtonStates();
        }
    }

    private void showForecastAtIndex(int index) {
        if (index >= 0 && index < forecastList.size()) {
            String forecast = forecastList.get(index);
            String weather = extractWeather(forecast);
            WeatherForecastUtil.setStyledText(textPane, forecast);
            forecastPanel.setBackgroundImage(WeatherForecastUtil.getBackgroundImageForWeather(weather));
            forecastPanel.setTextColor(Color.BLACK);
            forecastPanel.repaint();
        }
    }

    private String extractWeather(String forecast) {
        for (String line : forecast.split("\r?\n")) {
            if (line.startsWith("天気: ")) {
                String weatherValue = line.substring(4).trim();
                return weatherValue.isEmpty() ? "" : weatherValue.split("[ 　\t]")[0];
            }
        }
        return "";
    }

    private void updateButtonStates() {
        prevLineButton.setEnabled(forecastIndex > 1);
        nextLineButton.setEnabled(forecastIndex < forecastList.size());
    }
}