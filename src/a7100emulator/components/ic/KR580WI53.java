/*
 * KR580WI53.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   03.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des Timer-Schaltkreises PIT
 *
 * @author Dirk Bräuer
 */
public class KR580WI53 {

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
        counter[0].update(amount >> 2);
        counter[1].update(amount >> 5);
        counter[2].update(amount >> 2);
    }

    /**
     * Speichert den Zustand des PIT in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreiche war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].saveState(dos);
        }
    }

    /**
     * Liest den Zustand des PIT aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].loadState(dis);
        }
    }

    /**
     * Klasse zur Realisierung eines Zählers
     */
    private class Counter {

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
                    //        System.out.println("latched: " + latch);
                }
            } else {
                mode = (TEST_MODE & control) >> 1;
                rw = (TEST_RW & control) >> 4;
                type = TEST_TYPE & control;
            }
            //System.out.println("MODE:" + mode + " RW:" + rw + " Type:" + type);
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
            //  System.out.println("Counter "+id+" - Neuer Wert:" + value);
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
                value -= amount;
                if (value <= 0) {
                    value = 0;
                    if (value + amount > 0) {
                        running = false;
                        if (id == 0) {
                            InterruptSystem.getInstance().getPIC().requestInterrupt(2);
                        }
                    }
                }
                //if (value>0) System.out.println("Neuer Wert:" + value);
            }
        }

        /**
         * Speichert den Zustand des Zählers in einer Datei
         *
         * @param dos Stream zur Datei
         * @throws IOException Wenn Schreiben nicht erfolgreich war
         */
        private void saveState(DataOutputStream dos) throws IOException {
            dos.writeBoolean(running);
            dos.writeBoolean(latched);
            dos.writeInt(value);
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
        private void loadState(DataInputStream dis) throws IOException {
            running = dis.readBoolean();
            latched = dis.readBoolean();
            value = dis.readInt();
            mode = dis.readInt();
            type = dis.readInt();
            rw = dis.readInt();
            readWriteState = dis.readInt();
            latch = dis.readInt();
        }
    }
}
