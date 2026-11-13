package ir.mamadcraft.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** تصویرهای PNG سایت (پیش‌نمایش و تکسچر کیپ‌ها) را در پس‌زمینه می‌گیرد و به تکسچر ماینکرفت تبدیل می‌کند. */
public final class ImageCache {
    public static final class Img {
        public final ResourceLocation loc;
        public final int w, h;
        Img(ResourceLocation loc, int w, int h) { this.loc = loc; this.w = w; this.h = h; }
    }

    private static final String ALLOWED_PREFIX = "https://m8380139-alt.github.io/mamadcraft/";
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private static final Map<String, Img> done = new HashMap<String, Img>();          // فقط ترد اصلی
    private static final Set<String> loading = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> failed = Collections.synchronizedSet(new HashSet<String>());
    private static ExecutorService pool;

    private ImageCache() {}

    private static synchronized ExecutorService pool() {
        if (pool == null) {
            pool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "MamadClient-Images");
                t.setDaemon(true);
                return t;
            });
        }
        return pool;
    }

    /** دوباره‌تلاشِ آدرس‌هایی که قبلاً شکست خورده‌اند (مثلاً بعد از وصل شدن VPN). */
    public static void retryFailed() { failed.clear(); }

    /** از ترد اصلی صدا بزن. اگر هنوز آماده نباشد null برمی‌گرداند و دانلود را شروع می‌کند. */
    public static Img get(final String url) {
        if (url == null || !url.startsWith(ALLOWED_PREFIX) || !url.endsWith(".png")) return null;
        Img i = done.get(url);
        if (i != null) return i;
        if (!failed.contains(url) && loading.add(url)) {
            pool().submit(new Runnable() {
                public void run() {
                    try {
                        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                        c.setConnectTimeout(8000);
                        c.setReadTimeout(8000);
                        c.setRequestProperty("User-Agent", "MamadClient/1.0");
                        if (c.getResponseCode() != 200) throw new RuntimeException("http " + c.getResponseCode());
                        InputStream in = c.getInputStream();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int n, total = 0;
                        while ((n = in.read(buf)) > 0) {
                            total += n;
                            if (total > MAX_BYTES) throw new RuntimeException("too big");
                            out.write(buf, 0, n);
                        }
                        in.close();
                        final BufferedImage img = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
                        if (img == null) throw new RuntimeException("bad png");
                        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                            public void run() {
                                DynamicTexture tex = new DynamicTexture(img);
                                ResourceLocation rl = Minecraft.getMinecraft().getTextureManager()
                                        .getDynamicTextureLocation("mamadimg", tex);
                                done.put(url, new Img(rl, img.getWidth(), img.getHeight()));
                            }
                        });
                    } catch (Exception e) {
                        failed.add(url);
                    } finally {
                        loading.remove(url);
                    }
                }
            });
        }
        return null;
    }
}
