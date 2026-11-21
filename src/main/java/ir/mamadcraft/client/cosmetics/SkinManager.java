package ir.mamadcraft.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * اسکین دلخواه: کاربر یک فایل PNG انتخاب می‌کند، داخل پوشهٔ mamadclient/skins کپی می‌شود
 * و فقط روی بازیکن خودش (سمت کلاینت) نمایش داده می‌شود.
 */
public final class SkinManager {
    private static final long MAX_BYTES = 1024 * 1024;

    private static volatile ResourceLocation active;      // تکسچر آمادهٔ نمایش (فقط ترد اصلی می‌سازد)
    private static volatile boolean slim;                 // مدل دست باریک (Alex)
    private static volatile boolean busy;
    private static volatile String status = "";
    private static volatile boolean statusError;

    private SkinManager() {}

    // ---------------------------------------------------------------- وضعیت برای GUI

    public static ResourceLocation get() { return active; }
    public static boolean isSlim() { return slim; }
    public static boolean isBusy() { return busy; }
    public static boolean hasSkin() { return active != null; }
    public static String status() { return status; }
    public static boolean statusIsError() { return statusError; }
    public static String skinType() { return slim ? "slim" : "default"; }

    private static void setStatus(String s, boolean error) {
        status = s;
        statusError = error;
    }

    // ---------------------------------------------------------------- فایل‌ها

    private static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "mamadclient/skins");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    private static File skinFile() { return new File(dir(), "skin.png"); }
    private static File propsFile() { return new File(dir(), "skin.properties"); }

    // ---------------------------------------------------------------- شروع

    /** در شروع بازی، اسکین ذخیره‌شده را (اگر باشد) می‌خواند. */
    public static void init() {
        Thread t = new Thread(new Runnable() { public void run() { loadSaved(); } }, "MamadClient-SkinLoad");
        t.setDaemon(true);
        t.start();
    }

    private static void loadSaved() {
        try {
            File f = skinFile();
            if (!f.isFile()) return;
            Properties p = new Properties();
            File pf = propsFile();
            if (pf.isFile()) {
                InputStream in = new FileInputStream(pf);
                try { p.load(in); } finally { in.close(); }
            }
            slim = "true".equals(p.getProperty("slim"));
            BufferedImage img = ImageIO.read(f);
            if (validate(img) == null) apply(img);
        } catch (Exception e) {
            System.out.println("[MamadClient] skin load failed: " + e);
        }
    }

    // ---------------------------------------------------------------- انتخاب فایل

    /** پنجرهٔ انتخاب فایل را در یک ترد جدا باز می‌کند (بازی هنگ نمی‌کند). */
    public static void chooseFile() {
        if (busy) return;
        busy = true;
        setStatus("Choose a PNG file...", false);
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    File picked = showChooser();
                    if (picked == null) { setStatus("", false); return; }
                    importFile(picked);
                } catch (Throwable e) {
                    setStatus("Could not open the file window", true);
                    System.out.println("[MamadClient] skin chooser failed: " + e);
                } finally {
                    busy = false;
                }
            }
        }, "MamadClient-SkinPick");
        t.setDaemon(true);
        t.start();
    }

    private static File showChooser() throws Exception {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        JFileChooser fc = new JFileChooser() {
            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog d = super.createDialog(parent);
                d.setAlwaysOnTop(true);   // بازی تمام‌صفحه پنجره را نپوشاند
                return d;
            }
        };
        fc.setDialogTitle("Choose your Minecraft skin (.png)");
        fc.setFileFilter(new FileNameExtensionFilter("PNG skin (64x64 or 64x32)", "png"));
        fc.setAcceptAllFileFilterUsed(false);
        File start = new File(System.getProperty("user.home"), "Downloads");
        if (start.isDirectory()) fc.setCurrentDirectory(start);
        return fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    private static void importFile(File f) {
        try {
            if (!f.isFile() || !f.getName().toLowerCase().endsWith(".png")) { setStatus("Pick a .png file", true); return; }
            if (f.length() > MAX_BYTES) { setStatus("File is too big", true); return; }
            BufferedImage img = ImageIO.read(f);
            String err = validate(img);
            if (err != null) { setStatus(err, true); return; }

            File out = skinFile();
            if (!f.getCanonicalFile().equals(out.getCanonicalFile())) {
                InputStream in = new FileInputStream(f);
                OutputStream os = new FileOutputStream(out);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                } finally {
                    in.close();
                    os.close();
                }
            }
            apply(img);
            saveProps();
            setStatus("Skin applied", false);
        } catch (Exception e) {
            setStatus("Could not read this file", true);
            System.out.println("[MamadClient] skin import failed: " + e);
        }
    }

    /** null یعنی معتبر است؛ در غیر این صورت پیام خطا. */
    private static String validate(BufferedImage img) {
        if (img == null) return "This is not a valid PNG";
        int w = img.getWidth(), h = img.getHeight();
        if (w != 64 || (h != 64 && h != 32)) return "Size must be 64x64 or 64x32 (yours: " + w + "x" + h + ")";
        return null;
    }

    // ---------------------------------------------------------------- اعمال و بازنشانی

    /** اسکین‌های قدیمی ۶۴×۳۲ را همان‌طور که خود ماینکرفت انجام می‌دهد به ۶۴×۶۴ تبدیل می‌کند. */
    private static void apply(BufferedImage raw) {
        final BufferedImage img = new ImageBufferDownload().parseUserSkin(raw);
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                ResourceLocation old = active;
                DynamicTexture tex = new DynamicTexture(img);
                active = mc.getTextureManager().getDynamicTextureLocation("mamadskin", tex);
                if (old != null && !old.equals(active)) mc.getTextureManager().deleteTexture(old);
            }
        });
    }

    public static void setSlim(boolean value) {
        slim = value;
        saveProps();
    }

    /** اسکین دلخواه را حذف می‌کند و بازیکن به اسکین اصلی برمی‌گردد. */
    public static void reset() {
        final ResourceLocation old = active;
        active = null;
        if (old != null) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                public void run() { Minecraft.getMinecraft().getTextureManager().deleteTexture(old); }
            });
        }
        File f = skinFile();
        if (f.isFile()) f.delete();
        File pf = propsFile();
        if (pf.isFile()) pf.delete();
        slim = false;
        setStatus("Back to your normal skin", false);
    }

    private static void saveProps() {
        try {
            Properties p = new Properties();
            p.setProperty("slim", String.valueOf(slim));
            OutputStream os = new FileOutputStream(propsFile());
            try { p.store(os, "MamadClient skin"); } finally { os.close(); }
        } catch (Exception e) {
            System.out.println("[MamadClient] skin props save failed: " + e);
        }
    }
}
