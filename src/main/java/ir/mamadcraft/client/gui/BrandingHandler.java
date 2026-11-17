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
        Ui.rainbowText(mc.fontRendererObj, "Client", x + size + 6, y + 22, 0f);
        mc.fontRendererObj.drawStringWithShadow("1.8.9", x + size + 6, y + 36, 0xFFAAAAAA);
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || !ir.mamadcraft.client.modules.ModuleManager.WATERMARK.enabled) return;
        String t = "Mamad Client";
        Ui.roundRect(4, 4, mc.fontRendererObj.getStringWidth(t) + 28, 16, 8, 0x9A101216);
        drawLogo(7, 6, 12);
        Ui.rainbowText(mc.fontRendererObj, t, 22, 8, 0f);
    }
}
