/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 *
 * @author Dirk
 */
public class OpcodeStatistic {

    private static OpcodeStatistic instance;
    private int[] opCodeStatistic = new int[256];
    private PrintStream opcodeFile = null;

    private OpcodeStatistic() {
    }

    public static OpcodeStatistic getInstance() {
        if (instance == null) {
            instance = new OpcodeStatistic();
        }
        return instance;
    }

    public void addStatistic(int code) {
        opCodeStatistic[code]++;
    }

    public void dump() {
        try {
            opcodeFile = new PrintStream(new FileOutputStream("opcodes.log"));
            for (int i = 0; i < 256; i++) {
                opcodeFile.println("" + opCodeStatistic[i]);
            }
            opcodeFile.flush();
            opcodeFile.close();
        } catch (Exception e) {
        }
    }
}
