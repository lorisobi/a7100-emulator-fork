/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class SCPDiskModel {

    private final ArrayList<SCPFile> files = new ArrayList<SCPFile>();
    private SCPDiskViewer view;
    private final int DIRECTORY_OFFSET = 0x3800;
    private final int DIRECTORY_LENGTH = 0x1000;
    private String imageName = "kein Image geöffnet";
    private int usedBlocks;
    private byte[] bootloader=new byte[0x1A00];

    /**
     * @return the files
     */
    public ArrayList<SCPFile> getFiles() {
        return files;
    }

    public void setView(SCPDiskViewer view) {
        this.view = view;
    }

    public void readImage(File image) {
        try {
            byte[] buffer = new byte[(int) image.length()];
            InputStream in = new FileInputStream(image);
            in.read(buffer);
            in.close();

            files.clear();

            imageName = image.getAbsolutePath();
            usedBlocks=0;
            System.arraycopy(buffer, 0, bootloader, 0, bootloader.length);
            
            byte[] fcb = new byte[32];
            for (int index = DIRECTORY_OFFSET; index < DIRECTORY_OFFSET + DIRECTORY_LENGTH; index += 32) {
                System.arraycopy(buffer, index, fcb, 0, 32);

                if (((int) fcb[0] & 0xFF) <= 16) {

                    String name = "";
                    for (int i = 1; i < 9; i++) {
                        name += (char) fcb[i];
                    }
                    String extension = "";
                    for (int i = 9; i < 12; i++) {
                        extension += (char) (fcb[i] & 0x7F);
                    }
                    boolean readOnly = (fcb[9] & 0x80) == 0x80;
                    boolean system = (fcb[10] & 0x80) == 0x80;
                    boolean extra = (fcb[11] & 0x80) == 0x80;

                    SCPFile scpFile = new SCPFile(name, extension, readOnly, system, extra, fcb[0]);
                    if (fcb[12] == 0) {
                        int blocks = (int) fcb[15] & 0xFF;
                        byte[] fileData = new byte[blocks * 0x80];
                        int remainBlocks = blocks;
                        for (int i = 16; i < 32 && remainBlocks > 0; i = i + 2) {
                            int address = ((((int) (fcb[i + 1] & 0xFF) << 8)) | ((int) fcb[i] & 0xFF)) * 0x800 + 0x3800;
                            if (address != 0x3800) {
                                usedBlocks++;
                                System.arraycopy(buffer, address, fileData, ((i - 16) / 2) * 0x800, remainBlocks > 15 ? 0x800 : remainBlocks * 0x80);
                                remainBlocks -= 16;
                            }

                        }

                        scpFile.setData(fileData);
                        files.add(scpFile);
                    } else {
                        // Nicht erster Teil einer Datei
                        int blocks = (int) fcb[15] & 0xFF;
                        int fcbIndex = fcb[12];
                        for (SCPFile file : files) {
                            if (file.getName().equals(name) && file.getExtension().equals(extension) && file.getUser() == fcb[0]) {
                                byte[] oldData = new byte[file.getData().length];
                                System.arraycopy(file.getData(), 0, oldData, 0, file.getData().length);
                                byte[] fileData = new byte[oldData.length + blocks * 0x80];
                                System.arraycopy(oldData, 0, fileData, 0, oldData.length);
                                int remainBlocks = blocks;
                                for (int i = 16; i < 32 && remainBlocks > 0; i = i + 2) {
                                    int address = ((((int) (fcb[i + 1] & 0xFF) << 8)) | ((int) fcb[i] & 0xFF)) * 0x800 + 0x3800;
                                    if (address != 0x3800) {
                                        System.arraycopy(buffer, address, fileData, fcbIndex * 0x800 * 8 + ((i - 16) / 2) * 0x800, remainBlocks > 15 ? 0x800 : remainBlocks * 0x80);
                                        remainBlocks -= 16;
                                        usedBlocks++;
                                    }
                                }
                                file.setData(fileData);
                            }
                        }
                    }
                }
            }
            view.updateView();
            System.out.println(usedBlocks+":"+usedBlocks*2048);
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the imageName
     */
    public String getImageName() {
        return imageName;
    }

    void saveFile(int index, File file) {
        try {
            SCPFile scpFile = files.get(index);
            OutputStream out;

            out = new FileOutputStream(file);
            out.write(scpFile.getData());
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void saveAllFiles(File directory) {
        for (int i = 0; i < files.size(); i++) {
            SCPFile scpFile = files.get(i);
            String filename = directory.getAbsolutePath() + File.separator + scpFile.getFullName() + (scpFile.getUser() == 0 ? "" : ("_U" + scpFile.getUser()));
            File extractFile = new File(filename);
            if (!extractFile.exists()) {
                saveFile(i, extractFile);
            } else {
                System.out.println("Datei " + filename + " existiert bereits!");
            }
        }
        saveBootloader(new File(directory.getAbsolutePath() + File.separator+"bootloader"));
    }

    String getDiskInfo() {
        return usedBlocks*2+"K belegt,"+(620-usedBlocks*2)+"K frei";
    }
    
    void saveBootloader(File file) {
        try {
            OutputStream out;
            out = new FileOutputStream(file);
            out.write(bootloader);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(SCPDiskModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
