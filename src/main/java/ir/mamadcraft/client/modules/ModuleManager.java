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
        } catch (Exception e) {
            System.out.println("[MamadClient] config load failed: " + e);
        }
    }

    public static void save() {
        try (FileOutputStream out = new FileOutputStream(file())) {
            Properties p = new Properties();
            for (Module m : MODULES) p.setProperty(m.name, String.valueOf(m.enabled));
            p.store(out, "Mamad Client");
        } catch (Exception e) {
            System.out.println("[MamadClient] config save failed: " + e);
        }
    }
}
