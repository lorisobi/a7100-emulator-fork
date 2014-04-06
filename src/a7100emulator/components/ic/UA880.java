/*
 * UA880.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *
 */
package a7100emulator.components.ic;

/**
 * Klasse zur Realisierung eines UA880 Prozessors für Subsysteme
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 * 
 * @author Dirk Bräuer
 */
public class UA880 implements Runnable {

    /**
     * Hauptregister
     */
    private int a;
    private int b;
    private int d;
    private int h;
    private int f;
    private int c;
    private int e;
    private int l;
    /**
     * Tauschregister
     */
    private int a_;
    private int b_;
    private int d_;
    private int h_;
    private int f_;
    private int c_;
    private int e_;
    private int l_;
    /**
     * Interrupt-Register
     */
    private int i;
    /**
     * Refreshregister
     */
    private int r;
    /**
     * Indexregister 1
     */
    private int ix;
    /**
     * Indexregister 2
     */
    private int iy;
    /**
     * Stapelzeiger
     */
    private int sp;
    /**
     * Programmzähler
     */
    private int pc;

    private static final int NOP = 0x00;
    private static final int LD_IMM_BC = 0x01;
    private static final int LD_A_BC = 0x02;
    private static final int INC_BC=0x03;
    private static final int INC_B=0x04;
    private static final int DEC_B=0x05;
    private static final int LD_IMM_B=0x06;
    private static final int RLCA=0x07;
    private static final int EX_AF_AF_=0x08;
    private static final int ADD_HL_BC=0x09;
    private static final int LD_BC_A=0x0A;
    private static final int DEC_BC=0x0B;
    private static final int INC_C=0x0C;
    private static final int DEC_C=0x0D;
    private static final int LD_IMM_C=0x0E;
    private static final int RRCA=0x0F;
    
    private static final int DJNZ_IMM=0x10;
    private static final int LD_IMM_DE=0x11;
    private static final int LD_A_DE=0x12;
    private static final int INC_DE=0x13;
    private static final int INC_D=0x14;
    private static final int DEC_D=0x15;
    private static final int LD_IMM_D=0x16;
    private static final int RLA=0x17;
    private static final int JR_IMM=0x18;
    private static final int ADD_DE_HL=0x19;
    private static final int LD_DE_A=0x1A;
    private static final int DEC_DE=0x1B;
    private static final int INC_E=0x1C;
    private static final int DEC_E=0x1D;
    private static final int LD_IMM_E=0x1E;
    private static final int RRA=0x1F;
    
    

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
