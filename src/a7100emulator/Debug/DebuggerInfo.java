/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

/**
 *
 * @author Dirk
 */
public class DebuggerInfo {

    private long systemClock;
    private int cs;
    private int ip;
    private int opcode;
    private String code;
    private String operands;
    private static DebuggerInfo instance;

    private DebuggerInfo() {
    }

    public static DebuggerInfo getInstance() {
        if (instance == null) {
            instance = new DebuggerInfo();
        }
        return instance;
    }

    /**
     * @return the systemClock
     */
    public long getSystemClock() {
        return systemClock;
    }

    /**
     * @param systemClock the systemClock to set
     */
    public void setSystemClock(long systemClock) {
        this.systemClock = systemClock;
    }

    /**
     * @return the cs
     */
    public int getCs() {
        return cs;
    }

    /**
     * @param cs the cs to set
     */
    public void setCs(int cs) {
        this.cs = cs;
    }

    /**
     * @return the ip
     */
    public int getIp() {
        return ip;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(int ip) {
        this.ip = ip;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the operands
     */
    public String getOperands() {
        return operands;
    }

    /**
     * @param operands the operands to set
     */
    public void setOperands(String operands) {
        this.operands = operands;
    }

    /**
     * @return the opcode
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * @param opcode the opcode to set
     */
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
}
