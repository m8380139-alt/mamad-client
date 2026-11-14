package ir.mamadcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** لوگو و نام کلاینت: صفحهٔ اصلی ماینکرفت + واترمارک داخل بازی. */
public class BrandingHandler {
    private static final ResourceLocation LOGO = new ResourceLocation("mamadclient", "textures/logo.png");
    public static final int ORANGE = 0xFFFF7A00;

    private static void drawLogo(int x, int y, int size) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    @SubscribeEvent
    public void onMainMenu(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!(e.gui instanceof GuiMainMenu)) return;
        Minecraft mc = Minecraft.getMinecraft();
        int size = 56;
        int x = 8, y = 8;
        drawLogo(x, y, size);
        mc.fontRendererObj.drawStringWithShadow("Mamad", x + size + 6, y + 10, 0xFFFFFFFF);
        mc.fontRendererObj.drawStringWithShadow("Client", x + size + 6, y + 22, ORANGE);
        mc.fontRendererObj.drawStringWithShadow("1.8.9", x + size + 6, y + 36, 0xFFAAAAAA);
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || !ir.mamadcraft.client.modules.ModuleManager.WATERMARK.enabled) return;
        drawLogo(4, 4, 14);
        mc.fontRendererObj.drawStringWithShadow("Mamad Client", 22, 8, ORANGE);
    }
}
