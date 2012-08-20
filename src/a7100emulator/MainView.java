/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator;

import a7100emulator.Debug.Debugger;
import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.Screen;
import a7100emulator.components.system.SystemMemory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 *
 * @author Dirk
 */
public class MainView extends JFrame {

    private final JMenu menuEmulator = new JMenu("Emulator");
    private final JMenu menuDebug = new JMenu("Debug");
    private final JMenuItem menuEmulatorReset = new JMenuItem("Reset");
    private final JMenuItem menuEmulatorPause = new JMenuItem("Pause");
    private final JMenuItem menuEmulatorExit = new JMenuItem("Beenden");
    private final JMenu menuDebugMemory = new JMenu("Speicher");
    private final JMenu menuDebugDecoder = new JMenu("Decoder");
    private final JCheckBoxMenuItem menuDebugSwitch = new JCheckBoxMenuItem("Debugger");
    private final JMenuItem menuDebugMemoryShow = new JMenuItem("zeigen");
    private final JMenuItem menuDebugMemoryDump = new JMenuItem("Dump");
    private final JMenuItem menuDebugDecoderShow = new JMenuItem("zeigen");
    private final JMenuItem menuDebugDecoderDump = new JMenuItem("Dump");
    private MainMenuController controller = new MainMenuController();

    public MainView() {
        super("A7100 Emulator");
        JMenuBar menubar = new JMenuBar();
        menubar.add(menuEmulator);
        menuEmulator.add(menuEmulatorReset);
        menuEmulator.add(menuEmulatorPause);
        menuEmulator.addSeparator();
        menuEmulator.add(menuEmulatorExit);

        menubar.add(menuDebug);
        menuDebug.add(menuDebugMemory);
        menuDebugMemory.add(menuDebugMemoryShow);
        menuDebugMemory.add(menuDebugMemoryDump);
        menuDebug.add(menuDebugDecoder);
        menuDebugDecoder.add(menuDebugDecoderShow);
        menuDebugDecoder.add(menuDebugDecoderDump);
        menuDebug.add(menuDebugSwitch);

        menuDebugSwitch.addActionListener(controller);
        menuDebugMemoryShow.addActionListener(controller);
        menuDebugMemoryDump.addActionListener(controller);
        menuDebugDecoderShow.addActionListener(controller);
        menuDebugDecoderDump.addActionListener(controller);

        this.setJMenuBar(menubar);
        this.add(Screen.getInstance());
        this.addKeyListener(Keyboard.getInstance());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
        this.pack();
    }

    private class MainMenuController implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(menuDebugMemoryShow)) {
                (new MemoryAnalyzer()).show();
            } else if (e.getSource() == menuDebugDecoderShow) {
                Decoder.getInstance().show();
            } else if (e.getSource() == menuDebugMemoryDump) {
                SystemMemory.getInstance().dump();
            } else if (e.getSource() == menuDebugDecoderDump) {
                Decoder.getInstance().save();
            } else if (e.getSource()==menuDebugSwitch) {
                Debugger.getInstance().setDebug(menuDebugSwitch.isSelected());
            }
        }
    }
}
