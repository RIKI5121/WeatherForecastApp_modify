import javax.swing.*;

public class WeatherForecastApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeatherForecastFrame frame = new WeatherForecastFrame();
            frame.setVisible(true);
        });
    }
}
