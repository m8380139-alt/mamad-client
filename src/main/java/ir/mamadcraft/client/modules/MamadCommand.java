package ir.mamadcraft.client.modules;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

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
        String cat = a[0].toLowerCase(), sub = a[1].toLowerCase();
        if (cat.equals("friend") || cat.equals("enemy")) {
            Set<String> mine = cat.equals("friend") ? ModuleManager.friends : ModuleManager.enemies;
            Set<String> other = cat.equals("friend") ? ModuleManager.enemies : ModuleManager.friends;
            if (sub.equals("list")) { msg(s, cat + "s: " + (mine.isEmpty() ? "-" : mine.toString())); return; }
            if (a.length < 3 || a[2].length() < 3) { msg(s, "name must be at least 3 letters"); return; }
            String n = a[2].toLowerCase();
            if (sub.equals("add")) { mine.add(n); other.remove(n); msg(s, "added " + n + " to " + cat + "s"); }
            else if (sub.equals("remove")) { mine.remove(n); msg(s, "removed " + n); }
            else { help(s); return; }
            ModuleManager.save();
            if (!ModuleManager.FRIENDS.enabled) msg(s, "turn on 'Friend/Enemy Colors' in the menu (Right Shift)");
        } else if (cat.equals("crosshair")) {
            if (a.length < 3) { help(s); return; }
            if (sub.equals("color")) {
                try { ModuleManager.crossColor = Integer.parseInt(a[2].replace("#", ""), 16) & 0xFFFFFF; msg(s, "crosshair color set"); }
                catch (NumberFormatException e) { msg(s, "use a hex color like FF7A00"); return; }
            } else if (sub.equals("shape") && (a[2].equals("cross") || a[2].equals("dot") || a[2].equals("circle"))) {
                ModuleManager.crossShape = a[2]; msg(s, "crosshair shape: " + a[2]);
            } else { help(s); return; }
            ModuleManager.save();
            if (!ModuleManager.CROSSHAIR.enabled) msg(s, "turn on 'Custom Crosshair' in the menu (Right Shift)");
        } else if (cat.equals("chat") && a.length >= 3) {
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
