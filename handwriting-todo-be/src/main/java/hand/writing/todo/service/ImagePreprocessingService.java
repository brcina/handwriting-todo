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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Service
public class ImagePreprocessingService {

    private static final long CONVERT_TIMEOUT_SECONDS = 30;
    private static final float JPEG_QUALITY = 0.5f;

    @Value("${handwriting.image.max-dimension:1024}")
    private int maxDimension = 1024;

    public Mono<byte[]> preprocess(byte[] original) {
        return Mono.fromCallable(() -> doPreprocess(original))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * ImageMagick handles the EXIF auto-orient, grayscale conversion and downscaling
     * (all things the plain JDK/ImageIO stack can't do, or can't do well). Binarization
     * stays in Java via Otsu's method: this ImageMagick build has no -auto-threshold
     * support, and a fixed threshold washes out faint strokes on overexposed photos.
     */
    private byte[] doPreprocess(byte[] original) throws IOException, InterruptedException {
        byte[] grayscale = runConvert(original);
        BufferedImage gray = readGray(grayscale);
        binarize(gray);
        return encodeJpeg(gray);
    }

    private byte[] runConvert(byte[] original) throws IOException, InterruptedException {
        Path input = Files.createTempFile("handwriting-input-", ".jpg");
        Path output = Files.createTempFile("handwriting-output-", ".jpg");
        try {
            Files.write(input, original);

            ProcessBuilder builder = new ProcessBuilder(
                    "convert",
                    input.toString(),
                    "-auto-orient",
                    // Not required for Otsu's correctness (readGray() falls back to converting
                    // color input), but keeps resize/encode down to 1 channel instead of 3.
                    "-colorspace", "Gray",
                    "-resize", maxDimension + "x" + maxDimension + ">",
                    output.toString()
            ).redirectErrorStream(true);

            Process process = builder.start();
            boolean finished = process.waitFor(CONVERT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("Image preprocessing timed out");
            }
            if (process.exitValue() != 0) {
                String errorOutput = new String(process.getInputStream().readAllBytes());
                throw new IllegalArgumentException("Unsupported or corrupt image format: " + errorOutput.trim());
            }
            return Files.readAllBytes(output);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    private BufferedImage readGray(byte[] grayscaleJpeg) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(grayscaleJpeg));
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return image;
        }
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(image, 0, 0, null);
        return gray;
    }

    /**
     * Binarizes via Otsu's method: the split threshold is derived from this image's own
     * histogram instead of a fixed value, so overall brightness (e.g. an overexposed photo)
     * doesn't wash out faint strokes the way a fixed-percentage threshold would.
     */
    private void binarize(BufferedImage gray) {
        byte[] pixels = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        int threshold = otsuThreshold(pixels);
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i] & 0xFF;
            pixels[i] = (byte) (value > threshold ? 255 : 0);
        }
    }

    private int otsuThreshold(byte[] pixels) {
        int[] histogram = new int[256];
        for (byte pixel : pixels) {
            histogram[pixel & 0xFF]++;
        }

        int total = pixels.length;
        long sumAll = 0;
        for (int i = 0; i < 256; i++) {
            sumAll += (long) i * histogram[i];
        }

        long sumBackground = 0;
        int weightBackground = 0;
        double maxVariance = -1;
        int bestThreshold = 128;

        for (int t = 0; t < 256; t++) {
            weightBackground += histogram[t];
            if (weightBackground == 0) {
                continue;
            }
            int weightForeground = total - weightBackground;
            if (weightForeground == 0) {
                break;
            }

            sumBackground += (long) t * histogram[t];
            double meanBackground = (double) sumBackground / weightBackground;
            double meanForeground = (double) (sumAll - sumBackground) / weightForeground;
            double meanDiff = meanBackground - meanForeground;
            double betweenVariance = (double) weightBackground * weightForeground * meanDiff * meanDiff;

            if (betweenVariance > maxVariance) {
                maxVariance = betweenVariance;
                bestThreshold = t;
            }
        }
        return bestThreshold;
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
