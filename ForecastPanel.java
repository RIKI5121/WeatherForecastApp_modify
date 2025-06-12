import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ForecastPanel extends JPanel {
    private BufferedImage backgroundImage;
    private final JTextPane textPane;

    public ForecastPanel() {
        setLayout(new BorderLayout());
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Meiryo", Font.PLAIN, 20));
        textPane.setOpaque(false);
        textPane.setMargin(new Insets(10, 10, 10, 10));
        textPane.setForeground(Color.BLACK);
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

    public void setTextColor(Color color) {
        textPane.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
