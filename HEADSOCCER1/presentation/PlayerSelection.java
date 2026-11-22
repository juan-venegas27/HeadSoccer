package presentation;

import domain.HeadSoccer;
import domain.HeadSoccerException;
import domain.MachinePlayer;
import domain.Player;
import domain.Power;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The match setup and character selection screen.
 * Allows configuring modality, machine profile, match type, characters/powers,
 * and upgrading player statistics with earned points.
 */
public class PlayerSelection extends JPanel {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private JComboBox<String> modalityBox;
    private JComboBox<String> profileBox;
    private JComboBox<String> matchTypeBox;
    private JComboBox<Integer> matchValueBox;

    private PlayerConfigCard card1;
    private PlayerConfigCard card2;

    private JButton startButton;
    private JButton backButton;

    /**
     * Constructs the player selection panel.
     *
     * @param gui the main application window
     */
    public PlayerSelection(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setLayout(new BorderLayout(8, 8));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);

        JLabel title = new JLabel("CONFIGURAR PARTIDO Y PERSONAJES", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(255, 215, 40));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        northPanel.add(title, BorderLayout.NORTH);

        JPanel optionsBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        optionsBar.setBackground(new Color(18, 25, 62, 220));
        optionsBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        modalityBox   = new JComboBox<>(new String[]{"Jugador vs Jugador", "Jugador vs Máquina"});
        profileBox    = new JComboBox<>(MachinePlayer.PROFILES);
        matchTypeBox  = new JComboBox<>(new String[]{"Por Tiempo (min)", "Por Goles"});
        matchValueBox = new JComboBox<>(new Integer[]{1, 2, 3, 5, 7, 10});
        matchValueBox.setSelectedItem(3);

        profileBox.setEnabled(false);
        modalityBox.addActionListener(e -> {
            boolean vsMachine = modalityBox.getSelectedIndex() == 1;
            profileBox.setEnabled(vsMachine);
            gui.setModality(vsMachine ? HeadSoccer.MODE_PVM : HeadSoccer.MODE_PVP);
            if (card2 != null) {
                card2.refreshTitle(vsMachine ? "Máquina (" + profileBox.getSelectedItem() + ")" : "Jugador 2");
            }
        });
        profileBox.addActionListener(e -> {
            String prof = (String) profileBox.getSelectedItem();
            gui.setMachineProfile(prof);
            if (modalityBox.getSelectedIndex() == 1 && card2 != null) {
                card2.refreshTitle("Máquina (" + prof + ")");
            }
        });
        matchTypeBox.addActionListener(e -> {
            boolean byGoals = matchTypeBox.getSelectedIndex() == 1;
            gui.setMatchType(byGoals ? HeadSoccer.MATCH_BY_GOALS : HeadSoccer.MATCH_BY_TIME);
            if (byGoals) {
                matchValueBox.setSelectedItem(gui.getTargetGoals());
            } else {
                matchValueBox.setSelectedItem(gui.getMatchMinutes());
            }
        });
        matchValueBox.addActionListener(e -> {
            Integer val = (Integer) matchValueBox.getSelectedItem();
            if (val != null) {
                if (matchTypeBox.getSelectedIndex() == 1) {
                    gui.setTargetGoals(val);
                } else {
                    gui.setMatchMinutes(val);
                }
            }
        });

        optionsBar.add(createOptionLabel("Modalidad:"));
        optionsBar.add(modalityBox);
        optionsBar.add(createOptionLabel("Perfil IA:"));
        optionsBar.add(profileBox);
        optionsBar.add(createOptionLabel("Tipo:"));
        optionsBar.add(matchTypeBox);
        optionsBar.add(createOptionLabel("Valor:"));
        optionsBar.add(matchValueBox);

        northPanel.add(optionsBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        JPanel playersPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        playersPanel.setOpaque(false);
        playersPanel.setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 24));

        card1 = new PlayerConfigCard("Jugador 1", new Color(45, 115, 225),
                gui.getP1Country(), gui.getP1Progress(), true);
        card2 = new PlayerConfigCard("Jugador 2", new Color(215, 60, 60),
                gui.getP2Country(), gui.getP2Progress(), false);

        playersPanel.add(card1);
        playersPanel.add(card2);
        add(playersPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setOpaque(false);

        backButton  = createButton("REGRESAR", new Color(75, 95, 140));
        startButton = createButton("INICIAR PARTIDA", new Color(40, 150, 75));
        backButton.addActionListener(e -> goBack());
        startButton.addActionListener(e -> startGame());

        buttonPanel.add(backButton);
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage bg = AssetManager.getStadiumImage();
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g.setColor(new Color(12, 18, 45, 180));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Refreshes displayed player stats, points, and selections. */
    public void refreshUI() {
        boolean vsMachine = gui.getModality() == HeadSoccer.MODE_PVM;
        modalityBox.setSelectedIndex(vsMachine ? 1 : 0);
        profileBox.setSelectedItem(gui.getMachineProfile());
        profileBox.setEnabled(vsMachine);
        card1.refreshTitle("Jugador 1");
        card2.refreshTitle(vsMachine ? "Máquina (" + gui.getMachineProfile() + ")" : "Jugador 2");
        card1.updateFromProgress(gui.getP1Progress());
        card2.updateFromProgress(gui.getP2Progress());
    }

    private JLabel createOptionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 42));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Starts the game and navigates to the game panel. */
    public void startGame() {
        gui.saveProgressSafely();
        gui.openGame();
    }

    /** Returns to the main menu. */
    public void goBack() {
        gui.openMenu();
    }

    private class PlayerConfigCard extends JPanel {

        private static final long serialVersionUID = 1L;

        private JLabel titleLabel;
        private JComboBox<String> countryBox;
        private JLabel powerInfoLabel;
        private JLabel pointsLabel;
        private final JLabel[] statLabels = new JLabel[4];
        private final JButton[] plusButtons = new JButton[4];
        private JPanel previewPanel;
        private final boolean isPlayer1;
        private int[] progress;

        PlayerConfigCard(String title, Color borderColor, String initialCountry,
                         int[] progress, boolean isPlayer1) {
            this.isPlayer1 = isPlayer1;
            this.progress  = progress;

            setBackground(new Color(22, 30, 72, 235));
            setBorder(BorderFactory.createLineBorder(borderColor, 2));
            setLayout(new BorderLayout(6, 6));

            JPanel header = new JPanel(new BorderLayout(4, 4));
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));

            titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 17));
            titleLabel.setForeground(Color.WHITE);
            header.add(titleLabel, BorderLayout.NORTH);

            countryBox = new JComboBox<>(Power.AVAILABLE_COUNTRIES);
            countryBox.setSelectedItem(initialCountry);
            countryBox.setFont(new Font("Arial", Font.BOLD, 13));
            countryBox.addActionListener(e -> onCountryChanged());
            header.add(countryBox, BorderLayout.SOUTH);

            add(header, BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(4, 4));
            center.setOpaque(false);

            previewPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    String c = (String) countryBox.getSelectedItem();
                    BufferedImage head = AssetManager.getCharacterHead(c);
                    int size = Math.min(getWidth() - 10, Math.min(getHeight() - 6, 86));
                    if (size > 0) {
                        int x = (getWidth() - size) / 2;
                        int y = (getHeight() - size) / 2;
                        g.drawImage(head, x, y, size, size, null);
                    }
                }
            };
            previewPanel.setOpaque(false);
            previewPanel.setPreferredSize(new Dimension(100, 92));
            center.add(previewPanel, BorderLayout.CENTER);

            powerInfoLabel = new JLabel("", SwingConstants.CENTER);
            powerInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            powerInfoLabel.setForeground(new Color(170, 225, 255));
            center.add(powerInfoLabel, BorderLayout.SOUTH);

            add(center, BorderLayout.CENTER);

            JPanel statsContainer = new JPanel(new BorderLayout(4, 4));
            statsContainer.setOpaque(false);
            statsContainer.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 10));

            pointsLabel = new JLabel("Puntos: 0", SwingConstants.CENTER);
            pointsLabel.setFont(new Font("Arial", Font.BOLD, 14));
            pointsLabel.setForeground(new Color(255, 215, 40));
            statsContainer.add(pointsLabel, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(4, 2, 6, 4));
            grid.setOpaque(false);
            String[] names = {"Speed", "Jump", "Kick", "Survival"};
            String[] keys  = {"speed", "jump", "kick", "survival"};

            for (int i = 0; i < 4; i++) {
                final String statKey = keys[i];

                statLabels[i] = new JLabel(names[i] + ": 1/10");
                statLabels[i].setFont(new Font("Arial", Font.BOLD, 12));
                statLabels[i].setForeground(Color.WHITE);

                plusButtons[i] = new JButton("+ (500)");
                plusButtons[i].setFont(new Font("Arial", Font.BOLD, 11));
                plusButtons[i].setBackground(new Color(50, 120, 200));
                plusButtons[i].setForeground(Color.WHITE);
                plusButtons[i].setFocusPainted(false);
                plusButtons[i].addActionListener(e -> upgradeStat(statKey));

                grid.add(statLabels[i]);
                grid.add(plusButtons[i]);
            }

            statsContainer.add(grid, BorderLayout.CENTER);
            add(statsContainer, BorderLayout.SOUTH);

            onCountryChanged();
            updateFromProgress(progress);
        }

        void refreshTitle(String newTitle) {
            titleLabel.setText(newTitle);
        }

        private void onCountryChanged() {
            String country = (String) countryBox.getSelectedItem();
            if (isPlayer1) {
                gui.setP1Country(country);
            } else {
                gui.setP2Country(country);
            }
            Power p = Power.createForCountry(country);
            powerInfoLabel.setText("Poder: " + p.getName() + " (" + p.getEffectName()
                    + " - Daño " + p.getBaseDamage() + ")");
            previewPanel.repaint();
        }

        private void upgradeStat(String statKey) {
            Player temp = new Player(0, 0, Color.BLUE, "Temp",
                    Power.createForCountry(Power.COUNTRY_KOREA), 1);
            temp.setProgress(progress[0], progress[1], progress[2], progress[3], progress[4]);
            try {
                temp.upgradeStat(statKey);
                progress[0] = temp.getSpeedStat();
                progress[1] = temp.getJumpStat();
                progress[2] = temp.getKickStat();
                progress[3] = temp.getSurvivalStat();
                progress[4] = temp.getPoints();
                gui.saveProgressSafely();
                updateFromProgress(progress);
            } catch (HeadSoccerException ex) {
                JOptionPane.showMessageDialog(PlayerSelection.this, ex.getMessage(),
                        "Mejora de Estadística", JOptionPane.WARNING_MESSAGE);
            }
        }

        void updateFromProgress(int[] prog) {
            this.progress = prog;
            String[] names = {"Speed", "Jump", "Kick", "Survival"};
            pointsLabel.setText("Puntos disponibles: " + prog[4]);
            for (int i = 0; i < 4; i++) {
                int lvl = prog[i];
                statLabels[i].setText(names[i] + ": " + lvl + "/10");
                if (lvl >= Player.MAX_STAT_LEVEL) {
                    plusButtons[i].setText("MAX");
                    plusButtons[i].setEnabled(false);
                } else {
                    int cost = Player.getUpgradeCost(lvl);
                    plusButtons[i].setText("+ (" + cost + " pts)");
                    plusButtons[i].setEnabled(true);
                }
            }
        }
    }
}
