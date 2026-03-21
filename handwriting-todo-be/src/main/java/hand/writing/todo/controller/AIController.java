package hand.writing.todo.controller;

import hand.writing.todo.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/ai/ask")
    public Map<String,String> ask(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("answer", this.aiService.ask(message));
    }

    @GetMapping("/ai/askStream")
    public Flux<ChatResponse> askStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return this.aiService.askStream(message);
    }

}
