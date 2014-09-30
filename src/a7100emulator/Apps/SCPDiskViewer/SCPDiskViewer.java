/*
 * SCPFileViewer.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *   27.09.2014 MD5 Summen ergänzt
 */
package a7100emulator.Apps.SCPDiskViewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

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
     * Button - Disketten Image öffnen
     */
    private final JButton buttonOpenDisk = new JButton("Disketten Image öffnen");
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
     * Button - Beenden
     */
    private final JButton buttonExit = new JButton("Beenden");
    /**
     * Anzeige - Dateiinformationen
     */
    private final JLabel labelFileInfo = new JLabel(" ");
    /**
     * Anzeige Disketteninformationen
     */
    private final JLabel labelDiskInfo = new JLabel(" ");

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
        this.setPreferredSize(new Dimension(1000, 400));
        this.getContentPane().setLayout(new BorderLayout());

        fileTable.getColumn("RO").setPreferredWidth(2);
        fileTable.getColumn("SYS").setPreferredWidth(2);
        fileTable.getColumn("XTRA").setPreferredWidth(2);
        fileTable.getColumn("User").setPreferredWidth(2);
        fileTable.getColumn("MD5").setPreferredWidth(200);
        this.getContentPane().add(new JScrollPane(fileTable), BorderLayout.CENTER);

        JPanel panelButtons = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        panelButtons.add(buttonOpenDisk, gbc);
        gbc.gridy++;
        panelButtons.add(buttonExtractSingleFile, gbc);
        gbc.gridy++;
        panelButtons.add(buttonExtractAllFiles, gbc);
        gbc.gridy++;
        panelButtons.add(buttonExtractBootloader, gbc);
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 1;
        panelButtons.add(buttonExit, gbc);

        this.getContentPane().add(panelButtons, BorderLayout.EAST);

        JPanel panelLabels = new JPanel(new GridLayout(1, 2));
        labelDiskInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        panelLabels.add(labelFileInfo);
        panelLabels.add(labelDiskInfo);
        this.getContentPane().add(panelLabels, BorderLayout.SOUTH);

        this.pack();
        this.setVisible(true);

        this.setLocationRelativeTo(null);

        buttonExit.addActionListener(controller);
        buttonOpenDisk.addActionListener(controller);
        buttonExtractSingleFile.addActionListener(controller);
        buttonExtractAllFiles.addActionListener(controller);
        buttonExtractBootloader.addActionListener(controller);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Aktualisiert die Ansicht
     */
    void updateView() {
        ((SCPDiskTableModel) fileTable.getModel()).fireTableDataChanged();
        labelFileInfo.setText(diskModel.getImageName());
        labelDiskInfo.setText(diskModel.getDiskInfo());
    }

    /**
     * Controller für SCP-Disketten Tool
     *
     * @author Dirk Bräuer
     */
    private class SCPDiskController implements ActionListener {

        /**
         * Verarbeitet ein Action-Event
         *
         * @param e Event
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == buttonOpenDisk) {
                JFileChooser loadDialog = new JFileChooser("./disks/");
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = loadDialog.getSelectedFile();
                    diskModel.readImage(image);
                }
            } else if (e.getSource() == buttonExtractSingleFile) {
                int selectedRow = fileTable.getSelectedRow();
                if (selectedRow != -1) {
                    SCPFile file = diskModel.getFiles().get(selectedRow);
                    JFileChooser saveDialog = new JFileChooser("./disks/");

                    saveDialog.setSelectedFile(new File(file.getFullName()));
                    if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        diskModel.saveFile(selectedRow, saveDialog.getSelectedFile());

                    }
                }
            } else if (e.getSource() == buttonExtractAllFiles) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                saveDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    diskModel.saveAllFiles(saveDialog.getSelectedFile());
                }
            } else if (e.getSource() == buttonExtractBootloader) {
                JFileChooser saveDialog = new JFileChooser("./disks/");

                saveDialog.setSelectedFile(new File("bootloader.img"));
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    diskModel.saveBootloader(saveDialog.getSelectedFile());
                }
            } else if (e.getSource() == buttonExit) {
                SCPDiskViewer.this.dispose();
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
}
