# handwriting-todo-be

Spring Boot 3.4.3 (Java 21) Backend für Handwriting Todo.

## Entwicklung

Voraussetzungen: Java 21, Ollama lokal laufend

```bash
./gradlew bootRun
```

Backend läuft auf `http://localhost:8080/api`.

### Ollama

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

| Endpunkt                             | Methode | Content-Type          | Beschreibung                                                 |
|--------------------------------------|---------|-----------------------|--------------------------------------------------------------|
| `GET /api/ai/ask`                    | GET     | —                     | Einzelne Textantwort (`?message=...`)                        |
| `GET /api/ai/askStream`              | GET     | —                     | Streaming-Textantwort (Flux, `?message=...`)                 |
| `POST /api/ai/askAboutPicture`       | POST    | `multipart/form-data` | Einzelne Antwort zu einem Bild (`message`, `file`)           |
| `POST /api/ai/askAboutPictureStream` | POST    | `multipart/form-data` | Streaming-Antwort zu einem Bild (NDJSON, `message`, `file`) |

## Docker

```bash
docker build -t handwriting-todo-be .
```

Wird normalerweise über `docker compose up --build` im Root gestartet (Backend auf `http://localhost:8080`).


## Prompt

```text
Extract all text from this handwritten note.
Return only a markdown checklist, no explanation. The handwritten note is in german.
Format: - [ ] Task
```

## Image Optimization


```bash
## Reduce size
convert todo-290326.jpg -auto-orient -resize 1500x1500 -quality 80 todo-290326-opt.jpg
## Optimize contrast and grey scaling 
convert todo-290326.jpg \
  -auto-orient \
  -colorspace Gray \
  -normalize \
  -threshold 50% \
  todo-290326-bw.jpg
```

> Image Magic needs to be installed on the system