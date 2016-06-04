/*
 * AFS.java
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
 *   01.04.2014 - Kommentare vervollständigt
 *   29.11.2015 - Lokale Ports hinzugefügt
 *   05.12.2015 - Speicher hinzugefügt, SIO hinzugefügt
 *              - Port-Methoden hinzugefügt
 *   02.01.2016 - CPT Register implementiert
 *   26.03.2016 - Speichern und Laden aktualisiert
 *              - Reihenfolge der ROMs geändert
 *   27.03.2016 - Steuerregister CPT1, CPT2 hinzugefügt
 *   24.04.2016 - Lesen/Schreiben SIO Ports implementiert
 *              - Signal /ESE in CPT3 implementiert
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.UA856;
import a7100emulator.components.system.FloppyDrive;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der AFS (Anschluß für Folienspeicher)
 *
 * @author Dirk Bräuer
 */
public final class AFS implements Module {

    /**
     * Lokaler Port SIO Daten Kanal A
     */
    private final static int LOCAL_PORT_SIO_DATA_A = 0x90;
    /**
     * Lokaler Port SIO Daten Kanal B
     */
    private final static int LOCAL_PORT_SIO_DATA_B = 0x91;
    /**
     * Lokaler Port SIO Control Kanal A
     */
    private final static int LOCAL_PORT_SIO_CONTROL_A = 0x92;
    /**
     * Lokaler Port SIO Control Kanal B
     */
    private final static int LOCAL_PORT_SIO_CONTROL_B = 0x93;
    /**
     * Lokaler Port Steuerregister CPT3 0
     */
    private final static int LOCAL_PORT_CPT3_0 = 0x94;
    /**
     * Lokaler Port Steuerregister CPT3 1
     */
    private final static int LOCAL_PORT_CPT3_1 = 0x95;
    /**
     * Lokaler Port Steuerregister CPT3 2
     */
    private final static int LOCAL_PORT_CPT3_2 = 0x96;
    /**
     * Lokaler Port Steuerregister CPT3 3
     */
    private final static int LOCAL_PORT_CPT3_3 = 0x97;
    /**
     * Lokaler Port Steuerregister CPT1 0
     */
    private final static int LOCAL_PORT_CPT1_0 = 0x98;
    /**
     * Lokaler Port Steuerregister CPT1 1
     */
    private final static int LOCAL_PORT_CPT1_1 = 0x99;
    /**
     * Lokaler Port Steuerregister CPT1 2
     */
    private final static int LOCAL_PORT_CPT1_2 = 0x9A;
    /**
     * Lokaler Port Steuerregister CPT1 3
     */
    private final static int LOCAL_PORT_CPT1_3 = 0x9B;
    /**
     * Lokaler Port Steuerregister CPT2 0
     */
    private final static int LOCAL_PORT_CPT2_0 = 0x9C;
    /**
     * Lokaler Port Steuerregister CPT2 1
     */
    private final static int LOCAL_PORT_CPT2_1 = 0x9D;
    /**
     * Lokaler Port Steuerregister CPT2 2
     */
    private final static int LOCAL_PORT_CPT2_2 = 0x9E;
    /**
     * Lokaler Port Steuerregister CPT2 3
     */
    private final static int LOCAL_PORT_CPT2_3 = 0x9F;

    /**
     * Array der angeschlossenen Laufwerke
     */
    private final FloppyDrive[] drives = new FloppyDrive[4];
    /**
     * Speicher für Eprom-Inhalte
     */
    private final Memory eproms = new Memory(0x1000);
    /**
     * Steuerregister 1
     */
    private int cpt1;
    /**
     * Steuerregister 2
     */
    private int cpt2;
    /**
     * SIO für Kommunikation mit Floppylaufwerken
     */
    private final UA856 sio = new UA856();
    /**
     * Zustand Markenerkennung letzter Durchlauf
     */
    private boolean me = false;

    /**
     * Erzeugt eine neue AFS
     */
    public AFS() {
        init();
    }

    /**
     * Gibt die Referenz auf das entsprechende Diskettenlaufwerk zurück
     *
     * @param id ID des Laufwerks
     * @return Laufwerk
     */
    public FloppyDrive getFloppy(int id) {
        return drives[id];
    }

    /**
     * Initialisiert die AFS
     */
    @Override
    public void init() {
        final File afsRom1 = new File("./eproms/AFS-K5171-P873.rom");
        if (!afsRom1.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + afsRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        final File afsRom2 = new File("./eproms/AFS-K5171-P872.rom");
        if (!afsRom2.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + afsRom2.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        eproms.loadFile(0x0000, afsRom1, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
        eproms.loadFile(0x0800, afsRom2, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);

        drives[0] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[1] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[2] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
        drives[3] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
    }

    /**
     * Liest ein Byte aus den Eproms der AFS.
     *
     * @param address Adresse
     * @return Gelesenes Byte
     */
    int readByte(int address) {
        return eproms.readByte(address - 0x4000);
    }

    /**
     * Liest ein Wort aus den Eproms der AFS.
     *
     * @param address Adresse
     * @return Gelesenes Wort
     */
    int readWord(int address) {
        return eproms.readWord(address - 0x4000);
    }

    /**
     * Gibt Daten auf einem lokalen Port aus.
     *
     * @param port Port
     * @param data Daten
     */
    void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_SIO_DATA_A:
                System.out.println(String.format("Schreibe SIO-Port A Data: %02X", (Integer.reverse(data) >> 24) & 0xFF));
                System.out.println("Schreiben von SIO-Port noch nicht implementiert");
                // Daten für SIO liegen in umgekehrter Reihenfolge an!
                sio.writeData(0, (Integer.reverse(data) >> 24) & 0xFF);
                break;
            case LOCAL_PORT_SIO_DATA_B:
                System.out.println(String.format("Schreibe SIO-Port B Data: %02X", (Integer.reverse(data) >> 24) & 0xFF));
                System.out.println("Schreiben von SIO-Port noch nicht implementiert");
                // Daten für SIO liegen in umgekehrter Reihenfolge an!
                sio.writeData(1, (Integer.reverse(data) >> 24) & 0xFF);
                break;
            case LOCAL_PORT_SIO_CONTROL_A:
                System.out.println(String.format("Schreibe SIO-Port A Control: %02X", (Integer.reverse(data) >> 24) & 0xFF));
                System.out.println("Schreiben von SIO-Port noch nicht implementiert");
                // Daten für SIO liegen in umgekehrter Reihenfolge an!
                sio.writeControl(0, (Integer.reverse(data) >> 24) & 0xFF);
                break;
            case LOCAL_PORT_SIO_CONTROL_B:
                System.out.println(String.format("Schreibe SIO-Port B Control: %02X", (Integer.reverse(data) >> 24) & 0xFF));
                System.out.println("Schreiben von SIO-Port noch nicht implementiert");
                // Daten für SIO liegen in umgekehrter Reihenfolge an!
                sio.writeControl(1, (Integer.reverse(data) >> 24) & 0xFF);
                break;
            case LOCAL_PORT_CPT3_0:
            case LOCAL_PORT_CPT3_1:
            case LOCAL_PORT_CPT3_2:
            case LOCAL_PORT_CPT3_3:
                throw new IllegalArgumentException("Schreiben von CPT3 Port nicht erlaubt");
            case LOCAL_PORT_CPT1_0:
            case LOCAL_PORT_CPT1_1:
            case LOCAL_PORT_CPT1_2:
            case LOCAL_PORT_CPT1_3: {
                //System.out.println(String.format("Schreibe CPT1: %02X", data));
                cpt1 = data;
                int selectedDrive = getSelectedDrive();
                if (selectedDrive != -1) {
                    drives[selectedDrive].setWriteEnabled(!BitTest.getBit(cpt1, 0));
                    drives[selectedDrive].setStep(BitTest.getBit(cpt1, 3));
                }
            }
            break;
            case LOCAL_PORT_CPT2_0:
            case LOCAL_PORT_CPT2_1:
            case LOCAL_PORT_CPT2_2:
            case LOCAL_PORT_CPT2_3: {
                //System.out.println(String.format("Schreibe CPT2: %02X", data));
                cpt2 = data;
                int selectedDrive = getSelectedDrive();
                if (selectedDrive != -1) {
                    drives[selectedDrive].setLock(BitTest.getBit(cpt2, 5));
                    drives[selectedDrive].setDirection(BitTest.getBit(cpt2, 6));
                    drives[selectedDrive].setHead(BitTest.getBit(cpt2, 7));
                }
                // Motorbit gilt für alle Laufwerke
                for (FloppyDrive drive : drives) {
                    drive.setMotor(BitTest.getBit(cpt2, 4));
                }
                break;
            }
        }
    }

    /**
     * Liest Daten von einem lokalen Port.
     *
     * @param port Port
     * @return Gelesenes Byte
     */
    int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_SIO_DATA_A:
                System.out.println("Lesen von SIO-Port A Data noch nicht implementiert");
                return (Integer.reverse(sio.readData(0)) >> 24) & 0xFF;
            case LOCAL_PORT_SIO_DATA_B:
                System.out.println("Lesen von SIO-Port B Data noch nicht implementiert");
                return (Integer.reverse(sio.readData(1)) >> 24) & 0xFF;
            case LOCAL_PORT_SIO_CONTROL_A:
                System.out.println("Lesen von SIO-Port A Control noch nicht implementiert");
                return (Integer.reverse(sio.readControl(0)) >> 24) & 0xFF;
            case LOCAL_PORT_SIO_CONTROL_B:
                System.out.println("Lesen von SIO-Port B Cotrol noch nicht implementiert");
                return (Integer.reverse(sio.readControl(0)) >> 24) & 0xFF;
            case LOCAL_PORT_CPT3_0:
            case LOCAL_PORT_CPT3_1:
            case LOCAL_PORT_CPT3_2:
            case LOCAL_PORT_CPT3_3:
                int result = 0;
                int selectedDrive = getSelectedDrive();
                if (selectedDrive != -1) {
                    result |= (!drives[selectedDrive].isTrack0()) ? 0x01 : 0x00;
                    // TODO: Wie Index sinnvoll implementieren
                    result |= (!drives[selectedDrive].isIndex()) ? 0x02 : 0x00;
                    result |= (!drives[selectedDrive].isWriteProtect()) ? 0x04 : 0x00;
                    // TODO: TS
                    //result |= ()?0x20:0x00;
                    result |= (!drives[selectedDrive].isDiskInsert()) ? 0x40 : 0x00;
//                    System.out.println("Gewähltes Laufwerk: " + selectedDrive);
                }
                result |= (!sio.isDTR(1)) ? 0x08 : 0x00;
                // TODO: KLE konfigurierbar gestalten
                result |= 0x10;//:0x00;
                // TODO: ME
                // Wechselt momentan zwischen den Durchgängen
                me = !me;
                result |= (me) ? 0x80 : 0x00;
                //System.out.println(String.format("Lese CPT3: %02X", result));
                return result;
            case LOCAL_PORT_CPT1_0:
            case LOCAL_PORT_CPT1_1:
            case LOCAL_PORT_CPT1_2:
            case LOCAL_PORT_CPT1_3:
                throw new IllegalArgumentException("Lesen von CPT1 Port nicht erlaubt");
            case LOCAL_PORT_CPT2_0:
            case LOCAL_PORT_CPT2_1:
            case LOCAL_PORT_CPT2_2:
            case LOCAL_PORT_CPT2_3:
                throw new IllegalArgumentException("Lesen von CPT2 Port nicht erlaubt");
        }
        return 0;
    }

    /**
     * Aktualisiert die Systemzeit der AFS.
     *
     * @param cycles Anzahl der Takte
     */
    void updateClock(int cycles) {
        sio.updateClock(cycles);
    }

    /**
     * Schreibt den Zustand der AFS in eine Datei
     *
     * @param dos Stream der Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].saveState(dos);
        }
        eproms.saveMemory(dos);
        sio.saveState(dos);
        dos.writeInt(cpt1);
        dos.writeInt(cpt2);
    }

    /**
     * Liest den Zustand der AFS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].loadState(dis);
        }
        eproms.loadMemory(dis);
        sio.loadState(dis);
        cpt1 = dis.readInt();
        cpt2 = dis.readInt();
    }

    /**
     * Liefert das aktuell ausgewählte Laufwerk anhand des CPT2 Registers
     * zurück.
     *
     * @return <code>-1</code> wenn kein Laufwerk gewählt ist, sonst die Nummer
     * des gewählten Laufwerks <code>0..3</code>
     */
    private int getSelectedDrive() {
        int driveBits = cpt2 & 0x0F;
        return (driveBits == 0x00) ? -1 : ((driveBits == 0x01) ? 0 : ((driveBits == 0x02) ? 1 : ((driveBits == 0x04) ? 2 : 3)));
    }
}
