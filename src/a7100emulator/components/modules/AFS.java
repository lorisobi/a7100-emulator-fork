/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.FloppyDrive;
import java.io.Serializable;

/**
 *
 * @author Dirk
 */
public class AFS implements Serializable {

    private FloppyDrive[] drives = new FloppyDrive[4];

    public AFS() {
        drives[0] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[1] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[2] = null;
        drives[3] = null;
    }

    public FloppyDrive getFloppy(int id) {
        return drives[id];
        //return FloppyDrive.getInstance(id);
    }
}
