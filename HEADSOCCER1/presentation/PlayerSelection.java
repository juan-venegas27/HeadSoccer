package presentation;

import javax.swing.*;
import java.awt.*;

/**
 * The player selection screen. Displays both players and allows starting the game.
 */
public class PlayerSelection extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final JButton startButton;
    private final JButton backButton;

    /**
     * Constructs the player selection panel.
     *
     * @param gui the main application window
     */
    public PlayerSelection(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setBackground(new Color(20, 20, 60));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("SELECCION DE JUGADORES", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel playersPanel = new JPanel(new GridLayout(1, 2, 40, 0));
        playersPanel.setBackground(new Color(20, 20, 60));
        playersPanel.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));
        playersPanel.add(createPlayerCard("Jugador 1", Color.BLUE, "WASD + S=golpe"));
        playersPanel.add(createPlayerCard("Jugador 2", Color.RED, "Flechas + abajo=golpe"));
        add(playersPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        buttonPanel.setBackground(new Color(20, 20, 60));

        startButton = createButton("INICIAR PARTIDA");
        backButton  = createButton("REGRESAR");
        startButton.addActionListener(e -> startGame());
        backButton.addActionListener(e -> goBack());

        buttonPanel.add(backButton);
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createPlayerCard(String name, Color color, String controls) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2 - 10;

                g2.setColor(color);
                g2.fillOval(cx - 35, cy - 60, 70, 70);
                g2.setColor(color.darker());
                g2.drawOval(cx - 35, cy - 60, 70, 70);

                g2.setColor(color);
                g2.fillOval(cx - 12, cy + 10, 24, 24);
                g2.setColor(color.darker());
                g2.drawOval(cx - 12, cy + 10, 24, 24);
            }
        };
        card.setPreferredSize(new Dimension(200, 220));
        card.setBackground(new Color(30, 30, 80));
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        card.setLayout(new BorderLayout());

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        card.add(nameLabel, BorderLayout.NORTH);

        JLabel ctrlLabel = new JLabel("<html><center>" + controls + "</center></html>", SwingConstants.CENTER);
        ctrlLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        ctrlLabel.setForeground(Color.LIGHT_GRAY);
        ctrlLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 8, 5));
        card.add(ctrlLabel, BorderLayout.SOUTH);

        return card;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 48));
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(new Color(60, 120, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Starts the game and navigates to the game panel. */
    public void startGame() {
        gui.openGame();
    }

    /** Returns to the main menu. */
    public void goBack() {
        gui.openMenu();
    }
}
