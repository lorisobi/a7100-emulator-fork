/*
 * ABG.java
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
 *   09.08.2014 - Erstellt aus ABG.java
 *   30.09.2014 - Umbenannt in ABG
 *              - Kommentare vervollständigt
 *              - Darstellung Funktionstüchtig
 *              - Laden und Speichern implemetiert
 *   14.12.2014 - Anzeigen und Speichern der Bildwiederholspeicher implementiert   
 *   02.01.2015 - setRGB durch Zugriff auf Bildpuffer ersetzt
 *   03.01.2015 - Blinken bei nicht Cursor ergänzt
 *              - enum CursorMode entfernt
 *   09.08.2015 - Javadoc korrigiert
 *   24.07.2016 - updateClock() in clockUpdate() umbenannt
 */
package a7100emulator.components.modules;

import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.Screen;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Klasse zur Abbildung der ABG (Anschlußsteuerung für grafischen Bildschirm)
 *
 * @author Dirk Bräuer
 */
public final class ABG implements Module {

    /**
     * Port Funktionsregister
     */
    private static final int LOCAL_PORT_FUNCTION_REGISTER = 0x22;
    /**
     * Port Splitregister
     */
    private static final int LOCAL_PORT_SPLIT_REGISTER = 0x23;
    /**
     * Port Adresszähler Low Byte
     */
    private static final int LOCAL_PORT_ADDRESS_COUNTER_LOW = 0x20;
    /**
     * Port Adresszähler High Byte
     */
    private static final int LOCAL_PORT_ADDRESS_COUNTER_HIGH = 0x21;
    /**
     * Port Palettenregister 0
     */
    private static final int LOCAL_PORT_PALETTE_0 = 0x30;
    /**
     * Port Palettenregister 1
     */
    private static final int LOCAL_PORT_PALETTE_1 = 0x31;
    /**
     * Port Palettenregister 2
     */
    private static final int LOCAL_PORT_PALETTE_2 = 0x32;
    /**
     * Port Palettenregister 3
     */
    private static final int LOCAL_PORT_PALETTE_3 = 0x33;
    /**
     * Port Palettenregister 4
     */
    private static final int LOCAL_PORT_PALETTE_4 = 0x34;
    /**
     * Port Palettenregister 5
     */
    private static final int LOCAL_PORT_PALETTE_5 = 0x35;
    /**
     * Port Palettenregister 6
     */
    private static final int LOCAL_PORT_PALETTE_6 = 0x36;
    /**
     * Port Palettenregister 7
     */
    private static final int LOCAL_PORT_PALETTE_7 = 0x37;
    /**
     * Port Palettenregister 8
     */
    private static final int LOCAL_PORT_PALETTE_8 = 0x38;
    /**
     * Port Palettenregister 9
     */
    private static final int LOCAL_PORT_PALETTE_9 = 0x39;
    /**
     * Port Palettenregister A
     */
    private static final int LOCAL_PORT_PALETTE_A = 0x3A;
    /**
     * Port Palettenregister B
     */
    private static final int LOCAL_PORT_PALETTE_B = 0x3B;
    /**
     * Port Palettenregister C
     */
    private static final int LOCAL_PORT_PALETTE_C = 0x3C;
    /**
     * Port Palettenregister D
     */
    private static final int LOCAL_PORT_PALETTE_D = 0x3D;
    /**
     * Port Palettenregister E
     */
    private static final int LOCAL_PORT_PALETTE_E = 0x3E;
    /**
     * Port Palettenregister F
     */
    private static final int LOCAL_PORT_PALETTE_F = 0x3F;
    /**
     * Attribut Blinkende Darstellung
     */
    private static final int ATTRIBUTE_BLINK = 0x02;
    /**
     * Attribut inverse Darstellung
     */
    private static final int ATTRIBUTE_INVERSE = 0x04;
    /**
     * Attribut intensive Darstellung
     */
    private static final int ATTRIBUTE_INTENSE = 0x08;
    /**
     * Attribute Cursor
     */
    private static final int ATTRIBUTE_CURSOR = 0x10;
    /**
     * Farbe dunkles Grün
     */
    private static final int DARK_GREEN = new Color(0, 100, 0).getRGB();
    /**
     * Farbe normales Grün
     */
    private static final int GREEN = new Color(0, 150, 0).getRGB();
    /**
     * Farbe intensives Grün
     */
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    /**
     * Farbe Schwarz
     */
    private static final int BLACK = new Color(0, 0, 0).getRGB();
    /**
     * Aktuell Dargestellter Bildschirm
     */
    private final BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Zähler für Bildschirmupdates
     */
    private int localClock = 0;
    /**
     * Grafischer Bildwiederholspeicher
     */
    private final Memory[] graphicMemory = new Memory[2];
    /**
     * Alphanumerischer Bildiwederholspeicher
     */
    private final Memory[] alphanumericMemory = new Memory[2];
    /**
     * Palettenregister
     */
    private final int[] palette_register = new int[16];
    /**
     * Adresszähler
     */
    private int address_counter = 0;
    /**
     * Funktionsregister
     */
    private int function_register;
    /**
     * Splitregister
     */
    private int split_register;
    /**
     * Verweis auf KGS
     */
    private final KGS kgs;
    /**
     * Gibt an, ob die Fortsetzung der Darstellung an der Splitgrenze erfolgen
     * soll
     */
    private boolean continueSplit = false;

    /**
     * Erstellt eine neue ABG und initialisiert diese.
     *
     * @param kgs Zeiger auf KGS Modul
     */
    public ABG(KGS kgs) {
        this.kgs = kgs;
        init();
    }

    /**
     * Intitialisiert die ABG.
     */
    @Override
    public void init() {
        for (int i = 0; i < 2; i++) {
            graphicMemory[i] = new Memory(0x8000);
            alphanumericMemory[i] = new Memory(0x8000);
        }

        // Löschen des Bildschirmes
        Graphics g = screenImage.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        Screen.getInstance().setImage(screenImage);
    }

    /**
     * Verarbeitet die geänderte Taktzeit der KGS.
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        localClock += amount;
        if (localClock > 160000) {
            localClock = 0;
            updateScreen();
            //kgs.requestNMI();
        }
    }

    /**
     * Schreibt ein Wort in den Bildiwederholspeicher
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @param data Daten
     */
    void writeWord(int msel, int address, int data) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                graphicMemory[0].writeWord(address, data);
                break;
            case 0x03:
                // Alphanumerik 1
                alphanumericMemory[0].writeWord(address, data);
                break;
            case 0x04:
                // Grafik 2
                graphicMemory[1].writeWord(address, data);
                break;
            case 0x05:
                // Grafik 1 + 2
                graphicMemory[0].writeWord(address, data);
                graphicMemory[1].writeWord(address, data);
                break;
            case 0x0C:
                // Alphanumerik 2
                alphanumericMemory[1].writeWord(address, data);
                break;
            case 0x0F:
                // Alphanumerik 1+2
                alphanumericMemory[0].writeWord(address, data);
                alphanumericMemory[1].writeWord(address, data);
                break;
            default:
                System.out.println("Nicht definiertes MSEL Register Schreiben " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
    }

    /**
     * Schreibt ein Byte in den Bildwederholspeicher.
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @param data Daten
     */
    void writeByte(int msel, int address, int data) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                graphicMemory[0].writeByte(address, data);
                break;
            case 0x03:
                // Alphanumerik 1
                alphanumericMemory[0].writeByte(address, data);
                break;
            case 0x04:
                // Grafik 2
                graphicMemory[1].writeByte(address, data);
                break;
            case 0x05:
                // Grafik 1 + 2
                graphicMemory[0].writeByte(address, data);
                graphicMemory[1].writeByte(address, data);
                break;
            case 0x0C:
                // Alphanumerik 2
                alphanumericMemory[1].writeByte(address, data);
                break;
            case 0x0F:
                // Alphanumerik 1+2
                alphanumericMemory[0].writeByte(address, data);
                alphanumericMemory[1].writeByte(address, data);
                break;
            default:
                System.out.println("Nicht definiertes MSEL Register Schreiben " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
    }

    /**
     * Liest ein Wort aus dem Bildwederholspeicher.
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @return Daten
     */
    int readWord(int msel, int address) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                return graphicMemory[0].readWord(address);
            case 0x03:
                // Alphanumerik 1
                return alphanumericMemory[0].readWord(address);
            case 0x04:
                // Grafik 2
                return graphicMemory[1].readWord(address);
            case 0x0C:
                // Alphanumerik 2
                return alphanumericMemory[1].readWord(address);
            default:
                System.out.println("Nicht definiertes MSEL Register Lesen " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
        return 0;
    }

    /**
     * Liest ein Byte aus dem Bildiwederholspeicher.
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @return Daten
     */
    int readByte(int msel, int address) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                return graphicMemory[0].readByte(address);
            case 0x03:
                // Alphanumerik 1
                return alphanumericMemory[0].readByte(address);
            case 0x04:
                // Grafik 2
                return graphicMemory[1].readByte(address);
            case 0x0C:
                // Alphanumerik 2
                return alphanumericMemory[1].readByte(address);
            default:
                System.out.println("Nicht definiertes MSEL Register Lesen " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
        return 0;
    }

    /**
     * Liest Daten von einem lokalen Port
     *
     * @param port Port
     * @return Gelesenes Byte
     */
    public int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_FUNCTION_REGISTER:
                throw new IllegalArgumentException("Lesen Funktionsregister nicht erlaubt!");
            case LOCAL_PORT_SPLIT_REGISTER:
                throw new IllegalArgumentException("Lesen Splitregister nicht erlaubt!");
            case LOCAL_PORT_ADDRESS_COUNTER_LOW:
            case LOCAL_PORT_ADDRESS_COUNTER_HIGH:
                throw new IllegalArgumentException("Lesen Adresszähler nicht erlaubt!");
            case LOCAL_PORT_PALETTE_0:
            case LOCAL_PORT_PALETTE_1:
            case LOCAL_PORT_PALETTE_2:
            case LOCAL_PORT_PALETTE_3:
            case LOCAL_PORT_PALETTE_4:
            case LOCAL_PORT_PALETTE_5:
            case LOCAL_PORT_PALETTE_6:
            case LOCAL_PORT_PALETTE_7:
            case LOCAL_PORT_PALETTE_8:
            case LOCAL_PORT_PALETTE_9:
            case LOCAL_PORT_PALETTE_A:
            case LOCAL_PORT_PALETTE_B:
            case LOCAL_PORT_PALETTE_C:
            case LOCAL_PORT_PALETTE_D:
            case LOCAL_PORT_PALETTE_E:
            case LOCAL_PORT_PALETTE_F:
                throw new IllegalArgumentException("Lesen Palettenregister nicht erlaubt!");
        }
        return 0;
    }

    /**
     * Gibt Daten auf einem lokalen Port aus
     *
     * @param port Port
     * @param data Daten
     */
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_FUNCTION_REGISTER:
                function_register = data & 0xFF;
                //System.out.println("Function Register:" + String.format("%02X", function_register) + " " + Integer.toBinaryString(data));
                break;
            case LOCAL_PORT_SPLIT_REGISTER:
                split_register = data & 0xFF;
//                if (split_register != 0xFF) {
//                    System.out.println("Split Register:" + String.format("%02X", split_register));
//                }
                break;
            case LOCAL_PORT_ADDRESS_COUNTER_LOW:
                address_counter = (address_counter & 0x00FF) | ((data & 0xFF) << 8);
//                System.out.println("address_counter: " + String.format("%04X", address_counter));
                break;
            case LOCAL_PORT_ADDRESS_COUNTER_HIGH:
                address_counter = (address_counter & 0xFF00) | (data & 0xFF);
                break;
            case LOCAL_PORT_PALETTE_0:
            case LOCAL_PORT_PALETTE_1:
            case LOCAL_PORT_PALETTE_2:
            case LOCAL_PORT_PALETTE_3:
            case LOCAL_PORT_PALETTE_4:
            case LOCAL_PORT_PALETTE_5:
            case LOCAL_PORT_PALETTE_6:
            case LOCAL_PORT_PALETTE_7:
            case LOCAL_PORT_PALETTE_8:
            case LOCAL_PORT_PALETTE_9:
            case LOCAL_PORT_PALETTE_A:
            case LOCAL_PORT_PALETTE_B:
            case LOCAL_PORT_PALETTE_C:
            case LOCAL_PORT_PALETTE_D:
            case LOCAL_PORT_PALETTE_E:
            case LOCAL_PORT_PALETTE_F:
                palette_register[port - 0x30] = data & 0xFF;
                System.out.println("Palettenregister noch ohne Funktion!");
                break;
        }
    }

    /**
     * Aktualisiert die Bildschirmanzeige TODO: NMIs richtig implementieren
     * (warten auf KGS)
     */
    private void updateScreen() {
        // Adresszähler
        int address = ~address_counter & 0x7FFF;
        //int address_gr = ~address_counter & 0x7FFF;

        if (split_register == 0xFF) {
            //Wenn Splitregister=0xFF: Volles Alphanumerikbild
            for (int line = 0; line < 400; line++) {
                for (int column = 0; column < 640; column += 8) {
                    updateAlphanumericScreen(address, column, line, screenImage);
                    address = (address - 1) & 0x7FFF;
                }
            }
        } else if (split_register == 0xFE) {
            //Wenn Splitregister=0xFF: Volles Grafikbild
            for (int line = 0; line < 400; line++) {
                for (int column = 0; column < 640; column += 8) {
                    updateGraphicsScreen(address, column, line, screenImage);
                    address = (address - 1) & 0x7FFF;
                }
            }
        } else {
            // Gemischte Darstellung
            // Splitgrenze berechnen
            int splitline = split_register * 2 - 1;

            if (!continueSplit) {
                // Darstellung Grafikbereich
                for (int line = 0; line < splitline - 1; line++) {
                    for (int column = 0; column < 640; column += 8) {
                        updateGraphicsScreen(address, column, line, screenImage);
                        address = (address - 1) & 0x7FFF;
                    }
                }
                darkLine(splitline - 1, screenImage);
            } else {
                // Darstellung Alphanumerikbereich
                darkLine(splitline, screenImage);
                for (int line = splitline + 1; line < 400; line++) {
                    for (int column = 0; column < 640; column += 8) {
                        updateAlphanumericScreen(address, column, line, screenImage);
                        address = (address - 1) & 0x7FFF;
                    }
                }
            }
            continueSplit = !continueSplit;
        }

        kgs.requestNMI();
        Screen.getInstance().repaint();
    }

    /**
     * Aktualisiert den Alphanumerikbildschirm für eine Speicherzelle
     *
     * @param address Speicherzelle
     * @param column Spalte auf Bildschirm
     * @param line Zeile auf Bildschirm
     * @param image Zeiger auf Bild
     */
    private void updateAlphanumericScreen(int address, int column, int line, BufferedImage image) {
        int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        int data = alphanumericMemory[0].readByte(address);

        int attribute = alphanumericMemory[1].readByte(address);
        boolean intense = (attribute & ATTRIBUTE_INTENSE) != 0;
        boolean inverse = (attribute & ATTRIBUTE_INVERSE) != 0;
        boolean blink = ((attribute & ATTRIBUTE_BLINK) != 0);
        boolean cursor = (attribute & ATTRIBUTE_CURSOR) != 0;

        boolean blink_fnct = BitTest.getBit(function_register, 3);

        for (int pixel = 0; pixel < 8; pixel++) {
            boolean set = BitTest.getBit(data, pixel);
            if (cursor) {
                if (blink_fnct) {
                    imageData[column + 7 - pixel + line * 640] = intense ? INTENSE_GREEN : GREEN;
                } else {
                    imageData[column + 7 - pixel + line * 640] = BLACK;
                }
            } else {
                if ((set ^ inverse) && (!blink || (blink && blink_fnct))) {
                    imageData[column + 7 - pixel + line * 640] = intense ? INTENSE_GREEN : GREEN;
                } else {
                    imageData[column + 7 - pixel + line * 640] = BLACK;
                }
            }
        }
    }

    /**
     * Aktualisiert den Grafikbildschirm für eine Speicherzelle
     *
     * @param address Speicherzelle
     * @param column Spalte auf Bildschirm
     * @param line Zeile auf Bildschirm
     * @param image Zeiger auf Bild
     */
    private void updateGraphicsScreen(int address, int column, int line, BufferedImage image) {
        int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        int data1 = graphicMemory[0].readByte(address);
        int data2 = graphicMemory[1].readByte(address);
        for (int pixel = 0; pixel < 8; pixel++) {
            boolean b1 = BitTest.getBit(data1, pixel);
            boolean b2 = BitTest.getBit(data2, pixel);
            if (b1 && !b2) {
                imageData[column + 7 - pixel + line * 640] = DARK_GREEN;
            } else if (!b1 && b2) {
                imageData[column + 7 - pixel + line * 640] = GREEN;
            } else if (b1 && b2) {
                imageData[column + 7 - pixel + line * 640] = INTENSE_GREEN;
            } else {
                imageData[column + 7 - pixel + line * 640] = BLACK;
            }
        }
    }

    /**
     * Tastet eine Linie dunkel
     *
     * @param line Zeile auf Bildschirm
     * @param image Zeiger auf Bild
     */
    private void darkLine(int line, BufferedImage image) {
        int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int pixel = 0; pixel < 640; pixel++) {
            imageData[pixel + line * 640] = BLACK;
        }
    }

    /**
     * Zeigt den Inhalt des Alphanumerikspeichers an
     */
    public void showAlphanumericScreen() {
        final BufferedImage alphanumericImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);

        int address_an = ~address_counter & 0x7FFF;

        for (int line = 0; line < 400; line++) {
            for (int column = 0; column < 640; column += 8) {
                updateAlphanumericScreen(address_an, column, line, alphanumericImage);
                address_an = (address_an - 1) & 0x7FFF;
            }
        }

        JFrame frame = new JFrame("Alphanumerik-Bildschirm");
        frame.setResizable(false);
        JComponent component = new JComponent() {

            @Override
            public void paint(Graphics g) {
                g.drawImage(alphanumericImage, 0, 0, null);
            }
        };
        component.setMinimumSize(new Dimension(640, 400));
        component.setPreferredSize(new Dimension(640, 400));
        frame.add(component);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(ABG.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.setVisible(true);
        frame.pack();
    }

    /**
     * Zeigt den Inhalt des Grafikspeichers an
     */
    public void showGraphicScreen() {
        final BufferedImage graphicImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);

        int address_gr = 0x7FFF;

        for (int line = 0; line < 400; line++) {
            for (int column = 0; column < 640; column += 8) {
                updateGraphicsScreen(address_gr, column, line, graphicImage);
                address_gr = (address_gr - 1) & 0x7FFF;
            }
        }

        JFrame frame = new JFrame("Grafik-Bildschirm");
        frame.setResizable(false);
        JComponent component = new JComponent() {

            @Override
            public void paint(Graphics g) {
                g.drawImage(graphicImage, 0, 0, null);
            }
        };
        component.setMinimumSize(new Dimension(640, 400));
        component.setPreferredSize(new Dimension(640, 400));
        frame.add(component);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(ABG.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.setVisible(true);
        frame.pack();
    }

    /**
     * Zeigt den Inhalt des ABG-Speichers
     *
     * @param page Speicherebene
     */
    public void showMemory(int page) {
        switch (page) {
            case 0:
                (new MemoryAnalyzer(alphanumericMemory[0], "Alpahnumerikspeicher Ebene 1")).show();
                break;
            case 1:
                (new MemoryAnalyzer(alphanumericMemory[1], "Alpahnumerikspeicher Ebene 2")).show();
                break;
            case 2:
                (new MemoryAnalyzer(graphicMemory[0], "Grafikspeicher Ebene 1")).show();
                break;
            case 3:
                (new MemoryAnalyzer(graphicMemory[1], "Grafikspeicher Ebene 2")).show();
                break;
        }
    }

    /**
     * Schreibt den Inhalt des ABG-Speichers in eine Datei
     *
     * @param filename Dateiname
     * @param page Speicherebene
     */
    public void dumpMemory(String filename, int page) {
        DataOutputStream dos;
        try {
            dos = new DataOutputStream(new FileOutputStream(filename));
            switch (page) {
                case 0:
                    alphanumericMemory[0].saveMemory(dos);
                    break;
                case 1:
                    alphanumericMemory[1].saveMemory(dos);
                    break;
                case 2:
                    graphicMemory[0].saveMemory(dos);
                    break;
                case 3:
                    graphicMemory[1].saveMemory(dos);
                    break;
            }
            dos.close();
        } catch (IOException ex) {
            Logger.getLogger(ABG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Speichert den Zustand der ABG in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn das Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(localClock);
        for (int i = 0; i < 2; i++) {
            graphicMemory[i].saveMemory(dos);
            alphanumericMemory[i].saveMemory(dos);
        }
        for (int i = 0; i < 16; i++) {
            dos.writeInt(palette_register[i]);
        }
        dos.writeInt(address_counter);
        dos.writeInt(function_register);
        dos.writeInt(split_register);
    }

    /**
     * Lädt den Zustand der ABG aus einer Datei
     *
     * @param dis Stream der Datei
     * @throws IOException Wenn das Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        localClock = dis.readInt();
        for (int i = 0; i < 2; i++) {
            graphicMemory[i].loadMemory(dis);
            alphanumericMemory[i].loadMemory(dis);
        }
        for (int i = 0; i < 16; i++) {
            palette_register[i] = dis.readInt();
        }
        address_counter = dis.readInt();
        function_register = dis.readInt();
        split_register = dis.readInt();
    }
}
