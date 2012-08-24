/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.FloppyDrive;
import a7100emulator.components.system.FloppyDrive.DriveType;
import java.io.File;

/**
 *
 * @author Dirk
 */
public class AFS {

    private FloppyDrive[] drives = new FloppyDrive[4];

    public AFS() {
        drives[0] = new FloppyDrive(DriveType.K5601);
        drives[1] = new FloppyDrive(DriveType.K5601);
        drives[2] = new FloppyDrive(DriveType.K5602_10);
        drives[3] = new FloppyDrive(DriveType.K5602_10);
        
        drives[0].loadDisk(new File("./disks/disk2.bin"));
    }
    
    public FloppyDrive getFloppy(int id) {
        return drives[id];
    }
}
