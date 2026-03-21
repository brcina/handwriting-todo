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

| Endpunkt              | Beschreibung                   |
|-----------------------|--------------------------------|
| `GET /api/ai/ask`        | Einzelne Antwort            |
| `GET /api/ai/askStream`  | Streaming-Antwort (Flux)    |

## Docker

```bash
docker build -t handwriting-todo-be .
```

Wird normalerweise über `docker compose up --build` im Root gestartet (Backend auf `http://localhost:8080`).