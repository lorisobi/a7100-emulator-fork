/*
 * KR580WI53.java
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
 *   03.04.2014 - Kommentare vervollständigt
 *   26.07.2014 - Unterscheidung der Modi
 *   27.07.2014 - Puffer für kleine Aktualisierungszeiten hinzugefügt
 *              - Implementierung Mode 0, 2 und 3
 *              - Interface IC implementiert
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.StateSavable;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des Timer-Schaltkreises PIT
 *
 * @author Dirk Bräuer
 */
public class KR580WI53 implements IC {

    /**
     * Bitmaske zum Prüfen des angesprochenen Timers
     */
    private static final int TEST_COUNTER = 0xC0;
    /**
     * Bitmaske zum Prüfen des Counter Zugriffs (Lesen, Schreiben, Latch)
     */
    private static final int TEST_RW = 0x30;
    /**
     * Bitmaske zum Prüfen des Counter Modus
     */
    private static final int TEST_MODE = 0x0E;
    /**
     * Bitmaske zum Prüfen des Zählertyps
     */
    private static final int TEST_TYPE = 0x01;
    /**
     * Array der drei implementierten Zähler
     */
    private final Counter[] counter = new Counter[3];
    /**
     * Puffer für Aktualisierung des Counters
     */
    private final int[] buffer = new int[3];

    /**
     * Erzeugt einen neuen PIT
     */
    public KR580WI53() {
        for (int i = 0; i < 3; i++) {
            counter[i] = new Counter(i);
        }
    }

    /**
     * Gibt ein Byte an den PIT aus
     *
     * @param control daten
     */
    public void writeInit(int control) {
        int counterID = (control & TEST_COUNTER) >> 6;
        //System.out.print("Initialisiere Counter " + counterID + ": ");
        switch (counterID) {
            case 0:
            case 1:
            case 2:
                counter[counterID].setCounterInit(control);
                break;
            case 3:
                break;
        }
    }

    /**
     * Setzt den Wert des gewählten Zählers
     *
     * @param counterID ID des Zählers
     * @param data Wert
     */
    public void writeCounter(int counterID, int data) {
        //System.out.print("Setze Counter " + i + ": ");
        counter[counterID].setValue(data);
    }

    /**
     * Liest den aktuellen Wert des gewählten Zählers
     *
     * @param counterID ID des Zählers
     * @return Aktueller Zählerwert
     */
    public int readCounter(int counterID) {
        int val = counter[counterID].getValue();
        //System.out.println("Gelesen Counter "+i+": " + Integer.toHexString(val));
        return val;
    }

    /**
     * Verarbeitet die geänderte Systemzeit und erhöht die Werte der Zähler
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        buffer[0] += amount;
        buffer[1] += amount;
        buffer[2] += amount;

        counter[0].update(buffer[0] >> 2);
        counter[1].update(buffer[1] >> 5);
        counter[2].update(buffer[2] >> 2);

        buffer[0] -= (buffer[0] >> 2) << 2;
        buffer[1] -= (buffer[1] >> 5) << 5;
        buffer[2] -= (buffer[2] >> 2) << 2;

        for (int i = 0; i < 3; i++) {
            if (buffer[i] < 0) {
                buffer[i] = 0;
            }
        }
    }

    /**
     * Speichert den Zustand des PIT in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreiche war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].saveState(dos);
            dos.writeInt(buffer[i]);
        }
    }

    /**
     * Liest den Zustand des PIT aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].loadState(dis);
            buffer[i] = dis.readInt();
        }
    }

    /**
     * Klasse zur Realisierung eines Zählers
     */
    private class Counter implements StateSavable {

        /**
         * Nummer des Zählers
         */
        private final int id;
        /**
         * Gibt an ob der Zähler gegenwärtig läuft
         */
        private boolean running = false;
        /**
         * Gibt an ob der Counter-Latch Modus aktiviert ist (Wert speichern für
         * Auslesen)
         */
        private boolean latched = false;
        /**
         * Startwert des Zählers
         */
        private int initialValue = 0;
        /**
         * Aktueller Wert des Counters
         */
        private int value = 0;
        /**
         * Modus
         */
        private int mode;
        /**
         * Counterart
         */
        private int type;
        /**
         * Zugriffsmodus (Lesen, Schreiben, Latch)
         */
        private int rw;
        /**
         * Status des Lesens/Schreibens, wird für sequentiellen Zugriff auf
         * beide Bytes des Zählers verwendet
         */
        private int readWriteState = 0;
        /**
         * Zwischenspeicher nach Latch für späteres Auslesen
         */
        private int latch = 0;
        /**
         * Zustand des Ausgangspins;
         */
        private boolean outp = false;

        /**
         * Erstellt einen neuen Zähler
         *
         * @param id Nummer des Zählers
         */
        private Counter(int id) {
            this.id = id;
        }

        /**
         * Konfiguriert den Zähler
         *
         * @param control Steuerwort
         */
        private void setCounterInit(int control) {
            if (((TEST_RW & control) >> 4) == 0) {
                if (!latched) {
                    latch = value;
                    latched = true;
                    // System.out.println("latched: " + latch);
                }
            } else {
                mode = (TEST_MODE & control) >> 1;
                rw = (TEST_RW & control) >> 4;
                type = TEST_TYPE & control;
                switch (mode) {
                    case 0:
                        // Mode 0
                        outp = false;
                        break;
                    case 1:
                        // Mode 1
                        // TODO: Modus 1 noch nicht Implementiert
                        break;
                    case 2:
                    case 6:
                    case 3:
                    case 7:
                        // Mode 2,3
                        outp = true;
                        break;
                    case 4:
                        // Mode 4
                        // TODO: Modus 4 noch nicht implementiert
                        break;
                    case 5:
                        // Mode 5
                        // TODO: Modus 5 noch nicht implementiert
                        break;
                }
            }
            //System.out.println("ID:" + id + " MODE:" + mode + " RW:" + rw + " Type:" + type);
        }

        /**
         * Setzt den Wert des Zählers
         *
         * @param val Neuer Wert (bzw. ein Byte des Wertes)
         */
        private void setValue(int val) {
            switch (rw) {
                case 0:
                    break;
                case 1: // bits 0..7
                    value |= val & 0xFF;
                    running = true;
                    break;
                case 2: // bits 8..15
                    value |= (val & 0xFF) << 8;
                    running = true;
                    break;
                case 3:
              //      System.out.println("Counter " + id + " RW-State:" + readWriteState + " Wert:" + val);
                    if (readWriteState == 0) {
                        value = val & 0xFF;
                        readWriteState++;
                        running = false;
                    } else {
                        value |= (val & 0xFF) << 8;
                        readWriteState = 0;
                        if (value == 0) {
                            value = 65536;
                        }
                        running = true;
                    }
                    break;
            }
            initialValue = value;
            //System.out.println("Counter " + id + " - Neuer Wert:" + value);
        }

        /**
         * Liest den Wert des Zählers
         *
         * @return aktueller Wert (bzw. ein Byte des Wertes)
         */
        private int getValue() {
            switch (rw) {
                case 0:
                    //return latch;
                    //return 0;
                    throw new IllegalStateException("Wrong Read/Write Mode in Counter");
                //break;
                case 1: // bits 0..7
                    if (latched) {
                        latched = false;
                        return latch & 0xFF;
                    } else {
                        return value & 0xFF;
                    }
                case 2: // bits 8..15
                    if (latched) {
                        latched = false;
                        return (latch >> 8) & 0xFF;
                    } else {
                        return (value >> 8) & 0xFF;
                    }
                case 3:
                    if (readWriteState == 0) {
                        readWriteState++;
                        if (latched) {
                            return latch & 0xFF;
                        } else {
                            return value & 0xFF;
                        }
                    } else {
                        readWriteState = 0;
                        if (latched) {
                            latched = false;
                            return (latch >> 8) & 0xFF;
                        } else {
                            return (value >> 8) & 0xFF;
                        }
                    }
                default:
                    throw new IllegalStateException("Wrong Read/Write Mode in Counter");
            }
        }

        /**
         * Verarbeitet Änderungen der Systemzeit und setzt die entsprechenden
         * Zählerwerte, für Counter 0 wird ggf. ein Interrupt ausgelöst
         *
         * @param amount Anzahl der Ticks
         */
        private void update(long amount) {
            if (running) {
                boolean old_outp = outp;
                value -= amount;
                switch (mode) {
                    case 0:
                        // Mode 0
                        if (value <= 0) {
                            value = 0;
                            outp = true;
                        }
                        break;
                    case 1:
                        // Mode 1
                        // TODO: Modus 1 noch nicht implementiert
                        break;
                    case 2:
                    case 6:
                        // Mode 2/6
                        // TODO: Modus 2/6 noch nicht implementiert
                        break;
                    case 3:
                    case 7:
                        // Mode 3
                        if (value <= (initialValue >> 2)) {
                            outp = false;
                        }
                        if (value <= 0) {
                            value += initialValue;
                            outp=true;
                            //running=false;
                        }
                        break;
                    case 4:
                        // Mode 4
                        // TODO: Modus 4 noch nicht implementiert
                        break;
                    case 5:
                        // Mode 5
                        // TODO: Modus 5 noch nciht implementiert
                        break;
                }

                // Level von LOW auf HIGH am Ausgang -> Bei Zähler 0 Interrupt
                if (id == 0 && outp && !old_outp) {
                    MMS16Bus.getInstance().requestInterrupt(2);
                }

            }
        }

        /**
         * Speichert den Zustand des Zählers in einer Datei
         *
         * @param dos Stream zur Datei
         * @throws IOException Wenn Schreiben nicht erfolgreich war
         */
        @Override
        public void saveState(final DataOutputStream dos) throws IOException {
            dos.writeBoolean(running);
            dos.writeBoolean(latched);
            dos.writeInt(value);
            dos.writeInt(initialValue);
            dos.writeInt(mode);
            dos.writeInt(type);
            dos.writeInt(rw);
            dos.writeInt(readWriteState);
            dos.writeInt(latch);
        }

        /**
         * Lädt den Zustand des Zählers aus einer Datei
         *
         * @param dis Stream zur Datei
         * @throws IOException Wenn Laden nicht erfolgreich war
         */
        @Override
        public void loadState(final DataInputStream dis) throws IOException {
            running = dis.readBoolean();
            latched = dis.readBoolean();
            value = dis.readInt();
            initialValue = dis.readInt();
            mode = dis.readInt();
            type = dis.readInt();
            rw = dis.readInt();
            readWriteState = dis.readInt();
            latch = dis.readInt();
        }
    }
}
