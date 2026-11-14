package ir.mamadcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** ابزارهای مشترک رسم رابط (مستطیل گرد، لوگو) برای صفحه‌های Mamad Client. */
public final class Ui {
    public static final int ACCENT = 0xFFFF5A00;
    public static final int ACCENT_HOVER = 0xFFFF7A2A;
    public static final int PANEL = 0xF01C1C20;
    public static final int FIELD = 0xFF2A2A2E;
    public static final int BTN = 0xCC2A2A2E;
    public static final int BTN_HOVER = 0xCC3A3A40;
    public static final int DANGER = 0xFF4A2226;
    public static final int DANGER_HOVER = 0xFF63292E;
    public static final int RED = 0xFFD83A3A;

    private static final ResourceLocation LOGO = new ResourceLocation("mamadclient", "textures/logo.png");

    private Ui() {}

    public static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** مستطیل با گوشه‌های گرد (ردیف‌به‌ردیف، بدون تکسچر). */
    public static void roundRect(int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int dy = 0; dy < h; dy++) {
            int inset = 0;
            if (dy < r) {
                inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - dy) * (r - dy)));
            } else if (dy >= h - r) {
                int d = h - 1 - dy;
                inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - d) * (r - d)));
            }
            Gui.drawRect(x + inset, y + dy, x + w - inset, y + dy + 1, color);
        }
    }

    /** مستطیل گرد با حاشیهٔ رنگی (۱ پیکسل). */
    public static void roundRectBorder(int x, int y, int w, int h, int r, int fill, int border) {
        roundRect(x, y, w, h, r, border);
        // پایهٔ مات تا رنگِ حاشیه از زیرِ پُرشدگی نیمه‌شفاف بیرون نزند
        roundRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), 0xFF15171B);
        roundRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public static int lerp(int a, int b, float t) {
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int rr = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    /** مستطیل گرد با شیب رنگ از بالا به پایین. */
    public static void roundRectV(int x, int y, int w, int h, int r, int top, int bottom) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int dy = 0; dy < h; dy++) {
            int inset = 0;
            if (dy < r) {
                inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - dy) * (r - dy)));
            } else if (dy >= h - r) {
                int d = h - 1 - dy;
                inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - d) * (r - d)));
            }
            Gui.drawRect(x + inset, y + dy, x + w - inset, y + dy + 1, lerp(top, bottom, dy / (float) Math.max(1, h - 1)));
        }
    }

    /** کاشی شیشه‌ای نقره‌ای زیر آیکون ابزارها. */
    public static void glassTile(int x, int y, int s) {
        roundRect(x - 1, y - 1, s + 2, s + 2, 9, 0x55FFFFFF);
        roundRectV(x, y, s, s, 8, 0xDD9AA1AD, 0xDD4E535D);
        roundRect(x + 2, y + 2, s - 4, s / 2 - 1, 6, 0x26FFFFFF);
    }

    public static void logo(int x, int y, int size) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }
}
