/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author Dirk
 */
public class Screen extends JComponent {

    BufferedImage[][] characters = new BufferedImage[80][25];
    BufferedImage empty = new BufferedImage(8, 16, BufferedImage.TYPE_INT_RGB);

    public Screen() {
        this.setMinimumSize(new Dimension(640,400));
        this.setPreferredSize(new Dimension(640,400));
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, 640, 400);

        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                if (characters[i][j] != null) {
                    g.drawImage(characters[i][j], i * 8, j * 16, this);
                }
            }
        }
    }

    public void setCharacter(int x, int y, BufferedImage image) {
        characters[x][y] = image;
    }
}
