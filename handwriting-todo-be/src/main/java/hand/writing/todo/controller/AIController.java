package hand.writing.todo.controller;

import hand.writing.todo.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/ai/ask")
    public Map<String,String> ask(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("answer", this.aiService.ask(message));
    }

    @GetMapping("/ai/askStream")
    public Flux<Map<String, String>> askStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return this.aiService.askStream(message)
                .map(r -> Map.of("answer", Objects.requireNonNull(r.getResult().getOutput().getText())));
    }

    @PostMapping("/ai/askAboutPicture")
    public Map<String, String> askAboutPicture(
            @RequestParam("message") String message,
            @RequestParam("file") MultipartFile file) throws IOException {

        Media media = new Media(
                MimeTypeUtils.parseMimeType(Objects.requireNonNull(file.getContentType())),
                new ByteArrayResource(file.getBytes())
        );
        ChatResponse response = aiService.askAboutPicture(message, media);
        return Map.of("answer", Objects.requireNonNull(response.getResult().getOutput().getText()));
    }

    @PostMapping(value = "/ai/askAboutPictureStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> askAboutPictureStream(
            @RequestParam("message") String message,
            @RequestParam("file") MultipartFile file) throws IOException {

        Media media = new Media(
                MimeTypeUtils.parseMimeType(Objects.requireNonNull(file.getContentType())),
                new ByteArrayResource(file.getBytes())
        );
        return aiService.askAboutPictureStream(message, media)
                .map(r -> Map.of("answer", Objects.requireNonNull(r.getResult().getOutput().getText())));
    }

}
