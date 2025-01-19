/*
 * V30IDE.java
 *
 * Minimalistische IDE Emulation zur Unterstuetzung des V30 IDE Adapters
 * in einer speziell angepassten Version von MUTOS 1700.
 * Die IDE Emulation ist unvollstaendig. Es werden lediglich die Dinge
 * emuliert, die der MUTOS Treiber verwendet.
 *
 * Diese Datei gehört zum Projekt A7100 Emulator
 * Copyright (c) 2011-2025, Dirk Bräuer, Jens Markwardt
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
 *   10.01.2025 - Initiale Version
 *   19.01.2025 - Device Control Register hinzugefuegt
 *              - IDE Software Reset hinzugefuegt
 *              - Test auf zu grosse Sektornummern hinzugefuegt
 */

package a7100emulator.components.modules;

import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.LBAHardDisk;
import a7100emulator.Tools.ConfigurationManager;
import a7100emulator.Tools.BitTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

/**
 * Klasse zur Emulation des V30 IDE Adapters
 *
 * @author Jens Markwardt
 */
public final class V30IDE implements IOModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(V30IDE.class.getName());

    /**
     * IDE Register Adressen
     */

    /**
     * Data Register
     */
    private final static int PORT_DATA = 0xe010;
    /**
     * Error Register Adresse (lesen)
     */
    private final static int PORT_ERROR = 0xe012;
    /**
     * Feature Register Adresse (schreiben)
     */
    private final static int PORT_FR = 0xe012;
    /**
     * Sector Count Register Adresse
     */
    private final static int PORT_SC = 0xe014;
    /**
     * Sector Number Register Adresse
     */
    private final static int PORT_SN = 0xe016;
    /**
     * Cylinder Low Register Adresse
     */
    private final static int PORT_CL = 0xe018;
    /**
     * Cylinder High Register Adresse
     */
    private final static int PORT_CH = 0xe01a;
    /**
     * Device Head Register Adresse
     */
    private final static int PORT_DH = 0xe01c;
    /**
     * Primary Status Register Adresse (lesen)
     */
    private final static int PORT_STAT = 0xe01e;
    /**
     * Command Register Adresse (schreiben)
     */
    private final static int PORT_CMD = 0xe01e;
    /**
     * Alternate Status Register Adresse (lesen)
     */
    private final static int PORT_ASTAT = 0xe02c;
    /**
     * Device Control Register Adresse (schreiben)
     */
    private final static int PORT_DC = 0xe02c;

    /**
     * IDE Status Flags
     */

    /**
     * Busy Flag
     */
    private final static int BSY = 0x80;
    /**
     * Device Fault Flag
     */
    private final static int DF = 0x20;
    /**
     * Data Request Flag
     */
    private final static int DRQ = 0x08;
    /**
     * Error Flag
     */
    private final static int ERR = 0x01;

    /**
     * IDE Error Flags
     */

    /**
     * Command Aborted Flag
     */
    private final static int ABRT = 0x04;

    /**
     * IDE Commands
     */

    /**
     * Identify Device Kommando
     */
    private final static int IDENTDV = 0xec;
    /**
     * Read Sector(s) Kommando
     */
    private final static int RD_SECT = 0x20;
    /**
     * Write Sector(s) Kommando
     */
    private final static int WR_SECT = 0x30;
    /**
     * Flush Cache Kommando
     */
    private final static int FLSHCCH = 0xe7;

    /**
     * Sonstige IDE Bits
     */

    /**
     * Unit 0 Bitmaske
     */
    private final static int DEV0 = 0x00;
    /**
     * Unit 1 Bitmaske
     */
    private final static int DEV1 = 0x10;
    /**
     * LBA Modus Bitmaske
     */
    private final static int LBA = 0x40;
    /**
     * LBA Unterstuetzung (IDENTDV word 49, bit 9)
     */
    private final static int LBACAP = 0x200;
    /**
     * Device control: set SRST Bitmaske
     */
    private final static int RSTON = 0x4;
    /**
     * Device control: clear SRST Bitmaske
     */
    private final static int RSTOFF = 0x0;
    /**
     * Device control: set nIEN Bitmaske
     */
    private final static int IRQOFF = 0x2;

    /**
     * Speicher fuer die IDE Register der beiden Units
     */

    /**
     * Speicher fuer das Error Register
     */
    private static int[] reg_error = new int[2];
    /**
     * Speicher fuer das Sector Count Register
     */
    private static int[] reg_sc = new int[2];
    /**
     * Speicher fuer das Sector Number Register
     */
    private static int[] reg_sn = new int[2];
    /**
     * Speicher fuer das Cylinder Low Register
     */
    private static int[] reg_cl = new int[2];
    /**
     * Speicher fuer das Cylinder High Register
     */
    private static int[] reg_ch = new int[2];
    /**
     * Speicher fuer das Device Head Register
     */
    private static int[] reg_dh = new int[2];
    /**
     * Speicher fuer das Primary Status Register
     */
    private static int[] reg_stat = new int[2];
    /**
     * Speicher fuer das Alternate Status Register
     */
    private static int[] reg_astat = new int[2];
    /**
     * Speicher fuer das Device Control Register
     */
    private static int[] reg_dc = new int[2];

    /**
     * Merkvariable fuer die aktuelle Unit
     */
    private static int currUnit = 0;

    /**
     * Speicher fuer die Festplatten-Images
     */
    private LBAHardDisk[] image = new LBAHardDisk[2];

    /**
     * Speicher fuer den IDENTIFY-DEVICE Sektor
     */
    private static byte[] identDev = new byte[512];

    /**
     * "Zeiger"-Variable: zeigt entweder auf eines der Festplatten-Images
     * oder auf den IDENTIFY-DEVICE Sektor
     */
    private static byte[] buffer = identDev;

    /**
     * Index des aktuellen Bytes innerhalb des Buffer-Speichers
     */
    private static int bufferIdx = 0;

    /**
     * Anzahl der Bytes, die noch zu lesen oder zu schreiben sind
     */
    private static int bytesLeft = 0;

    /**
     * Erstellt eine neue V30 IDE Instanz
     */
    public V30IDE() {
        init();
    }

    /**
     * Registriert die Ports am Systembus
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_DATA);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ERROR);
        MMS16Bus.getInstance().registerIOPort(this, PORT_SC);
        MMS16Bus.getInstance().registerIOPort(this, PORT_SN);
        MMS16Bus.getInstance().registerIOPort(this, PORT_CL);
        MMS16Bus.getInstance().registerIOPort(this, PORT_CH);
        MMS16Bus.getInstance().registerIOPort(this, PORT_DH);
        MMS16Bus.getInstance().registerIOPort(this, PORT_STAT);
        MMS16Bus.getInstance().registerIOPort(this, PORT_CMD);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASTAT);
        MMS16Bus.getInstance().registerIOPort(this, PORT_DC);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        switch (port) {
            case PORT_DATA:
                buffer[bufferIdx++] = (byte) data;
                bytesLeft--;
                if (bytesLeft == 0) {
                    reg_stat[currUnit] = 0;
                }
                //System.out.println("Write Byte " + String.format("0x%02X", data) + " to DATA register (bytesLeft = " + bytesLeft + ")");
                break;
            case PORT_SC:
                reg_sc[currUnit] = data;
                //System.out.println("Write register SC [unit " + currUnit + "] : " + String.format("0x%02X", reg_sc[currUnit]));
                break;
            case PORT_SN:
                reg_sn[currUnit] = data;
                //System.out.println("Write register SN [unit " + currUnit + "] : " + String.format("0x%02X", reg_sn[currUnit]));
                break;
            case PORT_CL:
                reg_cl[currUnit] = data;
                //System.out.println("Write register CL [unit " + currUnit + "] : " + String.format("0x%02X", reg_cl[currUnit]));
                break;
            case PORT_CH:
                reg_ch[currUnit] = data;
                //System.out.println("Write register CH [unit " + currUnit + "] : " + String.format("0x%02X", reg_ch[currUnit]));
                break;
            case PORT_DH:
                reg_dh[currUnit] = data;
                //System.out.println("Write register DH [unit " + currUnit + "] : " + String.format("0x%02X", reg_dh[currUnit]));
                /**
                 * Pruefe, ob ein Wechsel der aktuellen
                 * Unit angefordert wurde
                 */
                chkSwitchDev();
                break;
            case PORT_CMD:
                switch (data) {
                    case IDENTDV:
                        //System.out.println("IDENTIFY DEVICE command received for unit " + currUnit);
                        int totalSectors = image[currUnit].getTotalSectors();
                        if (totalSectors > 0) {
                            fillIdentDevBuffer(image[currUnit].getDiskName(), totalSectors);
                        } else {
                            reg_stat[currUnit] = reg_stat[currUnit] | DF;
                            reg_stat[currUnit] = reg_stat[currUnit] | ERR;
                            break;
                        }
                        reg_stat[currUnit] = reg_stat[currUnit] | DRQ;
                        buffer = identDev;
                        bufferIdx = 0;
                        bytesLeft = 512;
                        break;
                    case RD_SECT:
                        //System.out.println("READ SECTOR(S) command received for unit " + currUnit);
                        setupHarddiskReadWrite();
                        break;
                    case WR_SECT:
                        //System.out.println("WR_SECTOR(S) command received for unit " + currUnit);
                        setupHarddiskReadWrite();
                        break;
                    case FLSHCCH:
                        //System.out.println("FLUSH CACHE command received for unit " + currUnit);
                        break;
                    default:
                        LOG.log(Level.WARNING, "IDE Kommando " + String.format("0x%02X", data) + " ist noch nicht unterstuetzt");
                }
                break;
            case PORT_FR:
                break;
            case PORT_DC:
                reg_dc[currUnit] = data;
                //System.out.println("Write register DC [unit " + currUnit + "] : " + String.format("0x%02X", reg_dc[currUnit]));
                /**
                 * Test auf Software Reset
                 */
                if (BitTest.getBit(reg_dc[currUnit], 2)) {
                    //System.out.println("IDE Software Reset");
                    for (int i = 0; i < 2; i++) {
                        reg_error[i] = 0;
                        reg_sc[i] = 0;
                        reg_sn[i] = 0;
                        reg_cl[i] = 0;
                        reg_ch[i] = 0;
                        reg_dh[i] = 0;
                        reg_stat[i] = 0;
                        reg_astat[i] = 0;
                        reg_dc[i] = 0;
                    }
                }
                break;
            default:
                LOG.log(Level.WARNING, "Unbekannter Write-Port " + String.format("0x%04X", port));
        }
    }

    /**
     * Gibt ein Wort auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortWord(int port, int data) {
        int low, high;

        switch (port) {
            case PORT_DATA:
                low = data & 0x00ff;
                high = (data & 0xff00) >> 8;
                buffer[bufferIdx++] = (byte) low;
                buffer[bufferIdx++] = (byte) high;
                bytesLeft = bytesLeft - 2;
                image[currUnit].setModified(true);
                if (bytesLeft == 0) {
                    reg_stat[currUnit] = 0;
                }
                //System.out.println("Write word " + String.format("0x%04X", data) + " to DATA register (bytesLeft = " + bytesLeft + ")");
                break;
            default:
                LOG.log(Level.WARNING, "Unbekannter Write-Port " + String.format("0x%04X", port));
        }
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
        int data;

        switch (port) {
            case PORT_DATA:
                data = buffer[bufferIdx++];
                bytesLeft--;
                if (bytesLeft == 0) {
                    reg_stat[currUnit] = 0;
                }
                //System.out.println("Read Byte " + String.format("0x%02X", (data)) + " from DATA register (bytesLeft = " + bytesLeft + ")");
                return data;
            case PORT_ERROR:
                return(reg_error[currUnit]);
            case PORT_STAT:
                //System.out.println("STAT [unit " + currUnit + "] : " + String.format("0x%02X", reg_stat[currUnit]));
                return(reg_stat[currUnit]);
            default:
                LOG.log(Level.WARNING, "Unbekannter Read-Port " + String.format("0x%04X", port));
        }
        return 0;
    }

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPortWord(int port) {
        int msb, lsb, data;

        switch (port) {
            case PORT_DATA:
                msb = buffer[bufferIdx++] & 0xff;
                lsb = buffer[bufferIdx++] & 0xff;
                bytesLeft = bytesLeft - 2;
                if (bytesLeft == 0) {
                    reg_stat[currUnit] = 0;
                }
                /**
                 * Vertausche die Bytes im Wort
                 */
                data = (lsb << 8) | msb;
                //System.out.println("Read word " + String.format("0x%04X", (data)) + " from DATA register (bytesLeft = " + bytesLeft + ")");
                return data;
            default:
                LOG.log(Level.WARNING, "Unbekannter Read-Port " + String.format("0x%04X", port));
        }
        return 0;
    }

    /**
     * Initialisiert die V30 IDE Emulation
     */
    @Override
    public void init() {
        registerPorts();
        initializeHarddiskImages();
    }

    /**
     * Schreibt den Zustand der V30 IDE Emulation in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 2; i++) {
            dos.writeInt(reg_error[i]);
            dos.writeInt(reg_sc[i]);
            dos.writeInt(reg_sn[i]);
            dos.writeInt(reg_cl[i]);
            dos.writeInt(reg_ch[i]);
            dos.writeInt(reg_dh[i]);
            dos.writeInt(reg_stat[i]);
            dos.writeInt(reg_astat[i]);
            dos.writeInt(reg_dc[i]);
            image[i].saveState(dos);
        }
        dos.write(identDev);
        dos.writeInt(currUnit);
        dos.writeInt(bufferIdx);
        dos.writeInt(bytesLeft);
    }

    /**
     * Liest den Zustand der V30 IDE Emulation aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 2; i++) {
            reg_error[i] = dis.readInt();
            reg_sc[i] = dis.readInt();
            reg_sn[i] = dis.readInt();
            reg_cl[i] = dis.readInt();
            reg_ch[i] = dis.readInt();
            reg_dh[i] = dis.readInt();
            reg_stat[i] = dis.readInt();
            reg_astat[i] = dis.readInt();
            reg_dc[i] = dis.readInt();
            image[i].loadState(dis);
        }
        dis.read(identDev);
        currUnit = dis.readInt();
        buffer = image[currUnit].diskData;
        bufferIdx = dis.readInt();
        bytesLeft = dis.readInt();
    }

    /**
     * Speichert die veraenderten Daten der Festplatten-Images auf dem Datentraeger
     */
    public void syncImages() {
        for (int i = 0; i < 2; i++) {
            boolean isVolatile = ConfigurationManager.getInstance().readBoolean("V30IDE", "isVolatile_unit_" + (char)(i + '0'), false);
            if (image[i].isModified() && !isVolatile) {
                LOG.log(Level.INFO, "Schreibe Image von Unit " + i + " (" + image[i].getDiskName() + ") zurueck auf den Datentraeger");
                try {
                    image[i].saveDisk();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "Fehler beim Schreiben des Festplattenimages Unit " + i + ": " + ex);
                }
            }
        }
    }

    /**
     * Prueft, ob ein Wechsel des aktuellen Laufwerks angefordert wurde
     * und passt die Variable <code>currUnit</code> entsprechend an
     */
    private void chkSwitchDev() {
        switch ((reg_dh[currUnit]) & DEV1) {
            case DEV0:
                if (currUnit == 0) {
                    return;
                }
                currUnit = 0;
                break;
            case DEV1:
                if (currUnit == 1) {
                    return;
                }
                currUnit = 1;
                break;
        }
        //System.out.println("Switch to IDE unit " + currUnit);
    }

    /**
     * Fuellt den IDENTIFY DEVICE Sektor-Puffer
     * mit den notwendigen Daten (unvollstaendig)
     *
     * @param imageName Name des Festplatten-Images
     * @param numSectors Anahl der Sektoren des Festplatten-Images
     */
    private void fillIdentDevBuffer(String imageName, int numSectors) {
        /**
         * Fuelle die Eintraege "firmware" und "model name"
         * komplett mit Leerzeichen auf
         */
        Arrays.fill(identDev, 23*2, 47*2, (byte) ' ');

        /**
         * Trage die Firmware-Version des emulierten Laufwerks ein
         */
        String firmware = "unknown";
        int fwLength = firmware.length();
        /**
         * max. 8 Zeichen fuer die Firmware
         */
        if (fwLength > 8) {
            fwLength = 8;
        }
        /**
         * Offset der Firmware im identDev Sektor
         */
        int fwOffset = 23*2;
        for (int i = 0; i < fwLength; i++) {
            identDev[fwOffset + i] = (byte) (firmware.toCharArray())[i];
        }

        /**
         * Trage den Modell-Namen des emulierten Laufwerks ein,
         * nutze den Dateinamen des Images als Modell-Namen
         */
        int nameLength = imageName.length();
        /**
         * max. 40 Zeichen fuer den Modell-Namen
         */
        if (nameLength > 40) {
            nameLength = 40;
        }
        /**
         * Offset des Modell-Namens im identDev Sektor
         */
        int nameOffset = 27*2;
        for (int i = 0; i < nameLength; i++) {
            identDev[nameOffset + i] = (byte) (imageName.toCharArray())[i];
        }

        /**
         * Setze Bit 9 (LBA supported) im Capabilities-Config-Word (Wort 49)
         */
        identDev[49*2] = (byte) (LBACAP >> 8);

        /**
         * Trage die Anzahl der Sektoren im LBA-Modus im Little-Endian-Format ein
         * Bsp: numSectors = 0x11223344
         */
        identDev[60*2 + 0] = (byte) ((numSectors & 0x0000ff00) >> 8);    // Bits  8..15: 0x33
        identDev[60*2 + 1] = (byte)  (numSectors & 0x000000ff);          // Bits  0.. 7: 0x44
        identDev[60*2 + 2] = (byte) ((numSectors & 0xff000000) >> 24);   // Bits 24..31: 0x11
        identDev[60*2 + 3] = (byte) ((numSectors & 0x00ff0000) >> 16);   // Bits 16..23: 0x22

        /**
         * Tausche Bytes innerhalb jedes Wortes fuer den gesamten Sektor
         */
        for (int i = 0; i < 512; i = i + 2) {
            byte temp = identDev[i];
            identDev[i] = identDev[i+1];
            identDev[i+1] = temp;
        }
    }

    /**
     * Bereitet das RD_SECT/WR_SECT Kommando vor
     */
    private void setupHarddiskReadWrite() {
        buffer = image[currUnit].diskData;
        bytesLeft = reg_sc[currUnit] * 512;
        int snum = ((reg_dh[currUnit] & 0xf) << 24) |
                    (reg_ch[currUnit] << 16) |
                    (reg_cl[currUnit] << 8) |
                     reg_sn[currUnit];
        bufferIdx = snum * 512;
        //System.out.println("  DH [unit " + currUnit + "] : " + String.format("0x%02X", reg_dh[currUnit]));
        //System.out.println("  CH [unit " + currUnit + "] : " + String.format("0x%02X", reg_ch[currUnit]));
        //System.out.println("  CL [unit " + currUnit + "] : " + String.format("0x%02X", reg_cl[currUnit]));
        //System.out.println("  SN [unit " + currUnit + "] : " + String.format("0x%02X", reg_sn[currUnit]));
        //System.out.println("  SC [unit " + currUnit + "] : " + String.format("0x%02X", reg_sc[currUnit]));
        //System.out.println("     bytesLeft = " + bytesLeft);
        //System.out.println("     sector number = " + snum);
        //System.out.println("     bufferIdx = " + bufferIdx);

        if (snum >= image[currUnit].getTotalSectors()) {
            reg_stat[currUnit] = reg_stat[currUnit] | DF;
            reg_stat[currUnit] = reg_stat[currUnit] | ERR;
        } else {
            reg_stat[currUnit] = reg_stat[currUnit] | DRQ;
        }
    }

    /**
     * Liest die Festplatten-Images in den Speicher ein
     */
    private void initializeHarddiskImages() {
        for (int i = 0; i < 2; i++) {
            String diskName = ConfigurationManager.getInstance().readString("V30IDE", "hd_unit_" + (char)(i + '0'), "hd" + (char)(i + '0') + ".img");
            image[i] = new LBAHardDisk(diskName);
            try {
                image[i].loadDisk();
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Fehler beim Einlesen des Festplattenimages Unit " + i + ": " + ex);
            }
        }
    }
}
