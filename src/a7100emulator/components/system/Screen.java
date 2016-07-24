/*
 * Screen.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
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
