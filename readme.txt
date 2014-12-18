A7100 Emulator - Readme v0.7.90
===============================

Inhaltsverzeichnis:

  1. Lizenzinformationen
  2. Letzte Änderungen
  3. Softwarekompatibilität
  4. Hinweise zur Bedienung
  5. Bekannte Fehler / Nicht unterstützte Funktionen
  6. Unterstützung
  7. Kontakt

--------------------------------------------------------------------------------
1. Lizenzinformationen:

Diese Software ist Freeware und darf uneingeschränkt genutzt, kopiert und
verbreitet werden, solange die folgenden Bedingungen erfüllt sind:
  1. Die Software, oder ihre Bestandteile, dürfen nicht verkauft oder ohne 
     Genehmigung des Autors mit anderen Programmen gebündelt werden.
  2. Die Software darf kostenlos zum Download bereitgestellt werden.
  3. Der Autor bleibt auch bei Weitergabe Eigentümer der Software.
  4. Alle Programmteile müssen für die Weitergabe unverändert bleiben. 
     Insbesondere dürfen weder Programmname, Name des Autors noch die 
     vorliegenden Lizenzinformationen verändert werden.
  5. Die Software darf ohne Einwilligung des Autors nicht Disassembliert
     werden.
  6. Die für den Emulator benötigten EPROM- und Diskettenabbilder unterliegen
     ggf. weiteren Lizenzbestimmungen. Der Anwender verpflichtet sich diese bei
     der Verwendung der Software einzuhalten.   
  7. Das Programm wird bereitgestellt "WIE-ES-IST" und die Nutzung erfolgt 
     ausschließlich auf eigenes Risiko. Der Autor übernimmt keine Garantie das
     die Software frei von Fehlern ist, ohne Unterbrechung arbeitet oder den 
     jeweils gestellten Anforderungen entspricht. Für Sachschäden oder 
     finanzielle Schäden, welche aus der Verwendung des Programms resultieren, 
     bspw. Verlust von Daten, Verlust von Gewinn, Betriebsunterbrechung,
     übernimmt der Autor keinerlei Haftung. 

--------------------------------------------------------------------------------
2. Letzte Änderungen

  v0.7.90 - 19.12.2014
  Neue Features
    - KGS/ABG neu implementiert mit Emulation des UA880 Subsystems
    - Grafikkommandos sind ausführbar
    - Debugger-System neu implementiert (Debuggen des UA880, Ausgabe Speicher 
      KGS und ABG)
    - Hinzufügen von Dateien im SCP-Disketten-Tool
    - Hacks hinzugefügt
  Änderungen/Bugfixes:
    - Tastaturpuffer neu implementiert
  Softwarekompatibilität:
    - Läuft mit kleinen Einschränkungen -> Läuft : SCP 2.2, SCP 3.0
    - Läuft Nicht -> Läuft: L
    - Läuft Nicht -> Läuft mit kleinen Einschränkungen: Grafik M/16
    - Läuft Nicht -> Funktioniert in Teilen: Gedit M/16
  ----------------------------------------
  v0.6.20 - 15.07.2014
  Neue Features:
    - Lesen von Teledisk, Imagedisk und Catweasel-Images
    - Beliebig formatierte RAW-Images
  Änderungen/Bugfixes:
    - Schreiben von Wörtern auf KES-Ports implementiert
  Softwarekompatibilität:
    - Nicht getestet -> Absturz nach Titel: MUTOS 1700
  ----------------------------------------
  v0.6.00 - 31.03.2014
  Neue Features:
    - Erste öffentliche Version
    - Screenshots über Menü möglich
  ----------------------------------------
  v0.5.39 - 30.03.2014
  Änderungen/Bugfixes:
    - Segment Wraparound implementiert
    - Implementierung des SCP-Disk Tools
  Softwarekompatibilität:
    - Läuft nur in Teilen -> Läuft: Tabcalc 3.0, Tabcalc 2.0, Basic 1700
  ----------------------------------------
  v0.5.30 - 01.03.2014
  Änderungen/Bugfixes:
    - Zugriff auf Festplatten korrekt abgewiesen
  Softwarekompatibilität:
    - Läuft Nicht -> Läuft: SCP 3.1, SCP 3.2
    - Läuft Nicht -> Läuft mit kleinen Einschränkungen : SCP 3.0
  ----------------------------------------
  v0.5.23 - 28.02.2014
  Änderungen/Bugfixes:
    - Carry-Flag-Fehler für INC, DEC behoben
  Softwarekompatibilität:
    - Absturz nach Titel -> Läuft: WORDSTAR
  ----------------------------------------
  v0.5.22 - 27.02.2014
  Änderungen/Bugfixes:
    - Fehler in ESC-Sequenzen behoben
    - Standardparameter für ESC[K
    - Tastaturbefehle für Numpad hinzugefügt
    - Tastaturcodes PF1-PF12 aktualisiert
    - Fehler Wraparound behoben
  Softwarekompatibilität:
    - Absturz nach Titel -> Läuft in Teilen: TEXT40
 
--------------------------------------------------------------------------------
3. Softwarekompatibilität

Legende:
Status:  *** - Funktioniert soweit getestet
         **  - Funktioniert mit kleinen Einschränkungen
         *   - Funktioniert nur in Teilen
         !!! - Startet nicht
         !!  - Absturz nach Titel          
         ??? - Nicht getestet    
Hacks :  P - Paritätsprüfung OPS abschalten
         K - Tastaturreset abschalten
                          
Programm Version Status Hack Anmerkungen

Betriebssysteme
SCP         2.2    ***     
SCP         3.0    ***     
SCP         3.1    ***
SCP         3.2    ***
MUTOS              !!        Hängt in HLT Befehl / Trap
BOS                ???

Grafikprogramme
Gedit M/16  2.0    *    P    Unregelmäßiger Start, Darstellungsfehler
Grafik/M16  1.0    **        Darstellungsfehler Tortengrafik

Textverarbeitung
Text 40     4.0    **   K    Format A4BREIT (und andere?) funktioniert nicht
Wordstar           ***  
Edit               ***

Tabellenkalkulation
Tabcalc M16 3.0    ***  K
Tabcalc M16 2.0    ***  K

Datenbanken
Redabas     5.0    ***       Startet in Kommandozeile
Dbase              ***       Startet in Kommandozeile

Programmierung
Basic 1700  1.03   ***
Pascal      3.01   **        Darstellungsfehler im Compiler
Fortran            ???

Spiele
Castle             ***
Wall               ***
Schach             ***

Systemprogramme
Power              ***
Ibmcode            ***
Kyrill             ***
German             ***
Init               ***
Copydisk           ***
Stat               ***
LDCopy             ***
L                  ***   
Graphics           ***

Sonstiges
Messe              ***

--------------------------------------------------------------------------------
4. Hinweise zur Bedienung	

4.1 Benötigte EPROMS:
Für den Betrieb des Emulators werden gegenwärtig die EPROMS der ZVE und KGS
benötigt. Diese müssen beim Programmstart im Unterverzeichnis EPROMS wie folgt
abgelegt werden:
    ZVE: - AHCL     ./eproms/ZVE-K2771.10-259.rom
         - AWCL     ./eproms/ZVE-K2771.10-260.rom
         - BOCL     ./eproms/ZVE-K2771.10-261.rom
         - CGCL     ./eproms/ZVE-K2771.10-262.rom
    KGS: - KGS-ROM  ./eproms/KGS-K7070-152.rom
    
Andere als die angegebenen Versionen wurden bisher nicht getestet.

4.2 Arbeit mit Disketten
4.2.1 Diskettenabbilder
Seit v0.6.20 werden beliebige RAW-Abbilder unterstützt. Dabei müssen durch den
Benutzer beim Laden des Images die Formatparameter festgelegt werden. Als
Standard ist hierbei das SCP-Format hinterlegt. Weiterhin wird das Lesen von 
Teledisk- (TD0), Imagedisk- (IMD) und Catweasel- (DMK) Abbilder unterstützt.
                                                          
4.2.2 Speichern auf Disketten
Veränderte Disketten werden nicht automatisch in der Binärdatei geändert. Das
Speichern muss über Geräte->Laufwerk X->Speicher Image erfolgen.

4.2.3 SCP-Disk Betrachter
Der Diskbetrachter ermöglicht das Lesen von SCP-Disketten sowie das extrahieren
der darauf enthaltenen Dateien. Seit Version v0.7.90 können auch Dateien dem 
Image hinzugefügt werden. Auch hier werden nur Binärdateien unterstützt.

4.3 Konfiguration des A7100
Im der aktuellen Version ist der A7100 mit folgenden Moduln bestückt:
    - ZVE
    - KGS + ABG
    - KES + AFS
    - 3xOPS

 4.4 Tastatur
 Vom Emulator wird aktuell die Tastatur K7637 verwendet. Darstellbare Zeichen
 entsprechen weitestgehend denen der PC-Tastatur. Die Sondertasten sind wie
 folgt zugeordnet, wobei noch nicht alle Tasten der K7637 verwendet werden
 können:
    - F1-F12          ->    PF1-PF12
    - Pause           ->    Break
    - Pos1            ->    PA1
    - Shift+Pos1      ->    PA2
    - Ende            ->    PA3
    - Shift+Ende      ->    Pfeil nach links oben

-------------------------------------------------------------------------------- 
5. Bekannte Fehler / Nicht unterstützte Funktionen

KGS/ABG Synchronisation:
Die mit Version v0.7.90 erfolgte Neuimplementierung von KGS/ABG läuft noch nicht
vollständig stabil. So kann es bei der Synchronisation zwischen der ZVE und den
Subsystemen zu Deadlocks kommen. Dies ist teilweise auch von Nutzeraktivitäten
abhängig. Für manche Programme (bspw. Gedit) hilft hier nur mehrfaches
neustarten. Außerdem sollten bei Problemen die Tastatureingaben nicht zu schnell
hintereinander ausgeführt werden.

Hacks:
Der Tastaturcontroller und die Paritätsprüfung der OPS-Module sind noch nicht
vollständig bzw. noch nicht korrekt implementiert. Um dennoch gegenwärtig 
möglichst viel Software zu unterstützen, lässt sich über das Menü Hacks die
Funktionalität der Komponenten zur Laufzeit ändern. Welche Änderungen für
welche Software notwendig ist, lässt sich aus der Softwarekompatibilitätsliste
entnehmen.

KES Simulation:
Der UA880 auf dem KES wird momentan noch simuliert. Dies stellt jedoch in den
meisten Fällen kein Problem dar.

Keine Frontbaugruppe:
Die Funktionen der Frontbaugruppe "Bereitschaft Ferneinschaltung" sowie der
Tongeber werden nicht unterstützt. 

Performance:
Die Emulation der KGS-CPU und das damit verbundene ständige Berechnen der
Anzeige aus den Bildwiederholspeichern der ABG führt dazu, dass der Emulator
in Version v0.7.90 deutlich langsamer läuft als in den vorherigen Versionen.

Probleme bei der Darstellung:
Blinkender Text wird momentan nicht unterstützt. Zusätzlich gibt es Probleme bei
der Verwendung einer Splitgrenze. Der Alphanumerikteil wird in diesem Fall 
fehlerhaft dargestellt.

--------------------------------------------------------------------------------
6. Unterstützung

Die Entwicklung des Emulators ist entscheidend von der vorhandenen A7100
Dokumentation und Software abhängig. Hier werden die fehlenden Unterlagen und
Programme gelistet:
Dringend:
  - Dokumentation zur KGS-Firmware Version 6 (GRAF6.FRM)
  - AFS K 5171.20 EPROMS
Wäre schön:
  - A7150 Rechner und Geräte Band 2/3
  - Sämtliche nicht getestete Software
Nicht so wichtig:
  - Tastatur EPROMS
  - Andere Versionen (wenn vorhanden) der ZVE EPROMS
  
--------------------------------------------------------------------------------
7. Kontakt

Jeder der Anmerkungen oder Verbesserungsvorschläge hat darf sich gerne bei mir
melden. Ebenso wäre es sehr hilfreich, wenn weitere Programme auf ihre 
Kompatibilität geprüft werden. Wer entsprechende Software zum Testen hat kann
mir diese auch zukommen lassen.

Man erreicht mich am besten im Forum auf www.robotrontechnik.de 
unter dem Namen Madir.

================================================================================                       