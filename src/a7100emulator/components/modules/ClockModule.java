/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components.modules;

/**
 *
 * @author Dirk
 */
public interface ClockModule extends Module {

    void registerClocks();

    void clockUpdate(int amount);

}
