package ir.mamadcraft.client.modules;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** لیست ماژول‌های HUD و ذخیره/خواندن روشن‌وخاموش بودنشان. */
public final class ModuleManager {
    public static final class Module {
        public final String name;
        public boolean enabled;
        Module(String name, boolean enabled) { this.name = name; this.enabled = enabled; }
    }

    public static final List<Module> MODULES = new ArrayList<Module>();
    public static final Module FPS = add("FPS", true);
    public static final Module CPS = add("CPS", true);
    public static final Module PING = add("Ping", true);
    public static final Module COORDS = add("Coordinates", false);
    public static final Module KEYSTROKES = add("Keystrokes", false);
    public static final Module ARMOR = add("Armor Status", false);
    public static final Module POTIONS = add("Potion Effects", false);
    public static final Module REACH = add("Reach Display", false);
    public static final Module SPRINT = add("Toggle Sprint", false);
    public static final Module SNEAK = add("Toggle Sneak", false);
    public static final Module FULLBRIGHT = add("Fullbright", false);
    public static final Module NOHURT = add("No Hurt Cam", false);
    public static final Module CROSSHAIR = add("Custom Crosshair", false);
    public static final Module CHAT = add("Chat Tweaks", false);
    public static final Module FRIENDS = add("Friend/Enemy Colors", false);

    public static int crossColor = 0xFF7A00;
    public static String crossShape = "cross";
    public static boolean hideSystem = false, filterSpam = true;
    public static final java.util.Set<String> friends = new java.util.LinkedHashSet<String>();
    public static final java.util.Set<String> enemies = new java.util.LinkedHashSet<String>();

    private static void loadSet(String v, java.util.Set<String> set) {
        set.clear();
        if (v == null) return;
        for (String x : v.split(",")) if (!x.trim().isEmpty()) set.add(x.trim().toLowerCase());
    }

    private static String join(java.util.Set<String> set) {
        StringBuilder b = new StringBuilder();
        for (String x : set) { if (b.length() > 0) b.append(','); b.append(x); }
        return b.toString();
    }

    private ModuleManager() {}

    private static Module add(String name, boolean def) {
        Module m = new Module(name, def);
        MODULES.add(m);
        return m;
    }

    private static File file() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        dir.mkdirs();
        return new File(dir, "mamadclient.properties");
    }

    public static void load() {
        File f = file();
        if (!f.exists()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            for (Module m : MODULES) {
                String v = p.getProperty(m.name);
                if (v != null) m.enabled = Boolean.parseBoolean(v);
            }
            try { String c = p.getProperty("crosshair.color"); if (c != null) crossColor = Integer.parseInt(c, 16) & 0xFFFFFF; } catch (Exception ignored) {}
            String sh = p.getProperty("crosshair.shape"); if (sh != null) crossShape = sh;
            hideSystem = Boolean.parseBoolean(p.getProperty("chat.hidesystem", "false"));
            filterSpam = Boolean.parseBoolean(p.getProperty("chat.spam", "true"));
            loadSet(p.getProperty("friends"), friends);
            loadSet(p.getProperty("enemies"), enemies);
        } catch (Exception e) {
            System.out.println("[MamadClient] config load failed: " + e);
        }
    }

    public static void save() {
        try (FileOutputStream out = new FileOutputStream(file())) {
            Properties p = new Properties();
            for (Module m : MODULES) p.setProperty(m.name, String.valueOf(m.enabled));
            p.setProperty("crosshair.color", Integer.toHexString(crossColor));
            p.setProperty("crosshair.shape", crossShape);
            p.setProperty("chat.hidesystem", String.valueOf(hideSystem));
            p.setProperty("chat.spam", String.valueOf(filterSpam));
            p.setProperty("friends", join(friends));
            p.setProperty("enemies", join(enemies));
            p.store(out, "Mamad Client");
        } catch (Exception e) {
            System.out.println("[MamadClient] config save failed: " + e);
        }
    }
}
