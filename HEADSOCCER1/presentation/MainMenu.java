package presentation;

import javax.swing.*;
import java.awt.*;

/**
 * The main menu screen of Head Soccer.
 * Displays the title and navigation buttons.
 */
public class MainMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final JButton playButton;
    private final JButton settingsButton;
    private final JButton exitButton;

    /**
     * Constructs the main menu panel.
     *
     * @param gui the main application window
     */
    public MainMenu(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setBackground(new Color(20, 20, 60));
        setLayout(new GridBagLayout());

        playButton     = createButton("JUGAR");
        settingsButton = createButton("CONFIGURACION");
        exitButton     = createButton("SALIR");

        playButton.addActionListener(e -> openPlayerSelection());
        settingsButton.addActionListener(e -> openSettings());
        exitButton.addActionListener(e -> exitGame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(12, 0, 12, 0);

        JLabel title = new JLabel("HEAD SOCCER");
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 40, 0);
        add(title, gbc);

        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridy = 1; add(playButton,     gbc);
        gbc.gridy = 2; add(settingsButton, gbc);
        gbc.gridy = 3; add(exitButton,     gbc);
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(220, 48));
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setBackground(new Color(60, 120, 200));
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
