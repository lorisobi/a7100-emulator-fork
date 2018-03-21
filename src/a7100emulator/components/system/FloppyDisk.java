/*
 * FloppyDisk.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
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
 *   02.04.2014 - Kommentare vervollständigt
 *   12.04.2014 - Funktionen zum Lesen von Images, Neue Datenstruktur
 *   25.05.2014 - Klasse umbenannt in FloppyDisk
 *   18.11.2014 - getBit in BitTest.getBit geändert
 *              - Interface StateSavable implementiert
 *              - Fehler beim Laden des Zustands behoben
 *   18.12.2014 - Fehler beim Wechsel von Kopf behoben
 *   25.07.2015 - Fehler DMK-Images behoben
 *   30.07.2015 - Sektorformat lesen implementiert
 *   09.08.2015 - Javadoc korrigiert
 *   14.08.2015 - Lesen von CopyQM Images implementiert
 *   16.08.2015 - Bereich für Daten in checkAndAddDiskGeometry hinzugefügt
 *              - writeData, checkAndAddDiskGeometry public
 *              - Parameterreihenfolge readData und writeData geändert
 *              - Lesen von Images ausgelagert
 *              - getFlatContent hinzugefügt
 *   25.07.2016 - Rückgabe null bei fehlendem Sektor
 *   29.07.2016 - IOException beim Speichern von Images hinzugefügt
 *   09.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 *   13.10.2016 - Diskettennamen hinzugefügt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung einer Diskette
 *
 * @author Dirk Bräuer
 */
public class FloppyDisk implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(FloppyDisk.class.getName());

    /**
     * Daten der Diskette
     */
    byte[][][][] diskData = new byte[0][0][0][0];

    /**
     * Schreibschutz
     */
    private boolean writeProtect = false;

    /**
     * Name der Diskette - wird für das Anzeigen der Status-Informationen
     * genutzt
     */
    private String diskName = "";

    /**
     * Erstellt eine leere Diskette
     */
    public FloppyDisk() {
    }

    /**
     * Erstellt eine leere Diskette
     *
     * @param diskName Name der Diskette
     */
    public FloppyDisk(String diskName) {
        this.diskName = diskName;
    }

    /**
     * Gibt an ob die Diskette schreibgeschützt ist
     *
     * @return true wenn schreibgeschützt, false sonst
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
     * Speichert die Diskette in die angegebene Datei
     *
     * @param image Datei
     * @throws java.io.IOException Wenn das Speichern der Diskette auf dem
     * Datenträger nicht erfolgreich war
     */
    public void saveDisk(File image) throws IOException {
        FileOutputStream fos = new FileOutputStream(image);
        fos.write(getFlatData());
        fos.close();
        diskName = image.getName();
    }

    /**
     * Gibt den Disketteninhalt als eindimensionales Array zurück.
     *
     * @return Disketteninhalt als eindimensionales Array
     */
    public byte[] getFlatData() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            for (byte[][][] cylinder : diskData) {
                for (byte[][] head : cylinder) {
                    for (byte[] sector : head) {
                        bos.write(sector);
                    }
                }
            }
            return bos.toByteArray();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return null;
    }

    /**
     * Formatiert den angegebenen Bereich der Diskette
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param mod Modifizierung
     * @param formatData Datenbytes
     * @param interleave Interleave-Faktor
     * @param sectorsPerTrack Anzahl der Sektoren pro Zylinder
     * @param bytesPerSector Anzahl der Bytes pro Sektor
     */
    void format(int cylinder, int head, int mod, int[] formatData, int interleave, int sectorsPerTrack, int bytesPerSector) {
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            checkAndAddDiskGeometry(cylinder, head, sector + 1, bytesPerSector);
            diskData[cylinder][head][sector][0] = (byte) formatData[0];
            Arrays.fill(diskData[cylinder][head][sector], 1, bytesPerSector - 1, (byte) formatData[1]);
        }
    }

    /**
     * Liest Daten an der angegebenen Position von der Diskette.
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @param cnt Anzahl der Bytes
     * @return gelesene Daten oder <code>null</code> wenn die angegebene
     * Position auf der Diskette nicht vorhanden ist.
     */
    public byte[] readData(int cylinder, int head, int sector, int cnt) {
        if (checkDiskGeometry(cylinder, head, sector)) {
            byte[] res = new byte[cnt];
            byte[] sectorData = diskData[cylinder][head][sector - 1];
            int byteToCopy = Math.min(cnt, sectorData.length);
            System.arraycopy(sectorData, 0, res, 0, byteToCopy);
            cnt -= byteToCopy;
            if (cnt > 0) {
                if (diskData[cylinder][head].length == sector) {
                    // Wenn letzter Sektor
                    sector = 1;
                    if (diskData[cylinder].length == (head + 1)) {
                        // Wenn Letzter Kopf
                        head = 0;
                        if (diskData.length == (cylinder + 1)) {
                            // Letzter Zylinder
                            throw new IllegalStateException("End of Disk");
                        } else {
                            // Nicht letzter Zylinder
                            cylinder++;
                        }
                    } else {
                        // Nicht letzter Kopf
                        head++;
                    }
                } else {
                    // Nicht letzter Sector
                    sector++;
                }
                byte[] nextData = readData(cylinder, head, sector, cnt);
                System.arraycopy(nextData, 0, res, byteToCopy, nextData.length);
            }
            return res;
        } else {
            return null;
        }
    }

    /**
     * Schreibt Daten auf die Diskette.
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @param data Zu schreibende Daten
     */
    public void writeData(int cylinder, int head, int sector, byte[] data) {
        byte[] sectorData = diskData[cylinder][head][sector - 1];
        int byteToCopy = Math.min(data.length, sectorData.length);
        System.arraycopy(data, 0, sectorData, 0, byteToCopy);
        if (byteToCopy < data.length) {
            byte[] resData = new byte[data.length - byteToCopy];
            System.arraycopy(data, byteToCopy, resData, 0, data.length - byteToCopy);
            if (diskData[cylinder][head].length == sector) {
                // Wenn letzter Sektor
                sector = 1;
                if (diskData[cylinder].length == (head + 1)) {
                    // Wenn letzter Kopf
                    head = 0;
                    if (diskData.length == (cylinder + 1)) {
                        // Wenn letzter Zylinder
                        throw new IllegalStateException("End of Disk");
                    } else {
                        // Nicht letzter Zylinder
                        cylinder++;
                    }
                } else {
                    // Nicht letzter Kopf
                    head++;
                }
            } else {
                // Nicht letzter Sektor
                sector++;
            }
            writeData(cylinder, head, sector, resData);
        }
    }

    /**
     * Fügt der Diskette einen neuen Zylinder hinzu
     *
     * @param cylinder Zylindernummer
     */
    private void addCylinder(int cylinder) {
        byte[][][][] newDiskData = new byte[cylinder + 1][0][0][0];
        System.arraycopy(diskData, 0, newDiskData, 0, diskData.length);
        diskData = newDiskData;
    }

    /**
     * Fügt der Diskette eine neue Seite hinzu
     *
     * @param cylinder Zylindernummer
     * @param head Seitennummer/Kopfnummer
     */
    private void addHead(int cylinder, int head) {
        byte[][][] newCylinderData = new byte[head + 1][0][0];
        System.arraycopy(diskData[cylinder], 0, newCylinderData, 0, diskData[cylinder].length);
        diskData[cylinder] = newCylinderData;
    }

    /**
     * Fügt der Diskette einen neuen Sektor hinzu
     *
     * @param cylinder Zylinder
     * @param head Seite/Kopf
     * @param sector Sektor
     */
    private void addSector(int cylinder, int head, int sector) {
        byte[][] newHeadData = new byte[sector][0];
        System.arraycopy(diskData[cylinder][head], 0, newHeadData, 0, diskData[cylinder][head].length);
        diskData[cylinder][head] = newHeadData;
    }

    /**
     * Prüft ob die angegebene Position auf der Diskette verfügbar ist.
     *
     * @param cylinder Zylindernummer
     * @param head Seite/Kopfnummer
     * @param sector Sektor
     * @return <code>true</code> wenn der Sektor vorhanden ist.
     * <code>false</code> sonst
     */
    public boolean checkDiskGeometry(int cylinder, int head, int sector) {
        return !(cylinder >= diskData.length || head >= diskData[cylinder].length || (sector - 1) >= diskData[cylinder][head].length);
    }

    /**
     * Prüft ob die angegebene Position der Diskette verfügbar ist und ergänzt
     * ggf. die fehlende Geometrie und legt einen Datenbereich für den Sektor
     * an.
     *
     * @param cylinder Zylindernummer
     * @param head Seite/Kopfnummer
     * @param sector Sektor
     * @param sectorSize Größe des Sektors
     */
    public void checkAndAddDiskGeometry(int cylinder, int head, int sector, int sectorSize) {
        if (cylinder >= diskData.length) {
            addCylinder(cylinder);
        }
        if (head >= diskData[cylinder].length) {
            addHead(cylinder, head);
        }
        if ((sector - 1) >= diskData[cylinder][head].length) {
            addSector(cylinder, head, sector);
        }
        diskData[cylinder][head][sector - 1] = new byte[sectorSize];
    }

    /**
     * Gibt das Sektorformat der ausgewählten Spur zurück.
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @return Sektorformat 00 - 128 Bytes, 01 - 256 Bytes, 10 - 512 Bytes, 11 -
     * 1024 Bytes
     */
    int getSectorFormat(int cylinder, int head, int sector) {
        switch (diskData[cylinder][head][sector - 1].length) {
            case 128:
                return 0;
            case 256:
                return 1;
            case 512:
                return 2;
            case 1024:
                return 3;
            default:
                return -1;
        }
    }

    /**
     * Gibt die Anzahl der Zylinder zurück.
     *
     * @return Anzahl der Zylinder
     */
    public int getCylinder() {
        return diskData.length;
    }

    /**
     * Gibt die Anzahl der Köpfe für den angegebenen Zylinder zurück.
     *
     * @param cylinder Zylindernummer
     * @return Anzahl der Köpfe
     */
    public int getHeads(int cylinder) {
        return diskData[cylinder].length;
    }

    /**
     * Gibt die Anzahl der Sektroen für den angegebenen Zylinder und Kopf
     * zurück.
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @return Anzahl der Sektoren
     */
    public int getSectors(int cylinder, int head) {
        return diskData[cylinder][head].length;
    }

    /**
     * Gibt die Datenbytes für den angegebenen Zylinder, Kopf und Sektor zurück.
     *
     * @param cylinder Zylindernummer
     * @param head Kopfnummer
     * @param sector Sektornummer
     * @return Sektorgröße in Bytes
     */
    public int getSectorSize(int cylinder, int head, int sector) {
        return diskData[cylinder][head][sector - 1].length;
    }

    /**
     * Speichert die Diskette in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(diskData.length);
        for (byte[][][] cylinder : diskData) {
            dos.writeInt(cylinder.length);
            for (byte[][] head : cylinder) {
                dos.writeInt(head.length);
                for (byte[] sector : head) {
                    dos.writeInt(sector.length);
                    dos.write(sector);
                }
            }
        }
        dos.writeBoolean(writeProtect);
        dos.writeUTF(diskName);
    }

    /**
     * Liest die Diskette aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        diskData = new byte[0][0][0][0];
        int cylinders = dis.readInt();
        for (int t = 0; t < cylinders; t++) {
            int heads = dis.readInt();
            for (int h = 0; h < heads; h++) {
                int sectors = dis.readInt();
                for (int s = 0; s < sectors; s++) {
                    int sectorSize = dis.readInt();
                    checkAndAddDiskGeometry(t, h, s + 1, sectorSize);
                    dis.read(diskData[t][h][s]);
                }
            }
        }
        writeProtect = dis.readBoolean();
        diskName = dis.readUTF();
    }

    /**
     * Gibt den Namen der Diskette zurück.
     *
     * @return Name der Diskette
     */
    public String getDiskName() {
        return diskName;
    }

    /**
     * Setzt den Namen der Diskette.
     *
     * @param diskName Name der Diskette
     */
    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }
}
