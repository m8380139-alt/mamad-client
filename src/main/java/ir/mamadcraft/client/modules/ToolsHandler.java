package ir.mamadcraft.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

/** ابزارهای تازه: Clock, Uptime, Memory, Server Address, Combo Counter, Zoom, Time/Weather Changer, Hitbox و نمایش/مخفی‌کردن Boss Bar, Hot Bar, Tab. */
public class ToolsHandler {
    public static KeyBinding zoomKey;

    private static final long START = System.currentTimeMillis();
    private static final int ORANGE = 0xFFFF7A00;
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm");

    private int combo;
    private long lastHit;
    private int prevResist;
    private boolean hitboxSet;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.entityPlayer != mc.thePlayer || !(e.target instanceof EntityLivingBase)) return;
        long now = System.currentTimeMillis();
        if (now - lastHit > 2500) combo = 0;
        combo++;
        lastHit = now;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        int resist = mc.thePlayer.hurtResistantTime;
        if (resist > prevResist + 5) combo = 0;   // ضربه خوردی؛ کامبو قطع می‌شود
        prevResist = resist;

        if (ModuleManager.TIME.enabled) mc.theWorld.setWorldTime(6000L);
        if (ModuleManager.WEATHER.enabled) {
            mc.theWorld.setRainStrength(0f);
            mc.theWorld.setThunderStrength(0f);
        }

        boolean hb = ModuleManager.HITBOX.enabled;
        if (hb != hitboxSet) {
            mc.getRenderManager().setDebugBoundingBox(hb);
            hitboxSet = hb;
        }
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        if (ModuleManager.ZOOM.enabled && zoomKey != null && zoomKey.isKeyDown()) e.newfov *= 0.25f;
    }

    @SubscribeEvent
    public void onPre(RenderGameOverlayEvent.Pre e) {
        switch (e.type) {
            case BOSSHEALTH:
                if (!ModuleManager.BOSSBAR.enabled) e.setCanceled(true);
                break;
            case HOTBAR:
                if (!ModuleManager.HOTBAR.enabled) e.setCanceled(true);
                break;
            case PLAYER_LIST:
                if (!ModuleManager.TABLIST.enabled) e.setCanceled(true);
                break;
            default:
                break;
        }
    }

    private int line(FontRenderer fr, int w, int y, String label, String value) {
        String full = label + ": " + value;
        int x = w / 2 - fr.getStringWidth(full) / 2;
        fr.drawStringWithShadow(label + ": ", x, y, ORANGE);
        fr.drawStringWithShadow(value, x + fr.getStringWidth(label + ": "), y, 0xFFFFFFFF);
        return y + 11;
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.thePlayer == null) return;
        FontRenderer fr = mc.fontRendererObj;
        int w = e.resolution.getScaledWidth();
        int y = 22;

        if (ModuleManager.CLOCK.enabled) y = line(fr, w, y, "Time", clockFormat.format(new Date()));
        if (ModuleManager.UPTIME.enabled) {
            long s = (System.currentTimeMillis() - START) / 1000L;
            y = line(fr, w, y, "Uptime", String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
        }
        if (ModuleManager.MEMORY.enabled) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
            y = line(fr, w, y, "Memory", used + "/" + (rt.maxMemory() >> 20) + " MB");
        }
        if (ModuleManager.SERVER.enabled) {
            ServerData sd = mc.getCurrentServerData();
            y = line(fr, w, y, "Server", sd == null ? "Singleplayer" : sd.serverIP);
        }
        if (ModuleManager.COMBO.enabled && combo > 0 && System.currentTimeMillis() - lastHit < 2500) {
            line(fr, w, y, "Combo", String.valueOf(combo));
        }
    }
}
