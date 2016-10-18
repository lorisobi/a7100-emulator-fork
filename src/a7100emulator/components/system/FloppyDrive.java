/*
 * FloppyDrive.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   12.04.2014 - Parameter für Initialisierung hinzugefügt
 *              - Formatierung überarbeitet
 *   18.11.2014 - getBit durch BitTest.getBit ersetzt
 *              - Interface StateSavable implementiert
 *   30.07.2015 - Spurpositionierung und Lesen Sektor Identifikationsfeld
 *                implementiert
 *   16.08.2015 - Parameterreihenfolge readData und writeData geändert
 *              - Laden von Binärdateien, Angabe Imagetyp entfernt
 *   24.07.2016 - getDisk() hinzugefügt
 *   29.07.2016 - IOException beim Lesen und Speichern von Images hinzugefügt
 *   09.08.2016 - Logger hinzugefügt
 *   13.10.2016 - Name für leere Disketten ergänzt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.FloppyImageParser;
import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung eines Diskettenlaufwerkes.
 *
 * @author Dirk Bräuer
 */
public class FloppyDrive implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(FloppyDrive.class.getName());

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
    private FloppyDisk disk;
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
     * Zylindernummer der letzten Spurpositionierung
     */
    private int positionCylinder = 0;
    /**
     * Kopfnummer der letzten Spurpositionierung
     */
    private int positionHead = 0;
    /**
     * Status des Motors
     */
    private boolean motor;
    /**
     * Status der Verriegelung
     */
    private boolean lock;
    /**
     * Gewählte Schrittrichtung
     */
    private boolean stepDirection;
    /**
     * Gewählte Seite
     */
    private boolean selectedHead;
    /**
     * Aktuelles Schrittsignal
     */
    private boolean step;
    /**
     * Schreiben erlaubt
     */
    private boolean writeEnabled;
    /**
     * Index erkannt
     */
    private boolean index = false;

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
        if (cylinder == 0 && head == 0 && BitTest.getBit(mod, 7)) {
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
     * Liefert die eingelegte Diskette zurück.
     *
     * @return Verweis auf eingelegte Diskette oder <code>null</code> wenn keine
     * Diskette eingelegt ist
     */
    public FloppyDisk getDisk() {
        return disk;
    }

    /**
     * Erzeugt eine leere Diskette
     */
    public void newDisk() {
        disk = new FloppyDisk("[Leere Diskette]");
    }

    /**
     * Schreibt Daten auf die eingelegte Diskette
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @param data Daten
     */
    public void writeData(int cylinder, int head, int sector, byte[] data) {
        if (disk == null) {
            return;
        }
        disk.writeData(cylinder, head, sector, data);
    }

    /**
     * Speichert die Diskette als Image
     *
     * @param image Image-File
     * @throws java.io.IOException Wenn das Speichern der Diskette auf dem
     * Datenträger nicht erfolgreich war
     */
    public void saveDiskToFile(File image) throws IOException {
        if (disk == null) {
            return;
        }
        disk.saveDisk(image);
    }

    /**
     * Lädt eine Diskette aus einer Datei
     *
     * @param file Image
     * @throws java.io.IOException Wenn beim Lesen des Images ein Fehler
     * auftritt
     */
    public void loadDiskFromFile(File file) throws IOException {
        disk = FloppyImageParser.loadDiskFromImageFile(file);
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
     * @param head Kopfnummer
     * @param sector Sketornummer
     * @param cnt Anzahl der zu lesenden Bytes
     * @return gelesene Daten
     */
    public byte[] readData(int cylinder, int head, int sector, int cnt) {
        if (disk == null) {
            return null;
        }
        return disk.readData(cylinder, head, sector, cnt);
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
    public boolean isDiskInsert() {
        return disk != null;
    }

    /**
     * Leitet die Spurpositionierung ein
     *
     * @param cylinder Zylinder
     * @param head Kopfnummer
     */
    public void setTrackPosition(int cylinder, int head) {
        this.positionCylinder = cylinder;
        this.positionHead = head;
    }

    /**
     * Liest das Sektor-Identifikationsfeld nach vorheriger Spurpositionierung
     * <p>
     * TODO: Positionierung auch beim Schreiben und Lesen verändern?
     *
     * @return Sektor-Identifikationsfeld
     */
    public byte[] readSectorID() {
        byte[] result = new byte[5];
        result[0] = (byte) (positionCylinder & 0xFF);
        result[1] = (byte) ((positionCylinder >> 8) & 0xFF);
        result[2] = (byte) (positionHead);
        result[3] = 1;
        result[4] = (byte) ((disk.getSectorFormat(positionCylinder, positionHead, 1)) << 4);
        return result;
    }

    /**
     * Schaltet den Motor des Laufwerks an oder aus.
     *
     * @param motor <code>true</code> - wenn Motor an, false - sonst
     */
    public void setMotor(boolean motor) {
        this.motor = motor;
    }

    /**
     * Schaltet die Verriegelung des Laufwerks.
     *
     * @param lock <code>true</code> - wenn verriegelt, false - sonst
     */
    public void setLock(boolean lock) {
        this.lock = lock;
    }

    /**
     * Setzt die Schrittrichtung des Laufwerks.
     *
     * @param direction Schrittrichtung
     */
    public void setDirection(boolean direction) {
        this.stepDirection = direction;
    }

    /**
     * Wählt die Seite/den Kopf des Laufwerks
     *
     * @param head <code>true</code> Seite 1, <code>false</code> Seite 0
     */
    public void setHead(boolean head) {
        this.selectedHead = head;
    }

    /**
     * Gibt an, ob das Laufwerk auf Spur 0 positioniert ist.
     *
     * @return <code>true</code> für Spur 0, <code>false</code> sonst
     */
    public boolean isTrack0() {
        return positionCylinder == 0;
    }

    /**
     * Gibt an, ob ein Index erkannt wurde.
     *
     * @return <code>true</code> für Index, <code>false</code> sonst
     */
    public boolean isIndex() {
        // Wenn Motor aktiviert schalte Index um
        // TODO: Besser implementieren
        index = motor ? !index : false;
        return index;
    }

    /**
     * Gibt an, ob ein Schreibschutz vorhanden ist.
     *
     * @return <code>true</code> für Schreibschutz, <code>false</code> sonst
     */
    public boolean isWriteProtect() {
        return isDiskInsert() && disk.isWriteProtect();
    }

    /**
     * Setzt die Schreiberlaubnis für das Laufwerk
     *
     * @param we <code>true</code> wenn Schreiben erlaubt, <code>false</code>
     * sonst
     */
    public void setWriteEnabled(boolean we) {
        this.writeEnabled = we;
    }

    /**
     * Setzt das Schrittsignal für das Laufwerk.
     *
     * @param step Schrittsignal
     */
    public void setStep(boolean step) {
        // Prüfe auf steigende Flanke
        if (!this.step && step) {
            if (stepDirection) {
                if (positionCylinder < cylinder) {
                    positionCylinder++;
                }
            } else {
                if (positionCylinder > 0) {
                    positionCylinder--;
                }
            }
            System.out.println("Neuer Cylinder: " + positionCylinder);
        }
        // Speichere Signal
        this.step = step;
    }

    /**
     * Speichert den Zustand des Laufwerks in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeUTF(driveType.name());
        dos.writeInt(positionCylinder);
        dos.writeInt(positionHead);

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
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        driveType = DriveType.valueOf(dis.readUTF());
        positionCylinder = dis.readInt();
        positionHead = dis.readInt();

        boolean diskInserted = dis.readBoolean();
        if (diskInserted) {
            disk = new FloppyDisk();
            disk.loadState(dis);
        }
    }
}
