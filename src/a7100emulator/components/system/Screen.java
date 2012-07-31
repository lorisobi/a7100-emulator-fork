/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.Tools.BitmapGenerator;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author Dirk
 */
public class Screen extends JComponent {

    private static Screen instance;
    private BufferedImage screenImage;

    private Screen() {
        showScreen();
    }

    public static Screen getInstance() {
        if (instance == null) {
            instance = new Screen();
        }
        return instance;
    }

    public void setImage(BufferedImage screenImage) {
        this.screenImage = screenImage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(screenImage, 0, 0, this);
    }

    private void showScreen() {
        this.setMinimumSize(new Dimension(640, 400));
        this.setPreferredSize(new Dimension(640, 400));

        JFrame frame = new JFrame("Monitor");
        frame.addKeyListener(Keyboard.getInstance());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(this);
        frame.setVisible(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.pack();
    }
}
