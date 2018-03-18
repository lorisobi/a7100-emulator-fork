/*
 * UA856.java
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
 *   12.08.2014 - Erstellt
 *   28.09.2014 - Lesen von Diagnosestatus hinzugefügt
 *   18.11.2014 - Speichern und Laden implementiert
 *              - Interface IC implementiert
 *   01.12.2015 - Doppelte Typdefinition in LinkedList entfernt
 *   24.04.2016 - Abfrage von RTS/DTR implementiert
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung des U856 SIO
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class UA856 implements IC {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(UA856.class.getName());

    /**
     * Serielle Ein-/Ausgabekanäle
     */
    private final SioChannel[] channels = new SioChannel[2];

    /**
     * Erzeugt einen neuen SIO
     */
    public UA856() {
        for (int i = 0; i < 2; i++) {
            channels[i] = new SioChannel();
        }
    }

    /**
     * Liest ein Byte von einem Datenkanal.
     *
     * @param channel Kanal
     * @return Gelesenes Byte
     */
    public int readData(int channel) {
        return channels[channel].getReceiveData();
    }

    /**
     * Liest ein Steuerbyte von einem Kanal.
     *
     * @param channel Kanal
     * @return Steuerbyte
     */
    public int readControl(int channel) {
        // TODO: Implementierung unvollständig
        int result = channels[channel].readControl();
        //System.out.println("Lese SIO:" + result);
        return result;
    }

    /**
     * Schreibt ein Byte auf einem Datenkanal
     *
     * @param channel Kanal
     * @param data Byte
     */
    public void writeData(int channel, int data) {
        // TODO: Versenden
        if (isRTS(1)) {
            channels[channel].receiveData(data);
        }
    }

    /**
     * Schreibt ein Byte auf einem Control-Kanal
     *
     * @param channel Kanal
     * @param data Byte
     */
    public void writeControl(int channel, int data) {
        channels[channel].writeControl(data);
    }

    /**
     * Gibt an, ob das Signal RTS (Request To Send) gesetzt ist. Das Signal ist
     * invertiert: High - RTS inaktiv, Low - RTS aktiv.
     *
     * @param channel SIO-Kanal
     * @return <code>true</code> wenn RTS inaktiv, <code>false</code> wenn RTS
     * aktiv
     */
    public boolean isRTS(int channel) {
        return !BitTest.getBit(channels[channel].writeRegister[5], 1);
    }

    /**
     * Gibt an, ob das Signal DTR (Data Terminal Ready) gesetzt ist. Das Signal
     * ist invertiert: High - DTR inaktiv, Low - DTR aktiv.
     *
     * @param channel SIO-Kanal
     * @return <code>true</code> wenn DTR inaktiv, <code>false</code> wenn DTR
     * aktiv
     */
    public boolean isDTR(int channel) {
        return !BitTest.getBit(channels[channel].writeRegister[5], 7);
    }

    /**
     * Speichert den Zustand des SIO in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        for (SioChannel channel : channels) {
            channel.saveState(dos);
        }
    }

    /**
     * Liest den Zustand des SIO aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        for (SioChannel channel : channels) {
            channel.loadState(dis);
        }
    }

    /**
     * Aktualisiert den SIO auf Basis der Änderungen der Systemzeit
     * <p>
     * TODO: Ggf. löschen
     *
     * @param amount Anzahl der Zyklen
     */
    public void updateClock(int amount) {
    }

    /**
     * Klasse zur Realisierung eines SIO-Kanals
     */
    class SioChannel implements StateSavable {

        /**
         * Puffer der empfangenen Bytes
         */
        private final LinkedList<Integer> receiveData = new LinkedList<>();
        /**
         * Datenbyte Transmit
         */
        private int outputData;
        /**
         * Datenbyte Receive
         */
        private int inputData;
        /**
         * Registersatz
         */
        private int[] writeRegister = new int[7];
        /**
         * Zeiger für nächsten Registerzugriff
         */
        private int registerPointer = 0;

        /**
         * Schreibt ein Control-Byte für den Kanal
         *
         * @param data Control-Byte
         */
        private void writeControl(int data) {
            writeRegister[registerPointer] = data & 0xFF;
            //System.out.println("Setze Register WR" + registerPointer + ":" + Integer.toBinaryString(data));
            if (registerPointer == 0) {
                registerPointer = data & 0x07;
            } else {
                registerPointer = 0;
            }
        }

        /**
         * Empfängt ein Byte und speichert es im Empfangspuffer
         *
         * @param data Empfangenes Byte
         */
        private void receiveData(int data) {
            receiveData.offerLast(data);
        }

        /**
         * Gibt das erste empfangene Byte aus dem Puffer zurück.
         *
         * @return Gelesenes byte
         */
        private int getReceiveData() {
            if (receiveData.isEmpty()) {
                return 0;
            } else {
                return receiveData.pollFirst();
            }
        }

        /**
         * Überträgt ein Byte an das angeschlossene Gerät.
         * <p>
         * TODO: Noch nicht implementiert
         *
         * @param data Daten
         */
        private void transmitData(int data) {
            LOG.log(Level.WARNING, "Transmit Data im SIO UA856 noch nicht implementiert!");
        }

        /**
         * Liest das Steuerbyte von einem Kanal
         * <p>
         * TODO: Noch nicht vollständig implementiert
         *
         * @return Steuerwort
         */
        private int readControl() {
            switch (registerPointer) {
                case 0:
                    // TODO: Lesen noch nicht implementiert
                    LOG.log(Level.WARNING, "Lesen von Controlregister UA856 noch nicht implementiert, Standard-Rückgabewert ist 04h!");
                    return 0x04;
                case 1:
                    return 0;
                case 2:
                    return 0;
                default:
                    throw new IllegalStateException("Ungültiger Register Zeiger");
            }
        }

        /**
         * Speichert den Zustand des SIO-Kanals in einer Datei
         *
         * @param dos Stream zur Datei
         * @throws IOException Wenn Schreiben nicht erfolgreich war
         */
        @Override
        public void saveState(final DataOutputStream dos) throws IOException {
            dos.writeInt(outputData);
            dos.writeInt(inputData);
            for (int i = 0; i < 7; i++) {
                dos.writeInt(writeRegister[i]);
            }
            dos.writeInt(registerPointer);
            dos.writeInt(receiveData.size());
            for (Integer rd : receiveData) {
                dos.writeInt(rd);
            }
        }

        /**
         * Liest den Zustand des SIO-Kanals aus einer Datei
         *
         * @param dis Stream zur Datei
         * @throws IOException Wenn Lesen nicht erfolgreich war
         */
        @Override
        public void loadState(final DataInputStream dis) throws IOException {
            outputData = dis.readInt();
            inputData = dis.readInt();
            for (int i = 0; i < 7; i++) {
                writeRegister[i] = dis.readInt();
            }
            registerPointer = dis.readInt();
            receiveData.clear();
            int sizeRD = dis.readInt();
            for (int rd = 0; rd < sizeRD; rd++) {
                receiveData.add(dis.readInt());
            }
        }
    }
}
