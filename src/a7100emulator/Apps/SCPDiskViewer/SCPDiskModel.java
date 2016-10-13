/*
 * SCPFileModel.java
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
 *   16.12.2014 - Hinzufügen von Datein implementiert
 *   01.01.2015 - Datenbank implementiert
 *   24.07.2015 - Datenbank exportieren ergänzt
 *   26.07.2015 - Kommentare ergänzt
 *   09.08.2015 - Javadoc korrigiert
 *   16.08.2015 - parseImage vereinfacht
 *              - Lesen über FloppyImageParser realisiert
 *              - Andere Formate als SCP-Hausformat möglich
 *   25.07.2016 - Doppelte Typdefinition in files entfernt
 *   26.07.2016 - Spezifische Exceptions definiert
 *   28.07.2016 - Konstruktor hinzugefügt
 *              - throws für Methoden mit Datenträgerzugriff hinzugefügt
 */
package a7100emulator.Apps.SCPDiskViewer;

import a7100emulator.Tools.FloppyImageParser;
import a7100emulator.components.system.FloppyDisk;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Modell des SCP-Disketten Tools
 *
 * @author Dirk Bräuer
 */
public class SCPDiskModel {

    /**
     * Disketteninhalt
     */
    private byte[] diskData;
    /**
     * Liste der Dateien
     */
    private final ArrayList<SCPFile> files = new ArrayList<>();
    /**
     * Refernz auf Ansicht
     */
    private SCPDiskViewer view;
    /**
     * Name des geöffneten Images
     */
    private String imageName = "kein Image geöffnet";
    /**
     * Offset für das Lesen der Verzeichnisinformationen
     */
    private int directoryOffset;
    /**
     * Blockgröße
     */
    private final int BLOCK_SIZE = 0x800;
    /**
     * Größe einer Speichereinheit
     */
    private final int UNIT_SIZE = 0x80;
    /**
     * Länge der Verzeichnisinformationen
     */
    private final int DIRECTORY_LENGTH = 2 * BLOCK_SIZE;
    /**
     * Anzahl der belegten Blöcke
     */
    private int usedBlocks;
    /**
     * Inhalt des Bootloaders
     */
    private byte[] bootloader;
    /**
     * Liste freier Blöcke Blocknummer ist um 2 versetzt gespeichert
     */
    private boolean[] usedBlock = new boolean[309];
    /**
     * Liste Freier FCBs
     */
    private boolean[] usedFCB = new boolean[128];
    /**
     * Verweis auf Datenbank
     */
    private final FileDatabaseManager databaseManager;

    /**
     * Erstellt ein neues SCP-Disk Modell. Dabei wird auch ein neuer
     * <code>FileDatabaseManager</code> angelegt und die Datenbank wird vom
     * Datenträger geladen oder erzeugt.
     *
     * @throws IOException Wenn beim Laden oder Erzeugen der Datenbank ein
     * Fehler auftritt
     */
    public SCPDiskModel() throws IOException {
        databaseManager = new FileDatabaseManager();
    }

    /**
     * Gibt die Liste der SCP-Dateien zurück
     *
     * @return Dateien
     */
    ArrayList<SCPFile> getFiles() {
        return files;
    }

    /**
     * Setzt die Ansicht
     *
     * @param view Ansicht
     */
    public void setView(SCPDiskViewer view) {
        this.view = view;
        view.updateView();
    }

    /**
     * Liest ein Diskettenabbild in den Speicher.
     *
     * @param image Datei
     * @throws java.security.NoSuchAlgorithmException Wenn beim Erzeugen der
     * MD5-Hashwerte ein Fehler auftritt
     * @throws java.io.IOException Wenn beim Lesen der Datei ein Fehler auftritt
     */
    void readImage(File image) throws NoSuchAlgorithmException, IOException {
        FloppyDisk disk = FloppyImageParser.loadDiskFromImageFile(image);
        if (disk != null) {
            imageName = image.getAbsolutePath();
            // TODO: einseitige Disketten, andere Formatierungen
            // Verzeichnis befindet 
            directoryOffset = disk.getSectorSize(0, 0, 1) * disk.getSectors(0, 0) + 3 * disk.getSectorSize(0, 1, 1) * disk.getSectors(0, 1);
            diskData = disk.getFlatData();
            parseImage();
        }
    }

    /**
     * Liest ein Verzeichnis als neue Diskette ein.
     *
     * @param folder Verzeichnis
     * @throws java.security.NoSuchAlgorithmException Wenn beim Erzeugen der
     * MD5-Hashwerte ein Fehler auftritt
     * @throws java.io.IOException Wenn beim Lesen der Datei ein Fehler auftritt
     */
    void readFolder(File folder) throws NoSuchAlgorithmException, IOException {
        // Daten für SCP-Diskette
        int diskSize = 0x9F800;
        diskData = new byte[diskSize];
        directoryOffset = 0x3800;

        // Anzahl der Blöcke und FCBs
        int blocks = (diskSize - directoryOffset) / BLOCK_SIZE;
        int fcbs = DIRECTORY_LENGTH / 32;

        // Lösche alte Dateien
        files.clear();

        // Bereits 2 Blöcke für Verzeichnis verwendet
        usedBlocks = DIRECTORY_LENGTH / BLOCK_SIZE;
        usedBlock = new boolean[blocks];
        usedFCB = new boolean[fcbs];
        // Markiere Verzeichnis als benutzt
        for (int b = 0; b < DIRECTORY_LENGTH / BLOCK_SIZE; b++) {
            usedBlock[b] = true;
        }

        Arrays.fill(diskData, 0, diskData.length - 1, (byte) 0xE5);

        // Kopiere Bootloader
        bootloader = new byte[directoryOffset];
        System.arraycopy(diskData, 0, bootloader, 0, bootloader.length);

        imageName = "Verzeichnis:" + folder.getAbsolutePath();

        files.clear();
        File[] inputFiles = folder.listFiles();

        for (File file : inputFiles) {
            if (file.isFile()) {
                int startExtension = file.getName().lastIndexOf('.');
                String extension = (startExtension == -1 ? "" : file.getName().substring(startExtension + 1)).toUpperCase();
                if (extension.length() > 3) {
                    extension = extension.substring(0, 3);
                }
                String filename = (startExtension == -1 ? file.getName() : file.getName().substring(0, startExtension)).toUpperCase();
                if (filename.length() > 8) {
                    filename = filename.substring(0, 8);
                }

                byte[] data = new byte[(int) file.length()];
                InputStream in = new FileInputStream(file);
                in.read(data);
                in.close();

                insertFile(filename, extension, false, false, false, 0, data);
            } else {
                System.out.println("Verzeichnis " + file.getName() + " wird übersprungen!");
            }
        }

        view.updateView();
    }

    /**
     * Liest ein Diskettenabbild ein und extrahiert die Informationen über die
     * einzelnen Datein. Die in <code>diskData</code> hinterlegten Rohdaten
     * werden analysiert und die Informationen über die einzelnen Dateien
     * extrahiert.
     *
     * @throws java.security.NoSuchAlgorithmException wenn beim Erzeugen der
     * MD5-Hashwerte ein Fehler auftritt
     */
    private void parseImage() throws NoSuchAlgorithmException {
        // Daten für SCP-Diskette
        int diskSize = diskData.length;

        // Anzahl der Blöcke und FCBs
        int blocks = (diskSize - directoryOffset) / BLOCK_SIZE;
        int fcbs = DIRECTORY_LENGTH / 32;

        // Lösche alte Dateien
        files.clear();

        // Bereits 2 Blöcke für Verzeichnis verwendet
        usedBlocks = DIRECTORY_LENGTH / BLOCK_SIZE;
        usedBlock = new boolean[blocks];
        usedFCB = new boolean[fcbs];
        // Markiere Verzeichnis als benutzt
        for (int b = 0; b < DIRECTORY_LENGTH / BLOCK_SIZE; b++) {
            usedBlock[b] = true;
        }

        // Kopiere Bootloader
        bootloader = new byte[directoryOffset];
        System.arraycopy(diskData, 0, bootloader, 0, bootloader.length);

        // Struktur für FCB
        byte[] fcb = new byte[32];

        // Gehe durch Verzeichnisstruktur
        for (int index = directoryOffset; index < directoryOffset + DIRECTORY_LENGTH; index += 32) {
            System.arraycopy(diskData, index, fcb, 0, 32);

            // Prüfe ob Laufwerksindex gültig
            if (((int) fcb[0] & 0xFF) <= 16) {
                // Markiere FCB als benutzt
                usedFCB[(index - directoryOffset) / 32] = true;

                // Lade Name aus FCB
                String name = "";
                for (int i = 1; i < 9; i++) {
                    name += (char) fcb[i];
                }

                // Lade Erweiterung aus FCB
                String extension = "";
                for (int i = 9; i < 12; i++) {
                    extension += (char) (fcb[i] & 0x7F);
                }

                // Lade Attribute aus FCB
                boolean readOnly = (fcb[9] & 0x80) == 0x80;
                boolean system = (fcb[10] & 0x80) == 0x80;
                boolean extra = (fcb[11] & 0x80) == 0x80;

                // Lade Anzahl der durch den FCB belegten Einheiten
                int units = (int) fcb[15] & 0xFF;
                // Lade ex - Aktuelle Bereichsnummer
                int fcbIndex = fcb[12];

                // SCP-Datei
                SCPFile scpFile;
                // Daten
                byte[] fileData;

                // Prüfe ex - Aktuelle Bereichsnummer
                if (fcbIndex == 0) {
                    // Erster Teil einer Datei
                    scpFile = new SCPFile(name, extension, readOnly, system, extra, fcb[0]);
                    // Erzeuge Array mit leeren Daten
                    scpFile.setData(new byte[0]);
                    // Füge Datei der Liste hinzu
                    files.add(scpFile);
                }

                // Suche Dateieintrag
                for (SCPFile file : files) {
                    // Suche Datei in bisheriger Liste
                    if (file.getName().equals(name) && file.getExtension().equals(extension) && file.getUser() == fcb[0]) {
                        scpFile = file;

                        // Kopiere alte Daten
                        byte[] oldData = new byte[scpFile.getData().length];
                        System.arraycopy(scpFile.getData(), 0, oldData, 0, scpFile.getData().length);

                        // Erstelle neues Array für gesamte Daten
                        fileData = new byte[oldData.length + units * UNIT_SIZE];
                        System.arraycopy(oldData, 0, fileData, 0, oldData.length);

                        int remainUnits = units;

                        for (int i = 16; i < 32 && remainUnits > 0; i = i + 2) {
                            // Lese 16-Bit Blocknummer aus FCB
                            int blockNumber = ((int) (fcb[i + 1] & 0xFF) << 8) | ((int) fcb[i] & 0xFF);

                            if (blockNumber != 0) {
                                // Berechne Adresse für Daten: Offset Systemspuren + BlockNummer * BlockGröße
                                int address = blockNumber * BLOCK_SIZE + 0x3800;

                                // Kopiere eine kompletten Block, wenn noch mindestens 16 Einheiten fehlen, sonst nur fehlende Einheiten
                                System.arraycopy(diskData, address, fileData, (fcbIndex * 8 + ((i - 16) / 2)) * BLOCK_SIZE, remainUnits > 15 ? BLOCK_SIZE : remainUnits * UNIT_SIZE);
                                remainUnits -= 16;

                                // Inkrementiere Anzahl der belegten Blöcke
                                usedBlocks++;
                                // Markiere Block als benutzt
                                usedBlock[blockNumber - 2] = true;
                            }
                        }
                        file.setData(fileData);

                        // Beende Suche
                        break;
                    }
                }
            }
        }
        view.updateView();
    }

    /**
     * Fügt dem Diskettenabbild eine Datei hinzu.
     *
     * @param filename Dateiname
     * @param extension Erweiterung
     * @param readOnly Attribut Schreibgeschützt
     * @param system Attribut System
     * @param extra Attribut Extra
     * @param user Nutzernummer
     * @param fileData Daten
     * @throws java.security.NoSuchAlgorithmException Wenn beim Erzeugen der
     * MD5-Hashwerte ein Fehler auftritt
     */
    void insertFile(String filename, String extension, boolean readOnly, boolean system, boolean extra, int user, byte[] fileData) throws NoSuchAlgorithmException {
        int requiredFCBs = fileData.length == 0 ? 1 : ((fileData.length / (8 * BLOCK_SIZE)) + (fileData.length % (8 * BLOCK_SIZE) == 0 ? 0 : 1));
        int requiredBlocks = (fileData.length / BLOCK_SIZE) + (fileData.length % BLOCK_SIZE == 0 ? 0 : 1);
        int requiredSectors = (fileData.length / UNIT_SIZE) + (fileData.length % UNIT_SIZE == 0 ? 0 : 1);

        // Kopiere Daten um
        byte[] data = new byte[requiredSectors * UNIT_SIZE];
        System.arraycopy(fileData, 0, data, 0, fileData.length);

        int[] FCBs = new int[requiredFCBs];
        int[] blocks = new int[requiredBlocks];

        boolean noFCB = false;

        for (int fcb = 0; fcb < requiredFCBs; fcb++) {
            int nextFCB = getNextFreeFCB();
            FCBs[fcb] = nextFCB;
            if (nextFCB == -1) {
                noFCB = true;
                break;
            }
        }

        if (noFCB) {
            JOptionPane.showMessageDialog(null, "Nicht genügend freie FCBs!", "Fehler Datei hinzufügen", JOptionPane.ERROR_MESSAGE);
        } else {
            boolean noBlock = false;
            for (int blk = 0; blk < requiredBlocks; blk++) {
                int nextBlock = getNextFreeBlock();
                blocks[blk] = nextBlock;
                if (nextBlock == -1) {
                    noBlock = true;
                    break;
                }
            }

            if (noBlock) {
                JOptionPane.showMessageDialog(null, "Nicht genügend freie Blöcke!", "Fehler Datei hinzufügen", JOptionPane.ERROR_MESSAGE);
            } else {
                // Füge Datei hinzu
                byte[] fcb = new byte[32];

                // Setze User
                fcb[0] = (byte) user;
                // Setze Dateiname
                for (int i = 0; i < 8; i++) {
                    fcb[i + 1] = (i < filename.length()) ? (byte) filename.charAt(i) : 0x20;
                }
                // Setze Erweiterung
                for (int i = 0; i < 3; i++) {
                    fcb[i + 9] = (i < extension.length()) ? (byte) extension.charAt(i) : 0x20;
                }
                // Setze Attribute
                fcb[9] = readOnly ? (byte) (fcb[9] | 0x80) : fcb[9];
                fcb[10] = system ? (byte) (fcb[10] | 0x80) : fcb[10];
                fcb[11] = extra ? (byte) (fcb[11] | 0x80) : fcb[11];
                // Setze Zusätzliche Bytes
                fcb[13] = 0x00;
                fcb[14] = 0x00;

                int remainingSectors = requiredSectors;
                for (int f = 0; f < FCBs.length; f++) {
                    // Setze Dateibereich
                    fcb[12] = (byte) f;
                    // Setze Anzahl der Blöcke
                    fcb[15] = (byte) (remainingSectors > UNIT_SIZE ? UNIT_SIZE : remainingSectors);
                    // Lösche Blöcke
                    for (int i = 0; i < 16; i++) {
                        fcb[i + 16] = 0;
                    }
                    for (int blk = 0; blk < 8; blk++) {
                        if (remainingSectors > 0) {
                            int block = blocks[f * 8 + blk];
                            // Speicher Adressen
                            fcb[16 + blk * 2] = (byte) (block & 0xFF);
                            fcb[16 + blk * 2 + 1] = (byte) ((block >> 8) & 0xFF);
                            // Berechne Adresse im Image
                            int address = block * BLOCK_SIZE + directoryOffset;
                            // Hole 2K Datenarray
                            byte[] blkData = new byte[BLOCK_SIZE];
                            int sectorsToCopy = (remainingSectors >= 16) ? 16 : remainingSectors;
                            int bytesToCopy = sectorsToCopy * UNIT_SIZE;
                            System.arraycopy(data, (f * 8 + blk) * BLOCK_SIZE, blkData, 0, bytesToCopy);
                            // Schreibe Block in Image
                            System.arraycopy(blkData, 0, diskData, address, BLOCK_SIZE);

                            remainingSectors -= sectorsToCopy;
                        } else {
                            // Nichts zum Schreiben
                            fcb[16 + blk * 2] = 0x00;
                            fcb[16 + blk * 2 + 1] = 0x00;
                        }
                        // Schreibe FCB
                        System.arraycopy(fcb, 0, diskData, FCBs[f] * 32 + directoryOffset, 32);
                    }
                }
                parseImage();
            }
        }
    }

    /**
     * Gibt den Namen des Abbilds zurück
     *
     * @return Name
     */
    String getImageName() {
        return imageName;
    }

    /**
     * Speichert eine SCP-Datei
     *
     * @param index Nummer der Datei
     * @param file Ausgabedatei
     */
    void saveFile(int index, File file) {
        try {
            SCPFile scpFile = files.get(index);
            OutputStream out;
            out = new FileOutputStream(file);
            out.write(scpFile.getData());
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Speichert alle Dateien des Abbildes
     *
     * @param directory Verzeichniss für Ausgabe
     */
    void saveAllFiles(File directory) {
        for (int i = 0; i < files.size(); i++) {
            SCPFile scpFile = files.get(i);
            String filename = directory.getAbsolutePath() + File.separator + scpFile.getFullName() + (scpFile.getUser() == 0 ? "" : ("_U" + scpFile.getUser()));
            File extractFile = new File(filename);
            if (!extractFile.exists()) {
                saveFile(i, extractFile);
            } else {
                System.out.println("Datei " + filename + " existiert bereits!");
            }
        }
        saveBootloader(new File(directory.getAbsolutePath() + File.separator + "bootloader"));
    }

    /**
     * Gibt die Disketteninformationen zurück
     *
     * @return Disketteninformationen
     */
    String getDiskInfo() {
        if (diskData == null) {
            return "";
        }
        return usedBlocks * 2 + "K belegt," + getFreeBlocks() + "K frei";
    }

    /**
     * Gibt die Anzahl der freien 2K Blöcke zurück
     *
     * @return Anzahl freier Blöcke
     */
    int getFreeBlocks() {
        return (diskData.length - directoryOffset) / 1024 - usedBlocks * 2;
    }

    /**
     * Speichert den Bootloader.
     *
     * @param file Ausgabedatei
     */
    void saveBootloader(File file) {
        try {
            OutputStream out;
            out = new FileOutputStream(file);
            out.write(bootloader);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gibt den nächsten freien FCB zurück
     *
     * @return FCB oder -1 wenn kein freier FCB vorhanden ist
     */
    private int getNextFreeFCB() {
        for (int i = 0; i < usedFCB.length; i++) {
            if (!usedFCB[i]) {
                usedFCB[i] = true;
                return i;
            }
        }
        return -1;
    }

    /**
     * Gibt den nächsten freien Block zurück.
     *
     * @return Block oder -1 wenn kein freier Block vorhanden ist
     */
    private int getNextFreeBlock() {
        for (int i = 0; i < usedBlock.length; i++) {
            if (!usedBlock[i]) {
                usedBlock[i] = true;
                return i + 2;
            }
        }
        return -1;
    }

    /**
     * Liefert zurück, ob eine Diskette geladen ist
     *
     * @return <code>true</code> - Wenn Diskette geladen ist, <code>false</code>
     * sonst
     */
    boolean diskLoaded() {
        return diskData != null;
    }

    /**
     * Speichert das Diskettenabbild in eine Datei.
     *
     * @param file Datei
     */
    void saveImage(File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(diskData);
            out.close();
            imageName = file.getName();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Liefert die in der Datenabnk vorhandenen Dateitypen zurück.
     *
     * @return Array mit Dateitypen
     */
    String[] getFileTypes() {
        return databaseManager.getFileTypes();
    }

    /**
     * Liefert die in der Datenbank gespeicherten Software-Pakete zurück.
     *
     * @return Array mit Software-Paketen
     */
    String[] getSoftwarePackages() {
        return databaseManager.getSoftwarePackages();
    }

    /**
     * Liefrt den Datenbankeintrag zu einem SCP-File zurück.
     *
     * @param file SCP-Datei
     * @return Datenbankeintrag oder <code>null</code> wenn kein entsprechender
     * Eintrag vorhanden ist.
     */
    FileInfo getFileInfo(SCPFile file) {
        return databaseManager.getFileInfo(file.getMD5());
    }

    /**
     * Aktualisiert Informationen in der Datenbank.
     *
     * @param md5 MD5-Hashwert
     * @param info Aktualisierte Datenbankinformationen
     * @throws java.io.IOException Wenn beim Speichern der Informationen in der
     * Datenbank ein Fehler auftritt
     */
    void updateDBInfo(String md5, FileInfo info) throws IOException {
        databaseManager.updateFileInfo(md5, info);
        databaseManager.saveDatabase();
    }

    /**
     * Entfernt Informationen aus der Datenbank.
     *
     * @param md5 MD5-Hashwert
     * @throws java.io.IOException Wenn beim Speichern der Informationen in der
     * Datenbank ein Fehler auftritt
     */
    void removeDBInfo(String md5) throws IOException {
        databaseManager.removeFileInfo(md5);
        databaseManager.saveDatabase();
    }

    /**
     * Speichert den Inhalt der Datenbank in ein CSV File.
     *
     * @param saveFile Export Datei
     * @param user     <code>true</code> wenn Benutzereinträge exportiert werden
     * sollen, <code>false</code> für Systemeinträge
     * @throws java.io.FileNotFoundException Wenn die angegebene Datei ungültig
     * ist oder nicht erzeugt werden kann
     */
    void exportDB(File saveFile, boolean user) throws FileNotFoundException {
        databaseManager.exportDB(saveFile, user);
    }

    /**
     * Startfunktion bein Einzelanwendung.
     *
     * @param args Kommandozeilenparameter
     */
    public static void main(String args[]) {
        try {
            SCPDiskModel model = new SCPDiskModel();
            SCPDiskViewer view = new SCPDiskViewer(model);
            model.setView(view);
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
