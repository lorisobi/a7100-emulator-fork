/*
 * A7100.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   01.04.2014 Kommentare vervollständigt
 *   17.11.2014 Starten der Systemzeit implementiert
 *
 */
package a7100emulator.components;

import a7100emulator.components.modules.*;
import a7100emulator.components.system.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A7100 Hauptklasse
 *
 * @author Dirk Bräuer
 */
public class A7100 {

    /**
     * ZVE-Modul
     */
    private ZVE zve = new ZVE();
    /**
     * ZPS-Modul
     */
    private ZPS zps = null;
    /**
     * 1. OPS-Modul
     */
    private OPS ops1 = new OPS();
    /**
     * 2. OPS-Modul
     */
    private OPS ops2 = new OPS();
    /**
     * 3. OPS-Modul
     */
    private OPS ops3 = new OPS();
    /**
     * KGS-Modul
     */
   // private KGS kgs = new KGS();
    /**
     * KGS-Modul
     */
    private KGS kgs = new KGS();
    /**
     * KES-Modul
     */
    private KES kes = new KES();
    /**
     * ASP-Modul
     */
    private ASP asp = null;

    /**
     * Erstellt einen neuen virtuellen A7100 und startet ihn
     */
    public A7100() {
        startGlobalClock();
    }

    /**
     * Startet die Systemzeit
     */
    private void startGlobalClock() {
        Thread clockThread;
        clockThread = new Thread(GlobalClock.getInstance(), "Global Clock");
        clockThread.start();
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
     * Speichert den aktuellen Zustand des Emulators in der Datei
     * "./state/state.a7100" Dabei werden die saveState Methoden der Module
     * sowie der verwendeten Peripherie aufgerufen.
     */
    public void saveState() {
        zve.pause();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("./state/state.a7100"));

            // TODO: Speichern der Module ZPS, ASP, KGS
            
            zve.saveState(dos);
            ops1.saveState(dos);
            ops2.saveState(dos);
            ops3.saveState(dos);
            //kgs.saveState(dos);
            kes.saveState(dos);

            InterruptSystem.getInstance().saveState(dos);
            Keyboard.getInstance().saveState(dos);
            MMS16Bus.getInstance().saveState(dos);

            dos.flush();
            dos.close();
        } catch (IOException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        zve.resume();
    }

    /**
     * Lädt den aktuellen Zustand des Emulators aus der Datei
     * "./state/state.a7100" Dabei werden die loadState Methoden der Module
     * sowie der verwendeten Peripherie aufgerufen.
     */
    public void loadState() {
        zve.pause();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("./state/state.a7100"));

            // TODO: Laden der Module ZPS, ASP, KGS
            
            zve.loadState(dis);
            ops1.loadState(dis);
            ops2.loadState(dis);
            ops3.loadState(dis);
            //kgs.loadState(dis);
            kes.loadState(dis);

            InterruptSystem.getInstance().loadState(dis);
            Keyboard.getInstance().loadState(dis);
            MMS16Bus.getInstance().loadState(dis);

            dis.close();
        } catch (IOException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        zve.resume();
    }

    /**
     * Setzt den Zustand des Emulators auf die Starteinstellung zurück. Dabei
     * werden die reset Funktionen der Module sowie der Peripherie aufgerufen.
     */
    public void reset() {
        zve.stopCPU();

        MMS16Bus.getInstance().reset();;
        InterruptSystem.getInstance().reset();
        Keyboard.getInstance().reset();

        OPS.ops_count = 0;
        KES.kes_count = 0;
        ASP.asp_count = 0;

        zve = new ZVE();
        zps = null;
        ops1 = new OPS();
        ops2 = new OPS();
        ops3 = new OPS();
        //kgs = new KGS();
        kes = new KES();
        asp = null;

        zve.start();
    }

    /**
     * Pausiert den A7100
     */
    public void pause() {
        GlobalClock.getInstance().pause();
    }

    /**
     * Lässt den A7100 weiterlaufen
     */
    public void resume() {
        synchronized (GlobalClock.getInstance()) {
            GlobalClock.getInstance().resume();
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
}
