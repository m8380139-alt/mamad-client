package ir.mamadcraft.client.modules;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;
import java.util.regex.Matcher;
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
            // پیام‌های دارای کلیک/هاور (لینک، دکمه‌ی سرور) را دست نمی‌زنیم تا خراب نشوند
            if (!out.equals(s) && !hasEvents(e.message)) e.message = new ChatComponentText(out);
        }
    }

    private static boolean hasEvents(IChatComponent c) {
        if (c.getChatStyle().getChatClickEvent() != null || c.getChatStyle().getChatHoverEvent() != null) return true;
        for (Object o : c.getSiblings()) {
            if (hasEvents((IChatComponent) o)) return true;
        }
        return false;
    }

    private static String tint(String s, Set<String> names, String code) {
        for (String n : names) {
            Matcher m = Pattern.compile("(?i)\\b(" + Pattern.quote(n) + ")\\b").matcher(s);
            StringBuilder out = new StringBuilder();
            int last = 0;
            while (m.find()) {
                out.append(s, last, m.start());
                // بعد از اسم، رنگ/استایل قبلی پیام برمی‌گردد (نه سفید)
                out.append(code).append(m.group(1)).append("§r").append(activeFormat(s, m.start()));
                last = m.end();
            }
            out.append(s, last, s.length());
            s = out.toString();
        }
        return s;
    }

    /** رنگ و استایل فعال در متن تا قبل از موقعیت end. */
    private static String activeFormat(String s, int end) {
        String color = "";
        StringBuilder styles = new StringBuilder();
        for (int i = 0; i + 1 < end; i++) {
            if (s.charAt(i) != '§') continue;
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) { color = "§" + c; styles.setLength(0); }
            else if (c == 'r') { color = ""; styles.setLength(0); }
            else if (c >= 'k' && c <= 'o') styles.append('§').append(c);
            i++;
        }
        return color + styles;
    }
}
