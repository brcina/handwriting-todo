package hand.writing.todo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImagePreprocessingService {

    private static final float JPEG_QUALITY = 0.8f;
    private static final int THRESHOLD = 128;

    @Value("${handwriting.image.max-dimension:1024}")
    private int maxDimension = 1024;

    public Mono<byte[]> preprocess(byte[] original) {
        return Mono.fromCallable(() -> doPreprocess(original))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] doPreprocess(byte[] original) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
        if (source == null) {
            throw new IllegalArgumentException("Unsupported or corrupt image format");
        }
        BufferedImage gray = resizeAndGrayscale(source);
        normalizeAndThreshold(gray);
        return encodeJpeg(gray);
    }

    private BufferedImage resizeAndGrayscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, maxDimension / (double) Math.max(width, height));
        int targetWidth = (int) Math.round(width * scale);
        int targetHeight = (int) Math.round(height * scale);

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = target.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private void normalizeAndThreshold(BufferedImage gray) {
        byte[] pixels = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();

        int min = 255;
        int max = 0;
        for (byte pixel : pixels) {
            int value = pixel & 0xFF;
            if (value < min) min = value;
            if (value > max) max = value;
        }

        boolean flat = max == min;
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i] & 0xFF;
            int stretched = flat ? value : (value - min) * 255 / (max - min);
            pixels[i] = (byte) (stretched >= THRESHOLD ? 255 : 0);
        }
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
