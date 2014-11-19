/*
 * Screen.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Singleton-Klasse zur Darstellung der Bildschirmausgaben
 *
 * @author Dirk Bräuer
 */
public class Screen extends JComponent {

    /**
     * Instanz
     */
    private static Screen instance;
    /**
     * Bild des Bildschirminhalts
     */
    private BufferedImage screenImage;

    /**
     * Erzeugt einen neuen Bildschirm
     */
    private Screen() {
        setMinimumSize(new Dimension(640, 400));
        setPreferredSize(new Dimension(640, 400));
    }

    /**
     * Gibt die Instanz des Bildschirms zurück
     *
     * @return Instanz
     */
    public static Screen getInstance() {
        if (instance == null) {
            instance = new Screen();
        }
        return instance;
    }

    /**
     * Setzt das darzustellende Bild
     *
     * @param screenImage Bild
     */
    public void setImage(BufferedImage screenImage) {
        this.screenImage = screenImage;
        repaint();
    }

    /**
     * Zeichnet die Komponente neu
     *
     * @param g Zeichenbereich
     */
    @Override
    protected void paintComponent(Graphics g) {
        Dimension dim = getSize();
        g.drawImage(screenImage, 0, 0, dim.width, dim.height, 0, 0, 640, 400, this);
    }

    /**
     * Gibt das dargestellte Bild zurück
     *
     * @return Bild
     */
    public BufferedImage getImage() {
        return screenImage;
    }
}
