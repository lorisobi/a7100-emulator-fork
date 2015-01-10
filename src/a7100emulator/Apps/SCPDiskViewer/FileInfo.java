/*
 * FileInfo.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   30.12.2014 - Erste Version
 *   01.01.2015 - FileType gelöscht und durch String ersetzt
 *   02.01.2015 - Kommentare ergänzt
 */
package a7100emulator.Apps.SCPDiskViewer;

/**
 * Klasse zum Speichern von Dateiinformationen.
 *
 * @author Dirk Bräuer
 */
public class FileInfo {

    /**
     * Gewöhnlicher Name
     */
    private final String name;
    /**
     * Dateityp
     */
    private final String fileType;
    /**
     * Software-Paket
     */
    private final String softwarePackage;
    /**
     * Versionsnummer
     */
    private final String version;
    /**
     * Beschreibung
     */
    private final String description;
    /**
     * <code>true</code> wenn es sich um einen Benutzerdefinierten Eintrag
     * handelt, <code>false</code> sonst
     */
    private final boolean user;

    /**
     * Legt neue Dateiinformationen an.
     *
     * @param name Name
     * @param fileType Dateityp
     * @param softwarePackage Software-Paket
     * @param version Versionsnummer
     * @param description Beschreibung
     * @param user Benutzerdefinierter Eintrag
     */
    FileInfo(String name, String fileType, String softwarePackage, String version, String description, boolean user) {
        this.name = name;
        this.version = version;
        this.fileType = fileType;
        this.softwarePackage = softwarePackage;
        this.description = description;
        this.user = user;
    }

    /**
     * Gibt den Namen der Datei zurück.
     *
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gibt die Versionsnummer der Datei zurück.
     *
     * @return Versionsnummer
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gibt den Dateityp zurück.
     *
     * @return Dateityp
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Gibt das Software-Paket zurück.
     *
     * @return Software-Paket
     */
    public String getSoftwarePackage() {
        return softwarePackage;
    }

    /**
     * Gibt die Beschreibung zurück.
     *
     * @return Beschreibung
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gibt an, ob es sich um einen benutzerdefinierten Eintrag handelt.
     *
     * @return <code>true</code> bei einem benutzerdefinierten Eintrag,
     * <code>false</code> sonst.
     */
    public boolean isUser() {
        return user;
    }
}
