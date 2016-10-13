/*
 * OpcodeStatistic.java
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
 *   05.04.2014 - Kommentare vervollständigt
 *   09.08.2016 - Logger hinzugefügt
 *              - IOException bei Speicherfehler ausgeben
 */
package a7100emulator.Debug;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

/**
 * Singleton-Klasse zum Sammeln der Opcode Statistik
 *
 * @author Dirk Bräuer
 */
public class OpcodeStatistic {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(OpcodeStatistic.class.getName());

    /**
     * Instanz
     */
    private static OpcodeStatistic instance;
    /**
     * Array mit Opcodezählern
     */
    private final int[] opCodeStatistic = new int[256];

    /**
     * Erstellt eine neue OpcodeStatistic
     */
    private OpcodeStatistic() {
    }

    /**
     * Gibt die Instanz der OpcodeStatistic zurück
     *
     * @return Instanz
     */
    public static OpcodeStatistic getInstance() {
        if (instance == null) {
            instance = new OpcodeStatistic();
        }
        return instance;
    }

    /**
     * Aktualisiert die Statistik basierend auf neuem Opcode
     *
     * @param code Opcode
     */
    public void addStatistic(int code) {
        opCodeStatistic[code]++;
    }

    /**
     * Speichert die Statistik in der Datei ./debug/OpcodeStatistic.log
     *
     * @throws java.io.IOException Wenn beim Speichern ein Fehler auftritt
     */
    public void dump() throws IOException {
            PrintStream opcodeFile = new PrintStream(new FileOutputStream("./debug/OpcodeStatistic.log"));
            for (int i = 0; i < 256; i++) {
                opcodeFile.println("" + opCodeStatistic[i]);
            }
            opcodeFile.flush();
            opcodeFile.close();
        }
    }
