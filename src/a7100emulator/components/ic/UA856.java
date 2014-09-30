/*
 * UA856.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   12.08.2014 Erstellt
 *
 */
package a7100emulator.components.ic;

/**
 * Klasse zur Realisierung des U856 SIO
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class UA856 {

    private sioChannel[] channels = new sioChannel[2];

    public UA856() {
        for (int i = 0; i < 2; i++) {
            channels[i] = new sioChannel();
        }
    }

    public int readData(int channel) {
        return channels[channel].channelData;
    }

    public int readControl(int channel) {
        // TODO
        return channels[channel].channelData;
    }

    public void writeData(int channel, int data) {
        channels[channel].channelData = data;
    }

    public void writeControl(int channel, int data) {
        channels[channel].writeControl(data);
    }

    public boolean isDiagnose() {
        return !getBit(channels[1].writeRegister[5], 1);
    }

    public boolean isLocalROM() {
        return getBit(channels[1].writeRegister[5], 7);
    }

    class sioChannel {

        private int channelData;
        private int[] writeRegister = new int[7];
        private int registerPointer = 0;

        private void writeControl(int data) {
            writeRegister[registerPointer] = data & 0xFF;
            //System.out.println("Setze Register WR" + registerPointer + ":" + Integer.toBinaryString(data));
//            if (registerPointer == 5) {
//                if (getBit(data, 1)) {
//                    System.out.println("Setze RTS");
//                } else {
//                    System.out.println("Lösche RTS");
//                }
//                if (getBit(data, 7)) {
//                    System.out.println("Setze DTR");
//                } else {
//                    System.out.println("Lösche DTR");
//                }
//            }

            if (registerPointer == 0) {
                registerPointer = data & 0x07;
            } else {
                registerPointer = 0;
            }
        }

    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op Operand
     * @param i Nummer des Bits
     * @return true - wenn das Bit gesetzt ist, false - sonst
     */
    private boolean getBit(int op, int i) {
        return (((op >> i) & 0x1) == 0x1);
    }
}
