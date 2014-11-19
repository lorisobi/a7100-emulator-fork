/*
 * UA857.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   12.08.2014 - Erstellt
 *   18.11.2014 - Speichern und Laden implementiert
 *              - Interface IC implementiert
 *
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.StateSavable;
import a7100emulator.components.modules.KGS;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des U857 CTC
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class UA857 {

    /**
     * Zähler
     */
    private final Counter[] counter = new Counter[4];
    /**
     * Interrupt Vektor
     */
    private int interruptVector;
    /**
     * Zeiger auf KGS TODO: Referenz ersetzen oder verallgemeinern
     */
    private KGS kgs;

    /**
     * Erzeugt einen neuen CTC
     *
     * @param kgs Referenz zur KGS
     */
    public UA857(KGS kgs) {
        this.kgs = kgs;
        for (int i = 0; i < 4; i++) {
            counter[i] = new Counter(i);
        }
    }

    /**
     * Gibt ein Byte auf einem Kanal aus
     *
     * @param channel Kanal
     * @param data Daten
     */
    public void writeChannel(int channel, int data) {
        //System.out.println("Channel " + channel + " Data " + Integer.toBinaryString(data));
        if ((channel == 0) && !counter[0].timeConstantFollowing && !BitTest.getBit(data, 0)) {
            interruptVector = data;
        } else {
            counter[channel].setControlWord(data);
        }
    }

    /**
     * Liest Daten von einem Kanal
     *
     * @param channel Kanal
     * @return Daten
     */
    public int readChannel(int channel) {
        return counter[channel].readValue();
    }

    /**
     * Aktualisiert die Zähler basierend auf den Änderungen des Taktes.
     *
     * @param amount Anzahl der Taktzyklen
     */
    public void updateClock(int amount) {
        for (Counter cnt : counter) {
            cnt.updateClock(amount);
        }
    }

    /**
     * Speichert den Zustand des CTC in einer Datei
     * <p>
     * TODO: noch nicht implementiert
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(interruptVector);
        for (Counter cnt : counter) {
            cnt.saveState(dos);
        }
    }

    /**
     * Liest den Zustand des CTC aus einer Datei
     * <p>
     * TODO: Noch nicht implementiert
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        interruptVector = dis.readInt();
        for (Counter cnt : counter) {
            cnt.loadState(dis);
        }
    }

    /**
     * Klasse zur Realisierung eines CTC-Zählerkanals
     */
    class Counter implements StateSavable {

        /**
         * ID des Zählers
         */
        private final int id;
        /**
         * Steuerwort
         */
        private int controlWord;
        /**
         * Zeitkonstante
         */
        private int timeConstant;
        /**
         * Puffer für Zähleraktualisierung
         */
        private int buffer;
        /**
         * Aktueller Zählerwert
         */
        private int value;
        /**
         * Gibt an, ob mit dem nächsten Kommando eine Zeitkonstante gesendet
         * wird
         */
        private boolean timeConstantFollowing = false;
        /**
         * Gibt an, ob der Zähler läuft
         */
        private boolean running = false;
        /**
         * Gibt an, ob ein Interrupt des Zählers aussteht
         */
        private boolean interruptPending = false;

        /**
         * Erzeugt einen neuen Counter
         */
        public Counter(int id) {
            this.id = id;
        }

        /**
         * Schreibt ein Control Word in den Counter
         *
         * @param data Control Word
         */
        public void setControlWord(int data) {
            if (timeConstantFollowing) {
                timeConstant = data;
                value = data;
                //System.out.println("Time Constant " + id + ": " + Integer.toBinaryString(data));
                timeConstantFollowing = false;
                if (!BitTest.getBit(controlWord, 3)) {
                    running = true;
                }
            } else {
                controlWord = data;
                //System.out.println("Control Word " + id + ": " + Integer.toBinaryString(data));
                if (BitTest.getBit(controlWord, 2)) {
                    timeConstantFollowing = true;
                }
            }
        }

        /**
         * Liest den Wert des Counters
         *
         * @return
         */
        public int readValue() {
            return value;
        }

        /**
         * Aktualisiert die Zähler auf Basis des Taktgebers
         *
         * @param amount Anzahl der Ticks
         */
        private void updateClock(int amount) {
            // TODO: Counter-Modus, weiterzählen
            if (running) {
                buffer += amount;
                int prescaler = BitTest.getBit(controlWord, 5) ? 8 : 4;
                value -= buffer >> prescaler;
                buffer -= (buffer >> prescaler) << prescaler;
                if (buffer < 0) {
                    buffer = 0;
                }
                if (value <= 0) {
                    value = timeConstant;
                    //System.out.println("Zähler " + id + " 0");
                    if (BitTest.getBit(controlWord, 7)) {
                        // Interrupt
                        kgs.requestInterrupt((interruptVector & 0xF8) | (id << 1));
                        running = false;
                    }
                }
            }
        }

        /**
         * Speichert den Zustand des Zählers in einer Datei
         *
         * @param dos Stream zur Datei
         * @throws IOException Wenn Speichern fehlgeschlagen
         */
        @Override
        public void saveState(DataOutputStream dos) throws IOException {
            dos.writeInt(controlWord);
            dos.writeInt(timeConstant);
            dos.writeInt(buffer);
            dos.writeInt(value);
            dos.writeBoolean(timeConstantFollowing);
            dos.writeBoolean(running);
            dos.writeBoolean(interruptPending);
        }

        /**
         * Lädt den Zustand des Zählers aus einer Datei
         *
         * @param dis Stream zur Datei
         * @throws IOException Wenn Laden fehlgeschlagen
         */
        @Override
        public void loadState(DataInputStream dis) throws IOException {
            controlWord = dis.readInt();
            timeConstant = dis.readInt();
            buffer = dis.readInt();
            value = dis.readInt();
            timeConstantFollowing = dis.readBoolean();
            running = dis.readBoolean();
            interruptPending = dis.readBoolean();
        }
    }
}
