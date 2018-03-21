/*
 * FloppyImageParser.java
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
 *   16.08.2014 - Erstellt aus FloppyDisk
 *   26.07.2016 - Doppelte Typecasts entfernt
 *   09.08.2016 - Logger hinzugefügt
 *   25.09.2016 - Kommentare Lesen bei Teledisk ergänzt
 *   13.10.2016 - Diskettennamen setzen hinzugefügt
 */
package a7100emulator.Tools;

import a7100emulator.components.system.*;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Klasse mit statischen Funktionen zum Einlesen eines Diskettenabbildes.
 *
 * @author Dirk Bräuer
 */
public class FloppyImageParser {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(FloppyImageParser.class.getName());

    /**
     * Privater Konstruktor, da nur statische Funktionen verwendet werden.
     */
    private FloppyImageParser() {
    }

    /**
     * Erstellt eine Diskette basierend auf einer Imagedatei.
     *
     * @param image Datei zum Laden
     * @return Gelesene FloppyDisk ode <code>null</code> wenn das Lesen
     * abgebrochen wurde
     * @throws java.io.IOException Wenn beim Lesen des Images ein Fehler
     * auftritt
     */
    public static FloppyDisk loadDiskFromImageFile(File image) throws IOException {
        if (image == null || !image.isFile()) {
            throw new IllegalArgumentException("Ungültiges Diskettenabbild.");
        }

        InputStream in = new FileInputStream(image);
        byte[] buffer = new byte[(int) image.length()];
        in.read(buffer);
        in.close();

        // Versuche anhand der Dateierweiterung Imagetyp zu erkennen
        String extension = image.getName().substring(image.getName().length() - 3, image.getName().length()).toLowerCase();

        // Diskette auslesen
        FloppyDisk disk = null;

        switch (extension) {
            case "imd": {
                disk = readImagediskFile(buffer);
            }
            break;
            case "td0": {
                disk = readTelediskFile(buffer);
            }
            break;
            case "dmk": {
                disk = readDMKFile(buffer);
            }
            break;
            case "cqm": {
                disk = readCopyQMFile(buffer);
            }
            break;
            default: {
                NumberFormat integerFormat = NumberFormat.getIntegerInstance();
                integerFormat.setGroupingUsed(false);
                JFormattedTextField editCylinder = new JFormattedTextField(NumberFormat.getIntegerInstance());
                JFormattedTextField editHeads = new JFormattedTextField(NumberFormat.getIntegerInstance());
                JFormattedTextField editSectorsPerTrack = new JFormattedTextField(NumberFormat.getIntegerInstance());
                JFormattedTextField editBytesPerSector = new JFormattedTextField(NumberFormat.getIntegerInstance());
                JFormattedTextField editSectorsInTrack0 = new JFormattedTextField(NumberFormat.getIntegerInstance());
                JFormattedTextField editBytesPerSectorTrack0 = new JFormattedTextField(NumberFormat.getIntegerInstance());
                editCylinder.setValue(80);
                editHeads.setValue(2);
                editSectorsPerTrack.setValue(16);
                editBytesPerSector.setValue(256);
                editSectorsInTrack0.setValue(16);
                editBytesPerSectorTrack0.setValue(128);
                JPanel panelEdit = new JPanel(new GridLayout(6, 2));
                panelEdit.add(new JLabel("Anzahl der Zylinder:"));
                panelEdit.add(editCylinder);
                panelEdit.add(new JLabel("Anzahl der Seiten:"));
                panelEdit.add(editHeads);
                panelEdit.add(new JLabel("Sektoren pro Spur:"));
                panelEdit.add(editSectorsPerTrack);
                panelEdit.add(new JLabel("Bytes pro Sektor:"));
                panelEdit.add(editBytesPerSector);
                panelEdit.add(new JLabel("Sektoren in Spur 0:"));
                panelEdit.add(editSectorsInTrack0);
                panelEdit.add(new JLabel("Bytes pro Sektor Spur 0:"));
                panelEdit.add(editBytesPerSectorTrack0);
                if (JOptionPane.showConfirmDialog(null, panelEdit, "Image laden", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    int cylinder = ((Number) editCylinder.getValue()).intValue();
                    int heads = ((Number) editHeads.getValue()).intValue();
                    int sectorsPerTrack = ((Number) editSectorsPerTrack.getValue()).intValue();
                    int bytesPerSector = ((Number) editBytesPerSector.getValue()).intValue();
                    int sectorsInTrack0 = ((Number) editSectorsInTrack0.getValue()).intValue();
                    int bytesPerSectorTrack0 = ((Number) editBytesPerSectorTrack0.getValue()).intValue();
                    disk = parseBinaryFile(buffer, cylinder, heads, sectorsPerTrack, bytesPerSector, sectorsInTrack0, bytesPerSectorTrack0);
                }
            }
            break;
        }

        // Name der Datei als Diskettenname setzen
        if (disk != null) {
            disk.setDiskName(image.getName());
        }

        return disk;
    }

    /**
     * Liest eine binäre Imagedatei unter Verwendung der angegebenen Geometrie.
     *
     * @param buffer Image Daten
     * @param cylinders Anzahl der Zylinder
     * @param heads Anzahl der Köpfe
     * @param sectorsPerTrack Anzahl der Sektoren pro Spur
     * @param bytesPerSector Anzahl der Bytes pro Sektor
     * @param sectorsInTrack0 Anzahl der Sektoren in Spur 0
     * @param bytesPerSectorTrack0 Anzahl der Bytes pro Sektor in Spur 0
     * @return Gelesene FloppyDisk oder <code>null</code> im Fehlerfall
     */
    private static FloppyDisk parseBinaryFile(byte[] buffer, int cylinders, int heads, int sectorsPerTrack, int bytesPerSector, int sectorsInTrack0, int bytesPerSectorTrack0) {
        FloppyDisk disk = new FloppyDisk();

        int pos = 0;
        byte sectorData[];

        for (int c = 0; c < cylinders; c++) {
            for (int h = 0; h < heads; h++) {
                if (c == 0 && h == 0) {
                    sectorData = new byte[bytesPerSectorTrack0];
                    for (int s = 1; s <= sectorsInTrack0; s++) {
                        disk.checkAndAddDiskGeometry(c, h, s, bytesPerSectorTrack0);
                        System.arraycopy(buffer, pos, sectorData, 0, bytesPerSectorTrack0);
                        disk.writeData(c, h, s, sectorData);
                        pos += bytesPerSectorTrack0;
                    }
                } else {
                    sectorData = new byte[bytesPerSector];
                    for (int s = 1; s <= sectorsPerTrack; s++) {
                        disk.checkAndAddDiskGeometry(c, h, s, bytesPerSector);
                        System.arraycopy(buffer, pos, sectorData, 0, bytesPerSector);
                        disk.writeData(c, h, s, sectorData);
                        pos += bytesPerSector;
                    }
                }
            }
        }
        return disk;
    }

    /**
     * Liest ein Imagedisk Image.
     *
     * @param buffer Image Daten
     * @return Gelesene FloppyDisk oder <code>null</code> im Fehlerfall
     */
    private static FloppyDisk readImagediskFile(byte[] buffer) {
        FloppyDisk disk = new FloppyDisk();

        int pos = 0;
        byte[] sectorData;

        // Lese bis Ende des Kommentarfeldes
        while (buffer[pos] != 0x1A) {
            pos++;
        }
        pos++;

        while (pos < buffer.length) {
            // MOD-Value
            int modeValue = buffer[pos++];
            // Lese Zylindernummer
            int cylinder = buffer[pos++];
            // Lese Kopfnummer
            int head = buffer[pos++];
            boolean useSectorCylinderMap = BitTest.getBit(head, 7);
            boolean useSectorHeadMap = BitTest.getBit(head, 6);
            head = head & 0x01;

            // Lese Anzahl der Sektoren
            int sectorCount = buffer[pos++];

            // Lese Anzahl der Bytes pro Sektor
            int sectorSizeBytes = (int) Math.pow(2, buffer[pos++] + 7);
            int[] sectorNumberingMap = new int[sectorCount];
            for (int i = 0; i < sectorCount; i++) {
                sectorNumberingMap[i] = buffer[pos++];
            }
            int[] sectorCylinderMap = new int[sectorCount];
            if (useSectorCylinderMap) {
                for (int i = 0; i < sectorCount; i++) {
                    sectorCylinderMap[i] = buffer[pos++];
                }
            }
            int[] sectorHeadMap = new int[sectorCount];
            if (useSectorHeadMap) {
                for (int i = 0; i < sectorCount; i++) {
                    sectorHeadMap[i] = buffer[pos++];
                }
            }
            for (int i = 0; i < sectorCount; i++) {
                if (useSectorCylinderMap) {
                    cylinder = sectorCylinderMap[i];
                }
                if (useSectorHeadMap) {
                    head = sectorHeadMap[i];
                }
                int sector = sectorNumberingMap[i];

                disk.checkAndAddDiskGeometry(cylinder, head, sector, sectorSizeBytes);

                int sectorDataInfo = buffer[pos++];

                switch (sectorDataInfo) {
                    case 0x00: {
                        System.out.println("Sektordaten konnten nicht gelesen werden!");
                    }
                    break;
                    case 0x01:
                    case 0x03:
                    case 0x05:
                    case 0x07: {
                        sectorData = new byte[sectorSizeBytes];
                        System.arraycopy(buffer, pos, sectorData, 0, sectorSizeBytes);
                        disk.writeData(cylinder, head, sector, sectorData);
                        pos += sectorSizeBytes;
                    }
                    break;
                    case 0x02:
                    case 0x04:
                    case 0x06:
                    case 0x08: {
                        byte repeatData = buffer[pos++];
                        sectorData = new byte[sectorSizeBytes];
                        Arrays.fill(sectorData, repeatData);
                        disk.writeData(cylinder, head, sector, sectorData);
                    }
                    break;
                }
            }
        }
        return disk;
    }

    /**
     * Liest ein Teledisk Image
     *
     * @param buffer Image Daten
     * @return Gelesene FloppyDisk oder <code>null</code> im Fehlerfall
     */
    private static FloppyDisk readTelediskFile(byte[] buffer) {
        FloppyDisk disk = new FloppyDisk();

        int pos = 0;
        byte[] sectorData;

        String signature = "" + (char) buffer[pos++] + (char) buffer[pos++];
        int sequence = buffer[pos++];
        int checksequence = buffer[pos++];
        int version = buffer[pos++];
        int datarate = buffer[pos++];
        int drivetype = buffer[pos++];
        int stepping = buffer[pos++];
        int dosallocation = buffer[pos++];
        int sides = buffer[pos++];
        int crc = buffer[pos++] | buffer[pos++] << 8;

        if (BitTest.getBit(stepping, 7)) {
            int commentCRC = buffer[pos++] | buffer[pos++] << 8;
            int commentLength = buffer[pos++] | buffer[pos++] << 8;
            int commentYear = buffer[pos++];
            int commentMonth = buffer[pos++];
            int commentDay = buffer[pos++];
            int commentHour = buffer[pos++];
            int commentMinute = buffer[pos++];
            int commentSecond = buffer[pos++];
            byte[] commentData = new byte[commentLength];
            System.arraycopy(buffer, pos, commentData, 0, commentLength);
            pos += commentLength;
        }

        int sectorCount;
        do {
            sectorCount = buffer[pos++];
            if (sectorCount != -1) {
                int cylinder = buffer[pos++];
                int head = buffer[pos++] & 0x01;
                int trackCRC = buffer[pos++];

                for (int i = 0; i < sectorCount; i++) {
                    int cylinderNumber = buffer[pos++];
                    int headNumber = buffer[pos++] & 0x1;
                    int sectorNumber = buffer[pos++];
                    int sectorSize = buffer[pos++];
                    int sectorSizeBytes = (int) Math.pow(2, sectorSize + 7);
                    int flags = buffer[pos++];
                    int sectorCRC = buffer[pos++];

                    disk.checkAndAddDiskGeometry(cylinderNumber, headNumber, sectorNumber, sectorSizeBytes);

                    if (!BitTest.getBit(flags, 5) && !BitTest.getBit(flags, 6)) {
                        int dataBlockSize = (buffer[pos++] & 0xFF) | ((int) buffer[pos++]) << 8;
                        int encoding = buffer[pos++];
                        switch (encoding) {
                            case 0x00: {
                                // RAW
                                sectorData = new byte[sectorSizeBytes];
                                System.arraycopy(buffer, pos, sectorData, 0, sectorSizeBytes);
                                disk.writeData(cylinder, head, sectorNumber, sectorData);
                            }
                            break;
                            case 0x01: {
                                // Repeat 2 Bytes
                                int blockPos = 0;

                                // Hole bisherige Sektordaten
                                sectorData = disk.readData(cylinder, head, sectorNumber, sectorSizeBytes);

                                // Füge Bytes ein
                                for (int j = 0; j < dataBlockSize - 1; j = j + 4) {
                                    int size = (buffer[pos] & 0xFF) | ((int) buffer[pos + 1] & 0xFF) << 8;
                                    byte pat1 = buffer[pos + 2];
                                    byte pat2 = buffer[pos + 3];
                                    for (int k = 0; k < size; k++) {
                                        sectorData[blockPos++] = pat1;
                                        sectorData[blockPos++] = pat2;
                                    }
                                }

                                // Schreibe aktualisierte Sektordaten
                                disk.writeData(cylinder, head, sectorNumber, sectorData);
                            }
                            break;
                            case 0x02: {
                                // Run Length Encoding
                                int blockPos = 0;

                                // Hole bisherige Sektordaten
                                sectorData = disk.readData(cylinder, head, sectorNumber, sectorSizeBytes);

                                for (int j = 0; j < dataBlockSize - 1; j++) {
                                    int length = buffer[pos + j] & 0xFF;
                                    if (length == 00) {
                                        int l2 = buffer[pos + j + 1] & 0xFF;
                                        System.arraycopy(buffer, pos + j + 2, sectorData, blockPos, l2);
                                        j += l2 + 1;
                                        blockPos += l2;
                                    } else {
                                        int r = buffer[pos + j + 1] & 0xFF;
                                        for (int k = 0; k < r; k++) {
                                            System.arraycopy(buffer, pos + j + 2, sectorData, blockPos, length * 2);
                                            blockPos += length * 2;
                                        }
                                        j += 1 + length * 2;
                                    }
                                }
                                // Schreibe aktualisierte Sektordaten
                                disk.writeData(cylinder, head, sectorNumber, sectorData);
                            }
                            break;
                        }
                        pos += dataBlockSize - 1;
                    }
                }
            }
        } while (sectorCount != -1);

        return disk;
    }

    /**
     * Liest ein DMK Image.
     *
     * @param buffer Image Daten
     * @return Gelesene FloppyDisk oder <code>null</code> im Fehlerfall
     */
    private static FloppyDisk readDMKFile(byte[] buffer) {
        FloppyDisk disk = new FloppyDisk();

        int pos = 0;
        byte[] sectorData;

        // Lese Header ab Byte 00
        // Byte 00: Schreibschutz (FF bei Schreibschutz, 00 sonst)
        int writeProtectImage = buffer[pos++] & 0xFF;
        disk.setWriteProtect(writeProtectImage == 0xFF);
        // Byte 01: Anzahl der Tracks
        int trackCount = buffer[pos++] & 0xFF;
        // Byte02,03 : Länge eines Tracks
        int trackSize = (buffer[pos] & 0xFF) | ((int) buffer[pos + 1] & 0xFF) << 8;
        // Byte 4: Flags
        int flags = buffer[pos++];
        // Bit 4: 1 - Single Side / 0 - Double Side
        boolean singleSide = BitTest.getBit(flags, 4);
        // Bit 6: 1 - Single Density / 0 - Double Density
        boolean singleDensity = BitTest.getBit(flags, 6);

        // Gesamtgröße des Headers
        final int HEADER_SIZE = 0x10;
        // Anzahl der Seiten
        int sides = (singleSide) ? 1 : 2;

        int diskSize = 0;
        // Für alle Tracks
        for (int t = 0; t < trackCount; t++) {
            // Neuer Track
            // Für alle Seiten
            for (int s = 0; s < sides; s++) {
                // Platz für alle IDAM Pointer (2 Bytes)
                int[] idam = new int[64];
                // Lese IDAMS
                int sectorCount = 0;
                do {
                    // Lese Zeiger auf IDAM
                    idam[sectorCount] = (buffer[HEADER_SIZE + sides * t * trackSize + s * trackSize + sectorCount * 2] & 0xFF) | ((int) buffer[HEADER_SIZE + sides * t * trackSize + s * trackSize + sectorCount * 2 + 1] & 0xFF) << 8;
                    sectorCount++;
                } while (idam[sectorCount - 1] != 0);
                sectorCount--;

                // Für alle gelesenen IDAMS
                for (int sec = 0; sec < sectorCount; sec++) {
                    // Prüfe auf Double Density (Bit 15 in IDAM)
                    boolean doubleDensity = BitTest.getBit(idam[sec], 15);
                    // Lösche Bit 14/15
                    idam[sec] &= ~0xC000;
                    // Berechne Position des Datenblocks (Letztes FE)
                    // Header + Beginn IDAM Feld
                    int blockPos = HEADER_SIZE + sides * t * trackSize + s * trackSize + idam[sec] + 1;
                    blockPos += doubleDensity ? 0 : 1;
                    int cylinder = buffer[blockPos++];
                    blockPos += doubleDensity ? 0 : 1;
                    int head = buffer[blockPos++];
                    blockPos += doubleDensity ? 0 : 1;
                    int sector = buffer[blockPos++];
                    blockPos += doubleDensity ? 0 : 1;
                    int sectorSize = (int) Math.pow(2, 7 + buffer[blockPos++]);

                    disk.checkAndAddDiskGeometry(cylinder, head, sector, sectorSize);

                    // CRC überspringen
                    blockPos += doubleDensity ? 2 : 4;

                    // Lese solange Bytes bis Ende Data AM erreicht ist
                    byte data_am;
                    do {
                        data_am = buffer[blockPos++];
                    } while ((data_am != ((byte) 0xFB)) && (data_am != ((byte) 0xF8)));
                    blockPos += doubleDensity ? 0 : 1;
                    sectorData = new byte[sectorSize];
                    System.arraycopy(buffer, blockPos, sectorData, 0, sectorSize);
                    disk.writeData(cylinder, head, sector, sectorData);
                    diskSize += sectorSize;
                }
            }
        }
        return disk;
    }

    /**
     * Liest ein CopyQM Image.
     *
     * @param buffer Image Daten
     * @return Gelesene FloppyDisk oder <code>null</code> im Fehlerfall
     */
    private static FloppyDisk readCopyQMFile(byte[] buffer) {
        FloppyDisk disk = new FloppyDisk();

        int pos = 0;

        // Lese Header ab Byte 00
        // Lese Signatur CQ
        String signature = "" + (char) buffer[pos++] + (char) buffer[pos++];
        // Byte 02: ??
        pos++;
        // Byte 03-04: Lese Sektorgröße
        int sectorSize = (buffer[pos++] & 0xFF) | ((int) buffer[pos++] & 0xFF) << 8;
        // Byte 05-0F: ???
        pos = 0x10;
        // Byte 10-11: Anzahl der Sektoren pro Track
        int sectorsPerTrack = (buffer[pos++] & 0xFF) | ((int) buffer[pos++] & 0xFF) << 8;
        // Byte 12-13: Anzahl der Köpfe
        int heads = (buffer[pos++] & 0xFF) | ((int) buffer[pos++] & 0xFF) << 8;
        // Byte 14-59 : ??? Kommentar + ???
        pos = 0x5A;
        // Byte 5A: Anzahl benutzter Zylinder
        int tracksUsed = buffer[pos++];
        // Byte 5B: Anzahl Zylinder
        int tracksTotal = buffer[pos++];
        // Byte 5C-84: ???
        pos = 0x85;

        byte[] raw = new byte[tracksTotal * heads * sectorsPerTrack * sectorSize];
        int rawpos = 0;

        // Lese Datenblöcke
        while (pos < buffer.length) {
            int length = (short) ((buffer[pos++] & 0xFF) | ((int) buffer[pos++] & 0xFF) << 8);

            if (length < 0) {
                // Ein Byte (-length) mal wiederholt
                byte fillByte = buffer[pos++];
                Arrays.fill(raw, rawpos, rawpos - length, fillByte);
                rawpos -= length;
            } else {
                // length Bytes
                System.arraycopy(buffer, pos, raw, rawpos, length);
                pos += length;
                rawpos += length;
            }
        }

        rawpos = 0;
        byte[] sectorData;
        // Für alle Tracks
        for (int t = 0; t < tracksUsed; t++) {
            // Für alle Seiten
            for (int h = 0; h < heads; h++) {
                // Für alle Sektoren
                for (int s = 1; s <= sectorsPerTrack; s++) {
                    disk.checkAndAddDiskGeometry(t, h, s, sectorSize);
                    sectorData = new byte[sectorSize];
                    System.arraycopy(raw, rawpos, sectorData, 0, sectorSize);
                    disk.writeData(t, h, s, sectorData);
                    rawpos += sectorSize;
                }
            }
        }
        return disk;
    }
}
