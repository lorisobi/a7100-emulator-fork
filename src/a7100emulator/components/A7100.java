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
    public A7100() {
        ZVE zve=new ZVE();
        //ZPS zps=new ZPS();
        OPS ops1=new OPS();
        OPS ops2=new OPS();
        //OPS ops3=new OPS();
        KGS kgs=new KGS();
        KES kes=new KES();
        zve.start();
    }
}
