/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.Tools;

/**
 *
 * @author Dirk
 */
public class AddressSpace {

    private final int lowerAddress;
    private final int higherAddress;

    public AddressSpace(int low, int high) {
        this.lowerAddress=low;
        this.higherAddress=high;
    }

    /**
     * @return the lowerAddress
     */
    public int getLowerAddress() {
        return lowerAddress;
    }

    /**
     * @return the higherAddress
     */
    public int getHigherAddress() {
        return higherAddress;
    }
}
