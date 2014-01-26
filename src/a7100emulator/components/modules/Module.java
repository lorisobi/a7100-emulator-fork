/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public interface Module {

    /**
     * 
     */
    public void init();
    
    /**
     * 
     * @param dos
     * @throws IOException
     */
    public void saveState(DataOutputStream dos) throws IOException;
    
    /**
     * 
     * @param dis
     * @throws IOException
     */
    public void loadState(DataInputStream dis) throws IOException;

}
