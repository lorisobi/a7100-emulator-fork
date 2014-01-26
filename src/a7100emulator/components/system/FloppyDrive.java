/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public class FloppyDrive {

    /**
     * 
     */
    public enum DriveType {

        /**
         * 
         */
        K5600_20,
        /**
         *
         */
        K5602_10,
        /**
         *
         */
        K5601
    }
    private Disk disk;
    private DriveType driveType;

    /**
     * 
     * @param driveType
     */
    public FloppyDrive(DriveType driveType) {
        this.driveType = driveType;
    }

    /**
     * 
     * @param writeProtected
     */
    public void setWriteProtect(boolean writeProtected) {
        if (disk != null) {
            disk.setWriteProtect(writeProtected);
        }
    }

    /**
     * 
     * @param cylinder
     * @param head
     * @param mod
     * @param data
     * @param interleave
     */
    public void format(int cylinder, int head, int mod, int[] data, int interleave) {
        disk.format(cylinder, head, mod, data, interleave);
    }

    /**
     * 
     */
    public void newDisk() {
        disk = new Disk();
    }

    /**
     * 
     * @param cylinder
     * @param sector
     * @param head
     * @param data
     */
    public void writeData(int cylinder, int sector, int head, byte[] data) {
        if (disk == null) {
            return;
        }
        disk.writeData(cylinder, sector, head, data);
    }

    /**
     * 
     * @param image
     */
    public void saveDiskToFile(File image) {
        if (disk == null) {
            return;
        }
        disk.saveDisk(image);
    }

    /**
     * 
     * @param file
     */
    public void loadDiskFromFile(File file) {
        disk = new Disk(file);
    }

    /**
     * 
     */
    public void ejectDisk() {
        disk = null;
    }

    /**
     * 
     * @param cylinder
     * @param sector
     * @param head
     * @param cnt
     * @return
     */
    public byte[] readData(int cylinder, int sector, int head, int cnt) {
        if (disk == null) {
            return null;
        }
        return disk.readData(cylinder, sector, head, cnt);
    }

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
    public boolean getDoubleStep() {
        return false;
    }

    /**
     * 
     * @return
     */
    public int getPrecompensationCode() {
        switch (driveType) {
            case K5602_10:
            case K5600_20:
            case K5601:
                return 0;
        }
        return 0;
    }

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
    public int getHeadSink() {
        return 0;
    }

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
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

    /**
     * 
     * @return
     */
    public boolean getDiskInsert() {
        return disk != null;
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeUTF(driveType.name());
        if (disk == null) {
            dos.writeBoolean(false);
        } else {
            dos.writeBoolean(true);
            disk.saveState(dos);
        }
    }

    /**
     * 
     * @param dis
     * @throws IOException
     */
    public void loadState(DataInputStream dis) throws IOException {
        driveType = DriveType.valueOf(dis.readUTF());
        boolean diskInserted = dis.readBoolean();
        if (diskInserted) {
            disk=new Disk();
            disk.loadState(dis);
        }
    }
}
