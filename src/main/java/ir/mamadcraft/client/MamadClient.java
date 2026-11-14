package ir.mamadcraft.client;

import ir.mamadcraft.client.cosmetics.CapeManager;
import ir.mamadcraft.client.gui.BrandingHandler;
import ir.mamadcraft.client.gui.ExtraHud;
import ir.mamadcraft.client.gui.MainMenuHandler;
import ir.mamadcraft.client.gui.HudHandler;
import ir.mamadcraft.client.gui.CrosshairHandler;
import ir.mamadcraft.client.modules.ChatHandler;
import ir.mamadcraft.client.modules.FeatureHandler;
import ir.mamadcraft.client.modules.MamadCommand;
import net.minecraftforge.client.ClientCommandHandler;
import ir.mamadcraft.client.gui.MamadMenu;
import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
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
    private static KeyBinding menuKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CapeManager.init();
        MinecraftForge.EVENT_BUS.register(new BrandingHandler());
        MinecraftForge.EVENT_BUS.register(new HudHandler());
        MinecraftForge.EVENT_BUS.register(new ExtraHud());
        MinecraftForge.EVENT_BUS.register(new FeatureHandler());
        MinecraftForge.EVENT_BUS.register(new CrosshairHandler());
        MinecraftForge.EVENT_BUS.register(new MainMenuHandler());
        MinecraftForge.EVENT_BUS.register(new ChatHandler());
        MinecraftForge.EVENT_BUS.register(new ir.mamadcraft.client.modules.ToolsHandler());
        ir.mamadcraft.client.modules.ToolsHandler.zoomKey = new KeyBinding("Zoom", Keyboard.KEY_C, "Mamad Client");
        ClientRegistry.registerKeyBinding(ir.mamadcraft.client.modules.ToolsHandler.zoomKey);
        ClientCommandHandler.instance.registerCommand(new MamadCommand());
        MinecraftForge.EVENT_BUS.register(this);
        ModuleManager.load();
        menuKey = new KeyBinding("Open Mamad Menu", Keyboard.KEY_RSHIFT, "Mamad Client");
        ClientRegistry.registerKeyBinding(menuKey);
        System.out.println("[MamadClient] loaded");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (menuKey.isPressed() && mc.currentScreen == null) mc.displayGuiScreen(new MamadMenu());
    }
}
