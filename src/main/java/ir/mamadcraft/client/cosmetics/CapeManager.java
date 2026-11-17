package ir.mamadcraft.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * کیپ هر بازیکن را با اسم ماینکرفتش از سایت ممدکرفت می‌خواند (نمای عمومی player_capes).
 * فقط تکسچرهایی که از سایت خودمان باشند قبول می‌شوند.
 */
public final class CapeManager {
    private static final String GRAPHQL_URL = "https://vvtyudilywndoyxqiqkm.graphql.eu-central-1.nhost.run/v1";
    private static final String ALLOWED_PREFIX = "https://m8380139-alt.github.io/mamadcraft/";
    private static final long POLL_MS = 3000L;   // هر چند میلی‌ثانیه کیپ‌ها از سایت چک شوند
    private static final long SEEN_MS = 15000L;  // بازیکنی که تا این مدت دیده نشده از چک حذف می‌شود
    private static final int MAX_NAMES = 60;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    // نام (حروف کوچک) -> مکان تکسچر آمادهٔ نمایش
    private static final Map<String, ResourceLocation> capes = new ConcurrentHashMap<String, ResourceLocation>();
    // بعد از هر تلاش دانلود، تا این زمان دوباره برای همان آدرس تلاش نمی‌کنیم (جلوگیری از اسپم شبکه)
    private static final Map<String, Long> retryAfter = new ConcurrentHashMap<String, Long>();
    private static final long RETRY_MS = 30000L;
    // آدرس کیپی که الان برای هر نام نمایش داده می‌شود (برای دانلود دوباره‌ی بی‌مورد)
    private static final Map<String, String> capeUrls = new ConcurrentHashMap<String, String>();
    // نام (حروف کوچک) -> آخرین باری که بازیکن دیده شد
    private static final Map<String, Long> seen = new ConcurrentHashMap<String, Long>();
    private static volatile boolean pollSoon;
    private static final Set<String> busy = Collections.synchronizedSet(new HashSet<String>());
    private static ExecutorService pool;

    // پیش‌نمایش داخل منو: کیپ انتخاب‌شده روی بازیکن خودمان (فقط تا وقتی منو بازه)
    public static volatile ResourceLocation previewOverride;
    public static volatile String previewFor;

    private CapeManager() {}

    public static void init() {
        pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MamadClient-Capes");
            t.setDaemon(true);
            return t;
        });
        Thread poller = new Thread(new Runnable() { public void run() { pollLoop(); } }, "MamadClient-CapePoll");
        poller.setDaemon(true);
        poller.start();
    }

    /** از ترد رندر صدا زده می‌شود؛ فقط بازیکن را ثبت می‌کند و تکسچر آماده را برمی‌گرداند. */
    public static ResourceLocation get(String playerName) {
        if (playerName == null || pool == null) return null;
        final String name = playerName.toLowerCase(Locale.ROOT);
        ResourceLocation over = previewOverride;
        if (over != null && previewFor != null && previewFor.equalsIgnoreCase(playerName)) return over;
        if (seen.put(name, System.currentTimeMillis()) == null) pollSoon = true; // بازیکن تازه: زود چک کن
        return capes.get(name);
    }

    private static void pollLoop() {
        while (true) {
            try {
                pollOnce();
            } catch (Exception e) {
                System.out.println("[MamadClient] cape poll failed: " + e);
            }
            for (int i = 0; i < 15 && !pollSoon; i++) {
                try { Thread.sleep(POLL_MS / 15); } catch (InterruptedException ie) { return; }
            }
            pollSoon = false;
        }
    }

    /** کیپ همهٔ بازیکن‌هایی که اخیراً دیده شده‌اند را با یک درخواست می‌خواند. */
    private static void pollOnce() throws Exception {
        long now = System.currentTimeMillis();
        final List<String> list = new ArrayList<String>();
        JsonArray names = new JsonArray();
        for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > SEEN_MS) { it.remove(); continue; }
            if (list.size() < MAX_NAMES) { list.add(e.getKey()); names.add(new JsonPrimitive(e.getKey())); }
        }
        if (list.isEmpty()) return;

        String query = "query($n:[String!]!){player_capes(where:{name:{_in:$n},category:{_eq:\"cape\"}}){name texture_url}}";
        JsonObject vars = new JsonObject();
        vars.add("n", names);
        JsonObject body = new JsonObject();
        body.addProperty("query", query);
        body.add("variables", vars);

        HttpURLConnection c = (HttpURLConnection) new URL(GRAPHQL_URL).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("User-Agent", "MamadClient/1.0");
        OutputStream os = c.getOutputStream();
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        if (c.getResponseCode() != 200) return;
        JsonObject res = new JsonParser().parse(new String(readAll(c.getInputStream()), StandardCharsets.UTF_8)).getAsJsonObject();
        if (!res.has("data") || res.get("data").isJsonNull()) return;
        JsonArray arr = res.getAsJsonObject("data").getAsJsonArray("player_capes");
        if (arr == null) return;

        Map<String, String> urls = new HashMap<String, String>();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            JsonElement n = o.get("name"), u = o.get("texture_url");
            if (n == null || n.isJsonNull() || u == null || u.isJsonNull()) continue;
            String key = n.getAsString().toLowerCase(Locale.ROOT);
            if (!urls.containsKey(key)) urls.put(key, u.getAsString());
        }
        for (final String name : list) {
            final String url = urls.get(name);
            if (url == null) {
                if (!capes.containsKey(name)) continue;
                runOnMain(new Runnable() { public void run() {
                    capeUrls.remove(name);
                    release(capes.remove(name));
                } });
            } else if (!url.equals(capeUrls.get(name)) && now >= retryAfter.getOrDefault(name + "|" + url, 0L) && busy.add(name)) {
                pool.submit(new Runnable() { public void run() {
                    try {
                        download(name, url);
                    } catch (Exception e) {
                        System.out.println("[MamadClient] cape download failed for " + name + ": " + e);
                    } finally {
                        retryAfter.put(name + "|" + url, System.currentTimeMillis() + RETRY_MS);
                        busy.remove(name);
                    }
                } });
            }
        }
    }

    private static void download(final String name, final String url) throws Exception {
        if (!url.startsWith(ALLOWED_PREFIX) || !url.endsWith(".png")) return;

        HttpURLConnection ic = (HttpURLConnection) new URL(url).openConnection();
        ic.setConnectTimeout(8000);
        ic.setReadTimeout(15000);
        ic.setRequestProperty("User-Agent", "MamadClient/1.0");
        if (ic.getResponseCode() != 200) return;
        final BufferedImage img = ImageIO.read(new ByteArrayInputStream(readAll(ic.getInputStream())));
        if (img == null) return;
        int w = img.getWidth(), h = img.getHeight();
        // فرمت استاندارد کیپ: نسبت ۲:۱ و مضرب ۶۴×۳۲ (حداکثر ۱۰۲۴×۵۱۲)
        if (w < 64 || w > 1024 || w != h * 2 || w % 64 != 0) return;

        runOnMain(new Runnable() {
            public void run() {
                DynamicTexture tex = new DynamicTexture(img);
                ResourceLocation rl = Minecraft.getMinecraft().getTextureManager()
                        .getDynamicTextureLocation("mamadcape_" + name, tex);
                release(capes.put(name, rl));
                capeUrls.put(name, url);
            }
        });
    }

    /** بعد از عوض شدن کیپ، دفعهٔ بعد بدون انتظار از سایت دوباره بخوان (ترد اصلی). */
    public static void invalidate(String playerName) {
        if (playerName != null) { seen.put(playerName.toLowerCase(Locale.ROOT), System.currentTimeMillis()); pollSoon = true; }
    }

    /** تکسچر قبلی را از حافظهٔ کارت گرافیک آزاد می‌کند. */
    private static void release(ResourceLocation old) {
        if (old != null) Minecraft.getMinecraft().getTextureManager().deleteTexture(old);
    }

    private static void runOnMain(Runnable r) {
        Minecraft.getMinecraft().addScheduledTask(r);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n, total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_BYTES) throw new Exception("too large");
            out.write(buf, 0, n);
        }
        in.close();
        return out.toByteArray();
    }
}
