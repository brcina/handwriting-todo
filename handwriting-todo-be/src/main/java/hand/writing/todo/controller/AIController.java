package hand.writing.todo.controller;

import hand.writing.todo.service.AIService;
import hand.writing.todo.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/ask")
    public Mono<Map<String, String>> ask(
            @RequestParam(value = "system") String system,
            @RequestParam(value = "message") String message
    ) {
        return Mono.fromCallable(() -> Map.of("answer", aiService.ask(system, message)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/askStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askStream(
            @RequestParam(value = "system") String system,
            @RequestParam(value = "message") String message
    ) {
        return aiService.askStream(system, message)
                .map(text -> Map.of("answer", text));
    }

    @PostMapping(value = "/askAboutPicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Map<String, String>> askAboutPicture(
            @RequestPart("system") String system,
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return Utils.readMedia(file)
                .flatMap(media ->
                        Mono.fromCallable(() -> aiService.ask(system, message, media))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(answer -> Map.of("answer", answer));
    }

    @PostMapping(value = "/askAboutPictureStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askAboutPictureStream(
            @RequestPart("system") String system,
            @RequestPart("message") String message,
            @RequestPart("file") FilePart file) {

        return Utils.readMedia(file)
                .flatMapMany(media -> aiService.askStream(system, message, media))
                .map(text -> Map.of("answer", text));
    }

}