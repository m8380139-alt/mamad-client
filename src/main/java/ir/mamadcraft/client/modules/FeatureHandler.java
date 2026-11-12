package ir.mamadcraft.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** ماژول‌های رفتاری: Toggle Sprint / Toggle Sneak / Fullbright / No Hurt Cam. */
public class FeatureHandler {
    private boolean sneakToggled, sneakPrev;
    private float oldGamma = -1f;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // اگه با Fullbright روشن از بازی بیرون رفته باشی، gamma توی options.txt روی ۱۰۰ مانده؛ برش می‌گردانیم
        if (mc.thePlayer == null) {
            if (oldGamma >= 0f) { mc.gameSettings.gammaSetting = oldGamma; oldGamma = -1f; }
            else if (!ModuleManager.FULLBRIGHT.enabled && mc.gameSettings.gammaSetting > 1f) mc.gameSettings.gammaSetting = 1f;
            return;
        }

        if (ModuleManager.FULLBRIGHT.enabled) {
            if (oldGamma < 0f) oldGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100f;
        } else if (oldGamma >= 0f) {
            mc.gameSettings.gammaSetting = oldGamma;
            oldGamma = -1f;
        } else if (mc.gameSettings.gammaSetting > 1f) {
            mc.gameSettings.gammaSetting = 1f;
        }

        if (mc.currentScreen != null) return;

        if (ModuleManager.SPRINT.enabled && mc.gameSettings.keyBindForward.isKeyDown()
                && !mc.thePlayer.isSneaking() && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6) {
            mc.thePlayer.setSprinting(true);
        }

        int code = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean down = code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code);
        if (ModuleManager.SNEAK.enabled) {
            if (down && !sneakPrev) sneakToggled = !sneakToggled;
            KeyBinding.setKeyBindState(code, sneakToggled);
        } else if (sneakToggled) {
            sneakToggled = false;
            KeyBinding.setKeyBindState(code, down);
        }
        sneakPrev = down;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !ModuleManager.NOHURT.enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.hurtTime = 0;
    }
}
