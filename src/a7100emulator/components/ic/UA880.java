/*
 * UA880.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
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
 *   24.04.2014 - Alle 1-Byte Opcodes ergänzt
 *              - Implementierung LD Befehle
 *              - Konstanten und Funktionen für Register erstellt
 *   25.04.2014 - 1-Byte Opcodes fast vollständig implementiert
 *              - CB-Opcodes begonnen
 *   26.04.2014 - CB/DD/FD Opcodes implementiert
 *              - ED Opcodes begonnen
 *   25.05.2014 - ED Opcodes fast vollständig
 *              - DD/FD CB Opcodes fertig
 *   06.08.2014 - Debug-Ausgaben hinzugefügt
 *   12.10.2014 - Taktzyklen ergänzt
 *              - Vearbeitung Interrupts ergänzt
 *              - Fehler in XOR behoben
 *   16.11.2014 - Ticks zählen ergänzt
 *   18.11.2014 - BitTest eingeführt
 *              - Speichern und Laden implemetiert
 *              - Interface IC implementiert
 *   19.11.2014 - Ticks auf double geändert
 *              - Thread Funktionalität vorerst entfernt
 *   04.12.2014 - Abfrage ob debug aktiv ist
 *   08.12.2014 - Fehler Debugausgabe INC, DEC behoben
 *   12.12.2014 - CTC Hack entfernt
 *   14.12.2014 - checkSignFlag für 16 Bit implementiert
 *   25.07.2015 - Slowdown deaktiviert
 *   09.08.2015 - Debug Operanden LDIR ergänzt
 *              - Zero Flag 16 Bit Überprüfung ergänzt
 *              - Javadoc korrigiert
 *   30.11.2015 - KGS durch SubsystemModule abstrahiert
 *   14.02.2016 - kgs in module umbenannt
 *   28.02.2016 - Debugausgabe bei veränderlichen Register korrigiert
 *              - Fehler in JP (HL) behoben
 *   14.03.2016 - Fehler in DD CB - Bitoperationen behoben
 *   23.03.2016 - Fehler Carry Flag in CPI, CPD, CPIR, CPDR behoben
 *              - Debugausgaben ergänzt
 *   28.03.2016 - Verzögerte Interruptfreigabe implementiert
 *   23.07.2016 - Von CPU abgeleitet
 *   24.07.2016 - TICK_RATIO entfernt
 *              - Methoden push(),pop() und executeCPUCycle() private gesetzt
 *   28.07.2016 - Kommentare erweitert
 *              - Decoder ergänzt
 *   08.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 *   14.10.2016 - Fehler Debugausgabe Behoben
 *   16.10.2016 - Abfrage nach RETI hinzugefügt
 */
package a7100emulator.components.ic;

import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.DebuggerInfo;
import a7100emulator.Debug.Decoder;
import a7100emulator.Tools.BitTest;
import a7100emulator.components.modules.KES;
import a7100emulator.components.modules.SubsystemModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung eines UA880 Prozessors für A7100 Subsysteme.
 * <p>
 * TODO: - Prüfen ob 16 Bit checkSignFlag immer verwendet wird, HalfCarryFlag
 * bei 16 Bit implementieren
 *
 * @author Dirk Bräuer
 */
public class UA880 implements CPU {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(UA880.class.getName());

    /**
     * Hauptregistersatz: Akkumulator
     */
    private int a;
    /**
     * Hauptregistersatz: Universalregister b
     */
    private int b;
    /**
     * Hauptregistersatz: Universalregister d
     */
    private int d;
    /**
     * Hauptregistersatz: Universalregister h
     */
    private int h;
    /**
     * Hauptregistersatz: Flagregister
     */
    private int f;
    /**
     * Hauptregistersatz: Universalregister c
     */
    private int c;
    /**
     * Hauptregistersatz: Universalregister e
     */
    private int e;
    /**
     * Hauptregistersatz: Universalregister l
     */
    private int l;

    /**
     * Tauschregistersatz: Akkumulator
     */
    private int a_;
    /**
     * Tauschregistersatz: Universalregister b
     */
    private int b_;
    /**
     * Tauschregistersatz: Universalregister d
     */
    private int d_;
    /**
     * Tauschregistersatz: Universalregister g
     */
    private int h_;
    /**
     * Tauschregistersatz: Flagregister
     */
    private int f_;
    /**
     * Tauschregistersatz: Universalregister c
     */
    private int c_;
    /**
     * Tauschregistersatz: Universalregister e
     */
    private int e_;
    /**
     * Tauschregistersatz: Universalregister l
     */
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
    /**
     * Interruptmaske 1
     */
    private int iff1;
    /**
     * Interruptmaske 2
     */
    private int iff2;

    /**
     * Opcode: Lade Register B aus B
     */
    private static final int LD_B_B = 0x40;
    /**
     * Opcode: Lade Register B aus C
     */
    private static final int LD_C_B = 0x41;
    /**
     * Opcode: Lade Register B aus D
     */
    private static final int LD_D_B = 0x42;
    /**
     * Opcode: Lade Register B aus E
     */
    private static final int LD_E_B = 0x43;
    /**
     * Opcode: Lade Register B aus H
     */
    private static final int LD_H_B = 0x44;
    /**
     * Opcode: Lade Register B aus L
     */
    private static final int LD_L_B = 0x45;
    /**
     * Opcode: Lade Register B aus A
     */
    private static final int LD_A_B = 0x47;
    /**
     * Opcode: Lade Register C aus B
     */
    private static final int LD_B_C = 0x48;
    /**
     * Opcode: Lade Register C aus C
     */
    private static final int LD_C_C = 0x49;
    /**
     * Opcode: Lade Register C aus D
     */
    private static final int LD_D_C = 0x4A;
    /**
     * Opcode: Lade Register C aus E
     */
    private static final int LD_E_C = 0x4B;
    /**
     * Opcode: Lade Register C aus H
     */
    private static final int LD_H_C = 0x4C;
    /**
     * Opcode: Lade Register C aus L
     */
    private static final int LD_L_C = 0x4D;
    /**
     * Opcode: Lade Register C aus A
     */
    private static final int LD_A_C = 0x4F;
    /**
     * Opcode: Lade Register D aus B
     */
    private static final int LD_B_D = 0x50;
    /**
     * Opcode: Lade Register D aus C
     */
    private static final int LD_C_D = 0x51;
    /**
     * Opcode: Lade Register D aus D
     */
    private static final int LD_D_D = 0x52;
    /**
     * Opcode: Lade Register D aus E
     */
    private static final int LD_E_D = 0x53;
    /**
     * Opcode: Lade Register D aus H
     */
    private static final int LD_H_D = 0x54;
    /**
     * Opcode: Lade Register D aus L
     */
    private static final int LD_L_D = 0x55;
    /**
     * Opcode: Lade Register D aus A
     */
    private static final int LD_A_D = 0x57;
    /**
     * Opcode: Lade Register E aus B
     */
    private static final int LD_B_E = 0x58;
    /**
     * Opcode: Lade Register E aus C
     */
    private static final int LD_C_E = 0x59;
    /**
     * Opcode: Lade Register E aus D
     */
    private static final int LD_D_E = 0x5A;
    /**
     * Opcode: Lade Register E aus E
     */
    private static final int LD_E_E = 0x5B;
    /**
     * Opcode: Lade Register E aus H
     */
    private static final int LD_H_E = 0x5C;
    /**
     * Opcode: Lade Register E aus L
     */
    private static final int LD_L_E = 0x5D;
    /**
     * Opcode: Lade Register E aus A
     */
    private static final int LD_A_E = 0x5F;
    /**
     * Opcode: Lade Register H aus B
     */
    private static final int LD_B_H = 0x60;
    /**
     * Opcode: Lade Register H aus C
     */
    private static final int LD_C_H = 0x61;
    /**
     * Opcode: Lade Register H aus D
     */
    private static final int LD_D_H = 0x62;
    /**
     * Opcode: Lade Register H aus E
     */
    private static final int LD_E_H = 0x63;
    /**
     * Opcode: Lade Register H aus H
     */
    private static final int LD_H_H = 0x64;
    /**
     * Opcode: Lade Register H aus L
     */
    private static final int LD_L_H = 0x65;
    /**
     * Opcode: Lade Register H aus A
     */
    private static final int LD_A_H = 0x67;
    /**
     * Opcode: Lade Register L aus B
     */
    private static final int LD_B_L = 0x68;
    /**
     * Opcode: Lade Register L aus C
     */
    private static final int LD_C_L = 0x69;
    /**
     * Opcode: Lade Register L aus D
     */
    private static final int LD_D_L = 0x6A;
    /**
     * Opcode: Lade Register L aus E
     */
    private static final int LD_E_L = 0x6B;
    /**
     * Opcode: Lade Register L aus H
     */
    private static final int LD_H_L = 0x6C;
    /**
     * Opcode: Lade Register L aus L
     */
    private static final int LD_L_L = 0x6D;
    /**
     * Opcode: Lade Register L aus A
     */
    private static final int LD_A_L = 0x6F;
    /**
     * Opcode: Lade Register A aus B
     */
    private static final int LD_B_A = 0x78;
    /**
     * Opcode: Lade Register A aus C
     */
    private static final int LD_C_A = 0x79;
    /**
     * Opcode: Lade Register A aus D
     */
    private static final int LD_D_A = 0x7A;
    /**
     * Opcode: Lade Register A aus E
     */
    private static final int LD_E_A = 0x7B;
    /**
     * Opcode: Lade Register A aus H
     */
    private static final int LD_H_A = 0x7C;
    /**
     * Opcode: Lade Register A aus L
     */
    private static final int LD_L_A = 0x7D;
    /**
     * Opcode: Lade Register A aus A
     */
    private static final int LD_A_A = 0x7F;
    /**
     * Opcode: Lade Register B aus direktem Operanden
     */
    private static final int LD_IMM_B = 0x06;
    /**
     * Opcode: Lade Register C aus direktem Operanden
     */
    private static final int LD_IMM_C = 0x0E;
    /**
     * Opcode: Lade Register D aus direktem Operanden
     */
    private static final int LD_IMM_D = 0x16;
    /**
     * Opcode: Lade Register E aus direktem Operanden
     */
    private static final int LD_IMM_E = 0x1E;
    /**
     * Opcode: Lade Register L aus direktem Operanden
     */
    private static final int LD_IMM_L = 0x2E;
    /**
     * Opcode: Lade Register A aus direktem Operanden
     */
    private static final int LD_IMM_A = 0x3E;
    /**
     * Opcode: Lade Register H aus direktem Operanden
     */
    private static final int LD_IMM_H = 0x26;
    /**
     * Opcode: Lade Register A nach (BC)
     */
    private static final int LD_A_MEM_BC = 0x02;
    /**
     * Opcode: Lade Register A nach (DE)
     */
    private static final int LD_A_MEM_DE = 0x12;
    /**
     * Opcode: Lade Register A nach (Speicher)
     */
    private static final int LD_A_MEM = 0x32;
    /**
     * Opcode: Lade (BC) nach Register A
     */
    private static final int LD_MEM_BC_A = 0x0A;
    /**
     * Opcode: Lade (DE) nach Register A
     */
    private static final int LD_MEM_DE_A = 0x1A;
    /**
     * Opcode: Lade (Speicher) nach Register A
     */
    private static final int LD_MEM_A = 0x3A;
    /**
     * Opcode: Lade (HL) nach Register B
     */
    private static final int LD_MEM_HL_B = 0x46;
    /**
     * Opcode: Lade (HL) nach Register C
     */
    private static final int LD_MEM_HL_C = 0x4E;
    /**
     * Opcode: Lade (HL) nach Register D
     */
    private static final int LD_MEM_HL_D = 0x56;
    /**
     * Opcode: Lade (HL) nach Register E
     */
    private static final int LD_MEM_HL_E = 0x5E;
    /**
     * Opcode: Lade (HL) nach Register H
     */
    private static final int LD_MEM_HL_H = 0x66;
    /**
     * Opcode: Lade (HL) nach Register L
     */
    private static final int LD_MEM_HL_L = 0x6E;
    /**
     * Opcode: Lade (HL) nach Register A
     */
    private static final int LD_MEM_HL_A = 0x7E;
    /**
     * Opcode: Lade Register B nach (HL)
     */
    private static final int LD_B_MEM_HL = 0x70;
    /**
     * Opcode: Lade Register C nach (HL)
     */
    private static final int LD_C_MEM_HL = 0x71;
    /**
     * Opcode: Lade Register D nach (HL)
     */
    private static final int LD_D_MEM_HL = 0x72;
    /**
     * Opcode: Lade Register E nach (HL)
     */
    private static final int LD_E_MEM_HL = 0x73;
    /**
     * Opcode: Lade Register H nach (HL)
     */
    private static final int LD_H_MEM_HL = 0x74;
    /**
     * Opcode: Lade Register L nach (HL)
     */
    private static final int LD_L_MEM_HL = 0x75;
    /**
     * Opcode: Lade Register A nach (HL)
     */
    private static final int LD_A_MEM_HL = 0x77;

    /**
     * Opcode: Lade direkten Operand nach BC
     */
    private static final int LD_IMM_BC = 0x01;
    /**
     * Opcode: Lade direkten Operand nach DE
     */
    private static final int LD_IMM_DE = 0x11;
    /**
     * Opcode: Lade direkten Operand nach HL
     */
    private static final int LD_IMM_HL = 0x21;
    /**
     * Opcode: Lade HL nach (Speicher)
     */
    private static final int LD_HL_MEM = 0x22;
    /**
     * Opcode: Lade (Speicher) nach HL
     */
    private static final int LD_MEM_HL = 0x2A;
    /**
     * Opcode: Lade direkten Operand nach SP
     */
    private static final int LD_IMM_SP = 0x31;
    /**
     * Opcode: Lade direkten Operand nach (HL)
     */
    private static final int LD_IMM_MEM_HL = 0x36;
    /**
     * Opcode: Lade HL nach SP
     */
    private static final int LD_HL_SP = 0xF9;

    /**
     * Opcode: Hole BC von Stack
     */
    private static final int POP_BC = 0xC1;
    /**
     * Opcode: Hole DE von Stack
     */
    private static final int POP_DE = 0xD1;
    /**
     * Opcode: Hole HL von Stack
     */
    private static final int POP_HL = 0xE1;
    /**
     * Opcode: Hole BC von Stack
     */
    private static final int POP_AF = 0xF1;
    /**
     * Opcode: Speicher BC auf Stack
     */
    private static final int PUSH_BC = 0xC5;
    /**
     * Opcode: Speicher DE auf Stack
     */
    private static final int PUSH_DE = 0xD5;
    /**
     * Opcode: Speicher HL auf Stack
     */
    private static final int PUSH_HL = 0xE5;
    /**
     * Opcode: Speicher AF auf Stack
     */
    private static final int PUSH_AF = 0xF5;

    /**
     * Opcode: Gebe A auf direkt angegebenem Port aus
     */
    private static final int OUT_A_IMM = 0xD3;
    /**
     * Opcode: Lese A von direkt angegebenem Port
     */
    private static final int IN_IMM_A = 0xDB;

    /**
     * Opcode: Tausche Hauptregister A und F mit Tauschregistersatz
     */
    private static final int EX_AF = 0x08;
    /**
     * Opcode: Tausche Alle Hauptregister mit Tauschregistersatz
     */
    private static final int EXX = 0xD9;
    /**
     * Opcode: Tausche (HL) mit SP
     */
    private static final int EX_HL_MEM_SP = 0xE3;
    /**
     * Opcode: Tausche HL mit DE
     */
    private static final int EX_HL_DE = 0xEB;

    /**
     * Opcode: Addiere Register B zu A
     */
    private static final int ADD_B_A = 0x80;
    /**
     * Opcode: Addiere Register C zu A
     */
    private static final int ADD_C_A = 0x81;
    /**
     * Opcode: Addiere Register D zu A
     */
    private static final int ADD_D_A = 0x82;
    /**
     * Opcode: Addiere Register E zu A
     */
    private static final int ADD_E_A = 0x83;
    /**
     * Opcode: Addiere Register H zu A
     */
    private static final int ADD_H_A = 0x84;
    /**
     * Opcode: Addiere Register L zu A
     */
    private static final int ADD_L_A = 0x85;
    /**
     * Opcode: Addiere Register A zu A
     */
    private static final int ADD_A_A = 0x87;
    /**
     * Opcode: Addiere (HL) zu A
     */
    private static final int ADD_MEM_HL_A = 0x86;
    /**
     * Opcode: Addiere direkten Operanden zu A
     */
    private static final int ADD_IMM_A = 0xC6;
    /**
     * Opcode: Addiere Register B zu A mit Übertrag
     */
    private static final int ADC_B_A = 0x88;
    /**
     * Opcode: Addiere Register C zu A mit Übertrag
     */
    private static final int ADC_C_A = 0x89;
    /**
     * Opcode: Addiere Register D zu A mit Übertrag
     */
    private static final int ADC_D_A = 0x8A;
    /**
     * Opcode: Addiere Register E zu A mit Übertrag
     */
    private static final int ADC_E_A = 0x8B;
    /**
     * Opcode: Addiere Register H zu A mit Übertrag
     */
    private static final int ADC_H_A = 0x8C;
    /**
     * Opcode: Addiere Register L zu A mit Übertrag
     */
    private static final int ADC_L_A = 0x8D;
    /**
     * Opcode: Addiere Register A zu A mit Übertrag
     */
    private static final int ADC_A_A = 0x8F;
    /**
     * Opcode: Addiere (HL) zu A mit Übertrag
     */
    private static final int ADC_MEM_HL_A = 0x8E;
    /**
     * Opcode: Addiere direkten Operanden zu A mit Übertrag
     */
    private static final int ADC_IMM_A = 0xCE;
    /**
     * Opcode: Subtrahiere Register B von A
     */
    private static final int SUB_B_A = 0x90;
    /**
     * Opcode: Subtrahiere Register C von A
     */
    private static final int SUB_C_A = 0x91;
    /**
     * Opcode: Subtrahiere Register D von A
     */
    private static final int SUB_D_A = 0x92;
    /**
     * Opcode: Subtrahiere Register E von A
     */
    private static final int SUB_E_A = 0x93;
    /**
     * Opcode: Subtrahiere Register H von A
     */
    private static final int SUB_H_A = 0x94;
    /**
     * Opcode: Subtrahiere Register L von A
     */
    private static final int SUB_L_A = 0x95;
    /**
     * Opcode: Subtrahiere Register A von A
     */
    private static final int SUB_A_A = 0x97;
    /**
     * Opcode: Subtrahiere (HL) von A
     */
    private static final int SUB_MEM_HL_A = 0x96;
    /**
     * Opcode: Subtrahiere direkten Operanden von A
     */
    private static final int SUB_IMM_A = 0xD6;
    /**
     * Opcode: Subtrahiere Register B von A mit Übertrag
     */
    private static final int SBC_B_A = 0x98;
    /**
     * Opcode: Subtrahiere Register C von A mit Übertrag
     */
    private static final int SBC_C_A = 0x99;
    /**
     * Opcode: Subtrahiere Register D von A mit Übertrag
     */
    private static final int SBC_D_A = 0x9A;
    /**
     * Opcode: Subtrahiere Register E von A mit Übertrag
     */
    private static final int SBC_E_A = 0x9B;
    /**
     * Opcode: Subtrahiere Register H von A mit Übertrag
     */
    private static final int SBC_H_A = 0x9C;
    /**
     * Opcode: Subtrahiere Register L von A mit Übertrag
     */
    private static final int SBC_L_A = 0x9D;
    /**
     * Opcode: Subtrahiere Register A von A mit Übertrag
     */
    private static final int SBC_A_A = 0x9F;
    /**
     * Opcode: Subtrahiere (HL) von A mit Übertrag
     */
    private static final int SBC_MEM_HL_A = 0x9E;
    /**
     * Opcode: Subtrahiere direkten Operanden von A mit Übertrag
     */
    private static final int SBC_IMM_A = 0xDE;
    /**
     * Opcode: Inkrementiere Register B
     */
    private static final int INC_B = 0x04;
    /**
     * Opcode: Inkrementiere Register C
     */
    private static final int INC_C = 0x0C;
    /**
     * Opcode: Inkrementiere Register D
     */
    private static final int INC_D = 0x14;
    /**
     * Opcode: Inkrementiere Register E
     */
    private static final int INC_E = 0x1C;
    /**
     * Opcode: Inkrementiere Register H
     */
    private static final int INC_H = 0x24;
    /**
     * Opcode: Inkrementiere Register L
     */
    private static final int INC_L = 0x2C;
    /**
     * Opcode: Inkrementiere (HL)
     */
    private static final int INC_MEM_HL = 0x34;
    /**
     * Opcode: Inkrementiere Register A
     */
    private static final int INC_A = 0x3C;
    /**
     * Opcode: Dekrementiere Register B
     */
    private static final int DEC_B = 0x05;
    /**
     * Opcode: Dekrementiere Register C
     */
    private static final int DEC_C = 0x0D;
    /**
     * Opcode: Dekrementiere Register D
     */
    private static final int DEC_D = 0x15;
    /**
     * Opcode: Dekrementiere Register E
     */
    private static final int DEC_E = 0x1D;
    /**
     * Opcode: Dekrementiere Register H
     */
    private static final int DEC_H = 0x25;
    /**
     * Opcode: Dekrementiere Register L
     */
    private static final int DEC_L = 0x2D;
    /**
     * Opcode: Dekrementiere (HL)
     */
    private static final int DEC_MEM_HL = 0x35;
    /**
     * Opcode: Dekrementiere Register A
     */
    private static final int DEC_A = 0x3D;
    /**
     * Opcode: Dezimalanpassung des Akkumulators
     */
    private static final int DAA = 0x27;
    /**
     * Opcode: Komplementiere Akkumulator
     */
    private static final int CPL = 0x2F;

    /**
     * Opcode: Addiere BC zu HL
     */
    private static final int ADD_BC_HL = 0x09;
    /**
     * Opcode: Addiere DE zu HL
     */
    private static final int ADD_DE_HL = 0x19;
    /**
     * Opcode: Addiere HL zu HL
     */
    private static final int ADD_HL_HL = 0x29;
    /**
     * Opcode: Addiere SP zu HL
     */
    private static final int ADD_SP_HL = 0x39;
    /**
     * Opcode: Inkrementiere BC
     */
    private static final int INC_BC = 0x03;
    /**
     * Opcode: Inkrementiere DE
     */
    private static final int INC_DE = 0x13;
    /**
     * Opcode: Inkrementiere HL
     */
    private static final int INC_HL = 0x23;
    /**
     * Opcode: Inkrementiere SP
     */
    private static final int INC_SP = 0x33;
    /**
     * Opcode: Dekrementiere BC
     */
    private static final int DEC_BC = 0x0B;
    /**
     * Opcode: Dekrementiere DE
     */
    private static final int DEC_DE = 0x1B;
    /**
     * Opcode: Dekrementiere HL
     */
    private static final int DEC_HL = 0x2B;
    /**
     * Opcode: Dekrementiere SP
     */
    private static final int DEC_SP = 0x3B;

    /**
     * Opcode: Bitweises UND Register A mit B
     */
    private static final int AND_B = 0xA0;
    /**
     * Opcode: Bitweises UND Register A mit C
     */
    private static final int AND_C = 0xA1;
    /**
     * Opcode: Bitweises UND Register A mit D
     */
    private static final int AND_D = 0xA2;
    /**
     * Opcode: Bitweises UND Register A mit E
     */
    private static final int AND_E = 0xA3;
    /**
     * Opcode: Bitweises UND Register A mit H
     */
    private static final int AND_H = 0xA4;
    /**
     * Opcode: Bitweises UND Register A mit L
     */
    private static final int AND_L = 0xA5;
    /**
     * Opcode: Bitweises UND Register A mit A
     */
    private static final int AND_A = 0xA7;
    /**
     * Opcode: Bitweises UND Register A mit (HL)
     */
    private static final int AND_MEM_HL = 0xA6;
    /**
     * Opcode: Bitweises UND Register A mit direktem Operand
     */
    private static final int AND_IMM = 0xE6;
    /**
     * Opcode: Bitweises XOR Register A mit B
     */
    private static final int XOR_B = 0xA8;
    /**
     * Opcode: Bitweises XOR Register A mit C
     */
    private static final int XOR_C = 0xA9;
    /**
     * Opcode: Bitweises XOR Register A mit D
     */
    private static final int XOR_D = 0xAA;
    /**
     * Opcode: Bitweises XOR Register A mit E
     */
    private static final int XOR_E = 0xAB;
    /**
     * Opcode: Bitweises XOR Register A mit H
     */
    private static final int XOR_H = 0xAC;
    /**
     * Opcode: Bitweises XOR Register A mit L
     */
    private static final int XOR_L = 0xAD;
    /**
     * Opcode: Bitweises XOR Register A mit (HL)
     */
    private static final int XOR_MEM_HL = 0xAE;
    /**
     * Opcode: Bitweises XOR Register A mit A
     */
    private static final int XOR_A = 0xAF;
    /**
     * Opcode: Bitweises XOR Register A mit direktem Operand
     */
    private static final int XOR_IMM = 0xEE;
    /**
     * Opcode: Bitweises ODER Register A mit B
     */
    private static final int OR_B = 0xB0;
    /**
     * Opcode: Bitweises ODER Register A mit C
     */
    private static final int OR_C = 0xB1;
    /**
     * Opcode: Bitweises ODER Register A mit D
     */
    private static final int OR_D = 0xB2;
    /**
     * Opcode: Bitweises ODER Register A mit E
     */
    private static final int OR_E = 0xB3;
    /**
     * Opcode: Bitweises ODER Register A mit H
     */
    private static final int OR_H = 0xB4;
    /**
     * Opcode: Bitweises ODER Register A mit L
     */
    private static final int OR_L = 0xB5;
    /**
     * Opcode: Bitweises ODER Register A mit A
     */
    private static final int OR_A = 0xB7;
    /**
     * Opcode: Bitweises ODER Register A mit (HL)
     */
    private static final int OR_MEM_HL = 0xB6;
    /**
     * Opcode: Bitweises ODER Register A mit direktem Operanden
     */
    private static final int OR_IMM = 0xF6;
    /**
     * Opcode: Vergleiche Register A mit B
     */
    private static final int CP_B = 0xB8;
    /**
     * Opcode: Vergleiche Register A mit C
     */
    private static final int CP_C = 0xB9;
    /**
     * Opcode: Vergleiche Register A mit D
     */
    private static final int CP_D = 0xBA;
    /**
     * Opcode: Vergleiche Register A mit E
     */
    private static final int CP_E = 0xBB;
    /**
     * Opcode: Vergleiche Register A mit H
     */
    private static final int CP_H = 0xBC;
    /**
     * Opcode: Vergleiche Register A mit L
     */
    private static final int CP_L = 0xBD;
    /**
     * Opcode: Vergleiche Register A mit A
     */
    private static final int CP_A = 0xBF;
    /**
     * Opcode: Vergleiche Register A mit (HL)
     */
    private static final int CP_MEM_HL = 0xBE;
    /**
     * Opcode: Vergleiche Register A mit direktem Operanden
     */
    private static final int CP_IMM = 0xFE;

    /**
     * Opcode: Springe zu direktem Operanden
     */
    private static final int JP_IMM = 0xC3;
    /**
     * Opcode: Springe zu (HL)
     */
    private static final int JP_MEM_HL = 0xE9;
    /**
     * Opcode: Sprung, wenn nicht null zu direktem Operanden
     */
    private static final int JP_NZ_IMM = 0xC2;
    /**
     * Opcode: Sprung, wenn null zu direktem Operanden
     */
    private static final int JP_Z_IMM = 0xCA;
    /**
     * Opcode: Sprung, wenn kein Carry zu direktem Operanden
     */
    private static final int JP_NC_IMM = 0xD2;
    /**
     * Opcode: Sprung, wenn Carry zu direktem Operanden
     */
    private static final int JP_C_IMM = 0xDA;
    /**
     * Opcode: Sprung, wenn ungerade Parität zu direktem Operanden
     */
    private static final int JP_PO_IMM = 0xE2;
    /**
     * Opcode: Sprung, wenn gerade Parität zu direktem Operanden
     */
    private static final int JP_PE_IMM = 0xEA;
    /**
     * Opcode: Sprung, wenn positiv zu direktem Operanden
     */
    private static final int JP_P_IMM = 0xF2;
    /**
     * Opcode: Sprung, wenn negativ zu direktem Operanden
     */
    private static final int JP_M_IMM = 0xFA;
    /**
     * Opcode: Springe relativ
     */
    private static final int JR_IMM = 0x18;
    /**
     * Opcode: Sprung relativ, wenn nicht null
     */
    private static final int JR_NZ_IMM = 0x20;
    /**
     * Opcode: Sprung relativ, wenn null
     */
    private static final int JR_Z_IMM = 0x28;
    /**
     * Opcode: Sprung relativ, wenn kein Carry
     */
    private static final int JR_NC_IMM = 0x30;
    /**
     * Opcode: Sprung relativ, wenn Carry
     */
    private static final int JR_C_IMM = 0x38;
    /**
     * Opcode: Dekrementiere B und Sprung, wenn nicht null
     */
    private static final int DJNZ_IMM = 0x10;

    /**
     * Opcode: Unterprogrammaufruf zu direktem Operanden
     */
    private static final int CALL_IMM = 0xCD;
    /**
     * Opcode: Unterprogrammaufruf, wenn nicht null zu direktem Operanden
     */
    private static final int CALL_NZ_IMM = 0xC4;
    /**
     * Opcode: Unterprogrammaufruf, wenn null zu direktem Operanden
     */
    private static final int CALL_Z_IMM = 0xCC;
    /**
     * Opcode: Unterprogrammaufruf, wenn kein Carry zu direktem Operanden
     */
    private static final int CALL_NC_IMM = 0xD4;
    /**
     * Opcode: Unterprogrammaufruf, wenn Carry zu direktem Operanden
     */
    private static final int CALL_C_IMM = 0xDC;
    /**
     * Opcode: Unterprogrammaufruf, wenn ungerade Parität zu direktem Operanden
     */
    private static final int CALL_PO_IMM = 0xE4;
    /**
     * Opcode: Unterprogrammaufruf, wenn gerade Parität zu direktem Operanden
     */
    private static final int CALL_PE_IMM = 0xEC;
    /**
     * Opcode: Unterprogrammaufruf, wenn positiv zu direktem Operanden
     */
    private static final int CALL_P_IMM = 0xF4;
    /**
     * Opcode: Unterprogrammaufruf, wenn negativ zu direktem Operanden
     */
    private static final int CALL_M_IMM = 0xFC;
    /**
     * Opcode: Rückkehr aus Unterprogramm
     */
    private static final int RET = 0xC9;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn nicht null
     */
    private static final int RET_NZ = 0xC0;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn null
     */
    private static final int RET_Z = 0xC8;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn kein Carry
     */
    private static final int RET_NC = 0xD0;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn Carry
     */
    private static final int RET_C = 0xD8;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn Parität ungerade
     */
    private static final int RET_PO = 0xE0;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn Parität gerade
     */
    private static final int RET_PE = 0xE8;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn positiv
     */
    private static final int RET_P = 0xF0;
    /**
     * Opcode: Rückkehr aus Unterprogramm, wenn negativ
     */
    private static final int RET_M = 0xF8;
    /**
     * Opcode: Neustart bei Adresse 0x00
     */
    private static final int RST_00 = 0xC7;
    /**
     * Opcode: Neustart bei Adresse 0x08
     */
    private static final int RST_08 = 0xCF;
    /**
     * Opcode: Neustart bei Adresse 0x10
     */
    private static final int RST_10 = 0xD7;
    /**
     * Opcode: Neustart bei Adresse 0x18
     */
    private static final int RST_18 = 0xDF;
    /**
     * Opcode: Neustart bei Adresse 0x20
     */
    private static final int RST_20 = 0xE7;
    /**
     * Opcode: Neustart bei Adresse 0x28
     */
    private static final int RST_28 = 0xEF;
    /**
     * Opcode: Neustart bei Adresse 0x30
     */
    private static final int RST_30 = 0xF7;
    /**
     * Opcode: Neustart bei Adresse 0x38
     */
    private static final int RST_38 = 0xFF;

    /**
     * Opcode: Keine Operation
     */
    private static final int NOP = 0x00;
    /**
     * Opcode: Setze Carry-FlagKeine Operation
     */
    private static final int SCF = 0x37;
    /**
     * Opcode: Komplementiere Carry-Flag
     */
    private static final int CCF = 0x3F;
    /**
     * Opcode: Prozessorhalt
     */
    private static final int HALT = 0x76;
    /**
     * Opcode: Deaktiviere Interrupts
     */
    private static final int DI = 0xF3;
    /**
     * Opcode: Aktiviere Interrupts
     */
    private static final int EI = 0xFB;

    /**
     * Opcode: Rotiere Akkumulator links
     */
    private static final int RLCA = 0x07;
    /**
     * Opcode: Rotiere Akkumulator rechts
     */
    private static final int RRCA = 0x0F;
    /**
     * Opcode: Rotiere Akkumulator links durch Carry-Flag
     */
    private static final int RLA = 0x17;
    /**
     * Opcode: Rotiere Akkumulator rechts durch Carry-Flag
     */
    private static final int RRA = 0x1F;

    /**
     * (0xCB) 2. Opcode : Rotiere Links
     */
    private static final int _CB_RLC = 0x00;
    /**
     * (0xCB) 2. Opcode : Rotiere Rechts
     */
    private static final int _CB_RRC = 0x08;
    /**
     * (0xCB) 2. Opcode : Rotiere Links durch Carry-Flag
     */
    private static final int _CB_RL = 0x10;
    /**
     * (0xCB) 2. Opcode : Rotiere Rechts durch Carry-Flag
     */
    private static final int _CB_RR = 0x18;
    /**
     * (0xCB) 2. Opcode : Arithmetisches Links-Schieben
     */
    private static final int _CB_SLA = 0x20;
    /**
     * (0xCB) 2. Opcode : Arithmetisches Rechts-Schieben
     */
    private static final int _CB_SRA = 0x28;
    /**
     * (0xCB) 2. Opcode : Unoffizieller Opcode SLL
     */
    private static final int _CB_SLL = 0x30;
    /**
     * (0xCB) 2. Opcode : Unoffizieller Opcode SRL
     */
    private static final int _CB_SRL = 0x38;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 0
     */
    private static final int _CB_BIT0 = 0x40;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 1
     */
    private static final int _CB_BIT1 = 0x48;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 2
     */
    private static final int _CB_BIT2 = 0x50;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 3
     */
    private static final int _CB_BIT3 = 0x58;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 4
     */
    private static final int _CB_BIT4 = 0x60;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 5
     */
    private static final int _CB_BIT5 = 0x68;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 6
     */
    private static final int _CB_BIT6 = 0x70;
    /**
     * (0xCB) 2. Opcode : Prüfe Bit 7
     */
    private static final int _CB_BIT7 = 0x78;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 0
     */
    private static final int _CB_RES0 = 0x80;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 1
     */
    private static final int _CB_RES1 = 0x88;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 2
     */
    private static final int _CB_RES2 = 0x90;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 3
     */
    private static final int _CB_RES3 = 0x98;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 4
     */
    private static final int _CB_RES4 = 0xA0;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 5
     */
    private static final int _CB_RES5 = 0xA8;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 6
     */
    private static final int _CB_RES6 = 0xB0;
    /**
     * (0xCB) 2. Opcode : Lösche Bit 7
     */
    private static final int _CB_RES7 = 0xB8;
    /**
     * (0xCB) 2. Opcode : Setze Bit 0
     */
    private static final int _CB_SET0 = 0xC0;
    /**
     * (0xCB) 2. Opcode : Setze Bit 1
     */
    private static final int _CB_SET1 = 0xC8;
    /**
     * (0xCB) 2. Opcode : Setze Bit 2
     */
    private static final int _CB_SET2 = 0xD0;
    /**
     * (0xCB) 2. Opcode : Setze Bit 3
     */
    private static final int _CB_SET3 = 0xD8;
    /**
     * (0xCB) 2. Opcode : Setze Bit 4
     */
    private static final int _CB_SET4 = 0xE0;
    /**
     * (0xCB) 2. Opcode : Setze Bit 5
     */
    private static final int _CB_SET5 = 0xE8;
    /**
     * (0xCB) 2. Opcode : Setze Bit 6
     */
    private static final int _CB_SET6 = 0xF0;
    /**
     * (0xCB) 2. Opcode : Setze Bit 7
     */
    private static final int _CB_SET7 = 0xF8;

    /**
     * (0xDD/0xFD) 2. Opcode : Addiere BC zu IX/IY
     */
    private static final int _DD_FD_ADD_BC_I = 0x09;
    /**
     * (0xDD/0xFD) 2. Opcode : Addiere DE zu IX/IY
     */
    private static final int _DD_FD_ADD_DE_I = 0x19;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade IX/IY aus direktem Operanden
     */
    private static final int _DD_FD_LD_IMM_I = 0x21;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (direkter Operand) aus IX/IY
     */
    private static final int _DD_FD_LD_I_MEM = 0x22;
    /**
     * (0xDD/0xFD) 2. Opcode : Inkrementiere IX/IY
     */
    private static final int _DD_FD_INC_I = 0x23;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Inkrementiere IXH/IYH
     */
    private static final int _DD_FD_INC_IH = 0x24;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Dekrementiere IXH/IYH
     */
    private static final int _DD_FD_DEC_IH = 0x25;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus direktem
     * Operanden
     */
    private static final int _DD_FD_LD_IMM_IH = 0x26;
    /**
     * (0xDD/0xFD) 2. Opcode : Addiere IX/IY zu IX/IY
     */
    private static final int _DD_FD_ADD_I_I = 0x29;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade IX/IY aus (direktem Operanden)
     */
    private static final int _DD_FD_LD_MEM_I = 0x2A;
    /**
     * (0xDD/0xFD) 2. Opcode : Dekrementiere IX/IY
     */
    private static final int _DD_FD_DEC_I = 0x2B;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Inkrementiere IXL/IYL
     */
    private static final int _DD_FD_INC_IL = 0x2C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Dekrementiere IXL/IYL
     */
    private static final int _DD_FD_DEC_IL = 0x2D;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus direktem
     * Operanden
     */
    private static final int _DD_FD_LD_IMM_IL = 0x2E;
    /**
     * (0xDD/0xFD) 2. Opcode : Inkrementiere (IX+0)/(IY+0)
     */
    private static final int _DD_FD_INC_I_0 = 0x34;
    /**
     * (0xDD/0xFD) 2. Opcode : Dekrementiere (IX+0)/(IY+0)
     */
    private static final int _DD_FD_DEC_I_0 = 0x35;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus direktem Operanden
     */
    private static final int _DD_FD_LD_IMM_I_0 = 0x36;
    /**
     * (0xDD/0xFD) 2. Opcode : Addiere SP zu IX/IY
     */
    private static final int _DD_FD_ADD_SP_I = 0x39;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register B aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_B = 0x44;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register B aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_B = 0x45;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register B aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_B = 0x46;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register C aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_C = 0x4C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register C aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_C = 0x4D;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register C aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_C = 0x4E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register D aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_D = 0x54;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register D aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_D = 0x55;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register D aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_D = 0x56;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register E aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_E = 0x5C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register E aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_E = 0x5D;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register E aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_E = 0x5E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus Register B
     */
    private static final int _DD_FD_LD_B_IH = 0x60;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus Register C
     */
    private static final int _DD_FD_LD_C_IH = 0x61;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus Register D
     */
    private static final int _DD_FD_LD_D_IH = 0x62;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus Register E
     */
    private static final int _DD_FD_LD_E_IH = 0x63;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_IH = 0x64;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXH/IYH aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_IH = 0x65;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register H aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_H = 0x66;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register A
     */
    private static final int _DD_FD_LD_A_IH = 0x67;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register B
     */
    private static final int _DD_FD_LD_B_IL = 0x68;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register C
     */
    private static final int _DD_FD_LD_C_IL = 0x69;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register D
     */
    private static final int _DD_FD_LD_D_IL = 0x6A;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register E
     */
    private static final int _DD_FD_LD_E_IL = 0x6B;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_IL = 0x6C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_IL = 0x6D;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register H aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_L = 0x6E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade IXL/IYL aus Register E
     */
    private static final int _DD_FD_LD_A_IL = 0x6F;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register B
     */
    private static final int _DD_FD_LD_B_I_0 = 0x70;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register C
     */
    private static final int _DD_FD_LD_C_I_0 = 0x71;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register D
     */
    private static final int _DD_FD_LD_D_I_0 = 0x72;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register E
     */
    private static final int _DD_FD_LD_E_I_0 = 0x73;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register H
     */
    private static final int _DD_FD_LD_H_I_0 = 0x74;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register L
     */
    private static final int _DD_FD_LD_L_I_0 = 0x75;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade (IX+0)/(IY+0) aus Register B
     */
    private static final int _DD_FD_LD_A_I_0 = 0x77;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register A aus IXH/IYH
     */
    private static final int _DD_FD_LD_IH_A = 0x7C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Lade Register A aus IXL/IYL
     */
    private static final int _DD_FD_LD_IL_A = 0x7D;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade Register A aus (IX+0)/(IY+0)
     */
    private static final int _DD_FD_LD_I_0_A = 0x7E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Addiere IXH/IYH zu A
     */
    private static final int _DD_FD_ADD_IH_A = 0x84;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Addiere IXL/IYL zu A
     */
    private static final int _DD_FD_ADD_IL_A = 0x85;
    /**
     * (0xDD/0xFD) 2. Opcode : Addiere (IX+0)/(IY+0) zu Register A
     */
    private static final int _DD_FD_ADD_I_0_A = 0x86;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Addiere IXH/IYH zu A mit
     * Übertrag
     */
    private static final int _DD_FD_ADC_IH_A = 0x8C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Addiere IXL/IYL zu A mit
     * Übertrag
     */
    private static final int _DD_FD_ADC_IL_A = 0x8D;
    /**
     * (0xDD/0xFD) 2. Opcode : Addiere (IX+0)/(IY+0) zu Register A mit Übertrag
     */
    private static final int _DD_FD_ADC_I_0_A = 0x8E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Subtrahiere IXH/IYH von A
     */
    private static final int _DD_FD_SUB_IH_A = 0x94;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Subtrahiere IXL/IYL von A
     */
    private static final int _DD_FD_SUB_IL_A = 0x95;
    /**
     * (0xDD/0xFD) 2. Opcode : Subtrahiere (IX+0)/(IY+0) von A
     */
    private static final int _DD_FD_SUB_I_0_A = 0x96;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Subtrahiere IXH/IYH von A
     * mit Übertrag
     */
    private static final int _DD_FD_SBC_IH_A = 0x9C;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Subtrahiere IXL/IYL von A
     * mit Übertrag
     */
    private static final int _DD_FD_SBC_IL_A = 0x9D;
    /**
     * (0xDD/0xFD) 2. Opcode : Subtrahiere (IX+0)/(IY+0) von A mit Übertrag
     */
    private static final int _DD_FD_SBC_I_0_A = 0x9E;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises UND IXH/IYH mit A
     */
    private static final int _DD_FD_AND_IH = 0xA4;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises UND IXL/IYL mit A
     */
    private static final int _DD_FD_AND_IL = 0xA5;
    /**
     * (0xDD/0xFD) 2. Opcode : Bitweises UND (IX+0)/(IY+0) mit A
     */
    private static final int _DD_FD_AND_I_0 = 0xA6;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises XOR IXH/IYH mit A
     */
    private static final int _DD_FD_XOR_IH = 0xAC;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises XOR IXL/IYL mit A
     */
    private static final int _DD_FD_XOR_IL = 0xAD;
    /**
     * (0xDD/0xFD) 2. Opcode : Bitweises XOR (IX+0)/(IY+0) mit A
     */
    private static final int _DD_FD_XOR_I_0 = 0xAE;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises ODER IXH/IYH mit A
     */
    private static final int _DD_FD_OR_IH = 0xB4;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Bitweises ODER IXL/IYL mit A
     */
    private static final int _DD_FD_OR_IL = 0xB5;
    /**
     * (0xDD/0xFD) 2. Opcode : Bitweises ODER (IX+0)/(IY+0) mit A
     */
    private static final int _DD_FD_OR_I_0 = 0xB6;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Vergleich IXH/IYH mit A
     */
    private static final int _DD_FD_CP_IH = 0xBC;
    /**
     * (0xDD/0xFD) 2. Opcode : Unoffizieller Opcode Vergleich IXL/IYL mit A
     */
    private static final int _DD_FD_CP_IL = 0xBD;
    /**
     * (0xDD/0xFD) 2. Opcode : Vergleich (IX+0)/(IY+0) mit A
     */
    private static final int _DD_FD_CP_I_0 = 0xBE;
    /**
     * (0xDD/0xFD) 2. Opcode : IX/IY von Stack laden
     */
    private static final int _DD_FD_POP_I = 0xE1;
    /**
     * (0xDD/0xFD) 2. Opcode : Tausche (SP) mit IX/IY
     */
    private static final int _DD_FD_EX_SP_I = 0xE3;
    /**
     * (0xDD/0xFD) 2. Opcode : IX/IY auf Stack speichern
     */
    private static final int _DD_FD_PUSH_I = 0xE5;
    /**
     * (0xDD/0xFD) 2. Opcode : Spring zu (IX)/(IY)
     */
    private static final int _DD_FD_JP_MEM_I = 0xE9;
    /**
     * (0xDD/0xFD) 2. Opcode : Lade SP aus IX/IY
     */
    private static final int _DD_FD_LD_I_SP = 0xF9;

    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register B
     */
    private static final int _ED_IN_C_B = 0x40;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register B
     */
    private static final int _ED_OUT_B_C = 0x41;
    /**
     * (0xED) 2. Opcode : Subtrahiere BC von HL mit Übertrag
     */
    private static final int _ED_SBC_BC_HL = 0x42;
    /**
     * (0xED) 2. Opcode : Lade BC aus (direktem Operand)
     */
    private static final int _ED_LD_BC_MEM = 0x43;
    /**
     * (0xED) 2. Opcode : Negiere Akkumulator
     */
    private static final int _ED_NEG = 0x44;
    /**
     * (0xED) 2. Opcode : Rücksprung von Nicht-Maskierbarem Interrupt
     */
    private static final int _ED_RETN = 0x45;
    /**
     * (0xED) 2. Opcode : Setze Interruptmodus 0
     */
    private static final int _ED_IM0 = 0x46;
    /**
     * (0xED) 2. Opcode : Lade Interruptregister aus A
     */
    private static final int _ED_LD_A_INT = 0x47;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register C
     */
    private static final int _ED_IN_C_C = 0x48;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register C
     */
    private static final int _ED_OUT_C_C = 0x49;
    /**
     * (0xED) 2. Opcode : Addiere BC zu HL mit Übertrag
     */
    private static final int _ED_ADC_BC_HL = 0x4A;
    /**
     * (0xED) 2. Opcode : Lade (direkten Operand) aus BC
     */
    private static final int _ED_LD_MEM_BC = 0x4B;
    /**
     * (0xED) 2. Opcode : Rücksprung von Interrupt
     */
    private static final int _ED_RETI = 0x4D;
    /**
     * (0xED) 2. Opcode : Lade Refreshregister aus A
     */
    private static final int _ED_LD_A_R = 0x4F;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register D
     */
    private static final int _ED_IN_C_D = 0x50;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register D
     */
    private static final int _ED_OUT_D_C = 0x51;
    /**
     * (0xED) 2. Opcode : Subtrahiere DE von HL mit Übertrag
     */
    private static final int _ED_SBC_DE_HL = 0x52;
    /**
     * (0xED) 2. Opcode : Lade (direkten Operand) aus DE
     */
    private static final int _ED_LD_DE_MEM = 0x53;
    /**
     * (0xED) 2. Opcode : Setze Interruptmodus 1
     */
    private static final int _ED_IM1 = 0x56;
    /**
     * (0xED) 2. Opcode : Lade A aus Interruptregister
     */
    private static final int _ED_LD_INT_A = 0x57;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register E
     */
    private static final int _ED_IN_C_E = 0x58;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register E
     */
    private static final int _ED_OUT_E_C = 0x59;
    /**
     * (0xED) 2. Opcode : Addiere DE zu HL mit Übertrag
     */
    private static final int _ED_ADC_DE_HL = 0x5A;
    /**
     * (0xED) 2. Opcode : Lade DE aus (direktem Operand)
     */
    private static final int _ED_LD_MEM_DE = 0x5B;
    /**
     * (0xED) 2. Opcode : Setze Interruptmodus 2
     */
    private static final int _ED_IM2 = 0x5E;
    /**
     * (0xED) 2. Opcode : Lade A aus Refreshregister
     */
    private static final int _ED_LD_R_A = 0x5F;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register H
     */
    private static final int _ED_IN_C_H = 0x60;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register H
     */
    private static final int _ED_OUT_H_C = 0x61;
    /**
     * (0xED) 2. Opcode : Subtrahiere HL von HL mit Übertrag
     */
    private static final int _ED_SBC_HL_HL = 0x62;
    /**
     * (0xED) 2. Opcode : Lade (direkten Operand) aus HL
     */
    private static final int _ED_LD_HL_MEM = 0x63;
    /**
     * (0xED) 2. Opcode : Rotiere Rechts Dezimal
     */
    private static final int _ED_RRD = 0x67;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register L
     */
    private static final int _ED_IN_C_L = 0x68;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register L
     */
    private static final int _ED_OUT_L_C = 0x69;
    /**
     * (0xED) 2. Opcode : Addiere HL zu HL mit Übertrag
     */
    private static final int _ED_ADC_HL_HL = 0x6A;
    /**
     * (0xED) 2. Opcode : Lade HL aus (direktem Operand)
     */
    private static final int _ED_LD_MEM_HL = 0x6B;
    /**
     * (0xED) 2. Opcode : Rotiere Links Dezimal
     */
    private static final int _ED_RLD = 0x6F;
    /**
     * (0xED) 2. Opcode : Unoffizieller Opcode Lese aus Port C nach Flagregister
     */
    private static final int _ED_IN_C_F = 0x70;
    /**
     * (0xED) 2. Opcode : Unoffizieller Opcode Schreibe auf Port C aus
     * Flagregister
     */
    private static final int _ED_OUT_F_C = 0x71;
    /**
     * (0xED) 2. Opcode : Subtrahiere SP von HL mit Übertrag
     */
    private static final int _ED_SBC_SP_HL = 0x72;
    /**
     * (0xED) 2. Opcode : Lade (direkten Operand) aus SP
     */
    private static final int _ED_LD_SP_MEM = 0x73;
    /**
     * (0xED) 2. Opcode : Lese aus Port C nach Register A
     */
    private static final int _ED_IN_C_A = 0x78;
    /**
     * (0xED) 2. Opcode : Schreibe auf Port C aus Register A
     */
    private static final int _ED_OUT_A_C = 0x79;
    /**
     * (0xED) 2. Opcode : Addiere SP zu HL mit Übertrag
     */
    private static final int _ED_ADC_SP_HL = 0x7A;
    /**
     * (0xED) 2. Opcode : Lade SP aus (direktem Operand)
     */
    private static final int _ED_LD_MEM_SP = 0x7B;
    /**
     * (0xED) 2. Opcode : Blockladen mit Inkrementieren
     */
    private static final int _ED_LDI = 0xA0;
    /**
     * (0xED) 2. Opcode : Vergleiche und Inkrementiere
     */
    private static final int _ED_CPI = 0xA1;
    /**
     * (0xED) 2. Opcode : Eingabe mit Inkrementieren
     */
    private static final int _ED_INI = 0xA2;
    /**
     * (0xED) 2. Opcode : Ausgabe mit Inkrementieren
     */
    private static final int _ED_OUTI = 0xA3;
    /**
     * (0xED) 2. Opcode : Blockladen mit Dekrementieren
     */
    private static final int _ED_LDD = 0xA8;
    /**
     * (0xED) 2. Opcode : Vergleiche und Dekrementiere
     */
    private static final int _ED_CPD = 0xA9;
    /**
     * (0xED) 2. Opcode : Eingabe mit Dekrementieren
     */
    private static final int _ED_IND = 0xAA;
    /**
     * (0xED) 2. Opcode : Ausgabe mit Dekrementieren
     */
    private static final int _ED_OUTD = 0xAB;
    /**
     * (0xED) 2. Opcode : Wiederholtes Blockladen mit Inkrementieren
     */
    private static final int _ED_LDIR = 0xB0;
    /**
     * (0xED) 2. Opcode : Blockvergleich und Inkrementieren
     */
    private static final int _ED_CPIR = 0xB1;
    /**
     * (0xED) 2. Opcode : Blockeingabe mit Inkrementieren
     */
    private static final int _ED_INIR = 0xB2;
    /**
     * (0xED) 2. Opcode : Blockausgabe mit Inkrementieren
     */
    private static final int _ED_OTIR = 0xB3;
    /**
     * (0xED) 2. Opcode : Wiederholtes Blockladen mit Dekrementieren
     */
    private static final int _ED_LDDR = 0xB8;
    /**
     * (0xED) 2. Opcode : Blockvergleich und Dekrementieren
     */
    private static final int _ED_CPDR = 0xB9;
    /**
     * (0xED) 2. Opcode : Blockeingabe mit Dekrementieren
     */
    private static final int _ED_INDR = 0xBA;
    /**
     * (0xED) 2. Opcode : Blockausgabe mit Inkrementieren
     */
    private static final int _ED_OTDR = 0xBB;

    /**
     * Maske Register B
     */
    private static final int REG_B = 0x00;
    /**
     * Maske Register C
     */
    private static final int REG_C = 0x01;
    /**
     * Maske Register D
     */
    private static final int REG_D = 0x02;
    /**
     * Maske Register E
     */
    private static final int REG_E = 0x03;
    /**
     * Maske Register H
     */
    private static final int REG_H = 0x04;
    /**
     * Maske Register L
     */
    private static final int REG_L = 0x05;
    /**
     * Maske Speicher (HL)
     */
    private static final int MEM_HL = 0x06;
    /**
     * Maske Register A
     */
    private static final int REG_A = 0x07;

    /**
     * Maske Registerpaar BC
     */
    private static final int REGP_BC = 0x00;
    /**
     * Maske Registerpaar DE
     */
    private static final int REGP_DE = 0x01;
    /**
     * Maske Registerpaar HL
     */
    private static final int REGP_HL = 0x02;
    /**
     * Maske Registerpaar IX/IY
     */
    private static final int REGP_IX_IY = 0x02;
    /**
     * Maske Register SP
     */
    private static final int REGP_SP = 0x03;
    /**
     * Maske Registerpaar AF
     */
    private static final int REGP_AF = 0x03;

    /**
     * Maske: Carry-Flag
     */
    private static final int CARRY_FLAG = 0x01;
    /**
     * Maske: Carry-Flag
     */
    private static final int SUBTRACT_FLAG = 0x02;
    /**
     * Maske: Paritäts/Überlauf-Flag
     */
    private static final int PARITY_OVERFLOW_FLAG = 0x04;
    /**
     * Maske: Undokumentiertes Flag Bit 3
     */
    private static final int BIT3_FLAG = 0x08;
    /**
     * Maske: Halbübertrags-Flag
     */
    private static final int HALF_CARRY_FLAG = 0x10;
    /**
     * Maske: Undokumentiertes Flag Bit 5
     */
    private static final int BIT5_FLAG = 0x20;
    /**
     * Maske: null Flag
     */
    private static final int ZERO_FLAG = 0x40;
    /**
     * Maske: Vorzeichen-Flag
     */
    private static final int SIGN_FLAG = 0x80;

    /**
     * Zeiger auf Debugger Instanz
     */
    private final Debugger debugger;
    /**
     * Zeiger auf Decoder Instanz
     */
    private final Decoder decoder;
    /**
     * Zeiger auf Debugger Informationen
     */
    private final DebuggerInfo debugInfo = new DebuggerInfo();
    /**
     * Zeiger auf Subsystem Modul
     */
    private final SubsystemModule module;
    /**
     * Gibt an, ob sich die CPU im HALT Zustand befindet.
     */
    private boolean halt = false;
    /**
     * Gibt an, ob ein Nicht-Maskierbarer-Interrupt ansteht
     */
    private boolean nmi = false;
    /**
     * Interrupt-Modus
     */
    private int interruptMode = 0;
    /**
     * Liste aller anstehendenInterruptanfragen
     */
    private final LinkedList<Integer> interruptsWaiting = new LinkedList<>();
    /**
     * Lokaler Taktzähler
     */
    private double ticks;
    /**
     * NMI in Bearbeitung
     * <p>
     * TODO: Verwendung der Variable prüfen
     */
    private boolean nmiInProgress = false;
    /**
     * Bus-Request durch anderes Gerät angefordert
     */
    private boolean busRequest;
    /**
     * Gibt an, ob ein Freigeben der Interrupts nach dem nächsten Befehl
     * aussteht.
     */
    private boolean eiWaiting = false;
    /**
     * Gibt an, ob ein RETI durch die CPU ausgeführt wurde, welches von
     * Peripherie genutzt wird um das Ende der Interruptbehandlung zu erkennen.
     */
    private boolean retiExceuted = false;
    /**
     * Zählt die Takte der aktuellen Befehlsausführung
     */
    private int tickBuffer = 0;

    /**
     * Erstellt einen neuen Prozessor.
     *
     * @param module Referenz auf KGS Modul
     * @param debug_ident Kürzel, welches die Debug-Ausgaben beinhalten
     */
    public UA880(SubsystemModule module, String debug_ident) {
        this.module = module;
        debugger = new Debugger("UA880_" + debug_ident, false, debug_ident);
        decoder = new Decoder("UA880_" + debug_ident, false, debug_ident);
        debugger.setDebug(false);
    }

    /**
     * Holt den nächsten Befehl aus dem Speicher und führt ihn aus.
     */
    private void executeNextInstruction() {
        boolean debug = debugger.isDebug();

        if (pc == 0x4000) {
            System.out.println("AFS Funktionsaufruf!");
            if (!debugger.isDebug()) {
                this.setDebug(true);
            }
        }

        if (pc == 0x0916) {
            System.out.println(String.format("KES Funktionscode: %02X", getRegister(REG_A)));
            if (getRegister(REG_A) == 0x0F) {
                System.out.println("DIAGNOSE: ");
            }
        }

        if (pc == 0x4025 && (module instanceof KES)) {
            System.out.print("Initialisiere Laufwerk:" + (module.readLocalByte(0x3B38) & 0x03) + " -");
            for (int in = 0; in < 8; in++) {
                System.out.print(String.format(" %02X", module.readLocalByte(ix + in)));
            }
            System.out.println();
        }

        if (pc == 0x4025 && (module instanceof KES)) {
            System.out.println("Fehler 0-2:" + String.format("%02X,%02X,%02X", module.readLocalByte(0x3B22), module.readLocalByte(0x3B23), module.readLocalByte(0x3B24)));
        }

        if (pc == 0x4414 && (module instanceof KES)) {
            System.out.println("Starte Debugger an Adresse 0x4414");
//            this.debugger.setDebug(true);
//            debug = true;
        }

        if (pc == 0x4D9B && (module instanceof KES)) {
            System.out.println("Starte Debugger an Adresse 0x4D9B");
            this.debugger.setDebug(true);
            debug = true;
        }

//        if (pc == 0x6E && (module instanceof KES)) {
//            System.out.println("Prüfe auf laufenden NMI:" + !getFlag(ZERO_FLAG));
//        }
//        if (pc == 0x88 && (module instanceof KES)) {
//            System.out.println("Ende Behandlung Kanal");
//        }
//        if (pc == 0x0970) {
//            System.out.println(String.format("KES Statusabfrage"));
//        }
//        if (pc == 0x00E0) {
//            System.out.println(String.format("Fehler PROM: %02X", getRegister(REG_A)));
//        }
//        if (pc == 0x0316) {
//            System.out.println("Ende DMA Copy 1");
//        }
        int opcode = module.readLocalByte(pc++);
        if (debug) {
            debugInfo.setIp(pc - 1);
            debugInfo.setOpcode(opcode);
        }

        if (eiWaiting) {
            iff1 = 1;
            iff2 = 1;
            eiWaiting = false;
        }

        switch (opcode) {
            case LD_B_B:
            case LD_C_C:
            case LD_D_D:
            case LD_E_E:
            case LD_H_H:
            case LD_L_L:
            case LD_A_A: {
                // Kopieren mit identischem Quell- und Zielregister
                // Behandelt als NOP von UA880
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("LD " + getRegisterString((opcode >> 3) & 0x07) + "," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh", getRegister(opcode & 0x07)));
                }
            }
            break;
            case LD_C_B:
            case LD_D_B:
            case LD_E_B:
            case LD_H_B:
            case LD_L_B:
            case LD_A_B:
            case LD_B_C:
            case LD_D_C:
            case LD_E_C:
            case LD_H_C:
            case LD_L_C:
            case LD_A_C:
            case LD_B_D:
            case LD_C_D:
            case LD_E_D:
            case LD_H_D:
            case LD_L_D:
            case LD_A_D:
            case LD_B_E:
            case LD_C_E:
            case LD_D_E:
            case LD_H_E:
            case LD_L_E:
            case LD_A_E:
            case LD_B_H:
            case LD_C_H:
            case LD_D_H:
            case LD_E_H:
            case LD_L_H:
            case LD_A_H:
            case LD_B_L:
            case LD_C_L:
            case LD_D_L:
            case LD_E_L:
            case LD_H_L:
            case LD_A_L:
            case LD_B_A:
            case LD_C_A:
            case LD_D_A:
            case LD_E_A:
            case LD_H_A:
            case LD_L_A: {
                // Kopieren von 8 Bit Register
                setRegister((opcode >> 3) & 0x07, getRegister(opcode & 0x07));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("LD " + getRegisterString((opcode >> 3) & 0x07) + "," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh", getRegister(opcode & 0x07)));
                }
            }
            break;
            case LD_IMM_B:
            case LD_IMM_C:
            case LD_IMM_D:
            case LD_IMM_E:
            case LD_IMM_L:
            case LD_IMM_A:
            case LD_IMM_H: {
                // Kopieren von direktem Operanden in 8 Bit Register
                setRegister((opcode >> 3) & 0x07, module.readLocalByte(pc++));
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD " + getRegisterString((opcode >> 3) & 0x07) + "," + String.format("%02Xh", module.readLocalByte(pc - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case LD_A_MEM_BC: {
                // Kopieren von A in Speicher an Adresse (BC)
                module.writeLocalByte(getRegisterPairHLSP(REGP_BC), getRegister(REG_A));
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD (BC),A");
                    debugInfo.setOperands(String.format("%02Xh", getRegister(REG_A)));
                }
            }
            break;
            case LD_A_MEM_DE: {
                // Kopieren von A in Speicher an Adresse (BC)
                module.writeLocalByte(getRegisterPairHLSP(REGP_DE), getRegister(REG_A));
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD (DE),A");
                    debugInfo.setOperands(String.format("%02Xh", getRegister(REG_A)));
                }
            }
            break;
            case LD_A_MEM: {
                // Kopieren von A in Speicher an direkt angegebene Adresse
                int address = module.readLocalWord(pc++);
                pc++;
                module.writeLocalByte(address, getRegister(REG_A));
                updateTicks(13);
                if (debug) {
                    debugInfo.setCode("LD " + String.format("%04Xh", address) + ",A");
                    debugInfo.setOperands(String.format("%02Xh", getRegister(REG_A)));
                }
            }
            break;
            case LD_MEM_BC_A: {
                // Kopieren von Speicher aus Adresse (BC) nach A
                setRegister(REG_A, module.readLocalByte(getRegisterPairHLSP(REGP_BC)));
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD A,(BC)");
                    debugInfo.setOperands(String.format("%02Xh", module.readLocalByte(getRegisterPairHLSP(REGP_BC))));
                }
            }
            break;
            case LD_MEM_DE_A: {
                // Kopieren von Speicher aus Adresse (DE) nach A
                setRegister(REG_A, module.readLocalByte(getRegisterPairHLSP(REGP_DE)));
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD A,(DE)");
                    debugInfo.setOperands(String.format("%02Xh", module.readLocalByte(getRegisterPairHLSP(REGP_DE))));
                }
            }
            break;
            case LD_MEM_A: {
                // Kopieren von Speicher aus direkt angegebener Adresse nach A
                int address = module.readLocalWord(pc++);
                pc++;
                setRegister(REG_A, module.readLocalByte(address));
                updateTicks(13);
                if (debug) {
                    debugInfo.setCode("LD A,(" + String.format("%04Xh", address) + ")");
                    debugInfo.setOperands(String.format("%02Xh", module.readLocalByte(address)));
                }
            }
            break;
            case LD_MEM_HL_B:
            case LD_MEM_HL_C:
            case LD_MEM_HL_D:
            case LD_MEM_HL_E:
            case LD_MEM_HL_H:
            case LD_MEM_HL_L:
            case LD_MEM_HL_A: {
                // Kopieren von Speicher aus Adresse (HL) nach 8 Bit Register
                int op1 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                setRegister((opcode >> 3) & 0x07, op1);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD " + getRegisterString((opcode >> 3) & 0x07) + ",(HL)");
                    debugInfo.setOperands(String.format("%02Xh", op1));
                }
            }
            break;
            case LD_B_MEM_HL:
            case LD_C_MEM_HL:
            case LD_D_MEM_HL:
            case LD_E_MEM_HL:
            case LD_H_MEM_HL:
            case LD_L_MEM_HL:
            case LD_A_MEM_HL: {
                // Kopieren von 8 Bit Register nach Speicher an Adresse (HL)
                int op1 = getRegister(opcode & 0x07);
                module.writeLocalByte(getRegisterPairHLSP(REGP_HL), op1);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("LD (HL)," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh", op1));
                }
            }
            break;

            /**
             * 16-bit-Ladebefehle
             */
            case LD_IMM_BC:
            case LD_IMM_DE:
            case LD_IMM_HL:
            case LD_IMM_SP: {
                // Kopieren von direktem Operanden in 16 Bit Register
                int op1 = module.readLocalWord(pc++);
                pc++;
                setRegisterPairHLSP((opcode >> 4) & 0x03, op1);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("LD " + getRegisterPairHLSPString((opcode >> 4) & 0x03) + "," + String.format("%04Xh", op1));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case LD_HL_MEM: {
                // Kopieren von HL in Speicher an direkt angegebene Adresse
                int op1 = getRegisterPairHLSP(REGP_HL);
                int address = module.readLocalWord(pc++);
                pc++;
                module.writeLocalWord(address, op1);
                updateTicks(16);
                if (debug) {
                    debugInfo.setCode("LD (" + String.format("%04Xh", address) + "),HL");
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case LD_MEM_HL: {
                // Kopieren von Speicher an direkt angegebener Adresser nach HL
                int address = module.readLocalWord(pc++);
                pc++;
                int op1 = module.readLocalWord(address);
                setRegisterPairHLSP(REGP_HL, op1);
                updateTicks(16);
                if (debug) {
                    debugInfo.setCode("LD HL,(" + String.format("%04Xh", address) + ")");
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case LD_IMM_MEM_HL: {
                // Kopieren von direktem Operanden in Speicher an Adresse (HL)
                int op1 = module.readLocalByte(pc++);
                module.writeLocalByte(getRegisterPairHLSP(REGP_HL), op1);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("LD (HL)," + String.format("%04Xh", op1) + ")");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case LD_HL_SP: {
                // Kopieren HL nach SP
                setRegisterPairHLSP(REGP_SP, getRegisterPairHLSP(REGP_HL));
                updateTicks(6);
                if (debug) {
                    debugInfo.setCode("LD SP,HL");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case POP_BC:
            case POP_DE:
            case POP_HL:
            case POP_AF: {
                int op1 = pop();
                setRegisterPairAF((opcode >> 4) & 0x03, op1);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("POP " + getRegisterPairAFString((opcode >> 4) & 0x03));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case PUSH_BC:
            case PUSH_DE:
            case PUSH_HL:
            case PUSH_AF: {
                push(getRegisterPairAF((opcode >> 4) & 0x03));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH " + getRegisterPairAFString((opcode >> 4) & 0x03));
                    debugInfo.setOperands(String.format("%04Xh", getRegisterPairAF((opcode >> 4) & 0x03)));
                }
            }
            break;

            /**
             * Ein- und Ausgabebefehle
             */
            case OUT_A_IMM: {
                int port = module.readLocalByte(pc++);
                module.writeLocalPort(port, getRegister(REG_A));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("OUT (" + String.format("%02Xh", port) + "),A");
                    debugInfo.setOperands(String.format("%02Xh", getRegister(REG_A)));
                }
            }
            break;
            case IN_IMM_A: {
                int port = module.readLocalByte(pc++);
                setRegister(REG_A, module.readLocalPort(port));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("IN A,(" + String.format("%02Xh", port) + ")");
                    debugInfo.setOperands(String.format("%02Xh", getRegister(REG_A)));
                }
            }
            break;

            /**
             * Austauschbefehle
             */
            case EX_AF: {
                int exOp = a;
                a = a_;
                a_ = exOp;
                exOp = f;
                f = f_;
                f_ = exOp;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("EX AF,AF'");
                    debugInfo.setOperands(String.format("%02Xh%02Xh,%02Xh%02Xh", a_, f_, a, f));
                }
            }
            break;
            case EXX: {
                int exOp = b;
                b = b_;
                b_ = exOp;
                exOp = c;
                c = c_;
                c_ = exOp;
                exOp = h;
                h = h_;
                h_ = exOp;
                exOp = l;
                l = l_;
                l_ = exOp;
                exOp = d;
                d = d_;
                d_ = exOp;
                exOp = e;
                e = e_;
                e_ = exOp;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("EXX");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case EX_HL_MEM_SP: {
                int exOp = getRegisterPairHLSP(REGP_HL);
                setRegisterPairHLSP(REGP_HL, module.readLocalWord(sp));
                module.writeLocalWord(sp, exOp);
                updateTicks(19);
                if (debug) {
                    debugInfo.setCode("EX (SP),HL");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", getRegisterPairHLSP(REGP_HL), module.readLocalWord(sp)));
                }
            }
            break;
            case EX_HL_DE: {
                int exOp = getRegisterPairHLSP(REGP_HL);
                setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_DE));
                setRegisterPairHLSP(REGP_DE, exOp);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("EX DE,HL");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", getRegisterPairHLSP(REGP_HL), getRegisterPairHLSP(REGP_DE)));
                }
            }
            break;

            /**
             * 8-bit-Arithmetikbefehle
             */
            case ADD_B_A:
            case ADD_C_A:
            case ADD_D_A:
            case ADD_E_A:
            case ADD_H_A:
            case ADD_L_A:
            case ADD_A_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = add8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADD A," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADD_MEM_HL_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = add8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("ADD A,(HL)");
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADD_IMM_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = add8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("ADD A," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_B_A:
            case ADC_C_A:
            case ADC_D_A:
            case ADC_E_A:
            case ADC_H_A:
            case ADC_L_A:
            case ADC_A_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = add8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADC A," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_MEM_HL_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = add8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("ADC A,(HL)");
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_IMM_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = add8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("ADC A," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_B_A:
            case SUB_C_A:
            case SUB_D_A:
            case SUB_E_A:
            case SUB_H_A:
            case SUB_L_A:
            case SUB_A_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = sub8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SUB A," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_MEM_HL_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = sub8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("SUB A,(HL)");
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_IMM_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = sub8(op1, op2, false);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("SUB A," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBC_B_A:
            case SBC_C_A:
            case SBC_D_A:
            case SBC_E_A:
            case SBC_H_A:
            case SBC_L_A:
            case SBC_A_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = sub8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SBC A," + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBC_MEM_HL_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = sub8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("SBC A,(HL)");
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBC_IMM_A: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = sub8(op1, op2, true);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("SBC A," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case INC_B:
            case INC_C:
            case INC_D:
            case INC_E:
            case INC_H:
            case INC_L:
            case INC_A: {
                int op = getRegister((opcode >> 3) & 0x07);
                int res = inc(op);
                setRegister((opcode >> 3) & 0x07, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("INC " + getRegisterString((opcode >> 3) & 0x07));
                    debugInfo.setOperands(String.format("->%02Xh", res));
                }
            }
            break;
            case INC_MEM_HL: {
                int op = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = inc(op);
                module.writeLocalByte(getRegisterPairHLSP(REGP_HL), res);
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("INC (HL)");
                    debugInfo.setOperands(String.format("->%02Xh", res));
                }
            }
            break;
            case DEC_B:
            case DEC_C:
            case DEC_D:
            case DEC_E:
            case DEC_H:
            case DEC_L:
            case DEC_A: {
                int op = getRegister((opcode >> 3) & 0x07);
                int res = dec(op);
                setRegister((opcode >> 3) & 0x07, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("DEC " + getRegisterString((opcode >> 3) & 0x07));
                    debugInfo.setOperands(String.format("->%02Xh", res));
                }
            }
            break;
            case DEC_MEM_HL: {
                int op = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = dec(op);
                module.writeLocalByte(getRegisterPairHLSP(REGP_HL), res);
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("DEC (HL)");
                    debugInfo.setOperands(String.format("->%02Xh", res));
                }
            }
            break;
            case DAA: {
                int op = getRegister(REG_A);
                int correction = 0x00;
                int res;
                if (op > 0x99 || getFlag(CARRY_FLAG)) {
                    correction = 0x60;
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                if ((op & 0x0F) > 0x09 || getFlag(HALF_CARRY_FLAG)) {
                    correction |= 0x06;
                }
                if (getFlag(SUBTRACT_FLAG)) {
                    res = op - correction;
                } else {
                    res = op + correction;
                }
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkParityFlag(res);
                if (BitTest.getBit(op, 4) ^ BitTest.getBit(res, 4)) {
                    setFlag(HALF_CARRY_FLAG);
                } else {
                    clearFlag(HALF_CARRY_FLAG);
                }
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("DAA");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CPL: {
                int op = getRegister(REG_A);
                setRegister(REG_A, ~op);
                setFlag(HALF_CARRY_FLAG);
                setFlag(SUBTRACT_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("CPL");
                    debugInfo.setOperands(null);
                }
            }
            break;

            /**
             * 16-bit-Arithmetikbefehle
             */
            case ADD_BC_HL:
            case ADD_DE_HL:
            case ADD_HL_HL:
            case ADD_SP_HL: {
                int op1 = getRegisterPairHLSP(REGP_HL);
                int op2 = getRegisterPairHLSP((opcode >> 4) & 0x03);
                int res = add16(op1, op2, false);
                setRegisterPairHLSP(REGP_HL, res);
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("ADD HL," + getRegisterPairHLSPString((opcode >> 4) & 0x03));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case INC_BC:
            case INC_DE:
            case INC_HL:
            case INC_SP: {
                int op = getRegisterPairHLSP((opcode >> 4) & 0x03);
                setRegisterPairHLSP((opcode >> 4) & 0x03, op + 1);
                updateTicks(6);
                if (debug) {
                    debugInfo.setCode("INC " + getRegisterPairHLSPString((opcode >> 4) & 0x03));
                    debugInfo.setOperands(String.format("->%04Xh", op + 1));
                }
            }
            break;
            case DEC_BC:
            case DEC_DE:
            case DEC_HL:
            case DEC_SP: {
                int op = getRegisterPairHLSP((opcode >> 4) & 0x03);
                setRegisterPairHLSP((opcode >> 4) & 0x03, op - 1);
                updateTicks(6);
                if (debug) {
                    debugInfo.setCode("DEC " + getRegisterPairHLSPString((opcode >> 4) & 0x03));
                    debugInfo.setOperands(String.format("->%04Xh", op - 1));
                }
            }
            break;

            /**
             * 8-bit-Logikbefehle
             */
            case AND_B:
            case AND_C:
            case AND_D:
            case AND_E:
            case AND_H:
            case AND_L:
            case AND_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = and(op1, op2);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("AND " + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case AND_MEM_HL: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = and(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("AND (HL)");
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case AND_IMM: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = and(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("AND " + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_B:
            case XOR_C:
            case XOR_D:
            case XOR_E:
            case XOR_H:
            case XOR_L:
            case XOR_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = xor(op1, op2);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("XOR " + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_MEM_HL: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = xor(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("XOR (HL)");
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_IMM: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = xor(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("XOR " + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }

            }
            break;
            case OR_B:
            case OR_C:
            case OR_D:
            case OR_E:
            case OR_H:
            case OR_L:
            case OR_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                int res = or(op1, op2);
                setRegister(REG_A, res);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("OR " + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case OR_MEM_HL: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                int res = or(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("OR (HL)");
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case OR_IMM: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                int res = or(op1, op2);
                setRegister(REG_A, res);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("OR " + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case CP_B:
            case CP_C:
            case CP_D:
            case CP_E:
            case CP_H:
            case CP_L:
            case CP_A: {
                int op1 = getRegister(REG_A);
                int op2 = getRegister(opcode & 0x07);
                sub8(op1, op2, false);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("CP " + getRegisterString(opcode & 0x07));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case CP_MEM_HL: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                sub8(op1, op2, false);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("CP (HL)");
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case CP_IMM: {
                int op1 = getRegister(REG_A);
                int op2 = module.readLocalByte(pc++);
                sub8(op1, op2, false);
                updateTicks(7);
                if (debug) {
                    debugInfo.setCode("CP " + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;

            /**
             * Sprungbefehle
             */
            case JP_IMM: {
                pc = module.readLocalWord(pc);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP " + String.format("%04Xh", pc));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_MEM_HL: {
                pc = getRegisterPairHLSP(REGP_HL);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("JP (HL)");
                    debugInfo.setOperands(String.format("%04Xh", pc));
                }
            }
            break;
            case JP_NZ_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(ZERO_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP NZ," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_Z_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(ZERO_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP Z," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_NC_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(CARRY_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP NC," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_C_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(CARRY_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP C," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_PO_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(PARITY_OVERFLOW_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP PO," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_PE_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(PARITY_OVERFLOW_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP PE," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_P_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(SIGN_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP P," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JP_M_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(SIGN_FLAG)) {
                    pc = address;
                }
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("JP M," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JR_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                pc += offset;
                updateTicks(12);
                if (debug) {
                    debugInfo.setCode("JR " + String.format("%02Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", pc));
                }
            }
            break;
            case JR_NZ_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                if (!getFlag(ZERO_FLAG)) {
                    pc += offset;
                    updateTicks(12);
                } else {
                    updateTicks(7);
                }
                if (debug) {
                    debugInfo.setCode("JR NZ," + String.format("%02Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", pc + offset));
                }
            }
            break;
            case JR_Z_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                if (getFlag(ZERO_FLAG)) {
                    pc += offset;
                    updateTicks(12);
                } else {
                    updateTicks(7);
                }
                if (debug) {
                    debugInfo.setCode("JR Z," + String.format("%02Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", pc + offset));
                }
            }
            break;
            case JR_NC_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                if (!getFlag(CARRY_FLAG)) {
                    pc += offset;
                    updateTicks(12);
                } else {
                    updateTicks(7);
                }
                if (debug) {
                    debugInfo.setCode("JR NC," + String.format("%02Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", pc + offset));
                }
            }
            break;
            case JR_C_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                if (getFlag(CARRY_FLAG)) {
                    pc += offset;
                    updateTicks(12);
                } else {
                    updateTicks(7);
                }
                if (debug) {
                    debugInfo.setCode("JR C," + String.format("%02Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", pc + offset));
                }
            }
            break;
            case DJNZ_IMM: {
                int offset = (byte) module.readLocalByte(pc++);
                int op = getRegister(REG_B);
                op = (op - 1) & 0xFF;
                setRegister(REG_B, op);
                if (op != 0) {
                    pc += offset;
                    updateTicks(13);
                } else {
                    updateTicks(8);
                }
                if (debug) {
                    debugInfo.setCode("DJNZ " + offset);
                    debugInfo.setOperands(String.format("%04Xh,%02Xh", pc, op));
                }
            }
            break;

            /**
             * Rufbefehle
             */
            case CALL_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                push(pc);
                pc = address;
                updateTicks(17);
                if (debug) {
                    debugInfo.setCode("CALL " + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_NZ_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(ZERO_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL NZ," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_Z_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(ZERO_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL Z," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_NC_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(CARRY_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL NC," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_C_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(CARRY_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL C," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_PO_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(PARITY_OVERFLOW_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL PO," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_PE_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(PARITY_OVERFLOW_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL PE," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_P_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (!getFlag(SIGN_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL P," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CALL_M_IMM: {
                int address = module.readLocalWord(pc++);
                pc++;
                if (getFlag(SIGN_FLAG)) {
                    push(pc);
                    pc = address;
                    updateTicks(17);
                } else {
                    updateTicks(10);
                }
                if (debug) {
                    debugInfo.setCode("CALL M," + String.format("%04Xh", address));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET: {
                int address = pop();
                pc = address;
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("RET");
                    debugInfo.setOperands(String.format("%04Xh", address));
                }
            }
            break;
            case RET_NZ: {
                if (!getFlag(ZERO_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET NZ");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_Z: {
                if (getFlag(ZERO_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET Z");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_NC: {
                if (!getFlag(CARRY_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET NC");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_C: {
                if (getFlag(CARRY_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET C");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_PO: {
                if (!getFlag(PARITY_OVERFLOW_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET PO");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_PE: {
                if (getFlag(PARITY_OVERFLOW_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET PE");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_P: {
                if (!getFlag(SIGN_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET P");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RET_M: {
                if (getFlag(SIGN_FLAG)) {
                    int address = pop();
                    pc = address;
                    updateTicks(11);
                } else {
                    updateTicks(5);
                }
                if (debug) {
                    debugInfo.setCode("RET M");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case RST_00:
            case RST_08:
            case RST_10:
            case RST_18:
            case RST_20:
            case RST_28:
            case RST_30:
            case RST_38: {
                push(pc);
                pc = opcode & 0x38;
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("RST " + String.format("%02Xh", opcode & 0x38));
                    debugInfo.setOperands(null);
                }

            }
            break;

            /**
             * Prozessorsteuerung
             */
            case NOP: {
                // NOP
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("NOP");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case SCF: {
                setFlag(CARRY_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SCF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case CCF: {
                if (getFlag(CARRY_FLAG)) {
                    clearFlag(CARRY_FLAG);
                } else {
                    setFlag(CARRY_FLAG);
                }
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("CCF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case HALT: {
                // TODO: HALT
                LOG.log(Level.INFO, "Befehl HALT an Adresse {0} noch nicht implementiert", String.format("%04X", pc - 1));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("HALT");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case DI: {
                iff1 = 0;
                iff2 = 0;
                //System.out.println("DI");
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("DI");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case EI: {
                //System.out.println("EI");
                eiWaiting = true;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("EI");
                    debugInfo.setOperands(null);
                }
            }
            break;

            /**
             * Rotations- und Verschiebebefehle
             */
            case RLCA: {
                int op = getRegister(REG_A);
                int res = (op << 1) & 0xFF;
                if (BitTest.getBit(op, 7)) {
                    setFlag(CARRY_FLAG);
                    res |= 0x01;
                } else {
                    clearFlag(CARRY_FLAG);
                }
                setRegister(REG_A, res);
                clearFlag(HALF_CARRY_FLAG);
                clearFlag(SUBTRACT_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("RLCA");
                    debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                }
            }
            break;
            case RRCA: {
                int op = getRegister(REG_A);
                int res = (op >> 1) & 0xFF;
                if (BitTest.getBit(op, 0)) {
                    setFlag(CARRY_FLAG);
                    res |= 0x80;
                } else {
                    clearFlag(CARRY_FLAG);
                }
                setRegister(REG_A, res);
                clearFlag(HALF_CARRY_FLAG);
                clearFlag(SUBTRACT_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("RRCA");
                    debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                }

            }
            break;
            case RLA: {
                int op = getRegister(REG_A);
                int res = (op << 1) & 0xFF;
                if (getFlag(CARRY_FLAG)) {
                    res |= 0x01;
                }
                if (BitTest.getBit(op, 7)) {
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                setRegister(REG_A, res);
                clearFlag(HALF_CARRY_FLAG);
                clearFlag(SUBTRACT_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("RLA");
                    debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                }

            }
            break;
            case RRA: {
                int op = getRegister(REG_A);
                int res = (op >> 1) & 0xFF;
                if (getFlag(CARRY_FLAG)) {
                    res |= 0x80;
                }
                if (BitTest.getBit(op, 0)) {
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                setRegister(REG_A, res);
                clearFlag(HALF_CARRY_FLAG);
                clearFlag(SUBTRACT_FLAG);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("RRA");
                    debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                }

            }
            break;

            //Zweier
            case 0xCB: {
                int opcode2 = module.readLocalByte(pc++);
                switch (opcode2 & 0xF8) {
                    case _CB_RLC: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op << 1) & 0xFF;
                        if (BitTest.getBit(op, 7)) {
                            setFlag(CARRY_FLAG);
                            res |= 0x01;
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("RLC " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_RRC: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op >> 1) & 0xFF;
                        if (BitTest.getBit(op, 0)) {
                            setFlag(CARRY_FLAG);
                            res |= 0x80;
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("RRC " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_RL: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op << 1) & 0xFF;
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x01;
                        }
                        if (BitTest.getBit(op, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("RL " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_RR: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op >> 1) & 0xFF;
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x80;
                        }
                        if (BitTest.getBit(op, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("RR " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_SLA: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op << 1) & 0xFF;
                        if (BitTest.getBit(op, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("SLA " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_SRA: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op >> 1) & 0xFF;
                        if (BitTest.getBit(op, 7)) {
                            res |= 0x80;
                        }
                        if (BitTest.getBit(op, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("SRA " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_SLL: {
                        LOG.log(Level.SEVERE, "Unoffizieller Opcode SLS an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _CB_SRL: {
                        int op = getRegister(opcode2 & 0x07);
                        int res = (op >> 1) & 0xFF;
                        if (BitTest.getBit(op, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setRegister(opcode2 & 0x07, res);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("SRL " + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _CB_BIT0:
                    case _CB_BIT1:
                    case _CB_BIT2:
                    case _CB_BIT3:
                    case _CB_BIT4:
                    case _CB_BIT5:
                    case _CB_BIT6:
                    case _CB_BIT7: {
                        int bit = (opcode2 >> 3) & 0x07;
                        int op = getRegister(opcode2 & 0x07);
                        if (BitTest.getBit(op, bit)) {
                            clearFlag(ZERO_FLAG);
                        } else {
                            setFlag(ZERO_FLAG);
                        }
                        setFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(12);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("BIT " + bit + "," + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(BitTest.getBit(op, bit) ? "1" : "0");
                        }
                    }
                    break;
                    case _CB_RES0:
                    case _CB_RES1:
                    case _CB_RES2:
                    case _CB_RES3:
                    case _CB_RES4:
                    case _CB_RES5:
                    case _CB_RES6:
                    case _CB_RES7: {
                        int bit = (opcode2 >> 3) & 0x07;
                        int op = getRegister(opcode2 & 0x07);
                        setRegister(opcode2 & 0x07, op & (~(0x01 << bit)));
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("RES " + bit + "," + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _CB_SET0:
                    case _CB_SET1:
                    case _CB_SET2:
                    case _CB_SET3:
                    case _CB_SET4:
                    case _CB_SET5:
                    case _CB_SET6:
                    case _CB_SET7: {
                        int bit = (opcode2 >> 3) & 0x07;
                        int op = getRegister(opcode2 & 0x07);
                        setRegister(opcode2 & 0x07, op | (0x01 << bit));
                        if ((opcode2 & 0x07) == 0x06) {
                            updateTicks(15);
                        } else {
                            updateTicks(8);
                        }
                        if (debug) {
                            debugInfo.setCode("SET " + bit + "," + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xDD:
            case 0xFD: {
                boolean useIY = opcode == 0xFD;
                int opcode2 = module.readLocalByte(pc++);
                switch (opcode2) {
                    case _DD_FD_ADD_BC_I:
                    case _DD_FD_ADD_DE_I:
                    case _DD_FD_ADD_I_I:
                    case _DD_FD_ADD_SP_I: {
                        int op1 = getRegisterPairISP((opcode2 >> 4) & 0x03, useIY);
                        int op2 = getRegisterPairISP(REGP_IX_IY, useIY);
                        int res = add16(op1, op2, false);
                        setRegisterPairISP(REGP_IX_IY, res, useIY);
                        updateTicks(15);
                        if (debug) {
                            debugInfo.setCode("ADD " + ((useIY) ? "IY" : "IX") + "," + getRegisterPairISPString((opcode2 >> 4) & 0x03, useIY));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_LD_IMM_I: {
                        setRegisterPairISP(REGP_IX_IY, module.readLocalWord(pc++), useIY);
                        pc++;
                        updateTicks(14);
                        if (debug) {
                            debugInfo.setCode("LD " + ((useIY) ? "IY" : "IX") + "," + String.format("%04Xh", module.readLocalWord(pc - 2)));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _DD_FD_LD_I_MEM: {
                        module.writeLocalWord(module.readLocalWord(pc++), getRegisterPairISP(REGP_IX_IY, useIY));
                        pc++;
                        updateTicks(20);
                        if (debug) {
                            debugInfo.setCode("LD " + String.format("%04Xh", module.readLocalWord(pc - 2)) + "," + ((useIY) ? "IY" : "IX"));
                            debugInfo.setOperands(String.format("%04Xh", getRegisterPairISP(REGP_IX_IY, useIY)));
                        }
                    }
                    break;
                    case _DD_FD_INC_I: {
                        int op = getRegisterPairISP(REGP_IX_IY, useIY);
                        setRegisterPairISP(REGP_IX_IY, op + 1, useIY);
                        updateTicks(10);
                        if (debug) {
                            debugInfo.setCode("INC (" + ((useIY) ? "IY" : "IX") + ")");
                            debugInfo.setOperands(String.format("%04Xh", op + 1));
                        }
                    }
                    break;
                    case _DD_FD_INC_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode INC IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_DEC_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode DEC IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IMM_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IH,imm an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_MEM_I: {
                        setRegisterPairISP(REGP_IX_IY, module.readLocalWord(module.readLocalWord(pc++)), useIY);
                        pc++;
                        updateTicks(20);
                        if (debug) {
                            debugInfo.setCode("LD " + ((useIY) ? "IY" : "IX") + ",(" + String.format("%04Xh", module.readLocalWord(pc - 2)) + ")");
                            debugInfo.setOperands(String.format("%04Xh", module.readLocalWord(module.readLocalWord(pc - 2))));
                        }
                    }
                    break;
                    case _DD_FD_DEC_I: {
                        int op = getRegisterPairISP(REGP_IX_IY, useIY);
                        setRegisterPairISP(REGP_IX_IY, op - 1, useIY);
                        updateTicks(10);
                        if (debug) {
                            debugInfo.setCode("DEC (" + ((useIY) ? "IY" : "IX") + ")");
                            debugInfo.setOperands(String.format("%04Xh", op - 1));
                        }
                    }
                    break;
                    case _DD_FD_INC_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode INC IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_DEC_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode DEC IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IMM_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IL,imm an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_INC_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = inc(op);
                        module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                        updateTicks(23);
                        if (debug) {
                            debugInfo.setCode("INC (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset));
                            debugInfo.setOperands(String.format("%02Xh", res));
                        }
                    }
                    break;
                    case _DD_FD_DEC_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = dec(op);
                        module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                        updateTicks(23);
                        if (debug) {
                            debugInfo.setCode("DEC (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset));
                            debugInfo.setOperands(String.format("%02Xh", res));
                        }
                    }
                    break;
                    case _DD_FD_LD_IMM_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, module.readLocalByte(pc++));
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("LD (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + "," + String.format("%04Xh", module.readLocalByte(pc - 1)));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _DD_FD_LD_IH_B:
                    case _DD_FD_LD_IH_C:
                    case _DD_FD_LD_IH_D:
                    case _DD_FD_LD_IH_E:
                    case _DD_FD_LD_IH_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD r,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IL_B:
                    case _DD_FD_LD_IL_C:
                    case _DD_FD_LD_IL_D:
                    case _DD_FD_LD_IL_E:
                    case _DD_FD_LD_IL_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD r,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_I_0_B:
                    case _DD_FD_LD_I_0_C:
                    case _DD_FD_LD_I_0_D:
                    case _DD_FD_LD_I_0_E:
                    case _DD_FD_LD_I_0_H:
                    case _DD_FD_LD_I_0_L:
                    case _DD_FD_LD_I_0_A: {
                        int offset = (byte) module.readLocalByte(pc++);
                        setRegister((opcode2 >> 3) & 0x07, module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset));
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("LD " + getRegisterString((opcode2 >> 3) & 0x07) + ",(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh", module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset)));
                        }
                    }
                    break;
                    case _DD_FD_LD_B_IH:
                    case _DD_FD_LD_C_IH:
                    case _DD_FD_LD_D_IH:
                    case _DD_FD_LD_E_IH:
                    case _DD_FD_LD_A_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IH,r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IH_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IH,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IL_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IH,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_B_IL:
                    case _DD_FD_LD_C_IL:
                    case _DD_FD_LD_D_IL:
                    case _DD_FD_LD_E_IL:
                    case _DD_FD_LD_A_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IL,r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IH_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IL,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_IL_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode LD IL,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_LD_B_I_0:
                    case _DD_FD_LD_C_I_0:
                    case _DD_FD_LD_D_I_0:
                    case _DD_FD_LD_E_I_0:
                    case _DD_FD_LD_H_I_0:
                    case _DD_FD_LD_L_I_0:
                    case _DD_FD_LD_A_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, getRegister(opcode2 & 0x07));
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("LD " + "(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")," + getRegisterString(opcode2 & 0x07));
                            debugInfo.setOperands(String.format("%02Xh", getRegister(opcode2 & 0x07)));
                        }
                    }
                    break;
                    case _DD_FD_ADD_IH_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode ADD A,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_ADD_IL_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode ADD A,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_ADD_I_0_A: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = add8(op1, op2, false);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("ADD A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_ADC_IH_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode ADC A,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_ADC_IL_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode ADC A,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_ADC_I_0_A: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = add8(op1, op2, true);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("ADC A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_SUB_IH_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode SUB A,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_SUB_IL_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode SUB A,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_SUB_I_0_A: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = sub8(op1, op2, false);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("SUB A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_SBC_IH_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode SBC A,IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_SBC_IL_A: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode SBC A,IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_SBC_I_0_A: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = sub8(op1, op2, true);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("SBC A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_AND_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode AND IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_AND_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode AND IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_AND_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = and(op1, op2);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("AND A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_XOR_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode XOR IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_XOR_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode XOR IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_XOR_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = xor(op1, op2);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("XOR A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_OR_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode OR IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_OR_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode OR IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_OR_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        int res = or(op1, op2);
                        setRegister(REG_A, res);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("ADD A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _DD_FD_CP_IH: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode CP IH an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_CP_IL: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode CP IL an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _DD_FD_CP_I_0: {
                        int offset = (byte) module.readLocalByte(pc++);
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                        sub8(op1, op2, false);
                        updateTicks(19);
                        if (debug) {
                            debugInfo.setCode("ADD A,(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                            debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                        }
                    }
                    break;
                    case _DD_FD_POP_I: {
                        setRegisterPairISP(REGP_IX_IY, pop(), useIY);
                        updateTicks(14);
                    }
                    break;
                    case _DD_FD_EX_SP_I: {
                        int exop = module.readLocalWord(getRegisterPairISP(REGP_SP, useIY));
                        module.writeLocalWord(getRegisterPairISP(REGP_SP, useIY), getRegisterPairISP(REGP_IX_IY, useIY));
                        setRegisterPairISP(REGP_IX_IY, exop, useIY);
                        updateTicks(23);
                        if (debug) {
                            debugInfo.setCode("EX " + ((useIY) ? "IY" : "IX") + "+(SP)");
                            debugInfo.setOperands(String.format("%04Xh,%04Xh", module.readLocalWord(getRegisterPairISP(REGP_SP, useIY)), exop));
                        }
                    }
                    break;
                    case _DD_FD_PUSH_I: {
                        push(getRegisterPairISP(REGP_IX_IY, useIY));
                        updateTicks(15);
                        if (debug) {
                            debugInfo.setCode("PUSH " + ((useIY) ? "IY" : "IX"));
                            debugInfo.setOperands(String.format("%04Xh", getRegisterPairISP(REGP_IX_IY, useIY)));
                        }
                    }
                    break;
                    case _DD_FD_JP_MEM_I: {
                        pc = getRegisterPairISP(REGP_IX_IY, useIY);
                        updateTicks(8);
                        if (debug) {
                            debugInfo.setCode("JP (" + ((useIY) ? "IY" : "IX") + ")");
                            debugInfo.setOperands(String.format("%04Xh", pc));
                        }
                    }
                    break;
                    case _DD_FD_LD_I_SP: {
                        setRegisterPairHLSP(REGP_SP, getRegisterPairISP(REGP_IX_IY, useIY));
                        updateTicks(10);
                        if (debug) {
                            debugInfo.setCode("LD SP," + ((useIY) ? "IY" : "IX"));
                            debugInfo.setOperands(String.format("%04Xh", getRegisterPairISP(REGP_IX_IY, useIY)));
                        }
                    }
                    break;
                    case 0xCB: {
                        int offset = module.readLocalByte(pc++);
                        int opcode3 = module.readLocalByte(pc++);
                        switch (opcode3 & 0xF8) {
                            case _CB_RLC: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op << 1) & 0xFF;
                                    if (BitTest.getBit(op, 7)) {
                                        setFlag(CARRY_FLAG);
                                        res |= 0x01;
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("RLC (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode RCL (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_RRC: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op >> 1) & 0xFF;
                                    if (BitTest.getBit(op, 0)) {
                                        setFlag(CARRY_FLAG);
                                        res |= 0x80;
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("RRC (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode RRC (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_RL: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op << 1) & 0xFF;
                                    if (getFlag(CARRY_FLAG)) {
                                        res |= 0x01;
                                    }
                                    if (BitTest.getBit(op, 7)) {
                                        setFlag(CARRY_FLAG);
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("RL (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode RL (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_RR: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op >> 1) & 0xFF;
                                    if (getFlag(CARRY_FLAG)) {
                                        res |= 0x80;
                                    }
                                    if (BitTest.getBit(op, 0)) {
                                        setFlag(CARRY_FLAG);
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("RR (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode RR (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_SLA: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op << 1) & 0xFF;
                                    if (BitTest.getBit(op, 7)) {
                                        setFlag(CARRY_FLAG);
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("SLA (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode SLA (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_SRA: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op >> 1) & 0xFF;
                                    if (BitTest.getBit(op, 7)) {
                                        res |= 0x80;
                                    }
                                    if (BitTest.getBit(op, 0)) {
                                        setFlag(CARRY_FLAG);
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("SRA (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode SRA (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_SLL: {
                                LOG.log(Level.FINE, "Unoffizieller Opcode SLS an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                            }
                            break;
                            case _CB_SRL: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    int res = (op >> 1) & 0xFF;
                                    if (BitTest.getBit(op, 0)) {
                                        setFlag(CARRY_FLAG);
                                    } else {
                                        clearFlag(CARRY_FLAG);
                                    }
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, res);
                                    clearFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    checkSignFlag8(res);
                                    checkZeroFlag8(res);
                                    checkParityFlag(res);
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("SRL (" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(Integer.toBinaryString(op) + "b->" + Integer.toBinaryString(res) + "b");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode SRL (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_BIT0:
                            case _CB_BIT1:
                            case _CB_BIT2:
                            case _CB_BIT3:
                            case _CB_BIT4:
                            case _CB_BIT5:
                            case _CB_BIT6:
                            case _CB_BIT7: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int bit = (opcode3 >> 3) & 0x07;
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    if (BitTest.getBit(op, bit)) {
                                        clearFlag(ZERO_FLAG);
                                    } else {
                                        setFlag(ZERO_FLAG);
                                    }
                                    setFlag(HALF_CARRY_FLAG);
                                    clearFlag(SUBTRACT_FLAG);
                                    updateTicks(20);
                                    if (debug) {
                                        debugInfo.setCode("BIT " + bit + ",(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(BitTest.getBit(op, bit) ? "1" : "0");
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode BIT (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_RES0:
                            case _CB_RES1:
                            case _CB_RES2:
                            case _CB_RES3:
                            case _CB_RES4:
                            case _CB_RES5:
                            case _CB_RES6:
                            case _CB_RES7: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int bit = (opcode3 >> 3) & 0x07;
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, op & (~(0x01 << bit)));
                                    updateTicks(23);
                                    if (debug) {
                                        debugInfo.setCode("RES " + bit + ",(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(null);
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode RES (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            case _CB_SET0:
                            case _CB_SET1:
                            case _CB_SET2:
                            case _CB_SET3:
                            case _CB_SET4:
                            case _CB_SET5:
                            case _CB_SET6:
                            case _CB_SET7: {
                                if ((opcode3 & 0x07) == 0x06) {
                                    int bit = (opcode3 >> 3) & 0x07;
                                    int op = module.readLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset);
                                    module.writeLocalByte(getRegisterPairISP(REGP_IX_IY, useIY) + offset, op | (0x01 << bit));
                                    updateTicks(20);
                                    if (debug) {
                                        debugInfo.setCode("SET " + bit + ",(" + ((useIY) ? "IY" : "IX") + "+" + String.format("%02Xh", offset) + ")");
                                        debugInfo.setOperands(null);
                                    }
                                } else {
                                    LOG.log(Level.FINE, "Unoffizieller Opcode SET (IX/IY+d)->r an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                                }
                            }
                            break;
                            default:
                                LOG.log(Level.SEVERE, "Ungültiger oder nicht implementierter OPcode {0} an Adresse {1}!", new String[]{String.format("0x%02X,0x%02X", opcode, opcode2), String.format("%04X", pc - 1)});
                                System.exit(0);
                                break;
                        }
                    }
                    break;
                }
            }
            break;
            case 0xED: {
                int opcode2 = module.readLocalByte(pc++);
                switch (opcode2) {
                    case _ED_IN_C_B:
                    case _ED_IN_C_C:
                    case _ED_IN_C_D:
                    case _ED_IN_C_E:
                    case _ED_IN_C_H:
                    case _ED_IN_C_L:
                    case _ED_IN_C_A: {
                        int port = getRegister(REG_C);
                        setRegister((opcode2 >> 3) & 0x07, module.readLocalPort(port));
                        updateTicks(12);
                        if (debug) {
                            debugInfo.setCode("IN " + getRegisterString((opcode2 >> 3) & 0x07) + ",(C)");
                            debugInfo.setOperands(String.format("%02Xh", port));
                        }
                    }
                    break;
                    case _ED_OUT_B_C:
                    case _ED_OUT_C_C:
                    case _ED_OUT_D_C:
                    case _ED_OUT_E_C:
                    case _ED_OUT_H_C:
                    case _ED_OUT_L_C:
                    case _ED_OUT_A_C: {
                        int port = getRegister(REG_C);
                        module.writeLocalPort(port, getRegister((opcode2 >> 3) & 0x07));
                        updateTicks(12);
                        if (debug) {
                            debugInfo.setCode("OUT (C)," + getRegisterString((opcode2 >> 3) & 0x07));
                            debugInfo.setOperands(String.format("%02Xh", port));
                        }
                    }
                    break;
                    case _ED_SBC_BC_HL:
                    case _ED_SBC_DE_HL:
                    case _ED_SBC_HL_HL:
                    case _ED_SBC_SP_HL: {
                        int op1 = getRegisterPairHLSP(REGP_HL);
                        int op2 = getRegisterPairHLSP((opcode2 >> 4) & 0x03);
                        int res = sub16(op1, op2, true);
                        setRegisterPairHLSP(REGP_HL, res);
                        updateTicks(15);
                        if (debug) {
                            debugInfo.setCode("SBC HL," + getRegisterPairHLSPString((opcode2 >> 4) & 0x03));
                            debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _ED_LD_BC_MEM:
                    case _ED_LD_DE_MEM:
                    case _ED_LD_HL_MEM:
                    case _ED_LD_SP_MEM: {
                        int address = module.readLocalWord(pc++);
                        pc++;
                        module.writeLocalWord(address, getRegisterPairHLSP((opcode2 >> 4) & 0x03));
                        updateTicks(20);
                        if (debug) {
                            debugInfo.setCode("LD (" + String.format("%04Xh", address) + ")," + getRegisterPairHLSPString((opcode2 >> 4) & 0x03));
                            debugInfo.setOperands(String.format("%04Xh", getRegisterPairHLSP((opcode2 >> 4) & 0x03)));
                        }
                    }
                    break;
                    case _ED_NEG: {
                        int op = getRegister(REG_A);
                        int res = 0 - op;
                        setFlag(SUBTRACT_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        if (op == 0x00) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        if (op == 0x80) {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        }
                        setRegister(REG_A, res);
                        checkHalfCarryFlagSub(0, op);
                        updateTicks(8);
                        if (debug) {
                            debugInfo.setCode("NEG");
                            debugInfo.setOperands(String.format("%02Xh->%02Xh", op, res));
                        }
                    }
                    break;
                    case _ED_RETN: {
                        pc = pop();
                        iff1 = iff2;
                        updateTicks(14);
                        if (debug) {
                            debugInfo.setCode("RETN");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_IM0: {
                        interruptMode = 0;
                        updateTicks(8);
                        if (debug) {
                            debugInfo.setCode("IM 0");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LD_A_INT: {
                        i = getRegister(REG_A);
                        updateTicks(9);
                        if (debug) {
                            debugInfo.setCode("LD I,A");
                            debugInfo.setOperands(String.format("%02X", getRegister(REG_A)));
                        }
                    }
                    break;
                    case _ED_ADC_BC_HL:
                    case _ED_ADC_DE_HL:
                    case _ED_ADC_HL_HL:
                    case _ED_ADC_SP_HL: {
                        int op1 = getRegisterPairHLSP(REGP_HL);
                        int op2 = getRegisterPairHLSP((opcode2 >> 4) & 0x03);
                        int res = add16(op1, op2, true);
                        setRegisterPairHLSP(REGP_HL, res);
                        updateTicks(15);
                        if (debug) {
                            debugInfo.setCode("ADC (HL)," + getRegisterPairHLSPString((opcode2 >> 4) & 0x03));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _ED_LD_MEM_BC:
                    case _ED_LD_MEM_DE:
                    case _ED_LD_MEM_HL:
                    case _ED_LD_MEM_SP: {
                        int address = module.readLocalWord(pc++);
                        pc++;
                        setRegisterPairHLSP((opcode2 >> 4) & 0x03, module.readLocalWord(address));
                        updateTicks(20);
                        if (debug) {
                            debugInfo.setCode("LD " + getRegisterPairHLSPString((opcode2 >> 4) & 0x03) + ",(" + String.format("%04Xh", address) + ")");
                            debugInfo.setOperands(String.format("%04Xh", module.readLocalWord(address)));
                        }
                    }
                    break;
                    case _ED_RETI: {
                        pc = pop();
                        updateTicks(14);
                        retiExceuted = true;
                        if (debug) {
                            debugInfo.setCode("RETI");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LD_A_R: {
                        r = getRegister(REG_A);
                        updateTicks(9);
                        if (debug) {
                            debugInfo.setCode("LD R,A");
                            debugInfo.setOperands(String.format("%02X", getRegister(REG_A)));
                        }
                    }
                    break;
                    case _ED_IM1: {
                        interruptMode = 1;
                        updateTicks(8);
                        if (debug) {
                            debugInfo.setCode("IM 1");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LD_INT_A: {
                        setRegister(REG_A, i);
                        updateTicks(9);
                        if (debug) {
                            debugInfo.setCode("LD A,I");
                            debugInfo.setOperands(String.format("%02X", i));
                        }
                    }
                    break;
                    case _ED_IM2: {
                        interruptMode = 2;
                        updateTicks(8);
                        if (debug) {
                            debugInfo.setCode("IM 2");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LD_R_A: {
                        setRegister(REG_A, r);
                        updateTicks(9);
                        if (debug) {
                            debugInfo.setCode("LD A,R");
                            debugInfo.setOperands(String.format("%02X", r));
                        }
                    }
                    break;
                    case _ED_RRD: {
                        int op1 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int op2 = getRegister(REG_A);
                        int res1 = (op2 & 0xF0) | (op1 & 0x0F);
                        setRegister(REG_A, res1);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), (op1 >> 4) | (op2 << 4));
                        checkSignFlag8(res1);
                        checkZeroFlag8(res1);
                        clearFlag(HALF_CARRY_FLAG);
                        checkParityFlag(res1);
                        clearFlag(SUBTRACT_FLAG);
                        updateTicks(18);
                        if (debug) {
                            debugInfo.setCode("RRD");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_RLD: {
                        int op1 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int op2 = getRegister(REG_A);
                        int res1 = (op2 & 0xF0) | (op1 >> 4);
                        setRegister(REG_A, res1);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), (op1 << 4) | (op2 & 0x0F));
                        checkSignFlag8(res1);
                        checkZeroFlag8(res1);
                        clearFlag(HALF_CARRY_FLAG);
                        checkParityFlag(res1);
                        clearFlag(SUBTRACT_FLAG);
                        updateTicks(18);
                        if (debug) {
                            debugInfo.setCode("RLD");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_IN_C_F: {
                        int port = getRegister(REG_C);
                        f = 0xFF & module.readLocalPort(port);
                        updateTicks(12);
                        if (debug) {
                            debugInfo.setCode("IN F,(C)");
                            debugInfo.setOperands(String.format("%02Xh", port));
                        }

                        LOG.log(Level.FINE, "Unoffizieller Opcode IN F,(C) an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _ED_OUT_F_C: {
                        LOG.log(Level.FINE, "Unoffizieller Opcode OUT (C),F an Adresse {0} noch nicht implementiert!", String.format("%04X", pc - 1));
                    }
                    break;
                    case _ED_LDI: {
                        module.writeLocalByte(getRegisterPairHLSP(REGP_DE), module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_DE, getRegisterPairHLSP(REGP_DE) + 1);
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("LDI");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_CPI: {
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int res = op1 - op2;
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        setFlag(SUBTRACT_FLAG);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkHalfCarryFlagSub(op1, op2);
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("CPI");
                            debugInfo.setOperands(String.format("%02X,%02X", op1, op2));
                        }
                    }
                    break;
                    case _ED_INI: {
                        int port = getRegister(REG_C);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), module.readLocalPort(port));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) == 0) {
                            setFlag(ZERO_FLAG);
                        } else {
                            clearFlag(ZERO_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("INI");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_OUTI: {
                        int port = getRegister(REG_C);
                        module.writeLocalPort(port, module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) == 0) {
                            setFlag(ZERO_FLAG);
                        } else {
                            clearFlag(ZERO_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("OUTI");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LDD: {
                        module.writeLocalByte(getRegisterPairHLSP(REGP_DE), module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_DE, getRegisterPairHLSP(REGP_DE) - 1);
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("LDD");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_CPD: {
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int res = op1 - op2;
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        setFlag(SUBTRACT_FLAG);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkHalfCarryFlagSub(op1, op2);
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("CPD");
                            debugInfo.setOperands(String.format("%02X,%02X", op1, op2));
                        }
                    }
                    break;
                    case _ED_IND: {
                        int port = getRegister(REG_C);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), module.readLocalPort(port));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) == 0) {
                            setFlag(ZERO_FLAG);
                        } else {
                            clearFlag(ZERO_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("IND");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_OUTD: {
                        int port = getRegister(REG_C);
                        module.writeLocalPort(port, module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) == 0) {
                            setFlag(ZERO_FLAG);
                        } else {
                            clearFlag(ZERO_FLAG);
                        }
                        updateTicks(16);
                        if (debug) {
                            debugInfo.setCode("OTD");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LDIR: {
                        module.writeLocalByte(getRegisterPairHLSP(REGP_DE), module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_DE, getRegisterPairHLSP(REGP_DE) + 1);
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if (getRegisterPairHLSP(REGP_BC) != 0) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            updateTicks(16);
                        }
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(PARITY_OVERFLOW_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        if (debug) {
                            debugInfo.setCode("LDIR");
                            debugInfo.setOperands(String.format("%02X", module.readLocalByte(getRegisterPairHLSP(REGP_HL) - 1)));
                        }
                    }
                    break;
                    case _ED_CPIR: {
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int res = op1 - op2;
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if ((getRegisterPairHLSP(REGP_BC) != 0) && (op1 != op2)) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            updateTicks(16);
                        }
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        setFlag(SUBTRACT_FLAG);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkHalfCarryFlagSub(op1, op2);
                        if (debug) {
                            debugInfo.setCode("CPIR");
                            debugInfo.setOperands(String.format("%02X,%02X", op1, op2));
                        }
                    }
                    break;
                    case _ED_INIR: {
                        int port = getRegister(REG_C);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), module.readLocalPort(port));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) != 0) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            setFlag(ZERO_FLAG);
                            updateTicks(16);
                        }
                        if (debug) {
                            debugInfo.setCode("INIR");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_OTIR: {
                        int port = getRegister(REG_C);
                        module.writeLocalPort(port, module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) + 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) != 0) {
                            pc = pc - 2;
                            updateTicks(21);
                        } else {
                            setFlag(ZERO_FLAG);
                            updateTicks(16);
                        }
                        if (debug) {
                            debugInfo.setCode("OTIR");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_LDDR: {
                        module.writeLocalByte(getRegisterPairHLSP(REGP_DE), module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_DE, getRegisterPairHLSP(REGP_DE) - 1);
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if (getRegisterPairHLSP(REGP_BC) != 0) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            updateTicks(16);
                        }
                        clearFlag(HALF_CARRY_FLAG);
                        clearFlag(SUBTRACT_FLAG);
                        clearFlag(PARITY_OVERFLOW_FLAG);
                        if (debug) {
                            debugInfo.setCode("LDDR");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_CPDR: {
                        int op1 = getRegister(REG_A);
                        int op2 = module.readLocalByte(getRegisterPairHLSP(REGP_HL));
                        int res = op1 - op2;
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegisterPairHLSP(REGP_BC, getRegisterPairHLSP(REGP_BC) - 1);
                        if ((getRegisterPairHLSP(REGP_BC) != 0) && (op1 != op2)) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            updateTicks(16);
                        }
                        if (getRegisterPairHLSP(REGP_BC) == 0) {
                            clearFlag(PARITY_OVERFLOW_FLAG);
                        } else {
                            setFlag(PARITY_OVERFLOW_FLAG);
                        }
                        setFlag(SUBTRACT_FLAG);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkHalfCarryFlagSub(op1, op2);
                        if (debug) {
                            debugInfo.setCode("CPDR");
                            debugInfo.setOperands(String.format("%02X,%02X", op1, op2));
                        }
                    }
                    break;
                    case _ED_INDR: {
                        int port = getRegister(REG_C);
                        module.writeLocalByte(getRegisterPairHLSP(REGP_HL), module.readLocalPort(port));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) != 0) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            setFlag(ZERO_FLAG);
                            updateTicks(16);
                        }
                        if (debug) {
                            debugInfo.setCode("INDR");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    case _ED_OTDR: {
                        int port = getRegister(REG_C);
                        module.writeLocalPort(port, module.readLocalByte(getRegisterPairHLSP(REGP_HL)));
                        setRegisterPairHLSP(REGP_HL, getRegisterPairHLSP(REGP_HL) - 1);
                        setRegister(REG_B, getRegister(REG_B) - 1);
                        setFlag(SUBTRACT_FLAG);
                        if (getRegister(REG_B) != 0) {
                            pc -= 2;
                            updateTicks(21);
                        } else {
                            setFlag(ZERO_FLAG);
                            updateTicks(16);
                        }
                        if (debug) {
                            debugInfo.setCode("OTDR");
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                    default:
                        LOG.log(Level.SEVERE, "Ungültiger oder nicht implementierter OPcode {0} an Adresse {1}!", new String[]{String.format("0x%02X,0x%02X", opcode, opcode2), String.format("%04X", pc - 1)});
                        System.exit(0);
                        break;
                }
            }
            break;
            default:
                LOG.log(Level.SEVERE, "Ungültiger oder nicht implementierter OPcode {0} an Adresse {1}!", new String[]{String.format("%0x02X", opcode), String.format("%04X", pc - 1)});
                System.exit(0);
                break;
        }
        if (debug) {
            if (debugInfo.getCode() != null) {
                debugger.addLine(debugInfo);
                decoder.addItem(debugInfo);
            }
        }
    }

    /**
     * Setzt den Wert eines Registers
     *
     * @param register Register
     * @param value Wert
     */
    private void setRegister(int register, int value) {
        switch (register) {
            case REG_B:
                b = value & 0xFF;
                break;
            case REG_C:
                c = value & 0xFF;
                break;
            case REG_D:
                d = value & 0xFF;
                break;
            case REG_E:
                e = value & 0xFF;
                break;
            case REG_H:
                h = value & 0xFF;
                break;
            case REG_L:
                l = value & 0xFF;
                break;
            case MEM_HL:
                module.writeLocalByte(getRegisterPairHLSP(REGP_HL), value);
                break;
            case REG_A:
                a = value & 0xFF;
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Wert eines Registers zurück
     *
     * @param register Register
     * @return Inhalt des Registers
     */
    private int getRegister(int register) {
        switch (register) {
            case REG_B:
                return b & 0xFF;
            case REG_C:
                return c & 0xFF;
            case REG_D:
                return d & 0xFF;
            case REG_E:
                return e & 0xFF;
            case REG_H:
                return h & 0xFF;
            case REG_L:
                return l & 0xFF;
            case MEM_HL:
                return module.readLocalByte(getRegisterPairHLSP(REGP_HL));
            case REG_A:
                return a & 0xFF;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Liefert einen String, welcher das Register repräsentiert
     *
     * @param register Register
     * @return String
     */
    private String getRegisterString(int register) {
        switch (register) {
            case REG_B:
                return "B";
            case REG_C:
                return "C";
            case REG_D:
                return "D";
            case REG_E:
                return "E";
            case REG_H:
                return "H";
            case REG_L:
                return "L";
            case MEM_HL:
                return "(HL)";
            case REG_A:
                return "A";
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Inhalt eines Registerpaares oder des Stackpointers (BC,DE,HL,SP)
     * als 16-bit Wert zurück
     *
     * @param registerPair Registerpaar
     * @return Inhalt des Registerpaars
     */
    private int getRegisterPairHLSP(int registerPair) {
        switch (registerPair) {
            case REGP_BC:
                return ((b & 0xFF) << 8) | (c & 0xFF);
            case REGP_DE:
                return ((d & 0xFF) << 8) | (e & 0xFF);
            case REGP_HL:
                return ((h & 0xFF) << 8) | (l & 0xFF);
            case REGP_SP:
                return sp;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den String zurück, welcher ein Registerpaar oder den Stackpointer
     * (BC,DE,HL,SP) repräsentiert
     *
     * @param registerPair Registerpaar
     * @return Inhalt des Registerpaars
     */
    private String getRegisterPairHLSPString(int registerPair) {
        switch (registerPair) {
            case REGP_BC:
                return "BC";
            case REGP_DE:
                return "DE";
            case REGP_HL:
                return "HL";
            case REGP_SP:
                return "SP";
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt den Inhalt eines Registerpaares oder des Stackpointers
     * (BC,DE,HL,SP)
     *
     * @param registerPair Registerpaar
     * @param value Wert
     */
    private void setRegisterPairHLSP(int registerPair, int value) {
        switch (registerPair) {
            case REGP_BC:
                c = value & 0xFF;
                b = (value >> 8) & 0xFF;
                break;
            case REGP_DE:
                e = value & 0xFF;
                d = (value >> 8) & 0xFF;
                break;
            case REGP_HL:
                l = value & 0xFF;
                h = (value >> 8) & 0xFF;
                break;
            case REGP_SP:
                sp = value & 0xFFFF;
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Inhalt eines Registerpaares, des Stackpointers oder eines
     * Indexregisters (BC,DE,SP,IX/IY) als 16-bit Wert zurück
     *
     * @param registerPair Registerpaar
     * @param useIY true - IndexRegister IY, false - Indexregister IX
     * @return Inhalt des Registerpaars
     */
    private int getRegisterPairISP(int registerPair, boolean useIY) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_SP:
                return getRegisterPairHLSP(registerPair);
            case REGP_IX_IY:
                return (useIY ? iy : ix);
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den String zurück, welcher ein Registerpaar, den Stackpointer oder
     * ein Indexregister (BC,DE,HL,IX/IY) repräsentiert
     *
     * @param registerPair Registerpaar
     * @param useIY true - IndexRegister IY, false - Indexregister IX
     * @return Inhalt des Registerpaars
     */
    private String getRegisterPairISPString(int registerPair, boolean useIY) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_SP:
                return getRegisterPairHLSPString(registerPair);
            case REGP_IX_IY:
                return (useIY ? "IY" : "IX");
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt den Inhalt eines Registerpaares, des Stackpointers oder eines
     * Indexregisters (BC,DE,SP,IX/IY)
     *
     * @param registerPair Registerpaar
     * @param useIY true - IndexRegister IY, false - Indexregister IX
     * @param value Wert
     */
    private void setRegisterPairISP(int registerPair, int value, boolean useIY) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_SP:
                setRegisterPairHLSP(registerPair, value);
                break;
            case REGP_IX_IY:
                if (useIY) {
                    iy = value & 0xFFFF;
                } else {
                    ix = value & 0xFFFF;
                }
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Inhalt eines Registerpaares (BC,DE,HL,AF) als 16-bit Wert zurück
     *
     * @param registerPair Registerpaar
     * @return Inhalt des Registerpaars
     */
    private int getRegisterPairAF(int registerPair) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_HL:
                return getRegisterPairHLSP(registerPair);
            case REGP_AF:
                return ((a & 0xFF) << 8) | (f & 0xFF);
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den String zurück, welcher ein Registerpaar (BC,DE,HL,AF)
     * repräsentiert
     *
     * @param registerPair Registerpaar
     * @return Inhalt des Registerpaars
     */
    private String getRegisterPairAFString(int registerPair) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_HL:
                return getRegisterPairHLSPString(registerPair);
            case REGP_AF:
                return "AF";
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt den Inhalt eines Registerpaares (BC,DE,HL,AF)
     *
     * @param registerPair Registerpaar
     * @param value Wert
     */
    private void setRegisterPairAF(int registerPair, int value) {
        switch (registerPair) {
            case REGP_BC:
            case REGP_DE:
            case REGP_HL:
                setRegisterPairHLSP(registerPair, value);
                break;
            case REGP_AF:
                f = value & 0xFF;
                a = (value >> 8) & 0xFF;
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Liest ein Wort vom Stack
     *
     * @return Gelesenes Wort
     */
    private int pop() {
        int result = module.readLocalWord(sp);
        setRegisterPairHLSP(REGP_SP, getRegisterPairHLSP(REGP_SP) + 2);
        return result;
    }

    /**
     * Legt ein Wert auf dem Stack ab
     *
     * @param value Zu speicherndes Wort
     */
    private void push(int value) {
        setRegisterPairHLSP(REGP_SP, getRegisterPairHLSP(REGP_SP) - 2);
        module.writeLocalWord(sp, value);
    }

    /**
     * Führt eine UND Operation durch und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @return Ergebnis
     */
    private int and(int op1, int op2) {
        int res = op1 & op2;
        checkSignFlag8(res);
        checkZeroFlag8(res);
        checkParityFlag(res);
        setFlag(HALF_CARRY_FLAG);
        clearFlag(SUBTRACT_FLAG);
        clearFlag(CARRY_FLAG);
        return res;
    }

    /**
     * Führt eine ODER Operation durch und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @return Ergebnis
     */
    private int or(int op1, int op2) {
        int res = op1 | op2;
        checkSignFlag8(res);
        checkZeroFlag8(res);
        checkParityFlag(res);
        clearFlag(HALF_CARRY_FLAG);
        clearFlag(SUBTRACT_FLAG);
        clearFlag(CARRY_FLAG);
        return res;
    }

    /**
     * Führt eine XOR Operation durch und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @return Ergebnis
     */
    private int xor(int op1, int op2) {
        int res = op1 ^ op2;
        checkSignFlag8(res);
        checkZeroFlag8(res);
        checkParityFlag(res);
        setFlag(HALF_CARRY_FLAG);
        clearFlag(SUBTRACT_FLAG);
        clearFlag(CARRY_FLAG);
        return res;
    }

    /**
     * Addiert zwei Bytes und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @param useCarry gibt an, ob ein Carry Flag berücksichtigt werden soll
     * (ADC)
     * @return Ergebnis
     */
    private int add8(int op1, int op2, boolean useCarry) {
        if (useCarry & getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 + op2;
        clearFlag(SUBTRACT_FLAG);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagAdd(op1, op2, res);
        checkHalfCarryFlagAdd(op1, op2);
        checkCarryFlagAdd8(res);
        return res;
    }

    /**
     * Addiert zwei Wörter und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @param useCarry gibt an, ob ein Carry Flag berücksichtigt werden soll
     * (ADC)
     * @return Ergebnis
     */
    private int add16(int op1, int op2, boolean useCarry) {
        if (useCarry & getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 + op2;
        clearFlag(SUBTRACT_FLAG);
        if (useCarry) {
            // Nur bei ADC werden diese Flags gesetzt
            checkZeroFlag16(res);
            checkSignFlag16(res);
        }
        checkHalfCarryFlagAdd(op1 >> 8, op2 >> 8);
        checkCarryFlagAdd16(res);
        return res;
    }

    /**
     * Subtrahiert zwei Bytes und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @param useCarry gibt an, ob ein Carry Flag berücksichtigt werden soll
     * (SBC)
     * @return Ergebnis
     */
    private int sub8(int op1, int op2, boolean useCarry) {
        if (useCarry & getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 - op2;
        setFlag(SUBTRACT_FLAG);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagSub8(op1, op2, res);
        checkHalfCarryFlagSub(op1, op2);
        checkCarryFlagSub(res);
        return res;
    }

    /**
     * Subtrahiert zwei Wörter und setzt die entsprechenden Flags
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @param useCarry gibt an, ob ein Carry Flag berücksichtigt werden soll
     * (SBC)
     * @return Ergebnis
     */
    private int sub16(int op1, int op2, boolean useCarry) {
        if (useCarry & getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 - op2;
        setFlag(SUBTRACT_FLAG);
        checkZeroFlag16(res);
        checkSignFlag16(res);
        checkOverflowFlagSub16(op1, op2, res);
        checkHalfCarryFlagSub(op1 >> 8, op2 >> 8);
        checkCarryFlagSub(res);
        return res;
    }

    /**
     * Inkrementiert einen Operanden und setzt die entsprechenden Flags.
     *
     * @param op Operand
     * @return Ergebnis
     */
    private int inc(int op) {
        int res = op + 1;
        clearFlag(SUBTRACT_FLAG);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagAdd(op, 1, res);
        checkHalfCarryFlagAdd(op, 1);
        return res;
    }

    /**
     * Dekrementiert einen Operanden und setzt die entsprechenden Flags.
     *
     * @param op Operand
     * @return Ergebnis
     */
    private int dec(int op) {
        int res = op - 1;
        setFlag(SUBTRACT_FLAG);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagSub8(op, 1, res);
        checkHalfCarryFlagSub(op, 1);
        return res;
    }

    /**
     * Setzt das Carry Flag entsprechend einer ausgeführten Addition von zwei
     * Bytes.
     *
     * @param res Ergebniss der Addition
     */
    private void checkCarryFlagAdd8(int res) {
        if (res > 0xFF) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    /**
     * Setzt das Carry Flag entsprechend einer ausgeführten Addition von zwei
     * Wörtern.
     *
     * @param res Ergebnis der Addition
     */
    private void checkCarryFlagAdd16(int res) {
        if (res > 0xFFFF) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    /**
     * Setzt das Carry Flag entsprechend einer ausgeführten Subtraktion von zwei
     * Bytes.
     *
     * @param res Ergebniss der Subtraktion
     */
    private void checkCarryFlagSub(int res) {
        if (res < 0x0) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    /**
     * Setzt das Half-Carry-Flag entsprechend einer ausgeführten Subtraktion
     * op1-op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     */
    private void checkHalfCarryFlagSub(int op1, int op2) {
        if ((op1 & 0xF) < (op2 & 0xF)) {
            setFlag(HALF_CARRY_FLAG);
        } else {
            clearFlag(HALF_CARRY_FLAG);
        }
    }

    /**
     * Setzt das Half-Carry-Flag entsprechend einer ausgeführten Addition
     * op1+op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     */
    private void checkHalfCarryFlagAdd(int op1, int op2) {
        if (((op1 & 0xF) + (op2 & 0xF)) > 0xF) {
            setFlag(HALF_CARRY_FLAG);
        } else {
            clearFlag(HALF_CARRY_FLAG);
        }
    }

    /**
     * Setzt das Overflow-Flag entsprechend einer ausgeführten Addition op1+op2.
     *
     * @param op1 1. Operand
     * @param op2 2. Operand
     * @param res Ergebnis
     */
    private void checkOverflowFlagAdd(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x80)) {
            setFlag(PARITY_OVERFLOW_FLAG);
        } else {
            clearFlag(PARITY_OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Overflow-Flag entsprechend einer ausgeführten Subtraktion von
     * zwei Bytes op1-op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     * @param res Ergebnis der Operation
     */
    private void checkOverflowFlagSub8(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x80)) {
            setFlag(PARITY_OVERFLOW_FLAG);
        } else {
            clearFlag(PARITY_OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Overflow Flag entsprechend einer ausgeführten Subtraktion von
     * zwei Wörtern op1-op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     * @param res Ergebnis der Operation
     */
    private void checkOverflowFlagSub16(int op1, int op2, int res) {
        if (((op1 & 0x8000) == 0x8000 && (op2 & 0x8000) == 0x0000 && (res & 0x8000) == 0x0000) | ((op1 & 0x8000) == 0x0000 && (op2 & 0x8000) == 0x8000 && (res & 0x8000) == 0x8000)) {
            setFlag(PARITY_OVERFLOW_FLAG);
        } else {
            clearFlag(PARITY_OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Parity-Flag entsprechend einer vorangehenden Operation.
     *
     * @param res Ergebnis
     */
    private void checkParityFlag(int res) {
        int sum = 0;
        for (int bit = 7; bit >= 0; bit--) {
            if ((((res & 0xFF) >> bit) & 0x01) == 1) {
                sum++;
            }
        }
        if ((sum & 0x01) == 0x01) {
            clearFlag(PARITY_OVERFLOW_FLAG);
        } else {
            setFlag(PARITY_OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Zero-Flag entsprechend einer vorangehenden Byte-Operation.
     *
     * @param res Ergebnis
     */
    private void checkZeroFlag8(int res) {
        if ((res & 0xFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    /**
     * Setzt das Zero-Flag entsprechend einer vorangehenden Wort-Operation.
     *
     * @param res Ergebnis
     */
    private void checkZeroFlag16(int res) {
        if ((res & 0xFFFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    /**
     * Setzt das Sign-Flag entsprechend einer vorangehenden Operation von Byte
     * Operanden.
     *
     * @param res Ergebnis
     */
    private void checkSignFlag8(int res) {
        if (res < 0 || (res & 0x80) == 0x80) {
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

    /**
     * Setzt das Sign-Flag entsprechend einer vorangehenden Operation von Wort
     * Operanden.
     *
     * @param res Ergebnis
     */
    private void checkSignFlag16(int res) {
        if (res < 0 || (res & 0x8000) == 0x8000) {
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

    /**
     * Gibt an, ob ein Flag gesetzt ist.
     *
     * @param flag Zu prüfendes Flag
     * @return <code>true</code> - wenn das entsprechende Flag gesetzt ist,
     * <code>false</code> - sonst
     */
    private boolean getFlag(int flag) {
        return (f & flag) != 0;
    }

    /**
     * Setzt ein Flag
     *
     * @param flag Zu setzendes Flag
     */
    private void setFlag(int flag) {
        f |= flag;
    }

    /**
     * Löscht ein Flag
     *
     * @param flag Zu löschendes Flag
     */
    private void clearFlag(int flag) {
        f &= ~flag;
    }

    /**
     * Aktualisiert die Anzahl der Taktzyklen
     *
     * @param cycles Anzahl der Zyklen
     */
    private void updateTicks(int cycles) {
        tickBuffer -= cycles;
        module.localClockUpdate(cycles);
    }

    /**
     * Führt einen CPU Zyklus aus.
     */
    private void executeCPUCycle() {
        // Prüfe ob ein Busgesucht vorliegt

        // Führe normale Operation durch
        executeNextInstruction();
        if (nmi) {
            nmi = false;
            nmi();
        } else if (iff1 == 1 && iff2 == 1) {
            if (interruptsWaiting.size() > 0) {
                interrupt(interruptsWaiting.pollFirst());
            }
        }

    }

    /**
     * Führt CPU Zyklen gemäß der angegebenen Anzahl der Takte aus.
     *
     * @param ticks Anzahl der Takte
     */
    @Override
    public void executeCycles(int ticks) {
        this.tickBuffer += ticks;

        // Mindestens 4 Takte sind für den kürzesten Befehl nötig
        while (tickBuffer >= 4) {
            if (!busRequest) {
                executeCPUCycle();
            } else {
                // Aktualisiere nur die anderen Komponenten
                updateTicks(4);
//            module.localClockUpdate((int) amountScaled);
            }
        }
    }

    /**
     * Arbeitet einen nichtmaskierbaren Interrupt ab
     */
    private void nmi() {
        if (debugger.isDebug()) {
            debugger.addComment("Verarbeite NMI");
        }
        if (module instanceof KES) {
            System.out.println("Verarbeite NMI");
        }
//        if (BitTest.getBit(kgs.readMemoryByte(0x2803), 4)) {
//            System.out.println("KGS: Splitgrenzen NMI");
//        } else {
//            System.out.println("KGS: Bildende NMI");
//        }

        push(pc);
        iff2 = iff1;
        iff1 = 0;
        pc = 0x0066;
    }

    /**
     * Arbeitet einen Interrupt ab
     *
     * @param irq Interrupt ID
     */
    private void interrupt(int irq) {
        if (debugger.isDebug()) {
            debugger.addComment("Verarbeite Interrupt " + irq);
        }
        iff1 = 0;
        iff2 = 0;
        switch (interruptMode) {
            case 0:
                // TODO Interrupt-Modus 0
                break;
            case 1:
                // TODO Interrupt-Modus 1
                push(pc);
                pc = 0x0038;
                break;
            case 2:
                // TODO Interrupt-Modus 2
                int isr_address = (i << 8) | irq;
                push(pc);

                pc = module.readLocalWord(isr_address);
                //System.out.println("Starte Interrupt an: " + String.format("%04X", pc));
                break;
            default:
                throw new IllegalStateException("Unbekannter Interrupt-Modus!");
        }
    }

    /**
     * Fordert für den nächsten Zyklus die Abarbeitung eines nichtmaskierbaren
     * Interrupts an
     */
    public void requestNMI() {
        this.nmi = true;
    }

    /**
     * Fordert eine Busrequest an oder hebt die Anforderung auf. Die CPU
     * unterbricht Ihre Arbeit und gibt den Bus für das anfordernde Gerät frei.
     *
     * @param request <code>true</code> für eine Anforderung, <code>false</code>
     * bei Freigabe
     */
    public void requestBus(boolean request) {
        this.busRequest = request;
    }

    /**
     * Fordert für den nächsten Zyklus die Abarbeitung eines Interrupts an
     *
     * @param i Interruptnummer
     */
    public void requestInterrupt(int i) {
        //if (iff1 == 1 && iff2 == 1) {
        //System.out.println("Interrupt auf CPU akzeptiert.");
        interruptsWaiting.add(i);
        interruptsWaiting.sort(null);
        //}
    }

    /**
     * Aktiviert oder Deaktiviert den Debugger
     *
     * @param debug true - zum Aktivieren, false- sonst
     */
    @Override
    public void setDebug(boolean debug) {
        debugger.setDebug(debug);
    }

    /**
     * Speichert den Zustand der CPU in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeInt(a);
        dos.writeInt(b);
        dos.writeInt(d);
        dos.writeInt(h);
        dos.writeInt(f);
        dos.writeInt(c);
        dos.writeInt(e);
        dos.writeInt(l);
        dos.writeInt(a_);
        dos.writeInt(b_);
        dos.writeInt(d_);
        dos.writeInt(h_);
        dos.writeInt(f_);
        dos.writeInt(c_);
        dos.writeInt(e_);
        dos.writeInt(l_);
        dos.writeInt(i);
        dos.writeInt(r);
        dos.writeInt(ix);
        dos.writeInt(iy);
        dos.writeInt(sp);
        dos.writeInt(pc);
        dos.writeInt(iff1);
        dos.writeInt(iff2);
        dos.writeBoolean(halt);
        dos.writeBoolean(nmi);
        dos.writeBoolean(busRequest);
        dos.writeInt(interruptMode);
        dos.writeInt(interruptsWaiting.size());
        for (Integer irw : interruptsWaiting) {
            dos.writeInt(irw);
        }
        dos.writeDouble(ticks);
        dos.writeBoolean(retiExceuted);
    }

    /**
     * Lädt den Zustand der CPU aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        a = dis.readInt();
        b = dis.readInt();
        d = dis.readInt();
        h = dis.readInt();
        f = dis.readInt();
        c = dis.readInt();
        e = dis.readInt();
        l = dis.readInt();
        a_ = dis.readInt();
        b_ = dis.readInt();
        d_ = dis.readInt();
        h_ = dis.readInt();
        f_ = dis.readInt();
        c_ = dis.readInt();
        e_ = dis.readInt();
        l_ = dis.readInt();
        i = dis.readInt();
        r = dis.readInt();
        ix = dis.readInt();
        iy = dis.readInt();
        sp = dis.readInt();
        pc = dis.readInt();
        iff1 = dis.readInt();
        iff2 = dis.readInt();
        halt = dis.readBoolean();
        nmi = dis.readBoolean();
        busRequest = dis.readBoolean();
        interruptMode = dis.readInt();
        interruptsWaiting.clear();
        int sizeInt = dis.readInt();
        for (int irw = 0; irw < sizeInt; irw++) {
            interruptsWaiting.add(dis.readInt());
        }
        ticks = dis.readDouble();
        retiExceuted = dis.readBoolean();
    }

    /**
     * Gibt an, ob noch ein NMI ansteht bzw. gerade ein NMI bearbeitet wird.
     *
     * @return <code>true</code> wenn ein NMI in Bearbeitung ist oder ansteht,
     * <code>false</code> sonst
     */
    public boolean isNmiInProgress() {
        return nmiInProgress;
    }

    /**
     * Setzt die CPU zurück und beginnt die Programmabarbeitung neu.
     */
    @Override
    public void reset() {
        interruptMode = 0;
        pc = 0x0000;
    }

    /**
     * Gibt die Instanz des Decoders zurück.
     *
     * @return Decoderinstanz oder <code>null</code> wenn kein Decoder
     * initialisiert ist.
     */
    @Override
    public Decoder getDecoder() {
        return decoder;
    }

    /**
     * Gibt an, ob die CPU ein RETI Return From Interrupt ausgeführt hat.
     *
     * @return <code>true</code> wenn ein RETI ausgeführt wurde,
     * <code>false</code> sonst.
     */
    public boolean isRetiExceuted() {
        if (retiExceuted) {
            retiExceuted = false;
            return true;
        }
        return false;
    }
}
