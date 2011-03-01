/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components.modules;

/**
 *
 * @author Dirk
 */
public final class ZPS implements Module {

    public static int zps_count=0;

    public ZPS() {
        zps_count++;
        init();
    }

    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
