/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components;

import a7100emulator.components.modules.*;
import a7100emulator.components.system.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class A7100 {

    private ZVE zve = new ZVE();
    private ZPS zps = null;
    private OPS ops1 = new OPS();
    private OPS ops2 = new OPS();
   // private OPS ops3 = new OPS();
    private KGS kgs = new KGS();
    private KES kes = new KES();
    private ASP asp = null;

    /**
     * 
     */
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

    /**
     * 
     */
    public void saveState() {
        zve.pause();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("./state/state.a7100"));

            zve.saveState(dos);
            ops1.saveState(dos);
            ops2.saveState(dos);
           // ops3.saveState(dos);
            kgs.saveState(dos);
            kes.saveState(dos);

            InterruptSystem.getInstance().saveState(dos);
            Keyboard.getInstance().saveState(dos);
            SystemClock.getInstance().saveState(dos);

            dos.flush();
            dos.close();
        } catch (IOException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        zve.resume();
    }

    /**
     * 
     */
    public void loadState() {
        zve.pause();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("./state/state.a7100"));

            zve.loadState(dis);
            ops1.loadState(dis);
            ops2.loadState(dis);
          //  ops3.loadState(dis);
            kgs.loadState(dis);
            kes.loadState(dis);

            InterruptSystem.getInstance().loadState(dis);
            Keyboard.getInstance().loadState(dis);
            SystemClock.getInstance().loadState(dis);

            dis.close();
        } catch (IOException ex) {
            Logger.getLogger(A7100.class.getName()).log(Level.SEVERE, null, ex);
        }
        zve.resume();
    }

    /**
     * 
     */
    public void reset() {
        zve.stopCPU();

        SystemMemory.getInstance().reset();
        SystemClock.getInstance().reset();
        SystemPorts.getInstance().reset();
        InterruptSystem.getInstance().reset();
        Keyboard.getInstance().reset();

        OPS.ops_count=0;
        KES.kes_count=0;
        ASP.asp_count=0;
        
        zve = new ZVE();
        zps = null;
        ops1 = new OPS();
        ops2 = new OPS();
        //ops3 = new OPS();
        kgs = new KGS();
        kes = new KES();
        asp = null;


        zve.start();
    }
}
