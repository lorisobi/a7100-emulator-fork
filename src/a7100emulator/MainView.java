/*
 * MainView.java
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
 *   12.04.2014 - Laden von verschiedenen Image-Typen implementiert
 *   31.08.2014 - Debugger deaktiviert, da Singleton Instanz entfernt
 *   04.12.2014 - Dump KGS Speicher hinzugefügt
 *              - Menüeinträge umbenannt
 *   12.12.2014 - Menü Debug neu strukturiert
 *              - Zeige KGS Speicher hinzugefügt
 *   14.12.2014 - Zeigen/Speichern ABG Speicher
 *   17.12.2014 - Hacks hinzugefügt
 *   19.12.2014 - Datum für Screenshots auf 24h Anzeige umgestellt
 *   03.01.2015 - Menü Parity Hack entfernt
 *   06.01.2015 - Menü Globaler Debugger und KGS Debugger hinzugefügt
 *   24.07.2015 - Tabulator für GUI Events entfernt
 *   25.07.2015 - Untermenü KES ausgeblendet
 *   26.07.2015 - Lizenzinformationen überarbeitet
 *   29.07.2015 - Formatierte Textfelder bei RAW Image richtig auslesen
 *   14.08.2015 - Lesen von CopyQM Images ergänzt
 *   16.08.2015 - Dialog für Floppy Format nach FloppyImageParser ausgelagert
 *   14.02.2016 - Menüpunkte KES hinzugefügt
 *   15.03.2016 - Menüpunkt KES Speicher anzeigen, speichern hinzugefügt 
 *   26.03.2016 - Schreibfehler korrigiert
 */
package a7100emulator;

import a7100emulator.Apps.SCPDiskViewer.SCPDiskModel;
import a7100emulator.Apps.SCPDiskViewer.SCPDiskViewer;
import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Debug.OpcodeStatistic;
import a7100emulator.components.A7100;
import a7100emulator.components.ic.KR580WM51A;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.Screen;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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
     * Untermenü Debug - System
     */
    private final JMenu menuDebugSystem = new JMenu("System");
    /**
     * Untermenü Debug - ZVE
     */
    private final JMenu menuDebugZVE = new JMenu("ZVE");
    /**
     * Untermenü Debug - KGS
     */
    private final JMenu menuDebugKGS = new JMenu("KGS");
    /**
     * Untermenü Debug - KES
     */
    private final JMenu menuDebugKES = new JMenu("KES");
    /**
     * Untermenü Debug - ABG
     */
    private final JMenu menuDebugABG = new JMenu("ABG");
    /**
     * Menüeintrag Globaler Debugger
     */
    private final JCheckBoxMenuItem menuDebugGlobalDebuggerSwitch = new JCheckBoxMenuItem("Globaler Debugger");
    /**
     * Menüeintrag Speicher anzeigen
     */
    private final JMenuItem menuDebugSystemMemoryShow = new JMenuItem("Zeige Systemspeicher");
    /**
     * Menüeintrag Speicher in Datei Schreiben
     */
    private final JMenuItem menuDebugSystemMemoryDump = new JMenuItem("Dump Systemspeicher");
    /**
     * Menüeintrag Debugger aktivieren
     */
    private final JCheckBoxMenuItem menuDebugZVEDebuggerSwitch = new JCheckBoxMenuItem("Debugger");
    /**
     * Menüeintrag Zeige KGS Speicher
     */
    private final JMenuItem menuDebugKGSMemoryShow = new JMenuItem("Zeige KGS Speicher");
    /**
     * Menüeintrag Speicher in Datei Schreiben
     */
    private final JMenuItem menuDebugKGSMemoryDump = new JMenuItem("Dump KGS Speicher");
    /**
     * Menüeintrag Debugger aktivieren
     */
    private final JCheckBoxMenuItem menuDebugKGSDebuggerSwitch = new JCheckBoxMenuItem("Debugger");
    /**
     * Menüeintrag Decoder anzeigen
     */
    private final JMenuItem menuDebugZVEDecoderShow = new JMenuItem("Zeige Decoder");
    /**
     * Menüeintrag Decoderinformationen speichern
     */
    private final JMenuItem menuDebugZVEDecoderDump = new JMenuItem("Dump Decoder");
    /**
     * Menüeintrag Zeichensatz anzeigen
     */
    private final JMenuItem menuDebugKGSCharacters = new JMenuItem("KGS Zeichensatz");
    /**
     * Menüeintrag Opcode-Statistik speichern
     */
    private final JMenuItem menuDebugZVEOpcodeStatistic = new JMenuItem("Dump Opcode Statistik");
    /**
     * Menüeintrag Debugger aktivieren
     * <p>
     * TODO: Weitere KES Debug Funktionen
     */
    private final JCheckBoxMenuItem menuDebugKESDebuggerSwitch = new JCheckBoxMenuItem("Debugger");
    /**
     * Menüeintrag Zeige KGS Speicher
     */
    private final JMenuItem menuDebugKESMemoryShow = new JMenuItem("Zeige KES Speicher");
    /**
     * Menüeintrag KES Speicher in Datei schreiben
     */
    private final JMenuItem menuDebugKESMemoryDump = new JMenuItem("Dump KES Speicher");

    /**
     * Menüeintrag ABG Zeige Alphanumerik
     */
    private final JMenuItem menuDebugABGAlphanumerics = new JMenuItem("Alphanumerikbild zeigen");
    /**
     * Menüeintrag ABG Zeige Grafikbild
     */
    private final JMenuItem menuDebugABGGraphics = new JMenuItem("Grafikbild zeigen");
    /**
     * Menüeintrag ABG Zeige Alphanumerikspeicher Ebene 1
     */
    private final JMenuItem menuDebugABGAlphanumericsPage1 = new JMenuItem("Alphanumerikspeicher Ebene 1 zeigen");
    /**
     * Menüeintrag ABG Zeige Alphanumerikspeicher Ebene 2
     */
    private final JMenuItem menuDebugABGAlphanumericsPage2 = new JMenuItem("Alphanumerikspeicher Ebene 2 zeigen");
    /**
     * Menüeintrag ABG Zeige Grafikspeicher Ebene 1
     */
    private final JMenuItem menuDebugABGGraphicsPage1 = new JMenuItem("Grafikspeicher Ebene 1 zeigen");
    /**
     * Menüeintrag ABG Zeige Graphikspeicher Ebene 2
     */
    private final JMenuItem menuDebugABGGraphicsPage2 = new JMenuItem("Grafikspeicher Ebene 2 zeigen");
    /**
     * Menüeintrag ABG Zeige Alphanumerikspeicher Ebene 1
     */
    private final JMenuItem menuDebugABGDumpAlphanumericsPage1 = new JMenuItem("Dump Alphanumerikspeicher Ebene 1");
    /**
     * Menüeintrag ABG Zeige Alphanumerikspeicher Ebene 2
     */
    private final JMenuItem menuDebugABGDumpAlphanumericsPage2 = new JMenuItem("Dump Alphanumerikspeicher Ebene 2");
    /**
     * Menüeintrag ABG Zeige Grafikspeicher Ebene 1
     */
    private final JMenuItem menuDebugABGDumpGraphicsPage1 = new JMenuItem("Dump Grafikspeicher Ebene 1");
    /**
     * Menüeintrag ABG Zeige Graphikspeicher Ebene 2
     */
    private final JMenuItem menuDebugABGDumpGraphicsPage2 = new JMenuItem("Dump Grafikspeicher Ebene 2");
    /**
     * Menü Tools
     */
    private final JMenu menuTools = new JMenu("Tools");
    /**
     * Menüeintrag SCP-Disk Betrachter starten
     */
    private final JMenuItem menuToolsSCPDiskTool = new JMenuItem("SCP-Disk Tool");
    /**
     * Menüeintrag Screenshot speichern
     */
    private final JMenuItem menuToolsScreenshot = new JMenuItem("Bildschirmfoto");
    /**
     * Menü Emulator Hacks TODO: Hacks wenn möglich entfernen
     */
    private final JMenu menuHacks = new JMenu("Hacks");
    /**
     * Menüeintrag Emulator Hacks - Deaktiviere Tastatur Reset TODO: Hacks wenn
     * möglich entfernen
     */
    private final JCheckBoxMenuItem menuHacksKeyboardReset = new JCheckBoxMenuItem("KR580WM51A Tastaturreset deaktivieren", false);
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
    //private final JLabel statusBar = new JLabel("Status");
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
        menuDebug.add(menuDebugSystem);
        menuDebugSystem.add(menuDebugGlobalDebuggerSwitch);
        menuDebugSystem.add(menuDebugSystemMemoryShow);
        menuDebugSystem.add(menuDebugSystemMemoryDump);
        menuDebug.add(menuDebugZVE);
        menuDebugZVE.add(menuDebugZVEDebuggerSwitch);
        menuDebugZVE.add(menuDebugZVEDecoderShow);
        menuDebugZVE.add(menuDebugZVEDecoderDump);
        menuDebugZVE.add(menuDebugZVEOpcodeStatistic);
        menuDebug.add(menuDebugKGS);
        menuDebugKGS.add(menuDebugKGSDebuggerSwitch);
        menuDebugKGS.add(menuDebugKGSMemoryShow);
        menuDebugKGS.add(menuDebugKGSMemoryDump);
        menuDebugKGS.add(menuDebugKGSCharacters);
        menuDebug.add(menuDebugKES);
        menuDebugKES.add(menuDebugKESDebuggerSwitch);
        menuDebugKES.add(menuDebugKESMemoryShow);
        menuDebugKES.add(menuDebugKESMemoryDump);
        menuDebug.add(menuDebugABG);
        menuDebugABG.add(menuDebugABGAlphanumerics);
        menuDebugABG.add(menuDebugABGGraphics);
        menuDebugABG.add(menuDebugABGAlphanumericsPage1);
        menuDebugABG.add(menuDebugABGAlphanumericsPage2);
        menuDebugABG.add(menuDebugABGGraphicsPage1);
        menuDebugABG.add(menuDebugABGGraphicsPage2);
        menuDebugABG.add(menuDebugABGDumpAlphanumericsPage1);
        menuDebugABG.add(menuDebugABGDumpAlphanumericsPage2);
        menuDebugABG.add(menuDebugABGDumpGraphicsPage1);
        menuDebugABG.add(menuDebugABGDumpGraphicsPage2);

        menuDebugGlobalDebuggerSwitch.addActionListener(controller);
        menuDebugSystemMemoryShow.addActionListener(controller);
        menuDebugSystemMemoryDump.addActionListener(controller);
        menuDebugZVEDebuggerSwitch.addActionListener(controller);
        menuDebugZVEDecoderShow.addActionListener(controller);
        menuDebugZVEDecoderDump.addActionListener(controller);
        menuDebugZVEOpcodeStatistic.addActionListener(controller);
        menuDebugKGSDebuggerSwitch.addActionListener(controller);
        menuDebugKGSMemoryShow.addActionListener(controller);
        menuDebugKGSMemoryDump.addActionListener(controller);
        menuDebugKGSCharacters.addActionListener(controller);
        menuDebugKESDebuggerSwitch.addActionListener(controller);
        menuDebugKESMemoryShow.addActionListener(controller);
        menuDebugKESMemoryDump.addActionListener(controller);
        menuDebugABGAlphanumerics.addActionListener(controller);
        menuDebugABGGraphics.addActionListener(controller);
        menuDebugABGAlphanumericsPage1.addActionListener(controller);
        menuDebugABGAlphanumericsPage2.addActionListener(controller);
        menuDebugABGGraphicsPage1.addActionListener(controller);
        menuDebugABGGraphicsPage2.addActionListener(controller);
        menuDebugABGDumpAlphanumericsPage1.addActionListener(controller);
        menuDebugABGDumpAlphanumericsPage2.addActionListener(controller);
        menuDebugABGDumpGraphicsPage1.addActionListener(controller);
        menuDebugABGDumpGraphicsPage2.addActionListener(controller);

        menubar.add(menuTools);
        menuTools.add(menuToolsSCPDiskTool);
        menuTools.add(menuToolsScreenshot);

        menuToolsSCPDiskTool.addActionListener(controller);
        menuToolsScreenshot.addActionListener(controller);

        menubar.add(menuHacks);
        menuHacks.add(menuHacksKeyboardReset);

        menuHacksKeyboardReset.addActionListener(controller);

        menubar.add(menuHelp);
        menuHelp.add(menuHelpAbout);

        menuHelpAbout.addActionListener(controller);

        this.setJMenuBar(menubar);
        this.add(Screen.getInstance(), BorderLayout.CENTER);
        //this.add(statusBar,BorderLayout.SOUTH);
        this.setFocusTraversalKeysEnabled(false);
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
                    a7100.pause();
                } else {
                    a7100.resume();
                }
                menuEmulatorSingle.setEnabled(menuEmulatorPause.isSelected());
            } else if (e.getSource().equals(menuEmulatorSingle)) {
                a7100.singleStep();
            } else if (e.getSource().equals(menuEmulatorSave)) {
                a7100.saveState();
            } else if (e.getSource().equals(menuEmulatorLoad)) {
                a7100.loadState();
                menuHacksKeyboardReset.setSelected(KR580WM51A.isKeyboardResetHack());
            } else if (e.getSource() == menuEmulatorExit) {
                System.exit(0);
            } else if (e.getSource().equals(menuDebugGlobalDebuggerSwitch)) {
                Debugger.getGlobalInstance().setDebug(menuDebugGlobalDebuggerSwitch.isSelected());
            } else if (e.getSource().equals(menuDebugSystemMemoryShow)) {
                (new MemoryAnalyzer()).show();
            } else if (e.getSource() == menuDebugZVEDecoderShow) {
                Decoder.getInstance().show();
            } else if (e.getSource() == menuDebugSystemMemoryDump) {
                MMS16Bus.getInstance().dumpSystemMemory("./debug/system_user_dump.hex");
            } else if (e.getSource() == menuDebugKGSMemoryShow) {
                a7100.getKGS().showMemory();
            } else if (e.getSource() == menuDebugKGSMemoryDump) {
                a7100.getKGS().dumpLocalMemory("./debug/kgs_user_dump.hex");
            } else if (e.getSource().equals(menuDebugKGSDebuggerSwitch)) {
                a7100.getKGS().setDebug(menuDebugKGSDebuggerSwitch.isSelected());
            } else if (e.getSource().equals(menuDebugKESDebuggerSwitch)) {
                a7100.getKES().setDebug(menuDebugKESDebuggerSwitch.isSelected());
            } else if (e.getSource() == menuDebugKESMemoryShow) {
                a7100.getKES().showMemory();
            } else if (e.getSource() == menuDebugKESMemoryDump) {
                a7100.getKES().dumpLocalMemory("./debug/kes_user_dump.hex");
            } else if (e.getSource() == menuDebugZVEDecoderDump) {
                Decoder.getInstance().save();
            } else if (e.getSource() == menuDebugZVEDebuggerSwitch) {
                boolean debug = menuDebugZVEDebuggerSwitch.isSelected();
                a7100.getZVE().setDebug(menuDebugZVEDebuggerSwitch.isSelected());
                if (debug) {
                    Decoder.getInstance().clear();
                }
            } else if (e.getSource() == menuDebugKGSCharacters) {
                a7100.getKGS().showCharacters();
            } else if (e.getSource() == menuDebugZVEOpcodeStatistic) {
                OpcodeStatistic.getInstance().dump();
            } else if (e.getSource() == menuDebugABGAlphanumerics) {
                a7100.getABG().showAlphanumericScreen();
            } else if (e.getSource() == menuDebugABGGraphics) {
                a7100.getABG().showGraphicScreen();
            } else if (e.getSource() == menuDebugABGAlphanumericsPage1) {
                a7100.getABG().showMemory(0);
            } else if (e.getSource() == menuDebugABGAlphanumericsPage2) {
                a7100.getABG().showMemory(1);
            } else if (e.getSource() == menuDebugABGGraphicsPage1) {
                a7100.getABG().showMemory(2);
            } else if (e.getSource() == menuDebugABGGraphicsPage2) {
                a7100.getABG().showMemory(3);
            } else if (e.getSource() == menuDebugABGDumpAlphanumericsPage1) {
                a7100.getABG().dumpMemory("./debug/abg_an1_user_dump.hex", 0);
            } else if (e.getSource() == menuDebugABGDumpAlphanumericsPage2) {
                a7100.getABG().dumpMemory("./debug/abg_an2_user_dump.hex", 1);
            } else if (e.getSource() == menuDebugABGDumpGraphicsPage1) {
                a7100.getABG().dumpMemory("./debug/abg_gr1_user_dump.hex", 2);
            } else if (e.getSource() == menuDebugABGDumpGraphicsPage2) {
                a7100.getABG().dumpMemory("./debug/abg_gr2_user_dump.hex", 3);
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
            } else if (e.getSource() == menuToolsSCPDiskTool) {
                SCPDiskModel model = new SCPDiskModel();
                SCPDiskViewer view = new SCPDiskViewer(model);
                model.setView(view);
            } else if (e.getSource() == menuToolsScreenshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
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
            } else if (e.getSource() == menuHacksKeyboardReset) {
                KR580WM51A.setKeyboardResetHack(menuHacksKeyboardReset.isSelected());
            } else if (e.getSource() == menuHelpAbout) {
                JPanel pan_about = new JPanel();
                pan_about.setLayout(new BorderLayout());
                pan_about.add(new JLabel(new ImageIcon(this.getClass().getClassLoader().getResource("Images/Icon.png"))), BorderLayout.WEST);
                JPanel pan_desc = new JPanel();
                pan_desc.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 10));
                pan_desc.setLayout(new GridLayout(2, 1));
                pan_desc.add(new JLabel("A7100 - Emulator v0.8.45"));
                pan_desc.add(new JLabel("Copyright (c) 2011-2016 Dirk Bräuer"));
                pan_about.add(pan_desc, BorderLayout.CENTER);
                JTextArea licenseText = new JTextArea();
                licenseText.setText("Lizenzinformationen:\n\n"
                        + "Der A7100 Emulator ist Freie Software: Sie können ihn "
                        + "unter den Bedingungen der GNU General Public License, "
                        + "wie von der Free Software Foundation, Version 3 "
                        + "der Lizenz oder jeder späteren veröffentlichten Version, "
                        + "weiterverbreiten und/oder modifizieren.\n\n"
                        + "Der A7100 Emulator wird in der Hoffnung, dass er nützlich "
                        + "sein wird, aber OHNE JEDE GEWÄHRLEISTUNG,bereitgestellt; "
                        + "sogar ohne die implizite Gewährleistung der MARKTFÄHIGKEIT "
                        + "oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK. Siehe die "
                        + "GNU General Public License für weitere Details\n\n"
                        + "Sie sollten eine Kopie der GNU General Public License "
                        + "zusammen mit diesem Programm erhalten haben. Wenn nicht, "
                        + "siehe <http://www.gnu.org/licenses/>.\n\n"
                        + "Hinweis: Die für den Emulator benötigten EPROM- und "
                        + "Diskettenabbilder unterliegen ggf. weiteren "
                        + "Lizenzbestimmungen. Der Anwender verpflichtet sich "
                        + "diese bei der Verwendung der Software einzuhalten."
                );
                licenseText.setEditable(false);
                licenseText.setLineWrap(true);
                licenseText.setWrapStyleWord(true);
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
                a7100.getKES().getAFS().getFloppy(drive).loadDiskFromFile(image);
            }
        }
    }
}
