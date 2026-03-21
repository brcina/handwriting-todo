package hand.writing.todo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.hibernate.internal.log.SubSystemLogging;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Log
@SpringBootTest
class AIServiceTest {

    @Autowired
    private AIService aiService;

    @Test
    void ask() {
        //when
        var question = "Tell me a joke about a penguin";
        var response = aiService.ask(question);
        //then
        log.info(response);
        assertThat(response).contains("penguin");
    }

    @Test
    void askStream() {
        //when
        var question = "Tell me a joke about a penguin";
        var chunks = aiService.askStream(question).collectList().block();
        //then
        assertThat(chunks).isNotEmpty();
        var fullText = chunks.stream()
                .map(r -> r.getResult().getOutput().getText())
                .collect(java.util.stream.Collectors.joining());
        log.info(fullText);
        assertThat(fullText).contains("penguin");
    }

    @Test
    void askAboutPicture_alphabetPicture() {
        // given
        var imageData = new ClassPathResource("/abc-test.jpg");
        var media = new Media(MimeTypeUtils.IMAGE_JPEG, imageData);
        // when
        var question = "Explain what do you see in this picture?";
        var response = aiService.askAboutPicture(question, media);
        // then
        String text = response.getResult().getOutput().getText();
        log.info(text);
        assertThat(text).contains("alphabet");
    }

    @Test
    void askAboutPicture_alphabetPictureStream() {
        // given
        var imageData = new ClassPathResource("/abc-test.jpg");
        var media = new Media(MimeTypeUtils.IMAGE_JPEG, imageData);
        // when
        var question = "Explain what do you see in this picture? Are you seeing an alphabet ?";
        var chunks = aiService.askAboutPictureStream(question, media).collectList().block();
        // then
        assertThat(chunks).isNotEmpty();
        var text = chunks.stream()
                .map(r -> r.getResult().getOutput().getText())
                .collect(java.util.stream.Collectors.joining());
        log.info(text);
        assertThat(text).contains("alphabet");
    }


}