package presentation;

import domain.HeadSoccer;
import domain.Scoreboard;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Panel displaying the final score, winner, and earned points. */
public class EndGamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private HeadSoccer model;

    private JLabel resultLabel;
    private JLabel winnerLabel;
    private JLabel pointsLabel;
    private JButton menuButton;
    private JButton exitButton;

    /** Creates the end-game panel for the finished match. */
    public EndGamePanel(HeadSoccerGUI gui, HeadSoccer model) {
        this.gui = gui;
        this.model = model;

        setPreferredSize(new Dimension(900, 540));
        setLayout(new GridBagLayout());

        JLabel title = new JLabel("FIN DEL PARTIDO");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.WHITE);

        resultLabel = new JLabel("-");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 56));
        resultLabel.setForeground(new Color(255, 215, 40));

        winnerLabel = new JLabel("");
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        winnerLabel.setForeground(Color.WHITE);

        pointsLabel = new JLabel("");
        pointsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pointsLabel.setForeground(new Color(180, 235, 255));

        menuButton = createButton("VOLVER AL MENU");
        exitButton = createButton("SALIR");

        menuButton.addActionListener(e -> gui.openMenu());
        exitButton.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 16, 0);
        add(title, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridy = 1; add(resultLabel, gbc);
        gbc.gridy = 2; add(winnerLabel, gbc);
        gbc.gridy = 3; add(pointsLabel, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(menuButton);
        btnPanel.add(exitButton);

        gbc.gridy = 4; gbc.insets = new Insets(28, 0, 10, 0);
        add(btnPanel, gbc);
    }

    /** Paints the stadium background overlay behind the end-game panel. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage bg = AssetManager.getStadiumImage();
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g.setColor(new Color(10, 16, 40, 175));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Creates a styled navigation button. */
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 46));
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(new Color(60, 120, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    /** Updates the displayed score, winner, and points. */
    public void refresh() {
        Scoreboard sb = model.getScoreboard();
        int s1 = sb.getScorePlayer1();
        int s2 = sb.getScorePlayer2();

        resultLabel.setText(s1 + " - " + s2);

        String winnerText;
        if (s1 > s2) {
            winnerText = model.getPlayer1().getName() + " (" + model.getPlayer1().getCountry() + ") GANA!";
            winnerLabel.setForeground(new Color(120, 185, 255));
        } else if (s2 > s1) {
            winnerText = model.getPlayer2().getName() + " (" + model.getPlayer2().getCountry() + ") GANA!";
            winnerLabel.setForeground(new Color(255, 120, 110));
        } else {
            winnerText = "EMPATE!";
            winnerLabel.setForeground(Color.WHITE);
        }
        winnerLabel.setText(winnerText);

        String ptsInfo = String.format(
            "<html><center>%s: +%d pts (Total: %d pts)<br>%s: +%d pts (Total: %d pts)</center></html>",
            model.getPlayer1().getName(), model.getLastPointsPlayer1(), model.getPlayer1().getPoints(),
            model.getPlayer2().getName(), model.getLastPointsPlayer2(), model.getPlayer2().getPoints()
        );
        pointsLabel.setText(ptsInfo);
        repaint();
    }
}
