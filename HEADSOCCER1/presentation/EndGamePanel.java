package presentation;

import domain.HeadSoccer;
import domain.Scoreboard;

import javax.swing.*;
import java.awt.*;

/**
 * The end-game screen shown when the match finishes.
 * Displays the final score, the winner, and navigation options.
 */
public class EndGamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final HeadSoccer model;

    private final JLabel resultLabel;
    private final JLabel winnerLabel;
    private final JButton menuButton;
    private final JButton exitButton;

    /**
     * Constructs the end-game panel.
     *
     * @param gui   the main application window
     * @param model the finished game model
     */
    public EndGamePanel(HeadSoccerGUI gui, HeadSoccer model) {
        this.gui   = gui;
        this.model = model;

        setPreferredSize(new Dimension(900, 540));
        setBackground(new Color(20, 20, 60));
        setLayout(new GridBagLayout());

        JLabel title = new JLabel("FIN DEL PARTIDO");
        title.setFont(new Font("Arial", Font.BOLD, 42));
        title.setForeground(Color.WHITE);

        resultLabel = new JLabel("-");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 56));
        resultLabel.setForeground(Color.YELLOW);

        winnerLabel = new JLabel("");
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        winnerLabel.setForeground(Color.WHITE);

        menuButton = createButton("VOLVER AL MENU");
        exitButton = createButton("SALIR");

        menuButton.addActionListener(e -> gui.openMenu());
        exitButton.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(14, 0, 14, 0);

        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 20, 0);
        add(title, gbc);

        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridy = 1; add(resultLabel, gbc);
        gbc.gridy = 2; add(winnerLabel, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setBackground(new Color(20, 20, 60));
        btnPanel.add(menuButton);
        btnPanel.add(exitButton);

        gbc.gridy = 3; gbc.insets = new Insets(30, 0, 10, 0);
        add(btnPanel, gbc);
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 48));
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(new Color(60, 120, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /**
     * Refreshes the displayed score and winner based on the current model state.
     * Must be called before showing this panel.
     */
    public void refresh() {
        Scoreboard sb = model.getScoreboard();
        int s1 = sb.getScorePlayer1();
        int s2 = sb.getScorePlayer2();

        resultLabel.setText(s1 + " - " + s2);

        String winnerText;
        if (s1 > s2) {
            winnerText = model.getPlayer1().getName() + " GANA!";
            winnerLabel.setForeground(Color.BLUE);
        } else if (s2 > s1) {
            winnerText = model.getPlayer2().getName() + " GANA!";
            winnerLabel.setForeground(Color.RED);
        } else {
            winnerText = "EMPATE!";
            winnerLabel.setForeground(Color.WHITE);
        }
        winnerLabel.setText(winnerText);
        repaint();
    }
}
