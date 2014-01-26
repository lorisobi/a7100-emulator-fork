/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Tools;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 * @author Dirk
 */
public class BitmapGenerator {

    private static final int GREEN = new Color(0, 150, 0).getRGB();
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    private static final int BLACK = new Color(0, 0, 0).getRGB();

    private BitmapGenerator() {
    }

    /**
     * 
     * @param linecode
     * @param intense
     * @param inverse
     * @param underline
     * @param flash
     * @return
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
            f_color=new Color(255,0,0).getRGB();
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

    private static boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
