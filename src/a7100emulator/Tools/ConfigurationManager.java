/*
 * ConfigurationManager.java
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
 *   17.03.2018 - Erste Version
 */
package a7100emulator.Tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Verwaltung der Emulatorkonfiguration. Diese Klasse stellt alle
 * Methoden zum Lesen der Konfigurationsdatei zur Verfügung.
 *
 * @author Dirk Bräuer
 */
public class ConfigurationManager {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ConfigurationManager.class.getName());

    /**
     * Konfigurationsdatei
     */
    private static final String CONFIG_FILE = "A7100Emulator.conf";

    /**
     * Singleton Instanz
     */
    private static ConfigurationManager instance;

    /**
     * Privater Konstruktor für Singleton-Instanz
     */
    private ConfigurationManager() {
    }

    /**
     * Liefert die Singleton Instanz des Konfigurationsmanagers zurück.
     *
     * @return Singleton Instanz
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    /**
     * Liest einen Boolean-Wert aus der Konfigurationsdatei.
     *
     * @param category Kategorie
     * @param key Schlüssel
     * @param defaultValue Standardwert
     * @return gelesener Wert oder <code>defaultValue</code>, falls der
     * angegebenen Schlüssel nicht gefunden wurde
     */
    public boolean readBoolean(String category, String key, boolean defaultValue) {
        String stringValue = readString(category, key, defaultValue ? "1" : "0");
        switch (stringValue) {
            case "0":
                return false;
            case "1":
                return true;
            default:
                LOG.log(Level.WARNING, "Unbekannter boolescher Wert " + stringValue + " beim Lesen des Schlüssels " + key + " in Kategorie " + category + "!");
                return defaultValue;
        }
    }
    
    /**
     * Liest einen Integer-Wert aus der Konfigurationsdatei.
     *
     * @param category Kategorie
     * @param key Schlüssel
     * @param defaultValue Standardwert
     * @return gelesener Wert oder <code>defaultValue</code>, falls der
     * angegebenen Schlüssel nicht gefunden wurde
     */
    public int readInteger(String category, String key, int defaultValue) {
        String stringValue = readString(category, key, Integer.toString(defaultValue));
        try {
            int value = Integer.parseInt(stringValue);
            return value;
        }catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Unbekannter Zahlenwert " + stringValue + " beim Lesen des Schlüssels " + key + " in Kategorie " + category + "!");
            return defaultValue;
        }
    }

    /**
     * Liest einen String-Wert aus der Konfigurationsdatei.
     *
     * @param category Kategorie
     * @param key Schlüssel
     * @param defaultValue Standardwert
     * @return gelesener Wert oder <code>defaultValue</code>, falls der
     * angegebenen Schlüssel oder die Kategorie nicht gefunden wurde
     */
    public String readString(String category, String key, String defaultValue) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_FILE));

            String line;
            // Kategorie suchen
            do {
                line = in.readLine();
                if (line == null) {
                    // Dateiende
                    LOG.log(Level.WARNING, "Angeforderte Kategorie " + category + " nicht gefunden!");
                    in.close();
                    return defaultValue;
                }
            } while (!line.trim().equalsIgnoreCase("[" + category + "]"));

            // Schlüssel suchen
            do {
                line = in.readLine();
                if (line == null || (line.trim().startsWith("[") && line.trim().endsWith("]"))) {
                    // Dateiende oder nächste Kategorie
                    LOG.log(Level.WARNING, "Angeforderter Schlüssel " + key + " in Kategorie " + category + " nicht gefunden!");
                    in.close();
                    return defaultValue;
                }
            } while (!line.split("=")[0].trim().equalsIgnoreCase(key));

            // Gefunden
            in.close();
            return line.split("=")[1].trim();
        } catch (FileNotFoundException ex) {
            LOG.log(Level.WARNING, "Konfigrationsdatei A7100Emulator.conf wurde nicht gefunden!", ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Fehler beim Lesen der Konfigurationsdatei A7100Emulator.conf!", ex);
        }
        return defaultValue;
    }
}
