package ir.mamadcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Locale;
import java.util.Properties;

/**
 * موقعیت المان‌های HUD. هر المان یک آفست (dx, dy) نسبت به جای پیش‌فرضش دارد.
 * همه‌چیز آرایهٔ ثابت است تا در هر فریم هیچ آبجکتی ساخته نشود.
 */
public final class HudLayout {
    public static final int STATS = 0, INFO = 1, KEYS = 2, POTIONS = 3, ARMOR = 4, REACH = 5, MARK = 6, COUNT = 7;
    public static final String[] NAMES = {"Stats", "Info", "Keystrokes", "Potions", "Armor", "Reach", "Watermark"};

    public static final int[] dx = new int[COUNT], dy = new int[COUNT];
    /** آخرین کادر رسم‌شده (بدون آفست)، و زمان آخرین رسم. */
    public static final int[] bx = new int[COUNT], by = new int[COUNT], bw = new int[COUNT], bh = new int[COUNT];
    private static final long[] seen = new long[COUNT];

    private HudLayout() {}

    /** شروع رسم یک المان: به‌اندازهٔ آفستش جابه‌جا می‌شود. حتماً با end() جفت شود. */
    public static void begin(int id) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) dx[id], (float) dy[id], 0f);
    }

    /** پایان رسم؛ کادر المان را (در مختصات پیش‌فرض) برای ادیتور ثبت می‌کند. */
    public static void end(int id, int x, int y, int w, int h) {
        GlStateManager.popMatrix();
        if (w <= 0 || h <= 0) return;
        bx[id] = x; by[id] = y; bw[id] = w; bh[id] = h;
        seen[id] = Minecraft.getSystemTime();
    }

    /** آیا این المان تازه رسم شده (یعنی روشن است و قابل ویرایش)؟ */
    public static boolean visible(int id) {
        return Minecraft.getSystemTime() - seen[id] < 500L;
    }

    /** آفست را طوری محدود می‌کند که المان از صفحه بیرون نزند. */
    public static void clamp(int id, int sw, int sh) {
        int minX = -bx[id], maxX = sw - bx[id] - bw[id];
        int minY = -by[id], maxY = sh - by[id] - bh[id];
        if (maxX >= minX) dx[id] = Math.max(minX, Math.min(maxX, dx[id]));
        if (maxY >= minY) dy[id] = Math.max(minY, Math.min(maxY, dy[id]));
    }

    public static void reset() {
        for (int i = 0; i < COUNT; i++) { dx[i] = 0; dy[i] = 0; }
    }

    private static String key(int id, String axis) {
        return "hud." + NAMES[id].toLowerCase(Locale.ROOT) + "." + axis;
    }

    public static void load(Properties p) {
        for (int i = 0; i < COUNT; i++) {
            dx[i] = parse(p.getProperty(key(i, "x")));
            dy[i] = parse(p.getProperty(key(i, "y")));
        }
    }

    public static void save(Properties p) {
        for (int i = 0; i < COUNT; i++) {
            p.setProperty(key(i, "x"), String.valueOf(dx[i]));
            p.setProperty(key(i, "y"), String.valueOf(dy[i]));
        }
    }

    private static int parse(String s) {
        if (s == null) return 0;
        try { return Math.max(-4000, Math.min(4000, Integer.parseInt(s.trim()))); }
        catch (NumberFormatException e) { return 0; }
    }
}
