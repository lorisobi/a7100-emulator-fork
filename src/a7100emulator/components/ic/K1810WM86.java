/*
 * K1810WM86.java
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
 *   04.04.2014 - Kommentare begonnen
 *   26.04.2014 - Prüfen des Carry Flags für Subtraktion zusammengefasst
 *   25.07.2014 - STI um einen Befehl verzögert aktivieren
 *              - stiWaiting speichern
 *   09.08.2014 - Zugriffe auf SystemMemory, SystemPorts und SystemClock durch
 *                MMS16Bus ersetzt
 *   04.09.2014 - updateClock in extra Funktion ausgelagert
 *   16.11.2014 - Funktion zum ausführen einer begrenzten Anzahl von
 *                Instruktionen
 *   18.11.2014 - getBit durch BitTest ersetzt
 *              - Kommentare ergänzt
 *              - Interface IC implementiert
 *   06.01.2015 - Fehler in DAA und DAS behoben
 *   07.01.2015 - Fehler in AAA behoben
 *   01.06.2015 - Debuginformationen MUL korrigiert
 *   03.06.2015 - RCR Overflow-Bit korrigiert
 *   04.06.2015 - Debugstring bei MOD/RM angeglichen
 *   14.07.2015 - Fehler in CMPSW behoben (Quelle und Ziel getauscht)
 *              - Debugausgaben CMPS korrigiert
 *   25.07.2015 - Slowdown und Automatischen Debuggerstart deaktiviert
 *   28.07.2015 - Debugger addLine() wieder aktiviert
 *   05.08.2015 - Debugausgabe MOVS korrigiert
 *   09.08.2015 - Javadoc korrigiert
 *   11.08.2015 - Segment-Präfixe separat behandelt
 *   23.07.2016 - Ausführung von Zyklen überarbeitet
 *              - Von Interface CPU abgeleitet, Runnable entfernt
 *              - Methoden zum Anhalten und Pausieren entfernt
 *   24.07.2016 - reset() und setDebug() nach Interface CPU ausgelagert
 *   26.07.2016 - Überflüssige Codereste entfernt
 *              - Kommentare vervollständigt
 *   28.07.2016 - Decoder Singletoninstanz entfernt
 *   29.07.2016 - try{} catch {} bei Systembedingten dumps hinzugefügt
 */
package a7100emulator.components.ic;

import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.DebuggerInfo;
import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.OpcodeStatistic;
import a7100emulator.Tools.BitTest;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der CPU K1810WM86.
 * <p>
 * Die Klasse steuert die Befehlsabarbeitung und das Interrupt-system der
 * Haupt-CPU. Über <code>executeCycles()</code> Kann die Abarbeitung für eine
 * bestimmte Anzahl von Takten aktiviert werden.
 * <p>
 * Folgende Opcodes werden von der CPU nicht behandelt, da sie in der
 * Dokumentation zur ZVE als (not used) deklariert sind: 0F, 60-6F, C0-C1,
 * C8-C9, D6, F1, 82-83 (REG: 001,100,110), 8C und 8E (REG: 1xx), 8F
 * (REG:001-111), C6-C7 (REG:001-111), D0-D3 (REG:110), C6-C7 (REG:001-111),
 * F6-F7 (REG:001), FE (REG:010-111), FF (REG:111)
 *
 * @author Dirk Bräuer
 */
public final class K1810WM86 implements CPU {

    /**
     * Interrupt System des A7100
     */
    private final InterruptSystem interruptSystem = InterruptSystem.getInstance();
    /**
     * MMS16 - Systembus des A7100
     */
    private final MMS16Bus mms16 = MMS16Bus.getInstance();

    /**
     * Opcode: Segment Präfix Extrasegment-Register ES
     */
    private static final int PREFIX_ES = 0x26;
    /**
     * Opcode: Segment Präfix Codesegment-Register CS
     */
    private static final int PREFIX_CS = 0x2E;
    /**
     * Opcode: Segment Präfix Stacksegment-Register SS
     */
    private static final int PREFIX_SS = 0x36;
    /**
     * Opcode: Segment Präfix Datensegment-Register DS
     */
    private static final int PREFIX_DS = 0x3E;

    /**
     * Opcode: ES auf Stack ablegen
     */
    private static final int PUSH_ES = 0x06;
    /**
     * Opcode: ES von Stack holen
     */
    private static final int POP_ES = 0x07;
    /**
     * Opcode: CS auf Stack ablegen
     */
    private static final int PUSH_CS = 0x0E;
    /**
     * Opcode: SS auf Stack ablegen
     */
    private static final int PUSH_SS = 0x16;
    /**
     * Opcode: SS von Stack holen
     */
    private static final int POP_SS = 0x17;
    /**
     * Opcode: DS auf Stack ablegen
     */
    private static final int PUSH_DS = 0x1E;
    /**
     * Opcode: DS von Stack holen
     */
    private static final int POP_DS = 0x1F;
    /**
     * Opcode: AX auf Stack ablegen
     */
    private static final int PUSH_AX = 0x50;
    /**
     * Opcode: CX auf Stack ablegen
     */
    private static final int PUSH_CX = 0x51;
    /**
     * Opcode: DX auf Stack ablegen
     */
    private static final int PUSH_DX = 0x52;
    /**
     * Opcode: BX auf Stack ablegen
     */
    private static final int PUSH_BX = 0x53;
    /**
     * Opcode: SP auf Stack ablegen
     */
    private static final int PUSH_SP = 0x54;
    /**
     * Opcode: BP auf Stack ablegen
     */
    private static final int PUSH_BP = 0x55;
    /**
     * Opcode: SI auf Stack ablegen
     */
    private static final int PUSH_SI = 0x56;
    /**
     * Opcode: DI auf Stack ablegen
     */
    private static final int PUSH_DI = 0x57;
    /**
     * Opcode: Flagregister auf Stack ablegen
     */
    private static final int PUSHF = 0x9C;
    /**
     * Opcode: AX von Stack holen
     */
    private static final int POP_AX = 0x58;
    /**
     * Opcode: CX von Stack holen
     */
    private static final int POP_CX = 0x59;
    /**
     * Opcode: DX von Stack holen
     */
    private static final int POP_DX = 0x5A;
    /**
     * Opcode: BX von Stack holen
     */
    private static final int POP_BX = 0x5B;
    /**
     * Opcode: SP von Stack holen
     */
    private static final int POP_SP = 0x5C;
    /**
     * Opcode: BP von Stack holen
     */
    private static final int POP_BP = 0x5D;
    /**
     * Opcode: SI von Stack holen
     */
    private static final int POP_SI = 0x5E;
    /**
     * Opcode: DI von Stack holen
     */
    private static final int POP_DI = 0x5F;
    /**
     * Opcode: Flagregister von Stack holen
     */
    private static final int POPF = 0x9D;

    /**
     * Opcode: Kopiere AH nach Flagregister
     */
    private static final int SAHF = 0x9E;
    /**
     * Opcode: Lade AH aus Flagregister
     */
    private static final int LAHF = 0x9F;
    /**
     * Opcode: Kopiere 8Bit Operand aus Register nach Register/Speicher
     */
    private static final int MOV_REG_MODRM_8 = 0x88;
    /**
     * Opcode: Kopiere 16Bit Operand aus Register nach Register/Speicher
     */
    private static final int MOV_REG_MODRM_16 = 0x89;
    /**
     * Opcode: Kopiere 8Bit Operand aus Register/Speicher nach Register
     */
    private static final int MOV_MODRM_REG_8 = 0x8A;
    /**
     * Opcode: Kopiere 16Bit Operand aus Register/Speicher nach Register
     */
    private static final int MOV_MODRM_REG_16 = 0x8B;
    /**
     * Opcode: Kopiere 8Bit aus Speicher nach AL
     */
    private static final int MOV_MEM_AL = 0xA0;
    /**
     * Opcode: Kopiere 16Bit aus Speicher nach AX
     */
    private static final int MOV_MEM_AX = 0xA1;
    /**
     * Opcode: Kopiere 8Bit aus AL nach Speicher
     */
    private static final int MOV_AL_MEM = 0xA2;
    /**
     * Opcode: Kopiere 16Bit aus AX nach Speicher
     */
    private static final int MOV_AX_MEM = 0xA3;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach AL
     */
    private static final int MOV_IMM_AL = 0xB0;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach CL
     */
    private static final int MOV_IMM_CL = 0xB1;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach DL
     */
    private static final int MOV_IMM_DL = 0xB2;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach BL
     */
    private static final int MOV_IMM_BL = 0xB3;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach AH
     */
    private static final int MOV_IMM_AH = 0xB4;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach CH
     */
    private static final int MOV_IMM_CH = 0xB5;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach DH
     */
    private static final int MOV_IMM_DH = 0xB6;
    /**
     * Opcode: Kopiere 8Bit aus direktem Operanden nach BH
     */
    private static final int MOV_IMM_BH = 0xB7;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach AX
     */
    private static final int MOV_IMM_AX = 0xB8;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach CX
     */
    private static final int MOV_IMM_CX = 0xB9;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach DX
     */
    private static final int MOV_IMM_DX = 0xBA;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach BX
     */
    private static final int MOV_IMM_BX = 0xBB;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach SP
     */
    private static final int MOV_IMM_SP = 0xBC;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach BP
     */
    private static final int MOV_IMM_BP = 0xBD;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach SI
     */
    private static final int MOV_IMM_SI = 0xBE;
    /**
     * Opcode: Kopiere 16Bit aus direktem Operanden nach DI
     */
    private static final int MOV_IMM_DI = 0xBF;
    /**
     * Opcode: Vertausche 8Bit Register mit Register/Speicher
     */
    private static final int XCHG_MODRM_REG_8 = 0x86;
    /**
     * Opcode: Vertausche 16Bit Register mit Register/Speicher
     */
    private static final int XCHG_MODRM_REG_16 = 0x87;
    /**
     * Opcode: Vertausche Register CX und AX
     */
    private static final int XCHG_CX_AX = 0x91;
    /**
     * Opcode: Vertausche Register DX und AX
     */
    private static final int XCHG_DX_AX = 0x92;
    /**
     * Opcode: Vertausche Register BX und AX
     */
    private static final int XCHG_BX_AX = 0x93;
    /**
     * Opcode: Vertausche Register SP und AX
     */
    private static final int XCHG_SP_AX = 0x94;
    /**
     * Opcode: Vertausche Register BP und AX
     */
    private static final int XCHG_BP_AX = 0x95;
    /**
     * Opcode: Vertausche Register SI und AX
     */
    private static final int XCHG_SI_AX = 0x96;
    /**
     * Opcode: Vertausche Register DI und AX
     */
    private static final int XCHG_DI_AX = 0x97;
    /**
     * Opcode: Lade Effektive Adresse
     */
    private static final int LEA_MEM_REG = 0x8D;
    /**
     * Opcode: Lade Adresse nach Register und Extrasegment-Register
     */
    private static final int LES_MEM_REG = 0xC4;
    /**
     * Opcode: Lade Adresse nach Register und Datensegment-Register
     */
    private static final int LDS_MEM_REG = 0xC5;
    /**
     * Opcode: Kopiere aus Tabellenzugriff
     */
    private static final int XLAT = 0xD7;

    /**
     * Opcode: Lese 8Bit von direkt angegebenen Port nach AL
     */
    private static final int IN_IMM_AL = 0xE4;
    /**
     * Opcode: Lese 16Bit von direkt angegebenen Port nach AX
     */
    private static final int IN_IMM_AX = 0xE5;
    /**
     * Opcode: Schreibe 8Bit aus AL in direkt angegebenen Port
     */
    private static final int OUT_IMM_AL = 0xE6;
    /**
     * Opcode: Schreibe 16Bit aus AX in direkt angegebenen Port
     */
    private static final int OUT_IMM_AX = 0xE7;
    /**
     * Opcode: Lese 8Bit von Port (DX) nach AL
     */
    private static final int IN_DX_AL = 0xEC;
    /**
     * Opcode: Lese 16Bit von Port (DX) nach AX
     */
    private static final int IN_DX_AX = 0xED;
    /**
     * Opcode: Schreibe 8Bit aus AL in Port (DX)
     */
    private static final int OUT_DX_AL = 0xEE;
    /**
     * Opcode: Schreibe 16Bit aus AX in Port (DX)
     */
    private static final int OUT_DX_AX = 0xEF;

    /**
     * Opcode: Addiere 8Bit Register zu Register/Speicher
     */
    private static final int ADD_REG_MODRM_8 = 0x00;
    /**
     * Opcode: Addiere 16Bit Register zu Register/Speicher
     */
    private static final int ADD_REG_MODRM_16 = 0x01;
    /**
     * Opcode: Addiere 8Bit Register/Speicher zu Register
     */
    private static final int ADD_MODRM_REG_8 = 0x02;
    /**
     * Opcode: Addiere 16Bit Register/Speicher zu Register
     */
    private static final int ADD_MODRM_REG_16 = 0x03;
    /**
     * Opcode: Addiere 8Bit direkten Operanden zu AL
     */
    private static final int ADD_IMM_AL = 0x04;
    /**
     * Opcode: Addiere 16Bit direkten Operanden zu AX
     */
    private static final int ADD_IMM_AX = 0x05;
    /**
     * Opcode: Addiere 8Bit Register zu Register/Speicher mit Übertrag
     */
    private static final int ADC_REG_MODRM_8 = 0x10;
    /**
     * Opcode: Addiere 16Bit Register zu Register/Speicher mit Übertrag
     */
    private static final int ADC_REG_MODRM_16 = 0x11;
    /**
     * Opcode: Addiere 8Bit Register/Speicher zu Register mit Übertrag
     */
    private static final int ADC_MODRM_REG_8 = 0x12;
    /**
     * Opcode: Addiere 16Bit Register/Speicher zu Register mit Übertrag
     */
    private static final int ADC_MODRM_REG_16 = 0x13;
    /**
     * Opcode: Addiere 8Bit direkten Operanden zu AL mit Übertrag
     */
    private static final int ADC_IMM_AL = 0x14;
    /**
     * Opcode: Addiere 16Bit direkten Operanden zu AX mit Übertrag
     */
    private static final int ADC_IMM_AX = 0x15;
    /**
     * Opcode: Subtrahiere 8Bit Register von Register/Speicher
     */
    private static final int SUB_REG_MODRM_8 = 0x28;
    /**
     * Opcode: Subtrahiere 16Bit Register von Register/Speicher
     */
    private static final int SUB_REG_MODRM_16 = 0x29;
    /**
     * Opcode: Subtrahiere 8Bit Register/Speicher von Register
     */
    private static final int SUB_MODRM_REG_8 = 0x2A;
    /**
     * Opcode: Subtrahiere 16Bit Register/Speicher von Register
     */
    private static final int SUB_MODRM_REG_16 = 0x2B;
    /**
     * Opcode: Subtrahiere 8Bit direkten Operanden von AL
     */
    private static final int SUB_IMM_AL = 0x2C;
    /**
     * Opcode: Subtrahiere 16Bit direkten Operanden von AX
     */
    private static final int SUB_IMM_AX = 0x2D;
    /**
     * Opcode: Subtrahiere 8Bit Register von Register/Speicher mit Übertrag
     */
    private static final int SBB_REG_MODRM_8 = 0x18;
    /**
     * Opcode: Subtrahiere 16Bit Register von Register/Speicher mit Übertrag
     */
    private static final int SBB_REG_MODRM_16 = 0x19;
    /**
     * Opcode: Subtrahiere 8Bit Register/Speicher von Register mit Übertrag
     */
    private static final int SBB_MODRM_REG_8 = 0x1A;
    /**
     * Opcode: Subtrahiere 16Bit Register/Speicher von Register mit Übertrag
     */
    private static final int SBB_MODRM_REG_16 = 0x1B;
    /**
     * Opcode: Subtrahiere 8Bit direkten Operanden von AL mit Übertrag
     */
    private static final int SBB_IMM_AL = 0x1C;
    /**
     * Opcode: Subtrahiere 16Bit direkten Operanden von AX mit Übertrag
     */
    private static final int SBB_IMM_AX = 0x1D;
    /**
     * Opcode: Vergleiche 8Bit Register mit Register/Speicher
     */
    private static final int CMP_REG_MODRM_8 = 0x38;
    /**
     * Opcode: Vergleiche 16Bit Register mit Register/Speicher
     */
    private static final int CMP_REG_MODRM_16 = 0x39;
    /**
     * Opcode: Vergleiche 8Bit Register/Speicher mit Register
     */
    private static final int CMP_MODRM_REG_8 = 0x3A;
    /**
     * Opcode: Vergleiche 16Bit Register/Speicher mit Register
     */
    private static final int CMP_MODRM_REG_16 = 0x3B;
    /**
     * Opcode: Vergleiche 8Bit direkten Operanden mit AL
     */
    private static final int CMP_IMM_AL = 0x3C;
    /**
     * Opcode: Vergleiche 16Bit direkten Operanden mit AX
     */
    private static final int CMP_IMM_AX = 0x3D;
    /**
     * Opcode: Dezimalkorrektur nach Addition
     */
    private static final int DAA = 0x27;
    /**
     * Opcode: Dezimalkorrektur nach Subtraktion
     */
    private static final int DAS = 0x2F;
    /**
     * Opcode: BCD-Korrektur nach Addition
     */
    private static final int AAA = 0x37;
    /**
     * Opcode: BCD-Korrektur nach Subtraktion
     */
    private static final int AAS = 0x3F;
    /**
     * Opcode: BCD-Korrektur nach Multiplikation
     */
    private static final int AAM = 0xD4;
    /**
     * Opcode: Korrektur vor BCD-Division
     */
    private static final int AAD = 0xD5;
    /**
     * Opcode: Inkrement AX
     */
    private static final int INC_AX = 0x40;
    /**
     * Opcode: Inkrement CX
     */
    private static final int INC_CX = 0x41;
    /**
     * Opcode: Inkrement DX
     */
    private static final int INC_DX = 0x42;
    /**
     * Opcode: Inkrement BX
     */
    private static final int INC_BX = 0x43;
    /**
     * Opcode: Inkrement SP
     */
    private static final int INC_SP = 0x44;
    /**
     * Opcode: Inkrement BP
     */
    private static final int INC_BP = 0x45;
    /**
     * Opcode: Inkrement SI
     */
    private static final int INC_SI = 0x46;
    /**
     * Opcode: Inkrement DI
     */
    private static final int INC_DI = 0x47;
    /**
     * Opcode: Decrement AX
     */
    private static final int DEC_AX = 0x48;
    /**
     * Opcode: Decrement CX
     */
    private static final int DEC_CX = 0x49;
    /**
     * Opcode: Decrement DX
     */
    private static final int DEC_DX = 0x4A;
    /**
     * Opcode: Decrement BX
     */
    private static final int DEC_BX = 0x4B;
    /**
     * Opcode: Decrement SP
     */
    private static final int DEC_SP = 0x4C;
    /**
     * Opcode: Decrement BP
     */
    private static final int DEC_BP = 0x4D;
    /**
     * Opcode: Decrement SI
     */
    private static final int DEC_SI = 0x4E;
    /**
     * Opcode: Decrement DI
     */
    private static final int DEC_DI = 0x4F;
    /**
     * Opcode: Umwandlung Byte in AL nach Wort in AX
     */
    private static final int CBW = 0x98;
    /**
     * Opcode: Umwandlung Wort in AX nach Doppelwort in DX:AX
     */
    private static final int CWD = 0x99;

    /**
     * Opcode: Zeichenkette Kopieren 8Bit
     */
    private static final int MOVS_8 = 0xA4;
    /**
     * Opcode: Zeichenkette Kopieren 16Bit
     */
    private static final int MOVS_16 = 0xA5;
    /**
     * Opcode: Zeichenkette Vergleich 8Bit
     */
    private static final int CMPS_8 = 0xA6;
    /**
     * Opcode: Zeichenkette Vergleich 16Bit
     */
    private static final int CMPS_16 = 0xA7;
    /**
     * Opcode: Zeichenkette Speichern 8Bit
     */
    private static final int STOS_8 = 0xAA;
    /**
     * Opcode: Zeichenkette Speichern 16Bit
     */
    private static final int STOS_16 = 0xAB;
    /**
     * Opcode: Zeichenkette Laden 8Bit
     */
    private static final int LODS_8 = 0xAC;
    /**
     * Opcode: Zeichenkette Laden 16Bit
     */
    private static final int LODS_16 = 0xAD;
    /**
     * Opcode: Zeichenkette Suchen 8Bit
     */
    private static final int SCAS_8 = 0xAE;
    /**
     * Opcode: Zeichenkette Suchen 16Bit
     */
    private static final int SCAS_16 = 0xAF;
    /**
     * Opcode: Wiederhole String-Operation solange nicht gleich/nicht null
     */
    private static final int REPNE_REPNZ = 0xF2;
    /**
     * Opcode: Wiederhole String-Operation solange CX!=0/gleich/null
     */
    private static final int REP_REPE_REPZ = 0xF3;

    /**
     * Opcode: Bitweises ODER 8Bit Register mit Register/Speicher
     */
    private static final int OR_REG_MODRM_8 = 0x08;
    /**
     * Opcode: Bitweises ODER 16Bit Register mit Register/Speicher
     */
    private static final int OR_REG_MODRM_16 = 0x09;
    /**
     * Opcode: Bitweises ODER 8Bit Register/Speicher mit Register
     */
    private static final int OR_MODRM_REG_8 = 0x0A;
    /**
     * Opcode: Bitweises ODER 16Bit Register/Speicher mit Register
     */
    private static final int OR_MODRM_REG_16 = 0x0B;
    /**
     * Opcode: Bitweises ODER 8Bit direkter Operand mit AL
     */
    private static final int OR_IMM_AL = 0x0C;
    /**
     * Opcode: Bitweises ODER 16Bit direkter Operand mit AX
     */
    private static final int OR_IMM_AX = 0x0D;
    /**
     * Opcode: Bitweises UND 8Bit Register mit Register/Speicher
     */
    private static final int AND_REG_MODRM_8 = 0x20;
    /**
     * Opcode: Bitweises UND 16Bit Register mit Register/Speicher
     */
    private static final int AND_REG_MODRM_16 = 0x21;
    /**
     * Opcode: Bitweises UND 8Bit Register/Speicher mit Register
     */
    private static final int AND_MODRM_REG_8 = 0x22;
    /**
     * Opcode: Bitweises UND 16Bit Register/Speicher mit Register
     */
    private static final int AND_MODRM_REG_16 = 0x23;
    /**
     * Opcode: Bitweises UND 8Bit direkter Operand mit AL
     */
    private static final int AND_IMM_AL = 0x24;
    /**
     * Opcode: Bitweises UND 16Bit direkter Operand mit AX
     */
    private static final int AND_IMM_AX = 0x25;
    /**
     * Opcode: Bitweises XOR 8Bit Register mit Register/Speicher
     */
    private static final int XOR_REG_MODRM_8 = 0x30;
    /**
     * Opcode: Bitweises XOR 16Bit Register mit Register/Speicher
     */
    private static final int XOR_REG_MODRM_16 = 0x31;
    /**
     * Opcode: Bitweises XOR 8Bit Register/Speicher mit Register
     */
    private static final int XOR_MODRM_REG_8 = 0x32;
    /**
     * Opcode: Bitweises XOR 16Bit Register/Speicher mit Register
     */
    private static final int XOR_MODRM_REG_16 = 0x33;
    /**
     * Opcode: Bitweises XOR 8Bit direkter Operand mit AL
     */
    private static final int XOR_IMM_AL = 0x34;
    /**
     * Opcode: Bitweises XOR 16Bit direkter Operand mit AX
     */
    private static final int XOR_IMM_AX = 0x35;
    /**
     * Opcode: Bitweises Testen 8Bit Register mit Register/Speicher
     */
    private static final int TEST_REG_MODRM_8 = 0x84;
    /**
     * Opcode: Bitweises Testen 16Bit Register mit Register/Speicher
     */
    private static final int TEST_REG_MODRM_16 = 0x85;
    /**
     * Opcode: Bitweises Testen 8Bit direkter Operand mit AL
     */
    private static final int TEST_IMM_AL = 0xA8;
    /**
     * Opcode: Bitweises Testen 16Bit direkter Operand mit AX
     */
    private static final int TEST_IMM_AX = 0xA9;

    /**
     * Opcode: Sprung, wenn Überlauf
     */
    private static final int JO = 0x70;
    /**
     * Opcode: Sprung, wenn kein Überlauf
     */
    private static final int JNO = 0x71;
    /**
     * Opcode: Sprung, wenn darunter/nicht darüber oder gleich/Übertrag
     */
    private static final int JB_JNAE_JC = 0x72;
    /**
     * Opcode: Sprung, wenn nicht darunter/kein Übertrag/darüber oder gleich
     */
    private static final int JNB_JAE_JNC = 0x73;
    /**
     * Opcode: Sprung, wenn gleich/null
     */
    private static final int JE_JZ = 0x74;
    /**
     * Opcode: Sprung, wenn nicht gleich/nicht null
     */
    private static final int JNE_JNZ = 0x75;
    /**
     * Opcode: Sprung, wenn darunter oder gleich/nicht darüber
     */
    private static final int JBE_JNA = 0x76;
    /**
     * Opcode: Sprung, wenn nicht darunter oder gleich/darüber
     */
    private static final int JNBE_JA = 0x77;
    /**
     * Opcode: Sprung, wenn Vorzeichen
     */
    private static final int JS = 0x78;
    /**
     * Opcode: Sprung, wenn kein Vorzeichen
     */
    private static final int JNS = 0x79;
    /**
     * Opcode: Sprung, wenn Parität/Parität gerade
     */
    private static final int JP_JPE = 0x7A;
    /**
     * Opcode: Sprung, wenn keine Parität/Parität ungerade
     */
    private static final int JNP_JPO = 0x7B;
    /**
     * Opcode: Sprung, wenn kleiner/nicht größer oder gleich
     */
    private static final int JL_JNGE = 0x7C;
    /**
     * Opcode: Sprung, wenn nicht kleiner/größer oder gleich
     */
    private static final int JNL_JGE = 0x7D;
    /**
     * Opcode: Sprung, wenn kleiner oder gleich/nicht größer
     */
    private static final int JLE_JNG = 0x7E;
    /**
     * Opcode: Sprung, wenn nicht kleiner oder gleich/größer
     */
    private static final int JNLE_JG = 0x7F;
    /**
     * Opcode: Sprung, wenn CX=0
     */
    private static final int JCXZ = 0xE3;

    /**
     * Opcode: Sprung innerhalb des Segments 8Bit
     */
    private static final int JMP_SHORT_LABEL = 0xEB;
    /**
     * Opcode: Sprung innerhalb des Segments 16Bit
     */
    private static final int JMP_NEAR_LABEL = 0xE9;
    /**
     * Opcode: Sprung mit Segmentwechsel
     */
    private static final int JMP_FAR_LABEL = 0xEA;

    /**
     * Opcode: Unterprogrammaufruf ohne Segmentwechsel
     */
    private static final int CALL_NEAR_PROC = 0xE8;
    /**
     * Opcode: Unterprogrammaufruf mit Segmentwechsel
     */
    private static final int CALL_FAR_PROC = 0x9A;
    /**
     * Opcode: Rückkehr aus Unterprogramm ohne Segmentwechsel
     */
    private static final int RET_IN_SEG = 0xC3;
    /**
     * Opcode: Rückkehr aus Unterprogramm ohne Segmentwechsel mit Versatz
     */
    private static final int RET_IN_SEG_IMM = 0xC2;
    /**
     * Opcode: Rückkehr aus Unterprogramm mit Segmentwechsel
     */
    private static final int RET_INTER_SEG = 0xCB;
    /**
     * Opcode: Rückkehr aus Unterprogramm mit Segmentwechsel mit Versatz
     */
    private static final int RET_INTER_SEG_IMM = 0xCA;

    /**
     * Opcode: Interrupt 3
     */
    private static final int INT3 = 0xCC;
    /**
     * Opcode: Interrupt aus direktem Operanden
     */
    private static final int INT_IMM = 0xCD;
    /**
     * Opcode: Interrupt Überlauf
     */
    private static final int INTO = 0xCE;
    /**
     * Opcode: Rückkehr aus Interrupt
     */
    private static final int IRET = 0xCF;

    /**
     * Opcode: Wiederhole solange CX!=0 und nicht null
     */
    private static final int LOOPNE_LOOPNZ = 0xE0;
    /**
     * Opcode: Wiederhole solange CX!=0 und null
     */
    private static final int LOOPE_LOOPZ = 0xE1;
    /**
     * Opcode: Wiederhole solgange CX!=0
     */
    private static final int LOOP = 0xE2;

    /**
     * Opcode: Keine Operation
     */
    private static final int NOP = 0x90;
    /**
     * Opcode: Warte bis Signalleitung TEST aktiv
     */
    private static final int WAIT = 0x9B;
    /**
     * Opcode: Aktiviere LOCK Signal
     */
    private static final int LOCK = 0xF0;
    /**
     * Opcode: Prozessorhalt
     */
    private static final int HLT = 0xF4;
    /**
     * Opcode: Escape (Speicherzugriff) op0
     */
    private static final int ESC0 = 0xD8;
    /**
     * Opcode: Escape (Speicherzugriff) op1
     */
    private static final int ESC1 = 0xD9;
    /**
     * Opcode: Escape (Speicherzugriff) op2
     */
    private static final int ESC2 = 0xDA;
    /**
     * Opcode: Escape (Speicherzugriff) op3
     */
    private static final int ESC3 = 0xDB;
    /**
     * Opcode: Escape (Speicherzugriff) op4
     */
    private static final int ESC4 = 0xDC;
    /**
     * Opcode: Escape (Speicherzugriff) op5
     */
    private static final int ESC5 = 0xDD;
    /**
     * Opcode: Escape (Speicherzugriff) op6
     */
    private static final int ESC6 = 0xDE;
    /**
     * Opcode: Escape (Speicherzugriff) op7
     */
    private static final int ESC7 = 0xDF;

    /**
     * Opcode: Komplementiere Carry-Flag
     */
    private static final int CMC = 0xF5;
    /**
     * Opcode: Lösche Carry-Flag
     */
    private static final int CLC = 0xF8;
    /**
     * Opcode: Setze Carry-Flag
     */
    private static final int STC = 0xF9;
    /**
     * Opcode: Lösche Interrupt-Flag
     */
    private static final int CLI = 0xFA;
    /**
     * Opcode: Setze Interrupt-Flag
     */
    private static final int STI = 0xFB;
    /**
     * Opcode: Lösche Richtungs-Flag
     */
    private static final int CLD = 0xFC;
    /**
     * Opcode: Setze Richtungs-Flag
     */
    private static final int STD = 0xFD;

    /**
     * (0x80) 2. Opcode: Addiere 8Bit direkten Operanden zu Register
     * vorzeichenlos
     */
    private static final int _80_ADD_IMM_REG_8_NOSIGN = 0x00;
    /**
     * (0x80) 2. Opcode: Bitweises ODER 8Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _80_OR_IMM_REG_8_NOSIGN = 0x08;
    /**
     * (0x80) 2. Opcode: Addiere 8Bit direkten Operanden zu Register
     * vorzeichenlos mit Übertrag
     */
    private static final int _80_ADC_IMM_REG_8_NOSIGN = 0x10;
    /**
     * (0x80) 2. Opcode: Subtrahiere 8Bit direkten Operanden von Register
     * vorzeichenlos mit Übertrag
     */
    private static final int _80_SBB_IMM_REG_8_NOSIGN = 0x18;
    /**
     * (0x80) 2. Opcode: Bitweises UND 8Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _80_AND_IMM_REG_8_NOSIGN = 0x20;
    /**
     * (0x80) 2. Opcode: Subtrahiere 8Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _80_SUB_IMM_REG_8_NOSIGN = 0x28;
    /**
     * (0x80) 2. Opcode: Bitweises XOR 8Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _80_XOR_IMM_REG_8_NOSIGN = 0x30;
    /**
     * (0x80) 2. Opcode: Vergleiche 8Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _80_CMP_IMM_REG_8_NOSIGN = 0x38;
    /**
     * (0x81) 2. Opcode: Addiere 16Bit direkten Operanden zu Register
     * vorzeichenlos
     */
    private static final int _81_ADD_IMM_REG_16_NOSIGN = 0x00;
    /**
     * (0x81) 2. Opcode: Bitweises ODER 16Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _81_OR_IMM_REG_16_NOSIGN = 0x08;
    /**
     * (0x81) 2. Opcode: Addiere 16Bit direkten Operanden zu Register
     * vorzeichenlos mit Übertrag
     */
    private static final int _81_ADC_IMM_REG_16_NOSIGN = 0x10;
    /**
     * (0x81) 2. Opcode: Subtrahiere 16Bit direkten Operanden von Register
     * vorzeichenlos mit Übertrag
     */
    private static final int _81_SBB_IMM_REG_16_NOSIGN = 0x18;
    /**
     * (0x81) 2. Opcode: Bitweises UND 16Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _81_AND_IMM_REG_16_NOSIGN = 0x20;
    /**
     * (0x81) 2. Opcode: Subtrahiere 16Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _81_SUB_IMM_REG_16_NOSIGN = 0x28;
    /**
     * (0x81) 2. Opcode: Bitweises XOR 16Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _81_XOR_IMM_REG_16_NOSIGN = 0x30;
    /**
     * (0x81) 2. Opcode: Vergleiche 16Bit direkten Operanden von Register
     * vorzeichenlos
     */
    private static final int _81_CMP_IMM_REG_16_NOSIGN = 0x38;
    /**
     * (0x82) 2. Opcode: Addiere 8Bit direkten Operanden zu Register
     * vorzeichenbehaftet
     */
    private static final int _82_ADD_IMM_REG_8_SIGN = 0x00;
    /**
     * (0x82) 2. Opcode: Subtrahiere 8Bit direkten Operanden von Register
     * vorzeichenbehaftet mit Übertrag
     */
    private static final int _82_ADC_IMM_REG_8_SIGN = 0x10;
    /**
     * (0x82) 2. Opcode: Subtrahiere 8Bit direkten Operanden von Register
     * vorzeichenbehaftet mit Übertrag
     */
    private static final int _82_SBB_IMM_REG_8_SIGN = 0x18;
    /**
     * (0x82) 2. Opcode: Subtrahiere 8Bit direkten Operanden von Register
     * vorzeichenbehaftet
     */
    private static final int _82_SUB_IMM_REG_8_SIGN = 0x28;
    /**
     * (0x82) 2. Opcode: Vergleiche 8Bit direkten Operanden von Register
     * vorzeichenbehaftet
     */
    private static final int _82_CMP_IMM_REG_8_SIGN = 0x38;
    /**
     * (0x83) 2. Opcode: Addiere 16Bit direkten Operanden zu Register
     * vorzeichenbehaftet
     */
    private static final int _83_ADD_IMM_REG_16_SIGN = 0x00;
    /**
     * (0x83) 2. Opcode: Addiere 16Bit direkten Operanden zu Register
     * vorzeichenbehaftet mit Übertrag
     */
    private static final int _83_ADC_IMM_REG_16_SIGN = 0x10;
    /**
     * (0x83) 2. Opcode: Subtrahiere 16Bit direkten Operanden von Register
     * vorzeichenbehaftet mit Übertrag
     */
    private static final int _83_SBB_IMM_REG_16_SIGN = 0x18;
    /**
     * (0x83) 2. Opcode: Subtrahiere 16Bit direkten Operanden von Register
     * vorzeichenbehaftet
     */
    private static final int _83_SUB_IMM_REG_16_SIGN = 0x28;
    /**
     * (0x83) 2. Opcode: Vergleiche 16Bit direkten Operanden von Register
     * vorzeichenbehaftet
     */
    private static final int _83_CMP_IMM_REG_16_SIGN = 0x38;

    /**
     * (0x8C) 2. Opcode: Kopiere ES nach Register/Speicher
     */
    private static final int _8C_MOV_ES_MODRM_16 = 0x00;
    /**
     * (0x8C) 2. Opcode: Kopiere CS nach Register/Speicher
     */
    private static final int _8C_MOV_CS_MODRM_16 = 0x08;
    /**
     * (0x8C) 2. Opcode: Kopiere SS nach Register/Speicher
     */
    private static final int _8C_MOV_SS_MODRM_16 = 0x10;
    /**
     * (0x8C) 2. Opcode: Kopiere DS nach Register/Speicher
     */
    private static final int _8C_MOV_DS_MODRM_16 = 0x18;
    /**
     * (0x8E) 2. Opcode: Kopiere Register/Speicher nach ES
     */
    private static final int _8E_MOV_MODRM_ES_16 = 0x00;
    /**
     * (0x8E) 2. Opcode: Kopiere Register/Speicher nach CS
     */
    private static final int _8E_MOV_MODRM_CS_16 = 0x08;
    /**
     * (0x8E) 2. Opcode: Kopiere Register/Speicher nach SS
     */
    private static final int _8E_MOV_MODRM_SS_16 = 0x10;
    /**
     * (0x8E) 2. Opcode: Kopiere Register/Speicher nach DS
     */
    private static final int _8E_MOV_MODRM_DS_16 = 0x18;
    /**
     * (0x8F) 2. Opcode: Register/Speicher von Stack holen
     */
    private static final int _8F_POP_MODRM_16 = 0x00;
    /**
     * (0xC6) 2. Opcode: Kopiere 8Bit direkten Operanden nach Speicher
     */
    private static final int _C6_MOV_IMM_MEM_8 = 0x00;
    /**
     * (0xC7) 2. Opcode: Kopiere 16Bit direkten Operanden nach Speicher
     */
    private static final int _C7_MOV_IMM_MEM_16 = 0x00;

    /**
     * (0xD0) 2. Opcode: Linksrotieren 8Bit Register/Speicher um 1
     */
    private static final int _D0_ROL_MODRM_1_8 = 0x00;
    /**
     * (0xD0) 2. Opcode: Rechtsrotieren 8Bit Register/Speicher um 1
     */
    private static final int _D0_ROR_MODRM_1_8 = 0x08;
    /**
     * (0xD0) 2. Opcode: Linksrotieren 8Bit mit Carry Register/Speicher um 1
     */
    private static final int _D0_RCL_MODRM_1_8 = 0x10;
    /**
     * (0xD0) 2. Opcode: Rechtsrotieren 8Bit mit Carry Register/Speicher um 1
     */
    private static final int _D0_RCR_MODRM_1_8 = 0x18;
    /**
     * (0xD0) 2. Opcode: Arithmetisches/logisches Linksverschieben 8Bit
     * Register/Speicher um 1
     */
    private static final int _D0_SAL_SHL_MODRM_1_8 = 0x20;
    /**
     * (0xD0) 2. Opcode: Logisches Rechtsverschieben 8Bit Register/Speicher um 1
     */
    private static final int _D0_SHR_MODRM_1_8 = 0x28;
    /**
     * (0xD0) 2. Opcode: Arithmetisches Rechtsverschieben 8Bit Register/Speicher
     * um 1
     */
    private static final int _D0_SAR_MODRM_1_8 = 0x38;
    /**
     * (0xD1) 2. Opcode: Linksrotieren 16Bit Register/Speicher um 1
     */
    private static final int _D1_ROL_MODRM_1_16 = 0x00;
    /**
     * (0xD1) 2. Opcode: Rechtsrotieren 16Bit Register/Speicher um 1
     */
    private static final int _D1_ROR_MODRM_1_16 = 0x08;
    /**
     * (0xD1) 2. Opcode: Linksrotieren 16Bit mit Carry Register/Speicher um 1
     */
    private static final int _D1_RCL_MODRM_1_16 = 0x10;
    /**
     * (0xD1) 2. Opcode: Rechtsrotieren 16Bit mit Carry Register/Speicher um 1
     */
    private static final int _D1_RCR_MODRM_1_16 = 0x18;
    /**
     * (0xD1) 2. Opcode: Arithmetisches/logisches Linksverschieben 16Bit
     * Register/Speicher um 1
     */
    private static final int _D1_SAL_SHL_MODRM_1_16 = 0x20;
    /**
     * (0xD1) 2. Opcode: Logisches Rechtsverschieben 16Bit Register/Speicher um
     * 1
     */
    private static final int _D1_SHR_MODRM_1_16 = 0x28;
    /**
     * (0xD1) 2. Opcode: Arithmetisches Rechtsverschieben 16Bit
     * Register/Speicher um 1
     */
    private static final int _D1_SAR_MODRM_1_16 = 0x38;
    /**
     * (0xD2) 2. Opcode: Linksrotieren 8Bit Register/Speicher um CL
     */
    private static final int _D2_ROL_MODRM_CL_8 = 0x00;
    /**
     * (0xD2) 2. Opcode: Rechtsrotieren 8Bit Register/Speicher um CL
     */
    private static final int _D2_ROR_MODRM_CL_8 = 0x08;
    /**
     * (0xD2) 2. Opcode: Linksrotieren 8Bit mit Carry Register/Speicher um CL
     */
    private static final int _D2_RCL_MODRM_CL_8 = 0x10;
    /**
     * (0xD2) 2. Opcode: Rechtsrotieren 8Bit mit Carry Register/Speicher um CL
     */
    private static final int _D2_RCR_MODRM_CL_8 = 0x18;
    /**
     * (0xD2) 2. Opcode: Arithmetisches/logisches Linksverschieben 8Bit
     * Register/Speicher um CL
     */
    private static final int _D2_SAL_SHL_MODRM_CL_8 = 0x20;
    /**
     * (0xD2) 2. Opcode: Logisches Rechtsverschieben 8Bit Register/Speicher um
     * CL
     */
    private static final int _D2_SHR_MODRM_CL_8 = 0x28;
    /**
     * (0xD2) 2. Opcode: Arithmetisches Rechtsverschieben 8Bit Register/Speicher
     * um CL
     */
    private static final int _D2_SAR_MODRM_CL_8 = 0x38;
    /**
     * (0xD3) 2. Opcode: Linksrotieren 16Bit Register/Speicher um CL
     */
    private static final int _D3_ROL_MODRM_CL_16 = 0x00;
    /**
     * (0xD3) 2. Opcode: Rechtsrotieren 16Bit Register/Speicher um CL
     */
    private static final int _D3_ROR_MODRM_CL_16 = 0x08;
    /**
     * (0xD3) 2. Opcode: Linksrotieren 16Bit mit Carry Register/Speicher um CL
     */
    private static final int _D3_RCL_MODRM_CL_16 = 0x10;
    /**
     * (0xD3) 2. Opcode: Rechtsrotieren 16Bit mit Carry Register/Speicher um CL
     */
    private static final int _D3_RCR_MODRM_CL_16 = 0x18;
    /**
     * (0xD3) 2. Opcode: Arithmetisches/logisches Linksverschieben 16Bit
     * Register/Speicher um CL
     */
    private static final int _D3_SAL_SHL_MODRM_CL_16 = 0x20;
    /**
     * (0xD3) 2. Opcode: Logisches Rechtsverschieben 16Bit Register/Speicher um
     * CL
     */
    private static final int _D3_SHR_MODRM_CL_16 = 0x28;
    /**
     * (0xD3) 2. Opcode: Arithmetisches Rechtsverschieben 16Bit
     * Register/Speicher um CL
     */
    private static final int _D3_SAR_MODRM_CL_16 = 0x38;

    /**
     * (0xF6) 2. Opcode: Bitweises Testen 8Bit direkter Operand
     * Register/Speicher
     */
    private static final int _F6_TEST_IMM_MODRM_8 = 0x00;
    /**
     * (0xF6) 2. Opcode: Logisches NOT (Einerkomplement) 8Bit Register/Speicher
     */
    private static final int _F6_NOT_MODRM_8 = 0x10;
    /**
     * (0xF6) 2. Opcode: Negieren (Zweierkomplement) 8Bit Register/Speicher
     */
    private static final int _F6_NEG_MODRM_8 = 0x18;
    /**
     * (0xF6) 2. Opcode: Multiplikation 8Bit ohne Vorzeichen
     */
    private static final int _F6_MUL_MODRM_8 = 0x20;
    /**
     * (0xF6) 2. Opcode: Multiplikation 8Bit mit Vorzeichen
     */
    private static final int _F6_IMUL_MODRM_8 = 0x28;
    /**
     * (0xF6) 2. Opcode: Division 8Bit ohne Vorzeichen
     */
    private static final int _F6_DIV_MODRM_8 = 0x30;
    /**
     * (0xF6) 2. Opcode: Division 8Bit mit Vorzeichen
     */
    private static final int _F6_IDIV_MODRM_8 = 0x38;
    /**
     * (0xF7) 2. Opcode: Bitweises Testen 16Bit direkter Operand
     * Register/Speicher
     */
    private static final int _F7_TEST_IMM_MODRM_16 = 0x00;
    /**
     * (0xF7) 2. Opcode: Logisches NOT (Einerkomplement) 16Bit Register/Speicher
     */
    private static final int _F7_NOT_MODRM_16 = 0x10;
    /**
     * (0xF7) 2. Opcode: Negieren (Zweierkomplement) 16Bit Register/Speicher
     */
    private static final int _F7_NEG_MODRM_16 = 0x18;
    /**
     * (0xF7) 2. Opcode: Multiplikation 16Bit ohne Vorzeichen
     */
    private static final int _F7_MUL_MODRM_16 = 0x20;
    /**
     * (0xF7) 2. Opcode: Multiplikation 16Bit mit Vorzeichen
     */
    private static final int _F7_IMUL_MODRM_16 = 0x28;
    /**
     * (0xF7) 2. Opcode: Division 16Bit ohne Vorzeichen
     */
    private static final int _F7_DIV_MODRM_16 = 0x30;
    /**
     * (0xF7) 2. Opcode: Division 16Bit mit Vorzeichen
     */
    private static final int _F7_IDIV_MODRM_16 = 0x38;

    /**
     * (0xFE) 2. Opcode: Inkrement 8Bit Speicher/Register
     */
    private static final int _FE_INC_MODRM_8 = 0x00;
    /**
     * (0xFE) 2. Opcode: Dekrement 8Bit Speicher/Register
     */
    private static final int _FE_DEC_MODRM_8 = 0x08;

    /**
     * (0xFF) 2. Opcode: Inkrement 16Bit Speicher/Register
     */
    private static final int _FF_INC_MEM_16 = 0x00;
    /**
     * (0xFF) 2. Opcode: Dekrement 16Bit Speicher/Register
     */
    private static final int _FF_DEC_MEM_16 = 0x08;
    /**
     * (0xFF) 2. Opcode: Unterprogrammaufruf Register/Speicher
     */
    private static final int _FF_CALL_MODRM_16 = 0x10;
    /**
     * (0xFF) 2. Opcode: Unterprogrammaufruf Ziel aus Register/Speicher
     */
    private static final int _FF_CALL_MEM_16 = 0x18;
    /**
     * (0xFF) 2. Opcode: Sprung Register/Speicher
     */
    private static final int _FF_JMP_MODRM_16 = 0x20;
    /**
     * (0xFF) 2. Opcode: Sprung Ziel aus Register/Speicher
     */
    private static final int _FF_JMP_MEM_16 = 0x28;
    /**
     * (0xFF) 2. Opcode: Speicher Adresse aus Register/Speicher auf Stack
     */
    private static final int _FF_PUSH_MEM_16 = 0x30;

    /**
     * Register Mode/Memory Mode: Kein Versatz
     */
    private static final int MOD_MEM_NO_DISPL = 0x00;
    /**
     * Register Mode/Memory Mode: 8Bit Versatz
     */
    private static final int MOD_MEM_8_DISPL = 0x40;
    /**
     * Register Mode/Memory Mode: 16Bit Versatz
     */
    private static final int MOD_MEM_16_DISPL = 0x80;
    /**
     * Register Mode/Memory Mode: Registerzugriff
     */
    private static final int MOD_REG = 0xC0;

    /**
     * Registerfeld: AL/AX
     */
    private static final int REG_AL_AX = 0x00;
    /**
     * Registerfeld: CL/CX
     */
    private static final int REG_CL_CX = 0x08;
    /**
     * Registerfeld: DL/DX
     */
    private static final int REG_DL_DX = 0x10;
    /**
     * Registerfeld: BL/BX
     */
    private static final int REG_BL_BX = 0x18;
    /**
     * Registerfeld: AH/SP
     */
    private static final int REG_AH_SP = 0x20;
    /**
     * Registerfeld: CH/BP
     */
    private static final int REG_CH_BP = 0x28;
    /**
     * Registerfeld: DH/SI
     */
    private static final int REG_DH_SI = 0x30;
    /**
     * Registerfeld: BH/DI
     */
    private static final int REG_BH_DI = 0x38;

    /**
     * Segmentregister: Extra-Segment
     */
    private static final int SREG_ES = 0x00;
    /**
     * Segmentregister: Code-Segment
     */
    private static final int SREG_CS = 0x01;
    /**
     * Segmentregister: Stack-Segment
     */
    private static final int SREG_SS = 0x02;
    /**
     * Segmentregister: Daten-Segment
     */
    private static final int SREG_DS = 0x03;

    /**
     * Registeroperand: (BX)+(SI)
     */
    private static final int RM_BX_SI = 0x00;
    /**
     * Registeroperand: (BX)+(DI)
     */
    private static final int RM_BX_DI = 0x01;
    /**
     * Registeroperand: (BP)+(SI)
     */
    private static final int RM_BP_SI = 0x02;
    /**
     * Registeroperand: (BX)+(DI)
     */
    private static final int RM_BP_DI = 0x03;
    /**
     * Registeroperand: (SI)
     */
    private static final int RM_SI = 0x04;
    /**
     * Registeroperand: (DI)
     */
    private static final int RM_DI = 0x05;
    /**
     * Registeroperand: Direkte Adressierung oder (BP)
     */
    private static final int RM_DIRECT_BP = 0x06;
    /**
     * Registeroperand: (BX)
     */
    private static final int RM_BX = 0x07;

    /**
     * Maske: Teste Opcode Register Mode/Memory Mode
     */
    private static final int TEST_MOD = 0xC0;
    /**
     * Maske: Teste Opcode Registeroperand/Erweiterung Opcode
     */
    private static final int TEST_REG = 0x38;
    /**
     * Maske: Teste Opcode Registeroperand
     */
    private static final int TEST_RM = 0x07;

    /**
     * Maske: Carry-Flag aus Flagregister
     */
    private static final int CARRY_FLAG = 0x0001;
    /**
     * Maske: Paritäts-Flag aus Flagregister
     */
    private static final int PARITY_FLAG = 0x0004;
    /**
     * Maske: Auxiliary-Carry-Flag aus Flagregister
     */
    private static final int AUXILIARY_CARRY_FLAG = 0x0010;
    /**
     * Maske: Zero-Flag aus Flagregister
     */
    private static final int ZERO_FLAG = 0x0040;
    /**
     * Maske: Vorzeichen-Flag aus Flagregister
     */
    private static final int SIGN_FLAG = 0x0080;
    /**
     * Maske: Trap-Flag aus Flagregister
     */
    private static final int TRAP_FLAG = 0x0100;
    /**
     * Maske: Interrupt-Flag aus Flagregister
     */
    private static final int INTERRUPT_ENABLE_FLAG = 0x0200;
    /**
     * Maske: Richtungs-Flag aus Flagregister
     */
    private static final int DIRECTION_FLAG = 0x0400;
    /**
     * Maske: Überlauf-Flag aus Flagregister
     */
    private static final int OVERFLOW_FLAG = 0x0800;

    /**
     * Kein Segment Präfix
     */
    private static final int NO_PREFIX = 0x00;

    /**
     * Akku-Register
     */
    private int ax;
    /**
     * Basis-Register
     */
    private int bx;
    /**
     * Count-Register
     */
    private int cx;
    /**
     * Daten-Register
     */
    private int dx;
    /**
     * Stackpointer
     */
    private int sp;
    /**
     * Basispointer
     */
    private int bp;
    /**
     * Source Indexregister
     */
    private int si;
    /**
     * Destination Indexregister
     */
    private int di;
    /**
     * Instruktionpointer
     */
    private int ip;
    /**
     * Statusregister
     */
    private int flags;
    /**
     * Codesegment-Register
     */
    private int cs;
    /**
     * Datensegment Register
     */
    private int ds;
    /**
     * Stacksegment Register
     */
    private int ss;
    /**
     * Extrasegment Register
     */
    private int es;
    /**
     * Aktueller Segmentpräfix
     */
    private int prefix = NO_PREFIX;
    /**
     * Aktueller Stringpräfix
     */
    private int string_prefix = NO_PREFIX;
    /**
     * Haltemodus
     */
    private boolean isHalted = false;
    /**
     * Zeiger auf Opcode-Statistik
     */
    private final OpcodeStatistic opCodeStatistic = OpcodeStatistic.getInstance();
    /**
     * Zeiger auf Decoder Instanz
     */
    private final Decoder decoder = new Decoder("K1810WM86", true, "ZVE");
    /**
     * Zeiger auf Debugger Instanz
     */
    private final Debugger debugger = new Debugger("K1810WM86", true, "ZVE");
    /**
     * Zeiger auf Debugger Informationen
     */
    private final DebuggerInfo debugInfo = new DebuggerInfo();
    /**
     * Gibt an, ob nach dem nächsten Befehl das Interrupt-Flag gesetzt wird
     */
    private boolean stiWaiting = false;
    /**
     * Zählt die Takte der aktuellen Befehlsausführung
     */
    private int tickBuffer = 0;

    /**
     * Erzeugt eine neue CPU
     */
    public K1810WM86() {
        debugger.setDebug(false);
    }

    /**
     * Führt die nächste Instruktion an der Adresse CS:IP aus
     */
    private void executeNextInstruction() {
        int opcode1 = mms16.readMemoryByte((cs << 4) + ip++);

        // Aktualisiere Zeit für Laden des Befehls
        // TODO: Timing nochmals überarbeiten/prüfen
        updateTicks(3);

        // Prüfe auf Segment Präfix
        prefix = NO_PREFIX;
        switch (opcode1) {
            /*
             * Präfix
             */
            case PREFIX_ES:
                debugInfo.setCode(null);
                prefix = PREFIX_ES;
                updateTicks(2);
                opcode1 = mms16.readMemoryByte((cs << 4) + ip++);
                break;
            case PREFIX_CS:
                debugInfo.setCode(null);
                prefix = PREFIX_CS;
                updateTicks(2);
                opcode1 = mms16.readMemoryByte((cs << 4) + ip++);
                break;
            case PREFIX_SS:
                debugInfo.setCode(null);
                prefix = PREFIX_SS;
                updateTicks(2);
                opcode1 = mms16.readMemoryByte((cs << 4) + ip++);
                break;
            case PREFIX_DS:
                debugInfo.setCode(null);
                prefix = PREFIX_DS;
                updateTicks(2);
                opcode1 = mms16.readMemoryByte((cs << 4) + ip++);
                break;
        }

        boolean debug = debugger.isDebug();

        if (debug && (opcode1 != PREFIX_CS && opcode1 != PREFIX_SS && opcode1 != PREFIX_DS && opcode1 != PREFIX_ES)) {
            debugInfo.setCs(cs);
            debugInfo.setIp(ip - 1 - (prefix == NO_PREFIX ? 0 : 1));
            debugInfo.setOpcode(opcode1);
        }

        opCodeStatistic.addStatistic(opcode1);

        // Aktiviere Interrupt Flag nach letztem STI
        // Beim 8086 um einen Zyklus verzögert
        if (stiWaiting) {
            flags |= INTERRUPT_ENABLE_FLAG;
            stiWaiting = false;
        }

        switch (opcode1) {
            /*
             * Datentransfer
             */
            case MOV_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, getReg8(opcode2 & TEST_REG), true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("MOV " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh", getReg8(opcode2 & TEST_REG)));
                }
            }
            break;
            case MOV_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getReg16(opcode2 & TEST_REG), true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh", getReg16(opcode2 & TEST_REG)));
                }
            }
            break;
            case MOV_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg8(opcode2 & TEST_REG, op1);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("MOV " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh", op1));
                }
            }
            break;
            case MOV_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, op1);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("MOV " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case MOV_MEM_AL: {
                int segment = getSegment(ds);
                int offset = mms16.readMemoryWord((cs << 4) + ip++);
                ip++;
                setReg8(REG_AL_AX, mms16.readMemoryByte((segment << 4) + offset));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("MOV AL," + getSegmentDebugString("DS") + ":" + String.format("%04Xh", offset));
                    debugInfo.setOperands(String.format("%02Xh", mms16.readMemoryByte((segment << 4) + offset)));
                }
            }
            break;
            case MOV_MEM_AX: {
                int segment = getSegment(ds);
                int offset = mms16.readMemoryWord((cs << 4) + ip++);
                ip++;
                setReg16(REG_AL_AX, mms16.readMemoryWord((segment << 4) + offset));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("MOV AX," + getSegmentDebugString("DS") + ":" + String.format("%04Xh", offset));
                    debugInfo.setOperands(String.format("%04Xh", mms16.readMemoryWord((segment << 4) + offset)));
                }
            }
            break;
            case MOV_AL_MEM: {
                int segment = getSegment(ds);
                int offset = mms16.readMemoryWord((cs << 4) + ip++);
                ip++;
                mms16.writeMemoryByte((segment << 4) + offset, getReg8(REG_AL_AX));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("MOV " + getSegmentDebugString("DS") + ":" + String.format("%04Xh", offset) + ",AL");
                    debugInfo.setOperands(String.format("%02Xh", getReg8(REG_AL_AX)));
                }
            }
            break;
            case MOV_AX_MEM: {
                int segment = getSegment(ds);
                int offset = mms16.readMemoryWord((cs << 4) + ip++);
                ip++;
                mms16.writeMemoryWord((segment << 4) + offset, getReg16(REG_AL_AX));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("MOV " + getSegmentDebugString("DS") + ":" + String.format("%04Xh", offset) + ",AX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AL_AX)));
                }
            }
            break;
            case MOV_IMM_AL: {
                setReg8(REG_AL_AX, mms16.readMemoryByte((cs << 4) + ip++));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV AL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_CL: {
                setReg8(REG_CL_CX, mms16.readMemoryByte((cs << 4) + ip++));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV CL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_DL: {
                setReg8(REG_DL_DX, mms16.readMemoryByte((cs << 4) + ip++));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV DL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_BL: {
                setReg8(REG_BL_BX, mms16.readMemoryByte((cs << 4) + (ip++)));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV BL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_AH: {
                setReg8(REG_AH_SP, mms16.readMemoryByte((cs << 4) + (ip++)));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV AH," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_CH: {
                setReg8(REG_CH_BP, mms16.readMemoryByte((cs << 4) + (ip++)));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV CH," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_DH: {
                setReg8(REG_DH_SI, mms16.readMemoryByte((cs << 4) + (ip++)));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV DH," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_BH: {
                setReg8(REG_BH_DI, mms16.readMemoryByte((cs << 4) + (ip++)));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV BH," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_AX: {
                setReg16(REG_AL_AX, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV AX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_CX: {
                setReg16(REG_CL_CX, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV CX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_DX: {
                setReg16(REG_DL_DX, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV DX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_BX: {
                setReg16(REG_BL_BX, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV BX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_SP: {
                setReg16(REG_AH_SP, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV SP," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_BP: {
                setReg16(REG_CH_BP, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV BP," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_SI: {
                setReg16(REG_DH_SI, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV SI," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case MOV_IMM_DI: {
                setReg16(REG_BH_DI, mms16.readMemoryWord((cs << 4) + (ip++)));
                ip++;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("MOV DI," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case XCHG_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                setReg8(opcode2 & TEST_REG, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XCHG " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case XCHG_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                setReg16(opcode2 & TEST_REG, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XCHG " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;
            case XCHG_CX_AX: {
                int op1 = getReg16(REG_CL_CX);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_CL_CX, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,CX");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_DX_AX: {
                int op1 = getReg16(REG_DL_DX);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_DL_DX, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,DX");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_BX_AX: {
                int op1 = getReg16(REG_BL_BX);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_BL_BX, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,BX");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_SP_AX: {
                int op1 = getReg16(REG_AH_SP);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_AH_SP, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,SP");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_BP_AX: {
                int op1 = getReg16(REG_CH_BP);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_CH_BP, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,BP");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_SI_AX: {
                int op1 = getReg16(REG_DH_SI);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_DH_SI, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,SI");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case XCHG_DI_AX: {
                int op1 = getReg16(REG_BH_DI);
                int op2 = getReg16(REG_AL_AX);
                setReg16(REG_BH_DI, op2);
                setReg16(REG_AL_AX, op1);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("XCHG AX,DI");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op2, op1));
                }
            }
            break;
            case PUSH_ES: {
                push(getSReg(SREG_ES));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("PUSH ES");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_ES)));
                }
            }
            break;
            case PUSH_CS: {
                push(getSReg(SREG_CS));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("PUSH CS");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_CS)));
                }
            }
            break;
            case PUSH_SS: {
                push(getSReg(SREG_SS));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("PUSH SS");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_SS)));
                }
            }
            break;
            case PUSH_DS: {
                push(getSReg(SREG_DS));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("PUSH DS");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_DS)));
                }
            }
            break;
            case PUSH_AX: {
                push(getReg16(REG_AL_AX));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH AX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AL_AX)));
                }
            }
            break;
            case PUSH_CX: {
                push(getReg16(REG_CL_CX));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH CX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_CL_CX)));
                }
            }
            break;
            case PUSH_DX: {
                push(getReg16(REG_DL_DX));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH DX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_DL_DX)));
                }
            }
            break;
            case PUSH_BX: {
                push(getReg16(REG_BL_BX));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH BX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_BL_BX)));
                }
            }
            break;
            case PUSH_SP: {
                mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH SP");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AH_SP)));
                }
            }
            break;
            case PUSH_BP: {
                push(getReg16(REG_CH_BP));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH BP");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_CH_BP)));
                }
            }
            break;
            case PUSH_SI: {
                push(getReg16(REG_DH_SI));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH SI");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_DH_SI)));
                }
            }
            break;
            case PUSH_DI: {
                push(getReg16(REG_BH_DI));
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("PUSH DI");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_BH_DI)));
                }
            }
            break;
            case POP_ES: {
                setSReg(SREG_ES, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP ES");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_ES)));
                }
            }
            break;
            case POP_SS: {
                setSReg(SREG_SS, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP SS");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_SS)));
                }
            }
            break;
            case POP_DS: {
                setSReg(SREG_DS, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP DS");
                    debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_DS)));
                }
            }
            break;
            case POP_AX: {
                setReg16(REG_AL_AX, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP AX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AL_AX)));
                }
            }
            break;
            case POP_CX: {
                setReg16(REG_CL_CX, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP CX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_CL_CX)));
                }
            }
            break;
            case POP_DX: {
                setReg16(REG_DL_DX, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP DX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_DL_DX)));
                }
            }
            break;
            case POP_BX: {
                setReg16(REG_BL_BX, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP BX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_BL_BX)));
                }
            }
            break;
            case POP_SP: {
                setReg16(REG_AH_SP, mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP)));
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP SP");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AH_SP)));
                }
            }
            break;
            case POP_BP: {
                setReg16(REG_CH_BP, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP BP");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_CH_BP)));
                }
            }
            break;
            case POP_SI: {
                setReg16(REG_DH_SI, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP SI");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_DH_SI)));
                }
            }
            break;
            case POP_DI: {
                setReg16(REG_BH_DI, pop());
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POP DI");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_BH_DI)));
                }
            }
            break;
            case PUSHF: {
                push(flags);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("PUSHF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case POPF: {
                flags = pop();
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("POPF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case SAHF: {
                flags &= 0xFF00;
                flags |= getReg8(REG_AH_SP) & 0xFF;
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SAHF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case LAHF: {
                setReg8(REG_AH_SP, flags & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("LAHF");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case LEA_MEM_REG: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int offset = getOffset(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, offset & 0xFFFF);
                updateTicks(2 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("LEA " + getReg16DebugString(opcode2 & TEST_REG) + "," + getOffsetDebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", offset));
                }
            }
            break;
            case LES_MEM_REG: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int address = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, mms16.readMemoryWord(address));
                setSReg(SREG_ES, mms16.readMemoryWord(address + 2));
                updateTicks(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("LES " + getReg16DebugString(opcode2 & TEST_REG) + "," + getAddressMODRMDebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", mms16.readMemoryWord(address), mms16.readMemoryWord(address + 2)));
                }
            }
            break;
            case LDS_MEM_REG: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int address = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, mms16.readMemoryWord(address));
                setSReg(SREG_DS, mms16.readMemoryWord(address + 2));
                updateTicks(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("LDS " + getReg16DebugString(opcode2 & TEST_REG) + "," + getAddressMODRMDebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", mms16.readMemoryWord(address), mms16.readMemoryWord(address + 2)));
                }
            }
            break;
            case XLAT: {
                int segment = getSegment(ds);
                int value = mms16.readMemoryByte((segment << 4) + getReg16(REG_BL_BX) + getReg8(REG_AL_AX));
                setReg8(REG_AL_AX, value);
                updateTicks(11);
                if (debug) {
                    debugInfo.setCode("XLAT " + getSegmentDebugString("DS"));
                    debugInfo.setOperands(String.format("->%02Xh", value));
                }
            }
            break;
            case IN_IMM_AL: {
                int port = mms16.readMemoryByte((cs << 4) + (ip++));
                int value = mms16.readIOByte(port);
                setReg8(REG_AL_AX, value);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("IN " + Integer.toHexString(port) + "h, AL");
                    debugInfo.setOperands(String.format("->%02Xh", value));
                }
            }
            break;
            case IN_IMM_AX: {
                int port = mms16.readMemoryByte((cs << 4) + (ip++));
                int value = mms16.readIOWord(port);
                setReg16(REG_AL_AX, value);
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("IN " + Integer.toHexString(port) + "h, AX");
                    debugInfo.setOperands(String.format("->%04Xh", value));
                }
            }
            break;
            case IN_DX_AL: {
                setReg8(REG_AL_AX, mms16.readIOByte(getReg16(REG_DL_DX)));
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("IN AL,DX");
                    debugInfo.setOperands(String.format("%04Xh->%02Xh", getReg16(REG_DL_DX), getReg8(REG_AL_AX)));
                }
            }
            break;
            case IN_DX_AX: {
                setReg16(REG_AL_AX, mms16.readIOWord(getReg16(REG_DL_DX)));
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("IN AX,DX");
                    debugInfo.setOperands(String.format("%04Xh->%04Xh", getReg16(REG_DL_DX), getReg16(REG_AL_AX)));
                }
            }
            break;
            case OUT_IMM_AL: {
                int port = mms16.readMemoryByte((cs << 4) + (ip++));
                mms16.writeIOByte(port, (byte) getReg8(REG_AL_AX));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("OUT " + Integer.toHexString(port) + "h,AL");
                    debugInfo.setOperands(String.format("%02Xh", getReg8(REG_AL_AX)));
                }
            }
            break;
            case OUT_IMM_AX: {
                int port = mms16.readMemoryByte((cs << 4) + ip++);
                mms16.writeIOWord(port, (short) getReg16(REG_AL_AX));
                updateTicks(10);
                if (debug) {
                    debugInfo.setCode("OUT " + Integer.toHexString(port) + "h,AX");
                    debugInfo.setOperands(String.format("%04Xh", getReg16(REG_AL_AX)));
                }
            }
            break;
            case OUT_DX_AL: {
                mms16.writeIOByte(getReg16(REG_DL_DX), getReg8(REG_AL_AX));
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("OUT DX,AL");
                    debugInfo.setOperands(String.format("%04Xh,%02Xh", getReg16(REG_DL_DX), getReg8(REG_AL_AX)));
                }
            }
            break;
            case OUT_DX_AX: {
                mms16.writeIOWord(getReg16(REG_DL_DX), (short) getReg16(REG_AL_AX));
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("OUT DX,AX");
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", getReg16(REG_DL_DX), getReg16(REG_AL_AX)));
                }
            }
            break;

            /**
             * Arithmetik
             */
            case ADD_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + ip++);
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = add8(op1, op2, false);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADD " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADD_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = add16(op1, op2, false);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADD " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case ADD_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = add8(op1, op2, false);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADD " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADD_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = add16(op1, op2, false);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADD " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case ADD_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = add8(op1, op2, false);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADD AL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADD_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = add16(op1, op2, false);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADD AX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case ADC_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = add8(op1, op2, true);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADC " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = add16(op1, op2, true);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADC " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case ADC_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = add8(op1, op2, true);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADC " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = add16(op1, op2, true);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("ADC " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case ADC_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = add8(op1, op2, true);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADC AL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case ADC_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = add16(op1, op2, true);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("ADC AX," + String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case SUB_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = sub8(op1, op2, false);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SUB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = sub16(op1, op2, false);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SUB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case SUB_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = sub8(op1, op2, false);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SUB " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = sub16(op1, op2, false);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SUB " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case SUB_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = sub8(op1, op2, false);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SUB AL," + String.format("%02Xh", mms16.readMemoryByte((cs << 4) + ip - 1)));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SUB_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = sub16(op1, op2, false);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SUB AX," + String.format("%02Xh", mms16.readMemoryWord((cs << 4) + ip - 2)));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case CMP_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                sub8(op1, op2, false);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("CMP " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case CMP_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                sub16(op1, op2, false);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("CMP " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;
            case CMP_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                sub8(op1, op2, false);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("CMP " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case CMP_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                sub16(op1, op2, false);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("CMP " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;
            case CMP_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                sub8(op1, op2, false);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("CMP AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case CMP_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                sub16(op1, op2, false);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("CMP AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;
            case SBB_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = sub8(op1, op2, true);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SBB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBB_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = sub16(op1, op2, true);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SBB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case SBB_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = sub8(op1, op2, true);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SBB " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBB_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = sub16(op1, op2, true);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("SBB " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case SBB_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = sub8(op1, op2, true);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SBB AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case SBB_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = sub16(op1, op2, true);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("SBB AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case INC_AX: {
                int res = inc16(getReg16(REG_AL_AX));
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC AX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_CX: {
                int res = inc16(getReg16(REG_CL_CX));
                setReg16(REG_CL_CX, res & 0xFFFF);
                if (debug) {
                    debugInfo.setCode("INC CX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_DX: {
                int res = inc16(getReg16(REG_DL_DX));
                setReg16(REG_DL_DX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC DX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_BX: {
                int res = inc16(getReg16(REG_BL_BX));
                setReg16(REG_BL_BX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC BX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_SP: {
                int res = inc16(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC SP");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_BP: {
                int res = inc16(getReg16(REG_CH_BP));
                setReg16(REG_CH_BP, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC BP");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_SI: {
                int res = inc16(getReg16(REG_DH_SI));
                setReg16(REG_DH_SI, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC SI");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case INC_DI: {
                int res = inc16(getReg16(REG_BH_DI));
                setReg16(REG_BH_DI, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("INC DI");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_AX: {
                int res = dec16(getReg16(REG_AL_AX));
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC AX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_CX: {
                int res = dec16(getReg16(REG_CL_CX));
                setReg16(REG_CL_CX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC CX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_DX: {
                int res = dec16(getReg16(REG_DL_DX));
                setReg16(REG_DL_DX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC DX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_BX: {
                int res = dec16(getReg16(REG_BL_BX));
                setReg16(REG_BL_BX, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC BX");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_SP: {
                int res = dec16(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC SP");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_BP: {
                int res = dec16(getReg16(REG_CH_BP));
                setReg16(REG_CH_BP, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC BP");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_SI: {
                int res = dec16(getReg16(REG_DH_SI));
                setReg16(REG_DH_SI, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC SI");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DEC_DI: {
                int res = dec16(getReg16(REG_BH_DI));
                setReg16(REG_BH_DI, res & 0xFFFF);
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("DEC DI");
                    debugInfo.setOperands(String.format("->%04Xh", res));
                }
            }
            break;
            case DAA: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || ((al & 0xF) > 9)) {
                    setReg8(REG_AL_AX, al + 6);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                al = getReg8(REG_AL_AX);
                if (getFlag(CARRY_FLAG) || (al > 0x9F)) {
                    setReg8(REG_AL_AX, al + 0x60);
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                checkZeroFlag8(getReg8(REG_AL_AX));
                checkSignFlag8(getReg8(REG_AL_AX));
                checkParityFlag(getReg8(REG_AL_AX));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("DAA");
                    debugInfo.setOperands(String.format("%02Xh->%02Xh", al, getReg8(REG_AL_AX)));
                }
            }
            break;
            case DAS: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || ((al & 0xF) > 9)) {
                    setReg8(REG_AL_AX, al - 6);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                al = getReg8(REG_AL_AX);
                if (getFlag(CARRY_FLAG) || (al > 0x9F)) {
                    setReg8(REG_AL_AX, al - 0x60);
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                checkZeroFlag8(getReg8(REG_AL_AX));
                checkSignFlag8(getReg8(REG_AL_AX));
                checkParityFlag(getReg8(REG_AL_AX));
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("DAS");
                    debugInfo.setOperands(String.format("%02Xh->%02Xh", al, getReg8(REG_AL_AX)));
                }
            }
            break;
            case AAA: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || ((al & 0xF) > 9)) {
                    setReg8(REG_AL_AX, (al + 6) & 0xF);
                    setReg8(REG_AH_SP, getReg8(REG_AH_SP) + 1);
                    //TODO: Ggf. wieder ersetzen
                    //setReg8(REG_AH_SP, (getReg8(REG_AH_SP) + 1) & 0xF);
                    setFlag(CARRY_FLAG);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("AAA");
                    debugInfo.setOperands(String.format("%02Xh->%04Xh", al, getReg16(REG_AL_AX)));
                }
            }
            break;
            case AAS: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || ((al & 0xF) > 9)) {
                    setReg8(REG_AL_AX, (al - 6) & 0xF);
                    setReg8(REG_AH_SP, (getReg8(REG_AH_SP) - 1) & 0xFF);
                    setFlag(CARRY_FLAG);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                updateTicks(8);
                if (debug) {
                    debugInfo.setCode("AAS");
                    debugInfo.setOperands(String.format("%02Xh->%04Xh", al, getReg16(REG_AL_AX)));
                }
            }
            break;
            case AAM: {
                ip++;
                int al = getReg8(REG_AL_AX);
                int res = ((al / 10) << 8) + (al % 10);
                checkZeroFlag16(res);
                checkParityFlag(res);
                checkSignFlag16(res);
                setReg16(REG_AL_AX, res);
                updateTicks(83);
                if (debug) {
                    debugInfo.setCode("AAM");
                    debugInfo.setOperands(String.format("%02Xh->%04Xh", al, res));
                }
            }
            break;
            case AAD: {
                ip++;
                int ah = getReg8(REG_AH_SP);
                int al = getReg8(REG_AL_AX);
                int res = (ah * 10 + al) & 0xFF;
                checkZeroFlag16(res);
                checkParityFlag(res);
                checkSignFlag16(res);
                setReg16(REG_AL_AX, res);
                updateTicks(60);
                if (debug) {
                    debugInfo.setCode("AAD");
                    debugInfo.setOperands(String.format("%02Xh,%02Xh->%04Xh", ah, al, res));
                }
            }
            break;
            case CBW: {
                if ((getReg8(REG_AL_AX) & 0x80) == 0x80) {
                    setReg8(REG_AH_SP, 0xFF);
                } else {
                    setReg8(REG_AH_SP, 0x00);
                }
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("CBW");
                    debugInfo.setOperands(String.format("%02Xh->%04Xh", getReg8(REG_AL_AX), getReg16(REG_AL_AX)));
                }
            }
            break;
            case CWD: {
                if ((getReg16(REG_AL_AX) & 0x8000) == 0x8000) {
                    setReg16(REG_DL_DX, 0xFFFF);
                } else {
                    setReg16(REG_DL_DX, 0x0000);
                }
                updateTicks(5);
                if (debug) {
                    debugInfo.setCode("CWD");
                    debugInfo.setOperands(String.format("%04Xh->%04Xh:%04Xh", getReg16(REG_AL_AX), getReg16(REG_DL_DX), getReg16(REG_AL_AX)));
                }
            }
            break;

            /**
             * Logische Operationen
             */
            case OR_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = or8(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("OR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case OR_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = or16(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("OR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh|%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case OR_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = or8(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("OR " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case OR_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = or16(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("OR " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh|%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case OR_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = or8(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("OR AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case OR_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = or16(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("OR AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh|%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case AND_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = and8(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("AND " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case AND_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = and16(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("AND " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh&%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case AND_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = and8(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("AND " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case AND_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = and16(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("AND " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh&%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case AND_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = and8(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("AND AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case AND_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = and16(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("AND AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh&%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case XOR_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = xor8(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XOR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = xor16(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XOR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh^%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case XOR_MODRM_REG_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = xor8(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XOR " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_MODRM_REG_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = xor16(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("XOR " + getReg16DebugString(opcode2 & TEST_REG) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh^%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case XOR_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = xor8(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("XOR AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                }
            }
            break;
            case XOR_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = xor16(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("XOR AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh^%04Xh->%04Xh", op1, op2, res));
                }
            }
            break;
            case TEST_REG_MODRM_8: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                and8(op1, op2);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("TEST " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case TEST_REG_MODRM_16: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = and16(op1, op2);
                updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    debugInfo.setCode("TEST " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16DebugString(opcode2 & TEST_REG));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;
            case TEST_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int res = and8(op1, op2);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("TEST AL," + String.format("%02Xh", op2));
                    debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                }
            }
            break;
            case TEST_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int res = and16(op1, op2);
                updateTicks(4);
                if (debug) {
                    debugInfo.setCode("TEST AX," + String.format("%04Xh", op2));
                    debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                }
            }
            break;

            /*
             * Übergabe der Steuerung
             */
            case JMP_NEAR_LABEL: {
                int increment = (short) mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                ip = 0xFFFF & (ip + increment);
                updateTicks(15);
                if (debug) {
                    debugInfo.setCode("JMP " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case JMP_FAR_LABEL: {
                int increment = mms16.readMemoryWord((cs << 4) + (ip++)) & 0xFFFF;
                ip++;
                int codesegment = mms16.readMemoryWord((cs << 4) + (ip++)) & 0xFFFF;
                ip++;
                cs = codesegment;
                ip = increment;
                updateTicks(15);
                if (debug) {
                    debugInfo.setCode("JMP " + Integer.toHexString(codesegment) + ":" + Integer.toHexString(increment));
                    debugInfo.setOperands(null);
                }
            }
            break;
            case JMP_SHORT_LABEL: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                ip += increment;
                updateTicks(15);
                if (debug) {
                    debugInfo.setCode("JMP " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case JO: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JO " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNO: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNO " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JB_JNAE_JC: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JC " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(CARRY_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNB_JAE_JNC: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNC " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(CARRY_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JE_JZ: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JZ " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNE_JNZ: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNZ " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JBE_JNA: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNA " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(CARRY_FLAG) || getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNBE_JA: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JA " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(CARRY_FLAG) && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JS: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JS " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(SIGN_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNS: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNS " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(SIGN_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JP_JPE: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JP " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(PARITY_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNP_JPO: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNP " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (!getFlag(PARITY_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JL_JNGE: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JL " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(SIGN_FLAG) != getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNL_JGE: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNL " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(SIGN_FLAG) == getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JLE_JNG: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JNG " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getFlag(SIGN_FLAG) != getFlag(OVERFLOW_FLAG) || getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case JNLE_JG: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JG " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if ((getFlag(SIGN_FLAG) == getFlag(OVERFLOW_FLAG)) && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(16);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case CALL_FAR_PROC: {
                if (debug) {
                    debugInfo.setCode("CALL " + String.format("%04X:%04X", mms16.readMemoryWord((cs << 4) + ip + 2), mms16.readMemoryWord((cs << 4) + ip)));
                    debugInfo.setOperands(null);
                }
                int increment = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                int codesegment = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) cs);
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) ip);
                cs = codesegment;
                ip = increment;
                updateTicks(28);
            }
            break;
            case CALL_NEAR_PROC: {
                int increment = (short) mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) ip);
                ip = (increment + ip) & 0xFFFF;
                updateTicks(19);
                if (debug) {
                    debugInfo.setCode("CALL " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case RET_IN_SEG_IMM: {
                int data = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                ip = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2 + data) & 0xFFFF);
                updateTicks(20);
                if (debug) {
                    debugInfo.setCode("RET " + data);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }

            }
            break;
            case RET_IN_SEG: {
                ip = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                updateTicks(16);
                if (debug) {
                    debugInfo.setCode("RET");
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case RET_INTER_SEG_IMM: {
                int data = mms16.readMemoryWord((cs << 4) + (ip++));
                ip++;
                ip = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                cs = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2 + data) & 0xFFFF);
                updateTicks(25);
                if (debug) {
                    debugInfo.setCode("RET " + data);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case RET_INTER_SEG: {
                ip = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                cs = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                updateTicks(26);
                if (debug) {
                    debugInfo.setCode("RET");
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;
            case LOOPNE_LOOPNZ: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("LOOPNE " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment - 1));
                }
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                if (getReg16(REG_CL_CX) != 0 && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(19);
                } else {
                    updateTicks(5);
                }
            }
            break;
            case LOOPE_LOOPZ: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("LOOPE " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment - 1));
                }
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                if (getReg16(REG_CL_CX) != 0 && getFlag(ZERO_FLAG)) {
                    ip += increment;
                    updateTicks(18);
                } else {
                    updateTicks(6);
                }
            }
            break;
            case LOOP: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                if (debug) {
                    debugInfo.setCode("LOOP " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment - 1));
                }
                // Hack to Abort -2 LOOPS
                /*if (increment == -2) {
                 updateTicks(getReg16(REG_CL_CX) * (17 + 3) + 5);
                 setReg16(REG_CL_CX, 0);
                 } else {*/
                if (getReg16(REG_CL_CX) != 0) {
                    ip += increment;
                    updateTicks(17);
                } else {
                    updateTicks(5);
                }
                //}
            }
            break;
            case JCXZ: {
                int increment = (byte) mms16.readMemoryByte((cs << 4) + (ip++));
                if (debug) {
                    debugInfo.setCode("JCXZ " + increment);
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip + increment));
                }
                if (getReg16(REG_CL_CX) == 0) {
                    ip += increment;
                    updateTicks(18);
                } else {
                    updateTicks(6);
                }
            }
            break;
            case INT_IMM: {
                int data = mms16.readMemoryByte((cs << 4) + (ip++));
                updateTicks(51);
                if (debug) {
                    debugInfo.setCode("INT " + String.format("%02X", data));
                    debugInfo.setOperands(null);
                }
                interrupt(data);
            }
            break;
            case INT3: {
                updateTicks(52);
                interrupt(3);
                if (debug) {
                    debugInfo.setCode("INT 3");
                    debugInfo.setOperands(null);
                }
                interrupt(0x03);
            }
            break;
            case INTO: {
                if (debug) {
                    debugInfo.setCode("INTO");
                    debugInfo.setOperands(null);
                }
                if (getFlag(OVERFLOW_FLAG)) {
                    interrupt(4);
                    updateTicks(53);
                } else {
                    updateTicks(4);
                }
            }
            break;
            case IRET: {
                ip = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                cs = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                flags = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                updateTicks(32);
                if (debug) {
                    debugInfo.setCode("IRET");
                    debugInfo.setOperands(String.format("%04X:%04X", cs, ip));
                }
            }
            break;

            /*
             * Zeichkettenverarbeitung
             */
            case MOVS_8: {
                int segment = getSegment(ds);
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    mms16.writeMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    updateTicks(18);
                } else {
                    updateTicks(9);
                    count = getReg16(REG_CL_CX);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(17);
                        mms16.writeMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                        }

                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("MOVS ES<-" + getSegmentDebugString("DS"));
                    String operand = "";
                    String ascii = "";
                    for (int i = 0; i < count; i++) {
                        if (getFlag(DIRECTION_FLAG)) {
                            int ch = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + count - i);
                            operand += String.format("%02Xh ", ch);
                            ascii += (char) ch;
                        } else {
                            int ch = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - count + i);
                            operand += String.format("%02Xh ", ch);
                            ascii += (char) ch;
                        }
                    }
                    debugInfo.setOperands(operand + " (" + ascii + ")");
                }
            }
            break;
            case MOVS_16: {
                int segment = getSegment(ds);
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    mms16.writeMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    updateTicks(18);
                } else {
                    updateTicks(9);
                    count = getReg16(REG_CL_CX);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(17);
                        mms16.writeMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("MOVSW ES<-" + getSegmentDebugString("DS"));
                    String operand = "";
                    String ascii = "";
                    for (int i = 0; i < count; i++) {
                        if (getFlag(DIRECTION_FLAG)) {
                            int ch = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + (count - i) * 2);
                            operand += String.format("%04Xh ", ch);
                            ascii += (char) (ch & 0xFF) + "" + (char) ((ch & 0xFF00) >> 8);
                        } else {
                            int ch = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - (count + i) * 2);
                            operand += String.format("%04Xh ", ch);
                            ascii += (char) (ch & 0xFF) + "" + (char) ((ch & 0xFF00) >> 8);
                        }
                    }
                    debugInfo.setOperands(operand + " (" + ascii + ")");
                }
            }
            break;
            case CMPS_8: {
                int segment = getSegment(ds);
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    int op1 = mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI));
                    int op2 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                    sub8(op1, op2, false);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    updateTicks(18);
                } else {
                    updateTicks(9);
                    count = getReg16(REG_CL_CX);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(17);
                        int op1 = mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI));
                        int op2 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                        sub8(op1, op2, false);
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("CMPS ES," + getSegmentDebugString("DS"));
                    String operand1 = "";
                    String ascii1 = "";
                    String operand2 = "";
                    String ascii2 = "";
                    for (int i = 0; i < count; i++) {
                        int ch1;
                        int ch2;
                        if (getFlag(DIRECTION_FLAG)) {
                            ch1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI) + (count - i));
                            ch2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + (count - i));
                        } else {
                            ch1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI) + (count + i));
                            ch2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - (count + i));
                        }
                        operand1 += String.format("%02Xh ", ch1);
                        ascii1 += (char) (ch1);
                        operand2 += String.format("%02Xh ", ch2);
                        ascii2 += (char) (ch2);
                    }
                    debugInfo.setOperands(operand1 + " (" + ascii1 + ")," + operand2 + "(" + ascii2 + ")");
                }
            }
            break;
            case CMPS_16: {
                int segment = getSegment(ds);
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    int op1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI));
                    int op2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                    sub16(op1, op2, false);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    updateTicks(18);
                } else {
                    updateTicks(9);
                    count = getReg16(REG_CL_CX);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(17);
                        int op1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI));
                        int op2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                        sub16(op1, op2, false);
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                        }

                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("CMPSW ES," + getSegmentDebugString("DS"));
                    String operand1 = "";
                    String ascii1 = "";
                    String operand2 = "";
                    String ascii2 = "";
                    for (int i = 0; i < count; i++) {
                        int ch1;
                        int ch2;
                        if (getFlag(DIRECTION_FLAG)) {
                            ch1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI) + (count - i) * 2);
                            ch2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + (count - i) * 2);
                        } else {
                            ch1 = mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI) - (count + i) * 2);
                            ch2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - (count + i) * 2);
                        }
                        operand1 += String.format("%04Xh ", ch1);
                        ascii1 += (char) ((ch1 & 0xFF00) >> 8) + "" + (char) (ch1 & 0xFF);
                        operand2 += String.format("%04Xh ", ch2);
                        ascii2 += (char) ((ch2 & 0xFF00) >> 8) + "" + (char) (ch2 & 0xFF);
                    }
                    debugInfo.setOperands(operand1 + " (" + ascii1 + ")," + operand2 + "(" + ascii2 + ")");
                }
            }
            break;
            case STOS_8: {
                if (string_prefix == NO_PREFIX) {
                    mms16.writeMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), (byte) getReg8(REG_AL_AX));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                    }
                    updateTicks(11);
                } else {
                    updateTicks(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(10);
                        mms16.writeMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), (byte) getReg8(REG_AL_AX));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("STOS");
                    debugInfo.setOperands(String.format("%02Xh", (byte) getReg8(REG_AL_AX)));
                }
            }
            break;
            case STOS_16: {
                if (string_prefix == NO_PREFIX) {
                    mms16.writeMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), (short) getReg16(REG_AL_AX));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                    }
                    updateTicks(11);
                } else {
                    updateTicks(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(10);
                        mms16.writeMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI), (short) getReg16(REG_AL_AX));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("STOSW");
                    debugInfo.setOperands(String.format("%04Xh", (short) getReg16(REG_AL_AX)));
                }
            }
            break;
            case LODS_8: {
                int segment = getSegment(ds);
                if (string_prefix == NO_PREFIX) {
                    setReg8(REG_AL_AX, mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    updateTicks(12);
                } else {
                    updateTicks(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(11);
                        setReg8(REG_AL_AX, mms16.readMemoryByte((segment << 4) + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                        } else {
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("LODS " + getSegmentDebugString("DS"));
                    debugInfo.setOperands(String.format("%02Xh", (byte) getReg8(REG_AL_AX)));
                }
            }
            break;
            case LODS_16: {
                int segment = getSegment(ds);
                if (string_prefix == NO_PREFIX) {
                    setReg16(REG_AL_AX, mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    updateTicks(12);
                } else {
                    updateTicks(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(11);
                        setReg16(REG_AL_AX, mms16.readMemoryWord((segment << 4) + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                        } else {
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("LODSW " + getSegmentDebugString("DS"));
                    debugInfo.setOperands(String.format("%04Xh", (short) getReg16(REG_AL_AX)));
                }
            }
            break;
            case SCAS_8: {
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    int op1 = getReg8(REG_AL_AX);
                    int op2 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                    sub8(op1, op2, false);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                    }
                    updateTicks(15);
                } else {
                    updateTicks(9);
                    count = 0;
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(15);
                        int op1 = getReg8(REG_AL_AX);
                        int op2 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                        sub8(op1, op2, false);
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        count++;
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("SCAS");
                    String operand = "";
                    String ascii = "";
                    for (int i = 0; i < count && i < 25; i++) {
                        int ch1;
                        if (getFlag(DIRECTION_FLAG)) {
                            ch1 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + (count - i));
                        } else {
                            ch1 = mms16.readMemoryByte((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - (count - i));
                        }
                        operand += String.format("%02Xh ", (ch1 & 0xFF));
                        ascii += (char) (ch1);
                    }
                    debugInfo.setOperands(operand + " (" + ascii + ")," + String.format("%02Xh", getReg8(REG_AL_AX)) + " (" + (char) getReg8(REG_AL_AX) + ")" + ((count > 25) ? " ..." : ""));
                }
            }
            break;
            case SCAS_16: {
                int count = 1;
                if (string_prefix == NO_PREFIX) {
                    int op1 = getReg16(REG_AL_AX);
                    int op2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                    sub16(op1, op2, false);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                    }
                    updateTicks(15);
                } else {
                    updateTicks(9);
                    count = getReg16(REG_AL_AX);
                    while (getReg16(REG_CL_CX) != 0) {
                        updateTicks(15);
                        int op1 = getReg16(REG_AL_AX);
                        int op2 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI));
                        sub16(op1, op2, false);
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    debugInfo.setCode("SCASW");
                    String operand = "";
                    String ascii = "";
                    for (int i = 0; i < count && i < 25; i++) {
                        int ch1;
                        if (getFlag(DIRECTION_FLAG)) {
                            ch1 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) + (count - i) * 2);
                        } else {
                            ch1 = mms16.readMemoryWord((getSReg(SREG_ES) << 4) + getReg16(REG_BH_DI) - (count - i) * 2);
                        }
                        operand += String.format("%04Xh ", ch1);
                        ascii += (char) (ch1 & 0xFF) + (char) ((ch1 & 0xFF00) >> 8);
                    }
                    debugInfo.setOperands(operand + " (" + ascii + ")," + String.format("%04Xh", getReg16(REG_AL_AX)) + " (" + (char) (getReg16(REG_AL_AX) & 0xFF) + (char) ((getReg16(REG_AL_AX) & 0xFF00) >> 8) + ")" + ((count > 25) ? " ..." : ""));
                }
            }
            break;
            case REPNE_REPNZ: {
                string_prefix = REPNE_REPNZ;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("REPNE");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case REP_REPE_REPZ: {
                string_prefix = REP_REPE_REPZ;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("REP");
                    debugInfo.setOperands(null);
                }
            }
            break;

            /**
             * Prozessorsteuerung
             */
            case NOP:
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("NOP");
                    debugInfo.setOperands(null);
                }
                break;
            case CMC:
                flags ^= CARRY_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("CMC");
                    debugInfo.setOperands(null);
                }
                break;
            case CLC:
                flags &= ~CARRY_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("CLC");
                    debugInfo.setOperands(null);
                }
                break;
            case STC:
                flags |= CARRY_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("STC");
                    debugInfo.setOperands(null);
                }
                break;
            case CLI:
                flags &= ~INTERRUPT_ENABLE_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("CLI");
                    debugInfo.setOperands(null);
                }
                break;
            case STI:
                stiWaiting = true;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("STI");
                    debugInfo.setOperands(null);
                }
                break;
            case CLD:
                flags &= ~DIRECTION_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("CLD");
                    debugInfo.setOperands(null);
                }
                break;
            case STD:
                flags |= DIRECTION_FLAG;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("STD");
                    debugInfo.setOperands(null);
                }
                break;
            case WAIT: {
                updateTicks(3);
                if (debug) {
                    debugInfo.setCode("WAIT");
                    debugInfo.setOperands(null);
                }
                System.out.println("Befehl WAIT noch nicht implementiert");
                try {
                    mms16.dumpSystemMemory("./debug/dump_wait.hex");
                } catch (IOException ex) {
                    Logger.getLogger(K1810WM86.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.exit(0);
            }
            break;
            case LOCK: {
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("LOCK");
                    debugInfo.setOperands(null);
                }
                System.out.println("Befehl LOCK noch nicht implementiert");
                try {
                    mms16.dumpSystemMemory("./debug/dump_lock.hex");
                } catch (IOException ex) {
                    Logger.getLogger(K1810WM86.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.exit(0);
            }
            break;
            case HLT: {
                isHalted = true;
                updateTicks(2);
                if (debug) {
                    debugInfo.setCode("HLT");
                    debugInfo.setOperands(null);
                }
            }
            break;
            case ESC0: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC0 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC1: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC1 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC2: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC2 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC3: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC3 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC4: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC4 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC5: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC5 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC6: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC6 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;
            case ESC7: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    debugInfo.setCode("ESC7 " + (opcode2 >> 3) + "," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                    debugInfo.setOperands(String.format("%04Xh", op1));
                }
            }
            break;

            /*
             * Zweier
             */
            case 0x80: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _80_ADD_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add8(op1, op2, false);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADD " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_OR_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = or8(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("OR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh|%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_ADC_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add8(op1, op2, true);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADC " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_SBB_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub8(op1, op2, true);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SBB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_AND_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = and8(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("AND " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh&%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_SUB_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub8(op1, op2, false);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SUB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_XOR_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = xor8(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("XOR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh^%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _80_CMP_IMM_REG_8_NOSIGN: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        sub8(op1, op2, false);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("CMP " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x81: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _81_ADD_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add16(op1, op2, false);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADD " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_OR_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = or16(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("OR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh|%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_ADC_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add16(op1, op2, true);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADC " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_SBB_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub16(op1, op2, true);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SBB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_AND_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = and16(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("AND " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh&%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_SUB_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub16(op1, op2, false);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SUB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_XOR_IMM_REG_16_NOSIGN: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = xor16(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("XOR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh^%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _81_CMP_IMM_REG_16_NOSIGN: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        sub16(op1, op2, false);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("CMP " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x82: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _82_ADD_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add8(op1, op2, false);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADD " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _82_ADC_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add8(op1, op2, true);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADC " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh+%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _82_SBB_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub8(op1, op2, true);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SBB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _82_SUB_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub8(op1, op2, false);
                        ip++;
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SUB " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh-%02Xh->%02Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _82_CMP_IMM_REG_8_SIGN: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        sub8(op1, op2, false);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("CMP " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x83: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _83_ADD_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = (byte) getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add16(op1, op2, false);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADD " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _83_ADC_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = (byte) getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = add16(op1, op2, true);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ADC " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh+%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _83_SBB_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = (byte) getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub16(op1, op2, true);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SBB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _83_SUB_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = (byte) getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = sub16(op1, op2, false);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SUB " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh-%04Xh->%04Xh", op1, op2, res));
                        }
                    }
                    break;
                    case _83_CMP_IMM_REG_16_SIGN: {
                        int op2 = (byte) getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        sub16(op1, op2, false);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("CMP " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8C: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8C_MOV_ES_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_ES), true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",ES");
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_ES)));
                        }
                    }
                    break;
                    case _8C_MOV_CS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_CS), true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CS");
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_CS)));
                        }
                    }
                    break;
                    case _8C_MOV_SS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_SS), true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",SS");
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_SS)));
                        }
                    }
                    break;
                    case _8C_MOV_DS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_DS), true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",DS");
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_DS)));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8E: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8E_MOV_MODRM_ES_16: {
                        setSReg(SREG_ES, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV ES," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_ES)));
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_CS_16: {
                        setSReg(SREG_CS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV CS," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_CS)));
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_SS_16: {
                        setSReg(SREG_SS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV SS," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_SS)));
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_DS_16: {
                        setSReg(SREG_DS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV DS," + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", getSReg(SREG_DS)));
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8F: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8F_POP_MODRM_16: {
                        int data = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, data, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 8 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("POP " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", data));
                        }
                    }
                    break;
                }
            }
            break;
            case 0xC6: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _C6_MOV_IMM_MEM_8: {
                        int data8 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, data8 & 0xFF, true);
                        ip++;
                        updateTicks(10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", data8));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xC7: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _C7_MOV_IMM_MEM_16: {
                        int data16 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, data16 & 0xFFFF, true);
                        ip = ip + 2;
                        updateTicks(10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MOV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", data16));
                            debugInfo.setOperands(null);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD0: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _D0_ROL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (BitTest.getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 << 1) & 0xFF) | ((op1 & 0x80) >> 7);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ROL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_ROR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 >> 1) & 0xFF) | ((op1 & 0x01) << 7);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(res, 6) != BitTest.getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ROR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_RCL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x01;
                        }
                        if (BitTest.getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("RCL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_RCR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x80;
                        }
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(op1, 6) != BitTest.getBit(op1, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("RCR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_SAL_SHL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFF);
                        if (BitTest.getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SHL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_SHR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(res, 7) != BitTest.getBit(res, 6)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SHR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D0_SAR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        if (BitTest.getBit(op1, 7)) {
                            res |= 0x80;
                        } else {
                            res &= 0x7F;
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SAR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD1: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _D1_ROL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (BitTest.getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 << 1) & 0xFFFF) | ((op1 & 0x8000) >> 15);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ROL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_ROR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 >> 1) & 0xFFFF) | ((op1 & 0x0001) << 15);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(res, 15) != BitTest.getBit(res, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("ROR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_RCL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFFFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x0001;
                        }
                        if (BitTest.getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("RCL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_RCR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x8000;
                        }
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(op1, 15) != BitTest.getBit(op1, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("RCR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_SAL_SHL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFFFF);
                        if (BitTest.getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != BitTest.getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag16(res);
                        checkZeroFlag16(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SHL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_SHR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (BitTest.getBit(res, 15) != BitTest.getBit(res, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag16(res);
                        checkZeroFlag16(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SHR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                    case _D1_SAR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (BitTest.getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        if (BitTest.getBit(op1, 15)) {
                            res |= 0x8000;
                        } else {
                            res &= 0x7FFF;
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("SAR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1");
                            debugInfo.setOperands(Integer.toBinaryString(op1) + "b->" + Integer.toBinaryString(res) + "b");
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD2: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int count = getReg8(REG_CL_CX);
                switch (opcode2 & TEST_REG) {
                    case _D2_ROL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 7)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFF) | ((op1 & 0x80) >> 7);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("ROL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_ROR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFF) | ((op1 & 0x01) << 7);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("ROR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_RCL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (BitTest.getBit(op1, 7)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFF);
                            if (lastCarry) {
                                op1 |= 0x01;
                            }
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("RCL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_RCR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFF);
                            if (lastCarry) {
                                op1 |= 0x80;
                            }
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("RCR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_SAL_SHL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 7)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFF);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        checkSignFlag8(op1);
                        checkZeroFlag8(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SAL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_SHR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFF);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        checkSignFlag8(op1);
                        checkZeroFlag8(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SHR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D2_SAR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        boolean signFlag = BitTest.getBit(op1, 7);
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFF);
                        }
                        if (signFlag) {
                            op1 |= 0x80;
                        } else {
                            op1 &= 0x7F;
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag8(op1);
                        checkZeroFlag8(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SAR " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD3: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                int count = getReg8(REG_CL_CX);
                switch (opcode2 & TEST_REG) {
                    case _D3_ROL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 15)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFFFF) | ((op1 & 0x8000) >> 15);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("ROL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_ROR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFFFF) | ((op1 & 0x0001) << 15);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("ROR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_RCL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (BitTest.getBit(op1, 15)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFFFF);
                            if (lastCarry) {
                                op1 |= 0x0001;
                            }
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("RCL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_RCR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFFFF);
                            if (lastCarry) {
                                op1 |= 0x8000;
                            }
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("RCR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_SAL_SHL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 15)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFFFF);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        checkSignFlag16(op1);
                        checkZeroFlag16(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SHL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_SHR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFFFF);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        checkSignFlag16(op1);
                        checkZeroFlag16(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SHR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                    case _D3_SAR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        boolean signFlag = BitTest.getBit(op1, 15);
                        for (int i = 0; i < count; i++) {
                            if (BitTest.getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFFFF);
                        }
                        if (signFlag) {
                            op1 |= 0x8000;
                        } else {
                            op1 &= 0x7FFF;
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag16(op1);
                        checkZeroFlag16(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? (count << 2) : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + (count << 2));
                        if (debug) {
                            debugInfo.setCode("SAR " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL");
                            debugInfo.setOperands(String.format("%02Xh", count) + " - " + Integer.toBinaryString(op1b) + "b->" + Integer.toBinaryString(op1) + "b");
                        }
                    }
                    break;
                }
            }
            break;
            case 0xF6: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _F6_TEST_IMM_MODRM_8: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        and8(op1, op2);
                        ip++;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 5 : 11 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("TEST " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + String.format("%02Xh", op2));
                            debugInfo.setOperands(String.format("%02Xh,%02Xh", op1, op2));
                        }
                    }
                    break;
                    case _F6_NOT_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = (~op1) & 0xFF;
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("NOT " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%02Xh->%02Xh", op1, res));
                        }
                    }
                    break;
                    case _F6_NEG_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        if (op1 != 0x80) {
                            op1 = (-op1) & 0xFF;
                            setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                            if (op1 == 0) {
                                clearFlag(CARRY_FLAG);
                            } else {
                                setFlag(CARRY_FLAG);
                            }
                            clearFlag(OVERFLOW_FLAG);
                        } else {
                            setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                            setFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag8(op1);
                        checkZeroFlag8(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("NEG " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%02Xh->%02Xh", op1b, op1));
                        }
                    }
                    break;
                    case _F6_MUL_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg8(REG_AL_AX);
                        int res = op1 * op2;
                        if (res > 0xFF) {
                            setFlag(CARRY_FLAG);
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                            clearFlag(OVERFLOW_FLAG);
                        }
                        setReg16(REG_AL_AX, res & 0xFFFF);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 77 : 83 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MUL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%02Xh*%02Xh->%02Xh", op1, op2, getReg16(REG_AL_AX)));
                        }
                    }
                    break;
                    case _F6_IMUL_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg8(REG_AL_AX);
                        int res = (byte) op1 * (byte) op2;
                        if (res > 0xFF) {
                            setFlag(CARRY_FLAG);
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                            clearFlag(OVERFLOW_FLAG);
                        }
                        setReg16(REG_AL_AX, res & 0xFFFF);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 98 : 104 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("IMUL " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%02Xh*%02Xh->%02Xh", op1, op1, getReg16(REG_AL_AX)));
                        }
                    }
                    break;
                    case _F6_DIV_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0xFF)) {
                            interrupt(0);
                        } else {
                            setReg8(REG_AL_AX, op2 / op1);
                            setReg8(REG_AH_SP, op2 % op1);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 90 : 96 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("DIV " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh/%02Xh->%02Xh,%02Xh", op2, op1, getReg8(REG_AL_AX), getReg8(REG_AH_SP)));
                        }
                    }
                    break;
                    case _F6_IDIV_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0x7F)) {
                            interrupt(0);
                        } else {
                            setReg8(REG_AL_AX, (short) op2 / (byte) op1);
                            setReg8(REG_AH_SP, (short) op2 % (byte) op1);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 112 : 118 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("IDIV " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh/%02Xh->%02Xh,%02Xh", op2, op1, getReg8(REG_AL_AX), getReg8(REG_AH_SP)));
                        }
                    }
                    break;
                }
            }
            break;
            case 0xF7: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _F7_TEST_IMM_MODRM_16: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = and16(op1, op2);
                        ip = ip + 2;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 5 : 11 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("TEST " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + String.format("%04Xh", op2));
                            debugInfo.setOperands(String.format("%04Xh,%04Xh", op1, op2));
                        }
                    }
                    break;
                    case _F7_NOT_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = (~op1) & 0xFFFF;
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("NOT " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh->%04Xh", op1, res));
                        }
                    }
                    break;
                    case _F7_NEG_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op1b = op1;
                        if (op1 != 0x8000) {
                            op1 = (-op1) & 0xFFFF;
                            setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                            if (op1 == 0) {
                                clearFlag(CARRY_FLAG);
                            } else {
                                setFlag(CARRY_FLAG);
                            }
                            clearFlag(OVERFLOW_FLAG);
                        } else {
                            setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                            setFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag16(op1);
                        checkZeroFlag16(op1);
                        checkParityFlag(op1);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("NEG " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh->%04Xh", op1b, op1));
                        }
                    }
                    break;
                    case _F7_MUL_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        if (op1 * op2 > 0xFFFF) {
                            setFlag(CARRY_FLAG);
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                            clearFlag(OVERFLOW_FLAG);
                        }
                        setReg16(REG_DL_DX, ((op2 * op1) >> 16) & 0xFFFF);
                        setReg16(REG_AL_AX, (op2 * op1) & 0xFFFF);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 118 : 139 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("MUL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh*%04Xh->%08Xh", op1, op2, (getReg16(REG_DL_DX) << 16 | getReg16(REG_AL_AX))));
                        }
                    }
                    break;
                    case _F7_IMUL_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        int res = (short) op1 * (short) op2;
                        if (res > 0xFFFF) {
                            setFlag(CARRY_FLAG);
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                            clearFlag(OVERFLOW_FLAG);
                        }
                        setReg16(REG_DL_DX, (res >> 16) & 0xFFFF);
                        setReg16(REG_AL_AX, res & 0xFFFF);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 154 : 160 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("IMUL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh*%04Xh->%08Xh", op1, op2, (getReg16(REG_DL_DX) << 16 | getReg16(REG_AL_AX))));
                        }
                    }
                    break;
                    case _F7_DIV_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = (getReg16(REG_DL_DX) << 16) | getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0xFFFF)) {
                            interrupt(0);
                        } else {
                            setReg16(REG_AL_AX, op2 / op1);
                            setReg16(REG_DL_DX, op2 % op1);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 162 : 172 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("DIV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%08Xh/%04Xh->%04Xh,%04Xh", op2, op1, getReg16(REG_AL_AX), getReg16(REG_DL_DX)));
                        }
                    }
                    break;
                    case _F7_IDIV_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = (getReg16(REG_DL_DX) << 16) | getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0x7FFF)) {
                            interrupt(0);
                        } else {
                            setReg16(REG_AL_AX, op2 / (short) op1);
                            setReg16(REG_DL_DX, op2 % (short) op1);
                        }
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 184 : 190 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("IDIV " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%08Xh/%04Xh->%04Xh,%04Xh", op2, op1, getReg16(REG_AL_AX), getReg16(REG_DL_DX)));
                        }
                    }
                    break;
                }
            }
            break;
            case 0xFE: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _FE_INC_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = inc8(op1);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("INC " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("->%02Xh", res));
                        }
                    }
                    break;
                    case _FE_DEC_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = dec8(op1);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("DEC " + getMODRM8DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("->%02Xh", res));
                        }
                    }
                    break;
                }
            }
            break;
            case 0xFF: {
                int opcode2 = mms16.readMemoryByte((cs << 4) + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _FF_INC_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = inc16(op1);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("INC " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("->%04Xh", res));
                        }
                    }
                    break;
                    case _FF_DEC_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = dec16(op1);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("DEC " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("->%04Xh", res));
                        }
                    }
                    break;
                    case _FF_CALL_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            debugInfo.setCode("CALL " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04X:%04X", cs, op1));
                        }
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) ip);
                        ip = op1;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 16 : 21 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_CALL_MEM_16: {
                        int op1 = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            debugInfo.setCode("CALL " + Integer.toHexString(mms16.readMemoryWord(op1 + 2)) + ":" + mms16.readMemoryWord(op1));
                            debugInfo.setOperands(null);
                        }
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) cs);
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) ip);
                        int increment = mms16.readMemoryWord(op1);
                        int codesegment = mms16.readMemoryWord(op1 + 2);
                        cs = codesegment;
                        ip = increment;
                        updateTicks(37 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_JMP_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            debugInfo.setCode("JMP " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04X:%04X", cs, op1));
                        }
                        ip = op1;
                        updateTicks(((opcode2 & TEST_MOD) == MOD_REG) ? 11 : 18 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_JMP_MEM_16: {
                        int addr = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            debugInfo.setCode("JMP " + getAddressMODRMDebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04X:%04X", mms16.readMemoryWord(addr + 2), mms16.readMemoryWord(addr)));
                        }
                        int increment = mms16.readMemoryWord(addr);
                        int codesegment = mms16.readMemoryWord(addr + 2);
                        cs = codesegment;
                        ip = increment;
                        updateTicks(24 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_PUSH_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        push(op1);
                        updateTicks(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            debugInfo.setCode("PUSH " + getMODRM16DebugString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                            debugInfo.setOperands(String.format("%04Xh", op1));
                        }
                    }
                    break;
                }
            }
            break;
            default:
                // TODO: Ungültige opcodes ignorieren
                System.out.println("Nicht implementierter oder ungültiger OPCode " + Integer.toHexString(opcode1) + " bei " + String.format("%04X:%04X", cs, (ip - 1)) + "!");
                try {
                    mms16.dumpSystemMemory("./debug/dump_unknown_opcode.hex");
                    decoder.save();
                } catch (IOException ex) {
                    Logger.getLogger(K1810WM86.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.exit(0);
                break;
        }

        if (debug) {
            if (debugInfo.getCode() != null) {
                decoder.addItem(debugInfo);
                debugger.addLine(debugInfo);
            }
        }
    }

    /**
     * Führt ein bitweises UND der 16Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int and16(int op1, int op2) {
        int res = op1 & op2;
        checkParityFlag(res);
        checkZeroFlag16(res);
        checkSignFlag16(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Führt ein bitweises XOR der 16Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int xor16(int op1, int op2) {
        int res = op1 ^ op2;
        checkParityFlag(res);
        checkZeroFlag16(res);
        checkSignFlag16(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Führt ein bitweises XOR der 8Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int xor8(int op1, int op2) {
        int res = op1 ^ op2;
        checkParityFlag(res);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Führt ein bitweises UND der 8Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int and8(int op1, int op2) {
        int res = op1 & op2;
        checkParityFlag(res);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Führt ein bitweises ODER der 16Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int or16(int op1, int op2) {
        int res = op1 | op2;
        checkParityFlag(res);
        checkZeroFlag16(res);
        checkSignFlag16(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Führt ein bitweises ODER der 8Bit Operanden durch und setzt die
     * entsprechenden Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @return Ergebniss
     */
    private int or8(int op1, int op2) {
        int res = op1 | op2;
        checkParityFlag(res);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        clearFlag(CARRY_FLAG);
        clearFlag(OVERFLOW_FLAG);
        return res;
    }

    /**
     * Addiert die angegebenen 8Bit Operanden und setzt die entsprechenden
     * Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @param useCarry <code>true</code> - wenn ein gesetztes Carry-Flag
     * berücksichtigt werden soll, <code>false</code> - sonst
     * @return Ergebniss
     */
    private int sub8(int op1, int op2, boolean useCarry) {
        if (useCarry && getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 - op2;
        checkCarryFlagSub(res);
        checkZeroFlag8(res);
        checkSignFlag8((byte) res);
        checkOverflowFlagSub8(op1, op2, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagSub(op1, op2);
        return res;
    }

    /**
     * Subtrahiert die angegebenen 16Bit Operanden und setzt die entsprechenden
     * Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @param useCarry <code>true</code> - wenn ein gesetztes Carry-Flag
     * berücksichtigt werden soll, <code>false</code> - sonst
     * @return Ergebniss
     */
    private int sub16(int op1, int op2, boolean useCarry) {
        if (useCarry && getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 - op2;
        checkCarryFlagSub(res);
        checkZeroFlag16(res);
        checkSignFlag16((short) res);
        checkOverflowFlagSub16(op1, op2, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagSub(op1, op2);
        return res;
    }

    /**
     * Verringert den angegebenen 8Bit Operanden um 1 und setzt die
     * entsprechenden Flags.
     *
     * @param op Operand
     * @return Ergebniss
     */
    private int dec8(int op) {
        int res = op - 1;
        checkZeroFlag8(res);
        checkSignFlag8((byte) res);
        checkOverflowFlagSub8(op, 1, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagSub(op, 1);
        return res;
    }

    /**
     * Verringert den angegebenen 16Bit Operanden um 1 und setzt die
     * entsprechenden Flags.
     *
     * @param op Operand
     * @return Ergebniss
     */
    private int dec16(int op) {
        int res = op - 1;
        checkZeroFlag16(res);
        checkSignFlag16((byte) res);
        checkOverflowFlagSub16(op, 1, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagSub(op, 1);
        return res;
    }

    /**
     * Addiert die angegebenen 16Bit Operanden und setzt die entsprechenden
     * Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @param useCarry <code>true</code> - wenn ein gesetztes Carry-Flag
     * berücksichtigt werden soll, <code>false</code> - sonst
     * @return Ergebniss
     */
    private int add16(int op1, int op2, boolean useCarry) {
        if (useCarry && getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 + op2;
        checkCarryFlagAdd16(res);
        checkZeroFlag16(res);
        checkSignFlag16(res);
        checkOverflowFlagAdd16(op1, op2, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagAdd(op1, op2);
        return res;
    }

    /**
     * Addiert die angegebenen 8Bit Operanden und setzt die entsprechenden
     * Flags.
     *
     * @param op1 Operand 1
     * @param op2 Operand 2
     * @param useCarry <code>true</code> - wenn ein gesetztes Carry-Flag
     * berücksichtigt werden soll, <code>false</code> - sonst
     * @return Ergebniss
     */
    private int add8(int op1, int op2, boolean useCarry) {
        if (useCarry && getFlag(CARRY_FLAG)) {
            op2++;
        }
        int res = op1 + op2;
        checkCarryFlagAdd8(res);
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagAdd8(op1, op2, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagAdd(op1, op2);
        return res;
    }

    /**
     * Erhöht den Angegebenen 8Bit Operanden um 1 und setzt die entsprechenden
     * Flags.
     *
     * @param op Operand
     * @return Ergebniss
     */
    private int inc8(int op) {
        int res = op + 1;
        checkZeroFlag8(res);
        checkSignFlag8(res);
        checkOverflowFlagAdd8(op, 1, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagAdd(op, 1);
        return res;
    }

    /**
     * Erhöht den Angegebenen 16Bit Operanden um 1 und setzt die entsprechenden
     * Flags.
     *
     * @param op Operand
     * @return Ergebniss
     */
    private int inc16(int op) {
        int res = op + 1;
        checkZeroFlag16(res);
        checkSignFlag16(res);
        checkOverflowFlagAdd16(op, 1, res);
        checkParityFlag(res);
        checkAuxiliaryCarryFlagAdd(op, 1);
        return res;
    }

    /**
     * Holt einen Wert vom Stack SS:SP und erhöht den Stackpointer.
     *
     * @return Vom Stack geholter Wert.
     */
    private int pop() {
        int result = mms16.readMemoryWord((ss << 4) + getReg16(REG_AH_SP));
        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
        return result;
    }

    /**
     * Verringert den Stackpointer und legt einen Wert auf dem Stack SS:SP.
     *
     * @param value Daten
     */
    private void push(int value) {
        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
        mms16.writeMemoryWord((ss << 4) + getReg16(REG_AH_SP), (short) value);
    }

    /**
     * Gibt das Immediate Byte basierend auf dem MOD/RM-Feld zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @return Immediate Byte
     */
    private int getImmediate8(int MOD, int RM) {
        switch (MOD) {
            case MOD_REG:
                return mms16.readMemoryByte((cs << 4) + ip);
            case MOD_MEM_NO_DISPL:
                if (RM == RM_DIRECT_BP) {
                    return mms16.readMemoryByte((cs << 4) + ip + 2);
                } else {
                    return mms16.readMemoryByte((cs << 4) + ip);
                }
            case MOD_MEM_8_DISPL:
                return mms16.readMemoryByte((cs << 4) + ip + 1);
            case MOD_MEM_16_DISPL:
                return mms16.readMemoryByte((cs << 4) + ip + 2);
            default:
                throw new IllegalArgumentException("Illegal MOD");
        }
    }

    /**
     * Gibt das Immediate Wort basierend auf dem MOD/RM-Feld zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @return Immediate Wort
     */
    private int getImmediate16(int MOD, int RM) {
        switch (MOD) {
            case MOD_REG:
                return mms16.readMemoryWord((cs << 4) + ip);
            case MOD_MEM_NO_DISPL:
                if (RM == RM_DIRECT_BP) {
                    return mms16.readMemoryWord((cs << 4) + ip + 2);
                } else {
                    return mms16.readMemoryWord((cs << 4) + ip);
                }
            case MOD_MEM_8_DISPL:
                return mms16.readMemoryWord((cs << 4) + ip + 1);
            case MOD_MEM_16_DISPL:
                return mms16.readMemoryWord((cs << 4) + ip + 2);
            default:
                throw new IllegalArgumentException("Illegal MOD");
        }
    }

    /**
     * Gibt den Inhalt eines Segmentregisters basierend auf dem aktuell
     * gesetzten Präfix zurück.
     *
     * @param defaultSegment Standardsegment, wenn kein Präfix definiert ist
     * @return Segmentregister
     */
    private int getSegment(int defaultSegment) {
        switch (prefix) {
            case NO_PREFIX:
                return defaultSegment;
            case PREFIX_CS:
                return cs;
            case PREFIX_DS:
                return ds;
            case PREFIX_ES:
                return es;
            case PREFIX_SS:
                return ss;
            default:
                throw new IllegalStateException("PREFIX not detected");
        }
    }

    /**
     * Gibt den Registernamen eines Segmentregisters entsprechend des gesetzten
     * Präfix an.
     *
     * @param default_segment Standardsegment, wenn kein Präfix vorhanden ist
     * @return Registername
     */
    private String getSegmentDebugString(String default_segment) {
        switch (prefix) {
            case NO_PREFIX:
                return default_segment;
            case PREFIX_CS:
                return "CS";
            case PREFIX_DS:
                return "DS";
            case PREFIX_ES:
                return "ES";
            case PREFIX_SS:
                return "SS";
            default:
                throw new IllegalStateException("PREFIX not detected");
        }
    }

    /**
     * Liefert einen Debug-String für das Offset entsprechend des MOD/RM-Feldes.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param ipOffset Zu berücksichtigender IP-Offset
     * @return Debug-String für Offset
     */
    private String getOffsetDebugString(int MOD, int RM, int ipOffset) {
        String offset = "";

        switch (RM) {
            case RM_BX_SI:
                offset = "[BX]+[SI]";
                break;
            case RM_BX_DI:
                offset = "[BX]+[DI]";
                break;
            case RM_BP_SI:
                offset = "[BP]+[SI]";
                break;
            case RM_BP_DI:
                offset = "[BP]+[DI]";
                break;
            case RM_SI:
                offset = "[SI]";
                break;
            case RM_DI:
                offset = "[DI]";
                break;
            case RM_DIRECT_BP:
                if (MOD == MOD_MEM_NO_DISPL) {
                    offset = String.format("%04Xh", mms16.readMemoryWord((cs << 4) + ip - 2 - ipOffset));
                } else {
                    offset = "[BP]";
                }
                break;
            case RM_BX:
                offset = "[BX]";
                break;
            default:
                throw new IllegalStateException("RM not detected");
        }

        switch (MOD) {
            case MOD_MEM_NO_DISPL:
                break;
            case MOD_MEM_8_DISPL:
                int displ = (byte) (mms16.readMemoryByte((cs << 4) + ip - 1 - ipOffset) & 0xFF);
                offset += ((displ > 0) ? "+" : "") + displ;
                break;
            case MOD_MEM_16_DISPL:
                offset += "+" + Integer.toHexString(mms16.readMemoryWord((cs << 4) + ip - 2 - ipOffset)) + "h";
                break;
        }
        return offset;
    }

    /**
     * Berechnet das Offset für einen Speicherzugriff entsprechend des
     * MOD/RM-Feldes.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param updateIP Gibt an ob der InstructionPointer aktualisiert werden
     * soll
     * @return Offset
     */
    private int getOffset(int MOD, int RM, boolean updateIP) {
        int modip = ip;
        int offset = 0;

        switch (RM) {
            case RM_BX_SI:
                offset = bx + si;
                break;
            case RM_BX_DI:
                offset = bx + di;
                break;
            case RM_BP_SI:
                offset = bp + si;
                break;
            case RM_BP_DI:
                offset = bp + di;
                break;
            case RM_SI:
                offset = si;
                break;
            case RM_DI:
                offset = di;
                break;
            case RM_DIRECT_BP:
                if (MOD == MOD_MEM_NO_DISPL) {
                    offset = mms16.readMemoryWord((cs << 4) + (modip++));
                    modip++;
                } else {
                    offset = bp;
                }
                break;
            case RM_BX:
                offset = bx;
                break;
            default:
                throw new IllegalStateException("RM not detected");
        }

        switch (MOD) {
            case MOD_MEM_NO_DISPL:
                break;
            case MOD_MEM_8_DISPL:
                offset += (byte) mms16.readMemoryByte((cs << 4) + (modip++));
                break;
            case MOD_MEM_16_DISPL:
                offset += mms16.readMemoryWord((cs << 4) + (modip++));
                modip++;
                break;
        }
        if (updateIP) {
            ip = modip;
        }

        return offset;
    }

    /**
     * Gibt den Registernamen eines 8Bit Registers als String zurück.
     *
     * @param reg Register
     * @return String mit Registernamen
     */
    private String getReg8String(int reg) {
        switch (reg) {
            case REG_AL_AX:
                return "AL";
            case REG_CL_CX:
                return "CL";
            case REG_DL_DX:
                return "DL";
            case REG_BL_BX:
                return "BL";
            case REG_AH_SP:
                return "AH";
            case REG_CH_BP:
                return "CH";
            case REG_DH_SI:
                return "DH";
            case REG_BH_DI:
                return "BH";
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Registernamen eines 16Bit Registers als String zurück.
     *
     * @param reg Register
     * @return String mit Registernamen
     */
    private String getReg16DebugString(int reg) {
        switch (reg) {
            case REG_AL_AX:
                return "AX";
            case REG_CL_CX:
                return "CX";
            case REG_DL_DX:
                return "DX";
            case REG_BL_BX:
                return "BX";
            case REG_AH_SP:
                return "SP";
            case REG_CH_BP:
                return "BP";
            case REG_DH_SI:
                return "SI";
            case REG_BH_DI:
                return "DI";
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Inhalt eines 8Bit Registers zurück.
     *
     * @param reg Register
     * @return Registerinhalt
     */
    private int getReg8(int reg) {
        switch (reg) {
            case REG_AL_AX:
                return ax & 0xFF;
            case REG_CL_CX:
                return cx & 0xFF;
            case REG_DL_DX:
                return dx & 0xFF;
            case REG_BL_BX:
                return bx & 0xFF;
            case REG_AH_SP:
                return (ax >> 8) & 0xFF;
            case REG_CH_BP:
                return (cx >> 8) & 0xFF;
            case REG_DH_SI:
                return (dx >> 8) & 0xFF;
            case REG_BH_DI:
                return (bx >> 8) & 0xFF;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt den Inhalt eines 16Bit Registers zurück.
     *
     * @param reg Register
     * @return Registerinhalt
     */
    private int getReg16(int reg) {
        switch (reg) {
            case REG_AL_AX:
                return ax & 0xFFFF;
            case REG_CL_CX:
                return cx & 0xFFFF;
            case REG_DL_DX:
                return dx & 0xFFFF;
            case REG_BL_BX:
                return bx & 0xFFFF;
            case REG_AH_SP:
                return sp & 0xFFFF;
            case REG_CH_BP:
                return bp & 0xFFFF;
            case REG_DH_SI:
                return si & 0xFFFF;
            case REG_BH_DI:
                return di & 0xFFFF;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt die Adresse des Segmentbereiches entsprechend des MOD/RM-Feldes
     * zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @return Adresse des Segmentbeginns
     */
    private int getSegmentAddress(int MOD, int RM) {
        int segmentAddress = 0;

        switch (prefix) {
            case NO_PREFIX:
                switch (RM) {
                    case RM_BX_SI:
                    case RM_BX_DI:
                    case RM_SI:
                    case RM_DI:
                    case RM_BX:
                        segmentAddress = ds << 4;
                        break;
                    case RM_DIRECT_BP:
                        if (MOD == MOD_MEM_NO_DISPL) {
                            segmentAddress = ds << 4;
                        } else {
                            segmentAddress = ss << 4;
                        }
                        break;
                    case RM_BP_SI:
                    case RM_BP_DI:
                        segmentAddress = ss << 4;
                        break;
                }
                break;
            case PREFIX_ES:
                segmentAddress = es << 4;
                break;
            case PREFIX_CS:
                segmentAddress = cs << 4;
                break;
            case PREFIX_SS:
                segmentAddress = ss << 4;
                break;
            case PREFIX_DS:
                segmentAddress = ds << 4;
                break;
            default:
                throw new IllegalStateException("PREFIX not detected");
        }
        return segmentAddress;
    }

    /**
     * Gibt einen Debug-String für das zu verwendende Segment entsprechend des
     * MOD/RM-Feldes zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @return String des Segments
     */
    private String getSegmentAddressAdressString(int MOD, int RM) {
        String segmentAddress = "";

        switch (prefix) {
            case NO_PREFIX:
                switch (RM) {
                    case RM_BX_SI:
                    case RM_BX_DI:
                    case RM_SI:
                    case RM_DI:
                    case RM_BX:
                        segmentAddress = "DS";
                        break;
                    case RM_DIRECT_BP:
                        if (MOD == MOD_MEM_NO_DISPL) {
                            segmentAddress = "DS";
                        } else {
                            segmentAddress = "SS";
                        }
                        break;
                    case RM_BP_SI:
                    case RM_BP_DI:
                        segmentAddress = "SS";
                        break;
                }
                break;
            case PREFIX_ES:
                segmentAddress = "ES";
                break;
            case PREFIX_CS:
                segmentAddress = "CS";
                break;
            case PREFIX_SS:
                segmentAddress = "SS";
                break;
            case PREFIX_DS:
                segmentAddress = "DS";
                break;
            default:
                throw new IllegalStateException("PREFIX not detected");
        }
        return segmentAddress;
    }

    /**
     * Setzt den Inhalt eines 8Bit Registers.
     *
     * @param reg Register
     * @param value Inhalt
     */
    private void setReg8(int reg, int value) {
        switch (reg) {
            case REG_AL_AX:
                ax = (ax & 0x0000FF00) | (value & 0xFF);
                break;
            case REG_CL_CX:
                cx = (cx & 0x0000FF00) | (value & 0xFF);
                break;
            case REG_DL_DX:
                dx = (dx & 0x0000FF00) | (value & 0xFF);
                break;
            case REG_BL_BX:
                bx = (bx & 0x0000FF00) | (value & 0xFF);
                break;
            case REG_AH_SP:
                ax = (ax & 0x000000FF) | ((value & 0xFF) << 8);
                break;
            case REG_CH_BP:
                cx = (cx & 0x000000FF) | ((value & 0xFF) << 8);
                break;
            case REG_DH_SI:
                dx = (dx & 0x000000FF) | ((value & 0xFF) << 8);
                break;
            case REG_BH_DI:
                bx = (bx & 0x000000FF) | ((value & 0xFF) << 8);
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt den Inhalt eines 16Bit Registers.
     *
     * @param reg Register
     * @param value Inhalt
     */
    private void setReg16(int reg, int value) {
        switch (reg) {
            case REG_AL_AX:
                ax = value & 0xFFFF;
                break;
            case REG_CL_CX:
                cx = value & 0xFFFF;
                break;
            case REG_DL_DX:
                dx = value & 0xFFFF;
                break;
            case REG_BL_BX:
                bx = value & 0xFFFF;
                break;
            case REG_AH_SP:
                sp = value & 0xFFFF;
                break;
            case REG_CH_BP:
                bp = value & 0xFFFF;
                break;
            case REG_DH_SI:
                si = value & 0xFFFF;
                break;
            case REG_BH_DI:
                di = value & 0xFFFF;
                break;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Gibt ein Byte entsprechend des gesetzten MOD/RM-Feldes zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param updateIP <code>true</code> - wenn der Instructionpointer
     * aktualisiert werden soll , <code>false</code> - sonst
     * @return gelesenes Byte
     */
    private int getMODRM8(int MOD, int RM, boolean updateIP) {
        if (MOD == MOD_REG) {
            return getReg8(RM << 3);
        } else {
            return mms16.readMemoryByte(getAddressMODRM(MOD, RM, updateIP));
        }
    }

    /**
     * Gibt ein Wort entsprechend des gesetzten MOD/RM-Feldes zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param updateIP <code>true</code> - wenn der Instructionpointer
     * aktualisiert werden soll , <code>false</code> - sonst
     * @return gelesenes Wort
     */
    private int getMODRM16(int MOD, int RM, boolean updateIP) {
        if (MOD == MOD_REG) {
            return getReg16(RM << 3);
        } else {
            return mms16.readMemoryWord(getAddressMODRM(MOD, RM, updateIP));
        }
    }

    /**
     * Gibt einen Debug-String für ein Byte entsprechend des gesetzten
     * MOD/RM-Feldes zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param ipOffset Zu berücksichtigender Offset des Instruction Pointers
     * @return Debug-String
     */
    private String getMODRM8DebugString(int MOD, int RM, int ipOffset) {
        if (MOD == MOD_REG) {
            return getReg8String(RM << 3);
        } else {
            return getAddressMODRMDebugString(MOD, RM, ipOffset);
        }
    }

    /**
     * Gibt einen Debug-String für ein Wort entsprechend des gesetzten
     * MOD/RM-Feldes zurück.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param ipOffset Zu berücksichtigender Offset des Instruction Pointers
     * @return Debug-String
     */
    private String getMODRM16DebugString(int MOD, int RM, int ipOffset) {
        if (MOD == MOD_REG) {
            return getReg16DebugString(RM << 3);
        } else {
            return getAddressMODRMDebugString(MOD, RM, ipOffset);
        }
    }

    /**
     * Setzt den Wert eines Registers oder eine Speicherzelle basierend auf dem
     * MOD/RM-Feld als Byte.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param value Daten
     * @param updateIP <code>true</code> - wenn der Instruction Pointer
     * aktualisiert werden soll, <code>false</code> - sonst
     */
    private void setMODRM8(int MOD, int RM, int value, boolean updateIP) {
        if (MOD == MOD_REG) {
            setReg8(RM << 3, value);
        } else {
            mms16.writeMemoryByte(getAddressMODRM(MOD, RM, updateIP), (byte) (value & 0xFF));
        }
    }

    /**
     * Setzt den Wert eines Registers oder eine Speicherzelle basierend auf dem
     * MOD/RM-Feld als Wort.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param value Daten
     * @param updateIP <code>true</code> - wenn der Instruction Pointer
     * aktualisiert werden soll, <code>false</code> - sonst
     */
    private void setMODRM16(int MOD, int RM, int value, boolean updateIP) {
        if (MOD == MOD_REG) {
            setReg16(RM << 3, value);
        } else {
            mms16.writeMemoryWord(getAddressMODRM(MOD, RM, updateIP), value & 0xFFFF);
        }
    }

    /**
     * Liefert einen Debugstring mit der Berechneten Adresse eines MOD/RM
     * Feldes.
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param ipOffset zu berücksichtigender IP-Offset, dies kann durch
     * vorheriges verändern des Instruction Pointers notwendig sein
     * @return String mit Adressinformationen
     */
    private String getAddressMODRMDebugString(int MOD, int RM, int ipOffset) {
        String segmentAddress = getSegmentAddressAdressString(MOD, RM);
        String offset = getOffsetDebugString(MOD, RM, ipOffset);
        return segmentAddress + ":" + offset;
    }

    /**
     * Liefert eine Adresse anhand des MOD/RM Feldes
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @param updateIP Gibt an, ob der Instruction-Pointer aktualisiert werden
     * soll
     * @return Berechnete Adresse
     */
    private int getAddressMODRM(int MOD, int RM, boolean updateIP) {
        int segmentAddress = getSegmentAddress(MOD, RM);
        int offset = getOffset(MOD, RM, updateIP) & 0xFFFF;
        return (segmentAddress + offset) & 0xFFFFF;
    }

    /**
     * Berechnet die Anzahl der Taktzyklen für einen Speicherzugriff anhand des
     * MOD/RM Feldes
     *
     * @param MOD MOD-Feld
     * @param RM RM-Feld
     * @return Anzahl der benötigten Takte
     */
    private int getOpcodeCyclesEA(int MOD, int RM) {
        int cyclesEA = (prefix == NO_PREFIX) ? 0 : 2;

        switch (RM) {
            case RM_BP_DI:
            case RM_BX_SI:
                cyclesEA += 7;
                break;
            case RM_BP_SI:
            case RM_BX_DI:
                cyclesEA += 8;
                break;
            case RM_SI:
            case RM_DI:
            case RM_BX:
                cyclesEA += 5;
                break;
            case RM_DIRECT_BP:
                cyclesEA += (MOD == MOD_MEM_NO_DISPL) ? 6 : 5;
                break;
            default:
                throw new IllegalStateException("RM not detected");
        }

        switch (MOD) {
            case MOD_MEM_NO_DISPL:
                break;
            case MOD_MEM_8_DISPL:
            case MOD_MEM_16_DISPL:
                cyclesEA += 4;
                break;
        }
        return cyclesEA;
    }

    /**
     * Liest ein Segmentregister.
     *
     * @param SREG Segmentregister
     * @return Wert
     */
    private int getSReg(int SREG) {
        switch (SREG) {
            case SREG_CS:
                return cs;
            case SREG_ES:
                return es;
            case SREG_DS:
                return ds;
            case SREG_SS:
                return ss;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt ein Segmentregister.
     *
     * @param SREG Segmentregister
     * @param value Neuer Wert
     */
    private void setSReg(int SREG, int value) {
        switch (SREG) {
            case SREG_CS:
                cs = value & 0xFFFF;
                return;
            case SREG_ES:
                es = value & 0xFFFF;
                return;
            case SREG_DS:
                ds = value & 0xFFFF;
                return;
            case SREG_SS:
                ss = value & 0xFFFF;
                return;
            default:
                throw new IllegalStateException("Register not detected");
        }
    }

    /**
     * Setzt das angegebene Flag.
     *
     * @param flag Zu setzendes Flag
     */
    private void setFlag(int flag) {
        flags |= flag;
    }

    /**
     * Löscht das angegebene Flag.
     *
     * @param flag Zu löschendes Flag
     */
    private void clearFlag(int flag) {
        flags &= ~flag;
    }

    /**
     * Liefert den Status des angegebenen Flags.
     *
     * @param flag Zu prüfendes Flag
     * @return <code>true</code> - wenn Flag gesetzt , <code>false</code> -
     * sonst
     */
    private boolean getFlag(int flag) {
        return (flags & flag) != 0;
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
     * @param res Ergebniss der Addition
     */
    private void checkCarryFlagAdd16(int res) {
        if (res > 0xFFFF) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    /**
     * Setzt das Carry Flag entsprechend einer ausgeführten Subtraktion.
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
     * Setzt das Zero Flag entsprechend des angegebenen Byte Operanden.
     *
     * @param res Operand
     */
    private void checkZeroFlag8(int res) {
        if ((res & 0xFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    /**
     * Setzt das Zero Flag entsprechend des angegebenen Wort Operanden.
     *
     * @param res Operand
     */
    private void checkZeroFlag16(int res) {
        if ((res & 0xFFFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    /**
     * Setzt das Sign Flag entsprechend des angegebenen Byte Operanden.
     *
     * @param res Operand
     */
    private void checkSignFlag8(int res) {
        if (res < 0 || (res & 0x80) == 0x80) {
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

    /**
     * Setzt das Sign Flag entsprechend des angegebenen Wort Operanden.
     *
     * @param res Operand
     */
    private void checkSignFlag16(int res) {
        if (res < 0 || (res & 0x8000) == 0x8000) {
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

    /**
     * Setzt das Parity Flag entsprechend des angegebenen Operanden.
     *
     * @param res Operand
     */
    private void checkParityFlag(int res) {
        int sum = 0;
        for (int b = 7; b >= 0; b--) {
            if ((((res & 0xFF) >> b) & 0x01) == 1) {
                sum++;
            }
        }
        if ((sum & 0x01) == 0x01) {
            clearFlag(PARITY_FLAG);
        } else {
            setFlag(PARITY_FLAG);
        }
    }

    /**
     * Setzt das Overflow Flag entsprechend einer ausgeführten Subtraktion von
     * zwei Bytes op1+op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     * @param res Ergebnis der Operation
     */
    private void checkOverflowFlagAdd8(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x80)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Overflow Flag entsprechend einer ausgeführten Addition von zwei
     * Wörtern op1+op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     * @param res Ergebnis der Operation
     */
    private void checkOverflowFlagAdd16(int op1, int op2, int res) {
        if (((op1 & 0x8000) == 0x8000 && (op2 & 0x8000) == 0x8000 && (res & 0x8000) == 0x0000) | ((op1 & 0x8000) == 0x0000 && (op2 & 0x8000) == 0x0000 && (res & 0x8000) == 0x8000)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Overflow Flag entsprechend einer ausgeführten Subtraktion von
     * zwei Bytes op1-op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     * @param res Ergebnis der Operation
     */
    private void checkOverflowFlagSub8(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x80)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
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
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    /**
     * Setzt das Auxiliary Flag entsprechend einer ausgeführten Addition
     * op1+op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     */
    private void checkAuxiliaryCarryFlagAdd(int op1, int op2) {
        if (((op1 & 0xF) + (op2 & 0xF)) > 0xF) {
            setFlag(AUXILIARY_CARRY_FLAG);
        } else {
            clearFlag(AUXILIARY_CARRY_FLAG);
        }
    }

    /**
     * Setzt das Auxiliary Flag entsprechend einer ausgeführten Subtraktion
     * op1-op2.
     *
     * @param op1 Erster Operand
     * @param op2 Zweiter Operand
     */
    private void checkAuxiliaryCarryFlagSub(int op1, int op2) {
        if ((op1 & 0xF) < (op2 & 0xF)) {
            setFlag(AUXILIARY_CARRY_FLAG);
        } else {
            clearFlag(AUXILIARY_CARRY_FLAG);
        }
    }

    /**
     * Setzt die CPU in ihren Anfangszustand. Dabei werden die Flags gelöscht
     * und die Code Abarbeitung begint bei FFFF:0000.
     */
    @Override
    public void reset() {
        flags = 0x0000;
        ip = 0x0000;
        ds = 0x0000;
        es = 0x0000;
        ss = 0x0000;
        cs = 0xFFFF;
    }

    /**
     * Arbeitet einen Interrupt ab. Dazu werden die Flags sowie CS und IP auf
     * den Stack gespeichert und die Abarbeitung in der Interrupt Routine
     * fortgeführt.
     *
     * @param interruptID ID Interruptcode
     */
    private void interrupt(int interruptID) {
        push(flags);
        clearFlag(INTERRUPT_ENABLE_FLAG);
        clearFlag(TRAP_FLAG);
        push(cs);
        push(ip);
        ip = mms16.readMemoryWord(interruptID << 2);
        cs = mms16.readMemoryWord((interruptID << 2) + 2);
        isHalted = false;
    }

    /**
     * Führt einen CPU-Zyklus aus. Dies umfasst das Ausführen des nächsten
     * OPCodes sowie das Abfragen eventuell anstehender Interrupts.
     */
    private void executeCPUCycle() {
        if (!isHalted) {
            executeNextInstruction();
            if (mms16.isTimeout()) {
                // 10 ms Timeout
                updateTicks(49152);
                mms16.clearTimeout();
            }
        } else {
            updateTicks(3);
        }

        if (interruptSystem.getNMI()) {
            interrupt(0x02);
        } else if (getFlag(INTERRUPT_ENABLE_FLAG)) {
            int irq = InterruptSystem.getInstance().getPIC().getInterrupt();
            if (irq != -1) {
                if (debugger.isDebug()) {
                    debugger.addComment("Verarbeite Interrupt: " + irq);
                }
                interrupt(irq);
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

        // Mindestens 5 Takte sind für den kürzesten Befehl nötig
        while (tickBuffer >= 5) {
            executeCPUCycle();
        }
    }

    /**
     * Aktualisiert die Anzahl der verstrichenen Takte nach einer
     * Befehlsausführung.
     *
     * @param amount Anzahl der Takte
     */
    private void updateTicks(int amount) {
        tickBuffer -= amount;
    }

    /**
     * Speichert den Zustand der CPU in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeInt(ax);
        dos.writeInt(bx);
        dos.writeInt(cx);
        dos.writeInt(dx);
        dos.writeInt(sp);
        dos.writeInt(bp);
        dos.writeInt(si);
        dos.writeInt(di);
        dos.writeInt(ip);
        dos.writeInt(flags);
        dos.writeInt(cs);
        dos.writeInt(ds);
        dos.writeInt(ss);
        dos.writeInt(es);
        dos.writeInt(prefix);
        dos.writeInt(string_prefix);
        dos.writeBoolean(isHalted);
        dos.writeBoolean(stiWaiting);
        dos.writeInt(tickBuffer);
    }

    /**
     * Lädt den Zustand der CPU aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        ax = dis.readInt();
        bx = dis.readInt();
        cx = dis.readInt();
        dx = dis.readInt();
        sp = dis.readInt();
        bp = dis.readInt();
        si = dis.readInt();
        di = dis.readInt();
        ip = dis.readInt();
        flags = dis.readInt();
        cs = dis.readInt();
        ds = dis.readInt();
        ss = dis.readInt();
        es = dis.readInt();
        prefix = dis.readInt();
        string_prefix = dis.readInt();
        isHalted = dis.readBoolean();
        stiWaiting = dis.readBoolean();
        tickBuffer = dis.readInt();
    }

    /**
     * Aktiviert oder deaktiviert den Debugger.
     *
     * @param debug <code>true</code> zum Aktivierendes Debuggers,
     * <code>false</code> zum Deaktivieren des Debuggers
     */
    @Override
    public void setDebug(boolean debug) {
        debugger.setDebug(debug);
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
}
