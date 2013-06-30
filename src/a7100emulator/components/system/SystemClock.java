/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.ClockModule;
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

    public static SystemClock getInstance() {
        if (instance == null) {
            instance = new SystemClock();
        }
        return instance;
    }

    public void updateClock(int amount) {
        updateModules(amount);
        clock += amount;
    }

    public long getClock() {
        return clock;
    }

    public void registerClock(ClockModule module) {
        clockModules.add(module);
    }

    private void updateModules(int amount) {
        for (ClockModule module : clockModules) {
            module.clockUpdate(amount);
        }
    }
}
