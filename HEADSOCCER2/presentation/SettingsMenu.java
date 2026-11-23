package presentation;

import domain.Field;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Settings screen for selecting resolution and viewing controls. */
public class SettingsMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private JComboBox<String> resolutionBox;
    private JButton backButton;
    private boolean updatingCombo;

    /** Creates the settings menu panel. */
    public SettingsMenu(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setLayout(new GridBagLayout());

        backButton = createButton("REGRESAR");
        backButton.addActionListener(e -> goBack());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        JLabel title = new JLabel("CONFIGURACION");
        title.setFont(new Font("Arial", Font.BOLD, 34));
        title.setForeground(new Color(255, 215, 40));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 14, 0);
        add(createResolutionPanel(), gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(8, 0, 18, 0);
        add(createControlsInfoArea(), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(6, 0, 10, 0);
        add(backButton, gbc);
    }

    /** Builds the resolution selector panel and binds its change listener. */
    private JPanel createResolutionPanel() {
        String[] resolutions = {
            "800 x 480 (Pequeña)",
            "900 x 540 (Mediana)",
            "1050 x 630 (Grande)"
        };
        resolutionBox = new JComboBox<>(resolutions);
        syncResolutionFromGUI();
        resolutionBox.setFont(new Font("Arial", Font.BOLD, 15));
        resolutionBox.setPreferredSize(new Dimension(240, 36));
        resolutionBox.addActionListener(e -> {
            if (!updatingCombo) {
                applySelectedResolution();
            }
        });

        JPanel resPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        resPanel.setBackground(new Color(20, 30, 75, 220));
        resPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        JLabel resLabel = new JLabel("Resolución de cancha:");
        resLabel.setForeground(Color.WHITE);
        resLabel.setFont(new Font("Arial", Font.BOLD, 15));
        resPanel.add(resLabel);
        resPanel.add(resolutionBox);
        return resPanel;
    }

    /** Builds the read-only control legend text area. */
    private JTextArea createControlsInfoArea() {
        JTextArea controlsInfo = new JTextArea(
            "CONTROLES DEL JUEGO:\n\n" +
            "Jugador 1 (Izquierda):\n" +
            "  A / D          = Moverse izquierda / derecha\n" +
            "  W              = Saltar\n" +
            "  S              = Patear balón / golpear rival\n" +
            "  E o ESPACIO    = Activar poder (cuando barra = 100%)\n\n" +
            "Jugador 2 (Derecha):\n" +
            "  <- / ->        = Moverse izquierda / derecha\n" +
            "  Arriba         = Saltar\n" +
            "  Abajo          = Patear balón / golpear rival\n" +
            "  ENTER o L      = Activar poder (cuando barra = 100%)\n\n" +
            "General:\n" +
            "  ESC            = Pausar / Guardar / Terminar partido"
        );
        controlsInfo.setEditable(false);
        controlsInfo.setFont(new Font("Monospaced", Font.PLAIN, 13));
        controlsInfo.setBackground(new Color(25, 32, 70));
        controlsInfo.setForeground(Color.WHITE);
        controlsInfo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        return controlsInfo;
    }

    /** Synchronizes the resolution combo box with the current GUI width without triggering resize. */
    public void syncResolutionFromGUI() {
        updatingCombo = true;
        int w = gui.getFieldWidth();
        if (w == Field.RES_SMALL_WIDTH) {
            resolutionBox.setSelectedIndex(0);
        } else if (w == Field.RES_LARGE_WIDTH) {
            resolutionBox.setSelectedIndex(2);
        } else {
            resolutionBox.setSelectedIndex(1);
        }
        updatingCombo = false;
    }

    /** Paints the stadium background overlay behind settings. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage bg = AssetManager.getStadiumImage();
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g.setColor(new Color(10, 16, 40, 175));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Applies the selected resolution option to the main window. */
    private void applySelectedResolution() {
        int idx = resolutionBox.getSelectedIndex();
        if (idx == 0) {
            gui.applyResolution(Field.RES_SMALL_WIDTH, Field.RES_SMALL_HEIGHT);
        } else if (idx == 2) {
            gui.applyResolution(Field.RES_LARGE_WIDTH, Field.RES_LARGE_HEIGHT);
        } else {
            gui.applyResolution(Field.RES_MEDIUM_WIDTH, Field.RES_MEDIUM_HEIGHT);
        }
    }

    /** Creates a styled navigation button. */
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(new Color(60, 120, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Returns to the previous screen. */
    public void goBack() {
        gui.openMenu();
    }
}
