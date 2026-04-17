package hand.writing.todo.controller;

import hand.writing.todo.service.HandwritingToDoService;
import hand.writing.todo.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/handwriting")
@RequiredArgsConstructor
public class HandwritingToDoController {

    private final HandwritingToDoService service;

    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, String>> convert(@RequestPart("file") FilePart file) {
        return Utils.readMedia(file)
                .flatMapMany(service::convertToMarkdown)
                .map(text -> Map.of("answer", text));
    }


}
