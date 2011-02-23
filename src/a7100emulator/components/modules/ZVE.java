/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class ZVE {

    RandomAccessFile prom1;
    RandomAccessFile prom2;
    RandomAccessFile prom3;
    RandomAccessFile prom4;

    public ZVE() {
        try {
            prom1 = new RandomAccessFile("./eproms/265.bin", "r");
            prom2 = new RandomAccessFile("./eproms/266.bin", "r");
            prom3 = new RandomAccessFile("./eproms/267.bin", "r");
            prom4 = new RandomAccessFile("./eproms/268.bin", "r");

            FileOutputStream fos1 = new FileOutputStream("1.hex");
            FileOutputStream fos2 = new FileOutputStream("2.hex");

            for (int i = 0; i < prom1.length(); i++) {
                fos1.write(prom3.readByte());
                fos1.write(prom1.readByte());
                fos2.write(prom4.readByte());
                fos2.write(prom2.readByte());
            }

            fos1.close();
            fos2.close();

        } catch (Exception ex) {
            Logger.getLogger(ZVE.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public static void main(String[] args) {
        new ZVE();
    }
}
