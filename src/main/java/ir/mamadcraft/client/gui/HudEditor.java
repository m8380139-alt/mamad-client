package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** صفحهٔ ویرایش HUD: المان‌ها را با ماوس بکش؛ R = بازنشانی، فلش‌ها = جابه‌جایی ۱ پیکسلی، Esc = ذخیره و خروج. */
public class HudEditor extends GuiScreen {
    /** از دستور چت درخواست می‌شود؛ چون چت بعد از اجرای دستور خودش را می‌بندد، صفحه در تیک بعدی باز می‌شود. */
    public static boolean openRequested = false;

    private int dragging = -1, selected = -1;
    private int startMx, startMy, startDx, startDy;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private int hit(int mx, int my) {
        for (int i = HudLayout.COUNT - 1; i >= 0; i--) {
            if (!HudLayout.visible(i)) continue;
            int x = HudLayout.bx[i] + HudLayout.dx[i], y = HudLayout.by[i] + HudLayout.dy[i];
            if (mx >= x && mx < x + HudLayout.bw[i] && my >= y && my < y + HudLayout.bh[i]) return i;
        }
        return -1;
    }

    private static void outline(int x, int y, int w, int h, int c) {
        Gui.drawRect(x - 1, y - 1, x + w + 1, y, c);
        Gui.drawRect(x - 1, y + h, x + w + 1, y + h + 1, c);
        Gui.drawRect(x - 1, y, x, y + h, c);
        Gui.drawRect(x + w, y, x + w + 1, y + h, c);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        FontRenderer fr = fontRendererObj;
        int hover = dragging >= 0 ? dragging : hit(mx, my);
        boolean any = false;
        for (int i = 0; i < HudLayout.COUNT; i++) {
            if (!HudLayout.visible(i)) continue;
            any = true;
            int x = HudLayout.bx[i] + HudLayout.dx[i], y = HudLayout.by[i] + HudLayout.dy[i];
            boolean active = i == hover || i == selected;
            outline(x, y, HudLayout.bw[i], HudLayout.bh[i], active ? 0xFF8B5CF6 : 0x66FFFFFF);
            if (active) fr.drawStringWithShadow(HudLayout.NAMES[i], x, y - 10 < 0 ? y + HudLayout.bh[i] + 3 : y - 10, 0xFFA78BFA);
        }
        String help = any ? "Drag to move  |  Arrows: nudge  |  R: reset  |  Esc: save"
                : "Turn on some HUD modules in the menu (Right Shift) first  |  Esc: back";
        int hw = fr.getStringWidth(help) + 16;
        Ui.hudPanel(width / 2 - hw / 2, height - 24, hw, 14, Ui.ACCENT);
        fr.drawStringWithShadow(help, width / 2 - hw / 2 + 8, height - 21, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        int h = hit(mx, my);
        selected = h;
        if (h < 0) return;
        dragging = h;
        startMx = mx; startMy = my;
        startDx = HudLayout.dx[h]; startDy = HudLayout.dy[h];
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging < 0) return;
        HudLayout.dx[dragging] = startDx + (mx - startMx);
        HudLayout.dy[dragging] = startDy + (my - startMy);
        HudLayout.clamp(dragging, width, height);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (dragging >= 0) { dragging = -1; ModuleManager.save(); }
    }

    @Override
    protected void keyTyped(char c, int key) throws java.io.IOException {
        if (key == Keyboard.KEY_R) { HudLayout.reset(); ModuleManager.save(); return; }
        if (selected >= 0 && HudLayout.visible(selected)) {
            int ddx = key == Keyboard.KEY_LEFT ? -1 : key == Keyboard.KEY_RIGHT ? 1 : 0;
            int ddy = key == Keyboard.KEY_UP ? -1 : key == Keyboard.KEY_DOWN ? 1 : 0;
            if (ddx != 0 || ddy != 0) {
                HudLayout.dx[selected] += ddx; HudLayout.dy[selected] += ddy;
                HudLayout.clamp(selected, width, height);
                return;
            }
        }
        super.keyTyped(c, key);
    }

    @Override
    public void onGuiClosed() { ModuleManager.save(); }
}
