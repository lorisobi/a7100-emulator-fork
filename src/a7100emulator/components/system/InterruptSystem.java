/*
 * InterruptSystem.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import a7100emulator.components.ic.K580WN59A;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Singleton-Klasse zur Realisierung des Interrupt-Systems
 *
 * @author Dirk Bräuer
 */
public class InterruptSystem {

    /**
     * Gibt an ob der Parity NMI aktiv ist
     */
    private boolean parityNMIEnable = true;
    /**
     * Instanz des Interrupt-Systems
     */
    private static InterruptSystem instance;
    /**
     * Gibt an ob ein NMI aufgetreten ist
     */
    private boolean nmi = false;
    /**
     * Referenz auf Interrupt Controller
     */
    private K580WN59A pic;

    /**
     * Erstellt ein neues Interrupt-System
     */
    private InterruptSystem() {
    }

    /**
     * Gibt die Instanz des Interrupt-Systems zurück
     *
     * @return Instanz
     */
    public static InterruptSystem getInstance() {
        if (instance == null) {
            instance = new InterruptSystem();
        }
        return instance;
    }

    /**
     * Setzt den Interruptcontroller PIC
     *
     * @param pic PIC
     */
    public void setPIC(K580WN59A pic) {
        this.pic = pic;
    }

    /**
     * Liefert den Interruptcontroller zurück
     *
     * @return Interruptcontroller
     */
    public K580WN59A getPIC() {
        return pic;
    }

    /**
     * Gibt an ob ein NMI aufgetreten ist und schaltet diesen ggf. ab
     *
     * @return true - wenn NMI aufgetreten , false - sonst
     */
    public boolean getNMI() {
        if (nmi) {
            nmi = false;
            return true;
        }
        return false;
    }

    /**
     * Setzt den Parity-NMI
     */
    public void addParityNMI() {
        if (parityNMIEnable) {
            nmi = true;
        }
    }

    /**
     * Aktiviert den Empfang von Parity-NMI
     */
    public void enableParityNMI() {
        parityNMIEnable = true;
    }

    /**
     * Deaktiviert den Empfang von Parity-NMI
     */
    public void disableParityNMI() {
        parityNMIEnable = false;
    }

    /**
     * Speichert den Zustand des Interrupt-Systems in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeBoolean(parityNMIEnable);
        dos.writeBoolean(nmi);
    }

    /**
     * Lädt den Zustand des Interrupt-Systems aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        parityNMIEnable = dis.readBoolean();
        nmi = dis.readBoolean();
    }

    /**
     * Setzt das Interrupt-System in den Grundzustand zurück
     */
    public void reset() {
        parityNMIEnable = false;
        nmi = false;
    }
}
