package hand.writing.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HandwritingToDoService {

    @Value("classpath:/prompts/handwriting-todo-system.st")
    private Resource systemPromptResource;

    private final AIService aiService;

    public Flux<String> convertToMarkdown(Media image) {
        Objects.requireNonNull(image);
        SystemMessage systemMessage = SystemMessage.builder().text(systemPromptResource).build();
        UserMessage imageMessage = UserMessage.builder().text("[BILD]").media(image).build();
        Prompt prompt = new Prompt(systemMessage, imageMessage);
        return aiService.askStream(prompt);
    }
}
