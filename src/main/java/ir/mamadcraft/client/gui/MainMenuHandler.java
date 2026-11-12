package ir.mamadcraft.client.gui;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** منوی اصلی ماینکرفت رو با منوی Mamad Client عوض می‌کنه. */
public class MainMenuHandler {
    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class) e.gui = new MamadMainMenu();
    }
}
