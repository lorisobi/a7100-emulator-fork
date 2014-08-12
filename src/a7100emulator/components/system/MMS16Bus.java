/*
 * MMS16Bus.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   07.08.2014 Erstellt
 *
 */
package a7100emulator.components.system;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.components.modules.ClockModule;
import a7100emulator.components.modules.MemoryModule;
import a7100emulator.components.modules.IOModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung des MMS 16 Systembusses und zur Verwaltung der dort
 * vorhandenen Signale
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert sie soll die
 * Singeltons InterruptSystem, SystemClock, SystemMemory und SystemPorts
 * vereinen
 *
 * @author Dirk Bräuer
 */
public class MMS16Bus {

    /**
     * Liste mit Modulen, welche Speicher bereitstellen
     */
    private final HashMap<AddressSpace, MemoryModule> memoryModules = new HashMap();
    /**
     * Liste mit Modulen, welche auf den E/A-Adressraum zugreifen
     */
    private final HashMap<Integer, IOModule> ioModules = new HashMap();
    /**
     * Liste der Module, welche den Systemtakt verwendet
     */
    private final LinkedList<ClockModule> clockModules = new LinkedList<ClockModule>();
    /**
     * Größte mögliche Speicheradresse
     */
    private static final int MAX_ADDRESS = 0xFFFFF;
    /**
     * Taktzähler
     */
    private long clock = 0;
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
     * Registriert ein Modul, auf Änderungen der Systemzeit zu reagieren
     *
     * @param module Modul
     */
    public void registerClockModule(ClockModule module) {
        clockModules.add(module);
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
     * @return Modul oder null, wenn kein Modul für diesen Speicherbereich
     * vorhanden ist
     */
    private MemoryModule getModuleForAddress(int address) {
        for (AddressSpace addressSpace : memoryModules.keySet()) {
            if (address >= addressSpace.getLowerAddress() && address <= addressSpace.getHigherAddress()) {
                return memoryModules.get(addressSpace);
            }
        }
        updateClock(0xC000);
        return null;
    }

    /**
     * Schreibt ein Byte in den Systemspeicher
     *
     * @param address Adresse
     * @param data Daten
     */
    public void writeMemoryByte(int address, int data) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");            
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
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
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
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
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
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
            return 0;
        }
        return module.readWord(address);
    }

    /**
     * Schreibt den Inhalt des Speichers in eine Datei
     *
     * @param filename Dateiname
     */
    public void dumpSystemMemory(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            for (int i = 0; i <= MAX_ADDRESS; i++) {
                fos.write(readMemoryByte(i));
            }
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(MMS16Bus.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            updateClock(0xC000);
        } else {
            module.writePort_Byte(port, 0xFF & value);
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
            updateClock(0xC000);
            return 0x00;
        } else {
            return module.readPort_Byte(port);
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
            updateClock(0xC000);
        } else {
            module.writePort_Word(port, 0xFFFF & value);
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
            updateClock(0xC000);
            return 0xFF;
        }
        return module.readPort_Word(port);
    }

    /**
     * Aktualisiert den Taktgeber und benachrichtigt die registrierten Module
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        updateModules(amount);
        clock += amount;
//        if ((clock - lastSync) > 196600) {
//            lastSync = clock;
//            long time = System.currentTimeMillis();
//            long diff = time - timeSync;
//            timeSync = time;
//            if (diff < 40) {
//                try {
//                    Thread.sleep(40 - diff);
//                } catch (InterruptedException ex) {
//                }
//            }
//        }
    }

    /**
     * Meldet die geänderte Systemzeit an die registrierten Module weiter
     *
     * @param amount Anzahl der Ticks
     */
    private void updateModules(int amount) {
        for (ClockModule module : clockModules) {
            module.clockUpdate(amount);
        }
    }

    /**
     * Speichert den Zustand des Taktgebers in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeLong(clock);
    }

    /**
     * Lädt den Zustand des Systemtaktgebers aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        clock = dis.readLong();
    }

    /**
     * Setzt die Komponenten auf dem Systembus zurück
     */
    public void reset() {
        memoryModules.clear();
        ioModules.clear();
        clock = 0;
        clockModules.clear();
    }

}
