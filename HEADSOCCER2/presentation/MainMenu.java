package presentation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Main menu screen for starting, opening, or configuring a match. */
public class MainMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private JButton playButton;
    private JButton openButton;
    private JButton settingsButton;
    private JButton exitButton;

    /**
     * Creates the main menu panel.
     * @param gui main application frame
     */
    public MainMenu(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setLayout(new GridBagLayout());

        playButton = createButton("JUGAR", new Color(40, 145, 75));
        openButton = createButton("ABRIR PARTIDA", new Color(50, 115, 200));
        settingsButton = createButton("CONFIGURACION", new Color(60, 120, 200));
        exitButton = createButton("SALIR", new Color(180, 55, 55));

        playButton.addActionListener(e -> openPlayerSelection());
        openButton.addActionListener(e -> gui.openSavedGame());
        settingsButton.addActionListener(e -> openSettings());
        exitButton.addActionListener(e -> exitGame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        JLabel title = new JLabel("DOPOSoccer");
        title.setFont(new Font("Arial", Font.BOLD, 54));
        title.setForeground(new Color(255, 215, 40));

        JLabel subtitle = new JLabel("HEAD SOCCER 2026-2");
        subtitle.setFont(new Font("Arial", Font.BOLD, 18));
        subtitle.setForeground(Color.WHITE);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 32, 0);
        add(subtitle, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridy = 2; add(playButton, gbc);
        gbc.gridy = 3; add(openButton, gbc);
        gbc.gridy = 4; add(settingsButton, gbc);
        gbc.gridy = 5; add(exitButton, gbc);
    }

    /**
     * Paints the stadium background overlay behind the main menu.
     * @param g graphics context used for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage bg = AssetManager.getStadiumImage();
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g.setColor(new Color(10, 16, 40, 155));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Creates a styled menu action button.
     * @param text text to display
     * @param bg button background color
     * @return configured button component
     */
    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(250, 46));
        btn.setFont(new Font("Arial", Font.BOLD, 17));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Navigates to the player selection screen. */
    public void openPlayerSelection() {
        gui.openPlayerSelection();
    }

    /** Navigates to the settings screen. */
    public void openSettings() {
        gui.openSettings();
    }

    /** Exits the application. */
    public void exitGame() {
        System.exit(0);
    }
}
