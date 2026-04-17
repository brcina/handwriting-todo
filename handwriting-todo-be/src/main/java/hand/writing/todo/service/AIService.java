package hand.writing.todo.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log
@Service
@RequiredArgsConstructor
public class AIService {
    private final OllamaChatModel chatModel;


    public String ask(String system, String question) {
        return ask(system, question, null);
    }

    public String ask(String system, String question, @Nullable  Media media) {
        SystemMessage systemMessage = SystemMessage.builder().text(system).build();
        var userMessageBuilder = UserMessage.builder().text(question);
        if(media != null) {
            userMessageBuilder.media(media);
        }
        var userMessage = userMessageBuilder.build();
        return Objects.requireNonNull(
                this.chatModel.call(new Prompt(List.of(systemMessage, userMessage))).getResult().getOutput().getText()
        );
    }

    public Flux<String> askStream(String system, String question) {
        return askStream(system, question, null);
    }

    public Flux<String> askStream(String system, String question, Media media) {
        SystemMessage systemMessage = SystemMessage.builder().text(system).build();
        var userMessageBuilder = UserMessage.builder().text(question);
        if(media != null) {
            userMessageBuilder.media(media);
        }
        var userMessage = userMessageBuilder.build();
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return askStream(prompt);
    }

    public Flux<String> askStream(Prompt prompt) {
        return chatModel.stream(prompt)
                .map(r -> r.getResult().getOutput().getText())
                .filter(Objects::nonNull);
    }

}
