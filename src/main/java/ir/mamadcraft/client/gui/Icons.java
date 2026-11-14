package ir.mamadcraft.client.gui;

import net.minecraft.client.gui.Gui;

/** آیکون‌های ابزارها: همه با خط و دایره کشیده می‌شوند (بدون فایل عکس). شبکهٔ طراحی ۱۶×۱۶. */
public final class Icons {
    private static int ox, oy, col;
    private static float k;

    private Icons() {}

    private static int px(float u) { return Math.round(u * k); }

    private static void rect(float x, float y, float w, float h) {
        int x0 = ox + px(x), y0 = oy + px(y);
        int x1 = ox + px(x + w), y1 = oy + px(y + h);
        if (x1 <= x0) x1 = x0 + 1;
        if (y1 <= y0) y1 = y0 + 1;
        Gui.drawRect(x0, y0, x1, y1, col);
    }

    private static void box(float x, float y, float w, float h, float t) {
        rect(x, y, w, t);
        rect(x, y + h - t, w, t);
        rect(x, y, t, h);
        rect(x + w - t, y, t, h);
    }

    private static void line(float x0, float y0, float x1, float y1, float t) {
        float dx = x1 - x0, dy = y1 - y0;
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * k));
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n;
            rect(x0 + dx * f - t / 2f, y0 + dy * f - t / 2f, t, t);
        }
    }

    private static void disc(float cx, float cy, float r) {
        float step = 1f / k;
        for (float dy = -r; dy <= r; dy += step) {
            float hw = (float) Math.sqrt(Math.max(0f, r * r - dy * dy));
            rect(cx - hw, cy + dy, 2f * hw, step);
        }
    }

    /** زاویه‌ها به درجه؛ ۰ = راست و ۹۰ = پایین. */
    private static void arc(float cx, float cy, float r, float a0, float a1, float t) {
        int steps = Math.max(10, (int) (r * k * Math.abs(a1 - a0) / 40f));
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / (double) steps);
            rect((float) (cx + r * Math.cos(a)) - t / 2f, (float) (cy + r * Math.sin(a)) - t / 2f, t, t);
        }
    }

    private static void ring(float cx, float cy, float r, float t) { arc(cx, cy, r, 0, 360, t); }

    /** با سایهٔ نرم زیرش تا روی شیشه برجسته شود. */
    public static boolean draw(String name, int x, int y, int size, int argb) {
        if (!glyph(name, x, y + 1, size, 0x55000000)) return false;
        glyph(name, x, y, size, argb);
        return true;
    }

    public static void power(int x, int y, int size, int argb) {
        ox = x; oy = y; k = size / 16f; col = argb;
        arc(8f, 8.6f, 5.2f, 300, 600, 1.8f);
        line(8f, 2.2f, 8f, 8f, 1.8f);
    }

    private static boolean glyph(String name, int x, int y, int size, int argb) {
        ox = x; oy = y; k = size / 16f; col = argb;
        switch (name) {
            case "FPS":
                arc(8, 9, 6, 150, 390, 1.6f); line(8, 9, 11.5f, 5.5f, 1.6f); disc(8, 9, 1.3f); return true;
            case "CPS":
                box(4.5f, 2, 7, 12, 1.4f); rect(4.5f, 7, 7, 1.2f); rect(7.4f, 3.6f, 1.2f, 2.4f); return true;
            case "Ping":
                arc(8, 12.5f, 3.5f, 225, 315, 1.5f); arc(8, 12.5f, 6.5f, 225, 315, 1.5f);
                arc(8, 12.5f, 9.5f, 230, 310, 1.5f); disc(8, 12.8f, 1.1f); return true;
            case "Coordinates":
                ring(8, 6.5f, 3.6f, 1.5f); line(8, 10, 8, 14.5f, 1.5f); disc(8, 6.5f, 1.2f); return true;
            case "Keystrokes":
                box(5.5f, 2.2f, 5, 5, 1.2f); box(1.5f, 8.8f, 4.4f, 4.4f, 1.2f);
                box(6, 8.8f, 4.4f, 4.4f, 1.2f); box(10.5f, 8.8f, 4.4f, 4.4f, 1.2f); return true;
            case "Armor Status":
                for (int i = 0; i < 13; i++) {
                    float w = i < 7 ? 10f : 10f - (i - 6) * 1.55f;
                    rect(8 - w / 2f, 2 + i, w, 1);
                }
                return true;
            case "Potion Effects":
                rect(6.6f, 2, 2.8f, 4);
                for (int i = 0; i < 8; i++) { float w = 2.8f + i * 1.2f; rect(8 - w / 2f, 6 + i, w, 1); }
                return true;
            case "Reach Display":
                line(3, 3, 3, 13, 1.5f); line(13, 3, 13, 13, 1.5f); line(3, 8, 13, 8, 1.3f);
                line(3, 8, 5.5f, 5.8f, 1.2f); line(3, 8, 5.5f, 10.2f, 1.2f);
                line(13, 8, 10.5f, 5.8f, 1.2f); line(13, 8, 10.5f, 10.2f, 1.2f); return true;
            case "Toggle Sprint":
                line(3, 3, 8, 8, 1.8f); line(3, 13, 8, 8, 1.8f); line(8, 3, 13, 8, 1.8f); line(8, 13, 13, 8, 1.8f); return true;
            case "Toggle Sneak":
                line(3, 5, 8, 10, 1.8f); line(13, 5, 8, 10, 1.8f); rect(3, 12.5f, 10, 1.5f); return true;
            case "Fullbright":
                disc(8, 8, 3.2f);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    line((float) (8 + 4.9 * Math.cos(a)), (float) (8 + 4.9 * Math.sin(a)),
                            (float) (8 + 7 * Math.cos(a)), (float) (8 + 7 * Math.sin(a)), 1.4f);
                }
                return true;
            case "No Hurt Cam":
                box(2, 4.5f, 12, 8.5f, 1.4f); ring(8, 8.8f, 2.4f, 1.4f); rect(5.5f, 3, 3, 1.5f); return true;
            case "Custom Crosshair":
                rect(7.2f, 1.5f, 1.6f, 4.5f); rect(7.2f, 10, 1.6f, 4.5f);
                rect(1.5f, 7.2f, 4.5f, 1.6f); rect(10, 7.2f, 4.5f, 1.6f); disc(8, 8, 1); return true;
            case "Chat Tweaks":
                box(2, 3, 12, 8, 1.4f); line(5, 11, 4, 14, 1.4f); line(4, 14, 8, 11, 1.4f);
                rect(4.5f, 6, 7, 1.2f); rect(4.5f, 8.3f, 4.5f, 1.2f); return true;
            case "Friend/Enemy Colors":
                disc(5.5f, 5.5f, 2.2f); arc(5.5f, 13, 4, 180, 360, 1.6f);
                disc(11, 6.5f, 2); arc(11, 14, 3.6f, 180, 360, 1.5f); return true;
            case "Clock":
                ring(8, 8, 6, 1.5f); line(8, 8, 8, 4, 1.5f); line(8, 8, 11, 9.5f, 1.5f); return true;
            case "Uptime":
                line(4, 2.5f, 12, 2.5f, 1.5f); line(4, 13.5f, 12, 13.5f, 1.5f);
                line(4.5f, 3, 11.5f, 13, 1.4f); line(11.5f, 3, 4.5f, 13, 1.4f); return true;
            case "Memory":
                box(4, 4, 8, 8, 1.4f);
                for (int i = 0; i < 3; i++) {
                    rect(5.5f + i * 2.6f, 1.8f, 1.2f, 2.2f); rect(5.5f + i * 2.6f, 12, 1.2f, 2.2f);
                    rect(1.8f, 5.5f + i * 2.6f, 2.2f, 1.2f); rect(12, 5.5f + i * 2.6f, 2.2f, 1.2f);
                }
                return true;
            case "Server Address":
                ring(8, 8, 6, 1.4f); rect(2, 7.3f, 12, 1.3f); line(8, 2, 8, 14, 1.2f); return true;
            case "Combo Counter":
                rect(2.5f, 9, 3, 5); rect(6.5f, 6, 3, 8); rect(10.5f, 2.5f, 3, 11.5f); return true;
            case "Zoom":
                ring(6.8f, 6.8f, 4.6f, 1.6f); line(10.3f, 10.3f, 14, 14, 2); rect(5, 6.2f, 3.6f, 1.2f); rect(6.2f, 5, 1.2f, 3.6f); return true;
            case "Time Changer":
                arc(8, 11, 4.5f, 180, 360, 1.6f); rect(1.5f, 11, 13, 1.5f);
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(225 + i * 45);
                    line((float) (8 + 6.2 * Math.cos(a)), (float) (11 + 6.2 * Math.sin(a)),
                            (float) (8 + 8 * Math.cos(a)), (float) (11 + 8 * Math.sin(a)), 1.4f);
                }
                return true;
            case "Weather Changer":
                disc(5.5f, 9, 3); disc(9, 7.2f, 3.6f); disc(12, 9.6f, 2.6f); rect(5.5f, 9, 6.5f, 3.6f); return true;
            case "Hitbox":
                box(2, 5, 9, 9, 1.4f); box(5, 2, 9, 9, 1.4f); line(2, 5, 5, 2, 1.2f); line(11, 14, 14, 11, 1.2f); return true;
            case "Boss Bar":
                box(1.5f, 5.5f, 13, 5, 1.4f); rect(3.2f, 7.2f, 6.5f, 1.6f); return true;
            case "Hot Bar":
                for (int i = 0; i < 4; i++) box(1 + i * 3.8f, 5.5f, 3.4f, 5, 1.2f);
                return true;
            case "Tab":
                for (int i = 0; i < 3; i++) { disc(3.4f, 4 + i * 4, 1.1f); rect(6, 3.4f + i * 4, 8, 1.3f); }
                return true;
            case "Watermark":
                for (int i = 0; i < 7; i++) rect(8 - (1 + i * 2) / 2f, 1.5f + i, 1 + i * 2, 1);
                for (int i = 0; i < 7; i++) rect(8 - Math.max(1, 13 - (i + 1) * 2) / 2f, 8.5f + i, Math.max(1, 13 - (i + 1) * 2), 1);
                return true;
            default:
                return false;
        }
    }
}
