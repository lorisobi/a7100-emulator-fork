/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public class KR580WI53 {

    private static final int TEST_COUNTER = 0xC0;
    private static final int TEST_RW = 0x30;
    private static final int TEST_MODE = 0x0E;
    private static final int TEST_TYPE = 0x01;
    private final Counter[] counter = new Counter[3];

    /**
     * 
     */
    public KR580WI53() {
        for (int i = 0; i < 3; i++) {
            counter[i] = new Counter(i);
        }
    }

    /**
     * 
     * @param control
     */
    public void writeInit(int control) {
        int counterID = (control & TEST_COUNTER) >> 6;
        //System.out.print("Initialisiere Counter " + counterID + ": ");
        switch (counterID) {
            case 0:
            case 1:
            case 2:
                counter[counterID].setCounterInit(control);
                break;
            case 3:
                break;
        }
    }

    /**
     * 
     * @param counterID
     * @param data
     */
    public void writeCounter(int counterID, int data) {
        //System.out.print("Setze Counter " + i + ": ");
        counter[counterID].setValue(data);
    }

    /**
     * 
     * @param counterID
     * @return
     */
    public int readCounter(int counterID) {
        int val = counter[counterID].getValue();
        //System.out.println("Gelesen Counter "+i+": " + Integer.toHexString(val));
        return val;
    }

    /**
     * 
     * @param amount
     */
    public void updateClock(int amount) {
        counter[0].update(amount >> 2);
        counter[1].update(amount >> 5);
        counter[2].update(amount >> 2);
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].saveState(dos);
        }
    }

    /**
     * 
     * @param dis
     * @throws IOException
     */
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 3; i++) {
            counter[i].loadState(dis);
        }
    }

    class Counter {

        private final int id;
        private boolean running = false;
        private boolean latched = false;
        private int value = 0;
        private int mode;
        private int type;
        private int rw;
        private int readWriteState = 0;
        private int latch = 0;

        public Counter(int id) {
            this.id = id;
        }

        public void setCounterInit(int control) {
            if (((TEST_RW & control) >> 4) == 0) {
                if (!latched) {
                    latch = value;
                    latched = true;
                    //        System.out.println("latched: " + latch);
                }
            } else {
                mode = (TEST_MODE & control) >> 1;
                rw = (TEST_RW & control) >> 4;
                type = TEST_TYPE & control;
            }
            //System.out.println("MODE:" + mode + " RW:" + rw + " Type:" + type);
        }

        public void setValue(int val) {
            switch (rw) {
                case 0:
                    break;
                case 1: // bits 0..7
                    value |= val & 0xFF;
                    running = true;
                    break;
                case 2: // bits 8..15
                    value |= (val & 0xFF) << 8;
                    running = true;
                    break;
                case 3:
                    if (readWriteState == 0) {
                        value = val & 0xFF;
                        readWriteState++;
                        running = false;
                    } else {
                        value |= (val & 0xFF) << 8;
                        readWriteState = 0;
                        if (value == 0) {
                            value = 65536;
                        }
                        running = true;
                    }
                    break;
            }
            //  System.out.println("Counter "+id+" - Neuer Wert:" + value);
        }

        public int getValue() {
            switch (rw) {
                case 0:
                    //return latch;
                    //return 0;
                    throw new IllegalStateException("Wrong Read/Write Mode in Counter");
                //break;
                case 1: // bits 0..7
                    if (latched) {
                        latched = false;
                        return latch & 0xFF;
                    } else {
                        return value & 0xFF;
                    }
                case 2: // bits 8..15
                    if (latched) {
                        latched = false;
                        return (latch >> 8) & 0xFF;
                    } else {
                        return (value >> 8) & 0xFF;
                    }
                case 3:
                    if (readWriteState == 0) {
                        readWriteState++;
                        if (latched) {
                            return latch & 0xFF;
                        } else {
                            return value & 0xFF;
                        }
                    } else {
                        readWriteState = 0;
                        if (latched) {
                            latched = false;
                            return (latch >> 8) & 0xFF;
                        } else {
                            return (value >> 8) & 0xFF;
                        }
                    }
                default:
                    throw new IllegalStateException("Wrong Read/Write Mode in Counter");
            }
        }

        private void update(long amount) {
            if (running) {
                value -= amount;
                if (value <= 0) {
                    value = 0;
                    if (value + amount > 0) {
                        running = false;
                        if (id == 0) {
                            InterruptSystem.getInstance().getPIC().requestInterrupt(2);
                        }
                    }
                }
                //if (value>0) System.out.println("Neuer Wert:" + value);
            }
        }

        private void saveState(DataOutputStream dos) throws IOException {
            dos.writeBoolean(running);
            dos.writeBoolean(latched);
            dos.writeInt(value);
            dos.writeInt(mode);
            dos.writeInt(type);
            dos.writeInt(rw);
            dos.writeInt(readWriteState);
            dos.writeInt(latch);
        }

        private void loadState(DataInputStream dis) throws IOException {
            running = dis.readBoolean();
            latched = dis.readBoolean();
            value = dis.readInt();
            mode = dis.readInt();
            type = dis.readInt();
            rw = dis.readInt();
            readWriteState = dis.readInt();
            latch = dis.readInt();
        }
    }
}
