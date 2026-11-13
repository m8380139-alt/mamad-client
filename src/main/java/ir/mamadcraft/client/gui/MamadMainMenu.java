package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.account.SiteAccount;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** منوی اصلی Mamad Client: پس‌زمینهٔ تار ماینکرفت، پروفایل بالا، لوگوی M وسط، دکمه‌های گرد. */
public class MamadMainMenu extends GuiScreen {
    private static final class Btn {
        final int id; final String label; int x, y, w, h;
        Btn(int id, String label) { this.id = id; this.label = label; }
        boolean hit(int mx, int my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    }

    private final GuiMainMenu bg = new GuiMainMenu();
    private Method skybox;
    private boolean skyboxFailed;
    private final List<Btn> buttons = new ArrayList<Btn>();
    private int acctX, acctY, acctW;

    @Override
    public void initGui() {
        try { bg.setWorldAndResolution(mc, width, height); } catch (Throwable t) { skyboxFailed = true; }
        buttons.clear();
        String[] names = {"Singleplayer", "Multiplayer", "Options"};
        for (int i = 0; i < 3; i++) {
            Btn b = new Btn(i, names[i]);
            b.w = 190; b.h = 24; b.x = width / 2 - 95; b.y = height / 2 - 4 + i * 30;
            buttons.add(b);
        }
        Btn mods = new Btn(4, "Mods"); mods.w = 56; mods.h = 20; mods.x = width / 2 - 60; mods.y = height - 30; buttons.add(mods);
        Btn cl = new Btn(5, "Client"); cl.w = 56; cl.h = 20; cl.x = width / 2 + 4; cl.y = height - 30; buttons.add(cl);
        Btn quit = new Btn(3, "X"); quit.w = 20; quit.h = 20; quit.x = width - 28; quit.y = 8; buttons.add(quit);
    }

    @Override
    public void updateScreen() {
        try { bg.updateScreen(); } catch (Throwable ignored) {}
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

        // پروفایل بازی (بالا چپ)
        int pw = 24 + fontRendererObj.getStringWidth(user) + 8;
        Ui.roundRect(8, 8, pw, 20, 10, 0xCC141418);
        Ui.logo(12, 11, 14);
        fontRendererObj.drawStringWithShadow(user, 30, 14, 0xFFFFFFFF);

        // حساب سایت (بالا راست): کلیک = ورود
        String acct = SiteAccount.isLoggedIn() ? SiteAccount.getUsername() : "Sign in";
        acctW = 24 + fontRendererObj.getStringWidth(acct) + 8;
        acctX = width - 28 - 6 - acctW;
        acctY = 8;
        boolean ah = Ui.inside(mx, my, acctX, acctY, acctW, 20);
        Ui.roundRect(acctX, acctY, acctW, 20, 10, ah ? 0xE02A2A30 : 0xCC141418);
        Ui.logo(acctX + 4, acctY + 3, 14);
        fontRendererObj.drawStringWithShadow(acct, acctX + 22, acctY + 6, SiteAccount.isLoggedIn() ? 0xFFFFFFFF : 0xFFFFB27A);

        int ls = 72;
        Ui.logo(width / 2 - ls / 2, height / 2 - ls - 12, ls);

        for (Btn b : buttons) {
            boolean hover = b.hit(mx, my);
            int col;
            if (b.id == 3) col = hover ? 0xFFFF4040 : Ui.RED;
            else col = hover ? Ui.BTN_HOVER : Ui.BTN;
            Ui.roundRect(b.x, b.y, b.w, b.h, b.h / 2, col);
            fontRendererObj.drawString(b.label, b.x + b.w / 2 - fontRendererObj.getStringWidth(b.label) / 2, b.y + (b.h - 8) / 2, 0xFFFFFFFF);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        if (Ui.inside(mx, my, acctX, acctY, acctW, 20)) { mc.displayGuiScreen(new LoginScreen(this)); return; }
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
