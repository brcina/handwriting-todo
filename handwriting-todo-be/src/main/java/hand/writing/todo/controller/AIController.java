package hand.writing.todo.controller;

import hand.writing.todo.service.AIService;
import hand.writing.todo.utils.Utils;
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
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/ai/ask")
    public Mono<Map<String, String>> ask(
            @RequestParam(value = "system") String system,
            @RequestParam(value = "message") String message
    ) {
        return Mono.fromCallable(() -> Map.of("answer", aiService.ask(system, message)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/ai/askStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askStream(
            @RequestParam(value = "system") String system,
            @RequestParam(value = "message") String message
    ) {
        return aiService.askStream(system, message)
                .map(r -> r.getResult().getOutput().getText())
                .filter(Objects::nonNull)
                .map(text -> Map.of("answer", text));
    }

    @PostMapping(value = "/ai/askAboutPicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Map<String, String>> askAboutPicture(
            @RequestPart("system") String system,
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return Utils.readBytes(file)
                .flatMap(bytes -> {
                    Media media = new Media(
                            Objects.requireNonNull(file.headers().getContentType()),
                            new ByteArrayResource(bytes)
                    );
                    return Mono.fromCallable(() -> aiService.ask(system, message, media))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .map(answer -> Map.of("answer", answer));
    }

    @PostMapping(value = "/ai/askAboutPictureStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askAboutPictureStream(
            @RequestPart("system") String system,
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return Utils.readBytes(file)
                .flatMapMany(bytes -> {
                    Media media = new Media(
                            Objects.requireNonNull(file.headers().getContentType()),
                            new ByteArrayResource(bytes)
                    );
                    return aiService.askStream(system, message, media);
                })
                .map(r -> r.getResult().getOutput().getText())
                .filter(Objects::nonNull)
                .map(text -> Map.of("answer", text));
    }


}