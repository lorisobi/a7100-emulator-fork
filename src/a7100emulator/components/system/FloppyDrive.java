/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import java.io.*;

/**
 *
 * @author Dirk
 */
public class FloppyDrive {

    public void setWriteProtect(boolean selected) {
        if (disk!=null) disk.setWriteProtect(selected);
    }

    public void format(int cylinder, int head, int mod, int[] data, int interleave) {
        disk.format(cylinder, head, mod, data,interleave);
    }

    public void newDisk() {
        disk=new Disk();
    }

    public enum DriveType {

        K5600_20, K5602_10, K5601
    }
    private Disk disk;
    private final DriveType driveType;
    private static DriveType[] DRIVE_TYPES = new DriveType[]{DriveType.K5601, DriveType.K5601, DriveType.K5602_10, DriveType.K5602_10};
    private static FloppyDrive instances[] = new FloppyDrive[4];

    public static FloppyDrive getInstance(int driveID) {
        if (instances[driveID] == null) {
            instances[driveID] = new FloppyDrive(DRIVE_TYPES[driveID]);
        }
        return instances[driveID];
    }

    private FloppyDrive(DriveType type) {
        this.driveType = type;
    }

    private FloppyDrive() {
        this.driveType = DriveType.K5601;
    }

    public final void loadDisk(File file) {
        disk = new Disk(file);
    }

    public void ejectDisk() {
        disk = null;
    }

    public byte[] readData(int track, int sector, int head, int cnt) {
        if (disk == null) {
            return null;
        }

        return disk.readData(track, sector, head, cnt);
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
        return disk != null;
    }
}
