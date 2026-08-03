# Anwendung zur Umwandlung handschriftlicher TODO-Notizen in Markdown

## Projektidee

Eine Anwendung, die handschriftlich notierte To-Do-Listen, die mit dem Smartphone fotografiert werden, automatisch in strukturiertes **Markdown** umwandelt.

Die Anwendung verwendet ein KI-Modell über **Ollama** (`qwen2.5vl:7b`), um Handschrift zu erkennen und in Text zu konvertieren. Die Anwendung lernt schrittweise die individuelle Handschrift des Users — ohne dass dieser aktiv etwas davon merkt. Der User lädt ein Bild hoch, bekommt Markdown zurück, korrigiert es bei Bedarf — der Rest passiert im Hintergrund.

## Projektstruktur

```
handwriting-todo/
├── handwriting-todo-fe/   # Angular 21 + Tailwind CSS
├── handwriting-todo-be/   # Spring Boot 3.4.3 (Java 21)
├── docs/
└── docker-compose.yml
```

### Frontend (`handwriting-todo-fe/`)

Implementiert mit **Angular 21** (Standalone Components, Signals):
- Hochladen von Fotos mit handschriftlichen Notizen
- Anzeige des generierten Markdown
- Möglichkeit zur manuellen Korrektur des Ergebnisses

Die Vokabular-Bibliothek wird automatisch im Hintergrund (V2/V3) aufgebaut und ist keine User-facing Verwaltungsfunktion im Frontend. Einsehbar ist sie allenfalls über den bestehenden API-Tester (`ApiTesterComponent`), z. B. später über `GET /api/vocabulary` — nicht über eine eigene Verwaltungs-UI für den Endnutzer.

### Backend (`handwriting-todo-be/`)

Implementiert mit **Spring Boot 3.4.3**:
- Verarbeitung der hochgeladenen Bilder (Skalierung + Binarisierung)
- Zuschneiden von Bildausschnitten für die Vokabular-Bibliothek
- Kommunikation mit dem KI-Modell via Ollama
- Umwandlung des erkannten Textes in Markdown
- REST API unter dem Prefix `/api`

### KI-Integration

- Modell: **`qwen2.5vl:7b`** via Ollama
- Ollama-URL konfigurierbar über `SPRING_AI_OLLAMA_BASE_URL` (lokal oder Service Provider wie vast.ai)

### Datenspeicherung

- **Bilder/Ausschnitte**: Lokales Dateisystem
- **Metadaten**: **H2** (Entwicklung), erweiterbar auf PostgreSQL

```sql
images (
  id,
  path_original,      -- skaliertes Originalbild (Absicherung für Reprocessing)
  path_binarized,     -- binarisierte Variante (an Vision-LLM gesendet, Basis für Zeilensegmentierung)
  markdown_raw,       -- LLM-Output
  created_at
)

corrections (
  id,
  image_id,
  markdown_original,  -- was das LLM erkannt hat
  markdown_corrected, -- User-Korrektur
  processed,          -- bereits zu Vokabular verarbeitet?
  created_at
)

vocabulary (
  id,
  snippet_path,       -- Bildausschnitt (aus path_binarized)
  correct_text,       -- korrektes Wort
  occurrences,        -- wie oft korrigiert
  created_at
)
```

## Betriebsmodi

### Entwicklung

Frontend und Backend werden unabhängig gestartet:

```bash
# Frontend (http://localhost:4200)
cd handwriting-todo-fe && npm start

# Backend (http://localhost:8080/api)
cd handwriting-todo-be && ./gradlew bootRun
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

## Evolutionsstufen

### V1 — Einfach & funktionierend

```
Bild-Upload
        ↓
Skalieren auf max. 1024px Breite
        ↓
   ┌────┴─────┐
Original   Binarisiert (Grayscale + Normalize + Threshold)
(Archiv)       ↓
           Chat History (letzte 10-15 Bilder + Markdowns)
               ↓
           qwen2.5vl:7b Vision → Rohtext
               ↓
           qwen2.5vl:7b Text → Markdown
               ↓
           User sieht Ergebnis, korrigiert manuell
               ↓
           Korrektur wird gespeichert (original + korrigiert)
```

**Ziel:** Funktionierender Grundbetrieb mit Chat-Memory.

Es werden pro Upload **zwei Bildvarianten** gespeichert:
- **Binarisiert** (`path_binarized`): geht ans Vision-LLM (höherer Kontrast, zuverlässigere Handschrifterkennung, weniger Tokens) und ist später auch die Quelle für die V2-Zeilensegmentierung — Vokabular-Snippets entsprechen so optisch genau dem, was das Modell beim Erkennungslauf gesehen hat.
- **Original** (`path_original`): dient nicht der Snippet-Qualität, sondern als Absicherung. Falls Binarisierungs- oder Downscaling-Pipeline später geändert werden, kann die gesamte Bild-History daraus neu verarbeitet werden, statt verloren zu gehen.

### V2 — Background-Verarbeitung

```
Gespeicherte Korrekturen (ab N Einträgen)
        ↓
Background-Job (nachts / bei Schwellwert)
        ↓
LLM vergleicht original vs. korrigiert → findet Diff
        ↓
Zeilensegmentierung per Java (BufferedImage auf path_binarized)
        ↓
Bildausschnitt pro falschem Wort ausschneiden
        ↓
Ausschnitt + korrektes Wort → Vokabular-Bibliothek
```

**Ziel:** Automatisches Lernen aus User-Korrekturen ohne manuelle Interaktion.

### V3 — Kombiniert & optimiert

```
System Prompt
  + Persönliches Vokabular (kompakt, aus V2)
  + Letzte 3-5 Bilder als Chat History
        ↓
qwen2.5vl:7b Vision → Markdown
```

**Ziel:** Bestes aus beiden Welten — kompaktes Langzeit-Wissen + frische Kontext-History.

## API-Flow

```
POST /api/images/upload
  → Bild auf max. 1024px skalieren (JPEG 85%)
  → Grayscale + Normalize + Threshold → binarisierte Variante
  → Chat History laden
  → Vision LLM: binarisiertes Bild + History → Rohtext
  → Korrektur LLM: Rohtext → Markdown
  ← { imageId, markdown }

POST /api/images/{imageId}/correction
  Body: { correctedMarkdown }
  → Original vs. Korrektur speichern
  → (V2) Background-Job triggern falls Schwellwert erreicht
  ← { ok }

GET /api/vocabulary
  ← Liste aller Ausschnitte + Wörter (Vokabular-Bibliothek)
```

## Bildverarbeitung (Java)

```
Upload
  → Skalieren auf max. 1024px Breite (proportional)
  → Original als JPEG 85% speichern (path_original)
  → Grayscale + Normalize + Threshold → binarisierte Variante speichern (path_binarized)
  → Tokenbedarf: ~300 Tokens statt ~3000 bei Originalgröße

Zeilensegmentierung (V2)
  → BufferedImage (auf path_binarized): Horizontale Projektion
  → Lücken zwischen Zeilen finden
  → Pro Zeile: getSubimage() → Ausschnitt speichern
```

## Token-Schätzung

|                         | Tokens              |
|-------------------------|---------------------|
| Context Window (Ollama) | 32.000              |
| System Prompt           | ~500                |
| Bild (1024px, JPEG)     | ~300                |
| Markdown pro Eintrag    | ~150                |
| **Pro History-Eintrag** | **~450**            |
| **Platz für History**   | **~10-15 Einträge** |

> Mit `OLLAMA_NUM_CTX=65536` verdoppelt sich die verfügbare History auf ~30 Einträge.

## Workflow

1. Der Benutzer fotografiert eine handschriftliche To-Do-Liste
2. Das Bild wird über das Frontend hochgeladen
3. Das Backend skaliert das Bild und erzeugt zwei Varianten: Original (Archiv) und binarisiert (Vision-Input)
4. Das Backend sendet die binarisierte Variante + Chat History an `qwen2.5vl:7b` via Ollama
5. Das Modell extrahiert den Text aus der Handschrift
6. Der erkannte Text wird automatisch in **Markdown-Format** umgewandelt (z. B. mit Checkbox-To-Dos)
7. Der Benutzer kann das Ergebnis überprüfen und korrigieren
8. Korrekturen werden gespeichert und fließen (V2) als Bildausschnitte in die Vokabular-Bibliothek ein, die künftige Requests (V3) verbessert

## Correction-Loop (V2/V3)

1. Ollama erkennt ein Wort falsch
2. Der Benutzer korrigiert den Text im Frontend
3. Die Korrektur wird gespeichert (`corrections`-Tabelle)
4. Ab einem Schwellwert vergleicht ein Background-Job original vs. korrigiert per LLM-Diff
5. Das Backend schneidet den entsprechenden **Bildausschnitt** aus der binarisierten Variante aus (Zeilensegmentierung)
6. Ausschnitt + korrektes Wort werden in der Vokabular-Bibliothek gespeichert

Ab V3 wird ein kompaktes, persönliches Vokabular direkt in den System-Prompt integriert (statt einzelner Bildausschnitte als Few-Shot-Beispiele), kombiniert mit der letzten Chat-History (3-5 Bilder).

### Prompt-Beispiel (Ziel-Design für V1/V2 mit Chat-History)

> Der aktuell implementierte System-Prompt (`handwriting-todo-be/src/main/resources/prompts/handwriting-todo-system.st`) ist ein einfacher OCR-Prompt ohne Tagging-Schema. Sobald Chat-History und Correction-Loop verdrahtet sind, soll der System-Prompt Nachrichten anhand eines Präfixes unterscheiden können:

```
Du bist ein Handschrift-Erkennungssystem für To-Do-Listen.

Der Nutzer schickt dir Bilder von handgeschriebenen Notizen.

Wenn eine Message mit [KORREKTUR] beginnt:
- Das Bild zeigt ein falsch erkanntes Wort
- Der korrekte Text steht nach dem Pfeil (→)
- Merke dir dieses Muster für zukünftige Erkennungen

Wenn eine Message mit [ERKENNUNG] beginnt:
- Extrahiere den gesamten Text aus dem Bild
- Nutze gelernte Korrekturen wenn ähnliche Muster auftauchen
- Gib das Ergebnis als Markdown-Checklist zurück (- [ ] Item)
```

So wird der Prompt aufgebaut, wenn 2 Korrekturen als Chat-History vorhanden sind:

```
[SYSTEM]
Du bist ein Handschrift-Erkennungssystem für To-Do-Listen...

[USER]
🖼 <Bildausschnitt von "Standap">
[KORREKTUR] → korrekter Text: 'Standup'

[ASSISTANT]
Verstanden, merke mir: dieses Muster = 'Standup'

[USER]
🖼 <Bildausschnitt von "Mecting">
[KORREKTUR] → korrekter Text: 'Meeting'

[ASSISTANT]
Verstanden, merke mir: dieses Muster = 'Meeting'

[USER]
🖼 <Neues Foto der To-Do-Liste>
[ERKENNUNG] Bitte erkenne den Text und gib ihn als Markdown-Checklist zurück.

[ASSISTANT]
- [ ] Standup
- [ ] Meeting vorbereiten
- [ ] PR reviewen
```

### Weitere Maßnahmen

- **Token-Limit beachten**: Größe des persönlichen Vokabulars im Prompt begrenzen
- **Korrekturen gewichten**: Seltene oder einmalige Korrekturen können herausgefiltert werden
