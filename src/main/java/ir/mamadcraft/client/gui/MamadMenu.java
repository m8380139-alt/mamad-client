package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.account.SiteAccount;
import ir.mamadcraft.client.cosmetics.CapeManager;
import ir.mamadcraft.client.cosmetics.CapeStore;
import ir.mamadcraft.client.cosmetics.ImageCache;
import ir.mamadcraft.client.modules.ModuleManager;
import ir.mamadcraft.client.modules.ToolsHandler;
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
 * منوی کلاینت (Right Shift): پنجرهٔ بزرگ شیشه‌ای با نوار کناری.
 * تب‌ها: Tools (ماژول‌ها با دسته‌بندی و پنل جزئیات/تنظیمات)، Cape (کیپ‌های حساب سایت)، Profiles (پروفایل‌های یک‌کلیکی).
 */
public class MamadMenu extends GuiScreen {
    private static final ResourceLocation ICON_GEAR = new ResourceLocation("mamadclient", "textures/icons/gear.png");
    private static final ResourceLocation ICON_CAPE = new ResourceLocation("mamadclient", "textures/icons/cape.png");
    private static final ResourceLocation ICON_SEARCH = new ResourceLocation("mamadclient", "textures/icons/search.png");

    private static final int SW = 124;      // عرض نوار کناری
    private static final int GAP = 8;
    private static final int CARD_H = 74;   // ارتفاع کارت ماژول
    private static final int PROF_H = 52;   // ارتفاع ردیف پروفایل
    private static final int TABS = 3;

    private static final String[] CATS = {"All", "PvP", "HUD", "Visual", "Utility", "Perf"};
    private static final int[] SWATCHES = {0xFF7A00, 0xFF3B3B, 0xFFD60A, 0x4CD964, 0x32D4FF, 0x5E7CFF, 0xB26BFF, 0xFFFFFF};
    private static final String[] SHAPES = {"cross", "dot", "circle"};
    private static final String[] SHAPE_LABELS = {"Cross", "Dot", "Circle"};

    private static final String[] PROFILE_NAMES = {"PvP", "Clean", "Performance", "Visual", "All Off"};
    private static final String[] PROFILE_DESC = {
            "FPS, CPS, ping, keystrokes, armor, reach, combo, sprint and no hurt cam.",
            "Only the FPS counter. Nothing else on screen.",
            "FPS Boost, Entity Culling, FPS and memory counters.",
            "Fullbright, zoom, custom crosshair, clock and FPS.",
            "Turns every optional module off."
    };
    private static final String[][] PROFILES = {
            {"FPS", "CPS", "Ping", "Keystrokes", "Armor Status", "Reach Display", "Combo Counter", "Toggle Sprint", "No Hurt Cam"},
            {"FPS"},
            {"FPS", "Memory", "FPS Boost", "Entity Culling"},
            {"FPS", "Fullbright", "Zoom", "Custom Crosshair", "Clock"},
            {}
    };

    private final GuiScreen parent;
    private int tab = 0;                    // 0 = Tools، 1 = Cape، 2 = Profiles
    private int cat = 0;                    // دستهٔ انتخاب‌شده در Tools
    private String selMod;                  // ماژول انتخاب‌شده (پنل جزئیات)
    private String search = "";
    private boolean searchFocus;
    private final float[] scroll = new float[TABS];
    private final float[] scrollT = new float[TABS];
    private final float[] hover = new float[ModuleManager.MODULES.size()];
    private final float[] on = new float[ModuleManager.MODULES.size()];
    private float tabY;
    private long openedAt;
    private String selectedId;
    private float yaw = 190f;
    private boolean dragging;
    private int lastDragX;
    private int x0, y0, W, H;
    private int cx, cy, cw, ch;
    private boolean scaleApplied;
    private int savedGuiScale;
    private boolean keepScale;   // وقتی صفحهٔ لاگین باز می‌شود، اسکیل ثابت بماند

    public MamadMenu() { this(null); }

    public MamadMenu(GuiScreen parent) { this.parent = parent; }

    // ---------------------------------------------------------------- چرخهٔ عمر

    @Override
    public void initGui() {
        if (openedAt == 0L) openedAt = System.currentTimeMillis();
        if (!scaleApplied) {
            // منو را روی اندازهٔ ثابت و خوانا نگه می‌داریم (GUI Scale خیلی بزرگ باعث بریده‌شدن می‌شد)
            scaleApplied = true;
            savedGuiScale = mc.gameSettings.guiScale;
            int cur = new ScaledResolution(mc).getScaleFactor();
            int fit = Math.max(1, Math.min(cur, Math.min(mc.displayWidth / 560, mc.displayHeight / 340)));
            if (fit != cur) {
                mc.gameSettings.guiScale = fit;
                ScaledResolution sr = new ScaledResolution(mc);
                setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());
                return;
            }
        }
        Keyboard.enableRepeatEvents(true);
        ImageCache.retryFailed();
        if (SiteAccount.isLoggedIn()) CapeStore.refresh();
        layout();
    }

    @Override
    public void onGuiClosed() {
        if (scaleApplied && !keepScale) { mc.gameSettings.guiScale = savedGuiScale; scaleApplied = false; }
        keepScale = false;
        Keyboard.enableRepeatEvents(false);
        CapeManager.previewOverride = null;
        CapeManager.previewFor = null;
        openedAt = 0L;
    }

    private void close() { mc.displayGuiScreen(parent); }

    private void openLogin() {
        keepScale = true;
        mc.displayGuiScreen(new LoginScreen(this));
    }

    private static float ease(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private float openT() {
        if (openedAt == 0L) return 1f;
        return Math.min(1f, (System.currentTimeMillis() - openedAt) / 220f);
    }

    private static float clampf(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void layout() {
        W = Math.min(width - 16, Math.min(780, Math.max(420, (int) (width * 0.86))));
        H = Math.min(height - 16, Math.min(450, Math.max(270, (int) (height * 0.84))));
        x0 = (width - W) / 2;
        y0 = (height - H) / 2 + (int) ((1f - ease(openT())) * 14f);   // باز شدن: کمی از پایین بالا می‌آید
        cx = x0 + SW + 14;
        cy = y0 + 48;
        cw = W - SW - 28;
        ch = H - 48 - 14;
    }

    /** اسکرول نرم و حرکت نرم نشانگر تب. */
    private void stepAnims() {
        for (int i = 0; i < TABS; i++) {
            float d = scrollT[i] - scroll[i];
            scroll[i] = Math.abs(d) < 0.4f ? scrollT[i] : scroll[i] + d * 0.3f;
        }
        float td = tab * 32f - tabY;
        tabY = Math.abs(td) < 0.3f ? tab * 32f : tabY + td * 0.3f;
    }

    private void resetScroll() {
        scroll[tab] = 0f;
        scrollT[tab] = 0f;
    }

    private void setTab(int i) {
        if (tab == i) return;
        tab = i;
        search = "";
        searchFocus = false;
        if (i == 1 && SiteAccount.isLoggedIn()) CapeStore.refresh();
    }

    // ---------------------------------------------------------------- ابزارهای رسم

    private void icon(ResourceLocation r, int x, int y, int size, int argb) {
        mc.getTextureManager().bindTexture(r);
        GlStateManager.enableBlend();
        GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** آیکون تب Profiles: سه لغزنده. */
    private void slidersIcon(int x, int y, int color) {
        Ui.roundRect(x, y + 1, 12, 2, 1, color);
        Ui.roundRect(x, y + 5, 12, 2, 1, color);
        Ui.roundRect(x, y + 9, 12, 2, 1, color);
        Ui.roundRect(x + 1, y, 4, 4, 2, color);
        Ui.roundRect(x + 7, y + 4, 4, 4, 2, color);
        Ui.roundRect(x + 3, y + 8, 4, 4, 2, color);
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

    /** متن فارسی/عربی را با بازآرایی راست‌به‌چپ می‌کشد (فونت ماینکرفت خودش حروف را نمی‌چسباند). */
    private void text(String s, int x, int y, int color) {
        boolean rtl = false;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0x0590 && s.charAt(i) <= 0x08FF) { rtl = true; break; }
        boolean old = fontRendererObj.getBidiFlag();
        if (rtl) fontRendererObj.setBidiFlag(true);
        fontRendererObj.drawString(s, x, y, color);
        fontRendererObj.setBidiFlag(old);
    }

    private void button(String label, int x, int y, int w, int h, int base, int hover, int text, int mx, int my) {
        boolean on = Ui.inside(mx, my, x, y, w, h);
        Ui.roundRect(x, y, w, h, h / 2, on ? hover : base);
        centered(label, x + w / 2, y + (h - 8) / 2, text);
    }

    /** کلید روشن/خاموش متحرک (۲۰×۱۰). t بین ۰ و ۱. */
    private void toggleSwitch(int x, int y, float t) {
        Ui.roundRect(x, y, 20, 10, 5, Ui.lerp(0xFF3C4048, Ui.ACCENT, t));
        Ui.roundRect(x + 1 + (int) (10 * t), y + 1, 8, 8, 4, 0xFFFFFFFF);
    }

    private boolean matches(String name) {
        return search.isEmpty() || name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private List<ModuleManager.Module> visibleModules() {
        List<ModuleManager.Module> out = new ArrayList<ModuleManager.Module>();
        for (ModuleManager.Module m : ModuleManager.MODULES) {
            if (cat != 0 && !CATS[cat].equals(category(m.name))) continue;
            if (matches(m.name)) out.add(m);
        }
        return out;
    }

    private List<CapeStore.Cosmetic> visibleCapes() {
        List<CapeStore.Cosmetic> out = new ArrayList<CapeStore.Cosmetic>();
        // فقط کیپ‌هایی که در سایت به اکانت اضافه شده‌اند (یا الان انتخاب‌شده‌اند)
        for (CapeStore.Cosmetic c : CapeStore.items) {
            boolean mine = CapeStore.owned.contains(c.id) || c.id.equals(CapeStore.equippedId);
            if (mine && c.name != null && matches(c.name)) out.add(c);
        }
        return out;
    }

    private CapeStore.Cosmetic selectedCape() {
        String id = selectedId != null ? selectedId : CapeStore.equippedId;
        if (id == null) return null;
        for (CapeStore.Cosmetic c : CapeStore.items) if (id.equals(c.id)) return c;
        return null;
    }

    // ---------------------------------------------------------------- دسته‌بندی و توضیح ماژول‌ها

    private static String category(String n) {
        switch (n) {
            case "CPS": case "Keystrokes": case "Armor Status": case "Reach Display": case "Combo Counter":
            case "Hitbox": case "No Hurt Cam": case "Friend/Enemy Colors":
                return "PvP";
            case "Fullbright": case "Custom Crosshair": case "Zoom": case "Time Changer": case "Weather Changer":
                return "Visual";
            case "Toggle Sprint": case "Toggle Sneak": case "Chat Tweaks":
                return "Utility";
            case "FPS Boost": case "Entity Culling":
                return "Perf";
            default:
                return "HUD";
        }
    }

    private static String categoryLabel(String n) {
        String c = category(n);
        return c.equals("Perf") ? "Performance" : c;
    }

    private static String desc(String n) {
        switch (n) {
            case "FPS": return "Shows your frames per second.";
            case "CPS": return "Shows how many times you click per second.";
            case "Ping": return "Shows your latency to the server.";
            case "Coordinates": return "Shows your X, Y and Z position.";
            case "Keystrokes": return "Shows WASD and mouse buttons while you press them.";
            case "Armor Status": return "Shows armor and held item durability.";
            case "Potion Effects": return "Lists your potion effects with timers.";
            case "Reach Display": return "Shows the distance of your last hit.";
            case "Toggle Sprint": return "Sprints automatically while you walk forward.";
            case "Toggle Sneak": return "Press sneak once to keep sneaking.";
            case "Fullbright": return "Maximum brightness. No dark caves.";
            case "No Hurt Cam": return "Removes the camera shake when you get hit.";
            case "Custom Crosshair": return "Pick your own crosshair shape and color.";
            case "Chat Tweaks": return "Cleaner chat: hide system lines and block spam.";
            case "Friend/Enemy Colors": return "Colors friends green and enemies red in chat.";
            case "Clock": return "Shows the current time.";
            case "Uptime": return "Shows how long the game has been open.";
            case "Memory": return "Shows the RAM the game is using.";
            case "Server Address": return "Shows the server you are playing on.";
            case "Combo Counter": return "Counts your hits in a row.";
            case "Zoom": return "Hold the zoom key to zoom in.";
            case "Time Changer": return "Keeps the world at midday.";
            case "Weather Changer": return "Removes rain and thunder.";
            case "Hitbox": return "Shows entity hitboxes.";
            case "Boss Bar": return "Show or hide the boss health bar.";
            case "Hot Bar": return "Show or hide the hotbar.";
            case "Tab": return "Show or hide the player list.";
            case "Watermark": return "Shows the Mamad Client logo on screen.";
            case "FPS Boost": return "Lighter graphics for more FPS. Turn it off to restore your settings.";
            case "Entity Culling": return "Skips drawing far away mobs (players are always drawn).";
            default: return "";
        }
    }

    // ---------------------------------------------------------------- رسم

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        stepAnims();
        float t = ease(openT());

        if (mc.theWorld != null) drawRect(0, 0, width, height, ((int) (0x80 * t)) << 24);
        else drawGradientRect(0, 0, width, height, (((int) (0xDD * t)) << 24) | 0x0B0B10, ((int) (0xF0 * t)) << 24);

        // پنجرهٔ شیشه‌ای
        Ui.roundRect(x0 - 1, y0 - 1, W + 2, H + 2, 15, 0x38FFFFFF);
        Ui.roundRect(x0, y0, W, H, 14, 0xC4101216);
        drawGradientRect(x0 + 14, y0 + 1, x0 + W - 14, y0 + H / 3, 0x18FFFFFF, 0x00FFFFFF);
        drawGradientRect(x0 + SW + 8, y0 + 1, x0 + W - 14, y0 + 34, 0x16FF5A00, 0x00FF5A00);
        Ui.roundRect(x0 + 6, y0 + 6, SW - 6, H - 12, 10, 0x44000000);

        drawSidebar(mx, my);
        drawHeader(mx, my);

        if (tab == 0) drawTools(mx, my);
        else if (tab == 1) drawCapes(mx, my);
        else drawProfiles(mx, my);

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawSidebar(int mx, int my) {
        Ui.logo(x0 + 16, y0 + 14, 24);
        fontRendererObj.drawStringWithShadow("Mamad Client", x0 + 46, y0 + 16, 0xFFFFFFFF);
        fontRendererObj.drawString("1.8.9", x0 + 46, y0 + 27, 0xFF8A8A92);

        String[] names = {"Tools", "Cape", "Profiles"};
        ResourceLocation[] icons = {ICON_GEAR, ICON_CAPE, null};
        int nx = x0 + 12, nw = SW - 18;

        // نشانگر متحرک تب انتخاب‌شده
        int ay = y0 + 56 + (int) tabY;
        Ui.roundRect(nx, ay, nw, 28, 10, 0x55FF5A00);
        Ui.roundRect(nx, ay + 7, 3, 14, 1, Ui.ACCENT);

        for (int i = 0; i < TABS; i++) {
            int ny = y0 + 56 + i * 32;
            boolean sel = tab == i;
            boolean hov = Ui.inside(mx, my, nx, ny, nw, 28);
            if (!sel && hov) Ui.roundRect(nx, ny, nw, 28, 10, 0x22FFFFFF);
            int col = sel ? Ui.ACCENT : 0xFFD0D0D6;
            if (icons[i] != null) icon(icons[i], nx + 9, ny + 8, 12, col);
            else slidersIcon(nx + 9, ny + 8, col);
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
        int px0 = ax + aw / 2 - 13, py0 = y0 + H - 37;
        boolean ph = Ui.inside(mx, my, px0, py0, 26, 26);
        Ui.roundRect(px0, py0, 26, 26, 13, ph ? 0xFFEF5350 : Ui.RED);
        Icons.power(px0 + 6, py0 + 6, 14, 0xFFFFFFFF);
    }

    private void drawHeader(int mx, int my) {
        String title = tab == 0 ? "Tools" : tab == 1 ? "Cape" : "Profiles";
        fontRendererObj.drawStringWithShadow(title, cx, y0 + 14, 0xFFFFFFFF);
        String sub;
        int subColor = 0xFF8A8A92;
        if (tab == 0) {
            int enabled = 0;
            for (ModuleManager.Module m : ModuleManager.MODULES) if (m.enabled) enabled++;
            sub = visibleModules().size() + " modules  -  " + enabled + " on";
        } else if (tab == 2) {
            sub = "One-click module sets";
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

        if (tab == 2) return;   // Profiles جستجو ندارد
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

    private void drawScrollbar(int x, int top, int h, float maxScroll, float pos) {
        if (maxScroll <= 0) return;
        int barH = Math.max(18, (int) (h * (h / (h + maxScroll))));
        int barY = top + (int) ((h - barH) * (pos / maxScroll));
        Ui.roundRect(x, barY, 3, barH, 1, 0x66FFFFFF);
    }

    // ---------------------------------------------------------------- Tools

    private int pw() { return cw >= 400 ? 150 : 132; }          // عرض پنل جزئیات
    private int tgw() { return cw - pw() - 10; }                 // عرض شبکهٔ کارت‌ها
    private int tgy() { return cy + 22; }                        // زیر ردیف دسته‌ها
    private int tgh() { return ch - 22; }
    private int toolCols() { return Math.max(2, Math.min(4, tgw() / 104)); }
    private int cardW(int cols) { return (tgw() - GAP * (cols - 1)) / cols; }
    private int cardX(int i, int cols) { return cx + (i % cols) * (cardW(cols) + GAP); }
    private int cardY(int i, int cols) { return tgy() + (i / cols) * (CARD_H + GAP) - (int) scroll[0]; }

    private ModuleManager.Module selectedModule() {
        if (selMod == null) return null;
        for (ModuleManager.Module m : ModuleManager.MODULES) if (m.name.equals(selMod)) return m;
        return null;
    }

    private void ensureSelection(List<ModuleManager.Module> mods) {
        for (ModuleManager.Module m : mods) if (m.name.equals(selMod)) return;
        selMod = mods.isEmpty() ? null : mods.get(0).name;
    }

    private int chipPad(int gw) {
        int total = 4 * (CATS.length - 1);
        for (String c : CATS) total += fontRendererObj.getStringWidth(c);
        int pad = 7;
        while (pad > 2 && total + pad * 2 * CATS.length > gw) pad--;
        return pad;
    }

    private int chipAt(int mx, int my) {
        int x = cx, pad = chipPad(tgw());
        for (int i = 0; i < CATS.length; i++) {
            int w = fontRendererObj.getStringWidth(CATS[i]) + pad * 2;
            if (Ui.inside(mx, my, x, cy, w, 16)) return i;
            x += w + 4;
        }
        return -1;
    }

    private void drawChips(int mx, int my) {
        int x = cx, pad = chipPad(tgw());
        for (int i = 0; i < CATS.length; i++) {
            int w = fontRendererObj.getStringWidth(CATS[i]) + pad * 2;
            boolean sel = cat == i;
            boolean hov = Ui.inside(mx, my, x, cy, w, 16);
            Ui.roundRect(x, cy, w, 16, 8, sel ? Ui.ACCENT : hov ? 0x40FFFFFF : 0x26FFFFFF);
            centered(CATS[i], x + w / 2, cy + 4, sel ? 0xFFFFFFFF : 0xFFC8C8CE);
            x += w + 4;
        }
    }

    private void drawTools(int mx, int my) {
        List<ModuleManager.Module> mods = visibleModules();
        ensureSelection(mods);
        drawChips(mx, my);

        int gw = tgw(), gy = tgy(), gh = tgh();
        int cols = toolCols();
        int cwid = cardW(cols);
        int rows = (mods.size() + cols - 1) / cols;
        float maxScroll = Math.max(0, rows * (CARD_H + GAP) - GAP - gh);
        scrollT[0] = clampf(scrollT[0], 0, maxScroll);
        scroll[0] = clampf(scroll[0], 0, maxScroll);

        clip(cx, gy, gw, gh);
        for (int i = 0; i < mods.size(); i++) {
            ModuleManager.Module m = mods.get(i);
            int idx = ModuleManager.MODULES.indexOf(m);
            int bx = cardX(i, cols);
            int by = cardY(i, cols);
            if (by + CARD_H < gy || by > gy + gh) continue;
            boolean hov = Ui.inside(mx, my, bx, by, cwid, CARD_H) && Ui.inside(mx, my, cx, gy, gw, gh);
            hover[idx] += ((hov ? 1f : 0f) - hover[idx]) * 0.3f;
            on[idx] += ((m.enabled ? 1f : 0f) - on[idx]) * 0.25f;

            int fill = Ui.lerp(0x28FFFFFF, 0x44FFFFFF, hover[idx]);
            if (m.name.equals(selMod)) Ui.roundRectBorder(bx, by, cwid, CARD_H, 12, 0x40FF5A00, Ui.ACCENT);
            else if (m.enabled) Ui.roundRectBorder(bx, by, cwid, CARD_H, 12, fill, 0x77FF5A00);
            else Ui.roundRect(bx, by, cwid, CARD_H, 12, fill);

            int tile = 30, tx = bx + (cwid - tile) / 2, ty = by + 10;
            Ui.glassTile(tx, ty, tile);
            Ui.roundRect(tx, ty, tile, tile, 8, Ui.lerp(0x00FF5A00, 0x44FF5A00, on[idx]));
            if (!Icons.draw(m.name, tx + 6, ty + 6, 18, 0xFFF1F3F6)) {
                centered(m.name.substring(0, 1).toUpperCase(Locale.ROOT), tx + tile / 2, ty + 11, 0xFFF1F3F6);
            }
            toggleSwitch(bx + cwid - 27, by + 7, on[idx]);
            centered(fontRendererObj.trimStringToWidth(m.name, cwid - 10), bx + cwid / 2, by + CARD_H - 20, 0xFFFFFFFF);
        }
        unclip();
        drawScrollbar(cx + gw - 4, gy, gh, maxScroll, scroll[0]);

        if (mods.isEmpty()) {
            centered("No modules found.", cx + gw / 2, gy + gh / 2 - 4, 0xFF8A8A92);
        }
        drawModulePanel(cx + gw + 10, pw(), mx, my);
    }

    private int settingsHeight(ModuleManager.Module m) {
        if (m == ModuleManager.CROSSHAIR) return 56;
        if (m == ModuleManager.CHAT) return 38;
        if (m == ModuleManager.ZOOM) return 24;
        if (m == ModuleManager.FRIENDS) return 24;
        return 0;
    }

    private int setTop(ModuleManager.Module m) {
        return cy + ch - 32 - 8 - settingsHeight(m);
    }

    private void drawModulePanel(int px, int pw, int mx, int my) {
        Ui.roundRect(px, cy, pw, ch, 12, 0x28FFFFFF);
        ModuleManager.Module m = selectedModule();
        if (m == null) {
            centered("Pick a module", px + pw / 2, cy + ch / 2 - 4, 0xFF8A8A92);
            return;
        }
        int mid = px + pw / 2;
        int tile = 38, tx = mid - tile / 2, ty = cy + 12;
        Ui.glassTile(tx, ty, tile);
        if (m.enabled) Ui.roundRect(tx, ty, tile, tile, 8, 0x44FF5A00);
        if (!Icons.draw(m.name, tx + 7, ty + 7, 24, 0xFFF1F3F6)) {
            centered(m.name.substring(0, 1).toUpperCase(Locale.ROOT), mid, ty + 15, 0xFFF1F3F6);
        }
        centered(fontRendererObj.trimStringToWidth(m.name, pw - 12), mid, ty + tile + 8, 0xFFFFFFFF);
        centered(categoryLabel(m.name), mid, ty + tile + 19, 0xFF8A8A92);

        int dy = ty + tile + 33;
        int maxY = setTop(m) - 4;
        List<String> lines = fontRendererObj.listFormattedStringToWidth(desc(m.name), pw - 20);
        for (int i = 0; i < lines.size() && dy + i * 10 + 8 <= maxY; i++) {
            centered(lines.get(i), mid, dy + i * 10, 0xFFB8B8C0);
        }

        drawModuleSettings(m, px, pw, mx, my);

        int bx = px + 12, bw = pw - 24, by = cy + ch - 32;
        if (m.enabled) button("Enabled", bx, by, bw, 22, Ui.ACCENT, Ui.ACCENT_HOVER, 0xFFFFFFFF, mx, my);
        else button("Disabled", bx, by, bw, 22, Ui.BTN, Ui.BTN_HOVER, 0xFFC8C8CE, mx, my);
    }

    private void settingRow(String label, boolean value, int x, int y, int w, int mx, int my) {
        boolean hov = Ui.inside(mx, my, x, y, w, 16);
        Ui.roundRect(x, y, w, 16, 8, hov ? 0x40FFFFFF : 0x26FFFFFF);
        fontRendererObj.drawString(label, x + 8, y + 4, 0xFFE0E0E6);
        toggleSwitch(x + w - 26, y + 3, value ? 1f : 0f);
    }

    private void drawModuleSettings(ModuleManager.Module m, int px, int pw, int mx, int my) {
        int bx = px + 12, bw = pw - 24, top = setTop(m), mid = px + pw / 2;
        if (m == ModuleManager.CROSSHAIR) {
            fontRendererObj.drawString("Shape", bx, top, 0xFF8A8A92);
            int sbw = (bw - 8) / 3;
            for (int i = 0; i < 3; i++) {
                int sx = bx + i * (sbw + 4);
                boolean sel = SHAPES[i].equals(ModuleManager.crossShape);
                boolean hov = Ui.inside(mx, my, sx, top + 10, sbw, 16);
                Ui.roundRect(sx, top + 10, sbw, 16, 8, sel ? Ui.ACCENT : hov ? 0x44FFFFFF : 0x30FFFFFF);
                centered(SHAPE_LABELS[i], sx + sbw / 2, top + 14, sel ? 0xFFFFFFFF : 0xFFC8C8CE);
            }
            fontRendererObj.drawString("Color", bx, top + 30, 0xFF8A8A92);
            int sz = (bw - 14) / 8;
            for (int i = 0; i < SWATCHES.length; i++) {
                int sx = bx + i * (sz + 2), sy = top + 41;
                boolean sel = SWATCHES[i] == (ModuleManager.crossColor & 0xFFFFFF);
                if (sel) Ui.roundRect(sx - 2, sy - 2, sz + 4, sz + 4, 4, 0xFFFFFFFF);
                Ui.roundRect(sx, sy, sz, sz, 3, 0xFF000000 | SWATCHES[i]);
            }
        } else if (m == ModuleManager.CHAT) {
            settingRow("Hide system chat", ModuleManager.hideSystem, bx, top, bw, mx, my);
            settingRow("Block spam", ModuleManager.filterSpam, bx, top + 20, bw, mx, my);
        } else if (m == ModuleManager.ZOOM) {
            String key = "C";
            if (ToolsHandler.zoomKey != null && ToolsHandler.zoomKey.getKeyCode() > 0) {
                key = Keyboard.getKeyName(ToolsHandler.zoomKey.getKeyCode());
            }
            centered("Hold " + key + " to zoom", mid, top + 2, 0xFFE0E0E6);
            centered("Change it in Controls", mid, top + 13, 0xFF8A8A92);
        } else if (m == ModuleManager.FRIENDS) {
            centered("Friends " + ModuleManager.friends.size() + "   Enemies " + ModuleManager.enemies.size(), mid, top + 2, 0xFFE0E0E6);
            centered("/mamad friend add name", mid, top + 13, 0xFF8A8A92);
        }
    }

    // ---------------------------------------------------------------- Profiles

    private static boolean structural(ModuleManager.Module m) {
        return m == ModuleManager.BOSSBAR || m == ModuleManager.HOTBAR || m == ModuleManager.TABLIST || m == ModuleManager.WATERMARK;
    }

    private static boolean profileHas(int p, String name) {
        for (String s : PROFILES[p]) if (s.equals(name)) return true;
        return false;
    }

    private static boolean profileActive(int p) {
        for (ModuleManager.Module m : ModuleManager.MODULES) {
            if (structural(m)) continue;
            if (m.enabled != profileHas(p, m.name)) return false;
        }
        return true;
    }

    private static void applyProfile(int p) {
        for (ModuleManager.Module m : ModuleManager.MODULES) {
            if (!structural(m)) m.enabled = profileHas(p, m.name);
        }
        ModuleManager.save();
    }

    private void drawProfiles(int mx, int my) {
        int rows = PROFILE_NAMES.length;
        float maxScroll = Math.max(0, rows * (PROF_H + GAP) - GAP - ch);
        scrollT[2] = clampf(scrollT[2], 0, maxScroll);
        scroll[2] = clampf(scroll[2], 0, maxScroll);

        clip(cx, cy, cw, ch);
        for (int i = 0; i < rows; i++) {
            int by = cy + i * (PROF_H + GAP) - (int) scroll[2];
            if (by + PROF_H < cy || by > cy + ch) continue;
            boolean active = profileActive(i);
            boolean hov = Ui.inside(mx, my, cx, by, cw, PROF_H) && Ui.inside(mx, my, cx, cy, cw, ch);
            if (active) Ui.roundRectBorder(cx, by, cw, PROF_H, 12, 0x30FF5A00, Ui.ACCENT);
            else Ui.roundRect(cx, by, cw, PROF_H, 12, hov ? 0x40FFFFFF : 0x28FFFFFF);

            int tile = 30, tx = cx + 10, ty = by + (PROF_H - tile) / 2;
            Ui.glassTile(tx, ty, tile);
            centered(PROFILE_NAMES[i].substring(0, 1), tx + tile / 2, ty + 11, 0xFFF1F3F6);
            fontRendererObj.drawStringWithShadow(PROFILE_NAMES[i], cx + 50, by + 10, 0xFFFFFFFF);
            List<String> lines = fontRendererObj.listFormattedStringToWidth(PROFILE_DESC[i], Math.max(60, cw - 50 - 96));
            for (int l = 0; l < lines.size() && l < 2; l++) {
                fontRendererObj.drawString(lines.get(l), cx + 50, by + 23 + l * 10, 0xFF8A8A92);
            }

            int bw = 70, bx = cx + cw - bw - 12, bY = by + (PROF_H - 22) / 2;
            if (active) {
                Ui.roundRect(bx, bY, bw, 22, 11, 0x336BE675);
                centered("Active", bx + bw / 2, bY + 7, 0xFF6BE675);
            } else {
                button("Apply", bx, bY, bw, 22, Ui.ACCENT, Ui.ACCENT_HOVER, 0xFFFFFFFF, mx, my);
            }
        }
        unclip();
        drawScrollbar(x0 + W - 9, cy, ch, maxScroll, scroll[2]);
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
        scrollT[1] = clampf(scrollT[1], 0, maxScroll);
        scroll[1] = clampf(scroll[1], 0, maxScroll);

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
            text(fontRendererObj.trimStringToWidth(c.name, cwid - 12), bx + 8, by + chei - 26, 0xFFFFFFFF);
            boolean eq = c.id.equals(CapeStore.equippedId);
            boolean own = CapeStore.owned.contains(c.id);
            String st = eq ? "Equipped" : own ? "Owned" : c.freeNow ? "Free" : "Locked";
            int stc = eq ? Ui.ACCENT : own ? 0xFF6BE675 : c.freeNow ? 0xFFDDDDDD : 0xFF8A8A92;
            fontRendererObj.drawString(st, bx + 8, by + chei - 14, stc);
        }
        unclip();
        drawScrollbar(x0 + W - 9, cy, ch, maxScroll, scroll[1]);
        if (capes.isEmpty() && CapeStore.loadedOnce && !CapeStore.busy) {
            centered("No capes on your account yet.", cx + gw / 2, cy + ch / 2 - 10, 0xFFDDDDDD);
            centered("Add some on the MamadCraft site (capes page).", cx + gw / 2, cy + ch / 2 + 4, 0xFF8A8A92);
        }

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
            String nm = fontRendererObj.trimStringToWidth(sel.name, pw - 16);
            text(nm, px + (pw - fontRendererObj.getStringWidth(nm)) / 2, cy + ch - 62, 0xFFFFFFFF);
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
        if (button != 0 && button != 1) return;
        layout();

        // کلیک راست روی کارت ماژول = روشن/خاموش سریع
        if (button == 1) {
            if (tab == 0) quickToggle(mx, my);
            return;
        }

        // نوار کناری
        for (int i = 0; i < TABS; i++) {
            if (Ui.inside(mx, my, x0 + 12, y0 + 56 + i * 32, SW - 18, 28)) { setTab(i); return; }
        }
        if (Ui.inside(mx, my, x0 + 12, y0 + H - 64, SW - 18, 22)) { openLogin(); return; }
        if (Ui.inside(mx, my, x0 + 12 + (SW - 18) / 2 - 13, y0 + H - 37, 26, 26)) { close(); return; }

        // جستجو
        int sw = 132, sx = x0 + W - 14 - sw, sy = y0 + 12;
        searchFocus = tab != 2 && Ui.inside(mx, my, sx, sy, sw, 20);
        if (searchFocus) return;

        if (!Ui.inside(mx, my, x0, y0, W, H)) { close(); return; }

        if (tab == 0) clickTools(mx, my);
        else if (tab == 1) clickCapes(mx, my);
        else clickProfiles(mx, my);
    }

    private int cardAt(int mx, int my, int count) {
        if (!Ui.inside(mx, my, cx, tgy(), tgw(), tgh())) return -1;
        int cols = toolCols();
        int cwid = cardW(cols);
        for (int i = 0; i < count; i++) {
            if (Ui.inside(mx, my, cardX(i, cols), cardY(i, cols), cwid, CARD_H)) return i;
        }
        return -1;
    }

    private void quickToggle(int mx, int my) {
        List<ModuleManager.Module> mods = visibleModules();
        int i = cardAt(mx, my, mods.size());
        if (i < 0) return;
        ModuleManager.Module m = mods.get(i);
        selMod = m.name;
        m.enabled = !m.enabled;
        ModuleManager.save();
    }

    private void clickTools(int mx, int my) {
        int chip = chipAt(mx, my);
        if (chip >= 0) {
            if (cat != chip) { cat = chip; resetScroll(); }
            return;
        }
        ModuleManager.Module sel = selectedModule();
        int px = cx + tgw() + 10;
        if (sel != null && clickModulePanel(sel, mx, my, px, pw())) return;

        List<ModuleManager.Module> mods = visibleModules();
        int i = cardAt(mx, my, mods.size());
        if (i < 0) return;
        ModuleManager.Module m = mods.get(i);
        int cols = toolCols();
        int bx = cardX(i, cols), by = cardY(i, cols), cwid = cardW(cols);
        if (Ui.inside(mx, my, bx + cwid - 30, by + 3, 28, 18)) {   // روی خود کلید: روشن/خاموش
            m.enabled = !m.enabled;
            ModuleManager.save();
        }
        selMod = m.name;
    }

    /** کلیک داخل پنل جزئیات؛ true یعنی کلیک مصرف شد. */
    private boolean clickModulePanel(ModuleManager.Module m, int mx, int my, int px, int pw) {
        int bx = px + 12, bw = pw - 24, by = cy + ch - 32;
        if (Ui.inside(mx, my, bx, by, bw, 22)) {
            m.enabled = !m.enabled;
            ModuleManager.save();
            return true;
        }
        int top = setTop(m);
        if (m == ModuleManager.CROSSHAIR) {
            int sbw = (bw - 8) / 3;
            for (int i = 0; i < 3; i++) {
                if (Ui.inside(mx, my, bx + i * (sbw + 4), top + 10, sbw, 16)) {
                    ModuleManager.crossShape = SHAPES[i];
                    ModuleManager.save();
                    return true;
                }
            }
            int sz = (bw - 14) / 8;
            for (int i = 0; i < SWATCHES.length; i++) {
                if (Ui.inside(mx, my, bx + i * (sz + 2), top + 41, sz, sz)) {
                    ModuleManager.crossColor = SWATCHES[i];
                    ModuleManager.save();
                    return true;
                }
            }
        } else if (m == ModuleManager.CHAT) {
            if (Ui.inside(mx, my, bx, top, bw, 16)) {
                ModuleManager.hideSystem = !ModuleManager.hideSystem;
                ModuleManager.save();
                return true;
            }
            if (Ui.inside(mx, my, bx, top + 20, bw, 16)) {
                ModuleManager.filterSpam = !ModuleManager.filterSpam;
                ModuleManager.save();
                return true;
            }
        }
        return Ui.inside(mx, my, px, cy, pw, ch);
    }

    private void clickProfiles(int mx, int my) {
        if (!Ui.inside(mx, my, cx, cy, cw, ch)) return;
        for (int i = 0; i < PROFILE_NAMES.length; i++) {
            int by = cy + i * (PROF_H + GAP) - (int) scroll[2];
            if (Ui.inside(mx, my, cx, by, cw, PROF_H)) {
                applyProfile(i);
                return;
            }
        }
    }

    private void clickCapes(int mx, int my) {
        if (!SiteAccount.isLoggedIn()) {
            if (Ui.inside(mx, my, cx + cw / 2 - 60, cy + ch / 2 - 4, 120, 22)) openLogin();
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
        if (d != 0) scrollT[tab] -= (d > 0 ? 1 : -1) * 34f;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT) { close(); return; }
        if (!searchFocus) {
            // میان‌بر تب‌ها: 1 / 2 / 3
            if (key == Keyboard.KEY_1) setTab(0);
            else if (key == Keyboard.KEY_2) setTab(1);
            else if (key == Keyboard.KEY_3) setTab(2);
            return;
        }
        if (key == Keyboard.KEY_RETURN) { searchFocus = false; return; }
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            resetScroll();
            return;
        }
        if (ChatAllowedCharacters.isAllowedCharacter(c) && search.length() < 24) { search += c; resetScroll(); }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
