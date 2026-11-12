package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Keystrokes / Armor Status / Potion Effects / Reach Display. */
public class ExtraHud {
    private long reachTime;
    private double reachValue;

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button != 0 || !e.buttonstate) return;
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mc.thePlayer != null && mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mop.hitVec != null) {
            reachValue = mc.thePlayer.getPositionEyes(1.0F).distanceTo(mop.hitVec);
            reachTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.thePlayer == null) return;
        int w = e.resolution.getScaledWidth(), h = e.resolution.getScaledHeight();
        FontRenderer fr = mc.fontRendererObj;

        if (ModuleManager.KEYSTROKES.enabled) {
            key(fr, 26, 84, 20, "W", mc.gameSettings.keyBindForward.isKeyDown());
            key(fr, 4, 106, 20, "A", mc.gameSettings.keyBindLeft.isKeyDown());
            key(fr, 26, 106, 20, "S", mc.gameSettings.keyBindBack.isKeyDown());
            key(fr, 48, 106, 20, "D", mc.gameSettings.keyBindRight.isKeyDown());
            key(fr, 4, 128, 31, "LMB", mc.gameSettings.keyBindAttack.isKeyDown());
            key(fr, 37, 128, 31, "RMB", mc.gameSettings.keyBindUseItem.isKeyDown());
        }

        if (ModuleManager.POTIONS.enabled) {
            int y = 4;
            for (Object o : mc.thePlayer.getActivePotionEffects()) {
                PotionEffect pe = (PotionEffect) o;
                String s = I18n.format(pe.getEffectName()) + (pe.getAmplifier() > 0 ? " " + (pe.getAmplifier() + 1) : "")
                        + " " + Potion.getDurationString(pe);
                fr.drawStringWithShadow(s, w - fr.getStringWidth(s) - 4, y, 0xFFFFFFFF);
                y += 11;
            }
        }

        if (ModuleManager.ARMOR.enabled) {
            int y = h / 2 - 50;
            for (int i = 3; i >= 0; i--) { item(mc, fr, mc.thePlayer.inventory.armorInventory[i], w, y); y += 18; }
            item(mc, fr, mc.thePlayer.getHeldItem(), w, y);
        }

        if (ModuleManager.REACH.enabled && System.currentTimeMillis() - reachTime < 3000) {
            String s = String.format("Reach: %.2f", reachValue);
            fr.drawStringWithShadow(s, w / 2 - fr.getStringWidth(s) / 2, h / 2 + 14, BrandingHandler.ORANGE);
        }
    }

    private void key(FontRenderer fr, int x, int y, int bw, String label, boolean down) {
        Gui.drawRect(x, y, x + bw, y + 20, down ? 0xB0FF7A00 : 0x80000000);
        fr.drawStringWithShadow(label, x + bw / 2 - fr.getStringWidth(label) / 2, y + 6, 0xFFFFFFFF);
    }

    private void item(Minecraft mc, FontRenderer fr, ItemStack st, int w, int y) {
        if (st == null) return;
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(st, w - 20, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.disableLighting();
        String s = null;
        int color = 0xFFFFFFFF;
        if (st.isItemStackDamageable()) {
            int max = st.getMaxDamage();
            int pct = (max - st.getItemDamage()) * 100 / Math.max(1, max);
            s = pct + "%";
            color = pct > 50 ? 0xFF55FF55 : pct > 25 ? 0xFFFFFF55 : 0xFFFF5555;
        } else if (st.stackSize > 1) {
            s = "x" + st.stackSize;
        }
        if (s != null) fr.drawStringWithShadow(s, w - 24 - fr.getStringWidth(s), y + 5, color);
    }
}
