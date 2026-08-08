# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project does

Converts photographed handwritten to-do lists into structured Markdown via a local or remote Ollama instance running `qwen2.5vl:7b`.

## Project structure

```
handwriting-todo/
├── handwriting-todo-fe/    # Angular 21 + Tailwind CSS
├── handwriting-todo-be/    # Spring Boot 3.4.3 + WebFlux (Java 21, reactive)
├── docs/
└── docker-compose.yml
```

## Development commands

**Prerequisites:** Node.js, Java 21, Ollama running locally, ImageMagick (`convert` binary on PATH)

```bash
# Frontend (http://localhost:4200)
cd handwriting-todo-fe && npm install && npm start

# Backend (http://localhost:8080/api)
cd handwriting-todo-be && ./gradlew bootRun

# Backend with vast.ai Ollama
cd handwriting-todo-be && export $(cat vastai.env | xargs) && ./gradlew bootRun
```

**Backend build & test:**
```bash
cd handwriting-todo-be
./gradlew build
./gradlew test
./gradlew bootJar          # JAR only, skips tests
```

**Frontend build & test:**
```bash
cd handwriting-todo-fe
npm run build
npm test
```

**Docker (production):**
```bash
docker compose up --build
# Set custom Ollama URL via env: OLLAMA_BASE_URL=https://api.example.com
```

## Planned features (not yet implemented)

See `docs/projektbeschreibung.md` for the full product vision. Key planned features not in the current codebase:

- **Correction loop / few-shot learning**: user corrects OCR output → backend crops the relevant image region → stored as `{ image_crop, correct_text }` → injected as few-shot examples into future prompts
- **`corrections` table** (H2 → PostgreSQL): `id, image_path, wrong_text, correct_text, created_at`
- **Frontend views**: manual correction UI, saved handwriting-sample library

## Architecture

### Backend (Spring Boot 3.4.3, WebFlux, Spring AI 1.1.3)

All controllers are under the `/api` base path (configured in `application.yaml`). The stack is fully reactive (Project Reactor).

**Layer flow:**
- `AIController` → `AIService` — generic AI endpoints (ask, askStream, askAboutPicture, askAboutPictureStream)
- `HandwritingToDoController` → `HandwritingToDoService` → `AIService` — dedicated handwriting conversion endpoint (`POST /api/handwriting/convert`)
- `Utils.readMedia()` converts a reactive `FilePart` into a Spring AI `Media` object used by both controllers

**Key config (`OllamaConfig`):**
- Injects a `Bearer` token into every `WebClient` request when `VAST_API_TOKEN` is set — this is how vast.ai authentication works
- Sets a 360-second read timeout on the `OllamaApi` RestClient (vision models are slow)
- Model: `qwen2.5vl:7b`, configured in `application.yaml`

**Env vars:**
| Variable | Default | Purpose |
|---|---|---|
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama endpoint |
| `VAST_API_TOKEN` | _(empty)_ | Bearer token for vast.ai; omit for local Ollama |

**Prompt:** The system prompt is loaded from `src/main/resources/prompts/handwriting-todo-system.st` (Spring Template format, loaded as a `Resource`). It instructs the model to output only a Markdown checkbox list.

**Image preprocessing (ImageMagick):**
```bash
convert input.jpg -auto-orient -colorspace Gray -resize 1500x1500 -normalize -quality 80 -threshold 50% output-optimized.jpg
```
Optimized images go in `src/test/resources/` for integration tests.

This manual command (fixed 50% threshold) is only for hand-preparing test
fixtures. The runtime pipeline in `ImagePreprocessingService` shells out to
`convert` for `-auto-orient` + grayscale + resize only (the installed
ImageMagick build has no `-auto-threshold` support), then binarizes in Java
via Otsu's method, so the split threshold adapts to each photo's own
histogram instead of washing out faint strokes on overexposed photos the
way a fixed threshold would.

### Frontend (Angular 21, standalone components)

Single-page app with one component: `ApiTesterComponent` (`src/app/api-tester/`). It proxies `/api/*` to `localhost:8080` in dev mode (`proxy.conf.json`). Streaming responses are consumed via the Fetch API with NDJSON line parsing (not Angular's `HttpClient`) to support true streaming.

### Streaming protocol

Streaming endpoints (`/askStream`, `/askAboutPictureStream`, `/handwriting/convert`) return `application/x-ndjson`. Each line is a JSON object `{"answer": "<chunk>"}`. The frontend accumulates chunks directly into a signal.

## Test style

Tests are `@SpringBootTest` integration tests — they hit a real Ollama instance. No mocks.

Follow the **given / when / then** comment pattern with explicit intermediate variables:

```java
// given
Media image = new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource(imageName));
// when
Flux<String> chunks = service.convertToMarkdown(image);
String text = String.join("", Objects.requireNonNull(chunks.collectList().block()));
// then
assertThat(text).contains(expectedTodo);
```

Test results (Markdown output) are written to `handwriting-todo-be/test-output/` for manual inspection.
