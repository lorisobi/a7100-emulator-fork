/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components;

import a7100emulator.components.modules.KES;
import a7100emulator.components.modules.KGS;
import a7100emulator.components.modules.OPS;
import a7100emulator.components.modules.ZVE;

/**
 *
 * @author Dirk
 */
public class A7100 {
    
        private ZVE zve=new ZVE();
        //ZPS zps=new ZPS();
        private OPS ops1=new OPS();
        private OPS ops2=new OPS();
        private OPS ops3=new OPS();
        private KGS kgs=new KGS();
        private KES kes=new KES();
    
    public A7100() {
        zve.start();
    }

    /**
     * @return the zve
     */
    public ZVE getZVE() {
        return zve;
    }

    /**
     * @return the ops1
     */
    public OPS getOPS1() {
        return ops1;
    }

    /**
     * @return the ops2
     */
    public OPS getOPS2() {
        return ops2;
    }

    /**
     * @return the kgs
     */
    public KGS getKGS() {
        return kgs;
    }

    /**
     * @return the kes
     */
    public KES getKES() {
        return kes;
    }
}
