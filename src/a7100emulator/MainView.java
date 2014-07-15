/*
 * MainView.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *   12.04.2014 Laden von verschiedenen Image-Typen implementiert
 *
 */
package a7100emulator;

import a7100emulator.Apps.SCPDiskViewer.SCPDiskModel;
import a7100emulator.Apps.SCPDiskViewer.SCPDiskViewer;
import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Debug.OpcodeStatistic;
import a7100emulator.components.A7100;
import a7100emulator.components.system.FloppyDisk;
import a7100emulator.Tools.FloppyImageType;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.Screen;
import a7100emulator.components.system.SystemMemory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Hauptansicht des A7100 Emulators
 *
 * @author Dirk Bräuer
 */
public class MainView extends JFrame {

    /**
     * Menü Emulator
     */
    private final JMenu menuEmulator = new JMenu("Emulator");
    /**
     * Menü Geräte
     */
    private final JMenu menuDevices = new JMenu("Geräte");
    /**
     * Menü Debug
     */
    private final JMenu menuDebug = new JMenu("Debug");
    /**
     * Menü Hilfe
     */
    private final JMenu menuHelp = new JMenu("Hilfe");
    /**
     * Menüeintrag Emulator zurücksetzen
     */
    private final JMenuItem menuEmulatorReset = new JMenuItem("Reset");
    /**
     * Menüeintrag Emulator pausieren
     */
    private final JCheckBoxMenuItem menuEmulatorPause = new JCheckBoxMenuItem("Pause");
    /**
     * Menüeintrag Emulator Einzelschritt
     */
    private final JMenuItem menuEmulatorSingle = new JMenuItem("Einzelschritt");
    /**
     * Menüeintrag Emulator Zustand Speichern
     */
    private final JMenuItem menuEmulatorSave = new JMenuItem("Zustand Speichern");
    /**
     * Menüeintrag Emulator Zustand Laden
     */
    private final JMenuItem menuEmulatorLoad = new JMenuItem("Zustand Laden");
    /**
     * Menüeintrag Emulator Beenden
     */
    private final JMenuItem menuEmulatorExit = new JMenuItem("Beenden");
    /**
     * Untermenü Laufwerk 0
     */
    private final JMenu menuDevicesDrive0 = new JMenu("Laufwerk 0");
    /**
     * Untermenü Laufwerk 1
     */
    private final JMenu menuDevicesDrive1 = new JMenu("Laufwerk 1");
    /**
     * Menüeintrag Laufwerk 0 - Image Laden
     */
    private final JMenuItem menuDevicesDrive0Load = new JMenuItem("Lade Image");
    /**
     * Menüeintrag Laufwerk 0 - Image Speichern
     */
    private final JMenuItem menuDevicesDrive0Save = new JMenuItem("Speicher Image");
    /**
     * Menüeintrag Laufwerk 0 - Image Auswerfen
     */
    private final JMenuItem menuDevicesDrive0Eject = new JMenuItem("Auswerfen");
    /**
     * Menüeintrag Laufwerk 0 - Leere Diskette einlegen
     */
    private final JMenuItem menuDevicesDrive0Empty = new JMenuItem("Leere Diskette");
    /**
     * Menüeintrag Laufwerk 0 - Schreibschutz setzen
     */
    private final JCheckBoxMenuItem menuDevicesDrive0WriteProtect = new JCheckBoxMenuItem("Schreibschutz");
    /**
     * Menüeintrag Laufwerk 1 - Image Laden
     */
    private final JMenuItem menuDevicesDrive1Load = new JMenuItem("Lade Image");
    /**
     * Menüeintrag Laufwerk 1 - Image Speichern
     */
    private final JMenuItem menuDevicesDrive1Save = new JMenuItem("Speicher Image");
    /**
     * Menüeintrag Laufwerk 1 - Image Auswerfen
     */
    private final JMenuItem menuDevicesDrive1Eject = new JMenuItem("Auswerfen");
    /**
     * Menüeintrag Laufwerk 1 - Leere Diskette einlegen
     */
    private final JMenuItem menuDevicesDrive1Empty = new JMenuItem("Leere Diskette");
    /**
     * Menüeintrag Laufwerk 1 - Schreibschutz setzen
     */
    private final JCheckBoxMenuItem menuDevicesDrive1WriteProtect = new JCheckBoxMenuItem("Schreibschutz");
    /**
     * Untermenü Speicher
     */
    private final JMenu menuDebugMemory = new JMenu("Speicher");
    /**
     * Untermenü Decoder
     */
    private final JMenu menuDebugDecoder = new JMenu("Decoder");
    /**
     * Untermenü Debugger
     */
    private final JMenu menuDebugDebugger = new JMenu("Debugger");
    /**
     * Menüeintrag Debugger aktivieren
     */
    private final JCheckBoxMenuItem menuDebugDebuggerSwitch = new JCheckBoxMenuItem("Debugger");
    /**
     * Menüeintrag Verzögerung setzen
     */
    private final JMenuItem menuDebugDebuggerSlowdown = new JMenuItem("Setze Verzögerung");
    /**
     * Menüeintrag Speicher anzeigen
     */
    private final JMenuItem menuDebugMemoryShow = new JMenuItem("zeigen");
    /**
     * Menüeintrag Speicher in Datei Schreiben
     */
    private final JMenuItem menuDebugMemoryDump = new JMenuItem("Dump");
    /**
     * Menüeintrag Decoder anzeigen
     */
    private final JMenuItem menuDebugDecoderShow = new JMenuItem("zeigen");
    /**
     * Menüeintrag Decoderinformationen speichern
     */
    private final JMenuItem menuDebugDecoderDump = new JMenuItem("Dump");
    /**
     * Menüeintrag Zeichensatz anzeigen
     */
    private final JMenuItem menuDebugCharacters = new JMenuItem("KGS Zeichensatz");
    /**
     * Menüeintrag Opcode-Statistik speichern
     */
    private final JMenuItem menuOpcodeStatistic = new JMenuItem("Dump Statistik");
    /**
     * Menü Tools
     */
    private final JMenu menuTools = new JMenu("Tools");
    /**
     * Menüeintrag SCP-Disk Betrachter starten
     */
    private final JMenuItem menuToolsSCPDiskViewer = new JMenuItem("SCP-Disk Betrachter");
    /**
     * Menüeintrag Screenshot speichern
     */
    private final JMenuItem menuToolsScreenshot = new JMenuItem("Bildschirmfoto");
    /**
     * Menüeintrag Über
     */
    private final JMenuItem menuHelpAbout = new JMenuItem("Über");
    /**
     * Controller
     */
    private final MainMenuController controller = new MainMenuController();
    /**
     * Statusanzeige
     */
    private final JLabel statusBar = new JLabel("Status");
    /**
     * Referenz auf A7100
     */
    private final A7100 a7100;

    /**
     * Erstellt eine neue Hauptansicht
     *
     * @param a7100 Referenz auf A7100
     */
    public MainView(A7100 a7100) {
        super("A7100 Emulator");

        this.a7100 = a7100;
        JMenuBar menubar = new JMenuBar();
        menubar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F10"), "none");

        menubar.add(menuEmulator);
        menuEmulator.add(menuEmulatorReset);
        menuEmulator.add(menuEmulatorPause);
        menuEmulator.add(menuEmulatorSingle);
        menuEmulator.addSeparator();
        menuEmulator.add(menuEmulatorSave);
        menuEmulator.add(menuEmulatorLoad);
        menuEmulator.addSeparator();
        menuEmulator.add(menuEmulatorExit);

        menuEmulatorReset.addActionListener(controller);
        menuEmulatorPause.addActionListener(controller);
        menuEmulatorSingle.addActionListener(controller);
        menuEmulatorSave.addActionListener(controller);
        menuEmulatorLoad.addActionListener(controller);
        menuEmulatorExit.addActionListener(controller);

        menuEmulatorSingle.setEnabled(false);

        menubar.add(menuDevices);
        menuDevices.add(menuDevicesDrive0);
        menuDevicesDrive0.add(menuDevicesDrive0Load);
        menuDevicesDrive0.add(menuDevicesDrive0Save);
        menuDevicesDrive0.add(menuDevicesDrive0Eject);
        menuDevicesDrive0.add(menuDevicesDrive0Empty);
        menuDevicesDrive0.add(menuDevicesDrive0WriteProtect);
        menuDevices.add(menuDevicesDrive1);
        menuDevicesDrive1.add(menuDevicesDrive1Load);
        menuDevicesDrive1.add(menuDevicesDrive1Save);
        menuDevicesDrive1.add(menuDevicesDrive1Eject);
        menuDevicesDrive1.add(menuDevicesDrive1Empty);
        menuDevicesDrive1.add(menuDevicesDrive1WriteProtect);

        menuDevicesDrive0Load.addActionListener(controller);
        menuDevicesDrive0Save.addActionListener(controller);
        menuDevicesDrive0Eject.addActionListener(controller);
        menuDevicesDrive0Empty.addActionListener(controller);
        menuDevicesDrive0WriteProtect.addActionListener(controller);
        menuDevicesDrive1Load.addActionListener(controller);
        menuDevicesDrive1Save.addActionListener(controller);
        menuDevicesDrive1Eject.addActionListener(controller);
        menuDevicesDrive1Empty.addActionListener(controller);
        menuDevicesDrive1WriteProtect.addActionListener(controller);

        menubar.add(menuDebug);
        menuDebug.add(menuDebugMemory);
        menuDebugMemory.add(menuDebugMemoryShow);
        menuDebugMemory.add(menuDebugMemoryDump);
        menuDebug.add(menuDebugDecoder);
        menuDebugDecoder.add(menuDebugDecoderShow);
        menuDebugDecoder.add(menuDebugDecoderDump);
        menuDebug.add(menuDebugDebugger);
        menuDebugDebugger.add(menuDebugDebuggerSwitch);
        menuDebugDebugger.add(menuDebugDebuggerSlowdown);
        menuDebug.add(menuDebugCharacters);
        menuDebug.add(menuOpcodeStatistic);

        menuDebugDebuggerSwitch.addActionListener(controller);
        menuDebugDebuggerSlowdown.addActionListener(controller);
        menuDebugMemoryShow.addActionListener(controller);
        menuDebugMemoryDump.addActionListener(controller);
        menuDebugDecoderShow.addActionListener(controller);
        menuDebugDecoderDump.addActionListener(controller);
        menuDebugCharacters.addActionListener(controller);
        menuOpcodeStatistic.addActionListener(controller);

        menubar.add(menuTools);
        menuTools.add(menuToolsSCPDiskViewer);
        menuTools.add(menuToolsScreenshot);

        menuToolsSCPDiskViewer.addActionListener(controller);
        menuToolsScreenshot.addActionListener(controller);

        menubar.add(menuHelp);
        menuHelp.add(menuHelpAbout);

        menuHelpAbout.addActionListener(controller);

        this.setJMenuBar(menubar);
        this.add(Screen.getInstance(), BorderLayout.CENTER);
        //this.add(statusBar,BorderLayout.SOUTH);
        this.addKeyListener(Keyboard.getInstance());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("Images/Icon.png"))).getImage());
        this.setResizable(true);
        this.setVisible(true);
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * Controller für Hauptansicht
     *
     * @author Dirk Bräuer
     */
    private class MainMenuController implements ActionListener {

        /**
         * Verarbeitet ein Action Event
         *
         * @param e Event
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(menuEmulatorReset)) {
                a7100.reset();
            } else if (e.getSource().equals(menuEmulatorPause)) {
                if (menuEmulatorPause.isSelected()) {
                    a7100.getZVE().pause();
                } else {
                    a7100.getZVE().resume();
                }
                menuEmulatorSingle.setEnabled(menuEmulatorPause.isSelected());
            } else if (e.getSource().equals(menuEmulatorSingle)) {
                a7100.getZVE().singleStep();
            } else if (e.getSource().equals(menuEmulatorSave)) {
                a7100.saveState();
            } else if (e.getSource().equals(menuEmulatorLoad)) {
                a7100.loadState();
            } else if (e.getSource() == menuEmulatorExit) {
                System.exit(0);
            } else if (e.getSource().equals(menuDebugMemoryShow)) {
                (new MemoryAnalyzer()).show();
            } else if (e.getSource() == menuDebugDecoderShow) {
                Decoder.getInstance().show();
            } else if (e.getSource() == menuDebugMemoryDump) {
                SystemMemory.getInstance().dump("./debug/user_dump.hex");
            } else if (e.getSource() == menuDebugDecoderDump) {
                Decoder.getInstance().save();
            } else if (e.getSource() == menuDebugDebuggerSwitch) {
                boolean debug = menuDebugDebuggerSwitch.isSelected();
                Debugger.getInstance().setDebug(debug);
                if (debug) {
                    Decoder.getInstance().clear();
                }
            } else if (e.getSource() == menuDebugDebuggerSlowdown) {
                try {
                    int slowdown = Integer.parseInt(JOptionPane.showInputDialog(null, "Verzögerung in ms:", Debugger.getInstance().getSlowdown()));
                    Debugger.getInstance().setSlowdown(slowdown);
                } catch (NumberFormatException ex) {

                }
            } else if (e.getSource() == menuDebugCharacters) {
                a7100.getKGS().showCharacters();
            } else if (e.getSource() == menuOpcodeStatistic) {
                OpcodeStatistic.getInstance().dump();
            } else if (e.getSource() == menuDevicesDrive0Load) {
                loadImageFile(0);
            } else if (e.getSource() == menuDevicesDrive0Save) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = saveDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(0).saveDiskToFile(image);
                }
            } else if (e.getSource() == menuDevicesDrive0Eject) {
                a7100.getKES().getAFS().getFloppy(0).ejectDisk();
            } else if (e.getSource() == menuDevicesDrive0Empty) {
                a7100.getKES().getAFS().getFloppy(0).newDisk();
            } else if (e.getSource() == menuDevicesDrive0WriteProtect) {
                a7100.getKES().getAFS().getFloppy(0).setWriteProtect(menuDevicesDrive0WriteProtect.isSelected());
            } else if (e.getSource() == menuDevicesDrive1Load) {
                loadImageFile(1);
            } else if (e.getSource() == menuDevicesDrive1Save) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = saveDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(1).saveDiskToFile(image);
                }
            } else if (e.getSource() == menuDevicesDrive1Eject) {
                a7100.getKES().getAFS().getFloppy(1).ejectDisk();
            } else if (e.getSource() == menuDevicesDrive1Empty) {
                a7100.getKES().getAFS().getFloppy(1).newDisk();
            } else if (e.getSource() == menuDevicesDrive1WriteProtect) {
                a7100.getKES().getAFS().getFloppy(1).setWriteProtect(menuDevicesDrive0WriteProtect.isSelected());
            } else if (e.getSource() == menuToolsSCPDiskViewer) {
                SCPDiskModel model = new SCPDiskModel();
                SCPDiskViewer view = new SCPDiskViewer(model);
                model.setView(view);
            } else if (e.getSource() == menuToolsScreenshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
                String dateString = sdf.format(Calendar.getInstance().getTime());
                File snapFile = new File("./screenshots/" + dateString + " A7100.png");
                if (snapFile.exists()) {
                    int i = 0;
                    do {
                        snapFile = new File("./screenshots/" + dateString + " A7100" + i + ".png");
                    } while (snapFile.exists());
                }
                try {
                    ImageIO.write(Screen.getInstance().getImage(), "png", snapFile);
                } catch (IOException ex) {
                    Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (e.getSource() == menuHelpAbout) {
                JPanel pan_about = new JPanel();
                pan_about.setLayout(new BorderLayout());
                pan_about.add(new JLabel(new ImageIcon(this.getClass().getClassLoader().getResource("Images/Icon.png"))), BorderLayout.WEST);
                JPanel pan_desc = new JPanel();
                pan_desc.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 10));
                pan_desc.setLayout(new GridLayout(2, 1));
                pan_desc.add(new JLabel("A7100 - Emulator v0.6.20"));
                pan_desc.add(new JLabel("2011-2014 Dirk Bräuer"));
                pan_about.add(pan_desc, BorderLayout.CENTER);
                JTextArea licenseText = new JTextArea();
                licenseText.setText("Lizenzinformationen:\n\n"
                        + "Diese Software ist Freeware und darf uneingeschränkt\n"
                        + "genutzt, kopiert und verbreitet werden, solange die\n"
                        + "folgenden Bedingungen erfüllt sind:\n"
                        + "  1. Die Software, oder ihre Bestandteile, dürfen nicht\n"
                        + "     verkauft oder ohne Genehmigung des Autors mit\n"
                        + "     anderen Programmen gebündelt werden.\n"
                        + "  2. Die Software darf kostenlos zum Download\n"
                        + "     bereitgestellt werden.\n"
                        + "  3. Der Autor bleibt auch bei Weitergabe Eigentümer der\n"
                        + "     Software.\n"
                        + "  4. Alle Programmteile müssen für die Weitergabe\n"
                        + "     unverändert bleiben. Insbesondere dürfen weder\n"
                        + "     Programmname, Name des Autors noch die vorliegenden\n"
                        + "     Lizenzinformationen verändert werden.\n"
                        + "  5. Die Software darf ohne Einwilligung des Autors\n"
                        + "     nicht Disassembliert werden.\n"
                        + "  6. Die für den Emulator benötigten EPROM- und\n"
                        + "     Diskettenabbilder unterliegen ggf. weiteren\n"
                        + "     Lizenzbestimmungen. Der Anwender verpflichtet sich\n"
                        + "     diese bei der Verwendung der Software einzuhalten.\n"
                        + "  7. Das Programm wird bereitgestellt \"WIE-ES-IST\" und\n"
                        + "     die Nutzung erfolgt ausschließlich auf eigenes\n"
                        + "     Risiko. Der Autor übernimmt keine Garantie das die\n"
                        + "     Software frei von Fehlern ist, ohne Unterbrechung\n"
                        + "     arbeitet oder den jeweils gestellten Anforderungen\n"
                        + "     entspricht. Für Sachschäden oder finanzielle\n"
                        + "     Schäden, welche aus der Verwendung des Programms\n"
                        + "     resultieren,bspw. Verlust von Daten, Verlust von\n"
                        + "     Gewinn, Betriebsunterbrechung, übernimmt der Autor\n"
                        + "     keinerlei Haftung."
                );
                licenseText.setEditable(false);
                final JScrollPane spLicenseText = new JScrollPane(licenseText);
                spLicenseText.setPreferredSize(new Dimension(350, 150));
                pan_about.add(spLicenseText, BorderLayout.SOUTH);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        spLicenseText.getVerticalScrollBar().setValue(0);
                    }
                });

                JOptionPane.showMessageDialog(MainView.this, pan_about, "Über", JOptionPane.PLAIN_MESSAGE);
            }
        }

        /**
         * Öffnet ein Diskettenabbild von der Festplatte, bietet dem Benutzer
         * ggf. zusätzliche Einstellungsmöglichkeiten zur Disketten-Geometrie
         *
         * @param drive Nummer des Laufwerks
         */
        private void loadImageFile(int drive) {
            JFileChooser loadDialog = new JFileChooser("./disks/");
            if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File image = loadDialog.getSelectedFile();
                String extension = image.getName().substring(image.getName().length() - 3, image.getName().length()).toLowerCase();
                if (extension.equals("imd")) {
                    a7100.getKES().getAFS().getFloppy(drive).loadDiskFromFile(image, FloppyImageType.IMAGEDISK);
                } else if (extension.equals("td0")) {
                    a7100.getKES().getAFS().getFloppy(drive).loadDiskFromFile(image, FloppyImageType.TELEDISK);
                } else if (extension.equals("dmk")) {
                    a7100.getKES().getAFS().getFloppy(drive).loadDiskFromFile(image, FloppyImageType.DMK);
                } else {
                    // Binär
                    JFormattedTextField editCylinder = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    JFormattedTextField editHeads = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    JFormattedTextField editSectorsPerTrack = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    JFormattedTextField editBytesPerSector = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    JFormattedTextField editSectorsInTrack0 = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    JFormattedTextField editBytesPerSectorTrack0 = new JFormattedTextField(NumberFormat.getIntegerInstance());
                    editCylinder.setValue(80);
                    editHeads.setValue(2);
                    editSectorsPerTrack.setValue(16);
                    editBytesPerSector.setValue(256);
                    editSectorsInTrack0.setValue(16);
                    editBytesPerSectorTrack0.setValue(128);
                    JPanel panelEdit = new JPanel(new GridLayout(6, 2));
                    panelEdit.add(new JLabel("Anzahl der Zylinder:"));
                    panelEdit.add(editCylinder);
                    panelEdit.add(new JLabel("Anzahl der Seiten:"));
                    panelEdit.add(editHeads);
                    panelEdit.add(new JLabel("Sektoren pro Spur:"));
                    panelEdit.add(editSectorsPerTrack);
                    panelEdit.add(new JLabel("Bytes pro Sektor:"));
                    panelEdit.add(editBytesPerSector);
                    panelEdit.add(new JLabel("Sektoren in Spur 0:"));
                    panelEdit.add(editSectorsInTrack0);
                    panelEdit.add(new JLabel("Bytes pro Sektor Spur 0:"));
                    panelEdit.add(editBytesPerSectorTrack0);
                    if (JOptionPane.showConfirmDialog(null, panelEdit, "Image laden", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                        int cylinder = Integer.parseInt(editCylinder.getText());
                        int heads = Integer.parseInt(editHeads.getText());
                        int sectorsPerTrack = Integer.parseInt(editSectorsPerTrack.getText());
                        int bytesPerSector = Integer.parseInt(editBytesPerSector.getText());
                        int sectorsInTrack0 = Integer.parseInt(editSectorsInTrack0.getText());
                        int bytesPerSectorTrack0 = Integer.parseInt(editBytesPerSectorTrack0.getText());
                        a7100.getKES().getAFS().getFloppy(drive).loadDiskFromFile(image, cylinder, heads, sectorsPerTrack, bytesPerSector, sectorsInTrack0, bytesPerSectorTrack0);
                    }

                }
            }

        }
    }
}
