package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Custom Crosshair: شکل (cross/dot/circle) و رنگ با دستور /mamad crosshair. */
public class CrosshairHandler {
    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !ModuleManager.CROSSHAIR.enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView != 0) return;
        e.setCanceled(true);
        int cx = e.resolution.getScaledWidth() / 2, cy = e.resolution.getScaledHeight() / 2;
        int c = 0xFF000000 | ModuleManager.crossColor;
        String shape = ModuleManager.crossShape;
        if ("dot".equals(shape)) {
            Gui.drawRect(cx - 1, cy - 1, cx + 1, cy + 1, c);
        } else if ("circle".equals(shape)) {
            for (int a = 0; a < 360; a += 15) {
                int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * 5);
                int y = cy + (int) Math.round(Math.sin(Math.toRadians(a)) * 5);
                Gui.drawRect(x, y, x + 1, y + 1, c);
            }
        } else {
            Gui.drawRect(cx - 8, cy, cx - 3, cy + 1, c);
            Gui.drawRect(cx + 3, cy, cx + 8, cy + 1, c);
            Gui.drawRect(cx, cy - 8, cx + 1, cy - 3, c);
            Gui.drawRect(cx, cy + 3, cx + 1, cy + 8, c);
        }
    }
}
