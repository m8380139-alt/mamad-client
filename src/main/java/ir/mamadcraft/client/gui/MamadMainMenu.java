package ir.mamadcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** منوی اصلی Mamad Client: پس‌زمینهٔ تار ماینکرفت، پروفایل بالا، لوگوی M وسط، دکمه‌های گرد. */
public class MamadMainMenu extends GuiScreen {
    private static final ResourceLocation LOGO = new ResourceLocation("mamadclient", "textures/logo.png");

    private static final class Btn {
        final int id; final String label; int x, y, w, h;
        Btn(int id, String label) { this.id = id; this.label = label; }
        boolean hit(int mx, int my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    }

    private final GuiMainMenu bg = new GuiMainMenu();
    private Method skybox;
    private boolean skyboxFailed;
    private final List<Btn> buttons = new ArrayList<Btn>();

    @Override
    public void initGui() {
        try { bg.setWorldAndResolution(mc, width, height); } catch (Throwable t) { skyboxFailed = true; }
        buttons.clear();
        String[] names = {"Singleplayer", "Multiplayer", "Options"};
        for (int i = 0; i < 3; i++) {
            Btn b = new Btn(i, names[i]);
            b.w = 140; b.h = 20; b.x = width / 2 - 70; b.y = height / 2 + 4 + i * 26;
            buttons.add(b);
        }
        Btn mods = new Btn(4, "Mods"); mods.w = 46; mods.h = 18; mods.x = width / 2 - 50; mods.y = height - 26; buttons.add(mods);
        Btn cl = new Btn(5, "Client"); cl.w = 46; cl.h = 18; cl.x = width / 2 + 4; cl.y = height - 26; buttons.add(cl);
        Btn quit = new Btn(3, "X"); quit.w = 20; quit.h = 20; quit.x = width - 28; quit.y = 8; buttons.add(quit);
    }

    @Override
    public void updateScreen() {
        try { bg.updateScreen(); } catch (Throwable ignored) {}
    }

    private static void roundRect(int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int dy = 0; dy < h; dy++) {
            int inset = 0;
            if (dy < r) inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - dy) * (r - dy)));
            else if (dy >= h - r) { int d = h - 1 - dy; inset = (int) Math.round(r - Math.sqrt((double) r * r - (double) (r - d) * (r - d))); }
            Gui.drawRect(x + inset, y + dy, x + w - inset, y + dy + 1, color);
        }
    }

    private void logo(int x, int y, int size) {
        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    private void background(int mx, int my, float pt) {
        boolean ok = false;
        if (!skyboxFailed) {
            try {
                if (skybox == null) {
                    skybox = ReflectionHelper.findMethod(GuiMainMenu.class, bg,
                            new String[]{"renderSkybox", "func_73971_c"}, int.class, int.class, float.class);
                }
                skybox.invoke(bg, mx, my, pt);
                ok = true;
            } catch (Throwable t) { skyboxFailed = true; }
        }
        if (!ok) drawGradientRect(0, 0, width, height, 0xFF1A0F05, 0xFF0A0A0C);
        drawGradientRect(0, 0, width, height, 0x66000000, 0xB0000000);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        background(mx, my, pt);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        String user = mc.getSession().getUsername();

        int pw = 24 + fontRendererObj.getStringWidth(user) + 8;
        roundRect(8, 8, pw, 20, 10, 0xCC141418);
        logo(12, 11, 14);
        fontRendererObj.drawStringWithShadow(user, 30, 14, 0xFFFFFFFF);

        int rw = 24 + fontRendererObj.getStringWidth(user) + 8;
        roundRect(width - 28 - 6 - rw, 8, rw, 20, 10, 0xCC141418);
        logo(width - 28 - 6 - rw + 4, 11, 14);
        fontRendererObj.drawStringWithShadow(user, width - 28 - 6 - rw + 22, 14, 0xFFFFFFFF);

        int ls = 72;
        logo(width / 2 - ls / 2, height / 2 - ls - 14, ls);
        String title = "Mamad Client";
        fontRendererObj.drawStringWithShadow(title, width / 2 - fontRendererObj.getStringWidth(title) / 2, height / 2 - 10, BrandingHandler.ORANGE);

        for (Btn b : buttons) {
            boolean hover = b.hit(mx, my);
            int col = b.id == 3 ? (hover ? 0xFFFF4040 : 0xFFD83A3A) : (hover ? 0xFFFF7A00 : 0xCC1B1B20);
            roundRect(b.x, b.y, b.w, b.h, b.h / 2, col);
            int tc = (hover && b.id != 3) ? 0xFF000000 : 0xFFFFFFFF;
            fontRendererObj.drawString(b.label, b.x + b.w / 2 - fontRendererObj.getStringWidth(b.label) / 2, b.y + (b.h - 8) / 2, tc);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        for (Btn b : buttons) {
            if (!b.hit(mx, my)) continue;
            switch (b.id) {
                case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
                case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
                case 2: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
                case 3: mc.shutdown(); break;
                case 4: mc.displayGuiScreen(new GuiModList(this)); break;
                case 5: mc.displayGuiScreen(new MamadMenu()); break;
            }
            return;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException { /* Esc روی منوی اصلی کاری نکنه */ }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
