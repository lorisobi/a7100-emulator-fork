/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemClock;

/**
 *
 * @author Dirk
 */
public class KR580WI53 {

    private static int TEST_COUNTER = 0xC0;
    private static int TEST_RW = 0x30;
    private static int TEST_MODE = 0x0E;
    private static int TEST_TYPE = 0x01;
    private Counter[] counter = new Counter[3];

    public KR580WI53() {
        for (int i = 0; i < 3; i++) {
            counter[i] = new Counter(i);
        }
    }

    public void writeInit(int control) {
        int counterID = (control & TEST_COUNTER) >> 6;
        System.out.print("Initialisiere Counter " + counterID + ": ");
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

    public void writeCounter(int i, int data) {
        System.out.print("Setze Counter " + i + ": ");
        counter[i].setValue(data);
    }

    public int readCounter(int i) {
        int val = counter[i].getValue();
        System.out.println("Gelesen: " + val);
        return val;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    public void updateClock(int amount) {
        long oldclock = SystemClock.getInstance().getClock();
        //System.out.println("--- " + ((oldclock + amount) / 4 - oldclock / 4) + " --- ");
        counter[0].update((oldclock + amount) / 4 - oldclock / 4);
        counter[1].update((oldclock + amount) / 32 - oldclock / 32);
        counter[2].update((oldclock + amount) / 4 - oldclock / 4);
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
                    System.out.println("latched: " + latch);
                }
            } else {
                mode = (TEST_MODE & control) >> 1;
                rw = (TEST_RW & control) >> 4;
                type = TEST_TYPE & control;
            }
            System.out.println("MODE:" + mode + " RW:" + rw + " Type:" + type);
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
                        value |= val & 0xFF;
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
            System.out.println("Neuer Wert:" + value);
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
                    if (id == 0 && value + amount > 0) {
                        InterruptSystem.getInstance().addInterrupt(34);
                    }
                }
                //if (value>0) System.out.println("Neuer Wert:" + value);
            }
        }
    }
}
