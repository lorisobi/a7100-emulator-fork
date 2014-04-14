/*
 * Disk.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *   12.04.2014 Funktionen zum Lesen von Images, Neue Datenstruktur
 *
 */
package a7100emulator.components.system;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung einer Diskette
 *
 * @author Dirk Bräuer
 */
public class Disk {

    /**
     * Enum mit unterstützten Images
     */
    public enum ImageType {

        /**
         * Binärdatei
         */
        BINARY,
        /**
         * Imagedisk Image
         */
        IMAGEDISK,
        /**
         * Teledisk Image
         */
        TELEDISK,
        /**
         * DMK Image
         */
        DMK
    }

    /**
     * Daten der Diskette
     */
    byte[][][][] diskData = new byte[0][0][0][0];

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
     * @param imageType Imagetyp
     */
    public Disk(File file, ImageType imageType) {
        InputStream in = null;

        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            switch (imageType) {
                case IMAGEDISK:
                    readImagediskFile(buffer);
                    break;
                case TELEDISK:
                    readTelediskFile(buffer);
                    break;
                case DMK:
                    readDMKFile(buffer);
                    break;
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
     * Liest eine binäre Imagedatei unter Verwendung der angegebenen Geometrie
     *
     * @param file Image Datei
     * @param cylinders Anzahl der Zylinder
     * @param heads Anzahl der Köpfe
     * @param sectorsPerTrack Anzahl der Sektoren pro Spur
     * @param bytesPerSector Anzahl der Bytes pro Sektor
     * @param sectorsInTrack0 Anzahl der Sektoren in Spur 0
     * @param bytesPerSectorTrack0 Anzahl der Bytes pro Sektor in Spur 0
     */
    public Disk(File file, int cylinders, int heads, int sectorsPerTrack, int bytesPerSector, int sectorsInTrack0, int bytesPerSectorTrack0) {
        InputStream in = null;

        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int pos = 0;

            diskData = new byte[cylinders][heads][][];

            for (int c = 0; c < cylinders; c++) {
                for (int h = 0; h < heads; h++) {
                    if (c == 0 && h == 0) {
                        diskData[c][h] = new byte[sectorsInTrack0][bytesPerSectorTrack0];
                        for (int s = 0; s < sectorsInTrack0; s++) {
                            System.arraycopy(buffer, pos, diskData[c][h][s], 0, bytesPerSectorTrack0);
                            pos += bytesPerSectorTrack0;
                        }
                    } else {
                        diskData[c][h] = new byte[sectorsPerTrack][bytesPerSector];
                        for (int s = 0; s < sectorsPerTrack; s++) {
                            System.arraycopy(buffer, pos, diskData[c][h][s], 0, bytesPerSector);
                            pos += bytesPerSector;
                        }
                    }
                }
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
     */
    public void saveDisk(File image) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(image);
            for (byte[][][] cylinder : diskData) {
                for (byte[][] head : cylinder) {
                    for (byte[] sector : head) {
                        fos.write(sector);
                    }
                }
            }
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
    void format(int cylinder, int head, int mod, int[] formatData, int interleave, int sectorsPerTrack, int bytesPerSector) {
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            checkAndAddDiskGeometry(cylinder, head, sector + 1);
            diskData[cylinder][head][sector] = new byte[bytesPerSector];
            diskData[cylinder][head][sector][0] = (byte) formatData[0];
            Arrays.fill(diskData[cylinder][head][sector], 1, bytesPerSector - 1, (byte) formatData[1]);
        }
    }

      /**
     * Speichert die Diskette in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich
     */
    void saveState(DataOutputStream dos) throws IOException {
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
    }

    /**
     * Liest die Diskette aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    void loadState(DataInputStream dis) throws IOException {
        diskData = new byte[dis.readInt()][0][0][0];
        for (byte[][][] cylinder : diskData) {
            cylinder = new byte[dis.readInt()][][];
            for (byte[][] head : cylinder) {
                head = new byte[dis.readInt()][];
                for (byte[] sector : head) {
                    sector = new byte[dis.readInt()];
                    dis.read(sector);
                }
            }
        }
        writeProtect = dis.readBoolean();
    }

    /**
     * Liest ein Imagedisk Image
     *
     * @param buffer Image Daten
     */
    private void readImagediskFile(byte[] buffer) {
        int pos = 0;

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
            boolean useSectorCylinderMap = getBit(head, 7);
            boolean useSectorHeadMap = getBit(head, 6);
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

                checkAndAddDiskGeometry(cylinder, head, sector);

                diskData[cylinder][head][sector - 1] = new byte[sectorSizeBytes];

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
                        System.arraycopy(buffer, pos, diskData[cylinder][head][sector - 1], 0, sectorSizeBytes);
                        pos += sectorSizeBytes;
                    }
                    break;
                    case 0x02:
                    case 0x04:
                    case 0x06:
                    case 0x08: {
                        byte repeatData = buffer[pos++];
                        Arrays.fill(diskData[cylinder][head][sector - 1], repeatData);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Liest ein Teledisk Image
     *
     * @param buffer Image Daten
     */
    private void readTelediskFile(byte[] buffer) {
        int pos = 0;

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

        int sectorCount;
        do {
            sectorCount = buffer[pos++];
            if (sectorCount != -1) {
                int cylinder = buffer[pos++];
                int head = buffer[pos++] & 0x1;
                int trackCRC = buffer[pos++];

                for (int i = 0; i < sectorCount; i++) {
                    int cylinderNumber = buffer[pos++];
                    int headNumber = buffer[pos++] & 0x1;
                    int sectorNumber = buffer[pos++];
                    int sectorSize = buffer[pos++];
                    int sectorSizeBytes = (int) Math.pow(2, sectorSize + 7);
                    int flags = buffer[pos++];
                    int sectorCRC = buffer[pos++];

                    checkAndAddDiskGeometry(cylinderNumber, headNumber, sectorNumber);
                    diskData[cylinder][head][sectorNumber - 1] = new byte[sectorSizeBytes];

                    if (!getBit(flags, 5) && !getBit(flags, 6)) {
                        int dataBlockSize = (buffer[pos++] & 0xFF) | ((int) buffer[pos++]) << 8;
                        int encoding = buffer[pos++];
                        switch (encoding) {
                            case 0x00: {
                                // RAW
                                System.arraycopy(buffer, pos, diskData[cylinder][head][sectorNumber - 1], 0, sectorSizeBytes);
                            }
                            break;
                            case 0x01: {
                                // Repeat 2 Bytes
                                int blockPos = 0;
                                for (int j = 0; j < dataBlockSize - 1; j = j + 4) {
                                    int size = (buffer[pos] & 0xFF) | ((int) buffer[pos + 1] & 0xFF) << 8;
                                    byte pat1 = buffer[pos + 2];
                                    byte pat2 = buffer[pos + 3];
                                    for (int k = 0; k < size; k++) {
                                        diskData[cylinder][head][sectorNumber - 1][blockPos++] = pat1;
                                        diskData[cylinder][head][sectorNumber - 1][blockPos++] = pat2;
                                    }
                                }
                            }
                            break;
                            case 0x02: {
                                // Run Length Encoding
                                int blockPos = 0;
                                for (int j = 0; j < dataBlockSize - 1; j++) {
                                    int length = (int) (buffer[pos + j] & 0xFF);
                                    if (length == 00) {
                                        int l2 = (int) (buffer[pos + j + 1] & 0xFF);
                                        System.arraycopy(buffer, pos + j + 2, diskData[cylinder][head][sectorNumber - 1], blockPos, l2);
                                        j += l2 + 1;
                                        blockPos += l2;
                                    } else {
                                        int r = (int) (buffer[pos + j + 1] & 0xFF);
                                        for (int k = 0; k < r; k++) {
                                            System.arraycopy(buffer, pos + j + 2, diskData[cylinder][head][sectorNumber - 1], blockPos, length * 2);
                                            blockPos += length * 2;
                                        }
                                        j += 1 + length * 2;

                                    }
                                }
                            }
                            break;
                        }
                        pos += dataBlockSize - 1;

                    }
                }
            }
        } while (sectorCount != -1);
    }

    /**
     * Liest ein DMK Image
     *
     * @param buffer Image Daten
     */
    private void readDMKFile(byte[] buffer) {
        int pos = 0;
        int writeProtectImage = (int) (buffer[pos++] & 0xFF);
        setWriteProtect(writeProtectImage == 0xFF);
        int trackCount = (int) (buffer[pos++] & 0xFF);
        int trackSize = (buffer[pos] & 0xFF) | ((int) buffer[pos + 1] & 0xFF) << 8;
        int flags = buffer[pos++];
        boolean singleSide = getBit(flags, 4);
        boolean singleDensity = getBit(flags, 6);

        int HEADER_SIZE = 0x10;
        int sides = (singleSide) ? 1 : 2;

        int diskSize = 0;
        for (int t = 0; t < trackCount; t++) {
            // Neuer Track
            for (int s = 0; s < sides; s++) {
                int[] idam = new int[64];
                // Lese IDAMS
                int sectorCount = 0;
                do {
                    idam[sectorCount] = (buffer[HEADER_SIZE + sides * t * trackSize + s * trackSize + sectorCount * 2] & 0xFF) | ((int) buffer[HEADER_SIZE + sides * t * trackSize + s * trackSize + sectorCount * 2 + 1] & 0xFF) << 8;
                    sectorCount++;
                } while (idam[sectorCount - 1] != 0);
                sectorCount--;

                for (int sec = 0; sec < sectorCount; sec++) {
                    boolean doubleDensity = getBit(idam[sec], 15);
                    idam[sec] ^= 0x8000;
                    int blockPos = HEADER_SIZE + sides * t * trackSize + s * trackSize + idam[sec] + 1;
                    int cylinder = buffer[blockPos++];
                    int head = buffer[blockPos++];
                    int sector = buffer[blockPos++];
                    int sectorSize = (int) Math.pow(2, 7 + buffer[blockPos++]);

                    this.checkAndAddDiskGeometry(cylinder, head, sector);
                    diskData[t][s][sec] = new byte[sectorSize];
                    // crc
                    blockPos++;
                    blockPos++;
                    byte data_am;
                    do {
                        data_am = buffer[blockPos++];
                    } while ((data_am != ((byte) 0xFB)) && (data_am != ((byte) 0xF8)));
                    System.arraycopy(buffer, blockPos, diskData[t][s][sec], 0, sectorSize);
                    diskSize += sectorSize;
                }
            }
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
        byte[] sectorData = diskData[cylinder][head][sector - 1];
        int byteToCopy = Math.min(cnt, sectorData.length);
        System.arraycopy(sectorData, 0, res, 0, byteToCopy);
        cnt -= byteToCopy;
        if (cnt > 0) {
            if (diskData[cylinder][head].length == (sector - 1)) {
                sector = 1;
                if (diskData[cylinder].length == head) {
                    head = 1;
                    if (diskData.length == cylinder) {
                        throw new IllegalStateException("End of Disk");
                    } else {
                        cylinder++;
                    }
                } else {
                    head++;
                }
            } else {
                sector++;
            }
            byte[] nextData = readData(cylinder, sector, head, cnt);
            System.arraycopy(nextData, 0, res, byteToCopy, nextData.length);
        }
        return res;
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
        byte[] sectorData = diskData[cylinder][head][sector - 1];
        int byteToCopy = Math.min(data.length, sectorData.length);
        System.arraycopy(data, 0, sectorData, 0, byteToCopy);
        if (byteToCopy < data.length) {
            byte[] resData = new byte[data.length - byteToCopy];
            System.arraycopy(data, byteToCopy, resData, 0, data.length - byteToCopy);
            if (diskData[cylinder][head].length == (sector - 1)) {
                sector = 1;
                if (diskData[cylinder].length == head) {
                    head = 1;
                    if (diskData.length == cylinder) {
                        throw new IllegalStateException("End of Disk");
                    } else {
                        cylinder++;
                    }
                } else {
                    head++;
                }
            } else {
                sector++;
            }
            writeData(cylinder, sector, head, resData);
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
     * Prüft ob die angegebene Position der Diskette verfügbar ist und ergänzt
     * ggf. die fehlende Geometrie
     *
     * @param cylinder Zylindernummer
     * @param head Seite/Kopfnummer
     * @param sector Sektor
     */
    private void checkAndAddDiskGeometry(int cylinder, int head, int sector) {
        if (cylinder >= diskData.length) {
            addCylinder(cylinder);
        }
        if (head >= diskData[cylinder].length) {
            addHead(cylinder, head);
        }
        if ((sector - 1) >= diskData[cylinder][head].length) {
            addSector(cylinder, head, sector);
        }
    }

}
