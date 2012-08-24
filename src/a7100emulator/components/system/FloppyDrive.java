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
public class FloppyDrive {

    public enum DriveType {

        K5600_20, K5602_10, K5601
    }
    private byte[] disk = new byte[653312];
    private boolean diskInsert = false;
    private final DriveType driveType;

    public FloppyDrive(DriveType type) {
        this.driveType = type;
    }

    public final void loadDisk(File file) {
        InputStream in = null;

        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int address = 0;
            for (byte b : buffer) {
                disk[address++] = b;
            }
            diskInsert = true;
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

    public void ejectDisk() {
        diskInsert = false;
    }

    public void dump() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("disk.bin");
            for (byte b : disk) {
                fos.write(b);
            }
            fos.close();
        } catch (IOException ex) {
        }
    }

    public byte[] readData(int track, int sector, int head, int cnt) {
        if (!diskInsert) {
            return null;
        }
        byte[] res = new byte[cnt];
        int pos;
        sector=sector-1;
        if (track == 0) {
            // Systemspur
            if (head == 0) {
                pos = sector * 128;
            } else {
                pos = (16 * 128) + (sector * 256);
            }
        } else {
            // Normale Spur
            pos = (16 * 128) + (16 * 256);
            pos += ((track - 1) * 2 * 16 * 256) + (head * 16 * 256) + (sector * 256);
        }
        System.arraycopy(disk, pos, res, 0, cnt);
        return res;
    }

    public int getCylinderCount() {
        switch (driveType) {
            case K5602_10:
                return 77;
            case K5600_20:
            case K5601:
                return 80;
        }
        return 0;
    }

    public boolean getDoubleStep() {
        return false;
    }

    public int getPrecompensationCode() {
        switch (driveType) {
            case K5602_10:
            case K5600_20:
            case K5601:
                return 0;
        }
        return 0;
    }

    public int getHeads() {
        switch (driveType) {
            case K5602_10:
            case K5600_20:
                return 1;
            case K5601:
                return 2;
        }
        return 0;
    }

    public int getHeadSink() {
        return 0;
    }

    public int getStepTime() {
        switch (driveType) {
            case K5602_10:
            case K5600_20:
                return 0x03;
            case K5601:
                return 0x02;
        }
        return 0;
    }

    public int getHeadTime() {
        switch (driveType) {
            case K5602_10:
            case K5600_20:
                return 0x05;
            case K5601:
                return 0x05;
        }
        return 0;
    }

    public int getWriting() {
        switch (driveType) {
            case K5602_10:
                return 0x00;
            case K5600_20:
            case K5601:
                return 0x01;
        }
        return 0;
    }

    public int getSectorsPerTrack() {
        switch (driveType) {
            case K5602_10:
                return 26;
            case K5600_20:
            case K5601:
                return 16;
        }
        return 0;
    }

    public int getBytesPerSector() {
        switch (driveType) {
            case K5602_10:
                return 128;
            case K5600_20:
            case K5601:
                return 256;
        }
        return 0;
    }

    public boolean getDiskInsert() {
        return diskInsert;
    }
}
