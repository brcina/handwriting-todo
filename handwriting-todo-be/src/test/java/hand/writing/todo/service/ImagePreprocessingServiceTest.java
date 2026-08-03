package hand.writing.todo.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagePreprocessingServiceTest {

    private final ImagePreprocessingService service = new ImagePreprocessingService();

    @Test
    void preprocess_downscalesGrayscalesAndShrinksPayload() throws IOException {
        // given
        String imageName = "todo-010426-optimized.jpg";
        byte[] original = readFixture(imageName);

        // when
        byte[] result = Mono.from(service.preprocess(original)).block();

        // then
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(image).isNotNull();
        assertThat(Math.max(image.getWidth(), image.getHeight())).isLessThanOrEqualTo(1024);
        assertThat(result.length).isLessThan(original.length);
        writeResult(imageName, result);
    }

    @Test
    void preprocess_corruptInput_throwsIllegalArgumentException() {
        // given
        byte[] corrupt = new byte[]{1, 2, 3, 4};

        // when / then
        assertThatThrownBy(() -> Mono.from(service.preprocess(corrupt)).block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] readFixture(String name) throws IOException {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            return in.readAllBytes();
        }
    }

    private void writeResult(String imageName, byte[] preprocessed) throws IOException {
        Path outputDir = Path.of("test-output");
        Files.createDirectories(outputDir);
        String fileName = imageName.replace(".jpg", "-preprocessed.jpg");
        Files.write(outputDir.resolve(fileName), preprocessed);
    }
}
