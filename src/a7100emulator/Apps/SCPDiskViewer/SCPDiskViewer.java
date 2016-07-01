/*
 * SCPFileViewer.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   27.09.2014 - MD5 Summen ergänzt
 *   16.12.2014 - Hinzufügen von Dateien ermöglicht
 *              - Ansicht angepasst
 *   01.01.2015 - Datenbankinformationen ergänzt
 *   17.05.2015 - Punkt im Menüeintrag entfernt
 *   24.07.2015 - Datenbank exportieren hinzugefügt
 *   26.07.2015 - Auswahl löschen beim Laden von Image
 *   26.07.2016 - Doppelte Typdefinition entfernt
 *   28.07.2016 - Anzeige von Fehlern
 */
package a7100emulator.Apps.SCPDiskViewer;

import a7100emulator.Debug.MemoryAnalyzer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.MaskFormatter;

/**
 * Ansicht des SCP-Disketten-Tools
 *
 * @author Dirk Bräuer
 */
public class SCPDiskViewer extends JFrame {

    /**
     * Tabelle mit Dateien
     */
    private final JTable fileTable = new JTable(new SCPDiskTableModel());
    /**
     * Controller
     */
    private final SCPDiskController controller = new SCPDiskController();
    /**
     * Modell
     */
    private final SCPDiskModel diskModel;
    /**
     * Menü Datei
     */
    private final JMenu menuFile = new JMenu("Datei");
    /**
     * Menü Image
     */
    private final JMenu menuImage = new JMenu("Image");
    /**
     * Menü Datenbank
     */
    private final JMenu menuDatabase = new JMenu("Datenbank");
    /**
     * Menü Image öffen
     */
    private final JMenuItem menuOpenDisk = new JMenuItem("Image öffnen...");
    /**
     * Menü Verzeichnis importieren
     */
    private final JMenuItem menuImportFolder = new JMenuItem("Verzeichnis importieren...");
    /**
     * Menü Image speichern
     */
    private final JMenuItem menuSaveImage = new JMenuItem("Image speichern...");
    /**
     * Menü Beenden
     */
    private final JMenuItem menuExit = new JMenuItem("Beenden");
    /**
     * Menü Datei extrahieren
     */
    private final JMenuItem menuExtractSingleFile = new JMenuItem("Datei extrahieren");
    /**
     * Menü alle Dateien extrahieren
     */
    private final JMenuItem menuExtractAllFiles = new JMenuItem("Alle Dateien extrahieren");
    /**
     * Menü Datei hinzufügen
     */
    private final JMenuItem menuInsertFile = new JMenuItem("Datei hinzufügen");
    /**
     * Menü Datei anzeigen
     */
    private final JMenuItem menuShowFile = new JMenuItem("Datei anzeigen");
    /**
     * Menü Bootloader extrahieren
     */
    private final JMenuItem menuExtractBootloader = new JMenuItem("Bootloader speichern");
    /**
     * Menü Systemdatenbank exportieren
     */
    private final JMenuItem menuExportSystemDB = new JMenuItem("Systemdaten exportieren");
    /**
     * Menü Benutzerdatenabnk exportieren
     */
    private final JMenuItem menuExportUserDB = new JMenuItem("Benutzerdaten exportieren");
    /**
     * Button - Datei anzeigen
     */
    private final JButton buttonShowFile = new JButton("Datei anzeigen");
    /**
     * Button - Datei extrahieren
     */
    private final JButton buttonExtractSingleFile = new JButton("Datei extrahieren");
    /**
     * Button - alle Dateien extrahieren
     */
    private final JButton buttonExtractAllFiles = new JButton("Alle Dateien extrahieren");
    /**
     * Button - Systemspur extrahieren
     */
    private final JButton buttonExtractBootloader = new JButton("Anfangslader speichern");
    /**
     * Button Datei Hinzufügen
     */
    private final JButton buttonAddFile = new JButton("Datei hinzufügen");
    /**
     * Anzeige - Dateiinformationen
     */
    private final JLabel labelFileInfo = new JLabel(" ");
    /**
     * Anzeige Disketteninformationen
     */
    private final JLabel labelDiskInfo = new JLabel(" ");
    /**
     * DB-Info Normaler Name
     */
    private final JTextField textDBInfoName = new JTextField(15);
    /**
     * DB-Info Programmpaket
     */
    private final JComboBox comboDBInfoPackage = new JComboBox();
    /**
     * DB-Info Programmpaket
     */
    private final JTextField textDBInfoVersion = new JTextField();
    /**
     * DB-Info Dateityp
     */
    private final JComboBox comboDBInfoType = new JComboBox();
    /**
     * DB-Info Beschreibung
     */
    private final JTextArea textDBInfoDescription = new JTextArea();
    /**
     * DB-Info Benutzer
     */
    private final JCheckBox checkDBInfoUser = new JCheckBox();
    /**
     * DB-Info hinzufügen
     */
    private final JButton buttonDBInfoAdd = new JButton("Eintrag hinzufügen");
    /**
     * DB-Info löschen
     */
    private final JButton buttonDBInfoDel = new JButton("Eintrag löschen");

    /**
     * Erstellt eine neue Ansicht
     *
     * @param model Modell
     */
    public SCPDiskViewer(SCPDiskModel model) {
        super("SCP-Disk Tool");
        this.diskModel = model;
        initialize();
    }

    /**
     * Initialisiert die Ansicht
     */
    private void initialize() {
        setPreferredSize(new Dimension(1200, 700));
        setMinimumSize(new Dimension(1200, 700));
        getContentPane().setLayout(new BorderLayout());

        // Menü
        JMenuBar menubar = new JMenuBar();
        menubar.add(menuImage);
        menuImage.add(menuOpenDisk);
        menuImage.add(menuImportFolder);
        menuImage.add(menuSaveImage);
        menuImage.addSeparator();
        menuImage.add(menuExit);

        menubar.add(menuFile);
        menuFile.add(menuExtractSingleFile);
        menuFile.add(menuExtractAllFiles);
        menuFile.add(menuInsertFile);
        menuFile.addSeparator();
        menuFile.add(menuShowFile);
        menuFile.addSeparator();
        menuFile.add(menuExtractBootloader);

        menubar.add(menuDatabase);
        menuDatabase.add(menuExportSystemDB);
        menuDatabase.add(menuExportUserDB);

        menuOpenDisk.addActionListener(controller);
        menuImportFolder.addActionListener(controller);
        menuSaveImage.addActionListener(controller);
        menuExit.addActionListener(controller);
        menuShowFile.addActionListener(controller);
        menuExtractSingleFile.addActionListener(controller);
        menuExtractAllFiles.addActionListener(controller);
        menuExtractBootloader.addActionListener(controller);
        menuInsertFile.addActionListener(controller);
        menuExportSystemDB.addActionListener(controller);
        menuExportUserDB.addActionListener(controller);

        setJMenuBar(menubar);

        // Tabelle
        fileTable.setRowSorter(new TableRowSorter<>(fileTable.getModel()));

        fileTable.getColumn("RO").setPreferredWidth(2);
        fileTable.getColumn("SYS").setPreferredWidth(2);
        fileTable.getColumn("XTRA").setPreferredWidth(2);
        fileTable.getColumn("User").setPreferredWidth(2);
        fileTable.getColumn("MD5").setPreferredWidth(200);

        fileTable.setDefaultRenderer(String.class, new SCPDiskTableRenderer());
        fileTable.setDefaultRenderer(Boolean.class, new SCPDiskTableRenderer());
        fileTable.setDefaultRenderer(Integer.class, new SCPDiskTableRenderer());

        fileTable.setFont(new Font("Monospaced", Font.PLAIN, 14));

        getContentPane().add(new JScrollPane(fileTable), BorderLayout.CENTER);

        // Datenbankinformationen
        JScrollPane scrollPaneDescription = new JScrollPane(textDBInfoDescription);
        scrollPaneDescription.setMinimumSize(new Dimension(400, 300));
        textDBInfoDescription.setLineWrap(true);
        textDBInfoDescription.setWrapStyleWord(true);
        comboDBInfoType.setModel(new DefaultComboBoxModel(diskModel.getFileTypes()));
        comboDBInfoType.setEditable(true);
        comboDBInfoType.setSelectedItem("");
        comboDBInfoPackage.setModel(new DefaultComboBoxModel(diskModel.getSoftwarePackages()));
        comboDBInfoPackage.setEditable(true);
        comboDBInfoPackage.setSelectedItem("");

        textDBInfoName.setEnabled(false);
        comboDBInfoType.setEnabled(false);
        comboDBInfoPackage.setEnabled(false);
        textDBInfoVersion.setEnabled(false);
        textDBInfoDescription.setEnabled(false);
        checkDBInfoUser.setEnabled(false);

        JPanel panelDBInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        panelDBInfo.add(new JLabel("Normaler Name:"), gbc);
        gbc.gridx++;
        panelDBInfo.add(textDBInfoName, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panelDBInfo.add(new JLabel("Dateityp:"), gbc);
        gbc.gridx++;
        panelDBInfo.add(comboDBInfoType, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panelDBInfo.add(new JLabel("Programmpaket:"), gbc);
        gbc.gridx++;
        panelDBInfo.add(comboDBInfoPackage, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panelDBInfo.add(new JLabel("Version:"), gbc);
        gbc.gridx++;
        panelDBInfo.add(textDBInfoVersion, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panelDBInfo.add(new JLabel("Beschreibung:"), gbc);
        gbc.gridx++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panelDBInfo.add(scrollPaneDescription, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelDBInfo.add(new JLabel("Benutzereintrag:"), gbc);
        gbc.gridx++;
        panelDBInfo.add(checkDBInfoUser, gbc);
        gbc.gridy++;
        panelDBInfo.add(buttonDBInfoAdd, gbc);
        gbc.gridy++;
        panelDBInfo.add(buttonDBInfoDel, gbc);
        panelDBInfo.setBorder(BorderFactory.createTitledBorder("Datenbank-Informationen"));

        // Buttons und DB Info
        JPanel panelButtons = new JPanel(new GridBagLayout());

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        panelButtons.add(buttonShowFile, gbc);
        gbc.gridy++;
        panelButtons.add(buttonExtractSingleFile, gbc);
        gbc.gridy++;
        panelButtons.add(buttonExtractAllFiles, gbc);
        gbc.gridy++;
        panelButtons.add(buttonAddFile, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panelButtons.add(panelDBInfo, gbc);

        getContentPane().add(panelButtons, BorderLayout.EAST);

        // Statusleiste
        JPanel panelLabels = new JPanel(new GridLayout(1, 2));
        labelDiskInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        panelLabels.add(labelFileInfo);
        panelLabels.add(labelDiskInfo);
        getContentPane().add(panelLabels, BorderLayout.SOUTH);

        pack();
        setVisible(true);
        setLocationRelativeTo(null);

        // Listener hinzufügen
        buttonShowFile.addActionListener(controller);
        buttonExtractSingleFile.addActionListener(controller);
        buttonExtractAllFiles.addActionListener(controller);
        buttonExtractBootloader.addActionListener(controller);
        buttonAddFile.addActionListener(controller);
        buttonDBInfoAdd.addActionListener(controller);
        buttonDBInfoDel.addActionListener(controller);

        fileTable.getSelectionModel().addListSelectionListener(controller);

        this.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("Images/Disk.png"))).getImage());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Aktualisiert die Ansicht.
     */
    void updateView() {
        ((SCPDiskTableModel) fileTable.getModel()).fireTableDataChanged();
        labelFileInfo.setText(diskModel.getImageName());
        labelDiskInfo.setText(diskModel.getDiskInfo());
        boolean diskLoaded = diskModel.diskLoaded();
        buttonShowFile.setEnabled(diskLoaded);
        menuShowFile.setEnabled(diskLoaded);
        buttonExtractSingleFile.setEnabled(diskLoaded);
        menuExtractSingleFile.setEnabled(diskLoaded);
        buttonExtractAllFiles.setEnabled(diskLoaded);
        menuExtractAllFiles.setEnabled(diskLoaded);
        menuExtractBootloader.setEnabled(diskLoaded);
        buttonAddFile.setEnabled(diskLoaded);
        menuSaveImage.setEnabled(diskLoaded);
        buttonDBInfoAdd.setEnabled(diskLoaded && fileTable.getSelectedRow() != -1);
        buttonDBInfoDel.setEnabled(diskLoaded && fileTable.getSelectedRow() != -1);
    }

    /**
     * Controller für SCP-Disketten Tool
     *
     * @author Dirk Bräuer
     */
    private class SCPDiskController implements ActionListener, ListSelectionListener {

        /**
         * Verarbeitet ein Action-Event
         *
         * @param e Event
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == menuOpenDisk) {
                JFileChooser loadDialog = new JFileChooser("./disks/");
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    fileTable.clearSelection();
                    File image = loadDialog.getSelectedFile();
                    try {
                        diskModel.readImage(image);
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Erzeugen der MD5-Hashwerte!", "MD5-Hash-Fehler", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Lesen der Datei " + image.getName() + "!", "Image-Lesefehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == menuImportFolder) {
                JFileChooser loadDialog = new JFileChooser("./disks/");
                loadDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    fileTable.clearSelection();
                    File folder = loadDialog.getSelectedFile();
                    try {
                        diskModel.readFolder(folder);
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Erzeugen der MD5-Hashwerte!", "MD5-Hash-Fehler", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Lesen des Verzeichnisses " + folder.getName() + "!", "Ordner-Importfehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == buttonShowFile || e.getSource() == menuShowFile) {
                int selectedRowView = fileTable.getSelectedRow();
                if (selectedRowView != -1) {
                    int selectedRowModel = fileTable.convertRowIndexToModel(selectedRowView);
                    SCPFile file = diskModel.getFiles().get(selectedRowModel);
                    (new MemoryAnalyzer(file.getData(), file.getFullName())).show();
                }
            } else if (e.getSource() == buttonExtractSingleFile || e.getSource() == menuExtractSingleFile) {
                int selectedRowView = fileTable.getSelectedRow();
                if (selectedRowView != -1) {
                    int selectedRowModel = fileTable.convertRowIndexToModel(selectedRowView);
                    SCPFile file = diskModel.getFiles().get(selectedRowModel);
                    JFileChooser saveDialog = new JFileChooser("./disks/");
                    saveDialog.setSelectedFile(new File(file.getFullName()));
                    if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        diskModel.saveFile(selectedRowModel, saveDialog.getSelectedFile());
                    }
                }
            } else if (e.getSource() == buttonExtractAllFiles || e.getSource() == menuExtractAllFiles) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                saveDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    diskModel.saveAllFiles(saveDialog.getSelectedFile());
                }
            } else if (e.getSource() == menuExtractBootloader) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                saveDialog.setSelectedFile(new File("bootloader.img"));
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    diskModel.saveBootloader(saveDialog.getSelectedFile());
                }
            } else if (e.getSource() == buttonAddFile || e.getSource() == menuInsertFile) {
                JFileChooser loadDialog = new JFileChooser(".");
                loadDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        File importFile = loadDialog.getSelectedFile();

                        int startExtension = importFile.getName().lastIndexOf('.');
                        String extension = (startExtension == -1 ? "" : importFile.getName().substring(startExtension + 1)).toUpperCase();
                        if (extension.length() > 3) {
                            extension = extension.substring(0, 3);
                        }
                        String filename = (startExtension == -1 ? importFile.getName() : importFile.getName().substring(0, startExtension)).toUpperCase();
                        if (filename.length() > 8) {
                            filename = filename.substring(0, 8);
                        }

                        byte[] data = new byte[(int) importFile.length()];
                        InputStream in = new FileInputStream(importFile);
                        in.read(data);
                        in.close();

                        JFormattedTextField editFilename = new JFormattedTextField(new MaskFormatter("********"));
                        JFormattedTextField editExtension = new JFormattedTextField(new MaskFormatter("***"));
                        JCheckBox checkReadOnly = new JCheckBox("");
                        JCheckBox checkSystem = new JCheckBox("");
                        JCheckBox checkExtra = new JCheckBox("");
                        JFormattedTextField editUser = new JFormattedTextField(NumberFormat.getIntegerInstance());

                        editFilename.setText(filename);
                        editExtension.setText(extension);
                        editUser.setValue(0);

                        JPanel panelEdit = new JPanel(new GridLayout(6, 2));
                        panelEdit.add(new JLabel("Dateiname:"));
                        panelEdit.add(editFilename);
                        panelEdit.add(new JLabel("Erweiterung:"));
                        panelEdit.add(editExtension);
                        panelEdit.add(new JLabel("Schreibgeschützt:"));
                        panelEdit.add(checkReadOnly);
                        panelEdit.add(new JLabel("System:"));
                        panelEdit.add(checkSystem);
                        panelEdit.add(new JLabel("Extra:"));
                        panelEdit.add(checkExtra);
                        panelEdit.add(new JLabel("Nutzer:"));
                        panelEdit.add(editUser);
                        if (JOptionPane.showConfirmDialog(null, panelEdit, "Datei importieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                            filename = editFilename.getText().toUpperCase();
                            extension = editExtension.getText().toUpperCase();
                            boolean readOnly = checkReadOnly.isSelected();
                            boolean system = checkSystem.isSelected();
                            boolean extra = checkExtra.isSelected();
                            int user = Integer.parseInt(editUser.getText());
                            user = (user < 0) ? 0 : ((user > 15) ? 15 : user);
                            diskModel.insertFile(filename, extension, readOnly, system, extra, user, data);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (e.getSource() == menuSaveImage) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                saveDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = saveDialog.getSelectedFile();
                    if (saveFile.exists()) {
                        JOptionPane.showMessageDialog(null, "Datei ist bereits vorhanden!", "Fehler Image Speichern", JOptionPane.ERROR_MESSAGE);
                    } else {
                        diskModel.saveImage(saveFile);
                    }
                }
            } else if (e.getSource() == menuExportSystemDB) {
                JFileChooser saveDialog = new JFileChooser(".");
                saveDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
                saveDialog.setSelectedFile(new File("system.csv"));
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = saveDialog.getSelectedFile();
                    try {
                        diskModel.exportDB(saveFile, false);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Speichern der Datei " + saveFile.getName() + "!", "Exportfehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == menuExportUserDB) {
                JFileChooser saveDialog = new JFileChooser(".");
                saveDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
                saveDialog.setSelectedFile(new File("user.csv"));
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = saveDialog.getSelectedFile();
                    try {
                        diskModel.exportDB(saveFile, true);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Speichern der Datei " + saveFile.getName() + "!", "Exportfehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == buttonDBInfoAdd) {
                int selectedRowView = fileTable.getSelectedRow();
                if (selectedRowView != -1) {
                    int selectedRowModel = fileTable.convertRowIndexToModel(selectedRowView);
                    String md5 = diskModel.getFiles().get(selectedRowModel).getMD5();
                    FileInfo info = new FileInfo(textDBInfoName.getText(), comboDBInfoType.getSelectedItem().toString(), comboDBInfoPackage.getSelectedItem().toString(), textDBInfoVersion.getText(), textDBInfoDescription.getText(), checkDBInfoUser.isSelected());

                    try {
                        diskModel.updateDBInfo(md5, info);
                    } catch (IOException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Speichern der Informationen in der Datenbank!", "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
                    }

                    comboDBInfoType.setModel(new DefaultComboBoxModel(diskModel.getFileTypes()));
                    comboDBInfoPackage.setModel(new DefaultComboBoxModel(diskModel.getSoftwarePackages()));

                    comboDBInfoType.setSelectedItem(info.getFileType());
                    comboDBInfoPackage.setSelectedItem(info.getSoftwarePackage());
                }
            } else if (e.getSource() == buttonDBInfoDel) {
                int selectedRowView = fileTable.getSelectedRow();
                if (selectedRowView != -1) {
                    int selectedRowModel = fileTable.convertRowIndexToModel(selectedRowView);
                    String md5 = diskModel.getFiles().get(selectedRowModel).getMD5();

                    try {
                        diskModel.removeDBInfo(md5);
                    } catch (IOException ex) {
                        Logger.getLogger(SCPDiskViewer.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(null, "Fehler beim Speichern der Informationen in der Datenbank!", "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == menuExit) {
                SCPDiskViewer.this.dispose();
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() == fileTable.getSelectionModel()) {
                int selectedRowView = fileTable.getSelectedRow();

                textDBInfoName.setEnabled(selectedRowView != -1);
                comboDBInfoType.setEnabled(selectedRowView != -1);
                comboDBInfoPackage.setEnabled(selectedRowView != -1);
                textDBInfoVersion.setEnabled(selectedRowView != -1);
                textDBInfoDescription.setEnabled(selectedRowView != -1);
                checkDBInfoUser.setEnabled(selectedRowView != -1);
                buttonDBInfoAdd.setEnabled(selectedRowView != -1);
                buttonDBInfoDel.setEnabled(selectedRowView != -1);

                if (selectedRowView != -1) {
                    int selectedRowModel = fileTable.convertRowIndexToModel(selectedRowView);
                    SCPFile scpFile = diskModel.getFiles().get(selectedRowModel);
                    FileInfo fileInfo = diskModel.getFileInfo(scpFile);

                    textDBInfoName.setText(fileInfo != null ? fileInfo.getName() : scpFile.getFullName());
                    comboDBInfoType.setSelectedItem(fileInfo != null ? fileInfo.getFileType() : "");
                    comboDBInfoPackage.setSelectedItem(fileInfo != null ? fileInfo.getSoftwarePackage() : "");
                    textDBInfoVersion.setText(fileInfo != null ? fileInfo.getVersion() : "");
                    textDBInfoDescription.setText(fileInfo != null ? fileInfo.getDescription() : "");
                    checkDBInfoUser.setSelected(fileInfo != null ? fileInfo.isUser() : true);

                    buttonDBInfoDel.setEnabled(fileInfo != null);
                }
            }
        }
    }

    /**
     * Tabellenmodell für SCP-Dateien
     *
     * @author Dirk Bräuer
     */
    private class SCPDiskTableModel extends AbstractTableModel {

        /**
         * Gibt die Anzahl der Zeilen zurück
         *
         * @return Anzahl der Zeilen
         */
        @Override
        public int getRowCount() {
            return diskModel.getFiles().size();
        }

        /**
         * Gibt die Anzahl der Spalten zurück
         *
         * @return Anzahl der Spalten
         */
        @Override
        public int getColumnCount() {
            return 7;
        }

        /**
         * Gibt den Spaltennamen zurück
         *
         * @param column Spaltennummer
         * @return Spaltenname
         */
        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Dateiname";
                case 1:
                    return "RO";
                case 2:
                    return "SYS";
                case 3:
                    return "XTRA";
                case 4:
                    return "User";
                case 5:
                    return "Größe in Bytes";
                case 6:
                    return "MD5";
                default:
                    throw new IllegalArgumentException("Column " + column + " existiert nicht!");
            }
        }

        /**
         * Gibt die Klasse der Spaltendaten zurück
         *
         * @param column Spaltennummer
         * @return Klasse
         */
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
                case 0:
                case 6:
                    return String.class;
                case 1:
                case 2:
                case 3:
                    return Boolean.class;
                case 4:
                case 5:
                    return Integer.class;
                default:
                    throw new IllegalArgumentException("Column " + column + " existiert nicht!");
            }
        }

        /**
         * Gibt an ob die gewählte Zelle editierbar ist
         *
         * @param row Zeilennummer
         * @param column Spaltennummer
         * @return true - wenn editierbar , false - sonst
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * Gibt das Objekt an der gegebenen Poision zurück
         *
         * @param row Zeilennummer
         * @param column Spaltennummer
         * @return Objekt der Zelle
         */
        @Override
        public Object getValueAt(int row, int column) {
            SCPFile file = diskModel.getFiles().get(row);

            switch (column) {
                case 0:
                    return file.getFullName();
                case 1:
                    return file.isReadOnlyFile();
                case 2:
                    return file.isSystemFile();
                case 3:
                    return file.isExtra();
                case 4:
                    return file.getUser();
                case 5:
                    return file.getData().length;
                case 6:
                    return file.getMD5();
                default:
                    throw new IllegalArgumentException("Column " + column + " existiert nicht!");
            }
        }

        /**
         * Setzt das Objekt der Zelle
         *
         * @param value Objekt
         * @param row Zeilennummer
         * @param column Spaltennummer
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }

    /**
     * Renderer für Tabelle mit SCP-Dateien. Färbt die Zeilen entsprechend den
     * Einträgen in der Datenbank ein.
     */
    private class SCPDiskTableRenderer extends DefaultTableCellRenderer {

        /**
         * Farbe Hellblau
         */
        private final Color LIGHT_BLUE = new Color(220, 220, 255);
        /**
         * Farbe helles Rot
         */
        private final Color LIGHT_RED = new Color(255, 220, 220);
        /**
         * Farbe helles Gelb
         */
        private final Color LIGHT_YELLOW = new Color(255, 255, 220);

        /**
         * Gibt den Renderer für die entsprechende Zelle zurück.
         *
         * @param table Tabelle
         * @param value Wert
         * @param isSelected <code>true</code> wenn Zelle ausgewählt ist
         * @param hasFocus   <code>true</code> wenn Zelle den Fokus besitzt
         * @param row Zeile
         * @param column Spalte
         * @return Anzeigekomponente
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            SCPFile file = diskModel.getFiles().get(table.convertRowIndexToModel(row));
            FileInfo fileInfo = diskModel.getFileInfo(file);

            if (value instanceof Integer) {
                ((JLabel) component).setHorizontalAlignment(JLabel.RIGHT);
            } else if (value instanceof Boolean) {
                component = new JCheckBox();
                ((JCheckBox) component).setHorizontalAlignment(JLabel.CENTER);
                ((JCheckBox) component).setSelected(value != null && ((Boolean) value));
            }

            if (table.getSelectedRow() == row) {
                component.setBackground(Color.GRAY);
            } else if (fileInfo == null) {
                component.setBackground(Color.WHITE);
            } else {
                if (file.getData().length == 0) {
                    component.setBackground(LIGHT_YELLOW);
                } else if (fileInfo.isUser()) {
                    component.setBackground(LIGHT_RED);
                } else {
                    component.setBackground(LIGHT_BLUE);
                }
            }
            return component;
        }
    }
}
