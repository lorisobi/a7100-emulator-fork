/*
 * UA857.java
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
 *   12.08.2014 - Erstellt
 *   18.11.2014 - Speichern und Laden implementiert
 *              - Interface IC implementiert
 *   12.12.2014 - Reset implementiert
 *   09.08.2015 - Javadoc korrigiert
 *   05.12.2015 - Zugriffe auf KGS abstrahiert
 *   30.12.2015 - Modul final gesetzt
 *   26.03.2015 - Kommentare überarbeitet
 *              - COUNTER Modus implementiert
 *              - Weiterlaufen sichergestellt
 *              - Von IC abgeleitet
 *   02.02.2016 - Override Annotation hinzugefügt
 *   26.07.2016 - Klasse Counter private gesetzt
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.StateSavable;
import a7100emulator.components.modules.SubsystemModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des U857 CTC.
 *
 * @author Dirk Bräuer
 */
public class UA857 implements IC {

    /**
     * Zähler
     */
    private final Counter[] counter = new Counter[4];
    /**
     * Interrupt Vektor
     */
    private int interruptVector;
    /**
     * Zeiger auf UA880 Modul
     */
    private final SubsystemModule module;

    /**
     * Erzeugt einen neuen CTC und legt die einzelnen Zähler neu an.
     *
     * @param module Referenz zum Modul
     */
    public UA857(SubsystemModule module) {
        this.module = module;
        for (int i = 0; i < 4; i++) {
            counter[i] = new Counter(i);
        }
    }

    /**
     * Gibt ein Byte auf einem Kanal aus.
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
     * Speichert den Zustand des CTC in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(interruptVector);
        for (Counter cnt : counter) {
            cnt.saveState(dos);
        }
    }

    /**
     * Liest den Zustand des CTC aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        interruptVector = dis.readInt();
        for (Counter cnt : counter) {
            cnt.loadState(dis);
        }
    }

    /**
     * Klasse zur Realisierung eines CTC-Zählerkanals
     */
    private class Counter implements StateSavable {

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
         * wird.
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
         * 
         * @param id ID des Counters
         */
        private Counter(int id) {
            this.id = id;
        }

        /**
         * Schreibt ein Control Word in den Counter
         *
         * @param data Control Word
         */
        public void setControlWord(int data) {
            if (timeConstantFollowing) {
                timeConstant = (data == 0) ? 256 : data;
                if (!running) {
                value = data;
                }
                timeConstantFollowing = false;
                if (!BitTest.getBit(controlWord, 3)) {
                    running = true;
                }
//                System.out.println("Time Constant " + id + ": " + timeConstant);
            } else {
                controlWord = data;
//                System.out.print("Control Word " + id + ": " + Integer.toBinaryString(data));
//                System.out.print(" Interrupt: "+BitTest.getBit(controlWord, 7));
//                System.out.print(",Mode: "+(BitTest.getBit(controlWord, 6)?"Timer":"Counter"));
//                System.out.print(",Prescaler: "+(BitTest.getBit(controlWord, 5)?"256":"16"));
//                System.out.print(",Edge: "+(BitTest.getBit(controlWord, 4)?"Rising Edge":"Falling Edge"));
//                System.out.print(",Time Trigger: "+(BitTest.getBit(controlWord, 3)?"Pulse":"Time Constant"));
//                System.out.print(",TimeConstant follows: "+BitTest.getBit(controlWord, 2));
//                System.out.println(",Reset: "+BitTest.getBit(controlWord, 1));
                if (BitTest.getBit(controlWord, 1)) {
                    running = false;
                }
                if (BitTest.getBit(controlWord, 2)) {
                    timeConstantFollowing = true;
                }
            }
        }

        /**
         * Liest den Wert des Counters.
         *
         * @return Zählerwert
         */
        public int readValue() {
            return value;
        }

        /**
         * Aktualisiert die Zähler auf Basis des Taktgebers.
         *
         * @param amount Anzahl der Ticks
         */
        private void updateClock(int amount) {
            if (running) {
                buffer += amount;

                if (BitTest.getBit(controlWord, 6)) {
                    // COUNTER
                    value -= amount;
                    buffer -= amount;
                } else {
                    // TIMER
                int prescaler = BitTest.getBit(controlWord, 5) ? 8 : 4;
                value -= buffer >> prescaler;
                buffer -= (buffer >> prescaler) << prescaler;
                }

                if (value <= 0) {
                    value += timeConstant;
                    if (BitTest.getBit(controlWord, 7)) {
                        // Interrupt
                        module.requestInterrupt((interruptVector & 0xF8) | (id << 1));
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
