/*
 * UA856.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   12.08.2014 - Erstellt
 *   28.09.2014 - Lesen von Diagnosestatus hinzugefügt
 *
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Klasse zur Realisierung des U856 SIO
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class UA856 {

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
        System.out.println("Lese SIO:" + result);
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
        if (isDiagnose()) {
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
     * Gibt an, ob sich der SIO (der KGS) im Diagnosemodus befindet
     *
     * @return true wenn im Diagnosemodus, false - sonst
     */
    public boolean isDiagnose() {
        return !BitTest.getBit(channels[1].writeRegister[5], 1);
    }

    /**
     * Gibt an, ob sich der SIO (der KGS) im Lokalen Modus befindet
     *
     * @return true wenn im lokalen Modus, false - sonst
     */
    public boolean isLocalROM() {
        return BitTest.getBit(channels[1].writeRegister[5], 7);
    }

    /**
     * Speichert den Zustand des SIO in einer Datei
     * <p>
     * TODO: Noch nicht implementiert
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Liest den Zustand des SIO aus einer Datei
     * <p>
     * TODO: Noch nicht implementiert
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    class SioChannel {

        /**
         * Puffer der empfangenen Bytes
         */
        private LinkedList<Integer> receiveData = new LinkedList<Integer>();
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
                    // TODO
                    return 0x04;
                case 1:
                    return 0;
                case 2:
                    return 0;
                default:
                    throw new IllegalStateException("Ungültiger Register Zeiger");
            }
        }
    }
}
