import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WeatherForecastPanel extends JPanel {
    private BufferedImage backgroundImage;
    private final JTextPane textPane;

    public WeatherForecastPanel() {
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Meiryo", Font.PLAIN, 20));
        textPane.setOpaque(false); // 背景を透過
        textPane.setMargin(new Insets(10, 10, 10, 10));
        textPane.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "天気予報",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Meiryo", Font.BOLD, 16),
                Color.DARK_GRAY));

        add(scrollPane, BorderLayout.CENTER);
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public void setBackgroundImage(BufferedImage img) {
        this.backgroundImage = img;
        repaint(); // 背景変更を反映
    }

    public void setTextColor(Color color) {
        textPane.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            g2d.dispose();
        }
    }
}