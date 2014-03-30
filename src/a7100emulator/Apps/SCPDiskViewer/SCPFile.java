/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.Apps.SCPDiskViewer;

/**
 *
 * @author Dirk
 */
public class SCPFile {
    private int user;
    private String name;
    private String extension;
    private boolean readOnly;
    private boolean system;
    private boolean extra;
    private byte[] data;

    
    public SCPFile(String name, String extension, boolean readOnly, boolean system, boolean extra, int user) {
        this.name=name;
        this.extension=extension;
        this.readOnly=readOnly;
        this.system=system;
        this.extra=extra;
        this.user=user;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @param extension the extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public String getFullName() {
        return name.trim()+"."+extension.trim();
    }

    /**
     * @return the readOnlyFile
     */
    public boolean isReadOnlyFile() {
        return readOnly;
    }

    /**
     * @param readOnlyFile the readOnlyFile to set
     */
    public void setReadOnlyFile(boolean readOnlyFile) {
        this.readOnly = readOnlyFile;
    }

    /**
     * @return the systemFile
     */
    public boolean isSystemFile() {
        return system;
    }

    /**
     * @param systemFile the systemFile to set
     */
    public void setSystemFile(boolean systemFile) {
        this.system = systemFile;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * @return the user
     */
    public int getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(int user) {
        this.user = user;
    }

    /**
     * @return the extra
     */
    public boolean isExtra() {
        return extra;
    }

    /**
     * @param extra the extra to set
     */
    public void setExtra(boolean extra) {
        this.extra = extra;
    }
    
}
