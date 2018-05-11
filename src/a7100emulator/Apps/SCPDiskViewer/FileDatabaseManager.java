/*
 * FileDatabaseManager.java
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
 *   30.12.2014 - Erste Version
 *   01.01.2015 - Funktionsfähige Version
 *   02.01.2014 - Kommentare ergänzt
 *   24.07.2015 - Datenbank exportieren ergänzt
 *   26.07.2016 - Spezifische Exceptions definiert
 *   28.07.2016 - Befehle zum Lesen der Datenbanken zusammengeführt
 *              - try-catch durch throw ersetzt
 *              - Kommentare überarbeitet
 *   10.05.2018 - Logger hinzugefügt
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung der SCP-Datei-Datenbank. Die Datenbank enthält
 * Informationen über SCP-Dateien, Dateitypen und Software-Pakete.
 *
 * @author Dirk Bräuer
 */
public class FileDatabaseManager {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(FileDatabaseManager.class.getName());

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
     * Erzeugt einen neuen Datenbankmanager. Dabei werden die Dateien system.dbd
     * und user.dbd aus dem aktuellen Verzeichnis des Datenträgers gelesen und
     * die jeweiligen Einträge der Datenbank hinzugefügt. Sind die Dateien nicht
     * vorhanden, so werden sie im jeweiligen Verzeichnis erzeugt.
     *
     * @throws IOException Wenn beim Lesen oder Erzeugen der Dateien ein Fehler
     * auftritt
     */
    public FileDatabaseManager() throws IOException {
        readDatabase(false);
        readDatabase(true);
    }

    /**
     * Gibt die Datei-Informationen zur einem MD5-Hashwert zurück. Diese Methode
     * liefert entweder ein <code>FileInfo</code> Objekt mit den in der
     * Datenbank hinterlegten Informationen oder <code>null</code> wenn für den
     * gesuchten MD5-Hashwert keine Informationen in der Datebank vorhanden
     * sind.
     *
     * @param md5 MD5-Hashwert
     * @return Dateiinformationen oder <code>null</code> wenn für den
     * MD5-Hashwert keine Information in der Datenbank vorhanden ist.
     */
    public FileInfo getFileInfo(String md5) {
        return fileDatabase.get(md5);
    }

    /**
     * Aktualisiert die Dateiinformationen in der Datenbank. Die übergebenen
     * Dateiinformationen werden in der Datenbank abgelegt. Weiterhin wird ein
     * eventuell neu definiertes Software-Paket oder ein neuer Dateityp in den
     * Listen ergänzt.
     *
     * @param md5 MD5-Hashwert
     * @param fileInfo Einzutragende Dateiinformationen
     */
    public void updateFileInfo(String md5, FileInfo fileInfo) {
        if (fileInfo == null) {
            throw new IllegalArgumentException("FileInfo ist null");
        }
        fileDatabase.put(md5, fileInfo);
        fileTypes.add(fileInfo.getFileType());
        packages.add(fileInfo.getSoftwarePackage());
    }

    /**
     * Speichert die Datenbank auf der Festplatte. Die Dateiinformationen werden
     * in zwei Dateien abgelegt: system.dbd für normale Einträge und user.dbd
     * für benutzerdefinierte Einträge.
     *
     * @throws IOException Wenn beim Speichern der Datenbanken auf dem
     * Datenträger ein Fehler auftritt
     */
    public void saveDatabase() throws IOException {
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
    }

    /**
     * Liest die Systemdatenbank system.dbd oder die Nutzerdatenbank user.dbd
     * vom aktuellen Datenträger ein. Ist die jeweilige Datei im aktuellen
     * Verzeichnis nicht vorhanden, so wird sie durch diese Methode erstellt.
     *
     * @param user <code>true</code> wenn die Nutzerdatenbank gelesen werden
     * soll, <code>false</code> für die Systemdatenbank
     * @throws java.io.IOException Wenn beim Lesen der Datenbank ein Fehler
     * auftritt
     */
    private void readDatabase(boolean user) throws IOException {
        File dbFile = new File("./" + (user ? "user" : "system") + ".dbd");

        // Wenn Datenbank nicht vorhanden, neu anlegen
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }
        DataInputStream dis = new DataInputStream(new FileInputStream(dbFile));
        readDatabase(dis, user);
        dis.close();
    }

    /**
     * Liest Daten aus einer Datenbank. Solange im Eingabestrom noch Daten sind
     * wird ein weiteres FileInfo Objekt aus dem Datenstrom gelesen und zur
     * Datenbank hinzugefügt.
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
     * Schreibt einen Datensatz in eine Datenbank. Die in <code>FileInfo</code>
     * übergebenen Daten werden in den Ausgabestrom geschrieben.
     *
     * @param dos Ausgabestrom zur Datei
     * @param md5 MD5-Hashwert
     * @param fileInfo Dateiinformationen
     * @throws IOException Wenn beim Schreiben ein Fehler auftritt
     */
    private void writeDatabase(DataOutputStream dos, String md5, FileInfo fileInfo) throws IOException {
        if (fileInfo == null) {
            throw new IllegalArgumentException("FileInfo ist null");
        }
        dos.writeUTF(md5);
        dos.writeUTF(fileInfo.getName());
        dos.writeUTF(fileInfo.getVersion());
        dos.writeUTF(fileInfo.getFileType());
        dos.writeUTF(fileInfo.getSoftwarePackage());
        dos.writeUTF(fileInfo.getDescription());
    }

    /**
     * Gibt ein sortiertes Array aller in der Datenbank vorhandenen Dateitypen
     * zurück.
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
     * Gibt ein sortiertes Array aller in der Datenbank vorhandenen
     * Software-Pakete zurück.
     *
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
     * Löscht einen Eintrag aus der Datenbank. Die Dateiinformationen für den
     * angegebenen MD5-Hashwert werden aus der Datenbank entfernt.
     *
     * @param md5 MD5-Hashwert
     */
    void removeFileInfo(String md5) {
        fileDatabase.remove(md5);
    }

    /**
     * Speichert den Inhalt der Datenbank in ein CSV File. Die einzelnen Felder
     * werden durch Semikolon getrennt abgelegt. Zusätzlich wird zu Beginn der
     * Datei ein Header eingefügt.
     *
     * @param saveFile Export Datei
     * @param user     <code>true</code> wenn Benutzereinträge exportiert werden
     * sollen, <code>false</code> für Systemeinträge
     * @throws java.io.FileNotFoundException Wenn die angegebene Datei ungültig
     * ist oder nicht erstellt werden kann
     */
    void exportDB(File saveFile, boolean user) throws FileNotFoundException {
        PrintStream exportFile = new PrintStream(new FileOutputStream(saveFile));
        exportFile.println("MD5;Name;Dateityp;Softwarepaket;Version;Beschreibung");

        for (Entry<String, FileInfo> dbEntry : fileDatabase.entrySet()) {
            if ((dbEntry.getValue().isUser() && user) || (!dbEntry.getValue().isUser() && !user)) {
                exportFile.println(dbEntry.getKey() + ";" + dbEntry.getValue().getName() + ";" + dbEntry.getValue().getFileType() + ";" + dbEntry.getValue().getSoftwarePackage() + ";" + dbEntry.getValue().getVersion() + ";" + dbEntry.getValue().getDescription());
            }
        }

        exportFile.close();
    }
}
