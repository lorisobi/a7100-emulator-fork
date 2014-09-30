/*
 * SCPFile.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *   27.09.2014 MD5 Summen ergänzt
 *
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verarbeitung einer SCP-Datei
 *
 * @author Dirk Bräuer
 */
public class SCPFile {

    /**
     * User
     */
    private int user;
    /**
     * Dateiname
     */
    private String name;
    /**
     * Endung
     */
    private String extension;
    /**
     * Gibt an ob die Datei schreibgeschützt ist
     */
    private boolean readOnly;
    /**
     * Gibt an ob es sich um eine Systemdatei handelt
     */
    private boolean system;
    /**
     * Gibt an, ob das Attribut extra gesetzt ist
     */
    private boolean extra;
    /**
     * Inhalt der Datei
     */
    private byte[] data;
    /**
     * MD5 Hash
     */
    private String md5;

    /**
     * Erstellt eine neue SCP-Datei
     *
     * @param name Name
     * @param extension Endung
     * @param readOnly Schreibschutz
     * @param system Systemdatei
     * @param extra Extra-Attribut
     * @param user Nutzernummer
     */
    SCPFile(String name, String extension, boolean readOnly, boolean system, boolean extra, int user) {
        this.name = name;
        this.extension = extension;
        this.readOnly = readOnly;
        this.system = system;
        this.extra = extra;
        this.user = user;
    }

    /**
     * Gibt den Namen der Datei zurück
     *
     * @return Name
     */
    String getName() {
        return name;
    }

    /**
     * Setzt den Namen der DAtei
     *
     * @param name Neuer Dateiname
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Gibt die Erweiterung der Datei zurück
     *
     * @return Erweiterung
     */
    String getExtension() {
        return extension;
    }

    /**
     * Setzt die Dateierweiterung
     *
     * @param extension Neue Erweiterung
     */
    void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Gibt den Namen+Erweiterung zurück
     *
     * @return Voller Dateiname
     */
    String getFullName() {
        return name.trim() + "." + extension.trim();
    }

    /**
     * Gibt an, ob die Datei schreibgeschützt ist
     *
     * @return true - wenn schreibgeschützt , false - sonst
     */
    boolean isReadOnlyFile() {
        return readOnly;
    }

    /**
     * Gibt die MD5 Prüfsumme zurück
     *
     * @return MD5 Prüfsumme
     */
    String getMD5() {
        return md5;
    }

    /**
     * Setzt den Schreibschutz der Datei
     *
     * @param readOnlyFile true - wenn schreibgeschützt , false - sonst
     */
    void setReadOnlyFile(boolean readOnlyFile) {
        this.readOnly = readOnlyFile;
    }

    /**
     * Gibt an, ob es sich um eine Systemdatei handelt
     *
     * @return true - wenn Systemdatei , false - sonst
     */
    boolean isSystemFile() {
        return system;
    }

    /**
     * Setzt den System-Status einer DAtei
     *
     * @param systemFile true - wenn Systemdatei , false - sonst
     */
    void setSystemFile(boolean systemFile) {
        this.system = systemFile;
    }

    /**
     * Gibt die Daten der Datei zurück
     *
     * @return Daten
     */
    byte[] getData() {
        return data;
    }

    /**
     * Setzt die Daten der Datei
     *
     * @param data neue Daten
     */
    void setData(byte[] data) {
        this.data = data;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(data);
            byte[] digest = md5.digest();
            String digestString = "";
            for (byte b : digest) {
                digestString += String.format("%02X", b);
            }
            this.md5 = digestString;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SCPFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gibt die Nutzernummer der Datei zurück
     *
     * @return Nutzernummer
     */
    int getUser() {
        return user;
    }

    /**
     * Setzt die Nutzernummer der DAtei
     *
     * @param user Neue Nutzernumer
     */
    void setUser(int user) {
        this.user = user;
    }

    /**
     * Gibt das Extra-Attribut der Datei zurück
     *
     * @return true - wenn Extra gesetzt , false - sonst
     */
    boolean isExtra() {
        return extra;
    }

    /**
     * Setzt das Extra-Attribut
     *
     * @param extra true - wenn Extra Attribut gesetzt werden soll, false -
     * sonst
     */
    void setExtra(boolean extra) {
        this.extra = extra;
    }

    /**
     * Setzt die MD5 Prüfsumme
     *
     * @param MD5 MD5 Prüfsumme
     */
    void setMD5(String md5) {
        this.md5 = md5;
    }
}
