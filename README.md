# Handwriting Todo

Wandelt handschriftlich fotografierte To-Do-Listen automatisch in strukturiertes **Markdown** um.

## Projektstruktur

```
handwriting-todo/
├── handwriting-todo-fe/    # Angular 21 + Tailwind CSS
├── handwriting-todo-be/    # Spring Boot 3.4.3 (Java 21)
├── docs/
└── docker-compose.yml
```

## Entwicklung (lokal)

Voraussetzungen: Node.js, Java 21, Ollama lokal laufend

```bash
# Terminal 1 – Frontend (http://localhost:4200)
cd handwriting-todo-fe
npm install
npm start

# Terminal 2 – Backend (http://localhost:8080)
cd handwriting-todo-be
./gradlew bootRun
```

Das Frontend proxied `/api/*` automatisch zum Backend auf `:8080`.

### Ollama

```bash
# Starten
ollama serve
# Mit Debug-Output
OLLAMA_DEBUG=1 ollama serve

# Modell laden
ollama pull llama3.2-vision:11b

# Modell ausführen
ollama run llama3.2-vision:11b

# Testen
curl http://localhost:11434/api/chat -d "{
  \"model\": \"llama3.2-vision:11b\",
  \"messages\": [
    {
      \"role\": \"user\",
      \"content\": \"what is in this image?\",
      \"images\": [\"$(base64 -w 0 abc-test.jpg)\"]
    }
  ]
}"

# Stoppen
ollama stop llama3.2-vision:11b
systemctl stop ollama
```

Die Ollama-URL ist über die Umgebungsvariable `SPRING_AI_OLLAMA_BASE_URL` konfigurierbar (Standard: `http://localhost:11434`).

## Produktion (Docker Compose)

```bash
docker compose up --build
```

| Service  | Erreichbar unter      |
|----------|-----------------------|
| Frontend | http://localhost      |
| Backend  | http://localhost:8080 |

Ollama wird nicht im Docker Compose betrieben — entweder lokal oder über einen Service Provider konfigurieren:

```bash
# .env
OLLAMA_BASE_URL=https://api.example.com
```

## API

Alle Backend-Endpunkte sind unter `/api` erreichbar:

| Endpunkt                             | Methode | Content-Type          | Beschreibung                                                |
|--------------------------------------|---------|-----------------------|-------------------------------------------------------------|
| `GET /api/ai/ask`                    | GET     | —                     | Einzelne Textantwort (`?message=...`)                       |
| `GET /api/ai/askStream`              | GET     | —                     | Streaming-Textantwort (Flux, `?message=...`)                |
| `POST /api/ai/askAboutPicture`       | POST    | `multipart/form-data` | Einzelne Antwort zu einem Bild (`message`, `file`)          |
| `POST /api/ai/askAboutPictureStream` | POST    | `multipart/form-data` | Streaming-Antwort zu einem Bild (NDJSON, `message`, `file`) |

## Gradle Commands (Backend)

```bash
cd handwriting-todo-be
./gradlew build
./gradlew test
```

## NPM Commands (Frontend)

```bash
cd handwriting-todo-fe
npm install
npm start
npm run build
npm test
```
