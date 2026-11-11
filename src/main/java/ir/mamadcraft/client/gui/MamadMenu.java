package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** منوی کلاینت (Right Shift): تب Tools برای ماژول‌ها، تب Custom برای کیپ. */
public class MamadMenu extends GuiScreen {
    private static final int W = 260, H = 190;
    private int tab = 0;

    private int x() { return (width - W) / 2; }
    private int y() { return (height - H) / 2; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        int x = x(), y = y();
        Gui.drawRect(x - 1, y - 1, x + W + 1, y + H + 1, BrandingHandler.ORANGE);
        Gui.drawRect(x, y, x + W, y + H, 0xF0101014);
        fontRendererObj.drawStringWithShadow("Mamad Client", x + 10, y + 8, BrandingHandler.ORANGE);

        String[] tabs = {"Tools", "Custom"};
        for (int i = 0; i < 2; i++) {
            int tx = x + 10 + i * 70;
            Gui.drawRect(tx, y + 24, tx + 64, y + 40, tab == i ? 0xFFFF7A00 : 0xFF26262C);
            fontRendererObj.drawString(tabs[i], tx + 32 - fontRendererObj.getStringWidth(tabs[i]) / 2, y + 28,
                    tab == i ? 0xFF000000 : 0xFFFFFFFF);
        }

        if (tab == 0) {
            int ry = y + 50;
            for (ModuleManager.Module m : ModuleManager.MODULES) {
                Gui.drawRect(x + 10, ry, x + W - 10, ry + 24, 0xFF1B1B20);
                fontRendererObj.drawStringWithShadow(m.name, x + 18, ry + 8, 0xFFFFFFFF);
                int px = x + W - 56;
                Gui.drawRect(px, ry + 5, px + 38, ry + 19, m.enabled ? 0xFFFF7A00 : 0xFF3A3A42);
                fontRendererObj.drawString(m.enabled ? "ON" : "OFF", px + 19 - fontRendererObj.getStringWidth(m.enabled ? "ON" : "OFF") / 2,
                        ry + 8, m.enabled ? 0xFF000000 : 0xFFAAAAAA);
                ry += 28;
            }
        } else {
            fontRendererObj.drawStringWithShadow("Your cape comes from your MamadCraft site account.", x + 14, y + 56, 0xFFFFFFFF);
            fontRendererObj.drawStringWithShadow("Change it on the site; it updates in ~20 seconds.", x + 14, y + 70, 0xFFAAAAAA);
            fontRendererObj.drawStringWithShadow("In-game cape picker: coming soon.", x + 14, y + 92, BrandingHandler.ORANGE);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        int x = x(), y = y();
        for (int i = 0; i < 2; i++) {
            int tx = x + 10 + i * 70;
            if (mx >= tx && mx <= tx + 64 && my >= y + 24 && my <= y + 40) tab = i;
        }
        if (tab == 0) {
            int ry = y + 50;
            for (ModuleManager.Module m : ModuleManager.MODULES) {
                if (mx >= x + 10 && mx <= x + W - 10 && my >= ry && my <= ry + 24) {
                    m.enabled = !m.enabled;
                    ModuleManager.save();
                }
                ry += 28;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
