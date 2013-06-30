/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator;

import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Debug.OpcodeStatistic;
import a7100emulator.components.A7100;
import a7100emulator.components.system.FloppyDrive;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.Screen;
import a7100emulator.components.system.SystemMemory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;

/**
 *
 * @author Dirk
 */
public class MainView extends JFrame {

    private final JMenu menuEmulator = new JMenu("Emulator");
    private final JMenu menuDevices = new JMenu("Geräte");
    private final JMenu menuDebug = new JMenu("Debug");
    private final JMenuItem menuEmulatorReset = new JMenuItem("Reset");
    private final JCheckBoxMenuItem menuEmulatorPause = new JCheckBoxMenuItem("Pause");
    private final JMenuItem menuEmulatorSingle = new JMenuItem("Einzelschritt");
    private final JMenuItem menuEmulatorSave = new JMenuItem("Zustand Speichern");
    private final JMenuItem menuEmulatorLoad = new JMenuItem("Zustand Laden");
    private final JMenuItem menuEmulatorExit = new JMenuItem("Beenden");
    private final JMenu menuDevicesDrive0 = new JMenu("Laufwerk 0");
    private final JMenu menuDevicesDrive1 = new JMenu("Laufwerk 1");
    private final JMenuItem menuDevicesDrive0Load = new JMenuItem("Lade Image");
    private final JMenuItem menuDevicesDrive0Save = new JMenuItem("Speicher Image");
    private final JMenuItem menuDevicesDrive0Eject = new JMenuItem("Auswerfen");
    private final JMenuItem menuDevicesDrive0Empty = new JMenuItem("Leere Diskette");
    private final JCheckBoxMenuItem menuDevicesDrive0WriteProtect = new JCheckBoxMenuItem("Schreibschutz");
    private final JMenuItem menuDevicesDrive1Load = new JMenuItem("Lade Image");
    private final JMenuItem menuDevicesDrive1Save = new JMenuItem("Speicher Image");
    private final JMenuItem menuDevicesDrive1Eject = new JMenuItem("Auswerfen");
    private final JMenuItem menuDevicesDrive1Empty = new JMenuItem("Leere Diskette");
    private final JCheckBoxMenuItem menuDevicesDrive1WriteProtect = new JCheckBoxMenuItem("Schreibschutz");
    private final JMenu menuDebugMemory = new JMenu("Speicher");
    private final JMenu menuDebugDecoder = new JMenu("Decoder");
    private final JCheckBoxMenuItem menuDebugSwitch = new JCheckBoxMenuItem("Debugger");
    private final JMenuItem menuDebugMemoryShow = new JMenuItem("zeigen");
    private final JMenuItem menuDebugMemoryDump = new JMenuItem("Dump");
    private final JMenuItem menuDebugDecoderShow = new JMenuItem("zeigen");
    private final JMenuItem menuDebugDecoderDump = new JMenuItem("Dump");
    private final JMenuItem menuDebugCharacters = new JMenuItem("KGS Zeichensatz");
    private final JMenuItem menuOpcodeStatistic = new JMenuItem("Dump Statistik");
    private MainMenuController controller = new MainMenuController();
    private final A7100 a7100;

    public MainView(A7100 a7100) {
        super("A7100 Emulator");

        this.a7100 = a7100;
        JMenuBar menubar = new JMenuBar();
        menubar.add(menuEmulator);
        menuEmulator.add(menuEmulatorReset);
        menuEmulator.add(menuEmulatorPause);
        menuEmulator.add(menuEmulatorSingle);
        menuEmulator.addSeparator();
        menuEmulator.add(menuEmulatorSave);
        menuEmulator.add(menuEmulatorLoad);
        menuEmulator.addSeparator();
        menuEmulator.add(menuEmulatorExit);

        menuEmulatorPause.addActionListener(controller);
        menuEmulatorSingle.addActionListener(controller);
        menuEmulatorSave.addActionListener(controller);
        menuEmulatorLoad.addActionListener(controller);

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
        menuDebug.add(menuDebugSwitch);
        menuDebug.add(menuDebugCharacters);
        menuDebug.add(menuOpcodeStatistic);

        menuDebugSwitch.addActionListener(controller);
        menuDebugMemoryShow.addActionListener(controller);
        menuDebugMemoryDump.addActionListener(controller);
        menuDebugDecoderShow.addActionListener(controller);
        menuDebugDecoderDump.addActionListener(controller);
        menuDebugCharacters.addActionListener(controller);
        menuOpcodeStatistic.addActionListener(controller);

        this.setJMenuBar(menubar);
        this.add(Screen.getInstance());
        this.addKeyListener(Keyboard.getInstance());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setVisible(true);
        this.pack();
    }

    private class MainMenuController implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(menuEmulatorPause)) {
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
            } else if (e.getSource().equals(menuDebugMemoryShow)) {
                (new MemoryAnalyzer()).show();
            } else if (e.getSource() == menuDebugDecoderShow) {
                Decoder.getInstance().show();
            } else if (e.getSource() == menuDebugMemoryDump) {
                SystemMemory.getInstance().dump("user_dump.hex");
            } else if (e.getSource() == menuDebugDecoderDump) {
                Decoder.getInstance().save();
            } else if (e.getSource() == menuDebugSwitch) {
                Debugger.getInstance().setDebug(menuDebugSwitch.isSelected());
            } else if (e.getSource() == menuDebugCharacters) {
                a7100.getKGS().showCharacters();
            } else if (e.getSource() == menuOpcodeStatistic) {
                OpcodeStatistic.getInstance().dump();
            } else if (e.getSource() == menuDevicesDrive0Load) {
                JFileChooser loadDialog = new JFileChooser("./disks/");
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = loadDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(0).loadDisk(image);
                }
            } else if (e.getSource() == menuDevicesDrive0Save) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = saveDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(0).saveDisk(image);
                }
            } else if (e.getSource() == menuDevicesDrive0Eject) {
                a7100.getKES().getAFS().getFloppy(0).ejectDisk();
            } else if (e.getSource() == menuDevicesDrive0Empty) {
                a7100.getKES().getAFS().getFloppy(0).newDisk();
            } else if (e.getSource() == menuDevicesDrive0WriteProtect) {
                a7100.getKES().getAFS().getFloppy(0).setWriteProtect(menuDevicesDrive0WriteProtect.isSelected());
            } else if (e.getSource() == menuDevicesDrive1Load) {
                JFileChooser loadDialog = new JFileChooser("./disks/");
                if (loadDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = loadDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(1).loadDisk(image);
                }
            } else if (e.getSource() == menuDevicesDrive1Save) {
                JFileChooser saveDialog = new JFileChooser("./disks/");
                if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File image = saveDialog.getSelectedFile();
                    a7100.getKES().getAFS().getFloppy(1).saveDisk(image);
                }
            } else if (e.getSource() == menuDevicesDrive1Eject) {
                a7100.getKES().getAFS().getFloppy(1).ejectDisk();
            } else if (e.getSource() == menuDevicesDrive1Empty) {
                a7100.getKES().getAFS().getFloppy(1).newDisk();
            } else if (e.getSource() == menuDevicesDrive1WriteProtect) {
                a7100.getKES().getAFS().getFloppy(1).setWriteProtect(menuDevicesDrive0WriteProtect.isSelected());
            }
        }
    }
}
