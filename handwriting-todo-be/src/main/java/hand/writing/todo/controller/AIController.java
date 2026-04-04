package hand.writing.todo.controller;

import hand.writing.todo.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/ai/ask")
    public Mono<Map<String, String>> ask(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Mono.fromCallable(() -> Map.of("answer", aiService.ask(message)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/ai/askStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return aiService.askStream(message)
                .map(r -> Map.of("answer", Objects.requireNonNull(r.getResult().getOutput().getText())));
    }

    @PostMapping(value = "/ai/askAboutPicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Map<String, String>> askAboutPicture(
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return readBytes(file)
                .flatMap(bytes -> {
                    Media media = new Media(
                            Objects.requireNonNull(file.headers().getContentType()),
                            new ByteArrayResource(bytes)
                    );
                    return Mono.fromCallable(() -> aiService.askAboutPicture(message, media))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .map(response -> Map.of("answer", Objects.requireNonNull(response.getResult().getOutput().getText())));
    }

    @PostMapping(value = "/ai/askAboutPictureStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askAboutPictureStream(
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return readBytes(file)
                .flatMapMany(bytes -> {
                    Media media = new Media(
                            Objects.requireNonNull(file.headers().getContentType()),
                            new ByteArrayResource(bytes)
                    );
                    return aiService.askAboutPictureStream(message, media);
                })
                .map(r -> Map.of("answer", Objects.requireNonNull(r.getResult().getOutput().getText())));
    }

    private Mono<byte[]> readBytes(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }
}