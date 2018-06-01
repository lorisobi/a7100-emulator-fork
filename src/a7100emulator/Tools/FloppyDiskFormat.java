/*
 * FloppyDiskFormat.java
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
 *   13.10.2016 - Erste Version
 */
package a7100emulator.Tools;

/**
 * Enum mit vordefinierte Diskettenformaten für häufig verwendete Formate.
 * <p>
 * TODO: Diese Klasse ist für eine spätere Verwendung vorgesehen.
 *
 * @author Dirk Bräuer
 */
public enum FloppyDiskFormat {

    /**
     * 8" Hausformat mit 294KByte
     */
    SCP_8_294K("8\" Hausformat 294KByte", 77, 1, 4, 1024, 26, 128),
    /**
     * 8" CPM/M Standardformat mit 239KByte
     */
    SCP_8_239K("8\" CP/M-Standardformat 239KByte", 77, 1, 26, 128, 26, 128),
    /**
     * 5.25" Hausformat mit 306KByte
     */
    SCP_525_306("5.25\" Hausformat 306K", 80, 1, 16, 256, 16, 128),
    /**
     * 5.25" Hausformat mit 620KByte
     */
    SCP_525_640("5.25\" Hausformat 620K", 80, 2, 16, 256, 16, 128),
    /**
     * MUTOS DMF Format mit 720KByte
     */
    MUTOS_DMF("5.25\" MUTOS-Format 720K", 80, 2, 9, 512, 9, 512);

    /**
     * Formatbezeichnung
     */
    private final String name;

    /**
     * Anzahl der Zylinder
     */
    private final int cylinder;

    /**
     * Anzahl der Seiten / Köpfe
     */
    private final int heads;

    /**
     * Anzahl der Sektoren je Zylinder
     */
    private final int sectorsPerTrack;

    /**
     * Anzahl der Bytes je Sektor
     */
    private final int bytesPerSector;

    /**
     * Anzahl der Sektoren in Spur 0 (Zylinder 0 / Seite 0)
     */
    private final int sectorsInTrack0;

    /**
     * Anzahl der Bytes je Sektor in Spur 0 (Zylinder 0 / Seite 0)
     */
    private final int bytesPerSectorTrack0;

    /**
     * Erstellt ein neues FloppyDiskFormat.
     *
     * @param name Bezeichner
     * @param cylinder Anzahl der Zylinder 7 Spuren
     * @param heads Anzahl der Seiten/Köpfe
     * @param sectorsPerTrack Anzahl der Sektro je Spur
     * @param bytesPerSector Anzahl der Bytes je Sektor
     * @param sectorsInTrack0 Anzahl der Sektoren in Spur 0
     * @param bytesPerSectorTrack0 Anzahl der Bytes je Sektor in Spur 0
     */
    FloppyDiskFormat(String name, int cylinder, int heads, int sectorsPerTrack, int bytesPerSector, int sectorsInTrack0, int bytesPerSectorTrack0) {
        this.name = name;
        this.cylinder = cylinder;
        this.heads = heads;
        this.sectorsPerTrack = sectorsPerTrack;
        this.bytesPerSector = bytesPerSector;
        this.sectorsInTrack0 = sectorsInTrack0;
        this.bytesPerSectorTrack0 = bytesPerSectorTrack0;
    }

    /**
     * Gibt den Formatbezeichner zurück.
     *
     * @return Bezeichnung
     */
    public String getName() {
        return name;
    }

    /**
     * Gibt die Anzahl der Zylinder zurück.
     *
     * @return Anzahl der Zylinder
     */
    public int getCylinder() {
        return cylinder;
    }

    /**
     * Gibt die Anzahl der Köpfe / Seiten zurück.
     *
     * @return Anzahl der Seiten
     */
    public int getHeads() {
        return heads;
    }

    /**
     * Gibt die Anzahl der Sektoren je Spur zurück.
     *
     * @return Anzahl der Sektoren je Spur.
     */
    public int getSectorsPerTrack() {
        return sectorsPerTrack;
    }

    /**
     * Gibt die Anzahl der Bytes je Sektor zurück.
     *
     * @return Anzahl der Bytes je Sektor
     */
    public int getBytesPerSector() {
        return bytesPerSector;
    }

    /**
     * Gibt die Anzahl der Sektoren in Spur 0 zurück.
     *
     * @return Anzahl der Sektoren in Spur 0
     */
    public int getSectorsInTrack0() {
        return sectorsInTrack0;
    }

    /**
     * Gibt die Anzahl der Bytes je Sektor in Spur 0 zurück.
     *
     * @return Anzahl der Bytes je Sektor in Spur 0
     */
    public int getBytesPerSectorTrack0() {
        return bytesPerSectorTrack0;
    }

}
