package presentation;

import controller.GameController;
import domain.HeadSoccer;
import domain.HeadSoccerException;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Main application window managing screens and match navigation. */
public class HeadSoccerGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String CARD_MENU = "MENU";
    public static final String CARD_SETTINGS = "SETTINGS";
    public static final String CARD_SELECTION = "SELECTION";
    public static final String CARD_GAME = "GAME";
    public static final String CARD_END = "END";

    private GameController controller;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    private MainMenu mainMenu;
    private SettingsMenu settingsMenu;
    private PlayerSelection playerSelection;
    private GamePanel gamePanel;
    private EndGamePanel endGamePanel;

    /** Creates the main window and initializes all screens. */
    public HeadSoccerGUI() {
        super("DOPOSoccer 2026-2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        controller = new GameController();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        mainMenu = new MainMenu(this);
        settingsMenu = new SettingsMenu(this);
        playerSelection = new PlayerSelection(this);

        cardPanel.add(mainMenu, CARD_MENU);
        cardPanel.add(settingsMenu, CARD_SETTINGS);
        cardPanel.add(playerSelection, CARD_SELECTION);

        add(cardPanel);

        applyResolution(controller.getFieldWidth(), controller.getFieldHeight());
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** Saves current player stats and points through the controller. */
    public void saveProgressSafely() {
        controller.saveProgress();
    }

    /** Applies one of the field resolutions and resizes panels cleanly. */
    public void applyResolution(int width, int height) {
        controller.setResolution(width, height);
        Dimension dim = new Dimension(width, height);
        mainMenu.setPreferredSize(dim);
        settingsMenu.setPreferredSize(dim);
        playerSelection.setPreferredSize(dim);
        cardPanel.setPreferredSize(dim);
        pack();
        setLocationRelativeTo(null);
    }

    /** Shows the main menu screen and stops any running game timers. */
    public void openMenu() {
        if (gamePanel != null) {
            gamePanel.stopGame();
        }
        cardLayout.show(cardPanel, CARD_MENU);
    }

    /** Shows the settings menu screen. */
    public void openSettings() {
        settingsMenu.syncResolutionFromGUI();
        cardLayout.show(cardPanel, CARD_SETTINGS);
    }

    /** Shows the player selection screen and refreshes stats/points. */
    public void openPlayerSelection() {
        playerSelection.refreshUI();
        cardLayout.show(cardPanel, CARD_SELECTION);
    }

    /** Creates a new match through the controller and opens the game panel. */
    public void openGame() {
        try {
            HeadSoccer model = controller.startNewMatch();
            launchModel(model);
        } catch (HeadSoccerException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de configuracion", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Initializes and displays the game panel for the given match model. */
    private void launchModel(HeadSoccer model) {
        if (gamePanel != null) {
            gamePanel.stopGame();
            cardPanel.remove(gamePanel);
        }
        if (endGamePanel != null) {
            cardPanel.remove(endGamePanel);
        }

        applyResolution(model.getField().getWidth(), model.getField().getHeight());

        endGamePanel = new EndGamePanel(this, model);
        gamePanel = new GamePanel(this, controller);

        cardPanel.add(gamePanel, CARD_GAME);
        cardPanel.add(endGamePanel, CARD_END);

        cardLayout.show(cardPanel, CARD_GAME);
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    /** Saves the current active match to a file chosen by the user. */
    public void saveCurrentGame(HeadSoccer model) {
        if (model == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar partida DOPOSoccer");
        chooser.setSelectedFile(new File("partida.soccer"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                controller.saveMatch(model, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Partida guardada correctamente.",
                        "Guardar Partida", JOptionPane.INFORMATION_MESSAGE);
            } catch (HeadSoccerException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Error al Guardar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Opens a previously saved match file chosen by the user. */
    public void openSavedGame() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Abrir partida DOPOSoccer");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                HeadSoccer loaded = controller.loadMatch(chooser.getSelectedFile());
                launchModel(loaded);
            } catch (HeadSoccerException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Error al Abrir Partida", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Shows the end-game results screen, syncs earned points, and stops the game loop. */
    public void openEndGame() {
        if (gamePanel != null) {
            gamePanel.stopGame();
            controller.finishMatch();
        }
        endGamePanel.refresh();
        cardLayout.show(cardPanel, CARD_END);
    }

    /** Returns the MVC controller coordinating this interface. */
    public GameController getController() { return controller; }

    /** Returns configured field width in pixels. */
    public int getFieldWidth() { return controller.getFieldWidth(); }

    /** Returns selected match modality. */
    public int getModality() { return controller.getModality(); }

    /** Sets match modality constant. */
    public void setModality(int modality) { controller.setModality(modality); }

    /** Returns selected machine profile. */
    public String getMachineProfile() { return controller.getMachineProfile(); }

    /** Sets machine profile name. */
    public void setMachineProfile(String machineProfile) { controller.setMachineProfile(machineProfile); }

    /** Returns selected country for player 1. */
    public String getP1Country() { return controller.getP1Country(); }

    /** Sets country name for player 1. */
    public void setP1Country(String p1Country) { controller.setP1Country(p1Country); }

    /** Returns selected country for player 2. */
    public String getP2Country() { return controller.getP2Country(); }

    /** Sets country name for player 2. */
    public void setP2Country(String p2Country) { controller.setP2Country(p2Country); }

    /** Returns match victory mode. */
    public int getMatchType() { return controller.getMatchType(); }

    /** Sets match victory mode constant. */
    public void setMatchType(int matchType) { controller.setMatchType(matchType); }

    /** Returns configured match duration in minutes. */
    public int getMatchMinutes() { return controller.getMatchMinutes(); }

    /** Sets match duration in minutes. */
    public void setMatchMinutes(int matchMinutes) { controller.setMatchMinutes(matchMinutes); }

    /** Returns configured goal target. */
    public int getTargetGoals() { return controller.getTargetGoals(); }

    /** Sets goal target count. */
    public void setTargetGoals(int targetGoals) { controller.setTargetGoals(targetGoals); }

    /** Returns player 1 progression array. */
    public int[] getP1Progress() { return controller.getP1Progress(); }

    /** Returns player 2 progression array. */
    public int[] getP2Progress() { return controller.getP2Progress(); }

    /** Application entry point. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeadSoccerGUI gui = new HeadSoccerGUI();
            gui.setVisible(true);
        });
    }
}
