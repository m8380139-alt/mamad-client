package ir.mamadcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** ابزارهای مشترک رسم رابط (مستطیل گرد، لوگو) برای صفحه‌های Mamad Client. */
public final class Ui {
    public static final int ACCENT = 0xFF8B5CF6;
    public static final int ACCENT_HOVER = 0xFFA78BFA;
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

    /** مستطیل با گوشه‌های گرد؛ در رزولوشن واقعی صفحه با لبهٔ صاف (HD). */
    public static void roundRect(int x, int y, int w, int h, int r, int color) {
        roundGrad(x, y, w, h, r, color, color);
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
        roundGrad(x, y, w, h, r, top, bottom);
    }

    /** رسم در پیکسل‌های واقعی صفحه (نه پیکسل‌های بزرگ GUI) با لبهٔ نرم؛ همه در یک batch. */
    private static void roundGrad(int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        int sf = scaleFactor(mc);
        float X = x * sf, Y = y * sf, W = w * sf, H = h * sf;
        float R = Math.min(r * sf, Math.min(W, H) / 2f);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1f / sf, 1f / sf, 1f);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int rows = (int) H;
        int cr = (int) Math.ceil(R);
        boolean fast = rows > 2 * cr + 2;
        if (fast) {
            // بخش وسط (بدون گوشه) یک quad با شیب رنگ؛ فقط ردیف‌های گوشه جدا رسم می‌شوند
            int ct = lerp(top, bottom, cr / (float) Math.max(1, rows - 1));
            int cb = lerp(top, bottom, (rows - cr) / (float) Math.max(1, rows - 1));
            quadV(wr, X, Y + cr, X + W, Y + rows - cr, ct, cb);
        }
        for (int dy = 0; dy < rows; dy++) {
            if (fast && dy == cr) { dy = rows - cr - 1; continue; }
            float yc = dy + 0.5f, inset = 0f;
            if (yc < R) { float d = R - yc; inset = R - (float) Math.sqrt(R * R - d * d); }
            else if (yc > H - R) { float d = yc - (H - R); inset = R - (float) Math.sqrt(R * R - d * d); }
            int fi = (int) Math.floor(inset);
            float cov = 1f - (inset - fi);
            int col = lerp(top, bottom, dy / (float) Math.max(1, rows - 1));
            float y0 = Y + dy, y1 = y0 + 1;
            if (fi + 1 >= W - fi - 1) { quad(wr, X + fi, y0, X + W - fi, y1, col, cov); continue; }
            quad(wr, X + fi + 1, y0, X + W - fi - 1, y1, col, 1f);
            quad(wr, X + fi, y0, X + fi + 1, y1, col, cov);
            quad(wr, X + W - fi - 1, y0, X + W - fi, y1, col, cov);
        }
        t.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static int cachedSf = 1, cachedW = -1, cachedH = -1, cachedGui = -1;
    private static boolean cachedUni;

    /** ScaledResolution فقط وقتی اندازه/تنظیمات عوض شد دوباره ساخته می‌شود (قبلاً در هر مستطیل ساخته می‌شد). */
    private static int scaleFactor(Minecraft mc) {
        int gs = mc.gameSettings.guiScale;
        boolean uni = mc.isUnicode();
        if (mc.displayWidth != cachedW || mc.displayHeight != cachedH || gs != cachedGui || uni != cachedUni) {
            cachedSf = new ScaledResolution(mc).getScaleFactor();
            cachedW = mc.displayWidth; cachedH = mc.displayHeight; cachedGui = gs; cachedUni = uni;
        }
        return cachedSf;
    }

    private static void quadV(WorldRenderer wr, float x0, float y0, float x1, float y1, int ctop, int cbot) {
        wr.pos(x0, y1, 0).color((cbot >> 16) & 255, (cbot >> 8) & 255, cbot & 255, (cbot >>> 24) & 255).endVertex();
        wr.pos(x1, y1, 0).color((cbot >> 16) & 255, (cbot >> 8) & 255, cbot & 255, (cbot >>> 24) & 255).endVertex();
        wr.pos(x1, y0, 0).color((ctop >> 16) & 255, (ctop >> 8) & 255, ctop & 255, (ctop >>> 24) & 255).endVertex();
        wr.pos(x0, y0, 0).color((ctop >> 16) & 255, (ctop >> 8) & 255, ctop & 255, (ctop >>> 24) & 255).endVertex();
    }

    private static void quad(WorldRenderer wr, float x0, float y0, float x1, float y1, int c, float cov) {
        int a = Math.round(((c >>> 24) & 255) * cov);
        int rr = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        wr.pos(x0, y1, 0).color(rr, g, b, a).endVertex();
        wr.pos(x1, y1, 0).color(rr, g, b, a).endVertex();
        wr.pos(x1, y0, 0).color(rr, g, b, a).endVertex();
        wr.pos(x0, y0, 0).color(rr, g, b, a).endVertex();
    }

    /** پنل شیشه‌ای مدرن برای HUD: زمینهٔ تیره نیمه‌شفاف + حاشیهٔ ظریف + نوار رنگی سمت چپ. */
    public static void hudPanel(int x, int y, int w, int h, int accent) {
        roundRect(x - 1, y - 1, w + 2, h + 2, 6, 0x22FFFFFF);
        roundRect(x, y, w, h, 5, 0xA0101216);
        roundRect(x + 3, y + 3, 2, h - 6, 1, accent);
    }

    /** یک خط «برچسب: مقدار» داخل پنل گرد؛ عرض پنل را برمی‌گرداند. */
    public static int statPill(net.minecraft.client.gui.FontRenderer fr, int x, int y, String label, String value, int labelColor, int valueColor) {
        String l = label + "  ";
        int w = fr.getStringWidth(l) + fr.getStringWidth(value) + 14;
        hudPanel(x, y, w, 14, labelColor);
        fr.drawStringWithShadow(l, x + 9, y + 3, labelColor);
        fr.drawStringWithShadow(value, x + 9 + fr.getStringWidth(l), y + 3, valueColor);
        return w;
    }

    /** کاشی شیشه‌ای نقره‌ای زیر آیکون ابزارها. */
    public static void glassTile(int x, int y, int s) {
        roundRect(x - 1, y - 1, s + 2, s + 2, 9, 0x55FFFFFF);
        roundRectV(x, y, s, s, 8, 0xDD9AA1AD, 0xDD4E535D);
        roundRect(x + 2, y + 2, s - 4, s / 2 - 1, 6, 0x26FFFFFF);
    }

    public static void logo(int x, int y, int size) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }
}
