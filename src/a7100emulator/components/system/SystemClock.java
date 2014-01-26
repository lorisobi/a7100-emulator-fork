/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.ClockModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author Dirk
 */
public class SystemClock {

    private LinkedList<ClockModule> clockModules = new LinkedList<ClockModule>();
    private long clock = 0;
    private static SystemClock instance;

    private SystemClock() {
    }

    /**
     * 
     * @return
     */
    public static SystemClock getInstance() {
        if (instance == null) {
            instance = new SystemClock();
        }
        return instance;
    }

    /**
     * 
     * @param amount
     */
    public void updateClock(int amount) {
        updateModules(amount);
        clock += amount;
    }

    /**
     * 
     * @return
     */
    public long getClock() {
        return clock;
    }

    /**
     * 
     * @param module
     */
    public void registerClock(ClockModule module) {
        clockModules.add(module);
    }

    private void updateModules(int amount) {
        for (ClockModule module : clockModules) {
            module.clockUpdate(amount);
        }
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeLong(clock);
    }

    /**
     * 
     * @param dis
     * @throws IOException
     */
    public void loadState(DataInputStream dis) throws IOException {
        clock=dis.readLong();
    }

    /**
     * 
     */
    public void reset() {
        clock=0;
    }
}
