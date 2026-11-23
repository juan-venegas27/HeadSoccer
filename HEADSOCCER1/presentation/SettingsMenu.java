package presentation;

import javax.swing.*;
import java.awt.*;

/**
 * The settings menu screen.
 * Displays key controls and options to return to the main menu.
 */
public class SettingsMenu extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final JButton backButton;

    /**
     * Constructs the settings menu panel.
     *
     * @param gui the main application window
     */
    public SettingsMenu(HeadSoccerGUI gui) {
        this.gui = gui;
        setPreferredSize(new Dimension(900, 540));
        setBackground(new Color(20, 20, 60));
        setLayout(new GridBagLayout());

        backButton = createButton("REGRESAR");
        backButton.addActionListener(e -> goBack());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(14, 0, 14, 0);

        JLabel title = new JLabel("CONFIGURACION");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 30, 0);
        add(title, gbc);

        JTextArea controlsInfo = new JTextArea(
            "CONTROLES DEL JUEGO:\n\n" +
            "Jugador 1 (Azul):\n" +
            "  A / D   = Moverse izquierda / derecha\n" +
            "  W       = Saltar\n" +
            "  S       = Golpear balon\n\n" +
            "Jugador 2 (Rojo):\n" +
            "  <- / -> = Moverse izquierda / derecha\n" +
            "  Arriba  = Saltar\n" +
            "  Abajo   = Golpear balon\n\n" +
            "General:\n" +
            "  ESC     = Pausar / Continuar"
        );
        controlsInfo.setEditable(false);
        controlsInfo.setFont(new Font("Arial", Font.PLAIN, 15));
        controlsInfo.setBackground(new Color(40, 40, 90));
        controlsInfo.setForeground(Color.WHITE);
        controlsInfo.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 20, 0);
        add(controlsInfo, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 10, 0);
        add(backButton, gbc);
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

    /** Returns to the previous screen. */
    public void goBack() {
        gui.openMenu();
    }
}
