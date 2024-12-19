/*
 * KR580WN59A.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   25.07.2014 - OCW1 und IMR zusammengefasst
 *              - IRR und OCW1 zurückgesetzt beim Empfang von ICW1
 *   18.11.2014 - getBit durch BitTest.getBit ersetzt
 *              - Interface IC implementiert
 *   09.08.2016 - Logger hinzugefügt
 *   19.12.2024 - Verbesserte IRR und ISR Emulation
 *              - Emulation zum Lesen des Status hinzugefuegt
 *              - Non-Auto-EOI Handling hinzugefuegt
 *              - Workaround fuer wartende Interrupts hinzugefuegt
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.components.system.InterruptSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung des Interruptcontrollers PIC
 *
 * @author Dirk Bräuer
 */
public class K580WN59A implements IC {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(K580WN59A.class.getName());

    /**
     * Status des PIC
     */
    private int state = 0;
    /**
     * Interrupt-Request-Register
     */
    private int irr = 0;
    /**
     * In-Service-Register
     */
    private int isr = 0;
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
     * Maskenregister
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
	 * Workaround: Merkzellen fuer wartende Interrupts
	 * Dies ist formal kein Bestandteil des PIC
	 * TODO: Implementiere wartende Interrupts an der Interrupt-Quelle
     */
	private boolean[] irqPending = new boolean[8];

    /**
     * Erstellt einen neuen PIC und initialisiert ihn
     */
    public K580WN59A() {
        registerController();
    }

    /**
     * Registriert den Controller als System-Interrupt-Controller
     */
    private void registerController() {
        InterruptSystem.getInstance().setPIC(this);
    }

    /**
     * Gibt den Status des PIC zurück
     *
     * @return Status
     */
    public int readStatus() {
		if (BitTest.getBit(ocw3, 1)) {
			if (BitTest.getBit(ocw3, 0)) {
				return isr;
			} else {
				return irr;
			}
		}
		return 0;
    }

    /**
     * Gibt das Maskenregister zurück
     *
     * @return Maskenregister
     */
    public int readOCW() {
        //System.out.println("Lese OCW1/IMR " + Integer.toBinaryString(ocw1));
        return ocw1;
    }

    /**
     * Verarbeitet ankommende Daten an Port 1
     *
     * @param data Daten
     */
    public void writePort0(int data) {
        if (BitTest.getBit(data, 4)) {
            // ICW1
            icw1 = data;
            ocw1 = 0x00;
            irr = 0;
			for (int i = 0; i <= 7; i++) {
				irqPending[i] = false;
			}
            icw1Send = true;
            // System.out.println("Setze ICW1 " + Integer.toBinaryString(icw1));
        } else {
            if (BitTest.getBit(data, 3)) {
                // OCW3
                ocw3 = data;
                //System.out.println("Setze OCW3 " + Integer.toBinaryString(ocw3));
				if (BitTest.getBit(ocw3, 6)) {
					LOG.log(Level.SEVERE, "OCW3: Special mask mode nicht implementiert");
					System.exit(0);
				}
				if (BitTest.getBit(ocw3, 2)) {
					LOG.log(Level.SEVERE, "OCW3: Poll command nicht implementiert");
					System.exit(0);
				}
            } else {
                // OCW2
                ocw2 = data;
                //System.out.println("Setze OCW2 " + Integer.toBinaryString(ocw2));
				// Test auf non-spefific EOI command
				if (!BitTest.getBit(ocw2, 7) && !BitTest.getBit(ocw2, 6) &&
					BitTest.getBit(ocw2, 5)) {
					// Durchlaufe alle ISR Bits, um das hoechste In-Service Level
					// zu bestimmen (0 = highest, 7 = lowest)
					for (int i = 0; i <= 7; i++) {
						if (BitTest.getBit(isr, i)) {
							// setze hoechstes ISR Bit zurueck
							isr &= ~(1 << i);
						}
					}
				} else {
					LOG.log(Level.SEVERE, "OCW2: Nicht implementierte Operation: " + Integer.toBinaryString(ocw2));
					System.exit(0);
				}
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
            icw2 = data & 0xFF;
            icw2Send = true;
            icw1Send = false;
            // System.out.println("Setze ICW2 " + Integer.toBinaryString(icw2));
        } else if (icw2Send && !BitTest.getBit(icw1, 1)) {
            // ICW3
            icw3 = data;
            icw3Send = true;
            icw2Send = false;
            // System.out.println("Setze ICW3 " + Integer.toBinaryString(icw3));
        } else if (BitTest.getBit(icw1, 0) && (icw3Send || icw2Send)) {
            // ICW4
            icw4 = data;
            icw2Send = false;
            icw3Send = false;
            //System.out.println("Setze ICW4 " + Integer.toBinaryString(icw4));
        } else {
            // OCW1    
            ocw1 = data;
            //System.out.println("Setze OCW1/IMR " + Integer.toBinaryString(ocw1));
        }
    }

	/**
	 * Aktualisiert das Interrupt-Request-Register, falls es wartende IRQs gibt
	 */
	private void updateIRR() {
		for (int i = 0; i <= 7; i++) {
			if (irqPending[i]) {
				irr |= (1 << i);
				irqPending[i] = false;
			}
		}
	}

    /**
     * Nimmt einer Interrupt-Anfrage eines angeschlossenen Gerätes entgegen
     *
     * @param id IRQ
     */
    public void requestInterrupt(int id) {
        if (id < 0 || id > 7) {
            throw new IllegalArgumentException("Ungültiger Interrupt " + id);
        }
		irqPending[id] = true;
    }

    /**
     * Liefert den nächsten anstehenden Interrupt-Request
     *
     * @return IRQ oder -1 wenn kein Interrupt vorliegt
     */
    public int getInterrupt() {
		updateIRR();

		// durchlaufe alle IRQ Level: 0 = highest, 7 = lowest
        for (int i = 0; i <= 7; i++) {
			if (!BitTest.getBit(isr, i)) {
				/* IRQ Level i ist derzeit nicht In-Service
				 * Teste, ob ein Interrupt-Request anliegt,
				 * der nicht maskiert ist
				 */
				if (BitTest.getBit(irr, i) && !BitTest.getBit(ocw1, i)) {
					// gueltiger Interrupt-Request, loesche IRR Bit
					irr &= ~(1 << i);
					// teste auf NON-AUTO-EOI
					if (!BitTest.getBit(icw4, 1)) {
						/* setze ISR Bit nur im Falle eines NON-AUTO-EOI,
						 * da es (derzeit) nur in diesem Fall auch wieder
						 * sauber zurueckgesetzt werden kann.
						 * TODO: Implementiere ISR-Handling fuer AUTO-EOI
						 */
						isr |= (1 << i);
					}
					return i | (icw2 & 0xF8);
				}
			} else {
				/* IRQ Level i ist derzeit In-Service
				 * und darf nicht unterbrochen werden.
				 * Niedriger priorisierte IRQs duerfen
                 * hoeher priorisierte nicht unterbrechen,
                 * daher gebe hier -1 zurueck
				 */
				return -1;
			}
        }
        return -1;
    }

    /**
     * Speichert den Zustand des PIC in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(state);
        dos.writeInt(irr);
        dos.writeInt(isr);
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
		for (int i = 0; i <= 7; i++) {
			dos.writeBoolean(irqPending[i]);
		}
    }

    /**
     * Lädt den Zustand des PIC aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        state = dis.readInt();
        irr = dis.readInt();
        isr = dis.readInt();
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
		for (int i = 0; i <= 7; i++) {
			irqPending[i] = dis.readBoolean();
		}
    }
}
