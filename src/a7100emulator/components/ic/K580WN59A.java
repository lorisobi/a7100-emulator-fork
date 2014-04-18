/*
 * KR580WN59A.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des Interruptcontrollers PIC
 *
 * @author Dirk Bräuer
 */
public class K580WN59A {

    /**
     * Status des PIC
     */
    private int state = 0;
    /**
     * Interrupt-Request-Register
     */
    private int irr = 0;
    /**
     * Interrupt-Service Routine
     */
    private int isr = 0;
    /**
     * Interrupt Mask Register
     */
    private int imr = 0;
    /**
     * Initialization Command Word 1
     */
    private int icw1 = 0;
    /**
     * Initialization Command Word 2
     */
    private int icw2 = 0;
    /**
     * Initialization Command Word 3
     */
    private int icw3 = 0;
    /**
     * Initialization Command Word 4
     */
    private int icw4 = 0;
    /**
     * Maskenregister TODO: Zusammenfassen mit IMR
     */
    private int ocw1 = 0;
    /**
     * Operation Command Word 2
     */
    private int ocw2 = 0;
    /**
     * Operation Command Word 3
     */
    private int ocw3 = 0;
    /**
     * Gibt an ob ICW1 empfangen wurde
     */
    private boolean icw1Send = false;
    /**
     * Gibt an ob ICW2 empfangen wurde
     */
    private boolean icw2Send = false;
    /**
     * Gibt an ob ICW3 empfangen wurde
     */
    private boolean icw3Send = false;

    /**
     * Erstellt einen neuen PIC und initialisiert ihn
     */
    public K580WN59A() {
        InterruptSystem.getInstance().setPIC(this);
    }

    /**
     * Gibt den Status des PIC zurück
     *
     * @return Status
     */
    public int readStatus() {
        return state;
    }

    /**
     * Gibt das Maskenregister zurück
     *
     * @return Maskenregister
     */
    public int readOCW() {
        return imr;
    }

    /**
     * Verarbeitet ankommende Daten an Port 1
     *
     * @param data Daten
     */
    public void writePort0(int data) {
        if (getBit(data, 4)) {
            // ICW1
            icw1 = data;
            icw1Send = true;
            // System.out.println("Setze ICW1 " + Integer.toBinaryString(icw1));
        } else {
            if (getBit(data, 3)) {
                // OCW3
                ocw3 = data;
                //   System.out.println("Setze OCW3 " + Integer.toBinaryString(ocw3));
            } else {
                // OCW2
                ocw2 = data;
                // System.out.println("Setze OCW2 " + Integer.toBinaryString(ocw2));
            }
        }
    }

    /**
     * Verarbeitet ankommende Daten an Port 2
     *
     * @param data Daten
     */
    public void writePort1(int data) {
        if (icw1Send) {
            //ICW2
            icw2 = data;
            icw2Send = true;
            icw1Send = false;
            //  System.out.println("Setze ICW2 " + Integer.toBinaryString(icw2));
        } else if (icw2Send && !getBit(icw1, 1)) {
            // ICW3
            icw3 = data;
            icw3Send = true;
            icw2Send = false;
            // System.out.println("Setze ICW3 " + Integer.toBinaryString(icw3));
        } else if (getBit(icw1, 0) && (icw3Send || icw2Send)) {
            // ICW4
            icw4 = data;
            icw2Send = false;
            icw3Send = false;
            //System.out.println("Setze ICW4 " + Integer.toBinaryString(icw4));
        } else {
            // OCW1    
            imr = data;
//            System.out.println("Setze OCW1 " + Integer.toBinaryString(imr));
        }
    }

    /**
     * Nimmt einer Interrupt-Anfrage eines angeschlossenen Gerätes entgegen
     *
     * @param id IRQ
     */
    public void requestInterrupt(int id) {
//        System.out.println("Interrupt Anfrage "+id+" IMR:"+getBit(imr,id));
        if (id < 0 || id > 7) {
            return;
        }
        if (!getBit(imr, id)) {
            irr |= (1 << id);
        }
    }

    /**
     * Liefert den nächsten anstehenden Interrupt-Request
     *
     * @return IRQ oder -1 wenn kein Interrupt vorliegt
     */
    public int getInterrupt() {
        for (int i = 0; i <= 7; i++) {
            if (getBit(irr, i)) {
                irr &= ~(1 << i);
                return i;
            }
        }
        return -1;
    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op1 Operand
     * @param i Zu Prüfendes Bit
     * @return true - wenn Bit gesetzt , false - sonst
     */
    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    /**
     * Speichert den Zustand des PIC in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(state);
        dos.writeInt(irr);
        dos.writeInt(isr);
        dos.writeInt(imr);
        dos.writeInt(icw1);
        dos.writeInt(icw2);
        dos.writeInt(icw3);
        dos.writeInt(icw4);
        dos.writeInt(ocw1);
        dos.writeInt(ocw2);
        dos.writeInt(ocw3);
        dos.writeBoolean(icw1Send);
        dos.writeBoolean(icw2Send);
        dos.writeBoolean(icw3Send);
    }

    /**
     * Lädt den Zustand des PIC aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        state = dis.readInt();
        irr = dis.readInt();
        isr = dis.readInt();
        imr = dis.readInt();
        icw1 = dis.readInt();
        icw2 = dis.readInt();
        icw3 = dis.readInt();
        icw4 = dis.readInt();
        ocw1 = dis.readInt();
        ocw2 = dis.readInt();
        ocw3 = dis.readInt();
        icw1Send = dis.readBoolean();
        icw2Send = dis.readBoolean();
        icw3Send = dis.readBoolean();
    }
}
