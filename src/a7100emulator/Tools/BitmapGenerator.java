/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Tools;

import java.awt.Color;
import java.awt.Graphics;
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

    public static BufferedImage generateBitmapFromLineCode(byte[] linecode, boolean intense, boolean inverse, boolean flash, boolean underline) {
        BufferedImage image = new BufferedImage(8, 16, BufferedImage.TYPE_INT_RGB);

        int f_color = GREEN;
        int b_color = BLACK;

        if (inverse) {
            f_color = BLACK;
            b_color = (intense) ? INTENSE_GREEN : GREEN;
        } else if (intense) {
            f_color = INTENSE_GREEN;
        }


        Graphics g = image.getGraphics();
        g.setColor(new Color(b_color));
        g.fillRect(0, 0, 8, 16);

        for (int lineIndex = 0; lineIndex < 16; lineIndex++) {
            int line = linecode[lineIndex];
            for (int columnIndex = 0; columnIndex < 8; columnIndex++) {
                if (getBit(line, columnIndex) || (lineIndex == 13 && underline)) {
                    image.setRGB(7 - columnIndex, lineIndex, f_color);
                }
            }
        }
        return image;
    }

    private static boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

//    public static void main(String[] args) {
//        byte[] codes = new byte[16];
//
//        JFrame frame = new JFrame();
//
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        Screen screen = new Screen();
//        frame.add(screen);
//        frame.setVisible(true);
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        frame.pack();
//
//
//        try {
//            RandomAccessFile raf = new RandomAccessFile("./eproms/KGS7070-152.bin", "r");
//            raf.seek(0x0);
//            int x = 0, y = 0;
//
//            for (int i = 0; i < 2000; i++) {
//                raf.read(codes);
//                screen.setCharacter(x, y, generateBitmapFromLineCode(codes, true, true, true, true));
//                x++;
//                if (x == 80) {
//                    y++;
//                    x = 0;
//                }
//            }
//            raf.close();
//        } catch (Exception ex) {
//            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
}
