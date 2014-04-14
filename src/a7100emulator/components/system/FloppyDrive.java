/*
 * FloppyDrive.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *   12.04.2014 Parameter für Initialisierung hinzugefügt, Formatierung überarbeitet
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
     * Anzahl der Zylinder
     */
    private int cylinder;
    /**
     * Anzahl der Köpfe
     */
    private int heads;
    /**
     * Anzahl der Sektoren je Track
     */
    private int sectorsPerTrack;
    /**
     * Anzahl der Bytes pro Sektor
     */
    private int bytesPerSector;
    /**
     * Doppelschritt
     */
    private boolean doubleStep;
    /**
     * Zylinder für Schreibpraekompensation
     */
    private int precompensation;
    /**
     * Zylinder für reduzierten Schreibstrom
     */
    private int reduceCurrent;
    /**
     * HeadSink
     */
    private int headSink;
    /**
     * MFM Modus
     */
    private boolean mfmMode;
    /**
     * Schrittrate
     */
    private int stepTime;
    /**
     * Kopfladezeit
     */
    private int headTime;

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
        if (cylinder == 0 && head == 0 && getBit(mod, 7)) {
            switch (driveType) {
                // 5.25" Diskette 16 Sektoren mit 128 Bytes
                case K5600_20:
                case K5601:
                    disk.format(cylinder, head, mod, data, interleave, 16, 128);
                    break;
                // 8" Diskette 26 Sektoren mit 128 Bytes
                case K5602_10:
                    disk.format(cylinder, head, mod, data, interleave, 26, 128);
                    break;
            }
        } else {
            disk.format(cylinder, head, mod, data, interleave, sectorsPerTrack, bytesPerSector);
        }
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
     * @param imageType Typ der Image-Datei
     */
    public void loadDiskFromFile(File file, Disk.ImageType imageType) {
        disk = new Disk(file, imageType);
    }

    /**
     * Lädt eine Diskette aus einer Binärdatei unter verwendung der angegebenen
     * Parameter
     *
     * @param file Image
     * @param cylinders Anzahl der Zylinder
     * @param heads Anzahl der Köpfe
     * @param sectorsPerTrack Anzahl der Sektoren pro Spur
     * @param bytesPerSector Anzahl der Bytes pro Sektor
     * @param sectorsInTrack0 Anzahl der Sektoren in Spur 0
     * @param bytesPerSectorTrack0 Anzahl der Bytes pro Sektor in Spur 0
     */
    public void loadDiskFromFile(File file, int cylinders, int heads, int sectorsPerTrack, int bytesPerSector, int sectorsInTrack0, int bytesPerSectorTrack0) {
        disk = new Disk(file, cylinders, heads, sectorsPerTrack, bytesPerSector, sectorsInTrack0, bytesPerSectorTrack0);
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
     * Gibt die Anzahl der eingestellten Zylinder zurück
     *
     * @return Anzahl der Zylinder
     */
    public int getCylinder() {
        return cylinder;
    }

    /**
     * Setzt die Anzahl der Zylinder
     *
     * @param cylinder Zylinderanzahl
     */
    public void setCylinder(int cylinder) {
        this.cylinder = cylinder;
    }

    /**
     * Gibt die Anzahl der Köpfe des Laufwerks zurück
     *
     * @return Anzahl der Köpfe
     */
    public int getHeads() {
        return heads;
    }

    /**
     * Setzt die Anzahl der Köpfe
     *
     * @param heads Anzahl der Köpfe
     */
    public void setHeads(int heads) {
        this.heads = heads;
    }

    /**
     * Liefert die Anzahl der Sektoren je Spur zurück
     *
     * @return Anzahl Sektoren/Spur
     */
    public int getSectorsPerTrack() {
        return sectorsPerTrack;
    }

    /**
     * Setzt die Anzahl der Sektoren pro Spur
     *
     * @param sectorsPerTrack Anzahl der Sektoren je Spur
     */
    public void setSectorsPerTrack(int sectorsPerTrack) {
        this.sectorsPerTrack = sectorsPerTrack;
    }

    /**
     * Liefert die Anzahl der Bytes pro Sektor zurück
     *
     * @return Bytes/Sektor
     */
    public int getBytesPerSector() {
        return bytesPerSector;
    }

    /**
     * Setzt die Anzahl der Bytes pro Sktor
     *
     * @param bytesPerSector Anzahl der Bytes pro Sektor
     */
    public void setBytesPerSector(int bytesPerSector) {
        this.bytesPerSector = bytesPerSector;
    }

    /**
     * Gibt an ob das Laufwerk Doppelschritt verwendet
     *
     * @return true - Wenn Doppelschritt verwendet wird, false - sonst
     */
    public boolean isDoubleStep() {
        return doubleStep;
    }

    /**
     * Legt fest, ob das Laufwerk Doppelschritt verwenden soll
     *
     * @param doubleStep true - Wenn Doppelschritt verwendet wird, false - sonst
     */
    public void setDoubleStep(boolean doubleStep) {
        this.doubleStep = doubleStep;
    }

    /**
     * Gibt den Code für den Zylinder zurück, bei welchem Schreibpräkompensation
     * beginnt
     *
     * @return Code für Präkompensation
     */
    public int getPrecompensation() {
        return precompensation;
    }

    /**
     * Setzt den Code für den Zylinder, bei welchem Präkompensation beginnt
     *
     * @param precompensation Code für Präkompensation
     */
    public void setPrecompensation(int precompensation) {
        this.precompensation = precompensation;
    }

    /**
     * Gibt den Code für den Zylinder an, ab welchem mit reduziertem
     * Schreibstrom gearbeitet wird
     *
     * @return Code für reduzierten Schreibstrom
     */
    public int getReduceCurrent() {
        return reduceCurrent;
    }

    /**
     * Setzt den Code für den Zylinder, ab welchem mit reduziertem Schreibstrom
     * gearbeitet wird
     *
     * @param reduceCurrent Code für reduzierten Schreibstrom
     */
    public void setReduceCurrent(int reduceCurrent) {
        this.reduceCurrent = reduceCurrent;
    }

    /**
     * Gibt die Verzögerung für das Abschwenken der Köpfe zurück
     *
     * @return Verzögerung
     */
    public int getHeadSink() {
        return headSink;
    }

    /**
     * Setzt die Verzögerung für das Abschwenken der Köpfe
     *
     * @param headSink Verzögerung
     */
    public void setHeadSink(int headSink) {
        this.headSink = headSink;
    }

    /**
     * Gibt an ob das MFM Aufzeichnungsverfahren verwendet wird
     *
     * @return true - MFM Modus , false - FM Modus
     */
    public boolean isMfmMode() {
        return mfmMode;
    }

    /**
     * Legt fest, ob das MFM Verfahren verwendet wird
     *
     * @param mfmMode true - MFM Modus , false - FM Modus
     */
    public void setMfmMode(boolean mfmMode) {
        this.mfmMode = mfmMode;
    }

    /**
     * Gibt den Code für die Schrittrate des Laufwerks zurück
     *
     * @return Schrittrate
     */
    public int getStepTime() {
        return stepTime;
    }

    /**
     * Setzt den Code für die Schrittrate des Laufwerks
     *
     * @param stepTime Code für Schrittrate
     */
    public void setStepTime(int stepTime) {
        this.stepTime = stepTime;
    }

    /**
     * Gibt den Code für die Kopfladezeit zurück
     *
     * @return Kopfladezeit
     */
    public int getHeadTime() {
        return headTime;
    }

    /**
     * Setzt den Code für die Kopfladezeit
     *
     * @param headTime Code für Kopfladezeit
     */
    public void setHeadTime(int headTime) {
        this.headTime = headTime;
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

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op Operand
     * @param i Nummer des Bits
     * @return true - wenn das Bit gesetzt ist, false - sonst
     */
    private boolean getBit(int op, int i) {
        return (((op >> i) & 0x1) == 0x1);
    }
}
