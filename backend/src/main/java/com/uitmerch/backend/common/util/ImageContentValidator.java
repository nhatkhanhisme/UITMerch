package com.uitmerch.backend.common.util;

import com.uitmerch.backend.common.exception.ValidationException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class ImageContentValidator {
    private ImageContentValidator() {}

    public static void validate(byte[] bytes, String declaredType) {
        try {
            if ("image/webp".equals(declaredType)) {
                // The JDK has no WebP decoder. Validate RIFF length and the WebP chunk envelope.
                if (bytes.length < 30 || !ascii(bytes, 0, 4).equals("RIFF")
                    || !ascii(bytes, 8, 4).equals("WEBP")
                    || !java.util.Set.of("VP8 ", "VP8L", "VP8X").contains(ascii(bytes, 12, 4))
                    || unsignedInt(bytes, 4) + 8 != bytes.length) throw invalid();
                validateWebpChunks(bytes);
                return;
            }
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid();
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!("image/png".equals(declaredType) && "png".equalsIgnoreCase(format))
                        && !("image/jpeg".equals(declaredType) && "jpeg".equalsIgnoreCase(format))) throw invalid();
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 4096 || height > 4096) throw invalid();
                    if (reader.read(0) == null) throw invalid();
                } finally { reader.dispose(); }
            }
        } catch (ValidationException e) { throw e; }
        catch (Exception e) { throw invalid(); }
    }

    private static void validateWebpChunks(byte[] bytes) {
        int offset = 12;
        boolean image = false;
        while (offset < bytes.length) {
            if (offset > bytes.length - 8) throw invalid();
            String tag = ascii(bytes, offset, 4);
            long size = unsignedInt(bytes, offset + 4);
            if (size > bytes.length - offset - 8L) throw invalid();
            int data = offset + 8;
            switch (tag) {
                case "VP8X" -> {
                    if (size != 10) throw invalid();
                    dimensions(1 + unsigned24(bytes, data + 4), 1 + unsigned24(bytes, data + 7));
                }
                case "VP8L" -> {
                    if (size < 5 || (bytes[data] & 255) != 0x2f) throw invalid();
                    long bits = unsignedInt(bytes, data + 1);
                    dimensions(1 + (int) (bits & 0x3fff), 1 + (int) ((bits >> 14) & 0x3fff));
                    image = true;
                }
                case "VP8 " -> {
                    if (size < 10 || (bytes[data] & 1) != 0 || (bytes[data + 3] & 255) != 0x9d
                        || (bytes[data + 4] & 255) != 1 || (bytes[data + 5] & 255) != 0x2a) throw invalid();
                    dimensions(((bytes[data + 6] & 255) | (bytes[data + 7] & 255) << 8) & 0x3fff,
                        ((bytes[data + 8] & 255) | (bytes[data + 9] & 255) << 8) & 0x3fff);
                    image = true;
                }
                case "ANMF" -> {
                    if (size < 24) throw invalid();
                    dimensions(1 + unsigned24(bytes, data + 6), 1 + unsigned24(bytes, data + 9));
                    image = true;
                }
                default -> { }
            }
            offset += 8 + (int) size + (int) (size & 1);
        }
        if (offset != bytes.length || !image) throw invalid();
    }
    private static void dimensions(int width, int height) {
        if (width < 1 || height < 1 || width > 4096 || height > 4096) throw invalid();
    }
    private static int unsigned24(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | (bytes[offset + 1] & 255) << 8 | (bytes[offset + 2] & 255) << 16;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
    private static long unsignedInt(byte[] bytes, int offset) {
        return (bytes[offset] & 255L) | (bytes[offset + 1] & 255L) << 8
            | (bytes[offset + 2] & 255L) << 16 | (bytes[offset + 3] & 255L) << 24;
    }
    private static ValidationException invalid() {
        return new ValidationException("Image content does not match its type, is damaged, or exceeds 4096 pixels per side.");
    }
}
