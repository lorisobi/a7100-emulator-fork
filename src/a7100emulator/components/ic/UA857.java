/*
 * UA857.java
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
 * Klasse zur Realisierung des U857 CTC
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class UA857 {

    private final Counter[] counter = new Counter[4];
    private int vector;

    public UA857() {
        for (int i=0;i<4;i++) {
            counter[i] = new Counter();
        }
    }

    public void writeChannel(int channel, int data) {
        if (channel == 0 && !getBit(data, 0)) {
            vector = data;
        } else {
//            System.out.println("Channel " + channel);
            counter[channel].setControlWord(data);
        }
    }

    public int readChannel(int channel) {
        return counter[channel].readValue();
    }

    class Counter {

        private int controlWord;
        private int timeConstant;
        private int value;

        private boolean timeConstantFollowing = false;
        
        public Counter() {
            
        }

        public void setControlWord(int data) {
            if (timeConstantFollowing) {
                timeConstant = data;
                timeConstantFollowing = false;
            } else {
                controlWord = data;
                if (getBit(controlWord, 2)) {
                    timeConstantFollowing = true;
                }
            }
        }

        public int readValue() {
            return value;
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
