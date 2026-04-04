package hand.writing.todo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Log
@Service
@RequiredArgsConstructor
public class AIService {
    private final OllamaChatModel chatModel;

    public String ask(String question) {

        return chatModel.call(question);
    }

    public Flux<ChatResponse> askStream(String question) {
        Prompt prompt = new Prompt(new UserMessage(question));
        return chatModel.stream(prompt);
    }

    public ChatResponse askAboutPicture(String question, Media media) {
        var userMessage = UserMessage.builder()
                .text(question)
                .media(List.of(media))
                .build();
        return this.chatModel.call(new Prompt(List.of(userMessage)));
    }

    public Flux<ChatResponse> askAboutPictureStream(String question, Media media) {
        var userMessage = UserMessage.builder()
                .text(question)
                .media(List.of(media))
                .build();
        return this.chatModel.stream(new Prompt(List.of(userMessage)));
    }

}
