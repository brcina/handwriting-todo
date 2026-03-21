# handwriting-todo-fe

Angular 21 + Tailwind CSS Frontend für Handwriting Todo.

## Entwicklung

Voraussetzungen: Node.js, Backend läuft auf `:8080`

```bash
npm install
npm start
```

App läuft auf `http://localhost:4200`. `/api/*` wird automatisch zum Backend auf `:8080` geproxied (`proxy.conf.json`).

## Nützliche Kommandos

```bash
# Komponente generieren
ng generate component component-name

# Build (Produktions-Output nach dist/)
ng build

# Tests
ng test
```

## Docker

```bash
docker build -t handwriting-todo-fe .
```

Wird normalerweise über `docker compose up --build` im Root gestartet (Frontend auf `http://localhost`).