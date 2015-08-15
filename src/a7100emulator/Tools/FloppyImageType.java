/*
 * FloppyImageType.java
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
 *   15.07.2014 - Erste Version ausgelagert aus FloppyDisk
 *   14.08.2015 - CopyQM hinzugefügt
 */
package a7100emulator.Tools;

/**
 * Enum mit unterstützten Images
 */
public enum FloppyImageType {

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
     * DMK (Catweasel) Image
     */
    DMK,
    /**
     * CopyQM Image
     */
    COPYQM
}
