/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Disk {

    private int cylinderPerDisk = 80;
    private int tracksPerCylinder = 2;
    private int sectorsPerTrack = 16;
    private int bytesPerSector = 256;
    private int t0BytesPerSector = 128;
    private byte[] data = new byte[653312];
    private boolean writeProtect = false;

    public Disk() {
    }

    public Disk(File file) {
        InputStream in = null;

        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int address = 0;
            for (byte b : buffer) {
                data[address++] = b;
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

    public boolean isWriteProtect() {
        return writeProtect;
    }

    public void setWriteProtect(boolean writeProtect) {
        this.writeProtect = writeProtect;
    }

    public byte[] readData(int track, int sector, int head, int cnt) {
        byte[] res = new byte[cnt];
        int pos = seek(track, head, sector);
        System.arraycopy(data, pos, res, 0, cnt);
        return res;
    }

    public void dump() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("disk.bin");
            for (byte b : data) {
                fos.write(b);
            }
            fos.close();
        } catch (IOException ex) {
        }
    }

    void format(int cylinder, int head, int mod, int[] formatData, int interleave) {
        // TODO: mod und interleave
        int pos = seek(cylinder, head, 1);
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            data[pos++]=(byte) formatData[0];
            if (cylinder == 0 && head == 0) {
               
               for (int b=0;b<t0BytesPerSector-1;b++) {
                   data[pos++]=(byte) formatData[1];
               }
            } else {
                   for (int b=0;b<bytesPerSector-1;b++) {
                   data[pos++]=(byte) formatData[1];
               }
            }
        }

    }

    private int seek(int track, int head, int sector) {
        int pos;
        sector = sector - 1;
        if (track == 0) {
            // Systemspur
            if (head == 0) {
                pos = sector * t0BytesPerSector;
            } else {
                pos = (sectorsPerTrack * t0BytesPerSector) + (sector * bytesPerSector);
            }
        } else {
            // Normale Spur
            pos = (sectorsPerTrack * t0BytesPerSector) + (sectorsPerTrack * bytesPerSector);
            pos += ((track - 1) * tracksPerCylinder * sectorsPerTrack * bytesPerSector) + (head * sectorsPerTrack * bytesPerSector) + (sector * bytesPerSector);
        }
        return pos;
    }
}
