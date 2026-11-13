package ir.mamadcraft.client.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

/**
 * ورود به همان حساب سایت MamadCraft (Nhost). نام کاربری حساب سایت و نام بازی یکی است؛
 * ایمیل ساختگی = نام‌کاربری@mamadcraft.example (مثل خود سایت).
 */
public final class SiteAccount {
    private static final String SUB = "vvtyudilywndoyxqiqkm";
    private static final String REGION = "eu-central-1";
    private static final String AUTH_URL = "https://" + SUB + ".auth." + REGION + ".nhost.run/v1";
    private static final String FAKE_DOMAIN = "mamadcraft.example";
    public static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public interface Callback { void done(boolean ok, String message); }

    private static final ExecutorService pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "MamadClient-account");
            t.setDaemon(true);
            return t;
        }
    });

    private static volatile String username;
    private static volatile String refreshToken;
    private static volatile String accessToken;
    private static volatile String userId;
    private static volatile long expiresAt;
    private static boolean loaded;

    private SiteAccount() {}

    private static File file() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        dir.mkdirs();
        return new File(dir, "mamadclient_account.json");
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (!f.isFile()) return;
            JsonObject o = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            username = o.get("username").getAsString();
            refreshToken = o.get("refreshToken").getAsString();
        } catch (Exception e) {
            username = null;
            refreshToken = null;
        }
    }

    public static boolean isLoggedIn() {
        load();
        return username != null && refreshToken != null;
    }

    public static String getUsername() {
        load();
        return username;
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("username", username);
            o.addProperty("refreshToken", refreshToken);
            Files.write(file().toPath(), o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public static void signOut() {
        load();
        username = null;
        refreshToken = null;
        accessToken = null;
        userId = null;
        try { file().delete(); } catch (Exception ignored) {}
    }

    /** ورود با نام کاربری و رمز؛ نتیجه روی ترد اصلی بازی برمی‌گردد. */
    public static void signIn(final String name, final String password, final Callback cb) {
        pool.submit(new Runnable() {
            public void run() {
                boolean ok = false;
                String msg;
                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("email", name.toLowerCase(Locale.ROOT) + "@" + FAKE_DOMAIN);
                    body.addProperty("password", password);
                    String[] res = post(AUTH_URL + "/signin/email-password", body);
                    int code = Integer.parseInt(res[0]);
                    if (code == 200) {
                        apply(new JsonParser().parse(res[1]).getAsJsonObject(), name);
                        ok = true;
                        msg = "Signed in as " + username;
                    } else {
                        msg = describe(code, res[1]);
                    }
                } catch (Exception e) {
                    msg = "Can't reach the server. Check internet / VPN.";
                }
                final boolean fok = ok;
                final String fmsg = msg;
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    public void run() { cb.done(fok, fmsg); }
                });
            }
        });
    }

    private static void apply(JsonObject json, String fallbackName) {
        JsonObject s = json.has("session") && json.get("session").isJsonObject() ? json.getAsJsonObject("session") : json;
        accessToken = s.get("accessToken").getAsString();
        refreshToken = s.get("refreshToken").getAsString();
        long ttl = s.has("accessTokenExpiresIn") ? s.get("accessTokenExpiresIn").getAsLong() : 900L;
        expiresAt = System.currentTimeMillis() + ttl * 1000L;
        String shown = fallbackName;
        if (s.has("user") && s.get("user").isJsonObject()) {
            JsonObject u = s.getAsJsonObject("user");
            if (u.has("id")) userId = u.get("id").getAsString();
            if (u.has("displayName") && !u.get("displayName").isJsonNull() && !u.get("displayName").getAsString().isEmpty()) {
                shown = u.get("displayName").getAsString();
            }
        }
        username = shown;
        loaded = true;
        save();
    }

    private static String describe(int code, String body) {
        String err = "";
        try { err = new JsonParser().parse(body).getAsJsonObject().get("error").getAsString(); } catch (Exception ignored) {}
        if (err.equals("invalid-email-password") || code == 401) return "Wrong username or password.";
        if (err.equals("unverified-user")) return "This account isn't verified yet.";
        if (err.equals("disabled-user")) return "This account is disabled.";
        return "Sign-in failed (" + (err.isEmpty() ? code : err) + ").";
    }

    /** @return {httpCode, body} */
    private static String[] post(String url, JsonObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("User-Agent", "MamadClient/1.0");
        OutputStream os = c.getOutputStream();
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = in == null ? "" : new String(readAll(in), StandardCharsets.UTF_8);
        return new String[]{String.valueOf(code), text};
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }
}
