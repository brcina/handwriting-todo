# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projektidee

Diese Anwendung wandelt handschriftlich notierte To-Do-Listen, die mit dem Smartphone fotografiert werden, automatisch in strukturiertes **Markdown** um.

Die Anwendung verwendet ein lokal laufendes KI-Modell über **Ollama** (`llama3.2-vision:11b`), um Handschrift zu erkennen und in Text zu konvertieren. Durch ein **bildbasiertes Feedback-System** lernt die Anwendung schrittweise, die individuelle Handschrift besser zu erkennen.

## Workflow

1. Der Benutzer fotografiert eine handschriftliche To-Do-Liste
2. Das Bild wird über das Frontend hochgeladen
3. Das Backend baut den Prompt auf (inkl. gespeicherter Handschrift-Beispiele)
4. Das Backend sendet Bild + Prompt an `llama3.2-vision:11b` via Ollama
5. Das Modell extrahiert den Text aus der Handschrift
6. Der erkannte Text wird automatisch in **Markdown-Format** umgewandelt (z. B. mit Checkbox-To-Dos)
7. Der Benutzer kann das Ergebnis überprüfen und gegebenenfalls korrigieren
8. Korrekturen werden als neue Bildausschnitte gespeichert und beim nächsten Request verwendet

## Ziel

Das Ziel der Anwendung ist es, handschriftliche Notizen schnell und effizient in ein digitales, strukturiertes Format zu überführen, das sich leicht weiterverarbeiten oder in Dokumentationen und Aufgabenlisten integrieren lässt. Durch das iterative Feedback-System verbessert sich die Erkennungsqualität mit jeder Korrektur automatisch.

## Project Structure

This is a Gradle-based multi-project build containing:
- **handwriting-todo-fe**: Angular 21 frontend with Tailwind CSS v4, using Vitest for testing
- **handwriting-todo-be**: Spring Boot 4.0.3 backend (Java)

The build is coordinated at the root level with dependencies between frontend and backend (backend compilation depends on frontend build artifacts being copied).

## KI-Integration

- Modell: **`llama3.2-vision:11b`** via Ollama
- Läuft vollständig **lokal** (`localhost:11434`)
- Die Ollama-URL ist über `application.properties` konfigurierbar (für späteren Wechsel z. B. zu vast.ai)

## Datenspeicherung

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

## Frontend Development

The frontend uses Angular 21 with standalone components and signals.

### Frontend Features
- Hochladen von Fotos mit handschriftlichen Notizen
- Anzeige des generierten Markdown
- Möglichkeit zur manuellen Korrektur des Ergebnisses
- Verwaltung gespeicherter Handschrift-Beispiele (Few-Shot-Bibliothek)

### Commands
```bash
# Navigate to frontend directory first
cd handwriting-todo-fe

# Development server (runs on http://localhost:4200)
npm run start
# or
ng serve

# Build for production
npm run build
# or
ng build

# Run tests with Vitest
npm run test
# or
ng test

# Development build with watch mode
npm run watch
```

### Architecture Notes
- Uses Angular standalone components (no NgModules)
- Routing configured in `src/app/app.routes.ts`
- Application config in `src/app/app.config.ts` with `provideBrowserGlobalErrorListeners()`
- Tailwind CSS v4 with PostCSS for styling
- Component prefix: `app`
- Uses Angular signals for reactive state management

## Backend Development

The backend is a Spring Boot application.

### Backend Responsibilities
- Verarbeitung der hochgeladenen Bilder
- Zuschneiden von Bildausschnitten für die Few-Shot-Bibliothek
- Kommunikation mit dem KI-Modell via Ollama
- Umwandlung des erkannten Textes in Markdown

### Commands
```bash
# Build backend (depends on frontend build)
./gradlew :handwriting-todo-be:build

# Run backend
./gradlew :handwriting-todo-be:bootRun

# Run tests
./gradlew :handwriting-todo-be:test
```

### Architecture Notes
- Spring Boot 4.0.3 with Spring dependency management
- Backend compilation depends on frontend artifacts via `compileJava.dependsOn project(':frontend').tasks.copyToBackend`
- Backend currently has minimal source files (appears to be early in development)
- Will use SQLite for storing correction metadata
- Image cropping functionality for Few-Shot examples

## Build System

This project uses Gradle 9.4.0 with a multi-project setup.

### Commands
```bash
# Build entire project (backend + frontend)
./gradlew build

# Clean build
./gradlew clean build

# See all tasks
./gradlew tasks

# Build specific subproject
./gradlew :handwriting-todo-fe:build
./gradlew :handwriting-todo-be:build
```

### Project Configuration
- Root project name: `handwriting-todo`
- Group: `com.iso`
- Version: `1.0.0`
- Subprojects: `handwriting-todo-fe`, `handwriting-todo-be`