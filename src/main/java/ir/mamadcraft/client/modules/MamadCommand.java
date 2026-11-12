package ir.mamadcraft.client.modules;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Locale;
import java.util.Set;

/**
 * /mamad friend|enemy add|remove|list [name]
 * /mamad crosshair color RRGGBB | shape cross|dot|circle
 * /mamad chat hidesystem|spam on|off
 */
public class MamadCommand extends CommandBase {
    @Override public String getCommandName() { return "mamad"; }
    @Override public String getCommandUsage(ICommandSender s) { return "/mamad <friend|enemy|crosshair|chat> ..."; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }

    private static void msg(ICommandSender s, String t) {
        s.addChatMessage(new ChatComponentText("§6[Mamad] §f" + t));
    }

    @Override
    public void processCommand(ICommandSender s, String[] a) throws CommandException {
        if (a.length < 2) { help(s); return; }

        String cat = a[0].toLowerCase(Locale.ROOT);
        String sub = a[1].toLowerCase(Locale.ROOT);

        if (cat.equals("friend") || cat.equals("enemy")) {
            Set<String> mine = cat.equals("friend") ? ModuleManager.friends : ModuleManager.enemies;
            Set<String> other = cat.equals("friend") ? ModuleManager.enemies : ModuleManager.friends;
            if (sub.equals("list")) {
                String label = cat.equals("friend") ? "friends" : "enemies";
                msg(s, label + ": " + (mine.isEmpty() ? "-" : mine.toString()));
                return;
            }
            if (a.length < 3 || !a[2].matches("[A-Za-z0-9_]{3,16}")) {
                msg(s, "name must be a valid Minecraft username (3-16 letters, numbers or underscores)");
                return;
            }

            String n = a[2].toLowerCase(Locale.ROOT);
            if (sub.equals("add")) {
                mine.add(n);
                other.remove(n);
                msg(s, "added " + n + " to " + (cat.equals("friend") ? "friends" : "enemies"));
            } else if (sub.equals("remove")) {
                mine.remove(n);
                msg(s, "removed " + n);
            } else {
                help(s);
                return;
            }
            ModuleManager.save();
            if (!ModuleManager.FRIENDS.enabled) msg(s, "turn on 'Friend/Enemy Colors' in the menu (Right Shift)");

        } else if (cat.equals("crosshair")) {
            if (a.length < 3) { help(s); return; }

            if (sub.equals("color")) {
                String hex = a[2].startsWith("#") ? a[2].substring(1) : a[2];
                if (!hex.matches("(?i)[0-9a-f]{6}")) {
                    msg(s, "use a 6-digit hex color like FF7A00");
                    return;
                }
                ModuleManager.crossColor = Integer.parseInt(hex, 16);
                msg(s, "crosshair color set");

            } else if (sub.equals("shape")) {
                String shape = a[2].toLowerCase(Locale.ROOT);
                if (!shape.equals("cross") && !shape.equals("dot") && !shape.equals("circle")) {
                    help(s);
                    return;
                }
                ModuleManager.crossShape = shape;
                msg(s, "crosshair shape: " + shape);

            } else {
                help(s);
                return;
            }
            ModuleManager.save();
            if (!ModuleManager.CROSSHAIR.enabled) msg(s, "turn on 'Custom Crosshair' in the menu (Right Shift)");

        } else if (cat.equals("chat") && a.length >= 3) {
            if (!a[2].equalsIgnoreCase("on") && !a[2].equalsIgnoreCase("off")) {
                msg(s, "use on or off");
                return;
            }
            boolean on = a[2].equalsIgnoreCase("on");
            if (sub.equals("hidesystem")) ModuleManager.hideSystem = on;
            else if (sub.equals("spam")) ModuleManager.filterSpam = on;
            else { help(s); return; }

            msg(s, "chat " + sub + ": " + (on ? "on" : "off"));
            ModuleManager.save();
            if (!ModuleManager.CHAT.enabled) msg(s, "turn on 'Chat Tweaks' in the menu (Right Shift)");
        } else {
            help(s);
        }
    }

    private static void help(ICommandSender s) {
        msg(s, "/mamad friend|enemy add|remove|list <name>");
        msg(s, "/mamad crosshair color <RRGGBB> | shape cross|dot|circle");
        msg(s, "/mamad chat hidesystem|spam on|off");
    }
}
