# Anwendung zur Umwandlung handschriftlicher TODO-Notizen in Markdown

## Projektidee

Ich möchte eine Anwendung entwickeln, die handschriftlich notierte To-Do-Listen, die ich mit dem Smartphone fotografiere, automatisch in strukturiertes **Markdown** umwandelt.

Die Anwendung verwendet ein lokal laufendes KI-Modell über **Ollama** (`llama3.2-vision:11b`), um Handschrift zu erkennen und in Text zu konvertieren. Durch ein **bildbasiertes Feedback-System** lernt die Anwendung schrittweise, die individuelle Handschrift besser zu erkennen.

## Architektur

### Frontend
Das Frontend wird mit **AngularJS** implementiert und bietet folgende Funktionen:
- Hochladen von Fotos mit handschriftlichen Notizen
- Anzeige des generierten Markdown
- Möglichkeit zur manuellen Korrektur des Ergebnisses
- Verwaltung gespeicherter Handschrift-Beispiele (Few-Shot-Bibliothek)

### Backend
Das Backend wird mit **Spring Boot** implementiert und übernimmt:
- Verarbeitung der hochgeladenen Bilder
- Zuschneiden von Bildausschnitten für die Few-Shot-Bibliothek
- Kommunikation mit dem KI-Modell via Ollama
- Umwandlung des erkannten Textes in Markdown

### KI-Integration
- Modell: **`llama3.2-vision:11b`** via Ollama
- Läuft vollständig **lokal** (`localhost:11434`)
- Die Ollama-URL ist über `application.properties` konfigurierbar (für späteren Wechsel z. B. zu vast.ai)

### Datenspeicherung
- **Bilder/Ausschnitte**: Lokales Dateisystem
- **Metadaten**: **SQLite** (einfach, keine Installation erforderlich, eine `.db`-Datei)

```sql
corrections (
  id,
  image_path,    -- Pfad zum Bildausschnitt
  wrong_text,    -- was Ollama erkannt hat
  correct_text,  -- manuelle Korrektur
  created_at
)
```

## Prompt-Engineering Strategie

### Bildbasiertes Few-Shot Learning (Correction Loop)

Der Kernmechanismus zur Verbesserung der Erkennung funktioniert wie folgt:

1. Ollama erkennt ein Wort falsch
2. Der Benutzer korrigiert den Text im Frontend
3. Das Backend schneidet den entsprechenden **Bildausschnitt** aus dem Originalbild aus
4. Ausschnitt + Korrektur werden gespeichert: `{ bild: "ausschnitt.png", richtig: "Standup" }`

Beim nächsten Request werden die gespeicherten Beispiele dynamisch in den Prompt injiziert:

```
Bekannte Wörter aus dieser Handschrift:
[Bild: ausschnitt1.png] = "Standup"
[Bild: ausschnitt2.png] = "Todo"

Erkenne nun den Text im neuen Bild...
```

Das Modell sieht echte Beispiele der Handschrift – deutlich effektiver als reine Text-Hints.

### Weitere Maßnahmen
- **Persönliches Vokabular**: Häufig verwendete Begriffe können dem Prompt vorab mitgegeben werden
- **Token-Limit beachten**: Maximale Anzahl Few-Shot-Beispiele im Prompt begrenzen
- **Korrekturen gewichten**: Seltene oder einmalige Korrekturen können herausgefiltert werden

## Workflow

1. Der Benutzer fotografiert eine handschriftliche To-Do-Liste.
2. Das Bild wird über das Frontend hochgeladen.
3. Das Backend baut den Prompt auf (inkl. gespeicherter Handschrift-Beispiele).
4. Das Backend sendet Bild + Prompt an `llama3.2-vision:11b` via Ollama.
5. Das Modell extrahiert den Text aus der Handschrift.
6. Der erkannte Text wird automatisch in **Markdown-Format** umgewandelt (z. B. mit Checkbox-To-Dos).
7. Der Benutzer kann das Ergebnis überprüfen und gegebenenfalls korrigieren.
8. Korrekturen werden als neue Bildausschnitte gespeichert und beim nächsten Request verwendet.

## Ziel

Das Ziel der Anwendung ist es, handschriftliche Notizen schnell und effizient in ein digitales, strukturiertes Format zu überführen, das sich leicht weiterverarbeiten oder in Dokumentationen und Aufgabenlisten integrieren lässt. Durch das iterative Feedback-System verbessert sich die Erkennungsqualität mit jeder Korrektur automatisch.
