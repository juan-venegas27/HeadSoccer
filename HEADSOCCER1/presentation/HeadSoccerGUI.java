package presentation;

import domain.Field;
import domain.HeadSoccer;
import domain.HeadSoccerException;
import domain.Log;
import domain.MachinePlayer;
import domain.Power;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Main window of the DOPOSoccer game (Version 2).
 * Uses a CardLayout to switch between application screens, manages resolution changes,
 * persistent player progress (stats and points), and match save/open dialogs.
 */
public class HeadSoccerGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String CARD_MENU      = "MENU";
    public static final String CARD_SETTINGS  = "SETTINGS";
    public static final String CARD_SELECTION = "SELECTION";
    public static final String CARD_GAME      = "GAME";
    public static final String CARD_END       = "END";

    private static final File PROGRESS_FILE = new File("progress.dat");

    private CardLayout cardLayout;
    private JPanel     cardPanel;

    private MainMenu        mainMenu;
    private SettingsMenu    settingsMenu;
    private PlayerSelection playerSelection;
    private GamePanel       gamePanel;
    private EndGamePanel    endGamePanel;

    private int    fieldWidth     = Field.RES_MEDIUM_WIDTH;
    private int    fieldHeight    = Field.RES_MEDIUM_HEIGHT;
    private int    modality       = HeadSoccer.MODE_PVP;
    private String machineProfile = MachinePlayer.PROFILE_OFENSIVA;
    private String p1Country      = Power.COUNTRY_KOREA;
    private String p2Country      = Power.COUNTRY_CAMEROON;
    private int    matchType      = HeadSoccer.MATCH_BY_TIME;
    private int    matchMinutes   = 3;
    private int    targetGoals    = 5;

    // Persistent player progression: {speed, jump, kick, survival, points}
    private int[] p1Progress = {1, 1, 1, 1, 0};
    private int[] p2Progress = {1, 1, 1, 1, 0};

    /**
     * Constructs the main application window and initialises initial screens.
     */
    public HeadSoccerGUI() {
        super("DOPOSoccer 2026-2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        loadProgressSafely();

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        mainMenu        = new MainMenu(this);
        settingsMenu    = new SettingsMenu(this);
        playerSelection = new PlayerSelection(this);

        cardPanel.add(mainMenu,        CARD_MENU);
        cardPanel.add(settingsMenu,    CARD_SETTINGS);
        cardPanel.add(playerSelection, CARD_SELECTION);

        add(cardPanel);

        applyResolution(fieldWidth, fieldHeight);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void loadProgressSafely() {
        if (PROGRESS_FILE.exists()) {
            try {
                int[][] loaded = HeadSoccer.loadProgress(PROGRESS_FILE);
                if (loaded != null && loaded.length == 2) {
                    p1Progress = loaded[0];
                    p2Progress = loaded[1];
                }
            } catch (HeadSoccerException e) {
                Log.record(e);
            }
        }
    }

    public void saveProgressSafely() {
        try {
            HeadSoccer.saveProgress(PROGRESS_FILE, p1Progress, p2Progress);
        } catch (HeadSoccerException e) {
            Log.record(e);
        }
    }

    /** Applies one of the three field resolutions and resizes panels cleanly. */
    public void applyResolution(int width, int height) {
        this.fieldWidth  = width;
        this.fieldHeight = height;
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

    /** Creates a fresh domain model with the selected settings and starts the match. */
    public void openGame() {
        try {
            HeadSoccer.validateConfiguration(fieldWidth, fieldHeight, matchMinutes, targetGoals);
        } catch (HeadSoccerException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de configuracion", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HeadSoccer model = new HeadSoccer(
                fieldWidth, fieldHeight,
                modality, machineProfile,
                p1Country, p2Country,
                matchType, matchMinutes, targetGoals
        );
        model.getPlayer1().setProgress(p1Progress[0], p1Progress[1], p1Progress[2], p1Progress[3], p1Progress[4]);
        model.getPlayer2().setProgress(p2Progress[0], p2Progress[1], p2Progress[2], p2Progress[3], p2Progress[4]);

        launchModel(model);
    }

    private void launchModel(HeadSoccer model) {
        if (gamePanel != null) {
            gamePanel.stopGame();
            cardPanel.remove(gamePanel);
        }
        if (endGamePanel != null) {
            cardPanel.remove(endGamePanel);
        }

        this.fieldWidth  = model.getField().getWidth();
        this.fieldHeight = model.getField().getHeight();
        applyResolution(fieldWidth, fieldHeight);

        endGamePanel = new EndGamePanel(this, model);
        gamePanel    = new GamePanel(this, model);

        cardPanel.add(gamePanel,    CARD_GAME);
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
                model.save(chooser.getSelectedFile());
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
                HeadSoccer loaded = HeadSoccer.open(chooser.getSelectedFile());
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
            HeadSoccer model = gamePanel.getModel();
            if (!model.isMatchFinished()) {
                model.finish();
            }
            syncProgressFromModel(model);
        }
        endGamePanel.refresh();
        cardLayout.show(cardPanel, CARD_END);
    }

    private void syncProgressFromModel(HeadSoccer model) {
        if (model == null) return;
        p1Progress = new int[]{
            model.getPlayer1().getSpeedStat(),
            model.getPlayer1().getJumpStat(),
            model.getPlayer1().getKickStat(),
            model.getPlayer1().getSurvivalStat(),
            model.getPlayer1().getPoints()
        };
        p2Progress = new int[]{
            model.getPlayer2().getSpeedStat(),
            model.getPlayer2().getJumpStat(),
            model.getPlayer2().getKickStat(),
            model.getPlayer2().getSurvivalStat(),
            model.getPlayer2().getPoints()
        };
        saveProgressSafely();
    }

    public int getFieldWidth() { return fieldWidth; }
    public int getModality() { return modality; }
    public void setModality(int modality) { this.modality = modality; }
    public String getMachineProfile() { return machineProfile; }
    public void setMachineProfile(String machineProfile) { this.machineProfile = machineProfile; }
    public String getP1Country() { return p1Country; }
    public void setP1Country(String p1Country) { this.p1Country = p1Country; }
    public String getP2Country() { return p2Country; }
    public void setP2Country(String p2Country) { this.p2Country = p2Country; }
    public int getMatchType() { return matchType; }
    public void setMatchType(int matchType) { this.matchType = matchType; }
    public int getMatchMinutes() { return matchMinutes; }
    public void setMatchMinutes(int matchMinutes) { this.matchMinutes = matchMinutes; }
    public int getTargetGoals() { return targetGoals; }
    public void setTargetGoals(int targetGoals) { this.targetGoals = targetGoals; }
    public int[] getP1Progress() { return p1Progress; }
    public int[] getP2Progress() { return p2Progress; }

    /** Application entry point. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeadSoccerGUI gui = new HeadSoccerGUI();
            gui.setVisible(true);
        });
    }
}
