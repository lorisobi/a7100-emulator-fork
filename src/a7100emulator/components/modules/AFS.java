/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.FloppyDrive;

/**
 *
 * @author Dirk
 */
public class AFS {

    public AFS() {
     //   FloppyDrive.getInstance(0).loadDisk(new File("./disks/spiele.bin"));
    }

    public FloppyDrive getFloppy(int id) {
        return FloppyDrive.getInstance(id);
    }
}
