package ir.mamadcraft.client.cosmetics;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * خواندن امن PNG: قبل از decode کردن، ابعاد را از هدر چک می‌کند تا فایل‌های «بمب تصویری»
 * (حجم کم ولی ابعاد غول‌پیکر) حافظهٔ بازی را پر نکنند.
 */
public final class SafeImage {
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;

    private SafeImage() {}

    /** null برمی‌گرداند اگر فایل PNG معتبر نباشد یا ابعادش از حد بیشتر باشد. */
    public static BufferedImage read(byte[] data, int maxDim) throws IOException {
        if (data == null || data.length == 0 || data.length > MAX_FILE_BYTES) return null;
        return decode(ImageIO.createImageInputStream(new ByteArrayInputStream(data)), maxDim);
    }

    public static BufferedImage read(File f, int maxDim, long maxBytes) throws IOException {
        if (f == null || !f.isFile() || f.length() > maxBytes) return null;
        return decode(ImageIO.createImageInputStream(f), maxDim);
    }

    private static BufferedImage decode(ImageInputStream in, int maxDim) throws IOException {
        if (in == null) return null;
        try {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
            if (!it.hasNext()) return null;
            ImageReader reader = it.next();
            try {
                reader.setInput(in, false, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w > maxDim || h > maxDim) return null;
                return reader.read(0);
            } catch (IOException | RuntimeException e) {
                return null;   // فایل خراب یا غیر PNG: مثل قبل «بدون تصویر» حساب می‌شود
            } finally {
                reader.dispose();
            }
        } finally {
            try { in.close(); } catch (IOException ignored) {}
        }
    }
}
