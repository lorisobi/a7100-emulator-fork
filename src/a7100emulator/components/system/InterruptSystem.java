/*
 * InterruptSystem.java
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
 *   05.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - Interface StateSavable implementiert
 *   25.07.2016 - parityNMIEnable=true in reset()
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import a7100emulator.components.ic.K580WN59A;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Singleton-Klasse zur Realisierung des Interrupt-Systems
 *
 * @author Dirk Bräuer
 */
public class InterruptSystem implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(InterruptSystem.class.getName());

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
     * Gibt an ob ein NMI aufgetreten ist und schaltet diesen ggf. ab.
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
    @Override
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
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        parityNMIEnable = dis.readBoolean();
        nmi = dis.readBoolean();
    }

    /**
     * Setzt das Interrupt-System in den Grundzustand zurück
     */
    public void reset() {
        parityNMIEnable = true;
        nmi = false;
    }
}
