# Handwriting Todo

Wandelt handschriftlich fotografierte To-Do-Listen automatisch in strukturiertes **Markdown** um.

## Projektstruktur

```
handwriting-todo/
├── handwriting-todo-fe/    # Angular 21 + Tailwind CSS
├── handwriting-todo-be/    # Spring Boot 3.4.3 + WebFlux (Java 21, reaktiv)
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

Die Ollama-URL ist über die Umgebungsvariable `SPRING_AI_OLLAMA_BASE_URL` konfigurierbar (Standard: `http://localhost:11434`).

#### Ollama lokal

```bash
# Starten
ollama serve
# Mit Debug-Output
OLLAMA_DEBUG=1 ollama serve

# Modell laden
ollama pull qwen2.5vl:7b

# Modell ausführen
ollama run qwen2.5vl:7b

# Testen
curl http://localhost:11434/api/chat -d "{
  \"model\": \"qwen2.5vl:7b\",
  \"messages\": [
    {
      \"role\": \"user\",
      \"content\": \"what is in this image?\",
      \"images\": [\"$(base64 -w 0 abc-test.jpg)\"]
    }
  ]
}"

# Stoppen
ollama stop qwen2.5vl:7b
systemctl stop ollama
```

#### Ollama vast.ai

```bash
# Angebote suchen (günstigstes zuerst, >=16 GB VRAM)
vastai search offers 'gpu_ram>=16 num_gpus=1' -o 'dph' --raw | jq '.[0:3] | .[] | {id, gpu_name, gpu_ram, dph_total}'

# Instanz erstellen
vastai create instance <OFFER_ID> \
  --image vastai/ollama:0.20.0 \
  --env '-p 1111:1111 -p 6006:6006 -p 7860:7860 -p 8080:8080 -p 8384:8384 -p 72299:72299 -p 21434:21434 -e OLLAMA_MODEL="qwen2.5vl:7b"' \
  --onstart-cmd 'entrypoint.sh' \
  --disk 32 --ssh --direct
# Ausgabe: Started. {'success': True, 'new_contract': <ID>, 'instance_api_key': '<KEY>'}

# Öffentliche IP anzeigen
vastai show instance <INSTANCE_ID> --raw | jq '{status: .actual_status, ip: .public_ipaddr}'

# Logs anzeigen
vastai logs <INSTANCE_ID>

# Auf Modell-Start warten (alle 5 s)
watch -n 5 'vastai logs <INSTANCE_ID> | tail -5'

# Testen
source ./vast-config.env
curl -H "Authorization: Bearer $VAST_TOKEN" \
     -d "{
  \"model\": \"qwen2.5vl:7b\",
  \"messages\": [
    {
      \"role\": \"user\",
      \"content\": \"what is in this image?\",
      \"images\": [\"$(base64 -w 0 ../abc-test.jpg)\"]
    }
  ]
}" \
$VAST_HOST/api/chat

# Instanz stoppen
vastai stop instance <INSTANCE_ID>

# Instanz wieder starten
vastai start instance <INSTANCE_ID>
```


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

| Endpunkt                             | Methode | Content-Type          | Beschreibung                                                              |
|--------------------------------------|---------|-----------------------|---------------------------------------------------------------------------|
| `GET /api/ai/ask`                    | GET     | —                     | Einzelne Textantwort (`?system=...&message=...`)                          |
| `GET /api/ai/askStream`              | GET     | `application/x-ndjson` | Streaming-Textantwort (`?system=...&message=...`)                        |
| `POST /api/ai/askAboutPicture`       | POST    | `multipart/form-data` | Einzelne Antwort zu einem Bild (`system`, `message`, `file`)              |
| `POST /api/ai/askAboutPictureStream` | POST    | `multipart/form-data` | Streaming-Antwort zu einem Bild (NDJSON, `system`, `message`, `file`)    |

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
