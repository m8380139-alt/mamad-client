package ir.mamadcraft.client.modules;

import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;
import java.util.regex.Pattern;

/** Chat Tweaks (حذف اسپم و پیام‌های سیستمی) + رنگی کردن اسم دوست‌ها (سبز) و دشمن‌ها (قرمز). */
public class ChatHandler {
    private String lastText = "";
    private long lastTime;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.message == null) return;
        if (ModuleManager.CHAT.enabled) {
            if (ModuleManager.hideSystem && e.type == 1) { e.setCanceled(true); return; }
            if (ModuleManager.filterSpam && e.type == 0) {
                String t = e.message.getUnformattedText();
                long now = System.currentTimeMillis();
                if (t.equals(lastText) && now - lastTime < 5000) { e.setCanceled(true); return; }
                lastText = t;
                lastTime = now;
            }
        }
        if (ModuleManager.FRIENDS.enabled && e.type == 0) {
            String s = e.message.getFormattedText();
            String out = tint(tint(s, ModuleManager.friends, "§a"), ModuleManager.enemies, "§c");
            if (!out.equals(s)) e.message = new ChatComponentText(out);
        }
    }

    private static String tint(String s, Set<String> names, String code) {
        for (String n : names) {
            s = s.replaceAll("(?i)\\b(" + Pattern.quote(n) + ")\\b", code + "$1§r");
        }
        return s;
    }
}
