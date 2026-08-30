TODOs
=====

## Aktuell
- [ ] Im Edit und fertig Screen vergangene Zeit anzeigen
- [ ] Persistence Data Modell designen
- [ ] Persistence mit r2dbc implementieren, 1. Version einfach Bilder speichern
- [ ] Correction-Loop für verbessertes Markdown implementieren
- [ ] Das Modell erfindet, Wörter, Sätze, wenn es was nicht versteht wie kann man das berücksichtigen
- [ ] Eine bessere sophisticated Way of Korrektur Loop z.B. das ist erfunden, ganzer Satz falsch etc.
- [ ] Ollama beim Start vorwärmen
- [ ] Autokorrektur-Modell integrieren und nach der Extraktion über den Text laufen lassen

## Vielleicht
- [ ] Verschiedene Ollama-Modelle für Handschrift installieren und im API-Tester auswählbar machen (REST-Endpunkt etc.)

## Erledigt
- [x] Handschriften-GUI-Komponente aktualisieren, um das Markdown nachträglich zu verbessern
- [x] Auf image magic umsteigen, schnelle Lösung, das Ergebnis jetzt mit iamge processor ist zu schlecht
- [x] Performance: Bild-Downscaling (1024px) und Bild-Vorverarbeitung (Kontrast + Binarisierung) im Backend vor dem Ollama-Aufruf implementieren
- [x] REST-Controller und Angular-GUI zum Hochladen einer Handschrift-Datei und Anzeigen des Textes als Markdown
- [x] Handschrift-TODOs-zu-Markdown-Service implementieren
- [x] vast.ai integrieren und beim Entwickeln konfigurierbar sowie automatisch startbar machen
- [x] System-Prompt im UI zugänglich machen und die entsprechenden REST-Methoden mappen
- [x] Sicherstellen, dass Stop-Stream beim Streaming einen echten Abbruch auslöst (vorher: Endlos-Loop)
- [x] Error-Handling im API-Tester verbessern (z. B. wenn Ollama nicht läuft)
- [x] REST-Controller und Angular-GUI-Komponenten zum Testen mit der GUI erstellen
- [x] Projekt auf Docker-Compose-Setup umstellen
- [x] Text- und Bild-Service-Implementierung erstellen
- [x] Ollama soweit zum Laufen bringen, dass es über eine Service-Implementierung ansprechbar ist
- [x] Projekt in Git einchecken
- [x] Build soweit fixen, dass die Anwendung erstellt und gestartet werden kann (Dummy-Anwendung)
- [x] Spring Boot mit Main-Klasse im BE-Projekt anlegen, alle initialen Dependencies
      (SQLite, Ollama, REST Web)
