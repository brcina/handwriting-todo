# handwriting-todo-be

Spring Boot backend that converts handwritten TODO images to Markdown via an AI service (Ollama/vast.ai).

## Test-Stil

Tests folgen dem **given / when / then** Kommentarmuster:

```java
// given
Media image = new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource(imageName));
// when
Flux<String> chunks = service.convertToMarkdown(image);
String text = String.join("", Objects.requireNonNull(chunks.collectList().block()));
// then
assertThat(text).contains(expectedTodo);
```

- Zwischenvariablen explizit benennen (z.B. `Media image = ...` statt inline)
