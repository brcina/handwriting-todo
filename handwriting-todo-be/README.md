# handwriting-todo-be

Spring Boot 3.4.3 (Java 21) Backend für Handwriting Todo.

## Entwicklung

## Lokal

Voraussetzungen: Java 21, Ollama lokal laufend

```bash
./gradlew bootRun
```

Backend läuft auf `http://localhost:8080/api`.


## Ollama in Vast.ai

Voraussetzungen: Java 21, Vast.ai instance mit Ollama laufend

```bash
# vastai.env
SPRING_AI_OLLAMA_BASE_URL=<VASTAI_URL>
VAST_API_TOKEN=<VASTAI_TOKEN>

```

```bash 
export $(cat vastai.env | xargs) && ./gradlew bootRun
```

Backend läuft auf `http://localhost:8080/api`.

### Ollama

#### Ollama local 

```bash
# Starten
ollama serve
# Mit Debug-Output
OLLAMA_DEBUG=1 ollama serve

# Modell laden
ollama pull llama3.2-vision:11b

# Testen
curl http://localhost:11434/api/chat -d "{
  \"model\": \"llama3.2-vision:11b\",
  \"messages\": [
    {
      \"role\": \"user\",
      \"content\": \"what is in this image?\",
      \"images\": [\"$(base64 -w 0 ../abc-test.jpg)\"]
    }
  ]
}"

# Stoppen
ollama stop llama3.2-vision:11b
systemctl stop ollama
```

Die Ollama-URL ist über die Umgebungsvariable `SPRING_AI_OLLAMA_BASE_URL` konfigurierbar (Standard: `http://localhost:11434`).

#### Ollama vast.ai

```bash
# Starten
## TODO: Hier beschreiben  wie ich das über die CLI mache

# Testen
curl http://localhost:11434/api/chat -d "{
  \"model\": \"llama3.2-vision:11b\",
  \"messages\": [
    {
      \"role\": \"user\",
      \"content\": \"what is in this image?\",
      \"images\": [\"$(base64 -w 0 ../abc-test.jpg)\"]
    }
  ]
}"

# Stoppen
##  TODO: Vast.ai commands
``


## Nützliche Kommandos

```bash
# Bauen
./gradlew build

# Testen
./gradlew test

# Nur JAR (ohne Tests)
./gradlew bootJar
```

## API

Alle Endpunkte sind unter `/api` erreichbar:

| Endpunkt                             | Methode | Content-Type          | Beschreibung                                                |
|--------------------------------------|---------|-----------------------|-------------------------------------------------------------|
| `GET /api/ai/ask`                    | GET     | —                     | Einzelne Textantwort (`?message=...`)                       |
| `GET /api/ai/askStream`              | GET     | —                     | Streaming-Textantwort (Flux, `?message=...`)                |
| `POST /api/ai/askAboutPicture`       | POST    | `multipart/form-data` | Einzelne Antwort zu einem Bild (`message`, `file`)          |
| `POST /api/ai/askAboutPictureStream` | POST    | `multipart/form-data` | Streaming-Antwort zu einem Bild (NDJSON, `message`, `file`) |

## Docker

```bash
docker build -t handwriting-todo-be .
```

Wird normalerweise über `docker compose up --build` im Root gestartet (Backend auf `http://localhost:8080`).


## Prompt

### Simple

```text
Das Bild zeigt eine handgeschriebene deutsche To-Do-Liste. 
Jeder Eintrag beginnt mit einem Bindestrich (-). 
Ignoriere durchgestrichene Wörter. Falls ein Wort unleserlich ist, schreibe [?] dahinter — erfinde keine Wörter. 
Antworte NUR mit der Markdown-Liste im Format: - [ ] Aufgabe
```

### System & User
**System**
```text
Du bist ein OCR-Tool. 
Erkenne handgeschriebenen deutschen Text in Bildern und gib ausschließlich eine Markdown-Checkbox-Liste aus. 
Kein Kommentar, keine Erklärung, kein einleitender Text.
```
**User**
```text
Das Bild zeigt eine handgeschriebene deutsche To-Do-Liste.

Regeln:
- Jeder Eintrag beginnt mit einem Bindestrich (-)
- Ignoriere durchgestrichene Wörter vollständig
- Unleserliche Wörter → schreibe [?] an die Stelle
- Erfinde keine Wörter
- Gib NUR die Markdown-Liste aus, beginnend mit dem ersten "- [ ]"
- Kein Text vor oder nach der Liste

Format:
- [ ] Aufgabe eins
- [ ] Aufgabe zwei [?]
```

## Image Optimization


```bash
## Reduce size, optimize contrast and grey scaling 
convert todo-290326.jpg -auto-orient -colorspace Gray -resize 1500x1500 -normalize -quality 80 -threshold 50% todo-290326-optimized.jpg
```

> Image Magic needs to be installed on the system