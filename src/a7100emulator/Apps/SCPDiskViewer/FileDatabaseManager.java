/*
 * FileDatabaseManager.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   30.12.2014 - Erste Version
 *   01.01.2015 - Funktionsfähige Version
 *   02.01.2014 - Kommentare ergänzt
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Datei zur Verwaltung der SCP-Datei-Datenbank.
 *
 * @author Dirk Bräuer
 */
public class FileDatabaseManager {

    /**
     * Datenbank aller bekannten SCP-Dateien
     */
    private final HashMap<String, FileInfo> fileDatabase = new HashMap();
    /**
     * Liste aller Software-Pakete
     */
    private final HashSet<String> packages = new HashSet();
    /**
     * Liste aller Dateitypen
     */
    private final HashSet<String> fileTypes = new HashSet();

    /**
     * Erezeugt einen Neuen Datenbankmanager.
     */
    public FileDatabaseManager() {
        readSystemDatabase();
        readUserDatabase();
    }

    /**
     * Gibt die Informationen zur einem MD5-Hashwert zurück.
     *
     * @param md5 MD5 Hash
     * @return Dateiinformationen oder <code>null</code> wenn die Datei nicht in
     * der Datenbank vorhanden ist.
     */
    public FileInfo getFileInfo(String md5) {
        return fileDatabase.get(md5);
    }

    /**
     * Aktualisiert die Dateiinformationen in der Datenbank.
     *
     * @param md5 MD5 Hash
     * @param fileInfo Einzutragende Dateiinformationen
     */
    public void updateFileInfo(String md5, FileInfo fileInfo) {
        fileDatabase.put(md5, fileInfo);
        fileTypes.add(fileInfo.getFileType());
        packages.add(fileInfo.getSoftwarePackage());
    }

    /**
     * Speichert die Datenbank auf der Festplatte in zwei Dateien: system.dbd
     * für normale Einträge und user.dbd für benutzerdefinierte Einträge.
     */
    public void saveDatabase() {
        try {
            DataOutputStream dosSystem = new DataOutputStream(new FileOutputStream("./system.dbd"));
            DataOutputStream dosUser = new DataOutputStream(new FileOutputStream("./user.dbd"));
            for (Entry<String, FileInfo> dbEntry : fileDatabase.entrySet()) {
                String md5 = dbEntry.getKey();
                FileInfo fileInfo = dbEntry.getValue();
                if (fileInfo.isUser()) {
                    writeDatabase(dosUser, md5, fileInfo);
                } else {
                    writeDatabase(dosSystem, md5, fileInfo);
                }
            }
            dosSystem.close();
            dosUser.close();
        } catch (Exception ex) {
            Logger.getLogger(FileDatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Liest die Systemdatenbank system.dbd von der Festplatte.
     */
    private void readSystemDatabase() {
        File systemDB = new File("./system.dbd");

        try {
            // Wenn Datenbank nicht vorhanden, neu anlegen
            if (!systemDB.exists()) {
                systemDB.createNewFile();
            }
            DataInputStream dis = new DataInputStream(new FileInputStream(systemDB));
            readDatabase(dis, false);
            dis.close();
        } catch (Exception ex) {
            Logger.getLogger(FileDatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Liest die Benutzerdatenbank user.dbd von der Festplatte.
     */
    private void readUserDatabase() {
        File userDB = new File("./user.dbd");

        try {
            // Wenn Datenbank nicht vorhanden, neu anlegen
            if (!userDB.exists()) {
                userDB.createNewFile();
            }
            DataInputStream dis = new DataInputStream(new FileInputStream(userDB));
            readDatabase(dis, true);
            dis.close();
        } catch (Exception ex) {
            Logger.getLogger(FileDatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Liest Daten aus einer Datenbank.
     *
     * @param dis Eingabestrom der Datei
     * @param user <code>true</code> wenn Benutzerdaten gelesen werden sollen,
     * <code>false</code> sonst
     * @throws IOException Wenn beim Lesen ein Fehler auftritt
     */
    private void readDatabase(DataInputStream dis, boolean user) throws IOException {
        while (dis.available() > 0) {
            String md5 = dis.readUTF();
            String name = dis.readUTF();
            String version = dis.readUTF();
            String fileType = dis.readUTF();
            String softwarePackage = dis.readUTF();
            String description = dis.readUTF();
            FileInfo fileInfo = new FileInfo(name, fileType, softwarePackage, version, description, user);
            updateFileInfo(md5, fileInfo);
        }
    }

    /**
     * Schreibt einen Datensatz in eine Datenbank.
     * 
     * @param dos Ausgabestrom zur Datei
     * @param md5 MD5 Hash
     * @param fileInfo Dateiinformationen
     * @throws IOException Wenn beim Schreiben ein Fehler auftritt
     */
    private void writeDatabase(DataOutputStream dos, String md5, FileInfo fileInfo) throws IOException {
        dos.writeUTF(md5);
        dos.writeUTF(fileInfo.getName());
        dos.writeUTF(fileInfo.getVersion());
        dos.writeUTF(fileInfo.getFileType());
        dos.writeUTF(fileInfo.getSoftwarePackage());
        dos.writeUTF(fileInfo.getDescription());
    }

    /**
     * Gibt ein sortiertes Array aller in der Datenbank vorhandenen Dateitypen zurück.
     * 
     * @return Array mit Dateitypen
     */
    String[] getFileTypes() {
        String[] result = new String[fileTypes.size()];
        ArrayList<String> fileTypesList = new ArrayList(fileTypes);
        Collections.sort(fileTypesList);
        fileTypesList.toArray(result);
        return result;
    }

    /**
     * Gibt ein sortiertes Array aller in der Datenbank vorhandenen Software-Pakete zurück.
     * @return Array mit Softwarepaketen
     */
    String[] getSoftwarePackages() {
        String[] result = new String[packages.size()];
        ArrayList<String> packagesList = new ArrayList(packages);
        Collections.sort(packagesList);
        packagesList.toArray(result);
        return result;
    }

    /**
     * Löscht einen Eintrag aus der Datenbank.
     * @param md5 MD5-Hash
     */
    void removeFileInfo(String md5) {
        fileDatabase.remove(md5);
    }
}
