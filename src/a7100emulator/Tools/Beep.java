/*
 * Beep.java
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
 *   26.07.2016 - Spezifische Exceptions definiert
 *   09.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 */
package a7100emulator.Tools;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;

/**
 * Klasse zur Realisierung des Tongebers
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class Beep {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(Beep.class.getName());

    /**
     * Abzuspielendes Sample
     */
    private Clip c = null;

    /**
     * Gibt einen Ton aus
     */
    public Beep() {
        try {
            AudioFormat af = new AudioFormat(44100, 16, 1, true, false);
            byte[] soundData = getSinusTone(2000, af);
            c = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
            c.open(af, soundData, 0, soundData.length);
        } catch (LineUnavailableException ex) {
            LOG.log(Level.FINE, null, ex);
        }
    }

    /**
     * Erzeugt einen Ton mit den angegebenen Parametern
     *
     * @param frequency Frequenz
     * @param af Audioformat
     * @return Byte-Array mit Samples
     */
    private static byte[] getSinusTone(int frequency, AudioFormat af) {
        byte sample_size = (byte) (af.getSampleSizeInBits() / 8);
        byte[] data = new byte[(int) af.getSampleRate() * sample_size];
        double step_width = (2 * Math.PI) / af.getSampleRate();
        double x = 0;
        
        for (int i = 0; i < data.length; i += sample_size) {
            int sample_max_value = (int) Math.pow(2, af.getSampleSizeInBits()) / 2 - 1;
            int value = (int) (sample_max_value * Math.sin(frequency * x));
            for (int j = 0; j < sample_size; j++) {
                byte sample_byte = (byte) ((value >> (8 * j)) & 0xff);
                data[i + j] = sample_byte;
            }
            x += step_width;
        }
        return data;
    }

    /**
     * Spielt den Ton ab
     */
    public synchronized void play() {
        c.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }
        c.stop();
        try {
            Thread.sleep(300);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }
    }
}
