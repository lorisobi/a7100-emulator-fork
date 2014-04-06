/*
 * Disk.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung einer Diskette
 *
 * @author Dirk Bräuer
 */
public class Disk {

    /**
     * Diskettentypen
     */
    public enum DiskType {

        /**
         * SCP-Diskette
         */
        SCP,
        /**
         * BOS-Diskette
         */
        BOS,
        /**
         * MUTOS-Diskette
         */
        MUTOS
    }

    /**
     * Anzahl der Zylinder der Diskette
     */
    private int cylinderPerDisk = 80;

    /**
     * Anzahl der Spuren pro Zylinder
     */
    private int tracksPerCylinder = 2;

    /**
     * Anzahl der Sektoren pro Spur
     */
    private int sectorsPerTrack = 16;

    /**
     * Anzahl der Bytes pro Sektor
     */
    private int bytesPerSector = 256;

    /**
     * Anzahl der Bytes pro Sektor für Spur 0
     */
    private int t0BytesPerSector = 128;

    /**
     * Größe der Diskette in Bytes
     */
    private int size = t0BytesPerSector * sectorsPerTrack + (cylinderPerDisk * 2 - 1) * sectorsPerTrack * bytesPerSector;

    /**
     * Daten der Diskette
     */
    private byte[] data = new byte[size];

    /**
     * Schreibschutz
     */
    private boolean writeProtect = false;

    /**
     * Erstellt eine leere Diskette
     */
    public Disk() {
    }

    /**
     * Erstellt eine Diskette basierend auf einer Datei
     *
     * @param file Datei zum Laden
     */
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
        } catch (IOException ex) {
            Logger.getLogger(FloppyDrive.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(FloppyDrive.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Gibt an ob die Diskette schreibgeschützt ist
     *
     * @return truw wenn schreibgeschützt, false sonst
     */
    public boolean isWriteProtect() {
        return writeProtect;
    }

    /**
     * Setzt den Schreibschutz der Diskette
     *
     * @param writeProtect true - Schreibschutz setzen / false - Schreibschutz
     * aufheben
     */
    public void setWriteProtect(boolean writeProtect) {
        this.writeProtect = writeProtect;
    }

    /**
     * Liest Daten an der angegebenen Position von der Diskette
     *
     * @param cylinder Zylindernummer
     * @param sector Sektornummer
     * @param head Kopfnummer
     * @param cnt Anzahl de Bytes
     * @return gelesene Daten
     */
    byte[] readData(int cylinder, int sector, int head, int cnt) {
        byte[] res = new byte[cnt];
        int pos = seek(cylinder, head, sector);
        System.arraycopy(data, pos, res, 0, cnt);
        return res;
    }

    /**
     * Speichert die Diskette in die angegebene Datei
     *
     * @param image Datei
     */
    public void saveDisk(File image) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(image);
            fos.write(data);
            fos.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Formatiert den angegebenen Bereich der Diskette
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param mod Modifizierung
     * @param formatData Datenbytes
     * @param interleave Interleave-Faktor
     */
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

    /**
     * Berechnet die absolute Adresse für Diskettenzugriff
     *
     * @param track Spur
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @return absolute Position
     */
    private int seek(int track, int head, int sector) {
        int pos;
        sector--;
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

    /**
     * Schreibt Daten auf die Diskette
     *
     * @param cylinder Zylindernummer
     * @param sector Sektornummer
     * @param head Kopfnummer
     * @param data Zu schreibende Daten
     */
    void writeData(int cylinder, int sector, int head, byte[] data) {
        int pos = seek(cylinder, head, sector);
        System.arraycopy(data, 0, this.data, pos, data.length);
    }

    /**
     * Speichert die Diskette in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich
     */
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

    /**
     * Liest die Diskette aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
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
