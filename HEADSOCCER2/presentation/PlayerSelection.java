package presentation;

import domain.CharacterRegistry;
import domain.HeadSoccer;
import domain.HeadSoccerException;
import domain.MachinePlayer;
import domain.Power;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Match configuration and character selection screen. */
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
     * Creates the player selection and match setup screen.
     * @param gui main application frame
     */
    public PlayerSelection(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setLayout(new BorderLayout(8, 8));

        add(createNorthOptionsPanel(), BorderLayout.NORTH);

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

        backButton = createButton("REGRESAR", new Color(75, 95, 140));
        startButton = createButton("INICIAR PARTIDA", new Color(40, 150, 75));
        backButton.addActionListener(e -> goBack());
        startButton.addActionListener(e -> startGame());

        buttonPanel.add(backButton);
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Builds the top title and match configuration combo bar.
     * @return header panel with match options
     */
    private JPanel createNorthOptionsPanel() {
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

        modalityBox = new JComboBox<>(new String[]{"Jugador vs Jugador", "Jugador vs Máquina"});
        profileBox = new JComboBox<>(MachinePlayer.getAvailableProfiles());
        matchTypeBox = new JComboBox<>(new String[]{"Por Tiempo (min)", "Por Goles"});
        matchValueBox = new JComboBox<>(new Integer[]{1, 2, 3, 5, 7, 10});
        matchValueBox.setSelectedItem(3);
        profileBox.setEnabled(false);
        bindOptionListeners();

        optionsBar.add(createOptionLabel("Modalidad:"));
        optionsBar.add(modalityBox);
        optionsBar.add(createOptionLabel("Perfil Máquina:"));
        optionsBar.add(profileBox);
        optionsBar.add(createOptionLabel("Tipo:"));
        optionsBar.add(matchTypeBox);
        optionsBar.add(createOptionLabel("Valor:"));
        optionsBar.add(matchValueBox);

        northPanel.add(optionsBar, BorderLayout.SOUTH);
        return northPanel;
    }

    /**
     * Wires change listeners for modality, machine profile, and match victory rules.
     */
    private void bindOptionListeners() {
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
            matchValueBox.setSelectedItem(byGoals ? gui.getTargetGoals() : gui.getMatchMinutes());
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
    }

    /**
     * Paints the stadium background overlay behind player selection.
     * @param g graphics context used for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage bg = AssetManager.getStadiumImage();
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g.setColor(new Color(12, 18, 45, 180));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Refreshes displayed player stats, points, and selections.
     */
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

    /**
     * Creates a styled configuration bar label.
     * @param text text to display
     * @return styled label for the options bar
     */
    private JLabel createOptionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }

    /**
     * Creates a styled screen action button.
     * @param text text to display
     * @param bg button background color
     * @return configured button component
     */
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

    /** Card displaying one player's country selector, head preview, and stat upgrades. */
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

        /**
         * Builds a player configuration card with country preview and stat controls.
         * @param title header title text
         * @param borderColor card border color
         * @param initialCountry initially selected country name
         * @param progress array containing stats and available points
         * @param isPlayer1 true for player 1, false for player 2
         */
        PlayerConfigCard(String title, Color borderColor, String initialCountry,
                         int[] progress, boolean isPlayer1) {
            this.isPlayer1 = isPlayer1;
            this.progress = progress;

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

            countryBox = new JComboBox<>(CharacterRegistry.getAvailableCountries());
            countryBox.setSelectedItem(initialCountry);
            countryBox.setFont(new Font("Arial", Font.BOLD, 13));
            countryBox.addActionListener(e -> onCountryChanged());
            header.add(countryBox, BorderLayout.SOUTH);

            add(header, BorderLayout.NORTH);
            add(createPreviewCenterPanel(), BorderLayout.CENTER);
            add(createStatsSouthPanel(), BorderLayout.SOUTH);

            onCountryChanged();
            updateFromProgress(progress);
        }

        /**
         * Builds the character head preview and power info panel.
         * @return center panel with character portrait and power summary
         */
        private JPanel createPreviewCenterPanel() {
            JPanel center = new JPanel(new BorderLayout(4, 4));
            center.setOpaque(false);

            previewPanel = new JPanel() {
                /**
                 * Renders the character head sprite for the selected country.
                 * @param g graphics context used for painting
                 */
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
            return center;
        }

        /**
         * Builds the player points label and 4-stat upgrade grid.
         * @return panel containing upgrade buttons and available points
         */
        private JPanel createStatsSouthPanel() {
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
            String[] keys = {"speed", "jump", "kick", "survival"};

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
            return statsContainer;
        }

        /**
         * Updates the displayed card header title.
         * @param newTitle updated card header title
         */
        void refreshTitle(String newTitle) {
            titleLabel.setText(newTitle);
        }

        /**
         * Updates GUI country choice, power summary label, and preview sprite.
         */
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

        /**
         * Attempts to spend points to upgrade the specified player statistic.
         * @param statKey statistic key (speed, jump, kick, or survival)
         */
        private void upgradeStat(String statKey) {
            try {
                gui.getController().upgradePlayerStat(isPlayer1, statKey);
                updateFromProgress(isPlayer1 ? gui.getP1Progress() : gui.getP2Progress());
            } catch (HeadSoccerException ex) {
                JOptionPane.showMessageDialog(PlayerSelection.this, ex.getMessage(),
                        "Mejora de Estadística", JOptionPane.WARNING_MESSAGE);
            }
        }

        /**
         * Refreshes displayed points, stat levels, and upgrade button labels.
         * @param prog array containing stats and available points
         */
        void updateFromProgress(int[] prog) {
            this.progress = prog;
            String[] names = {"Speed", "Jump", "Kick", "Survival"};
            pointsLabel.setText("Puntos disponibles: " + prog[4]);
            int maxLevel = gui.getController().getMaxStatLevel();
            for (int i = 0; i < 4; i++) {
                int lvl = prog[i];
                statLabels[i].setText(names[i] + ": " + lvl + "/10");
                if (lvl >= maxLevel) {
                    plusButtons[i].setText("MAX");
                    plusButtons[i].setEnabled(false);
                } else {
                    int cost = gui.getController().getStatUpgradeCost(lvl);
                    plusButtons[i].setText("+ (" + cost + " pts)");
                    plusButtons[i].setEnabled(true);
                }
            }
        }
    }
}
