A7100 Emulator - Readme v0.6.20
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

Legende:  *** - Funktioniert soweit getestet
          **  - Funktioniert mit kleinen Einschränkungen
          *   - Funktioniert nur in Teilen
          !!! - Startet nicht
          !!  - Absturz nach Titel          
          GKS - Läuft nicht, benötigt GKS der KGS
          ??? - Nicht getestet    
                          
Programm    Version Status Anmerkungen

Betriebssysteme
SCP         2.2    **     Tastatureingaben beim Start fehlerhaft / keine Ramdisk
SCP         3.0    **     Tastatureingaben beim Start fehlerhaft
SCP         3.1    ***
SCP         3.2    ***
MUTOS              !!     Hängt in HLT Befehl
BOS                ???

Grafikprogramme
Gedit M/16         GKS    Startet nicht (NMI occured), Benötigt SCP/GX
Grafik/M16  1.0    GKS    Startet, Benötigt SCP/GX

Textverarbeitung
Text 40     4.0    **     Format A4BREIT (und andere?) funktioniert nicht
Wordstar           ***
Edit               ***

Tabellenkalkulation
Tabcalc M16 3.0    ***
Tabcalc M16 2.0    ***

Datenbanken
Redabas     5.0    ***    Startet in Kommandozeile
Dbase              ***    Startet in Kommandozeile

Programmierung
Basic 1700  1.03   ***
Pascal      3.01   **     Darstellungsfehler im Editor + Compiler
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
L                  !!!   Fehler mit Übertragung Grafikkommando
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
    
Andere als die angegebenen Versionen wurden bisher nicht getestet. Für die KGS
werden gegenwärtig nur die oberen 2Kbyte (ab Adresse 0x1800) benötigt, welche
den festen Zeichensatz enthalten.

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
der darauf enthaltenen Dateien. Auch hier werden nur Binärdateien unterstützt.

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

KES/KGS Simulation:
Die UA880 auf KGS und KES werden momentan eher simuliert statt emuliert. Für die
KES stellt dies in der Regel kein Problem dar. Bei der KGS kann dies zu Fehlern
bei der Verwendung von ESC-Steuerfolgen führen. Außerdem wird bedingt dadurch 
keine grafische Ausgabe unterstützt.

Keine Frontbaugruppe:
Die Funktionen der Frontbaugruppe "Bereitschaft Ferneinschaltung" sowie der
Tongeber werden nicht unterstützt. 

Geschwindigkeit:
Der Emulator läuft gegenwärtig "so schnell wie möglich". Dies führt dazu, dass
manche Programme (vor allem Spiele) zu schnell laufen. Auch führt dies zu
unregelmäßigem Blinken des Cursors.

Blinkender Text:
Blinkender Text wird momentan nicht unterstützt. Dieser Text wird im Emulator
zur Unterscheidung rot dargestellt.

--------------------------------------------------------------------------------
6. Unterstützung

Die Entwicklung des Emulators ist entscheidend von der vorhandenen A7100
Dokumentation und Software abhängig. Hier werden die fehlenden Unterlagen und
Programme gelistet:
Dringend:
  - KES K 5170 Dokumentation (Rechner und Geräte Band 2)
  - Dokumentation zur KGS-Firmware Version 6 (GRAF6.FRM)
Wäre schön:
  - ABG K 7072 Dokumentation (Rechner und Geräte Band 2)
  - AFS K 5171.20 EPROMS
  - A7150 Rechner und Geräte Band 2/3
Nicht so wichtig:
  - OPS K 3571 Dokumentation (Rechner und Geräte Band 2)
  - ZPS K 2071 Dokumentation (Rechner und Geräte Band 2)

--------------------------------------------------------------------------------
7. Kontakt

Jeder der Anmerkungen oder Verbesserungsvorschläge hat darf sich gerne bei mir
melden. Ebenso wäre es sehr hilfreich, wenn weitere Programme auf ihre 
Kompatibilität geprüft werden. Wer entsprechende Software zum Testen hat kann
mir diese auch zukommen lassen.

Man erreicht mich am besten im Forum auf www.robotrontechnik.de 
unter dem Namen Madir.

================================================================================                       