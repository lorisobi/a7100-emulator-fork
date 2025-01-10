/*
 * LBAHardDisk.java
 *
 * Diese Datei gehört zum Projekt A7100 Emulator
 * Copyright (c) 2011-2025 Dirk Bräuer, Jens Markwardt
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
 *   09.01.2025 - Initiale Version aus FloppyDisk.java abgeleitet
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import a7100emulator.Tools.ConfigurationManager;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung einer LBA Festplatte
 *
 * @author Dirk Bräuer, Jens Markwardt
 */
public class LBAHardDisk implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(LBAHardDisk.class.getName());

    /**
     * Dateiname des Festplatten-Images
     */
    private String diskName;

    /**
     * Filepointer des Festplatten-Images
     */
    private File image;

    /**
     * Daten der Festplatte
     */
    public byte[] diskData = new byte[0];

    /**
     * Anzahl des Sektoren des Festplatten-Images
     */
    private int totalSectors = 0;

    /**
     * Gibt an, ob der Festplatteninhalt veraendert wurde
     */
    private boolean modified;

    /**
     * Legt ein Objekt fuer ein Festplatten-Image an
     *
     * @param diskName Name des zu oeffnenden Festplatten-Images
     */
    public LBAHardDisk(String diskName) {
        this.diskName = diskName;
    }

    /**
     * Gibt den Dateinamen des Festplatten-Images zurück
     *
     * @return Dateiname des Festplatten-Images
     */
    public String getDiskName() {
        return diskName;
    }

    /**
     * Gibt die Anzahl der Sektoren des Festplatten-Images zurück
     *
     * @return Anzahl der Sektoren des Festplatten-Images
     */
    public int getTotalSectors() {
        return totalSectors;
    }

    /**
     * Gibt an, ob das Image modifiziert wurde
     *
     * @return <code>true</code> wenn das Image modifiziert wurde,
     * <code>false</code> sonst
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Legt fest, ob das Image modifiziert wurde
     *
     * @param modified <code>true</code> wenn das Image modifiziert wurde,
     * <code>false</code> sonst
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Liest das Festplattenimage in den Speicher
     *
     * @throws java.io.IOException Wenn das Lesen des Festplatten-Images
     * vom Datentraeger nicht erfolgreich war
     */
    public void loadDisk() throws IOException {
        String directory = ConfigurationManager.getInstance().readString("directories", "harddisks", "./harddisks/");
        image = new File(directory + diskName);
        totalSectors = (int) (image.length() / 512);
        diskData = new byte[(int) image.length()];
        FileInputStream fis = new FileInputStream(image);
        fis.read(diskData);
        fis.close();
    }

    /**
     * Speichert das Festplattenimage auf den Datentraeger
     *
     * @throws java.io.IOException Wenn das Speichern des Festplatten-Images
     * auf dem Datentraeger nicht erfolgreich war
     */
    public void saveDisk() throws IOException {
        FileOutputStream fos = new FileOutputStream(image);
        fos.write(diskData);
        fos.close();
        modified = false;
    }

    /**
     * Speichert den Status der Festplatte in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeUTF(diskName);
        dos.writeInt(totalSectors);
        dos.writeBoolean(modified);
        dos.write(diskData);
    }

    /**
     * Liest den Status der Festplatte aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        diskName = dis.readUTF();
        totalSectors = dis.readInt();
        modified = dis.readBoolean();
        diskData = new byte[(int) totalSectors * 512];
        dis.read(diskData);
    }
}
