/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.Tools.Memory;
import a7100emulator.Tools.Memory.FileLoadMode;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class FloppyDrive {

    private byte[] disk = new byte[653312];
    private boolean diskInsert=false;

    public FloppyDrive() {
    }

    public final void loadDisk(File file) {
        InputStream in=null;

        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int address = 0;
            for (byte b : buffer) {
                disk[address++] = b;
            }
        } catch (Exception ex) {
            Logger.getLogger(FloppyDrive.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(FloppyDrive.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void dump() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("disk.bin");
            for (byte b:disk) {
                fos.write(b);
            }
            fos.close();
        } catch (IOException ex) {
        }
    }

    public static void main(String[] args) {
        new FloppyDrive();
    }
}
