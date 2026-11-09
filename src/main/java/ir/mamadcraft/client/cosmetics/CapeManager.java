package ir.mamadcraft.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * کیپ هر بازیکن را با اسم ماینکرفتش از سایت ممدکرفت می‌خواند (نمای عمومی player_capes).
 * فقط تکسچرهایی که از سایت خودمان باشند قبول می‌شوند.
 */
public final class CapeManager {
    private static final String GRAPHQL_URL = "https://vvtyudilywndoyxqiqkm.graphql.eu-central-1.nhost.run/v1";
    private static final String ALLOWED_PREFIX = "https://m8380139-alt.github.io/mamadcraft/";
    private static final long REFRESH_MS = 5 * 60 * 1000L;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    // نام (حروف کوچک) -> مکان تکسچر آمادهٔ نمایش
    private static final Map<String, ResourceLocation> capes = new HashMap<String, ResourceLocation>();
    private static final Map<String, Long> lastRequest = new HashMap<String, Long>();
    private static final Set<String> busy = Collections.synchronizedSet(new HashSet<String>());
    private static ExecutorService pool;

    private CapeManager() {}

    public static void init() {
        pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MamadClient-Capes");
            t.setDaemon(true);
            return t;
        });
    }

    /** از ترد رندر صدا زده می‌شود؛ سریع برمی‌گردد و اگر لازم باشد دانلود را در پس‌زمینه شروع می‌کند. */
    public static ResourceLocation get(String playerName) {
        if (playerName == null || pool == null) return null;
        final String name = playerName.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Long last = lastRequest.get(name);
        if ((last == null || now - last > REFRESH_MS) && busy.add(name)) {
            lastRequest.put(name, now);
            pool.submit(() -> {
                try {
                    fetch(name);
                } catch (Exception e) {
                    System.out.println("[MamadClient] cape fetch failed for " + name + ": " + e);
                } finally {
                    busy.remove(name);
                }
            });
        }
        return capes.get(name);
    }

    private static void fetch(final String name) throws Exception {
        String query = "query($n:String!){player_capes(where:{name:{_eq:$n},category:{_eq:\"cape\"}}){texture_url}}";
        JsonObject vars = new JsonObject();
        vars.addProperty("n", name);
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
        JsonArray arr = res.getAsJsonObject("data").getAsJsonArray("player_capes");
        if (arr == null || arr.size() == 0) {
            runOnMain(new Runnable() { public void run() { capes.remove(name); } });
            return;
        }
        JsonElement u = arr.get(0).getAsJsonObject().get("texture_url");
        if (u == null || u.isJsonNull()) return;
        final String url = u.getAsString();
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
                capes.put(name, rl);
            }
        });
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
