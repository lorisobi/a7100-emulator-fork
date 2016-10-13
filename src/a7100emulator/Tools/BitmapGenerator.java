/*
 * BitmapGenerator.java
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
 *   18.11.2014 - getBit durch BitTest ersetzt
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.Tools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Klasse zum Erzeugen von Zeichen für die Alphanumerische Bildschirmdarstellung
 *
 * @author Dirk Bräuer
 */
public class BitmapGenerator {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(BitmapGenerator.class.getName());

    /**
     * Farbe normales Grün
     */
    private static final int GREEN = new Color(0, 150, 0).getRGB();
    /**
     * Farbe intensives Grün
     */
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    /**
     * Farbe Schwarz
     */
    private static final int BLACK = new Color(0, 0, 0).getRGB();

    /**
     * Erstellt einen neuen BitmapGenerator
     */
    private BitmapGenerator() {
    }

    /**
     * Erzeugt ein Bild anhand der Daten eines darstellbaren Zeichens
     *
     * @param linecode Liniencode
     * @param intense Intensive Darstellung
     * @param inverse Inverse Darstellung
     * @param underline Unterstrichene Darstellung
     * @param flash Blinkende Darstellung
     * @return Bild des Zeichens
     */
    public static BufferedImage generateBitmapFromLineCode(byte[] linecode, boolean intense, boolean inverse, boolean underline, boolean flash) {
        BufferedImage image = new BufferedImage(8, 16, BufferedImage.TYPE_INT_RGB);

        int f_color = GREEN;
        int b_color = BLACK;

        if (inverse) {
            f_color = BLACK;
            b_color = (intense) ? INTENSE_GREEN : GREEN;
        } else if (intense) {
            f_color = INTENSE_GREEN;
        } else if (flash) {
            f_color = new Color(255, 0, 0).getRGB();
        }

        for (int lineIndex = 0; lineIndex < 16; lineIndex++) {
            int line = linecode[lineIndex];
            for (int columnIndex = 0; columnIndex < 8; columnIndex++) {
                if (BitTest.getBit(line, columnIndex) || (lineIndex == 13 && underline)) {
                    image.setRGB(7 - columnIndex, lineIndex, f_color);
                } else {
                    image.setRGB(7 - columnIndex, lineIndex, b_color);
                }
            }
        }
        return image;
    }
}
