package hand.writing.todo.utils;

import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class Utils {
    private Utils(){}

    public static Mono<byte[]> readBytes(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }

    public static Mono<Media> readMedia(FilePart file) {
        return readBytes(file).map(bytes -> new Media(
                Objects.requireNonNull(file.headers().getContentType()),
                new ByteArrayResource(bytes)
        ));
    }
}
