# Anwendung zur Umwandlung handschriftlicher TODO-Notizen in Markdown

## Projektidee

Eine Anwendung, die handschriftlich notierte To-Do-Listen, die mit dem Smartphone fotografiert werden, automatisch in strukturiertes **Markdown** umwandelt.

Die Anwendung verwendet ein KI-Modell über **Ollama** (`llama3.2-vision:11b`), um Handschrift zu erkennen und in Text zu konvertieren. Durch ein **bildbasiertes Feedback-System** lernt die Anwendung schrittweise, die individuelle Handschrift besser zu erkennen.

## Architektur

```
handwriting-todo/
├── frontend/          # Angular 21 + Tailwind CSS
├── backend/           # Spring Boot 3.4.3 (Java 21)
├── docs/
├── docker-compose.yml
└── build.gradle
```

### Frontend (`frontend/`)

Implementiert mit **Angular 21** (Standalone Components, Signals):
- Hochladen von Fotos mit handschriftlichen Notizen
- Anzeige des generierten Markdown
- Möglichkeit zur manuellen Korrektur des Ergebnisses
- Verwaltung gespeicherter Handschrift-Beispiele (Few-Shot-Bibliothek)

### Backend (`backend/`)

Implementiert mit **Spring Boot 3.4.3**:
- Verarbeitung der hochgeladenen Bilder
- Zuschneiden von Bildausschnitten für die Few-Shot-Bibliothek
- Kommunikation mit dem KI-Modell via Ollama
- Umwandlung des erkannten Textes in Markdown
- REST API unter dem Prefix `/api`

### KI-Integration

- Modell: **`llama3.2-vision:11b`** via Ollama
- Ollama-URL konfigurierbar über `SPRING_AI_OLLAMA_BASE_URL` (lokal oder Service Provider wie vast.ai)

### Datenspeicherung

- **Bilder/Ausschnitte**: Lokales Dateisystem
- **Metadaten**: **H2** (Entwicklung), erweiterbar auf PostgreSQL

```sql
corrections (
  id,
  image_path,    -- Pfad zum Bildausschnitt
  wrong_text,    -- was Ollama erkannt hat
  correct_text,  -- manuelle Korrektur
  created_at
)
```

## Betriebsmodi

### Entwicklung

Frontend und Backend werden unabhängig gestartet:

```bash
# Frontend (http://localhost:4200)
cd frontend && npm start

# Backend (http://localhost:8080/api)
./gradlew :backend:bootRun
```

Das Angular Dev-Server proxied `/api/*` automatisch zum Backend.

### Produktion

Docker Compose startet Frontend (nginx) und Backend als separate Container:

```bash
docker compose up --build
```

| Service  | URL                   |
|----------|-----------------------|
| Frontend | http://localhost      |
| Backend  | http://localhost:8080 |

Ollama läuft extern — konfigurierbar über `.env`:

```bash
OLLAMA_BASE_URL=http://host.docker.internal:11434  # lokal
OLLAMA_BASE_URL=https://api.example.com             # Service Provider
```

## Workflow

1. Der Benutzer fotografiert eine handschriftliche To-Do-Liste
2. Das Bild wird über das Frontend hochgeladen
3. Das Backend baut den Prompt auf (inkl. gespeicherter Handschrift-Beispiele)
4. Das Backend sendet Bild + Prompt an `llama3.2-vision:11b` via Ollama
5. Das Modell extrahiert den Text aus der Handschrift
6. Der erkannte Text wird automatisch in **Markdown-Format** umgewandelt (z. B. mit Checkbox-To-Dos)
7. Der Benutzer kann das Ergebnis überprüfen und korrigieren
8. Korrekturen werden als neue Bildausschnitte gespeichert und beim nächsten Request verwendet

## Prompt-Engineering Strategie

### Bildbasiertes Few-Shot Learning (Correction Loop)

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

### Weitere Maßnahmen

- **Persönliches Vokabular**: Häufig verwendete Begriffe können dem Prompt vorab mitgegeben werden
- **Token-Limit beachten**: Maximale Anzahl Few-Shot-Beispiele im Prompt begrenzen
- **Korrekturen gewichten**: Seltene oder einmalige Korrekturen können herausgefiltert werden
