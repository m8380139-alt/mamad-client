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

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.thePlayer == null) return;

        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
        if (ModuleManager.FPS.enabled) rows.add(new String[]{"FPS", String.valueOf(Minecraft.getDebugFPS())});
        if (ModuleManager.CPS.enabled) rows.add(new String[]{"CPS", String.valueOf(cps())});
        if (ModuleManager.PING.enabled) {
            int ping = 0;
            if (mc.getNetHandler() != null) {
                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (info != null) ping = info.getResponseTime();
            }
            rows.add(new String[]{"Ping", ping + " ms"});
        }
        if (ModuleManager.COORDS.enabled) {
            rows.add(new String[]{"XYZ", (int) mc.thePlayer.posX + " " + (int) mc.thePlayer.posY + " " + (int) mc.thePlayer.posZ});
        }
        if (rows.isEmpty()) return;

        // یک پنل گرد و نیمه‌شفاف با خط نارنجی کنار؛ مقدارها زیر هم تراز می‌شوند
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        int labelW = 0, valueW = 0;
        for (String[] r : rows) {
            labelW = Math.max(labelW, fr.getStringWidth(r[0]));
            valueW = Math.max(valueW, fr.getStringWidth(r[1]));
        }
        int x = 4, y = 24, pad = 6, gap = 6;
        int w = pad + labelW + gap + valueW + pad;
        int h = rows.size() * 11 + 7;
        Ui.roundRect(x, y, w, h, 6, 0x9A101216);
        Ui.roundRectV(x, y + 4, 2, h - 8, 1, Ui.rgb(0f), Ui.rgb(0.35f));
        int ty = y + 4, ri = 0;
        for (String[] r : rows) {
            fr.drawStringWithShadow(r[0], x + pad, ty, Ui.rgb(ri++ * 0.09f));
            fr.drawStringWithShadow(r[1], x + pad + labelW + gap, ty, 0xFFFFFFFF);
            ty += 11;
        }
    }
}
