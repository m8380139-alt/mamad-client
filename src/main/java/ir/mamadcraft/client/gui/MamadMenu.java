package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.account.SiteAccount;
import ir.mamadcraft.client.cosmetics.CapeManager;
import ir.mamadcraft.client.cosmetics.CapeStore;
import ir.mamadcraft.client.cosmetics.ImageCache;
import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * منوی کلاینت (Right Shift): پنجرهٔ بزرگ شیشه‌ای با نوار کناری — بخش Tools (ماژول‌ها) و Cape (کیپ‌های حساب سایت).
 */
public class MamadMenu extends GuiScreen {
    private static final ResourceLocation ICON_GEAR = new ResourceLocation("mamadclient", "textures/icons/gear.png");
    private static final ResourceLocation ICON_CAPE = new ResourceLocation("mamadclient", "textures/icons/cape.png");
    private static final ResourceLocation ICON_SEARCH = new ResourceLocation("mamadclient", "textures/icons/search.png");

    private static final int SW = 124;      // عرض نوار کناری
    private static final int GAP = 8;

    private final GuiScreen parent;
    private int tab = 0;                    // 0 = Tools، 1 = Cape
    private String search = "";
    private boolean searchFocus;
    private final float[] scroll = new float[2];
    private String selectedId;
    private float yaw = 190f;
    private boolean dragging;
    private int lastDragX;
    private int x0, y0, W, H;
    private int cx, cy, cw, ch;

    public MamadMenu() { this(null); }

    public MamadMenu(GuiScreen parent) { this.parent = parent; }

    // ---------------------------------------------------------------- چرخهٔ عمر

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        ImageCache.retryFailed();
        if (SiteAccount.isLoggedIn()) CapeStore.refresh();
        layout();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        CapeManager.previewOverride = null;
        CapeManager.previewFor = null;
    }

    private void close() { mc.displayGuiScreen(parent); }

    private void layout() {
        W = Math.min(width - 16, Math.min(720, Math.max(420, (int) (width * 0.76))));
        H = Math.min(height - 16, Math.min(430, Math.max(270, (int) (height * 0.80))));
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        cx = x0 + SW + 14;
        cy = y0 + 48;
        cw = W - SW - 28;
        ch = H - 48 - 14;
    }

    // ---------------------------------------------------------------- ابزارهای رسم

    private void icon(ResourceLocation r, int x, int y, int size, int argb) {
        mc.getTextureManager().bindTexture(r);
        GlStateManager.enableBlend();
        GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void clip(int x, int y, int w, int h) {
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }

    private void unclip() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    private void centered(String s, int midX, int y, int color) {
        fontRendererObj.drawString(s, midX - fontRendererObj.getStringWidth(s) / 2, y, color);
    }

    private void button(String label, int x, int y, int w, int h, int base, int hover, int text, int mx, int my) {
        boolean on = Ui.inside(mx, my, x, y, w, h);
        Ui.roundRect(x, y, w, h, h / 2, on ? hover : base);
        centered(label, x + w / 2, y + (h - 8) / 2, text);
    }

    private boolean matches(String name) {
        return search.isEmpty() || name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private List<ModuleManager.Module> visibleModules() {
        List<ModuleManager.Module> out = new ArrayList<ModuleManager.Module>();
        for (ModuleManager.Module m : ModuleManager.MODULES) if (matches(m.name)) out.add(m);
        return out;
    }

    private List<CapeStore.Cosmetic> visibleCapes() {
        List<CapeStore.Cosmetic> out = new ArrayList<CapeStore.Cosmetic>();
        for (CapeStore.Cosmetic c : CapeStore.items) if (c.name != null && matches(c.name)) out.add(c);
        return out;
    }

    private CapeStore.Cosmetic selectedCape() {
        String id = selectedId != null ? selectedId : CapeStore.equippedId;
        if (id == null) return null;
        for (CapeStore.Cosmetic c : CapeStore.items) if (id.equals(c.id)) return c;
        return null;
    }

    // ---------------------------------------------------------------- رسم

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        if (mc.theWorld != null) drawRect(0, 0, width, height, 0x80000000);
        else drawGradientRect(0, 0, width, height, 0xDD0B0B10, 0xF0000000);

        // پنجرهٔ شیشه‌ای
        Ui.roundRect(x0 - 1, y0 - 1, W + 2, H + 2, 15, 0x38FFFFFF);
        Ui.roundRect(x0, y0, W, H, 14, 0xC4101216);
        drawGradientRect(x0 + 14, y0 + 1, x0 + W - 14, y0 + H / 3, 0x18FFFFFF, 0x00FFFFFF);
        Ui.roundRect(x0 + 6, y0 + 6, SW - 6, H - 12, 10, 0x44000000);

        drawSidebar(mx, my);
        drawHeader(mx, my);

        if (tab == 0) drawTools(mx, my);
        else drawCapes(mx, my);

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawSidebar(int mx, int my) {
        Ui.logo(x0 + 16, y0 + 14, 24);
        fontRendererObj.drawStringWithShadow("Mamad Client", x0 + 46, y0 + 16, 0xFFFFFFFF);
        fontRendererObj.drawString("1.8.9", x0 + 46, y0 + 27, 0xFF8A8A92);

        String[] names = {"Tools", "Cape"};
        ResourceLocation[] icons = {ICON_GEAR, ICON_CAPE};
        for (int i = 0; i < 2; i++) {
            int nx = x0 + 12, ny = y0 + 56 + i * 32, nw = SW - 18;
            boolean sel = tab == i;
            boolean hov = Ui.inside(mx, my, nx, ny, nw, 28);
            if (sel) Ui.roundRect(nx, ny, nw, 28, 10, 0x55FF5A00);
            else if (hov) Ui.roundRect(nx, ny, nw, 28, 10, 0x22FFFFFF);
            icon(icons[i], nx + 9, ny + 8, 12, sel ? Ui.ACCENT : 0xFFD0D0D6);
            fontRendererObj.drawString(names[i], nx + 28, ny + 10, sel ? 0xFFFFFFFF : 0xFFC8C8CE);
        }

        // حساب سایت + بستن
        String acct = SiteAccount.isLoggedIn() ? SiteAccount.getUsername() : "Sign in";
        int ax = x0 + 12, aw = SW - 18;
        boolean ah = Ui.inside(mx, my, ax, y0 + H - 64, aw, 22);
        Ui.roundRect(ax, y0 + H - 64, aw, 22, 11, ah ? 0x44FFFFFF : 0x30FFFFFF);
        Ui.logo(ax + 5, y0 + H - 61, 16);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(acct, aw - 30), ax + 25, y0 + H - 56,
                SiteAccount.isLoggedIn() ? 0xFFFFFFFF : 0xFFFFB27A);
        button("Close", ax, y0 + H - 34, aw, 22, Ui.DANGER, Ui.DANGER_HOVER, 0xFFFF8A8A, mx, my);
    }

    private void drawHeader(int mx, int my) {
        String title = tab == 0 ? "Tools" : "Cape";
        fontRendererObj.drawStringWithShadow(title, cx, y0 + 14, 0xFFFFFFFF);
        String sub;
        int subColor = 0xFF8A8A92;
        if (tab == 0) {
            sub = visibleModules().size() + " modules";
        } else if (!SiteAccount.isLoggedIn()) {
            sub = "Sign in to use your capes";
        } else if (!CapeStore.status.isEmpty()) {
            sub = CapeStore.status;
            subColor = 0xFFFF6B6B;
        } else if (CapeStore.busy) {
            sub = "Loading...";
        } else {
            sub = visibleCapes().size() + " capes";
        }
        fontRendererObj.drawString(sub, cx, y0 + 27, subColor);

        int sw = 132, sx = x0 + W - 14 - sw, sy = y0 + 12;
        if (searchFocus) Ui.roundRectBorder(sx, sy, sw, 20, 10, Ui.FIELD, Ui.ACCENT);
        else Ui.roundRect(sx, sy, sw, 20, 10, 0xFF2A2A2E);
        icon(ICON_SEARCH, sx + 7, sy + 5, 10, 0xFFB0B0B8);
        String shown = search.isEmpty() && !searchFocus ? "Search" : fontRendererObj.trimStringToWidth(search, sw - 34, true);
        fontRendererObj.drawString(shown, sx + 22, sy + 6, search.isEmpty() && !searchFocus ? 0xFF8A8A92 : 0xFFFFFFFF);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0) {
            int tx = sx + 22 + fontRendererObj.getStringWidth(shown);
            Gui.drawRect(tx, sy + 5, tx + 1, sy + 15, 0xFFFFFFFF);
        }
    }

    // ---------------------------------------------------------------- Tools

    private int toolCols() { return Math.max(2, Math.min(4, cw / 128)); }

    private void drawTools(int mx, int my) {
        List<ModuleManager.Module> mods = visibleModules();
        int cols = toolCols();
        int cwid = (cw - GAP * (cols - 1)) / cols, chei = 52;
        int rows = (mods.size() + cols - 1) / cols;
        float maxScroll = Math.max(0, rows * (chei + GAP) - GAP - ch);
        scroll[0] = Math.max(0, Math.min(maxScroll, scroll[0]));

        clip(cx, cy, cw, ch);
        for (int i = 0; i < mods.size(); i++) {
            ModuleManager.Module m = mods.get(i);
            int bx = cx + (i % cols) * (cwid + GAP);
            int by = cy + (i / cols) * (chei + GAP) - (int) scroll[0];
            if (by + chei < cy || by > cy + ch) continue;
            boolean hov = Ui.inside(mx, my, bx, by, cwid, chei) && Ui.inside(mx, my, cx, cy, cw, ch);
            if (m.enabled) Ui.roundRectBorder(bx, by, cwid, chei, 12, hov ? 0x55FF5A00 : 0x40FF5A00, Ui.ACCENT);
            else Ui.roundRect(bx, by, cwid, chei, 12, hov ? 0x40FFFFFF : 0x28FFFFFF);
            Ui.roundRect(bx + 10, by + 12, 28, 28, 14, m.enabled ? Ui.ACCENT : 0xFF3A3A42);
            centered(m.name.substring(0, 1).toUpperCase(Locale.ROOT), bx + 24, by + 22, 0xFFFFFFFF);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(m.name, cwid - 52), bx + 44, by + 14, 0xFFFFFFFF);
            fontRendererObj.drawString(m.enabled ? "On" : "Off", bx + 44, by + 28, m.enabled ? 0xFF6BE675 : 0xFF8A8A92);
        }
        unclip();
        drawScrollbar(maxScroll, scroll[0]);
    }

    private void drawScrollbar(float maxScroll, float pos) {
        if (maxScroll <= 0) return;
        int barH = Math.max(18, (int) (ch * (ch / (ch + maxScroll))));
        int barY = cy + (int) ((ch - barH) * (pos / maxScroll));
        Ui.roundRect(x0 + W - 9, barY, 3, barH, 1, 0x66FFFFFF);
    }

    // ---------------------------------------------------------------- Cape

    private void drawCapes(int mx, int my) {
        if (!SiteAccount.isLoggedIn()) {
            centered("Your capes live on your MamadCraft account.", cx + cw / 2, cy + ch / 2 - 22, 0xFFDDDDDD);
            button("Sign in", cx + cw / 2 - 60, cy + ch / 2 - 4, 120, 22, Ui.ACCENT, Ui.ACCENT_HOVER, 0xFFFFFFFF, mx, my);
            return;
        }
        int panelW = 164;
        int gw = cw - panelW - 10;
        List<CapeStore.Cosmetic> capes = visibleCapes();

        // پیش‌نمایش روی بازیکن خودمان
        CapeStore.Cosmetic sel = selectedCape();
        String mc_name = mc.getSession().getUsername();
        if (sel != null) {
            ImageCache.Img tex = ImageCache.get(sel.textureUrl());
            CapeManager.previewOverride = tex == null ? null : tex.loc;
            CapeManager.previewFor = mc_name;
        } else {
            CapeManager.previewOverride = null;
        }

        // شبکهٔ کارت‌ها
        int cols = gw >= 250 ? 3 : 2;
        int cwid = (gw - GAP * (cols - 1)) / cols, chei = 104;
        int rows = (capes.size() + cols - 1) / cols;
        float maxScroll = Math.max(0, rows * (chei + GAP) - GAP - ch);
        scroll[1] = Math.max(0, Math.min(maxScroll, scroll[1]));

        clip(cx, cy, gw, ch);
        for (int i = 0; i < capes.size(); i++) {
            CapeStore.Cosmetic c = capes.get(i);
            int bx = cx + (i % cols) * (cwid + GAP);
            int by = cy + (i / cols) * (chei + GAP) - (int) scroll[1];
            if (by + chei < cy || by > cy + ch) continue;
            boolean isSel = sel != null && c.id.equals(sel.id);
            boolean hov = Ui.inside(mx, my, bx, by, cwid, chei) && Ui.inside(mx, my, cx, cy, gw, ch);
            if (isSel) Ui.roundRectBorder(bx, by, cwid, chei, 12, 0x40FF5A00, Ui.ACCENT);
            else Ui.roundRect(bx, by, cwid, chei, 12, hov ? 0x40FFFFFF : 0x28FFFFFF);

            ImageCache.Img img = ImageCache.get(c.previewUrl);
            int availW = cwid - 20, availH = chei - 38;
            if (img != null) {
                float s = Math.min((float) availW / img.w, (float) availH / img.h);
                int dw = Math.max(1, (int) (img.w * s)), dh = Math.max(1, (int) (img.h * s));
                mc.getTextureManager().bindTexture(img.loc);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                Gui.drawScaledCustomSizeModalRect(bx + (cwid - dw) / 2, by + 8 + (availH - dh) / 2, 0, 0, img.w, img.h, dw, dh, img.w, img.h);
            } else {
                centered("...", bx + cwid / 2, by + 8 + availH / 2 - 4, 0xFF777780);
            }
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(c.name, cwid - 12), bx + 8, by + chei - 26, 0xFFFFFFFF);
            boolean eq = c.id.equals(CapeStore.equippedId);
            boolean own = CapeStore.owned.contains(c.id);
            String st = eq ? "Equipped" : own ? "Owned" : c.freeNow ? "Free" : "Locked";
            int stc = eq ? Ui.ACCENT : own ? 0xFF6BE675 : c.freeNow ? 0xFFDDDDDD : 0xFF8A8A92;
            fontRendererObj.drawString(st, bx + 8, by + chei - 14, stc);
        }
        unclip();
        drawScrollbar(maxScroll, scroll[1]);

        drawCapePanel(cx + gw + 10, panelW, sel, mx, my);
    }

    private void drawCapePanel(int px, int pw, CapeStore.Cosmetic sel, int mx, int my) {
        Ui.roundRect(px, cy, pw, ch, 12, 0x28FFFFFF);
        int modelH = ch - 78;

        if (mc.thePlayer != null) {
            int scale = Math.max(18, Math.min(58, (int) ((modelH - 24) / 2.1f)));
            GlStateManager.pushMatrix();
            GlStateManager.translate(px + pw / 2f, 0f, 50f);
            GlStateManager.rotate(yaw - 180f, 0f, 1f, 0f);
            GlStateManager.translate(-(px + pw / 2f), 0f, -50f);
            GuiInventory.drawEntityOnScreen(px + pw / 2, cy + modelH - 8, scale, 0f, 0f, mc.thePlayer);
            GlStateManager.popMatrix();
            GlStateManager.enableBlend();
            centered("Drag to turn", px + pw / 2, cy + modelH - 2, 0xFF8A8A92);
        } else if (sel != null) {
            ImageCache.Img img = ImageCache.get(sel.previewUrl);
            if (img != null) {
                float s = Math.min((float) (pw - 40) / img.w, (float) (modelH - 20) / img.h);
                int dw = Math.max(1, (int) (img.w * s)), dh = Math.max(1, (int) (img.h * s));
                mc.getTextureManager().bindTexture(img.loc);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                Gui.drawScaledCustomSizeModalRect(px + (pw - dw) / 2, cy + 10 + (modelH - 20 - dh) / 2, 0, 0, img.w, img.h, dw, dh, img.w, img.h);
            }
            centered("Join a world to preview on you", px + pw / 2, cy + modelH - 2, 0xFF8A8A92);
        }

        // نام و دکمهٔ عمل
        if (sel != null) {
            centered(fontRendererObj.trimStringToWidth(sel.name, pw - 16), px + pw / 2, cy + ch - 62, 0xFFFFFFFF);
            boolean eq = sel.id.equals(CapeStore.equippedId);
            boolean own = CapeStore.owned.contains(sel.id);
            String label;
            boolean enabled = !CapeStore.busy;
            if (CapeStore.busy) label = "Please wait...";
            else if (eq) label = "Unequip";
            else if (own) label = "Equip";
            else if (sel.freeNow) label = "Claim & Equip";
            else { label = "Get it on the site"; enabled = false; }
            int bx = px + 12, by = cy + ch - 40, bw = pw - 24;
            if (enabled) button(label, bx, by, bw, 24, eq ? Ui.BTN : Ui.ACCENT, eq ? Ui.BTN_HOVER : Ui.ACCENT_HOVER, 0xFFFFFFFF, mx, my);
            else { Ui.roundRect(bx, by, bw, 24, 12, 0x30FFFFFF); centered(label, bx + bw / 2, by + 8, 0xFF8A8A92); }
        } else {
            centered("Pick a cape", px + pw / 2, cy + ch - 34, 0xFF8A8A92);
        }
    }

    // ---------------------------------------------------------------- ورودی

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        layout();

        // نوار کناری
        for (int i = 0; i < 2; i++) {
            if (Ui.inside(mx, my, x0 + 12, y0 + 56 + i * 32, SW - 18, 28)) {
                if (tab != i) { tab = i; search = ""; if (i == 1 && SiteAccount.isLoggedIn()) CapeStore.refresh(); }
                return;
            }
        }
        if (Ui.inside(mx, my, x0 + 12, y0 + H - 64, SW - 18, 22)) { mc.displayGuiScreen(new LoginScreen(this)); return; }
        if (Ui.inside(mx, my, x0 + 12, y0 + H - 34, SW - 18, 22)) { close(); return; }

        // جستجو
        int sw = 132, sx = x0 + W - 14 - sw, sy = y0 + 12;
        searchFocus = Ui.inside(mx, my, sx, sy, sw, 20);
        if (searchFocus) return;

        if (!Ui.inside(mx, my, x0, y0, W, H)) { close(); return; }

        if (tab == 0) clickTools(mx, my);
        else clickCapes(mx, my);
    }

    private void clickTools(int mx, int my) {
        if (!Ui.inside(mx, my, cx, cy, cw, ch)) return;
        List<ModuleManager.Module> mods = visibleModules();
        int cols = toolCols();
        int cwid = (cw - GAP * (cols - 1)) / cols, chei = 52;
        for (int i = 0; i < mods.size(); i++) {
            int bx = cx + (i % cols) * (cwid + GAP);
            int by = cy + (i / cols) * (chei + GAP) - (int) scroll[0];
            if (Ui.inside(mx, my, bx, by, cwid, chei)) {
                ModuleManager.Module m = mods.get(i);
                m.enabled = !m.enabled;
                ModuleManager.save();
                return;
            }
        }
    }

    private void clickCapes(int mx, int my) {
        if (!SiteAccount.isLoggedIn()) {
            if (Ui.inside(mx, my, cx + cw / 2 - 60, cy + ch / 2 - 4, 120, 22)) mc.displayGuiScreen(new LoginScreen(this));
            return;
        }
        int panelW = 164, gw = cw - panelW - 10;
        int px = cx + gw + 10;

        // پنل راست: دکمهٔ عمل و چرخاندن
        CapeStore.Cosmetic sel = selectedCape();
        if (sel != null && !CapeStore.busy && Ui.inside(mx, my, px + 12, cy + ch - 40, panelW - 24, 24)) {
            boolean eq = sel.id.equals(CapeStore.equippedId);
            boolean own = CapeStore.owned.contains(sel.id);
            if (eq) CapeStore.unequip();
            else if (own || sel.freeNow) CapeStore.equip(sel);
            return;
        }
        if (Ui.inside(mx, my, px, cy, panelW, ch - 78)) { dragging = true; lastDragX = mx; return; }

        // شبکهٔ کیپ‌ها
        if (!Ui.inside(mx, my, cx, cy, gw, ch)) return;
        List<CapeStore.Cosmetic> capes = visibleCapes();
        int cols = gw >= 250 ? 3 : 2;
        int cwid = (gw - GAP * (cols - 1)) / cols, chei = 104;
        for (int i = 0; i < capes.size(); i++) {
            int bx = cx + (i % cols) * (cwid + GAP);
            int by = cy + (i / cols) * (chei + GAP) - (int) scroll[1];
            if (Ui.inside(mx, my, bx, by, cwid, chei)) { selectedId = capes.get(i).id; return; }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging) {
            yaw += (mx - lastDragX) * 1.6f;
            lastDragX = mx;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragging = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll[tab] -= (d > 0 ? 1 : -1) * 30f;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT) { close(); return; }
        if (!searchFocus) return;
        if (key == Keyboard.KEY_RETURN) { searchFocus = false; return; }
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            scroll[tab] = 0;
            return;
        }
        if (ChatAllowedCharacters.isAllowedCharacter(c) && search.length() < 24) { search += c; scroll[tab] = 0; }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
