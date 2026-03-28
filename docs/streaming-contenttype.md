# Streaming & Content-Type im Frontend

## Das Problem

Spring gibt bei einem `Flux<T>`-Endpoint ohne `produces`-Angabe das gesamte Flux **als JSON-Array** zurück — erst wenn alle Elemente da sind:

```json
[{"answer":"Hallo"}, {"answer":" Welt"}]
```

Das ist kein Streaming, sondern eine gepufferte Antwort.

---

## GET Stream — `text/event-stream` + `EventSource`

**Backend** (Spring, automatisch bei `Flux` + GET):
```
Content-Type: text/event-stream

data: {"answer":"Hallo"}

data: {"answer":" Welt"}

```

**Frontend:**
```typescript
const source = new EventSource('/api/ai/askStream?message=...');
source.onmessage = e => {
  const chunk = JSON.parse(e.data); // e.data ist bereits ohne "data: "
  console.log(chunk.answer);
};
```

`EventSource` ist ein Browser-API das SSE automatisch parsed:
- schneidet `data: ` selbst ab
- gibt nur den reinen Inhalt in `e.data`
- **funktioniert nur mit GET**

---

## POST Stream — `application/x-ndjson` + `fetch`

POST mit Datei-Upload braucht einen Body — `EventSource` (GET only) fällt weg.

**Backend** (`produces` muss explizit gesetzt werden):
<!-- noinspection ALL -->    
```java
@PostMapping(value = "/ai/askAboutPictureStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
```

Wire-Format (Newline Delimited JSON):
```
{"answer":"Hallo"}\n
{"answer":" Welt"}\n
```

**Frontend:**
```typescript
const res = await fetch('/api/ai/askAboutPictureStream', { method: 'POST', body: form });
const reader = res.body!.getReader();
const decoder = new TextDecoder();
let buffer = '';

const read = () => reader.read().then(({ done, value }) => {
  if (done) return;
  buffer += decoder.decode(value, { stream: true });
  const lines = buffer.split('\n');
  buffer = lines.pop() ?? ''; // unvollständige letzte Zeile aufheben
  for (const line of lines) {
    if (!line.trim()) continue;
    const chunk = JSON.parse(line);
    console.log(chunk.answer);
  }
  read();
});
read();
```

### Warum der Buffer?

Ein einzelner `reader.read()`-Call kann **mehrere JSON-Zeilen** auf einmal liefern, oder eine Zeile kann auf **zwei Reads** aufgeteilt sein. Ohne Buffer würde `JSON.parse` auf unvollständigen oder zusammengesetzten Chunks scheitern.

---

## Vergleich

| | GET Stream | POST Stream |
|---|---|---|
| HTTP-Methode | GET | POST |
| Datei-Upload | nicht möglich | `multipart/form-data` |
| Content-Type | `text/event-stream` | `application/x-ndjson` |
| Frontend-API | `EventSource` | `fetch` + body reader |
| Parsing | automatisch (`e.data`) | manuell (Zeilensplitting) |
| `data: ` abschneiden | automatisch | nicht nötig (NDJSON hat kein Prefix) |

---

## Warum nicht SSE für POST?

Technisch wäre `produces = TEXT_EVENT_STREAM_VALUE` auch bei POST möglich. Aber dann müsste `fetch` `data: ` manuell abschneiden:

```typescript
const line = rawLine.startsWith('data: ') ? rawLine.slice(6) : rawLine;
```

NDJSON ist für `fetch`-Streaming die sauberere Wahl, da kein Prefix-Stripping nötig ist.