/*
 * BitmapGenerator.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.Tools;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Klasse zum Erzeugen von Zeichen für die Alphanumerische Bildschirmdarstellung
 *
 * @author Dirk Bräuer
 */
public class BitmapGenerator {

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
            // TODO - Blinkende Schrift, momentan dargestellt durch rot
            f_color = new Color(255, 0, 0).getRGB();
        }

        for (int lineIndex = 0; lineIndex < 16; lineIndex++) {
            int line = linecode[lineIndex];
            for (int columnIndex = 0; columnIndex < 8; columnIndex++) {
                if (getBit(line, columnIndex) || (lineIndex == 13 && underline)) {
                    image.setRGB(7 - columnIndex, lineIndex, f_color);
                } else {
                    image.setRGB(7 - columnIndex, lineIndex, b_color);
                }
            }
        }
        return image;
    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op1 Operand
     * @param i zu prüfendes Bit
     * @return true - wenn Bit gesetzt , false - sonst
     */
    private static boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
