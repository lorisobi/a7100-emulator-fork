/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator;

import a7100emulator.components.A7100;

/**
 *
 * @author Dirk
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new MainView(new A7100());
    }

}
