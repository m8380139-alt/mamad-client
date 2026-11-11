package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/** نمایش ماژول‌های FPS / CPS / Ping / مختصات روی صفحه. */
public class HudHandler {
    private final Deque<Long> clicks = new ArrayDeque<Long>();

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 0 && e.buttonstate) clicks.addLast(System.currentTimeMillis());
    }

    private int cps() {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
        return clicks.size();
    }

    private void line(Minecraft mc, int y, String label, String value) {
        mc.fontRendererObj.drawStringWithShadow(label + ": ", 4, y, BrandingHandler.ORANGE);
        mc.fontRendererObj.drawStringWithShadow(value, 4 + mc.fontRendererObj.getStringWidth(label + ": "), y, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.thePlayer == null) return;
        int y = 24;
        if (ModuleManager.FPS.enabled) { line(mc, y, "FPS", String.valueOf(Minecraft.getDebugFPS())); y += 11; }
        if (ModuleManager.CPS.enabled) { line(mc, y, "CPS", String.valueOf(cps())); y += 11; }
        if (ModuleManager.PING.enabled) {
            int ping = 0;
            if (mc.getNetHandler() != null) {
                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (info != null) ping = info.getResponseTime();
            }
            line(mc, y, "Ping", ping + " ms"); y += 11;
        }
        if (ModuleManager.COORDS.enabled) {
            line(mc, y, "XYZ", (int) mc.thePlayer.posX + " " + (int) mc.thePlayer.posY + " " + (int) mc.thePlayer.posZ);
        }
    }
}
