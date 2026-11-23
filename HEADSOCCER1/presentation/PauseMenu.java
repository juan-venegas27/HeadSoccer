package presentation;

import javax.swing.*;
import java.awt.*;

/**
 * The pause menu overlay panel displayed when the game is paused.
 * Provides options to continue, go to settings, or return to the main menu.
 */
public class PauseMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final GamePanel gamePanel;

    private final JButton continueButton;
    private final JButton settingsButton;
    private final JButton mainMenuButton;

    /**
     * Constructs the pause menu panel.
     *
     * @param gui       the main application window
     * @param gamePanel the game panel (to resume the game)
     */
    public PauseMenu(HeadSoccerGUI gui, GamePanel gamePanel) {
        this.gui       = gui;
        this.gamePanel = gamePanel;

        setOpaque(false);
        setLayout(new GridBagLayout());

        continueButton  = createButton("CONTINUAR");
        settingsButton  = createButton("CONFIGURACION");
        mainMenuButton  = createButton("MENU PRINCIPAL");

        continueButton.addActionListener(e -> resumeGame());
        settingsButton.addActionListener(e -> gui.openSettings());
        mainMenuButton.addActionListener(e -> gui.openMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel title = new JLabel("PAUSA");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 25, 0);
        add(title, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridy = 1; add(continueButton,  gbc);
        gbc.gridy = 2; add(settingsButton,  gbc);
        gbc.gridy = 3; add(mainMenuButton,  gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
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

    /** Resumes the game by hiding the pause menu and unpausing. */
    public void resumeGame() {
        gamePanel.resumeGame();
    }
}
