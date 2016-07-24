/*
 * FileInfo.java
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
