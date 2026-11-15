package ir.mamadcraft.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.util.Map;

/**
 * ماژول‌های بهینه‌سازی (سبک OptiFine، ولی خودمان):
 * FPS Boost = یک پریست تنظیمات سبک (ذرات، ابر، سایه، AO، vsync)؛ با خاموش‌کردن همه به حالت قبل برمی‌گردند.
 * Entity Culling = موجودات دور (غیر از بازیکن‌ها) رندر نمی‌شوند.
 */
public class PerfHandler {
    private static final double CULL_DIST_SQ = 40.0 * 40.0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        GameSettings gs = mc.gameSettings;
        if (gs == null) return;

        Map<String, String> orig = ModuleManager.perfOrig;
        boolean want = ModuleManager.BOOST.enabled;
        boolean applied = !orig.isEmpty();

        if (want && !applied) {
            orig.put("particles", String.valueOf(gs.particleSetting));
            orig.put("clouds", String.valueOf(gs.clouds));
            orig.put("fancy", String.valueOf(gs.fancyGraphics));
            orig.put("ao", String.valueOf(gs.ambientOcclusion));
            orig.put("shadows", String.valueOf(gs.entityShadows));
            orig.put("fps", String.valueOf(gs.limitFramerate));
            orig.put("vsync", String.valueOf(gs.enableVsync));
            gs.particleSetting = 2;
            gs.clouds = 0;
            gs.fancyGraphics = false;
            gs.ambientOcclusion = 0;
            gs.entityShadows = false;
            gs.limitFramerate = 260;
            gs.enableVsync = false;
            Display.setVSyncEnabled(false);
            mc.renderGlobal.loadRenderers();
            ModuleManager.save();
        } else if (!want && applied) {
            try {
                gs.particleSetting = Integer.parseInt(orig.get("particles"));
                gs.clouds = Integer.parseInt(orig.get("clouds"));
                gs.fancyGraphics = Boolean.parseBoolean(orig.get("fancy"));
                gs.ambientOcclusion = Integer.parseInt(orig.get("ao"));
                gs.entityShadows = Boolean.parseBoolean(orig.get("shadows"));
                gs.limitFramerate = Integer.parseInt(orig.get("fps"));
                gs.enableVsync = Boolean.parseBoolean(orig.get("vsync"));
                Display.setVSyncEnabled(gs.enableVsync);
                mc.renderGlobal.loadRenderers();
            } catch (Exception ex) {
                System.out.println("[MamadClient] restore settings failed: " + ex);
            }
            orig.clear();
            ModuleManager.save();
        }
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre e) {
        if (!ModuleManager.CULLING.enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || e.entity instanceof EntityPlayer) return;
        if (e.entity.getDistanceSqToEntity(mc.thePlayer) > CULL_DIST_SQ) e.setCanceled(true);
    }
}
