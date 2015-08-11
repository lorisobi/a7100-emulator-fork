/*
 * SCPFileModel.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2015 Dirk Bräuer
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
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final ArrayList<SCPFile> files = new ArrayList<SCPFile>();
    /**
     * Refernz auf Ansicht
     */
    private SCPDiskViewer view;
    /**
     * Offset für das Lesen der Verzeichnisinformationen
     */
    private final int DIRECTORY_OFFSET = 0x3800;
    /**
     * Länge der Verzeichnisinformationen
     */
    private final int DIRECTORY_LENGTH = 0x1000;
    /**
     * Name des geöffneten Images
     */
    private String imageName = "kein Image geöffnet";
    /**
     * Anzahl der belegten Blöcke
     */
    private int usedBlocks;
    /**
     * Inhalt des Bootloaders
     */
    private final byte[] bootloader = new byte[0x1A00];
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
    FileDatabaseManager databaseManager = new FileDatabaseManager();

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
     */
    void readImage(File image) {
        try {
            imageName = image.getAbsolutePath();
            diskData = new byte[(int) image.length()];
            InputStream in = new FileInputStream(image);
            in.read(diskData);
            in.close();

            parseImage();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Liest ein Verzeichnis als neue Diskette ein.
     * 
     * @param folder Verzeichnis
     */
    void readFolder(File folder) {
        diskData = new byte[0x9F800];
        files.clear();
        usedBlocks = 0;
        usedBlock = new boolean[310];
        usedFCB = new boolean[128];

        Arrays.fill(diskData, 0, diskData.length - 1, (byte) 0xE5);

        imageName = "Verzeichnis:" + folder.getAbsolutePath();

        files.clear();
        File[] inputFiles = folder.listFiles();

        try {
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
                    //SCPFile scpFile = new SCPFile(filename, extension, false, false, false, 0);

                    byte[] data = new byte[(int) file.length()];
                    InputStream in = new FileInputStream(file);
                    in.read(data);
                    in.close();

                    //scpFile.setData(data);
                    insertFile(filename, extension, false, false, false, 0, data);
                    //files.add(scpFile);
                } else {
                    System.out.println("Verzeichnis " + file.getName() + " wird übersprungen!");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        //diskLoaded=true;
        // parseImage();
        view.updateView();
    }

    /**
     * Liest ein Diskettenabbild ein.
     */
    private void parseImage() {
        files.clear();
        usedBlocks = 0;
        usedBlock = new boolean[310];
        usedFCB = new boolean[128];

        // Kopiere Bootloader
        System.arraycopy(diskData, 0, bootloader, 0, bootloader.length);

        byte[] fcb = new byte[32];

        // Gehe durch Verzeichnisstruktur
        for (int index = DIRECTORY_OFFSET; index < DIRECTORY_OFFSET + DIRECTORY_LENGTH; index += 32) {
            System.arraycopy(diskData, index, fcb, 0, 32);

            // Prüfe ob Laufwerksindex gültig
            if (((int) fcb[0] & 0xFF) <= 16) {
                // Markiere FCB als benutzt
                usedFCB[(index - DIRECTORY_OFFSET) / 32] = true;

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

                SCPFile scpFile = new SCPFile(name, extension, readOnly, system, extra, fcb[0]);

                if (fcb[12] == 0) {
                    // Erster Teil einer Datei

                    // Lade Anzahl der Blöcke
                    int blocks = (int) fcb[15] & 0xFF;
                    byte[] fileData = new byte[blocks * 0x80];
                    int remainBlocks = blocks;
                    for (int i = 16; i < 32 && remainBlocks > 0; i = i + 2) {
                        int blockNumber = ((int) (fcb[i + 1] & 0xFF) << 8) | ((int) fcb[i] & 0xFF);
                        int address = blockNumber * 0x800 + 0x3800;
                        if (address != 0x3800) {
                            System.arraycopy(diskData, address, fileData, ((i - 16) / 2) * 0x800, remainBlocks > 15 ? 0x800 : remainBlocks * 0x80);
                            remainBlocks -= 16;
                            usedBlocks++;
                            usedBlock[blockNumber - 2] = true;
                        }
                    }
                    scpFile.setData(fileData);

                    files.add(scpFile);
                } else {
                    // Nicht erster Teil einer Datei
                    int blocks = (int) fcb[15] & 0xFF;
                    int fcbIndex = fcb[12];
                    for (SCPFile file : files) {
                        // Suche Datei in bisheriger Liste
                        if (file.getName().equals(name) && file.getExtension().equals(extension) && file.getUser() == fcb[0]) {

                            // Kopiere alte Daten
                            byte[] oldData = new byte[file.getData().length];
                            System.arraycopy(file.getData(), 0, oldData, 0, file.getData().length);
                            byte[] fileData = new byte[oldData.length + blocks * 0x80];
                            System.arraycopy(oldData, 0, fileData, 0, oldData.length);
                            int remainBlocks = blocks;

                            for (int i = 16; i < 32 && remainBlocks > 0; i = i + 2) {
                                int blockNumber = ((int) (fcb[i + 1] & 0xFF) << 8) | ((int) fcb[i] & 0xFF);
                                int address = blockNumber * 0x800 + 0x3800;
                                if (address != 0x3800) {
                                    System.arraycopy(diskData, address, fileData, fcbIndex * 0x800 * 8 + ((i - 16) / 2) * 0x800, remainBlocks > 15 ? 0x800 : remainBlocks * 0x80);
                                    remainBlocks -= 16;
                                    usedBlocks++;
                                    usedBlock[blockNumber - 2] = true;
                                }
                            }
                            file.setData(fileData);
                        }
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
     */
    void insertFile(String filename, String extension, boolean readOnly, boolean system, boolean extra, int user, byte[] fileData) {
        int requiredFCBs = fileData.length == 0 ? 1 : ((fileData.length / (8 * 0x800)) + (fileData.length % (8 * 0x800) == 0 ? 0 : 1));
        int requiredBlocks = (fileData.length / 0x800) + (fileData.length % 0x800 == 0 ? 0 : 1);
        int requiredSectors = (fileData.length / 0x80) + (fileData.length % 0x80 == 0 ? 0 : 1);

        // Kopiere Daten um
        byte[] data = new byte[requiredSectors * 0x80];
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
                int remainingBlocks = requiredBlocks;
                for (int f = 0; f < FCBs.length; f++) {
                    // Setze Dateibereich
                    fcb[12] = (byte) f;
                    // Setze Anzahl der Blöcke
                    fcb[15] = (byte) (remainingSectors > 0x80 ? 0x80 : remainingSectors);
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
                            int address = block * 0x800 + 0x3800;
                            // Hole 2K Datenarray
                            byte[] blkData = new byte[0x800];
                            int sectorsToCopy = (remainingSectors >= 16) ? 16 : remainingSectors;
                            int bytesToCopy = sectorsToCopy * 0x80;
                            System.arraycopy(data, (f * 8 + blk) * 0x800, blkData, 0, bytesToCopy);
                            // Schreibe Block in Image
                            System.arraycopy(blkData, 0, diskData, address, 0x800);

                            remainingSectors -= sectorsToCopy;
                        } else {
                            // Nichts zum Schreiben
                            fcb[16 + blk * 2] = 0x00;
                            fcb[16 + blk * 2 + 1] = 0x00;
                        }
                        // Schreibe FCB
                        System.arraycopy(fcb, 0, diskData, FCBs[f] * 32 + 0x3800, 32);
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
        return usedBlocks * 2 + "K belegt," + (620 - usedBlocks * 2) + "K frei";
    }

    /**
     * Gibt die Anzahl der freien 2K Blöcke zurück
     *
     * @return Anzahl freier Blöcke
     */
    int getFreeBlocks() {
        return 620 - usedBlocks * 2;
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
        } catch (Exception ex) {
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
     * @param md5 MD5 Hash
     * @param info Aktualisierte Datenbankinformationen
     */
    void updateDBInfo(String md5, FileInfo info) {
        databaseManager.updateFileInfo(md5, info);
        databaseManager.saveDatabase();
    }

    /**
     * Entfernt Informationen aus der Datenbank.
     *
     * @param md5 MD5 Hash
     */
    void removeDBInfo(String md5) {
        databaseManager.removeFileInfo(md5);
        databaseManager.saveDatabase();
    }

    /**
     * Startfunktion bein Einzelanwendung.
     *
     * @param args Kommandozeilenparameter
     */
    public static void main(String args[]) {
        SCPDiskModel model = new SCPDiskModel();
        SCPDiskViewer view = new SCPDiskViewer(model);
        model.setView(view);
    }

    /**
     * Speichert den Inhalt der Datenbank in ein CSV File.
     *
     * @param saveFile Export Datei
     * @param user <code>true</code> wenn Benutzereinträge exportiert werden
     * sollen, <code>false</code> für Systemeinträge
     */
    void exportDB(File saveFile, boolean user) {
        databaseManager.exportDB(saveFile, user);
    }
}
