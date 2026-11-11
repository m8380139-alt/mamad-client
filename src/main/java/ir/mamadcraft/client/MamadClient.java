package ir.mamadcraft.client;

import ir.mamadcraft.client.cosmetics.CapeManager;
import ir.mamadcraft.client.gui.BrandingHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Mamad Client — کلاینت ممدکرفت (Forge 1.8.9).
 * قدم اول: نمایش کیپ بازیکن‌ها از سایت ممدکرفت.
 */
@Mod(modid = MamadClient.MODID, useMetadata = true)
public class MamadClient {
    public static final String MODID = "mamadclient";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CapeManager.init();
        MinecraftForge.EVENT_BUS.register(new BrandingHandler());
        System.out.println("[MamadClient] loaded");
    }
}
