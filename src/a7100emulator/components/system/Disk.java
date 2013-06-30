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

    public enum DiskType {

        SCP,
        BOS,
        MUTOS
    }
    private int cylinderPerDisk = 80;
    private int tracksPerCylinder = 2;
    private int sectorsPerTrack = 16;
    private int bytesPerSector = 256;
    private int t0BytesPerSector = 128;
    private int size = t0BytesPerSector * sectorsPerTrack + (cylinderPerDisk * 2 - 1) * sectorsPerTrack * bytesPerSector;
    private byte[] data = new byte[size];
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

    public byte[] readData(int cylinder, int sector, int head, int cnt) {
        byte[] res = new byte[cnt];
        int pos = seek(cylinder, head, sector);
        System.out.println("pos:" + pos);
        System.arraycopy(data, pos, res, 0, cnt);
        return res;
    }

    public void saveDisk(File image) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(image);
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
            data[pos++] = (byte) formatData[0];
            if (cylinder == 0 && head == 0) {
                for (int b = 0; b < t0BytesPerSector - 1; b++) {
                    data[pos++] = (byte) formatData[1];
                }
            } else {
                for (int b = 0; b < bytesPerSector - 1; b++) {
                    data[pos++] = (byte) formatData[1];
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

    void writeData(int cylinder, int sector, int head, byte[] data) {
        int pos = seek(cylinder, head, sector);
        System.arraycopy(data, 0, this.data, pos, data.length);
    }

    void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(cylinderPerDisk);
        dos.writeInt(tracksPerCylinder);
        dos.writeInt(sectorsPerTrack);
        dos.writeInt(bytesPerSector);
        dos.writeInt(t0BytesPerSector);
        dos.writeInt(size);
        dos.write(data);
        dos.writeBoolean(writeProtect);
    }

    void loadState(DataInputStream dis) throws IOException {
        cylinderPerDisk = dis.readInt();
        tracksPerCylinder = dis.readInt();
        sectorsPerTrack = dis.readInt();
        bytesPerSector = dis.readInt();
        t0BytesPerSector = dis.readInt();
        int readSize = dis.readInt();
        data = new byte[readSize];
        dis.read(data);
        writeProtect = dis.readBoolean();
    }
}
