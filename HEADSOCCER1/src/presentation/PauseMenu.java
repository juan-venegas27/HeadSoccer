package src.presentation;

import javax.swing.*;
import java.awt.*;

/**
 * The pause menu overlay panel displayed when the game is paused.
 * Provides options to continue, save match, open match, finish match, or return to the main menu.
 */
public class PauseMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private GamePanel gamePanel;

    private JButton continueButton;
    private JButton saveButton;
    private JButton openButton;
    private JButton finishMatchButton;
    private JButton mainMenuButton;

    /**
     * Constructs the pause menu panel.
     *
     * @param gui       the main application window
     * @param gamePanel the game panel (to resume or save the game)
     */
    public PauseMenu(HeadSoccerGUI gui, GamePanel gamePanel) {
        this.gui       = gui;
        this.gamePanel = gamePanel;

        setOpaque(false);
        setLayout(new GridBagLayout());

        continueButton    = createButton("CONTINUAR", new Color(40, 145, 75));
        saveButton        = createButton("GUARDAR PARTIDA", new Color(55, 120, 200));
        openButton        = createButton("ABRIR PARTIDA", new Color(55, 120, 200));
        finishMatchButton = createButton("TERMINAR PARTIDA", new Color(200, 120, 35));
        mainMenuButton    = createButton("MENU PRINCIPAL", new Color(175, 55, 55));

        continueButton.addActionListener(e -> resumeGame());
        saveButton.addActionListener(e -> gui.saveCurrentGame(gamePanel.getModel()));
        openButton.addActionListener(e -> gui.openSavedGame());
        finishMatchButton.addActionListener(e -> finishMatchNow());
        mainMenuButton.addActionListener(e -> gui.openMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        JLabel title = new JLabel("PAUSA");
        title.setFont(new Font("Arial", Font.BOLD, 34));
        title.setForeground(new Color(255, 215, 40));
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 18, 0);
        add(title, gbc);

        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridy = 1; add(continueButton,    gbc);
        gbc.gridy = 2; add(saveButton,        gbc);
        gbc.gridy = 3; add(openButton,        gbc);
        gbc.gridy = 4; add(finishMatchButton, gbc);
        gbc.gridy = 5; add(mainMenuButton,    gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(240, 42));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Resumes the game by hiding the pause menu and unpausing. */
    public void resumeGame() {
        gamePanel.resumeGame();
    }

    /** Terminates the current match immediately and shows the final result. */
    public void finishMatchNow() {
        gui.openEndGame();
    }
}
