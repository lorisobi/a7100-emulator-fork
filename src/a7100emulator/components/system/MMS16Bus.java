/*
 * MMS16Bus.java
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
 *   07.08.2014 - Erstellt
 *   16.11.2014 - Clock-Funktionalität entfernt
 *   18.11.2014 - Speichern und Laden implementiert
 *              - Interface StateSavable implementiert
 *   28.07.2015 - Lesen von Wörtern zwischen Modulgrenzen ermöglicht
 *   25.07.2016 - timeout bei reset hinzugefügt
 *   29.07.2016 - IOException beim Speichern des Systemspeichers hinzugefügt
 *   31.07.2016 - Unnötige Imports entfernt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.StateSavable;
import a7100emulator.components.modules.MemoryModule;
import a7100emulator.components.modules.IOModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung des MMS 16 Systembusses und zur Verwaltung der dort
 * vorhandenen Signale.
 *
 * @author Dirk Bräuer
 */
public class MMS16Bus implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(MMS16Bus.class.getName());

    /**
     * Liste mit Modulen, welche Speicher bereitstellen
     */
    private final HashMap<AddressSpace, MemoryModule> memoryModules = new HashMap();
    /**
     * Liste mit Modulen, welche auf den E/A-Adressraum zugreifen
     */
    private final HashMap<Integer, IOModule> ioModules = new HashMap();
    /**
     * Größte mögliche Speicheradresse
     */
    private static final int MAX_ADDRESS = 0xFFFFF;
    /**
     * Timeout
     */
    private boolean timeout = false;
    /**
     * Singleton Instanz des Systembusses
     */
    private static MMS16Bus instance;

    /**
     * Erstellt einen neuen Systembus
     */
    private MMS16Bus() {
    }

    /**
     * Gibt den Systembus zurück
     *
     * @return Systembus
     */
    public static MMS16Bus getInstance() {
        if (instance == null) {
            instance = new MMS16Bus();
        }
        return instance;
    }

    /**
     * Registriert ein Modul mit Arbeitsspeicher auf dem Systembus
     *
     * @param addressSpace Adressraum
     * @param module Modul
     */
    public void registerMemoryModule(AddressSpace addressSpace, MemoryModule module) {
        memoryModules.put(addressSpace, module);
    }

    /**
     * Registriert ein Modul für die Kommunikation über einen E/A-Port
     *
     * @param port Port
     * @param module Modul
     */
    public void registerIOPort(IOModule module, int port) {
        ioModules.put(port, module);
    }

    /**
     * Fordert einen Interrupt auf dem Systembus an
     *
     * @param number Interruptsignal /INTx
     */
    public void requestInterrupt(int number) {
        if (number == 0) {
            InterruptSystem.getInstance().addParityNMI();
        } else {
            InterruptSystem.getInstance().getPIC().requestInterrupt(number);
        }
    }

    /**
     * Liefert ein Modul für den angegebenen Speicherbereich
     *
     * @param address Adresse
     * @param word <code>true</code> für Wortzugriff, <code>false</code> für
     * Bytezugriff
     * @return Modul oder null, wenn kein Modul für den gesamten Speicherbereich
     * vorhanden ist
     */
    private MemoryModule getModuleForAddress(int address, boolean word) {
        for (AddressSpace addressSpace : memoryModules.keySet()) {
            if (address >= addressSpace.getLowerAddress() && (address + (word ? 1 : 0)) <= addressSpace.getHigherAddress()) {
                return memoryModules.get(addressSpace);
            }
        }
        timeout = true;
        return null;
    }

    /**
     * Schreibt ein Byte in den Systemspeicher
     *
     * @param address Adresse
     * @param data Daten
     */
    public void writeMemoryByte(int address, int data) {
        MemoryModule module = getModuleForAddress(address, false);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address));
            return;
        }
        module.writeByte(address, data);
    }

    /**
     * Schreibt ein Wort in den Systemspeicher
     *
     * @param address Adresse
     * @param data Daten
     */
    public void writeMemoryWord(int address, int data) {
        MemoryModule module = getModuleForAddress(address, true);
        if (module == null) {
            // Ggf. Wort zwischen Modulgrenzen
            // Modul für zweites Byte holen
            module = getModuleForAddress(address + 1, false);
            if (module == null) {
                //System.out.println("Zugriff auf nicht vorhandenen Speicher");
                LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address + 1));
                return;
            }
            module.writeByte(address + 1, data >> 8);

            // Modul für erstes Byte holen
            module = getModuleForAddress(address, false);
            if (module == null) {
                //System.out.println("Zugriff auf nicht vorhandenen Speicher");
                LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address));
                return;
            }
            module.writeByte(address, data);

            // timeout zurücksetzen, da Speicher vorhanden und nur zwischen 2 Modulen
            timeout = false;
            return;
        }
        module.writeWord(address, data);
    }

    /**
     * Liest ein Byte aus dem Systemspeicher
     *
     * @param address Adresse
     * @return Daten
     */
    public int readMemoryByte(int address) {
        MemoryModule module = getModuleForAddress(address, false);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address));
            return 0;
        }
        return module.readByte(address);
    }

    /**
     * Liest ein Wort aus dem Systemspeicher
     *
     * @param address Adresse
     * @return Daten
     */
    public int readMemoryWord(int address) {
        MemoryModule module = getModuleForAddress(address, true);
        if (module == null) {
            // Ggf. Wort zwischen Modulgrenzen
            // Modul für erstes Byte holen
            module = getModuleForAddress(address, false);
            if (module == null) {
                LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address));
                return 0;
            }
            int lb = module.readByte(address);

            // Modul für zweites Byte holen
            module = getModuleForAddress(address + 1, false);
            if (module == null) {
                LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen Speicher an Adresse {0}!", String.format("0x%05X", address + 1));
                return 0;
            }
            int hb = module.readByte(address + 1);

            // timeout zurücksetzen, da Speicher vorhanden und nur zwischen 2 Modulen
            timeout = false;

            return ((hb << 8) | (lb & 0xFF));
        }
        return module.readWord(address);
    }

    /**
     * Schreibt den Inhalt des Speichers in eine Datei.
     *
     * @param filename Dateiname
     * @throws IOException Wenn das Speichern des Speicherinhalts auf dem
     * Datenträger nicht erfolgreich war
     */
    public void dumpSystemMemory(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        for (int i = 0; i <= MAX_ADDRESS; i++) {
            fos.write(readMemoryByte(i));
        }
        fos.close();
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param value Daten
     */
    public void writeIOByte(int port, int value) {
        IOModule module = ioModules.get(port);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen IO-Port {0}!",String.format("0x%02X",port));
            timeout = true;
        } else {
            module.writePortByte(port, 0xFF & value);
        }
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    public int readIOByte(int port) {
        IOModule module = ioModules.get(port);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen IO-Port {0}!",String.format("0x%02X",port));
            timeout = true;
            return 0x00;
        } else {
            return module.readPortByte(port);
        }
    }

    /**
     * Gibt ein Wort auf einem Port aus
     *
     * @param port Port
     * @param value Daten
     */
    public void writeIOWord(int port, int value) {
        IOModule module = ioModules.get(port);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen IO-Port {0}!",String.format("0x%02X",port));
            timeout = true;
        } else {
            module.writePortWord(port, 0xFFFF & value);
        }
    }

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    public int readIOWord(int port) {
        IOModule module = ioModules.get(port);
        if (module == null) {
            LOG.log(Level.FINER, "Zugriff auf nicht vorhandenen IO-Port {0}!",String.format("0x%02X",port));
            timeout = true;
            return 0xFF;
        }
        return module.readPortWord(port);
    }

    /**
     * Setzt die Komponenten auf dem Systembus zurück
     */
    public void reset() {
        memoryModules.clear();
        ioModules.clear();
        timeout = false;
    }

    /**
     * Gibt an, ob bei dem letzten Schreib- oder Lesevorgang ein Timeout
     * aufgetreten ist
     *
     * @return true - bei Timeout, false - sonst
     */
    public boolean isTimeout() {
        return timeout;
    }

    /**
     * Löscht den Timeout
     */
    public void clearTimeout() {
        timeout = false;
    }

    /**
     * Speichert den Zustand des Systembusses in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeBoolean(timeout);
    }

    /**
     * Lädt den Zustand des Systembusses aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        timeout = dis.readBoolean();
    }
}
