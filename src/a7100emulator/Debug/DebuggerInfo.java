/*
 * DebuggerInfo.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2015 Dirk Bräuer
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
 *   05.04.2014 Kommentare vervollständigt
 *   31.08.2014 Singleton entfernt
 */
package a7100emulator.Debug;

/**
 * Klasse zum Bereitstellen der Debugger Informationen
 *
 * @author Dirk Bräuer
 */
public class DebuggerInfo {

    /**
     * Aktuelle Systemzeit
     */
    private long systemClock;
    /**
     * Codesegment
     */
    private int cs;
    /**
     * Instruction-Pointer
     */
    private int ip;
    /**
     * Opcode
     */
    private int opcode;
    /**
     * Debug-String des Befehls
     */
    private String code;
    /**
     * Debug-String der Operanden
     */
    private String operands;

    /**
     * Erstellt eine neue DebuggerInfo
     */
    public DebuggerInfo() {
    }

    /**
     * Liefert die Systemzeit zurück
     *
     * @return Systemzeit
     */
    long getSystemClock() {
        return systemClock;
    }

    /**
     * Setzt die Systemzeit
     *
     * @param systemClock Systemzeit
     */
    public void setSystemClock(long systemClock) {
        this.systemClock = systemClock;
    }

    /**
     * Liefert das Codesegment zurück
     *
     * @return Codesegment
     */
    int getCs() {
        return cs;
    }

    /**
     * Setzt das Codesegment
     *
     * @param cs Codsegment
     */
    public void setCs(int cs) {
        this.cs = cs;
    }

    /**
     * Gibt den Instruction-Pointer zurück
     *
     * @return Instruction-Pointer
     */
    public int getIp() {
        return ip;
    }

    /**
     * Setzt den Instruction-Pointer
     *
     * @param ip Instruction-Pointer
     */
    public void setIp(int ip) {
        this.ip = ip;
    }

    /**
     * Gibt den Debug-String des Befehls zurück
     *
     * @return Debug-String
     */
    public String getCode() {
        return code;
    }

    /**
     * Setzt den Debug-String des Befehls
     *
     * @param code Debug-String
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gibt den Debug-String der Operanden zurück
     *
     * @return Debug-String
     */
    String getOperands() {
        return operands;
    }

    /**
     * Setzt den Debug-String der Operanden
     *
     * @param operands Debug-String
     */
    public void setOperands(String operands) {
        this.operands = operands;
    }

    /**
     * Gibt den Opcode zurück
     *
     * @return Opcode
     */
    int getOpcode() {
        return opcode;
    }

    /**
     * Setzt den Opcode
     *
     * @param opcode Opcode
     */
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
}
