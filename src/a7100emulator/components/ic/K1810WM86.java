/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.Debug.Decoder;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemClock;
import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class K1810WM86 implements Runnable {

    /**
     * Speicher
     */
    private SystemMemory memory = SystemMemory.getInstance();
    /**
     * E/A Ports
     */
    private SystemPorts ports = SystemPorts.getInstance();
    /**
     * Taktgeber
     */
    private SystemClock clock = SystemClock.getInstance();
    /**
     * Interrupt System
     */
    private InterruptSystem interruptSystem = InterruptSystem.getInstance();
    /**
     * Nicht verwendet 0F 60-6F C0,C1,C8,C9 D6 F1 82 001,100,110 83 001,100,110
     * 8C 1xx 8E 1xx 8F 001-111 C6 001-111 C7 001-111 D0 110 D1 110 D2 110 D3
     * 110 C6 001-111 C7 001-111 D0 110 D1 110 D2 110 D3 110 F6 001 F7 001 FE
     * 010-111 FF 111
     */
    /*
     * Unklarheiten 7E/7F nichteindeutige Beschreibung in ZVE Doku Vermutung 7E
     * - JLE_JNG 7F - JNLE_JG Alternative 7E - JNLE_JG 7F - unbesetzt
     */
    /**
     * Präfixe
     */
    private static final int PREFIX_ES = 0x26;
    private static final int PREFIX_CS = 0x2E;
    private static final int PREFIX_SS = 0x36;
    private static final int PREFIX_DS = 0x3E;
    private static final int NO_PREFIX = 0x00;
    /**
     * Datentransfer
     */
    private static final int PUSH_ES = 0x06;
    private static final int POP_ES = 0x07;
    private static final int PUSH_CS = 0x0E;
    private static final int PUSH_SS = 0x16;
    private static final int POP_SS = 0x17;
    private static final int PUSH_DS = 0x1E;
    private static final int POP_DS = 0x1F;
    private static final int PUSH_AX = 0x50;
    private static final int PUSH_CX = 0x51;
    private static final int PUSH_DX = 0x52;
    private static final int PUSH_BX = 0x53;
    private static final int PUSH_SP = 0x54;
    private static final int PUSH_BP = 0x55;
    private static final int PUSH_SI = 0x56;
    private static final int PUSH_DI = 0x57;
    private static final int PUSHF = 0x9C;
    private static final int POP_AX = 0x58;
    private static final int POP_CX = 0x59;
    private static final int POP_DX = 0x5A;
    private static final int POP_BX = 0x5B;
    private static final int POP_SP = 0x5C;
    private static final int POP_BP = 0x5D;
    private static final int POP_SI = 0x5E;
    private static final int POP_DI = 0x5F;
    private static final int POPF = 0x9D;
    private static final int SAHF = 0x9E;
    private static final int LAHF = 0x9F;
    private static final int XCHG_MODRM_REG_8 = 0x86;
    private static final int XCHG_MODRM_REG_16 = 0x87;
    private static final int MOV_REG_MODRM_8 = 0x88;
    private static final int MOV_REG_MODRM_16 = 0x89;
    private static final int MOV_MODRM_REG_8 = 0x8A;
    private static final int MOV_MODRM_REG_16 = 0x8B;
    private static final int MOV_MEM_AL = 0xA0;
    private static final int MOV_MEM_AX = 0xA1;
    private static final int MOV_AL_MEM = 0xA2;
    private static final int MOV_AX_MEM = 0xA3;
    private static final int MOV_IMM_AL = 0xB0;
    private static final int MOV_IMM_CL = 0xB1;
    private static final int MOV_IMM_DL = 0xB2;
    private static final int MOV_IMM_BL = 0xB3;
    private static final int MOV_IMM_AH = 0xB4;
    private static final int MOV_IMM_CH = 0xB5;
    private static final int MOV_IMM_DH = 0xB6;
    private static final int MOV_IMM_BH = 0xB7;
    private static final int MOV_IMM_AX = 0xB8;
    private static final int MOV_IMM_CX = 0xB9;
    private static final int MOV_IMM_DX = 0xBA;
    private static final int MOV_IMM_BX = 0xBB;
    private static final int MOV_IMM_SP = 0xBC;
    private static final int MOV_IMM_BP = 0xBD;
    private static final int MOV_IMM_SI = 0xBE;
    private static final int MOV_IMM_DI = 0xBF;
    private static final int XCHG_CX_AX = 0x91;
    private static final int XCHG_DX_AX = 0x92;
    private static final int XCHG_BX_AX = 0x93;
    private static final int XCHG_SP_AX = 0x94;
    private static final int XCHG_BP_AX = 0x95;
    private static final int XCHG_SI_AX = 0x96;
    private static final int XCHG_DI_AX = 0x97;
    private static final int LEA_MEM_REG = 0x8D;
    private static final int LES_MEM_REG = 0xC4;
    private static final int LDS_MEM_REG = 0xC5;
    private static final int XLAT = 0xD7;
    private static final int IN_IMM_AL = 0xE4;
    private static final int IN_IMM_AX = 0xE5;
    private static final int OUT_IMM_AL = 0xE6;
    private static final int OUT_IMM_AX = 0xE7;
    private static final int IN_DX_AL = 0xEC;
    private static final int IN_DX_AX = 0xED;
    private static final int OUT_DX_AL = 0xEE;
    private static final int OUT_DX_AX = 0xEF;
    /**
     * Arithmetik
     */
    private static final int ADD_REG_MODRM_8 = 0x00;
    private static final int ADD_REG_MODRM_16 = 0x01;
    private static final int ADD_MODRM_REG_8 = 0x02;
    private static final int ADD_MODRM_REG_16 = 0x03;
    private static final int ADD_IMM_AL = 0x04;
    private static final int ADD_IMM_AX = 0x05;
    private static final int ADC_REG_MODRM_8 = 0x10;
    private static final int ADC_REG_MODRM_16 = 0x11;
    private static final int ADC_MODRM_REG_8 = 0x12;
    private static final int ADC_MODRM_REG_16 = 0x13;
    private static final int ADC_IMM_AL = 0x14;
    private static final int ADC_IMM_AX = 0x15;
    private static final int SBB_REG_MODRM_8 = 0x18;
    private static final int SBB_REG_MODRM_16 = 0x19;
    private static final int SBB_MODRM_REG_8 = 0x1A;
    private static final int SBB_MODRM_REG_16 = 0x1B;
    private static final int SBB_IMM_AL = 0x1C;
    private static final int SBB_IMM_AX = 0x1D;
    private static final int SUB_REG_MODRM_8 = 0x28;
    private static final int SUB_REG_MODRM_16 = 0x29;
    private static final int SUB_MODRM_REG_8 = 0x2A;
    private static final int SUB_MODRM_REG_16 = 0x2B;
    private static final int SUB_IMM_AL = 0x2C;
    private static final int SUB_IMM_AX = 0x2D;
    private static final int CMP_REG_MODRM_8 = 0x38;
    private static final int CMP_REG_MODRM_16 = 0x39;
    private static final int CMP_MODRM_REG_8 = 0x3A;
    private static final int CMP_MODRM_REG_16 = 0x3B;
    private static final int CMP_IMM_AL = 0x3C;
    private static final int CMP_IMM_AX = 0x3D;
    private static final int DAA = 0x27;
    private static final int DAS = 0x2F;
    private static final int AAA = 0x37;
    private static final int AAS = 0x3F;
    private static final int AAM = 0xD4;
    private static final int AAD = 0xD5;
    private static final int INC_AX = 0x40;
    private static final int INC_CX = 0x41;
    private static final int INC_DX = 0x42;
    private static final int INC_BX = 0x43;
    private static final int INC_SP = 0x44;
    private static final int INC_BP = 0x45;
    private static final int INC_SI = 0x46;
    private static final int INC_DI = 0x47;
    private static final int DEC_AX = 0x48;
    private static final int DEC_CX = 0x49;
    private static final int DEC_DX = 0x4A;
    private static final int DEC_BX = 0x4B;
    private static final int DEC_SP = 0x4C;
    private static final int DEC_BP = 0x4D;
    private static final int DEC_SI = 0x4E;
    private static final int DEC_DI = 0x4F;
    private static final int CBW = 0x98;
    private static final int CWD = 0x99;
    /**
     * Zeichenkettenverarbeitung
     */
    private static final int MOVS_8 = 0xA4;
    private static final int MOVS_16 = 0xA5;
    private static final int CMPS_8 = 0xA6;
    private static final int CMPS_16 = 0xA7;
    private static final int STOS_8 = 0xAA;
    private static final int STOS_16 = 0xAB;
    private static final int LODS_8 = 0xAC;
    private static final int LODS_16 = 0xAD;
    private static final int SCAS_8 = 0xAE;
    private static final int SCAS_16 = 0xAF;
    /**
     * Logische Befehle
     */
    private static final int OR_REG_MODRM_8 = 0x08;
    private static final int OR_REG_MODRM_16 = 0x09;
    private static final int OR_MODRM_REG_8 = 0x0A;
    private static final int OR_MODRM_REG_16 = 0x0B;
    private static final int OR_IMM_AL = 0x0C;
    private static final int OR_IMM_AX = 0x0D;
    private static final int AND_REG_MODRM_8 = 0x20;
    private static final int AND_REG_MODRM_16 = 0x21;
    private static final int AND_MODRM_REG_8 = 0x22;
    private static final int AND_MODRM_REG_16 = 0x23;
    private static final int AND_IMM_AL = 0x24;
    private static final int AND_IMM_AX = 0x25;
    private static final int XOR_REG_MODRM_8 = 0x30;
    private static final int XOR_REG_MODRM_16 = 0x31;
    private static final int XOR_MODRM_REG_8 = 0x32;
    private static final int XOR_MODRM_REG_16 = 0x33;
    private static final int XOR_IMM_AL = 0x34;
    private static final int XOR_IMM_AX = 0x35;
    private static final int TEST_REG_MODRM_8 = 0x84;
    private static final int TEST_REG_MODRM_16 = 0x85;
    private static final int TEST_IMM_AL = 0xA8;
    private static final int TEST_IMM_AX = 0xA9;
    /**
     * Übergabe der Steuerung
     */
    private static final int JO = 0x70;
    private static final int JNO = 0x71;
    private static final int JB_JNAE_JC = 0x72;
    private static final int JNB_JAE_JNC = 0x73;
    private static final int JE_JZ = 0x74;
    private static final int JNE_JNZ = 0x75;
    private static final int JBE_JNA = 0x76;
    private static final int JNBE_JA = 0x77;
    private static final int JS = 0x78;
    private static final int JNS = 0x79;
    private static final int JP_JPE = 0x7A;
    private static final int JNP_JPO = 0x7B;
    private static final int JL_JNGE = 0x7C;
    private static final int JNL_JGE = 0x7D;
    private static final int JLE_JNG = 0x7E;
    private static final int JNLE_JG = 0x7F;
    private static final int CALL_FAR_PROC = 0x9A;
    private static final int CALL_NEAR_PROC = 0xE8;
    private static final int RET_IN_SEG_IMM = 0xC2;
    private static final int RET_IN_SEG = 0xC3;
    private static final int RET_INTER_SEG_IMM = 0xCA;
    private static final int RET_INTER_SEG = 0xCB;
    private static final int INT3 = 0xCC;
    private static final int INT_IMM = 0xCD;
    private static final int INTO = 0xCE;
    private static final int IRET = 0xCF;
    private static final int LOOPNE_LOOPNZ = 0xE0;
    private static final int LOOPE_LOOPZ = 0xE1;
    private static final int LOOP = 0xE2;
    private static final int JCXZ = 0xE3;
    private static final int JMP_NEAR_LABEL = 0xE9;
    private static final int JMP_FAR_LABEL = 0xEA;
    private static final int JMP_SHORT_LABEL = 0xEB;
    private static final int REPNE_REPNZ = 0xF2;
    private static final int REP_REPE_REPZ = 0xF3;
    /**
     * Prozessorsteuerung
     */
    private static final int NOP = 0x90;
    private static final int WAIT = 0x9B;
    private static final int LOCK = 0xF0;
    private static final int HLT = 0xF4;
    private static final int CMC = 0xF5;
    private static final int CLC = 0xF8;
    private static final int STC = 0xF9;
    private static final int CLI = 0xFA;
    private static final int STI = 0xFB;
    private static final int CLD = 0xFC;
    private static final int STD = 0xFD;
    private static final int ESC0 = 0xD8;
    private static final int ESC1 = 0xD9;
    private static final int ESC2 = 0xDA;
    private static final int ESC3 = 0xDB;
    private static final int ESC4 = 0xDC;
    private static final int ESC5 = 0xDD;
    private static final int ESC6 = 0xDE;
    private static final int ESC7 = 0xDF;
    /**
     * Zweier - Nur Reg
     */
    // 80
    private static final int _80_ADD_IMM_REG_8_NOSIGN = 0x00;
    private static final int _80_OR_IMM_REG_8_NOSIGN = 0x08;
    private static final int _80_ADC_IMM_REG_8_NOSIGN = 0x10;
    private static final int _80_SBB_IMM_REG_8_NOSIGN = 0x18;
    private static final int _80_AND_IMM_REG_8_NOSIGN = 0x20;
    private static final int _80_SUB_IMM_REG_8_NOSIGN = 0x28;
    private static final int _80_XOR_IMM_REG_8_NOSIGN = 0x30;
    private static final int _80_CMP_IMM_REG_8_NOSIGN = 0x38;
    // 81
    private static final int _81_ADD_IMM_REG_16_NOSIGN = 0x00;
    private static final int _81_OR_IMM_REG_16_NOSIGN = 0x08;
    private static final int _81_ADC_IMM_REG_16_NOSIGN = 0x10;
    private static final int _81_SBB_IMM_REG_16_NOSIGN = 0x18;
    private static final int _81_AND_IMM_REG_16_NOSIGN = 0x20;
    private static final int _81_SUB_IMM_REG_16_NOSIGN = 0x28;
    private static final int _81_XOR_IMM_REG_16_NOSIGN = 0x30;
    private static final int _81_CMP_IMM_REG_16_NOSIGN = 0x38;
    // 82
    private static final int _82_ADD_IMM_REG_8_SIGN = 0x00;
    private static final int _82_ADC_IMM_REG_8_SIGN = 0x10;
    private static final int _82_SBB_IMM_REG_8_SIGN = 0x18;
    private static final int _82_SUB_IMM_REG_8_SIGN = 0x28;
    private static final int _82_CMP_IMM_REG_8_SIGN = 0x38;
    // 83
    private static final int _83_ADD_IMM_REG_16_SIGN = 0x00;
    private static final int _83_ADC_IMM_REG_16_SIGN = 0x10;
    private static final int _83_SBB_IMM_REG_16_SIGN = 0x18;
    private static final int _83_SUB_IMM_REG_16_SIGN = 0x28;
    private static final int _83_CMP_IMM_REG_16_SIGN = 0x38;
    // 8C
    private static final int _8C_MOV_ES_MODRM_16 = 0x00;
    private static final int _8C_MOV_CS_MODRM_16 = 0x08;
    private static final int _8C_MOV_SS_MODRM_16 = 0x10;
    private static final int _8C_MOV_DS_MODRM_16 = 0x18;
    // 8E
    private static final int _8E_MOV_MODRM_ES_16 = 0x00;
    private static final int _8E_MOV_MODRM_CS_16 = 0x08;
    private static final int _8E_MOV_MODRM_SS_16 = 0x10;
    private static final int _8E_MOV_MODRM_DS_16 = 0x18;
    // 8F
    private static final int _8F_POP_MODRM_16 = 0x00;
    // C6
    private static final int _C6_MOV_IMM_MEM_8 = 0x00;
    // C7
    private static final int _C7_MOV_IMM_MEM_16 = 0x00;
    // D0
    private static final int _D0_ROL_MODRM_1_8 = 0x00;
    private static final int _D0_ROR_MODRM_1_8 = 0x08;
    private static final int _D0_RCL_MODRM_1_8 = 0x10;
    private static final int _D0_RCR_MODRM_1_8 = 0x18;
    private static final int _D0_SAL_SHL_MODRM_1_8 = 0x20;
    private static final int _D0_SHR_MODRM_1_8 = 0x28;
    private static final int _D0_SAR_MODRM_1_8 = 0x38;
    // D1
    private static final int _D1_ROL_MODRM_1_16 = 0x00;
    private static final int _D1_ROR_MODRM_1_16 = 0x08;
    private static final int _D1_RCL_MODRM_1_16 = 0x10;
    private static final int _D1_RCR_MODRM_1_16 = 0x18;
    private static final int _D1_SAL_SHL_MODRM_1_16 = 0x20;
    private static final int _D1_SHR_MODRM_1_16 = 0x28;
    private static final int _D1_SAR_MODRM_1_16 = 0x38;
    // D2
    private static final int _D2_ROL_MODRM_CL_8 = 0x00;
    private static final int _D2_ROR_MODRM_CL_8 = 0x08;
    private static final int _D2_RCL_MODRM_CL_8 = 0x10;
    private static final int _D2_RCR_MODRM_CL_8 = 0x18;
    private static final int _D2_SAL_SHL_MODRM_CL_8 = 0x20;
    private static final int _D2_SHR_MODRM_CL_8 = 0x28;
    private static final int _D2_SAR_MODRM_CL_8 = 0x38;
    // D3
    private static final int _D3_ROL_MODRM_CL_16 = 0x00;
    private static final int _D3_ROR_MODRM_CL_16 = 0x08;
    private static final int _D3_RCL_MODRM_CL_16 = 0x10;
    private static final int _D3_RCR_MODRM_CL_16 = 0x18;
    private static final int _D3_SAL_SHL_MODRM_CL_16 = 0x20;
    private static final int _D3_SHR_MODRM_CL_16 = 0x28;
    private static final int _D3_SAR_MODRM_CL_16 = 0x38;
    // F6
    private static final int _F6_TEST_IMM_MODRM_8 = 0x00;
    private static final int _F6_NOT_MODRM_8 = 0x10;
    private static final int _F6_NEG_MODRM_8 = 0x18;
    private static final int _F6_MUL_MODRM_8 = 0x20;
    private static final int _F6_IMUL_MODRM_8 = 0x28;
    private static final int _F6_DIV_MODRM_8 = 0x30;
    private static final int _F6_IDIV_MODRM_8 = 0x38;
    // F7
    private static final int _F7_TEST_IMM_MODRM_16 = 0x00;
    private static final int _F7_NOT_MODRM_16 = 0x10;
    private static final int _F7_NEG_MODRM_16 = 0x18;
    private static final int _F7_MUL_MODRM_16 = 0x20;
    private static final int _F7_IMUL_MODRM_16 = 0x28;
    private static final int _F7_DIV_MODRM_16 = 0x30;
    private static final int _F7_IDIV_MODRM_16 = 0x38;
    // FE
    private static final int _FE_INC_MODRM_8 = 0x00;
    private static final int _FE_DEC_MODRM_8 = 0x08;
    // FF
    private static final int _FF_INC_MEM_16 = 0x00;
    private static final int _FF_DEC_MEM_16 = 0x08;
    private static final int _FF_CALL_MODRM_16 = 0x10;
    private static final int _FF_CALL_MEM_16 = 0x18;
    private static final int _FF_JMP_MODRM_16 = 0x20;
    private static final int _FF_JMP_MEM_16 = 0x28;
    private static final int _FF_PUSH_MEM_16 = 0x30;
    /**
     * MOD
     */
    private static final int MOD_MEM_NO_DISPL = 0x00;
    private static final int MOD_MEM_8_DISPL = 0x40;
    private static final int MOD_MEM_16_DISPL = 0x80;
    private static final int MOD_REG = 0xC0;
    /**
     * REG
     */
    private static final int REG_AL_AX = 0x00;
    private static final int REG_CL_CX = 0x08;
    private static final int REG_DL_DX = 0x10;
    private static final int REG_BL_BX = 0x18;
    private static final int REG_AH_SP = 0x20;
    private static final int REG_CH_BP = 0x28;
    private static final int REG_DH_SI = 0x30;
    private static final int REG_BH_DI = 0x38;
    /**
     * SREG
     */
    private static final int SREG_ES = 0x00;
    private static final int SREG_CS = 0x01;
    private static final int SREG_SS = 0x02;
    private static final int SREG_DS = 0x03;
    /**
     * RM
     */
    private static final int RM_BX_SI = 0x00;
    private static final int RM_BX_DI = 0x01;
    private static final int RM_BP_SI = 0x02;
    private static final int RM_BP_DI = 0x03;
    private static final int RM_SI = 0x04;
    private static final int RM_DI = 0x05;
    private static final int RM_DIRECT_BP = 0x06;
    private static final int RM_BX = 0x07;
    /**
     * MOD+RM
     */
    private static final int RM_BX_SI_MOD_NO_DISPL = 0x00;
    private static final int RM_BX_DI_MOD_NO_DISPL = 0x01;
    private static final int RM_BP_SI_MOD_NO_DISPL = 0x02;
    private static final int RM_BP_DI_MOD_NO_DISPL = 0x03;
    private static final int RM_SI_MOD_NO_DISPL = 0x04;
    private static final int RM_DI_MOD_NO_DISPL = 0x05;
    private static final int RM_DIRECT_BP_MOD_NO_DISPL = 0x06;
    private static final int RM_BX_MOD_NO_DISPL = 0x07;
    private static final int RM_BX_SI_MOD_DISPL_8 = 0x40;
    private static final int RM_BX_DI_MOD_DISPL_8 = 0x41;
    private static final int RM_BP_SI_MOD_DISPL_8 = 0x42;
    private static final int RM_BP_DI_MOD_DISPL_8 = 0x43;
    private static final int RM_SI_MOD_DISPL_8 = 0x44;
    private static final int RM_DI_MOD_DISPL_8 = 0x45;
    private static final int RM_DIRECT_BP_MOD_DISPL_8 = 0x46;
    private static final int RM_BX_MOD_DISPL_8 = 0x47;
    private static final int RM_BX_SI_MOD_DISPL_16 = 0x80;
    private static final int RM_BX_DI_MOD_DISPL_16 = 0x81;
    private static final int RM_BP_SI_MOD_DISPL_16 = 0x82;
    private static final int RM_BP_DI_MOD_DISPL_16 = 0x83;
    private static final int RM_SI_MOD_DISPL_16 = 0x84;
    private static final int RM_DI_MOD_DISPL_16 = 0x85;
    private static final int RM_DIRECT_BP_MOD_DISPL_16 = 0x86;
    private static final int RM_BX_MOD_DISPL_16 = 0x87;
    /**
     * Tests
     */
    private static final int TEST_D = 0x02;
    private static final int TEST_W = 0x01;
    private static final int TEST_MOD = 0xC0;
    private static final int TEST_REG = 0x38;
    private static final int TEST_RM = 0x07;
    private static final int TEST_MOD_RM = 0xC7;
    /**
     * Flags
     */
    private static final int CARRY_FLAG = 0x0001;
    private static final int PARITY_FLAG = 0x0004;
    private static final int AUXILIARY_CARRY_FLAG = 0x0010;
    private static final int ZERO_FLAG = 0x0040;
    private static final int SIGN_FLAG = 0x0080;
    private static final int TRAP_FLAG = 0x0100;
    private static final int INTERRUPT_ENABLE_FLAG = 0x0200;
    private static final int DIRECTION_FLAG = 0x0400;
    private static final int OVERFLOW_FLAG = 0x0800;
    /*
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
    /*
     * Instruktionpointer
     */
    private int ip;
    /*
     * Statusregister
     */
    private int flags;
    /*
     * Codesegment-Register
     */
    private int cs;
    /*
     * Datensegment Register
     */
    private int ds;
    /*
     * Stacksegment Register
     */
    private int ss;
    /*
     * Extrasegment Register
     */
    private int es;
    /**
     * Aktueller Präfix
     */
    private int prefix = NO_PREFIX;
    /**
     * String-Prefix
     */
    private int string_prefix = NO_PREFIX;
    /**
     * Haltemodus
     */
    private boolean isHalted = false;
    /**
     * Statistik
     */
    private int[] opCodeStatistic = new int[256];
    /**
     * Debugger
     */
    private Decoder decoder = Decoder.getInstance();
    private int decoderAddress;
    private String decoderString;
    private PrintStream debugFile = null;
    private boolean debug = false;
    private int debugstart = -1;//0xF800 * 16 + 0x459B;

    public K1810WM86() {
        if (debug) {
            try {
                debugFile = new PrintStream(new FileOutputStream("K1810WM86.log"));
            } catch (Exception ex) {
                Logger.getLogger(K1810WM86.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void executeNextInstruction() {
        int opcode1 = memory.readByte(cs * 16 + (ip++));

        if (debug && (opcode1 != PREFIX_CS && opcode1 != PREFIX_SS && opcode1 != PREFIX_DS && opcode1 != PREFIX_ES)) {
            decoderAddress = cs * 16 + (ip - 1 - (prefix == NO_PREFIX ? 0 : 1));
            decoderString = String.format("%04X", cs) + ":" + String.format("%04X", ip - 1 - (prefix == NO_PREFIX ? 0 : 1)) + " ";
        }

        opCodeStatistic[opcode1]++;

        clock.updateClock(3);

        switch (opcode1) {
            /*
             * Datentransfer
             */
            case MOV_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, getReg8(opcode2 & TEST_REG), true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "MOV " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case MOV_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getReg16(opcode2 & TEST_REG), true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case MOV_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                setReg8(opcode2 & TEST_REG, getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "MOV " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case MOV_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                setReg16(opcode2 & TEST_REG, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "MOV " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case MOV_MEM_AL: {
                int segment = getSegment(ds);
                int offset = memory.readWord(cs * 16 + (ip++));
                ip++;
                setReg8(REG_AL_AX, memory.readByte(segment * 16 + offset));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "MOV AL," + getSegmentString("DS") + ":" + Integer.toHexString(offset);
                }
            }
            break;
            case MOV_MEM_AX: {
                int segment = getSegment(ds);
                int offset = memory.readWord(cs * 16 + (ip++));
                ip++;
                setReg16(REG_AL_AX, memory.readWord(segment * 16 + offset));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "MOV AX," + getSegmentString("DS") + ":" + Integer.toHexString(offset);
                }
            }
            break;
            case MOV_AL_MEM: {
                int segment = getSegment(ds);
                int offset = memory.readWord(cs * 16 + (ip++));
                ip++;
                memory.writeByte(segment * 16 + offset, getReg8(REG_AL_AX));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "MOV " + getSegmentString("DS") + ":" + Integer.toHexString(offset) + ",AL";
                }
            }
            break;
            case MOV_AX_MEM: {
                int segment = getSegment(ds);
                int offset = memory.readWord(cs * 16 + (ip++));
                ip++;
                memory.writeWord(segment * 16 + offset, getReg16(REG_AL_AX));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "MOV " + getSegmentString("DS") + ":" + Integer.toHexString(offset) + ",AX";
                }
            }
            break;
            case MOV_IMM_AL: {
                setReg8(REG_AL_AX, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_CL: {
                setReg8(REG_CL_CX, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV CL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_DL: {
                setReg8(REG_DL_DX, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV DL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_BL: {
                setReg8(REG_BL_BX, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV BL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_AH: {
                setReg8(REG_AH_SP, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV AH," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_CH: {
                setReg8(REG_CH_BP, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV CH," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_DH: {
                setReg8(REG_DH_SI, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV DH," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_BH: {
                setReg8(REG_BH_DI, memory.readByte(cs * 16 + (ip++)));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV BH," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case MOV_IMM_AX: {
                setReg16(REG_AL_AX, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_CX: {
                setReg16(REG_CL_CX, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV CX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_DX: {
                setReg16(REG_DL_DX, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV DX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_BX: {
                setReg16(REG_BL_BX, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV BX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_SP: {
                setReg16(REG_AH_SP, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV SP," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_BP: {
                setReg16(REG_CH_BP, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV BP," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_SI: {
                setReg16(REG_DH_SI, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV SI," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case MOV_IMM_DI: {
                setReg16(REG_BH_DI, memory.readWord(cs * 16 + (ip++)));
                ip++;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "MOV DI," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case XCHG_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                setReg8(opcode2 & TEST_REG, getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false));
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XCHG " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case XCHG_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                setReg16(opcode2 & TEST_REG, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false));
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XCHG " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case XCHG_CX_AX: {
                int op1 = getReg16(REG_CL_CX);
                setReg16(REG_CL_CX, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,CX";
                }
            }
            break;
            case XCHG_DX_AX: {
                int op1 = getReg16(REG_DL_DX);
                setReg16(REG_DL_DX, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,DX";
                }
            }
            break;
            case XCHG_BX_AX: {
                int op1 = getReg16(REG_BL_BX);
                setReg16(REG_BL_BX, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,BX";
                }
            }
            break;
            case XCHG_SP_AX: {
                int op1 = getReg16(REG_AH_SP);
                setReg16(REG_AH_SP, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,SP";
                }
            }
            break;
            case XCHG_BP_AX: {
                int op1 = getReg16(REG_AH_SP);
                setReg16(REG_AH_SP, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,BP";
                }
            }
            break;
            case XCHG_SI_AX: {
                int op1 = getReg16(REG_DH_SI);
                setReg16(REG_DH_SI, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,SI";
                }
            }
            break;
            case XCHG_DI_AX: {
                int op1 = getReg16(REG_BH_DI);
                setReg16(REG_BH_DI, REG_AL_AX);
                setReg16(REG_AL_AX, op1);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "XCHG AX,DI";
                }
            }
            break;
            case PUSH_ES: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getSReg(SREG_ES));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "PUSH ES";
                }
            }
            break;
            case PUSH_CS: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getSReg(SREG_CS));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "PUSH CS";
                }
            }
            break;
            case PUSH_SS: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getSReg(SREG_SS));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "PUSH SS";
                }
            }
            break;
            case PUSH_DS: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getSReg(SREG_DS));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "PUSH DS";
                }
            }
            break;
            case PUSH_AX: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_AL_AX));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH AX";
                }
            }
            break;
            case PUSH_CX: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_CL_CX));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH CX";
                }
            }
            break;
            case PUSH_DX: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_DL_DX));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH DX";
                }
            }
            break;
            case PUSH_BX: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_BL_BX));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH BX";
                }
            }
            break;
            case PUSH_SP: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_AH_SP));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH SP";
                }
            }
            break;
            case PUSH_BP: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_CH_BP));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH BP";
                }
            }
            break;
            case PUSH_SI: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_DH_SI));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH SI";
                }
            }
            break;
            case PUSH_DI: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) getReg16(REG_BH_DI));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "PUSH DI";
                }
            }
            break;
            case POP_ES: {
                setSReg(SREG_ES, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP ES";
                }
            }
            break;
            case POP_SS: {
                setSReg(SREG_SS, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP SS";
                }
            }
            break;
            case POP_DS: {
                setSReg(SREG_DS, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP DS";
                }
            }
            break;
            case POP_AX: {
                setReg16(REG_AL_AX, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP AX";
                }
            }
            break;
            case POP_CX: {
                setReg16(REG_CL_CX, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP CX";
                }
            }
            break;
            case POP_DX: {
                setReg16(REG_DL_DX, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP DX";
                }
            }
            break;
            case POP_BX: {
                setReg16(REG_BL_BX, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP BX";
                }
            }
            break;
            case POP_SP: {
                setReg16(REG_AH_SP, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP SP";
                }
            }
            break;
            case POP_BP: {
                setReg16(REG_CH_BP, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP BP";
                }
            }
            break;
            case POP_SI: {
                setReg16(REG_DH_SI, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP SI";
                }
            }
            break;
            case POP_DI: {
                setReg16(REG_BH_DI, memory.readWord(getReg16(REG_AH_SP)));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POP DI";
                }
            }
            break;
            case PUSHF: {
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) flags);
                clock.updateClock(10);
                if (debug) {
                    decoderString += "PUSHF";
                }
            }
            break;
            case POPF: {
                flags = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(8);
                if (debug) {
                    decoderString += "POPF";
                }
            }
            break;
            case SAHF: {
                flags &= 0xFF00;
                flags |= getReg8(REG_AH_SP) & 0xFF;
                clock.updateClock(4);
                if (debug) {
                    decoderString += "SAHF";
                }
            }
            break;
            case LAHF: {
                setReg8(REG_AH_SP, flags & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "LAHF";
                }
            }
            break;
            case LEA_MEM_REG: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                setReg16(opcode2 & TEST_REG, getOffset(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                clock.updateClock(2 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "LEA " + getReg16String(opcode2 & TEST_REG) + "," + getOffsetString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case LES_MEM_REG: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int adress = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, memory.readWord(adress));
                setSReg(SREG_ES, memory.readWord(adress + 2));
                clock.updateClock(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "LES " + getReg16String(opcode2 & TEST_REG) + "," + getAddressMODRMString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case LDS_MEM_REG: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int adress = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                setReg16(opcode2 & TEST_REG, memory.readWord(adress));
                setSReg(SREG_DS, memory.readWord(adress + 2));
                clock.updateClock(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "LDS " + getReg16String(opcode2 & TEST_REG) + "," + getAddressMODRMString(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case XLAT: {
                setReg8(REG_AL_AX, memory.readByte(getReg16(REG_BL_BX) + getReg8(REG_AL_AX)));
                clock.updateClock(11);
                if (debug) {
                    decoderString += "XLAT";
                }
            }
            break;
            case IN_IMM_AL: {
                int port = memory.readByte(cs * 16 + (ip++));
                int value = ports.readByte(port);
                setReg8(REG_AL_AX, value);
                clock.updateClock(10);
                if (debug) {
                    decoderString += "IN " + Integer.toHexString(port) + "h, AL (" + Integer.toHexString(value) + "h)";
                }
            }
            break;
            case IN_IMM_AX: {
                int port = memory.readByte(cs * 16 + (ip++));
                int value = ports.readWord(port);
                setReg16(REG_AL_AX, value);
                clock.updateClock(10);
                if (debug) {
                    decoderString += "IN " + Integer.toHexString(port) + ", AX (" + Integer.toHexString(value) + "h)";
                }
            }
            break;
            case IN_DX_AL: {
                setReg8(REG_AL_AX, ports.readByte(getReg16(REG_DL_DX)));
                clock.updateClock(8);
                if (debug) {
                    decoderString += "IN AL,DX";
                }
            }
            break;
            case IN_DX_AX: {
                setReg16(REG_AL_AX, ports.readWord(getReg16(REG_DL_DX)));
                clock.updateClock(8);
                if (debug) {
                    decoderString += "IN AX,DX";
                }
            }
            break;
            case OUT_IMM_AL: {
                int port = memory.readByte(cs * 16 + (ip++));
                ports.writeByte(port, (byte) getReg8(REG_AL_AX));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "OUT " + Integer.toHexString(port) + "h,AL (" + Integer.toHexString((byte) getReg8(REG_AL_AX)) + ")";
                }
            }
            break;
            case OUT_IMM_AX: {
                int port = memory.readByte(cs * 16 + (ip++));
                ports.writeWord(port, (short) getReg16(REG_AL_AX));
                clock.updateClock(10);
                if (debug) {
                    decoderString += "OUT " + Integer.toHexString(port) + "h,AX (" + Integer.toHexString((short) getReg16(REG_AL_AX)) + ")";
                }
            }
            break;
            case OUT_DX_AL: {
                ports.writeByte(getReg16(REG_DL_DX), getReg8(REG_AL_AX));
                clock.updateClock(8);
                if (debug) {
                    decoderString += "OUT DX,AL (" + Integer.toHexString((short) getReg16(REG_DL_DX)) + "," + Integer.toHexString(getReg8(REG_AL_AX)) + ")";
                }
            }
            break;
            case OUT_DX_AX: {
                ports.writeWord(getReg16(REG_DL_DX), (short) getReg16(REG_AL_AX));
                clock.updateClock(8);
                if (debug) {
                    decoderString += "OUT DX,AX (" + Integer.toHexString((short) getReg16(REG_DL_DX)) + "," + Integer.toHexString((short) getReg16(REG_AL_AX)) + ")";
                }
            }
            break;

            /**
             * Arithmetik
             */
            case ADD_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 + op2;
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADD " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case ADD_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADD " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case ADD_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 + op2;
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADD " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case ADD_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADD " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case ADD_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 + op2;
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "ADD AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case ADD_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "ADD AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case ADC_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 + op2;
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADC " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case ADC_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADC " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case ADC_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 + op2;
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADC " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case ADC_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "ADC " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case ADC_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 + op2;
                checkCarryFlagAdd8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagAdd8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "ADC AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case ADC_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 + op2;
                checkCarryFlagAdd16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "ADC AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case SUB_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SUB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case SUB_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SUB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case SUB_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SUB " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case SUB_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SUB " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0)+" ("+Integer.toHexString(op1)+"-"+Integer.toHexString(op2)+")";
                }
            }
            break;
            case SUB_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "SUB AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case SUB_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "SUB AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case CMP_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "CMP " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case CMP_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "CMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG) + "(" + Integer.toHexString(op1) + "h," + Integer.toHexString(op2) + "h)";
                }
            }
            break;
            case CMP_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "CMP " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case CMP_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "CMP " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0)+" ("+Integer.toHexString(op1)+","+Integer.toHexString(op2)+")";
                }
            }
            break;
            case CMP_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "CMP AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case CMP_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "CMP AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h ("+Integer.toHexString(op1)+","+Integer.toHexString(op2)+")";
                }
            }
            break;
            case SBB_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SBB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case SBB_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SBB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case SBB_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg8(opcode2 & TEST_REG);
                int op2 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SBB " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case SBB_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getReg16(opcode2 & TEST_REG);
                int op2 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "SBB " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case SBB_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub8(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                checkOverflowFlagSub8(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "SBB AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case SBB_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                if (getFlag(CARRY_FLAG)) {
                    op2++;
                }
                int res = op1 - op2;
                checkCarryFlagSub16(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(op1, op2, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(op1, op2);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "SBB AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case INC_AX: {
                int res = ax + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(ax, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(ax, 1);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC AX";
                }
            }
            break;
            case INC_CX: {
                int res = cx + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(cx, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(cx, 1);
                setReg16(REG_CL_CX, res & 0xFFFF);
                if (debug) {
                    decoderString += "INC CX";
                }
            }
            break;
            case INC_DX: {
                int res = dx + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(dx, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(dx, 1);
                setReg16(REG_DL_DX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC DX";
                }
            }
            break;
            case INC_BX: {
                int res = bx + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(bx, 1, res);
                checkParityFlag(res);
                setReg16(REG_BL_BX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC BX (" + res + ")";
                }
            }
            break;
            case INC_SP: {
                int res = sp + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(sp, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(sp, 1);
                setReg16(REG_AH_SP, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC SP";
                }
            }
            break;
            case INC_BP: {
                int res = bp + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(bp, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(bp, 1);
                setReg16(REG_CH_BP, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC BP";
                }
            }
            break;
            case INC_SI: {
                int res = si + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(si, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(si, 1);
                setReg16(REG_DH_SI, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC SI";
                }
            }
            break;
            case INC_DI: {
                int res = di + 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagAdd16(di, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagAdd(di, 1);
                setReg16(REG_BH_DI, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "INC DI";
                }
            }
            break;
            case DEC_AX: {
                int res = ax - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(ax, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(ax, 1);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC AX";
                }
            }
            break;
            case DEC_CX: {
                int res = cx - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(cx, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(cx, 1);
                setReg16(REG_CL_CX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC CX";
                }
            }
            break;
            case DEC_DX: {
                int res = dx - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(dx, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(dx, 1);
                setReg16(REG_DL_DX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC DX";
                }
            }
            break;
            case DEC_BX: {
                int res = bx - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(bx, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(bx, 1);
                setReg16(REG_BL_BX, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC BX";
                }
            }
            break;
            case DEC_SP: {
                int res = sp - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(sp, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(sp, 1);
                setReg16(REG_AH_SP, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC SP";
                }
            }
            break;
            case DEC_BP: {
                int res = bp - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(bp, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(bp, 1);
                setReg16(REG_CH_BP, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC BP";
                }
            }
            break;
            case DEC_SI: {
                int res = si - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(si, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(si, 1);
                setReg16(REG_DH_SI, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC SI";
                }
            }
            break;
            case DEC_DI: {
                int res = di - 1;
                checkZeroFlag16(res);
                checkSignFlag16(res);
                checkOverflowFlagSub16(di, 1, res);
                checkParityFlag(res);
                checkAuxiliaryCarryFlagSub(di, 1);
                setReg16(REG_BH_DI, res & 0xFFFF);
                clock.updateClock(3);
                if (debug) {
                    decoderString += "DEC DI";
                }
            }
            break;
            case DAA: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) | (al & 0xF) > 9) {
                    setReg8(REG_AL_AX, al + 6);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                if (getFlag(CARRY_FLAG) | al > 0x9F) {
                    setReg8(REG_AL_AX, al + 0x60);
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                checkZeroFlag8(getReg8(REG_AL_AX));
                checkSignFlag8(getReg8(REG_AL_AX));
                checkParityFlag(getReg8(REG_AL_AX));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "DAA";
                }
            }
            break;
            case DAS: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) | (al & 0xF) > 9) {
                    setReg8(REG_AL_AX, al - 6);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                if (getFlag(CARRY_FLAG) | al > 0x9F) {
                    setReg8(REG_AL_AX, al - 0x60);
                    setFlag(CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                }
                checkZeroFlag8(getReg8(REG_AL_AX));
                checkSignFlag8(getReg8(REG_AL_AX));
                checkParityFlag(getReg8(REG_AL_AX));
                clock.updateClock(4);
                if (debug) {
                    decoderString += "DAS";
                }
            }
            break;
            case AAA: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || (al & 0xF) > 9) {
                    setReg8(REG_AL_AX, (al + 6) & 0xF);
                    setReg8(REG_AH_SP, (getReg8(REG_AH_SP) + 1) & 0xF);
                    setFlag(CARRY_FLAG);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                clock.updateClock(8);
                if (debug) {
                    decoderString += "AAA";
                }
            }
            break;
            case AAS: {
                int al = getReg8(REG_AL_AX);
                if (getFlag(AUXILIARY_CARRY_FLAG) || (al & 0xF) > 9) {
                    setReg8(REG_AL_AX, (al - 6) & 0xF);
                    setReg8(REG_AH_SP, (getReg8(REG_AH_SP) - 1) & 0xFF);
                    setFlag(CARRY_FLAG);
                    setFlag(AUXILIARY_CARRY_FLAG);
                } else {
                    clearFlag(CARRY_FLAG);
                    clearFlag(AUXILIARY_CARRY_FLAG);
                }
                clock.updateClock(8);
                if (debug) {
                    decoderString += "AAS";
                }
            }
            break;
            case AAM: {
                int al = getReg8(REG_AL_AX);
                int res = ((al / 10) << 8) + (al % 10);
                checkZeroFlag16(res);
                checkParityFlag(res);
                checkSignFlag16(res);
                setReg16(REG_AL_AX, res);
                clock.updateClock(83);
                if (debug) {
                    decoderString += "AAM";
                }
            }
            break;
            case AAD: {
                int ah = getReg8(REG_AH_SP);
                int al = getReg8(REG_AL_AX);
                int res = (ah * 10 + al) & 0xFF;
                checkZeroFlag16(res);
                checkParityFlag(res);
                checkSignFlag16(res);
                setReg16(REG_AL_AX, res);
                clock.updateClock(60);
                if (debug) {
                    decoderString += "AAD";
                }
            }
            break;
            case CBW: {
                if ((getReg8(REG_AL_AX) & 0x80) == 0x80) {
                    setReg8(REG_AH_SP, 0xFF);
                } else {
                    setReg8(REG_AH_SP, 0x00);
                }
                clock.updateClock(2);
                if (debug) {
                    decoderString += "CBW";
                }
            }
            break;
            case CWD: {
                if ((getReg16(REG_AL_AX) & 0x8000) == 0x8000) {
                    setReg16(REG_DL_DX, 0xFFFF);
                } else {
                    setReg16(REG_DL_DX, 0x0000);
                }
                clock.updateClock(5);
                if (debug) {
                    decoderString += "CWD";
                }
            }
            break;


            /**
             * Logische Operationen
             */
            case OR_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "OR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case OR_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "OR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case OR_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "OR " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case OR_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "OR " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case OR_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "OR AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case OR_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 | op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "OR AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case AND_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "AND " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case AND_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "AND " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case AND_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "AND " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case AND_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "AND " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case AND_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "AND AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case AND_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "AND AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case XOR_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XOR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case XOR_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XOR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case XOR_MODRM_REG_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(opcode2 & TEST_REG, res & 0xFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XOR " + getReg8String(opcode2 & TEST_REG) + "," + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case XOR_MODRM_REG_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(opcode2 & TEST_REG, res & 0xFFFF);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "XOR " + getReg16String(opcode2 & TEST_REG) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
            }
            break;
            case XOR_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg8(REG_AL_AX, res & 0xFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "XOR AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case XOR_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 ^ op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                setReg16(REG_AL_AX, res & 0xFFFF);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "XOR AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;
            case TEST_REG_MODRM_8: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg8(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "TEST " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg8String(opcode2 & TEST_REG);
                }
            }
            break;
            case TEST_REG_MODRM_16: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                int op2 = getReg16(opcode2 & TEST_REG);
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                if (debug) {
                    decoderString += "TEST " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + getReg16String(opcode2 & TEST_REG);
                }
            }
            break;
            case TEST_IMM_AL: {
                int op1 = getReg8(REG_AL_AX);
                int op2 = memory.readByte(cs * 16 + (ip++));
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag8(res);
                checkSignFlag8(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "TEST AL," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case TEST_IMM_AX: {
                int op1 = getReg16(REG_AL_AX);
                int op2 = memory.readWord(cs * 16 + (ip++));
                ip++;
                int res = op1 & op2;
                checkParityFlag(res);
                checkZeroFlag16(res);
                checkSignFlag16(res);
                clearFlag(CARRY_FLAG);
                clearFlag(OVERFLOW_FLAG);
                clock.updateClock(4);
                if (debug) {
                    decoderString += "TEST AX," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                }
            }
            break;

            /*
             * Übergabe der Steuerung
             */
            case JMP_NEAR_LABEL: {
                int increment = (short) memory.readWord(cs * 16 + (ip++));
                ip++;
                ip += increment;
                clock.updateClock(15);
                if (debug) {
                    decoderString += "JMP " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JMP_FAR_LABEL: {
                int increment = memory.readWord(cs * 16 + (ip++)) & 0xFFFF;
                ip++;
                int codesegment = memory.readWord(cs * 16 + (ip++)) & 0xFFFF;
                ip++;
                cs = codesegment;
                ip = increment;
                clock.updateClock(15);
                if (debug) {
                    decoderString += "JMP " + Integer.toHexString(codesegment) + ":" + Integer.toHexString(increment);
                }
            }
            break;
            case JMP_SHORT_LABEL: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                ip += increment;
                clock.updateClock(15);
                if (debug) {
                    decoderString += "JMP " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JO: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JO " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNO: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JNO " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (!getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JB_JNAE_JC: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JC " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(CARRY_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNB_JAE_JNC: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JNC " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (!getFlag(CARRY_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JE_JZ: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JZ " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNE_JNZ: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JNZ " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (!getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JBE_JNA: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JNA " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(CARRY_FLAG) || getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNBE_JA: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JA " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (!getFlag(CARRY_FLAG) && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JS: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JS " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(SIGN_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNS: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JNS " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (!getFlag(SIGN_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JP_JPE: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JP " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment) + ")";
                }
                if (getFlag(PARITY_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case JNP_JPO: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (!getFlag(PARITY_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
                if (debug) {
                    decoderString += "JNP " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JL_JNGE: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (getFlag(SIGN_FLAG) != getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
                if (debug) {
                    decoderString += "JL " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JNL_JGE: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (getFlag(SIGN_FLAG) == getFlag(OVERFLOW_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
                if (debug) {
                    decoderString += "JNL " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JLE_JNG: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (getFlag(SIGN_FLAG) != getFlag(OVERFLOW_FLAG) || getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
                if (debug) {
                    decoderString += "JNG " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case JNLE_JG: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "JG " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip+increment) + ")";
                }
                if ((getFlag(SIGN_FLAG) == getFlag(OVERFLOW_FLAG)) && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(16);
                } else {
                    clock.updateClock(4);
                }
            }
            break;
            case CALL_FAR_PROC: {
                if (debug) {
                    decoderString += "CALL " + Integer.toHexString(memory.readWord(cs * 16 + ip + 2)) + ":" + Integer.toHexString(memory.readWord(cs * 16 + ip));
                }
                int increment = memory.readWord(cs * 16 + (ip++));
                ip++;
                int codesegment = memory.readWord(cs * 16 + (ip++));
                ip++;
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) ip);
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) cs);
                cs = codesegment;
                ip = increment;
                clock.updateClock(28);
            }
            break;
            case CALL_NEAR_PROC: {
                int increment = (short) memory.readWord(cs * 16 + (ip++));
                ip++;
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                memory.writeWord(getReg16(REG_AH_SP), (short) ip);
                ip += increment;
                clock.updateClock(19);
                if (debug) {
                    decoderString += "CALL " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case RET_IN_SEG_IMM: {
                if (debug) {
                    decoderString += "RET " + memory.readWord(cs * 16 + ip);
                }
                int data = memory.readWord(cs * 16 + (ip++));
                ip++;
                ip = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2 + data) & 0xFFFF);
                clock.updateClock(20);
            }
            break;
            case RET_IN_SEG: {
                if (debug) {
                    decoderString += "RET";
                }
                ip = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(16);
            }
            break;
            case RET_INTER_SEG_IMM: {
                if (debug) {
                    decoderString += "RET " + memory.readWord(cs * 16 + ip);
                }
                int data = memory.readWord(cs * 16 + (ip++));
                ip++;
                cs = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                ip = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2 + data) & 0xFFFF);
                clock.updateClock(25);
            }
            break;
            case RET_INTER_SEG: {
                if (debug) {
                    decoderString += "RET";
                }
                cs = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                ip = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(26);
            }
            break;
            case LOOPNE_LOOPNZ: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "LOOPNE " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment - 1) + ")";
                }
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                if (getReg16(REG_CL_CX) != 0 && !getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(19);
                } else {
                    clock.updateClock(5);
                }
            }
            break;
            case LOOPE_LOOPZ: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (debug) {
                    decoderString += "LOOPE " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment - 1) + ")";
                }
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                if (getReg16(REG_CL_CX) != 0 && getFlag(ZERO_FLAG)) {
                    ip += increment;
                    clock.updateClock(18);
                } else {
                    clock.updateClock(6);
                }
            }
            break;
            case LOOP: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                int oldreg = getReg16(REG_CL_CX) + 1;
                if (debug) {
                    decoderString += "LOOP " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip + increment - 1) + ")";

                }
                // Hack to Abort -2 LOOPS
                if (increment == -2) {
                    clock.updateClock(getReg16(REG_CL_CX) * (17 + 3) + 5);
                    setReg16(REG_CL_CX, 0);
                } else {
                    if (getReg16(REG_CL_CX) != 0) {
                        ip += increment;
                        clock.updateClock(17);
                    } else {
                        clock.updateClock(5);
                    }
                }
            }
            break;
            case JCXZ: {
                int increment = (byte) memory.readByte(cs * 16 + (ip++));
                if (getReg16(REG_CL_CX) == 0) {
                    ip += increment;
                    clock.updateClock(18);
                } else {
                    clock.updateClock(6);
                }
                if (debug) {
                    decoderString += "JCXZ " + increment + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(ip) + ")";
                }
            }
            break;
            case INT_IMM: {
                int data = memory.readByte(cs * 16 + (ip++));
                clock.updateClock(51);
                interruptSystem.addInterrupt(data);
                if (debug) {
                    decoderString += "INT " + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                }
            }
            break;
            case INT3: {
                clock.updateClock(52);
                interruptSystem.addInterrupt(3);
                if (debug) {
                    decoderString += "INT 3";
                }
            }
            break;
            case INTO: {
                if (getFlag(OVERFLOW_FLAG)) {
                    interruptSystem.addInterrupt(4);
                    clock.updateClock(53);
                } else {
                    clock.updateClock(4);
                }
                if (debug) {
                    decoderString += "INTO";
                }
            }
            break;
            case IRET: {
                ip = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                cs = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                flags = memory.readWord(getReg16(REG_AH_SP));
                setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                clock.updateClock(32);
                if (debug) {
                    decoderString += "IRET";
                }
            }
            break;

            /*
             * Zeichkettenverarbeitung
             */
            case MOVS_8: {
                if (string_prefix == NO_PREFIX) {
                    memory.writeByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    clock.updateClock(18);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(17);
                        memory.writeByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                        }

                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
//                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
//                            break;
//                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    decoderString += "MOVS";
                }
            }
            break;
            case MOVS_16: {
                if (string_prefix == NO_PREFIX) {
                    memory.writeWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    clock.updateClock(18);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(17);
                        memory.writeWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                            setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
//                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
//                            break;
//                        }
                    }

                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    decoderString += "MOVSW";
                }
            }
            break;
            case CMPS_8: {
                if (string_prefix == NO_PREFIX) {
                    int op1 = memory.readByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI));
                    int op2 = memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                    int res = op1 - op2;
                    checkCarryFlagSub8(res);
                    checkZeroFlag8(res);
                    checkSignFlag8(res);
                    checkOverflowFlagSub8(op1, op2, res);
                    checkParityFlag(res);
                    checkAuxiliaryCarryFlagSub(op1, op2);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    clock.updateClock(18);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(17);
                        int op1 = memory.readByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI));
                        int op2 = memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
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
                    decoderString += "CMPS";
                }
            }
            break;
            case CMPS_16: {
                if (string_prefix == NO_PREFIX) {
                    int op1 = memory.readWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI));
                    int op2 = memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                    int res = op1 - op2;
                    checkCarryFlagSub16(res);
                    checkZeroFlag16(res);
                    checkSignFlag16(res);
                    checkOverflowFlagSub16(op1, op2, res);
                    checkParityFlag(res);
                    checkAuxiliaryCarryFlagSub(op1, op2);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    clock.updateClock(18);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(17);
                        int op1 = memory.readWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI));
                        int op2 = memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
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
                    decoderString += "CMPSW";
                }
            }
            break;
            case STOS_8: {
                if (string_prefix == NO_PREFIX) {
                    memory.writeByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), (byte) getReg8(REG_AL_AX));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                    }
                    clock.updateClock(11);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(10);
                        memory.writeByte(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), (byte) getReg8(REG_AL_AX));
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
//                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
//                            break;
//                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    decoderString += "STOS";
                }
            }
            break;
            case STOS_16: {
                if (string_prefix == NO_PREFIX) {
                    memory.writeWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), (short) getReg16(REG_AL_AX));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                    }
                    clock.updateClock(11);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(10);
                        memory.writeWord(getSReg(SREG_ES) * 16 + getReg16(REG_BH_DI), (short) getReg16(REG_AL_AX));
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
                    decoderString += "STOSW";
                }
            }
            break;
            case LODS_8: {
                if (string_prefix == NO_PREFIX) {
                    setReg8(REG_AL_AX, memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 1);
                    } else {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 1);
                    }
                    clock.updateClock(12);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(11);
                        setReg8(REG_AL_AX, memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
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
                    decoderString += "LODS";
                }
            }
            break;
            case LODS_16: {
                if (string_prefix == NO_PREFIX) {
                    setReg16(REG_AL_AX, memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) - 2);
                    } else {
                        setReg16(REG_DH_SI, getReg16(REG_DH_SI) + 2);
                    }
                    clock.updateClock(12);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(11);
                        setReg16(REG_AL_AX, memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI)));
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
                    decoderString += "LODSW";
                }
            }
            break;
            case SCAS_8: {
                if (string_prefix == NO_PREFIX) {
                    int op1 = getReg8(REG_AL_AX);
                    int op2 = memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                    int res = op1 - op2;
                    checkCarryFlagSub8(res);
                    checkZeroFlag8(res);
                    checkSignFlag8(res);
                    checkOverflowFlagSub8(op1, op2, res);
                    checkParityFlag(res);
                    checkAuxiliaryCarryFlagSub(op1, op2);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                    }
                    clock.updateClock(15);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(15);
                        int op1 = getReg8(REG_AL_AX);
                        int op2 = memory.readByte(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        if (getFlag(DIRECTION_FLAG)) {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 1);
                        } else {
                            setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 1);
                        }
                        setReg16(REG_CL_CX, getReg16(REG_CL_CX) - 1);
                        if ((string_prefix == REP_REPE_REPZ && !getFlag(ZERO_FLAG)) || (string_prefix == REPNE_REPNZ && getFlag(ZERO_FLAG))) {
                            break;
                        }
                    }
                    string_prefix = NO_PREFIX;
                }
                if (debug) {
                    decoderString += "SCAS";
                }
            }
            break;
            case SCAS_16: {
                if (string_prefix == NO_PREFIX) {
                    int op1 = getReg16(REG_AL_AX);
                    int op2 = memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                    int res = op1 - op2;
                    checkCarryFlagSub16(res);
                    checkZeroFlag16(res);
                    checkSignFlag16(res);
                    checkOverflowFlagSub16(op1, op2, res);
                    checkParityFlag(res);
                    checkAuxiliaryCarryFlagSub(op1, op2);
                    if (getFlag(DIRECTION_FLAG)) {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) - 2);
                    } else {
                        setReg16(REG_BH_DI, getReg16(REG_BH_DI) + 2);
                    }
                    clock.updateClock(15);
                } else {
                    clock.updateClock(9);
                    while (getReg16(REG_CL_CX) != 0) {
                        clock.updateClock(15);
                        int op1 = getReg16(REG_AL_AX);
                        int op2 = memory.readWord(getSReg(SREG_DS) * 16 + getReg16(REG_DH_SI));
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
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
                    decoderString += "SCASW";
                }
            }
            break;
            case REPNE_REPNZ: {
                string_prefix = REPNE_REPNZ;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "REPNE";
                }
            }
            break;
            case REP_REPE_REPZ: {
                string_prefix = REP_REPE_REPZ;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "REP";
                }
            }
            break;


            /*
             * Prefix
             */
            case PREFIX_ES:
                decoderString = null;
                prefix = PREFIX_ES;
                clock.updateClock(2);
                break;
            case PREFIX_CS:
                decoderString = null;
                prefix = PREFIX_CS;
                clock.updateClock(2);
                break;
            case PREFIX_SS:
                decoderString = null;
                prefix = PREFIX_SS;
                clock.updateClock(2);
                break;
            case PREFIX_DS:
                decoderString = null;
                prefix = PREFIX_DS;
                clock.updateClock(2);
                break;

            /**
             * Prozessorsteuerung
             */
            case NOP:
                clock.updateClock(3);
                if (debug) {
                    decoderString += "NOP";
                }
                break;
            case CMC:
                flags ^= CARRY_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "CMC";
                }
                break;
            case CLC:
                flags &= ~CARRY_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "CLC";
                }
                break;
            case STC:
                flags |= CARRY_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "STC";
                }
                break;
            case CLI:
                flags &= ~INTERRUPT_ENABLE_FLAG;
                clock.updateClock(2);
                interruptSystem.disableInterrupts();
                if (debug) {
                    decoderString += "CLI";
                }
                break;
            case STI:
                interruptSystem.enableInterrupts();
                flags |= INTERRUPT_ENABLE_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "STI";
                }
                break;
            case CLD:
                flags &= ~DIRECTION_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "CLD";
                }
                break;
            case STD:
                flags |= DIRECTION_FLAG;
                clock.updateClock(2);
                if (debug) {
                    decoderString += "STD";
                }
                break;
            case WAIT: {
                clock.updateClock(3);
                if (debug) {
                    decoderString += "WAIT";

                }
                System.out.println("Befehl WAIT noch nicht implementiert");
                memory.dump();
                System.exit(0);
            }
            break;
            case LOCK: {
                clock.updateClock(2);
                if (debug) {
                    decoderString += "LOCK";
                }
                System.out.println(decoderString);
                System.out.println("Befehl LOCK noch nicht implementiert");
                memory.dump();
                System.exit(0);
            }
            break;
            case HLT: {
                clock.updateClock(2);
                if (debug) {
                    decoderString += "HLT";
                }
                isHalted = true;
//                System.out.println("--- HALT ---");
//                SystemMemory.getInstance().dump();
//                System.exit(0);
            }
            break;
            case ESC0: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC0 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
//                System.out.println("Befehl ESC0 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC1: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC1 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC1 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC2: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC2 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC2 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC3: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC3 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC3 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC4: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC4 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC4 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC5: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC5 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC5 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC6: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC6 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC6 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            case ESC7: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                if (debug) {
                    decoderString += "ESC7 " + (opcode2 >> 3) + "," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                }
                System.out.println("Befehl ESC7 noch nicht implementiert");
//                memory.dump();
//                System.exit(0);
            }
            break;
            /*
             * Zweier
             */
            case 0x80: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _80_ADD_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 + op2;
                        checkCarryFlagAdd8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagAdd8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADD " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_OR_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 | op2;
                        checkParityFlag(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "OR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_ADC_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 + op2;
                        checkCarryFlagAdd8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagAdd8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADC " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_SBB_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SBB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_AND_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 & op2;
                        checkParityFlag(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "AND " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_SUB_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SUB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_XOR_IMM_REG_8_NOSIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 ^ op2;
                        checkParityFlag(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "XOR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _80_CMP_IMM_REG_8_NOSIGN: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "CMP " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h (" + op1 + "," + op2 + ")";
                        }
                    }
                    break;
                }
            }
            break;
            case 0x81: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _81_ADD_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 + op2;
                        checkCarryFlagAdd16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagAdd16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADD " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_OR_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 | op2;
                        checkParityFlag(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "OR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_ADC_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 + op2;
                        checkCarryFlagAdd16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagAdd16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADC " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_SBB_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SBB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_AND_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 & op2;
                        checkParityFlag(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "AND " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_SUB_IMM_REG_16_NOSIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SUB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_XOR_IMM_REG_16_NOSIGN: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = op1 ^ op2;
                        checkParityFlag(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "XOR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _81_CMP_IMM_REG_16_NOSIGN: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "CMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h (" + Integer.toHexString(op1) + ")";
                        }
                    }
                    break;
                }
            }
            break;
            case 0x82: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _82_ADD_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 + op2;
                        checkCarryFlagAdd8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagAdd8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADD " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _82_ADC_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 + op2;
                        checkCarryFlagAdd8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagAdd8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADC " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _82_SBB_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SBB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _82_SUB_IMM_REG_8_SIGN: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        ip++;
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SUB " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _82_CMP_IMM_REG_8_SIGN: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 - op2;
                        checkCarryFlagSub8(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "CMP " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                }
            }
            break;
            case 0x83: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _83_ADD_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 + op2;
                        checkCarryFlagAdd16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagAdd16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADD " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(op2) + "h";
                        }
                    }
                    break;
                    case _83_ADC_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 + op2;
                        checkCarryFlagAdd16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagAdd16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ADC " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(op2) + "h";
                        }
                    }
                    break;
                    case _83_SBB_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        if (getFlag(CARRY_FLAG)) {
                            op2++;
                        }
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SBB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(op2) + "h";
                        }
                    }
                    break;
                    case _83_SUB_IMM_REG_16_SIGN: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SUB " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(op2) + "h";
                        }
                    }
                    break;
                    case _83_CMP_IMM_REG_16_SIGN: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 - op2;
                        checkCarryFlagSub16(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, op2, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, op2);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "CMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(op2) + "h";
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8C: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8C_MOV_ES_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_ES), true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",ES";
                        }
                    }
                    break;
                    case _8C_MOV_CS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_CS), true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CS";
                        }
                    }
                    break;
                    case _8C_MOV_SS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_SS), true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",SS";
                        }
                    }
                    break;
                    case _8C_MOV_DS_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, getSReg(SREG_DS), true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 : 9 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",DS";
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8E: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8E_MOV_MODRM_ES_16: {
                        setSReg(SREG_ES, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV ES," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_CS_16: {
                        setSReg(SREG_CS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV CS," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_SS_16: {
                        setSReg(SREG_SS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV SS," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _8E_MOV_MODRM_DS_16: {
                        setSReg(SREG_DS, getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true));
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 8 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV DS," + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            case 0x8F: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _8F_POP_MODRM_16: {
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, memory.readWord(getReg16(REG_AH_SP)), true);
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) + 2) & 0xFFFF);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 8 : 17 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "POP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xC6: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _C6_MOV_IMM_MEM_8: {
                        int data8 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, data8 & 0xFF, true);
                        ip++;
                        clock.updateClock(10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(data8) + "h";
                        }
                    }
                    break;
                }
            }
            break;
            case 0xC7: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _C7_MOV_IMM_MEM_16: {
                        int data16 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, data16 & 0xFFFF, true);
                        ip = ip + 2;
                        clock.updateClock(10 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MOV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + data16;
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD0: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _D0_ROL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 << 1) & 0xFF) & ((op1 & 0x80) >> 7);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ROL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_ROR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 >> 1) & 0xFF) & ((op1 & 0x01) << 7);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 6) != getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ROR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_RCL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x01;
                        }
                        if (getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "RCL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_RCR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x80;
                        }
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 6) != getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "RCR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_SAL_SHL_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFF);
                        if (getBit(op1, 7)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 7)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SHL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_SHR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 7) != getBit(res, 6)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SHR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D0_SAR_MODRM_1_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFF);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        if (getBit(op1, 7)) {
                            res |= 0x80;
                        } else {
                            res &= 0x7F;
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SAR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD1: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _D1_ROL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 << 1) & 0xFFFF) & ((op1 & 0x8000) >> 15);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ROL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D1_ROR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        int res = ((op1 >> 1) & 0xFFFF) & ((op1 & 0x0001) << 15);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 15) != getBit(res, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "ROR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D1_RCL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFFFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x01;
                        }
                        if (getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "RCL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D1_RCR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (getFlag(CARRY_FLAG)) {
                            res |= 0x8000;
                        }
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 15) != getBit(res, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "RCR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1 ("+Integer.toBinaryString(op1)+"->"+Integer.toBinaryString(res)+")";
                        }
                    }
                    break;
                    case _D1_SAL_SHL_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 << 1) & 0xFFFF);
                        if (getBit(op1, 15)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getFlag(CARRY_FLAG) != getBit(res, 15)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag16(res);
                        checkZeroFlag16(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SHL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D1_SHR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        if (getBit(res, 15) != getBit(res, 14)) {
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(OVERFLOW_FLAG);
                        }
                        checkSignFlag16(res);
                        checkZeroFlag16(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SHR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                    case _D1_SAR_MODRM_1_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = ((op1 >> 1) & 0xFFFF);
                        if (getBit(op1, 0)) {
                            setFlag(CARRY_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                        }
                        if (getBit(op1, 15)) {
                            res |= 0x8000;
                        } else {
                            res &= 0x7FFF;
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res, true);
                        clearFlag(OVERFLOW_FLAG);
                        checkSignFlag8(res);
                        checkZeroFlag8(res);
                        checkParityFlag(res);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "SAR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",1";
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD2: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int count = getReg8(REG_CL_CX);
                switch (opcode2 & TEST_REG) {
                    case _D2_ROL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 7)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFF) & ((op1 & 0x80) >> 7);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "ROL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_ROR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFF) & ((op1 & 0x01) << 7);
                        }
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "ROR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_RCL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (getBit(op1, 7)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "RCL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_RCR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "RCR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_SAL_SHL_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 7)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SAL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_SHR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SHR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D2_SAR_MODRM_CL_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        boolean signFlag = getBit(op1, 7);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SAR " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                }
            }
            break;
            case 0xD3: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                int count = getReg8(REG_CL_CX);
                switch (opcode2 & TEST_REG) {
                    case _D3_ROL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 15)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 << 1) & 0xFFFF) & ((op1 & 0x8000) >> 15);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "ROL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_ROR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
                                setFlag(CARRY_FLAG);
                            } else {
                                clearFlag(CARRY_FLAG);
                            }
                            op1 = ((op1 >> 1) & 0xFFFF) & ((op1 & 0x0001) << 15);
                        }
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, op1, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "ROR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_RCL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (getBit(op1, 15)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "RCL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_RCR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            boolean lastCarry = getFlag(CARRY_FLAG);
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "RCR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_SAL_SHL_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 15)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SHL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_SHR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SHR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                    case _D3_SAR_MODRM_CL_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        boolean signFlag = getBit(op1, 15);
                        for (int i = 0; i < count; i++) {
                            if (getBit(op1, 0)) {
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 4 * count : 20 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM) + 4 * count);
                        if (debug) {
                            decoderString += "SAR " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + ",CL";
                        }
                    }
                    break;
                }
            }
            break;
            case 0xF6: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _F6_TEST_IMM_MODRM_8: {
                        int op2 = getImmediate8(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 & op2;
                        checkParityFlag(res);
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        ip++;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 5 : 11 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "TEST " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 1) + "," + Integer.toHexString(memory.readByte(cs * 16 + ip - 1)) + "h";
                        }
                    }
                    break;
                    case _F6_NOT_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, (~op1) & 0xFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "NOT " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F6_NEG_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "NEG " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F6_MUL_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg8(REG_AL_AX);
                        if (op1 * op2 > 0xFF) {
                            setFlag(CARRY_FLAG);
                            setFlag(OVERFLOW_FLAG);
                        } else {
                            clearFlag(CARRY_FLAG);
                            clearFlag(OVERFLOW_FLAG);
                        }
                        setReg16(REG_AL_AX, (op2 * op1) & 0xFFFF);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 77 : 83 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MUL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 98 : 104 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "IMUL " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F6_DIV_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0xFF)) {
                            // TODO: INT0
                        } else {
                            setReg8(REG_AL_AX, op2 / op1);
                            setReg8(REG_AH_SP, op2 % op1);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 90 : 96 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "DIV " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F6_IDIV_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0x7F)) {
                            // TODO: INT0
                        } else {
                            setReg8(REG_AL_AX, (short) op2 / (byte) op1);
                            setReg8(REG_AH_SP, (short) op2 % (byte) op1);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 112 : 118 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "IDIV " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xF7: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _F7_TEST_IMM_MODRM_16: {
                        int op2 = getImmediate16(opcode2 & TEST_MOD, opcode2 & TEST_RM);
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int res = op1 & op2;
                        checkParityFlag(res);
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        clearFlag(CARRY_FLAG);
                        clearFlag(OVERFLOW_FLAG);
                        ip = ip + 2;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 5 : 11 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "TEST " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 2) + "," + Integer.toHexString(memory.readWord(cs * 16 + ip - 2)) + "h";
                        }
                    }
                    break;
                    case _F7_NOT_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, (~op1) & 0xFFFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "NOT " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F7_NEG_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "NEG " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 118 : 139 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "MUL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
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
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 154 : 160 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "IMUL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _F7_DIV_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = (getReg16(REG_DL_DX) << 16) | getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0xFFFF)) {
                            // TODO: INT0
                        } else {
                            setReg16(REG_AL_AX, op2 / op1);
                            setReg16(REG_DL_DX, op2 % op1);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 162 : 172 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "DIV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + "(" + Integer.toHexString(op2) + "/" + Integer.toHexString(op1) + ")";
                        }
                    }
                    break;
                    case _F7_IDIV_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        int op2 = (getReg16(REG_DL_DX) << 16) | getReg16(REG_AL_AX);
                        if (op1 == 0 || ((op2 / op1) > 0x7FFF) || ((op2 / op1) < 0x8001)) {
                            // TODO: INT0
                        } else {
                            setReg16(REG_AL_AX, (int) op2 / (short) op1);
                            setReg16(REG_DL_DX, (int) op2 % (short) op1);
                        }
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 184 : 190 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "IDIV " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xFE: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _FE_INC_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = op1 + 1;
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagAdd8(op1, 1, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, 1);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "INC " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _FE_DEC_MODRM_8: {
                        int op1 = getMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = op1 - 1;
                        checkZeroFlag8(res);
                        checkSignFlag8(res);
                        checkOverflowFlagSub8(op1, 1, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, 1);
                        setMODRM8(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "DEC " + getMODRM8String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            case 0xFF: {
                int opcode2 = memory.readByte(cs * 16 + (ip++));
                switch (opcode2 & TEST_REG) {
                    case _FF_INC_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = op1 + 1;
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagAdd16(op1, 1, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagAdd(op1, 1);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 2 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "INC " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _FF_DEC_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, false);
                        int res = op1 - 1;
                        checkZeroFlag16(res);
                        checkSignFlag16(res);
                        checkOverflowFlagSub16(op1, 1, res);
                        checkParityFlag(res);
                        checkAuxiliaryCarryFlagSub(op1, 1);
                        setMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, res & 0xFFFF, true);
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 3 : 15 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "DEC " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                    case _FF_CALL_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            decoderString += "CALL " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(op1) + ")";
                        }
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        memory.writeWord(getReg16(REG_AH_SP), (short) ip);
                        ip = op1;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 16 : 21 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_CALL_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            decoderString += "CALL " + Integer.toHexString(memory.readWord(op1 + 2)) + ":" + memory.readWord(op1);
                        }
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        memory.writeWord(getReg16(REG_AH_SP), (short) ip);
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        memory.writeWord(getReg16(REG_AH_SP), (short) cs);
                        int increment = memory.readWord(op1);
                        int codesegment = memory.readWord(op1 + 2);
                        cs = codesegment;
                        ip = increment;
                        clock.updateClock(37 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_JMP_MODRM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        System.out.println("_FF_JMP_MODRM_16");
                        System.out.println(getRegisterString());
                        System.out.println(Integer.toBinaryString(opcode1) + " " + Integer.toBinaryString(opcode2) + " -- " + op1);
                        System.out.println("JMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0));
                        memory.dump();
                        System.exit(0);
                        if (debug) {
                            decoderString += "JMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + " (" + Integer.toHexString(cs) + ":" + Integer.toHexString(op1) + ")";
                        }
                        ip = op1;
                        clock.updateClock(((opcode2 & TEST_MOD) == MOD_REG) ? 11 : 18 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));

                    }
                    break;
                    case _FF_JMP_MEM_16: {
                        int addr = getAddressMODRM(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        if (debug) {
                            decoderString += "JMP " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0) + " (" + Integer.toHexString(memory.readWord(addr)) + ":" + Integer.toHexString(memory.readWord(addr + 2)) + ")";
                        }
                        int increment = memory.readWord(addr + 2);
                        int codesegment = memory.readWord(addr);
                        cs = codesegment;
                        ip = increment;
                        clock.updateClock(24 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                    }
                    break;
                    case _FF_PUSH_MEM_16: {
                        int op1 = getMODRM16(opcode2 & TEST_MOD, opcode2 & TEST_RM, true);
                        setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
                        memory.writeWord(getReg16(REG_AH_SP), (short) op1);
                        clock.updateClock(16 + getOpcodeCyclesEA(opcode2 & TEST_MOD, opcode2 & TEST_RM));
                        if (debug) {
                            decoderString += "PUSH " + getMODRM16String(opcode2 & TEST_MOD, opcode2 & TEST_RM, 0);
                        }
                    }
                    break;
                }
            }
            break;
            default:
                System.out.println("Nicht implementierter oder ungültiger OPCode " + Integer.toHexString(opcode1).toString() + " !");
                memory.dump();
                decoder.save();
                System.exit(0);
                break;
        }

        if (debug) {
            if (decoderString != null) {
                decoder.addItem(decoderAddress, decoderString);
                debugFile.println(decoderString);
                debugFile.flush();
            }
            //debugFile.println(getRegisterString());
        } else {
            if (cs * 16 + ip == debugstart) {
                try {
                    debugFile = new PrintStream(new FileOutputStream("K1810WM86.log"));
                    debug = true;
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(K1810WM86.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (prefix != NO_PREFIX && opcode1 != PREFIX_CS && opcode1 != PREFIX_SS && opcode1 != PREFIX_DS && opcode1 != PREFIX_ES) {
            prefix = NO_PREFIX;
        }
    }

    private int getImmediate8(int MOD, int RM) {
        switch (MOD) {
            case MOD_REG:
                return memory.readByte(cs * 16 + ip);
            case MOD_MEM_NO_DISPL:
                if (RM == RM_DIRECT_BP) {
                    return memory.readByte(cs * 16 + ip + 2);
                } else {
                    return memory.readByte(cs * 16 + ip);
                }
            case MOD_MEM_8_DISPL:
                return memory.readByte(cs * 16 + ip + 1);
            case MOD_MEM_16_DISPL:
                return memory.readByte(cs * 16 + ip + 2);
            default:
                throw new IllegalArgumentException("Illegal MOD");
        }
    }

    private int getImmediate16(int MOD, int RM) {
        switch (MOD) {
            case MOD_REG:
                return memory.readWord(cs * 16 + ip);
            case MOD_MEM_NO_DISPL:
                if (RM == RM_DIRECT_BP) {
                    return memory.readWord(cs * 16 + ip + 2);
                } else {
                    return memory.readByte(cs * 16 + ip);
                }
            case MOD_MEM_8_DISPL:
                return memory.readWord(cs * 16 + ip + 1);
            case MOD_MEM_16_DISPL:
                return memory.readWord(cs * 16 + ip + 2);
            default:
                throw new IllegalArgumentException("Illegal MOD");
        }
    }

    private int getSegment(int default_segment) {
        switch (prefix) {
            case NO_PREFIX:
                return default_segment;
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

    private String getSegmentString(String default_segment) {
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

    public String getRegisterString() {
        String result = "";

        result += "AX:" + Integer.toHexString(ax) + " ";
        result += "BX:" + Integer.toHexString(bx) + " ";
        result += "CX:" + Integer.toHexString(cx) + " ";
        result += "DX:" + Integer.toHexString(dx) + "\n";
        result += "SP:" + Integer.toHexString(sp) + " ";
        result += "BP:" + Integer.toHexString(bp) + " ";
        result += "SI:" + Integer.toHexString(si) + " ";
        result += "DI:" + Integer.toHexString(di) + "\n";
        result += "CS:" + Integer.toHexString(cs) + " ";
        result += "DS:" + Integer.toHexString(ds) + " ";
        result += "SS:" + Integer.toHexString(ss) + " ";
        result += "ES:" + Integer.toHexString(es) + "\n";
        result += "IP:" + Integer.toHexString(ip) + " ";
        result += "Flags:" + Integer.toBinaryString(flags) + "\n";

        return result;
    }

    public void printFlags() {
        System.out.println("CF:" + ((flags & CARRY_FLAG) != 0));
        System.out.println("OF:" + ((flags & OVERFLOW_FLAG) != 0));
        System.out.println("PF:" + ((flags & PARITY_FLAG) != 0));
        System.out.println("ZF:" + ((flags & ZERO_FLAG) != 0));
        System.out.println("SF:" + ((flags & SIGN_FLAG) != 0));
    }

    private String getOffsetString(int MOD, int RM, int ipOffset) {
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
                    offset = Integer.toHexString(memory.readWord(cs * 16 + ip - 2 - ipOffset)) + "h";
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
                int displ = (byte) (memory.readByte(cs * 16 + ip - 1 - ipOffset) & 0xFF);
                offset += ((displ > 0) ? "+" : "") + displ;
                break;
            case MOD_MEM_16_DISPL:
                offset += "+" + Integer.toHexString(memory.readWord(cs * 16 + ip - 2 - ipOffset)) + "h";
                break;
        }
        return offset;
    }

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
                    offset = memory.readWord(cs * 16 + (modip++));
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
                offset += (byte) (memory.readByte(cs * 16 + (modip++)) & 0xFF);
                break;
            case MOD_MEM_16_DISPL:
                offset += memory.readWord(cs * 16 + (modip++));
                modip++;
                break;
        }
        if (updateIP) {
            ip = modip;
        }

        return offset;
    }

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

    private String getReg16String(int reg) {
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

    private String getSegmentAddressString(int MOD, int RM) {
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

    private int getMODRM8(int MOD, int RM, boolean updateIP) {
        if (MOD == MOD_REG) {
            return getReg8(RM << 3);
        } else {
            return memory.readByte(getAddressMODRM(MOD, RM, updateIP));
        }
    }

    private int getMODRM16(int MOD, int RM, boolean updateIP) {
        if (MOD == MOD_REG) {
            return getReg16(RM << 3);
        } else {
            return memory.readWord(getAddressMODRM(MOD, RM, updateIP));
        }
    }

    private String getMODRM8String(int MOD, int RM, int ipOffset) {
        if (MOD == MOD_REG) {
            return getReg8String(RM << 3);
        } else {
            return getAddressMODRMString(MOD, RM, ipOffset);
        }
    }

    private String getMODRM16String(int MOD, int RM, int ipOffset) {
        if (MOD == MOD_REG) {
            return getReg16String(RM << 3);
        } else {
            return getAddressMODRMString(MOD, RM, ipOffset);
        }
    }

    private void setMODRM8(int MOD, int RM, int value, boolean updateIP) {
        if (MOD == MOD_REG) {
            setReg8(RM << 3, value);
        } else {
            memory.writeByte(getAddressMODRM(MOD, RM, updateIP), (byte) (value & 0xFF));
        }
    }

    private void setMODRM16(int MOD, int RM, int value, boolean updateIP) {
        if (MOD == MOD_REG) {
            setReg16(RM << 3, value);
        } else {
            memory.writeWord(getAddressMODRM(MOD, RM, updateIP), (short) (value & 0xFFFF));
        }
    }

    private String getAddressMODRMString(int MOD, int RM, int ipOffset) {
        String segmentAddress = getSegmentAddressString(MOD, RM);
        String offset = getOffsetString(MOD, RM, ipOffset);
        return segmentAddress + ":" + offset;
    }

    private int getAddressMODRM(int MOD, int RM, boolean updateIP) {
        int segmentAddress = getSegmentAddress(MOD, RM);
        int offset = getOffset(MOD, RM, updateIP);
        return segmentAddress + offset;
    }

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

    private void setFlag(int flag) {
        flags |= flag;
    }

    private void clearFlag(int flag) {
        flags &= ~flag;
    }

    private boolean getFlag(int flag) {
        return (flags & flag) != 0;
    }

    private void checkCarryFlagAdd8(int res) {
        if (res > 0xFF) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    private void checkCarryFlagAdd16(int res) {
        if (res > 0xFFFF) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    private void checkCarryFlagSub8(int res) {
        if (res < 0x0) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    private void checkCarryFlagSub16(int res) {
        if (res < 0x0) {
            setFlag(CARRY_FLAG);
        } else {
            clearFlag(CARRY_FLAG);
        }
    }

    private void checkZeroFlag8(int res) {
        if ((res & 0xFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    private void checkZeroFlag16(int res) {
        if ((res & 0xFFFF) == 0) {
            setFlag(ZERO_FLAG);
        } else {
            clearFlag(ZERO_FLAG);
        }
    }

    private void checkSignFlag8(int res) {
        if (res <0) {
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

    private void checkSignFlag16(int res) {
        if (res <0){
            setFlag(SIGN_FLAG);
        } else {
            clearFlag(SIGN_FLAG);
        }
    }

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

    private void checkOverflowFlagAdd8(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x80)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    private void checkOverflowFlagAdd16(int op1, int op2, int res) {
        if (((op1 & 0x8000) == 0x8000 && (op2 & 0x8000) == 0x8000 && (res & 0x8000) == 0x0000) | ((op1 & 0x8000) == 0x0000 && (op2 & 0x8000) == 0x0000 && (res & 0x8000) == 0x8000)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    private void checkOverflowFlagSub8(int op1, int op2, int res) {
        if (((op1 & 0x80) == 0x80 && (op2 & 0x80) == 0x00 && (res & 0x80) == 0x00) | ((op1 & 0x80) == 0x00 && (op2 & 0x80) == 0x80 && (res & 0x80) == 0x80)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    private void checkOverflowFlagSub16(int op1, int op2, int res) {
        if (((op1 & 0x8000) == 0x8000 && (op2 & 0x8000) == 0x0000 && (res & 0x8000) == 0x0000) | ((op1 & 0x8000) == 0x0000 && (op2 & 0x8000) == 0x8000 && (res & 0x8000) == 0x8000)) {
            setFlag(OVERFLOW_FLAG);
        } else {
            clearFlag(OVERFLOW_FLAG);
        }
    }

    private void checkAuxiliaryCarryFlagAdd(int op1, int op2) {
        if (((op1 & 0xF) + (op2 & 0xF)) > 0xF) {
            setFlag(AUXILIARY_CARRY_FLAG);
        } else {
            clearFlag(AUXILIARY_CARRY_FLAG);
        }
    }

    private void checkAuxiliaryCarryFlagSub(int op1, int op2) {
        if ((op1 & 0xF) < (op2 & 0xF)) {
            setFlag(AUXILIARY_CARRY_FLAG);
        } else {
            clearFlag(AUXILIARY_CARRY_FLAG);
        }
    }

    public void reset() {
        flags = 0x0000;
        ip = 0x0000;
        ds = 0x0000;
        es = 0x0000;
        ss = 0x0000;
        cs = 0xFFFF;

        //Monitor Entry
        //cs = 0xF800;
        //ip = 0x0406;

        //IRQ Entry
        //cs = 0xF800;
        //ip = 0x0337;

        //Speicher Initialisierung
        //cs = 0xF800;
        //ip=0x3813;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    private void checkInterrupts() {
        int interruptID = interruptSystem.getNextInterrupt();
        if (interruptID >= 0) {
            System.out.println("Verarbeite Interrupt " + interruptID);
            setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
            memory.writeWord(getReg16(REG_AH_SP), (short) flags);
            clearFlag(INTERRUPT_ENABLE_FLAG);
            clearFlag(TRAP_FLAG);
//            interruptSystem.disableInterrupts();
            setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
            memory.writeWord(getReg16(REG_AH_SP), (short) cs);
            setReg16(REG_AH_SP, (getReg16(REG_AH_SP) - 2) & 0xFFFF);
            memory.writeWord(getReg16(REG_AH_SP), (short) ip);
            ip = memory.readWord(interruptID * 4);
            cs = memory.readWord(interruptID * 4 + 2);
            isHalted = false;
        }
    }

    public void run() {
        while (true) {
            if (!isHalted) {
                executeNextInstruction();
            } else {
                clock.updateClock(3);
            }
            if (getFlag(INTERRUPT_ENABLE_FLAG)) {
                InterruptSystem.getInstance().enableInterrupts();
            } else {
                InterruptSystem.getInstance().disableInterrupts();
            }
            checkInterrupts();
        }
    }
}
