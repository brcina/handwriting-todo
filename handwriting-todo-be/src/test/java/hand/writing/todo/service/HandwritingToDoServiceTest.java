package hand.writing.todo.service;

import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Log
@SpringBootTest
class HandwritingToDoServiceTest {

    @Value("classpath:/todo-040426-optimized.jpg")
    private Resource image;


    @Autowired
    private HandwritingToDoService service;

    @Test
    void convertToMarkdown() {
        // when
        Flux<String> chuncks = service.convertToMarkdown(new Media(MimeTypeUtils.IMAGE_JPEG, image));
        // then
        String text = String.join("", Objects.requireNonNull(chuncks.collectList().block()));
        log.info(text);
        assertThat(text).contains("- [ ] Wandern Frühling Alb");
    }
}