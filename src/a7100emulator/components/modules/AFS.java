/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.FloppyDrive;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public final class AFS implements Module {

    private final FloppyDrive[] drives = new FloppyDrive[4];

    public AFS() {
        drives[0] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[1] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[2] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
        drives[3] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
    }

    public FloppyDrive getFloppy(int id) {
        return drives[id];
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].saveState(dos);
        }
    }

    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].loadState(dis);
        }
    }
}
