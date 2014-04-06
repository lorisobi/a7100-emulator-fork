/*
 * FloppyDrive.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Klasse zur Realisierung eines Diskettenlaufwerkes
 *
 * @author Dirk Bräuer
 */
public class FloppyDrive {

    /**
     * Laufwerkstyp
     */
    public enum DriveType {

        /**
         * Diskettenlaufwerk K5600.20: 5.25" 96tpi DD SS
         */
        K5600_20,
        /**
         * Diskettenlaufwerk K5602.10: 8" 48tpi SD
         */
        K5602_10,
        /**
         * Diskettenlaufwerk K5601: 5.25" 96tpi DD DS
         */
        K5601
    }
    /**
     * Referenz auf eingelegte Diskette
     */
    private Disk disk;
    /**
     * Laufwerkstyp
     */
    private DriveType driveType;

    /**
     * Erstellt ein neues Diskettenlaufwerk
     *
     * @param driveType Laufwerkstyp
     */
    public FloppyDrive(DriveType driveType) {
        this.driveType = driveType;
    }

    /**
     * Setzt oder löscht den Schreibschutz für die eingelegte Diskette
     *
     * @param writeProtected true - wenn Diskette schreibgeschützt werden soll ,
     * false - sonst
     */
    public void setWriteProtect(boolean writeProtected) {
        if (disk != null) {
            disk.setWriteProtect(writeProtected);
        }
    }

    /**
     * Formatiert den angegebenen Bereich der eingelegten Diskette
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param mod Modifizierung
     * @param data Datenbytes
     * @param interleave Interleave-Faktor
     */
    public void format(int cylinder, int head, int mod, int[] data, int interleave) {
        disk.format(cylinder, head, mod, data, interleave);
    }

    /**
     * Erzeugt eine leere Diskette
     */
    public void newDisk() {
        disk = new Disk();
    }

    /**
     * Schreibt Daten auf die eingelegte Diskette
     *
     * @param cylinder Zylindernummer
     * @param sector Sektornummer
     * @param head Kopfnummer
     * @param data Daten
     */
    public void writeData(int cylinder, int sector, int head, byte[] data) {
        if (disk == null) {
            return;
        }
        disk.writeData(cylinder, sector, head, data);
    }

    /**
     * Speichert die Diskette als Image
     *
     * @param image Image-File
     */
    public void saveDiskToFile(File image) {
        if (disk == null) {
            return;
        }
        disk.saveDisk(image);
    }

    /**
     * Lädt eine Diskette aus einer Datei
     *
     * @param file Image
     */
    public void loadDiskFromFile(File file) {
        disk = new Disk(file);
    }

    /**
     * Wirft die Diskette aus
     */
    public void ejectDisk() {
        disk = null;
    }

    /**
     * Liest Daten von der Diskette
     *
     * @param cylinder Zylindernummer
     * @param sector Sketornummer
     * @param head Kopfnummer
     * @param cnt Anzahl der zu lesenden Bytes
     * @return gelesene Daten
     */
    public byte[] readData(int cylinder, int sector, int head, int cnt) {
        if (disk == null) {
            return null;
        }
        return disk.readData(cylinder, sector, head, cnt);
    }

    /**
     * Gibt die Anzahl der Zylinder des Laufwerks zurück
     *
     * @return Anzahl der Zylinder
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
     * Gibt an ob das Laufwerk Double-Step verwendet
     *
     * @return true - wenn Double Step verwendet wird , false -sonst
     */
    public boolean getDoubleStep() {
        return false;
    }

    /**
     * Gibt den Präkompensationscode zurück
     *
     * @return Präkompensationscode
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
     * Gibt die Anzahl der Köpfe des Laufwerks zurück
     *
     * @return Anzahl der Köpfe
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
     * Gibt die Verzögerung für das Abschwenken der Köpfe an
     *
     * @return Verzögerung
     */
    public int getHeadSink() {
        return 0;
    }

    /**
     * Gibt die Schrittrate des Laufwerks an
     *
     * @return Schrittrate
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
     * Gibt die Kopfladezeit zurück
     *
     * @return Kopfladezeit
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
     * Gibt das Aufzeichnungsverfahren zurück
     *
     * @return Aufzeichnungsverfahren 0 - FM , 1 - MFM
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
     * Liefert die Anzahl der Sektoren je Spur zurück
     *
     * @return Anzahl Sektoren/Spur
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
     * Liefert die Anzahl der bytes pro Sektor zurück
     *
     * @return Bytes/Sektor
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
     * Gibt an, ob eine Diskette eingelegt ist
     *
     * @return true - wenn Diskette eingelegt , false - sonst
     */
    public boolean getDiskInsert() {
        return disk != null;
    }

    /**
     * Speichert den Zustand des Laufwerks in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
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
     * Lädt den Zustand des Laufwerks aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        driveType = DriveType.valueOf(dis.readUTF());
        boolean diskInserted = dis.readBoolean();
        if (diskInserted) {
            disk = new Disk();
            disk.loadState(dis);
        }
    }
}
