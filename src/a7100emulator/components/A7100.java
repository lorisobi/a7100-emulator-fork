/*
 * A7100.java
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
 *   17.11.2014 - Starten der Systemzeit implementiert
 *   05.06.2016 - Verweise auf alten KES entfernt
 *   23.07.2016 - Methoden für Pausieren und Einzelschritt überarbeitet
 *   24.07.2016 - Laden und Speichern des Zustands in beliebige Dateien
 *   29.07.2016 - Exceptions beim Laden und Speichern von Zuständen
 *   09.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 */
package a7100emulator.components;

import a7100emulator.components.modules.ABG;
import a7100emulator.components.modules.ASP;
import a7100emulator.components.modules.KES;
import a7100emulator.components.modules.KGS;
import a7100emulator.components.modules.OPS;
import a7100emulator.components.modules.ZPS;
import a7100emulator.components.modules.ZVE;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A7100 Hauptklasse
 *
 * @author Dirk Bräuer
 */
public class A7100 {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(A7100.class.getName());

    /**
     * ZVE-Modul
     */
    private ZVE zve;
    /**
     * ZPS-Modul
     */
    private ZPS zps;
    /**
     * 1. OPS-Modul
     */
    private OPS ops1;
    /**
     * 2. OPS-Modul
     */
    private OPS ops2;
    /**
     * 3. OPS-Modul
     */
    private OPS ops3;
    /**
     * KGS-Modul
     */
    private KGS kgs;
    /**
     * KES-Modul
     */
    private KES kes;
    /**
     * ASP-Modul
     */
    private ASP asp;

    /**
     * Erstellt einen neuen virtuellen A7100 und startet ihn
     */
    public A7100() {
        initModules();
        startClock();
    }

    /**
     * Startet die Systemzeit
     */
    private void startClock() {
        Thread clock = new Thread(GlobalClock.getInstance(), "Clock");
        clock.start();
    }

    /**
     * Gibt die Referenz auf das ZVE-Modul zurück. Wird für Debug-Ausgaben und
     * Emulator-Funktionen verwendet.
     *
     * @return ZVE-Modul
     */
    public ZVE getZVE() {
        return zve;
    }

    /**
     * Gibt die Referenz auf das KGS-Modul zurück. Wird für Debug-Ausgaben
     * verwendet
     *
     * @return KGS-Modul
     */
    public KGS getKGS() {
        return kgs;
    }

    /**
     * Gitb Referenz auf das KES-Modul zurück. Wird für die Verwaltung der
     * Disketten-Images verwendet.
     *
     * @return KES-Modul
     */
    public KES getKES() {
        return kes;
    }

    /**
     * Speichert den aktuellen Zustand des Emulators in der angegebenen Datei.
     * Dabei werden die saveState Methoden der Module sowie der verwendeten 
     * Peripherie aufgerufen.
     * 
     * @param stateFile Datei zum Speichern des Emulatorzustands
     * @throws java.io.IOException Wenn beim Speichern des Zustands ein Fehler
     * auftritt.
     */
    public void saveState(File stateFile) throws IOException {
        pause();
        try {
            // Warte 100ms um das Anhalten des Systems zu garantieren
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }

        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(stateFile));

            // TODO: Speichern der Module ZPS, ASP
            zve.saveState(dos);
            ops1.saveState(dos);
            ops2.saveState(dos);
            ops3.saveState(dos);
            kgs.saveState(dos);
            kes.saveState(dos);

            InterruptSystem.getInstance().saveState(dos);
            Keyboard.getInstance().saveState(dos);
            MMS16Bus.getInstance().saveState(dos);
            GlobalClock.getInstance().saveState(dos);

            dos.flush();
            dos.close();
        } finally {
            // Weiterlaufen des Emulators sicherstellen aber Exception dennoch nach oben weiterleiten
        resume();
    }
    }

    /**
     * Lädt den aktuellen Zustand des Emulators aus der angegebenen Datei. Dabei
     * werden die loadState Methoden der Module sowie der verwendeten Peripherie
     * aufgerufen.
     * 
     * @param stateFile Datei zum Laden des Emulatorzustands
     * @throws java.io.IOException Wenn beim Laden des Zustands ein Fehler
     * auftritt
     */
    public void loadState(File stateFile) throws IOException {
        pause();
        try {
            // Warte 100ms um das Anhalten des Systems zu garantieren
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }

        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(stateFile));

            // TODO: Laden der Module ZPS, ASP
            zve.loadState(dis);
            ops1.loadState(dis);
            ops2.loadState(dis);
            ops3.loadState(dis);
            kgs.loadState(dis);
            kes.loadState(dis);

            InterruptSystem.getInstance().loadState(dis);
            Keyboard.getInstance().loadState(dis);
            MMS16Bus.getInstance().loadState(dis);
            GlobalClock.getInstance().loadState(dis);

            dis.close();
        } finally {
            // Weiterlaufen des Emulators sicherstellen aber Exception dennoch nach oben weiterleiten
        resume();
    }
    }

    /**
     * Setzt den Zustand des Emulators auf die Starteinstellung zurück. Dabei
     * werden die reset Funktionen der Module sowie der Peripherie aufgerufen.
     */
    public void reset() {
        GlobalClock.getInstance().stop();
        try {
            // Warte 100ms um das Anhalten des Systems zu garantieren
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }

        MMS16Bus.getInstance().reset();
        InterruptSystem.getInstance().reset();
        Keyboard.getInstance().reset();
        GlobalClock.getInstance().reset();

        initModules();
        startClock();
    }

    /**
     * Pausiert den A7100.
     */
    public void pause() {
        GlobalClock.getInstance().setPause(true);
    }

    /**
     * Lässt den A7100 weiterlaufen.
     */
    public void resume() {
        synchronized (GlobalClock.getInstance()) {
            GlobalClock.getInstance().setPause(false);
            GlobalClock.getInstance().notify();
        }
    }

    /**
     * Führt einen einzelnen Zeitschritt durch
     */
    public void singleStep() {
        synchronized (GlobalClock.getInstance()) {
            GlobalClock.getInstance().notify();
        }
    }

    /**
     * Legt die MMS16-Module an und führt ggf. Initialisierungen durch.
     */
    private void initModules() {
        OPS.ops_count = 0;
        KES.kes_count = 0;
        ASP.asp_count = 0;
        ZPS.zps_count = 0;

        zve = new ZVE();
//        zps = new ZPS(zve);
        ops1 = new OPS();
        ops2 = new OPS();
        ops3 = new OPS();
        kgs = new KGS();
        kes = new KES();
        asp = null;
    }

    /**
     * Gibt die Referenz auf die an der KGS angeschlossene ABG zurück.
     *
     * @return ABG
     */
    public ABG getABG() {
        return kgs.getABG();
    }
}
