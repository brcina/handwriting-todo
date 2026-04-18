package hand.writing.todo.service;

import lombok.extern.java.Log;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Log
@SpringBootTest
class HandwritingToDoServiceTest {

    @Autowired
    private HandwritingToDoService service;

    static Stream<Arguments> images() {
        return Stream.of(
                Arguments.of("todo-040426-optimized.jpg", "- [ ] Wandern Frühling Alb"),
                Arguments.of("todo-180426-optimized.jpg", "- [ ] Gras bestellen")
        );
    }

    @ParameterizedTest
    @MethodSource("images")
    void convertToMarkdown(String imageName, String expectedTodo) throws IOException {
        // given
        Media image = new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource(imageName));
        // when
        Flux<String> chunks = service.convertToMarkdown(image);
        String text = String.join("", Objects.requireNonNull(chunks.collectList().block()));
        // then
        log.info("\n[" + imageName + "]\n" + text);
        assertThat(text).contains(expectedTodo);
        writeResult(imageName, text);
    }

    private void writeResult(String imageName, String text) throws IOException {
        Path outputDir = Path.of("test-output");
        Files.createDirectories(outputDir);
        String fileName = imageName.replace(".jpg", ".md");
        Files.writeString(outputDir.resolve(fileName), text);
    }
}