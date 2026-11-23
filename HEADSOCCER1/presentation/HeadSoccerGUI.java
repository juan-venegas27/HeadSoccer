package presentation;

import domain.HeadSoccer;

import javax.swing.*;
import java.awt.*;

/**
 * Main window of the Head Soccer game.
 * Uses a CardLayout to switch between all application screens.
 * Contains the application entry point ({@link #main(String[])}).
 */
public class HeadSoccerGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String CARD_MENU       = "MENU";
    public static final String CARD_SETTINGS   = "SETTINGS";
    public static final String CARD_SELECTION  = "SELECTION";
    public static final String CARD_GAME       = "GAME";
    public static final String CARD_END        = "END";

    private final CardLayout cardLayout;
    private final JPanel     cardPanel;

    private final MainMenu        mainMenu;
    private final SettingsMenu    settingsMenu;
    private final PlayerSelection playerSelection;
    private GamePanel             gamePanel;
    private EndGamePanel          endGamePanel;

    /**
     * Constructs the main application window and initialises initial screens.
     */
    public HeadSoccerGUI() {
        super("Head Soccer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        mainMenu        = new MainMenu(this);
        settingsMenu    = new SettingsMenu(this);
        playerSelection = new PlayerSelection(this);

        cardPanel.add(mainMenu,        CARD_MENU);
        cardPanel.add(settingsMenu,    CARD_SETTINGS);
        cardPanel.add(playerSelection, CARD_SELECTION);

        add(cardPanel);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Shows the main menu screen and stops any running game timers.
     */
    public void openMenu() {
        if (gamePanel != null) {
            gamePanel.stopGame();
        }
        cardLayout.show(cardPanel, CARD_MENU);
    }

    /**
     * Shows the settings menu screen.
     */
    public void openSettings() {
        cardLayout.show(cardPanel, CARD_SETTINGS);
    }

    /**
     * Shows the player selection screen.
     */
    public void openPlayerSelection() {
        cardLayout.show(cardPanel, CARD_SELECTION);
    }

    /**
     * Creates a fresh domain model, initialises game panels, and starts the match.
     * Safely cleans up any prior game instances and timers.
     */
    public void openGame() {
        if (gamePanel != null) {
            gamePanel.stopGame();
            cardPanel.remove(gamePanel);
        }
        if (endGamePanel != null) {
            cardPanel.remove(endGamePanel);
        }

        HeadSoccer model = new HeadSoccer(900, 540);
        endGamePanel = new EndGamePanel(this, model);
        gamePanel    = new GamePanel(this, model);

        cardPanel.add(gamePanel,    CARD_GAME);
        cardPanel.add(endGamePanel, CARD_END);

        cardLayout.show(cardPanel, CARD_GAME);
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    /**
     * Shows the end-game results screen and stops the game loop.
     */
    public void openEndGame() {
        if (gamePanel != null) {
            gamePanel.stopGame();
        }
        endGamePanel.refresh();
        cardLayout.show(cardPanel, CARD_END);
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeadSoccerGUI gui = new HeadSoccerGUI();
            gui.setVisible(true);
        });
    }
}
