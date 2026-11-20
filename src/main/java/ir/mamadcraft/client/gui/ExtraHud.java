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
            key(fr, 26, 100, 20, "W", mc.gameSettings.keyBindForward.isKeyDown());
            key(fr, 4, 122, 20, "A", mc.gameSettings.keyBindLeft.isKeyDown());
            key(fr, 26, 122, 20, "S", mc.gameSettings.keyBindBack.isKeyDown());
            key(fr, 48, 122, 20, "D", mc.gameSettings.keyBindRight.isKeyDown());
            key(fr, 4, 144, 31, "LMB", mc.gameSettings.keyBindAttack.isKeyDown());
            key(fr, 37, 144, 31, "RMB", mc.gameSettings.keyBindUseItem.isKeyDown());
        }

        if (ModuleManager.POTIONS.enabled) {
            int y = 4;
            for (Object o : mc.thePlayer.getActivePotionEffects()) {
                PotionEffect pe = (PotionEffect) o;
                String name = I18n.format(pe.getEffectName()) + (pe.getAmplifier() > 0 ? " " + (pe.getAmplifier() + 1) : "");
                String time = Potion.getDurationString(pe);
                int tc = pe.getDuration() < 200 ? 0xFFFF5555 : 0xFFFFFFFF;   // کمتر از ۱۰ ثانیه: قرمز
                int pw = fr.getStringWidth(name + "  ") + fr.getStringWidth(time) + 14;
                int px = w - pw - 4;
                Ui.hudPanel(px, y, pw, 14, tc == 0xFFFFFFFF ? BrandingHandler.ORANGE : tc);
                fr.drawStringWithShadow(name + "  ", px + 9, y + 3, 0xFFDDDDDD);
                fr.drawStringWithShadow(time, px + 9 + fr.getStringWidth(name + "  "), y + 3, tc);
                y += 17;
            }
        }

        if (ModuleManager.ARMOR.enabled) {
            int y = h / 2 - 50;
            Ui.hudPanel(w - 64, y - 3, 62, 5 * 18 + 4, BrandingHandler.ORANGE);
            for (int i = 3; i >= 0; i--) { item(mc, fr, mc.thePlayer.inventory.armorInventory[i], w, y); y += 18; }
            item(mc, fr, mc.thePlayer.getHeldItem(), w, y);
        }

        if (ModuleManager.REACH.enabled && System.currentTimeMillis() - reachTime < 3000) {
            String s = String.format("%.2f", reachValue);
            Ui.statPill(fr, w / 2 - (fr.getStringWidth("Reach  " + s) + 14) / 2, h / 2 + 14, "Reach", s,
                    BrandingHandler.ORANGE, 0xFFFFFFFF);
        }
    }

    private final java.util.Map<String, Float> pressAnim = new java.util.HashMap<String, Float>();

    private void key(FontRenderer fr, int x, int y, int bw, String label, boolean down) {
        // انیمیشن نرم فشردن/رها کردن کلید
        Float prev = pressAnim.get(label);
        float p = prev == null ? 0f : prev;
        p += ((down ? 1f : 0f) - p) * 0.35f;
        if (Math.abs((down ? 1f : 0f) - p) < 0.01f) p = down ? 1f : 0f;
        pressAnim.put(label, p);

        int rgb = ModuleManager.rgb(x + y * 2);
        int accent = ModuleManager.GAMING.enabled ? (0xFF000000 | (rgb & 0xFFFFFF)) : 0xFF8B5CF6;
        int fill = Ui.lerp(0x90101216, (accent & 0xFFFFFF) | 0xC0000000, p);
        int border = Ui.lerp(0x30FFFFFF, accent, p);
        Ui.roundRectBorder(x, y, bw, 20, 6, fill, border);
        fr.drawStringWithShadow(label, x + bw / 2 - fr.getStringWidth(label) / 2, y + 6, ModuleManager.hud(0xFFFFFFFF, x + y * 2));
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
