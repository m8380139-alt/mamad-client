package ir.mamadcraft.client.gui;

import ir.mamadcraft.client.account.SiteAccount;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** پنجرهٔ ورود به حساب سایت MamadCraft (نام کاربری همان نام بازی است). */
public class LoginScreen extends GuiScreen {
    private static final class Field {
        String text = "";
        final boolean mask;
        final int max;
        Field(boolean mask, int max) { this.mask = mask; this.max = max; }
    }

    private final GuiScreen parent;
    private final Field user = new Field(false, 20);
    private final Field pass = new Field(true, 64);
    private Field focus = pass;
    private boolean showPass;
    private boolean busy;
    private String msg = "";
    private int msgColor = 0xFFAAAAAA;
    private long closeAt;

    private int px, py, pw = 224, ph;
    // ردیف‌ها: 0 = Login، 1 = Sign out (فقط وقتی وارد شده)، آخرین = Cancel
    private int btnCount;

    public LoginScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        parent.setWorldAndResolution(mc, width, height);
        btnCount = SiteAccount.isLoggedIn() ? 3 : 2;
        ph = 152 + btnCount * 26;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        if (user.text.isEmpty()) {
            user.text = SiteAccount.isLoggedIn() ? SiteAccount.getUsername() : mc.getSession().getUsername();
            if (user.text.length() > 20) user.text = user.text.substring(0, 20);
        }
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    private int fieldY(int i) { return py + 48 + i * 40; }
    private int btnY(int i) { return py + 152 + i * 26 - 4; }

    private void drawField(Field f, int x, int y, int w, boolean focused) {
        if (focused) Ui.roundRectBorder(x, y, w, 20, 7, Ui.FIELD, Ui.ACCENT);
        else Ui.roundRect(x, y, w, 20, 7, Ui.FIELD);
        String shown = f.text;
        if (f.mask && !showPass) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < f.text.length(); i++) sb.append('*');
            shown = sb.toString();
        }
        shown = fontRendererObj.trimStringToWidth(shown, w - 14, true);
        fontRendererObj.drawString(shown, x + 7, y + 6, 0xFFFFFFFF);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = x + 7 + fontRendererObj.getStringWidth(shown);
            Gui.drawRect(cx, y + 5, cx + 1, y + 15, 0xFFFFFFFF);
        }
    }

    private void drawButton(String label, int x, int y, int w, int base, int hover, int mx, int my, int textColor) {
        boolean h = Ui.inside(mx, my, x, y, w, 20) && !busy;
        Ui.roundRect(x, y, w, 20, 10, h ? hover : base);
        fontRendererObj.drawString(label, x + w / 2 - fontRendererObj.getStringWidth(label) / 2, y + 6, textColor);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        parent.drawScreen(-1, -1, pt);
        drawRect(0, 0, width, height, 0xA0000000);
        GlStateManager.enableBlend();

        if (closeAt > 0 && System.currentTimeMillis() >= closeAt) {
            mc.displayGuiScreen(parent);
            return;
        }

        Ui.roundRect(px, py, pw, ph, 14, Ui.PANEL);
        fontRendererObj.drawString("Mamad Account", px + 16, py + 16, 0xFFFFFFFF);
        // دکمهٔ بستن
        boolean xh = Ui.inside(mx, my, px + pw - 26, py + 10, 16, 16);
        Ui.roundRect(px + pw - 26, py + 10, 16, 16, 8, xh ? 0xFF3A3A40 : 0xFF2A2A2E);
        fontRendererObj.drawString("x", px + pw - 21, py + 14, 0xFFDDDDDD);

        fontRendererObj.drawString("Username", px + 16, fieldY(0) - 12, 0xFFDDDDDD);
        drawField(user, px + 16, fieldY(0), pw - 32, focus == user);
        fontRendererObj.drawString("Password", px + 16, fieldY(1) - 12, 0xFFDDDDDD);
        drawField(pass, px + 16, fieldY(1), pw - 32, focus == pass);

        // Show password
        int cy = fieldY(1) + 28;
        Ui.roundRect(px + 16, cy, 10, 10, 3, showPass ? Ui.ACCENT : Ui.FIELD);
        fontRendererObj.drawString("Show password", px + 32, cy + 1, 0xFFCCCCCC);

        if (!msg.isEmpty()) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(msg, pw - 32), px + 16, cy + 14, msgColor);
        }

        int bx = px + 16, bw = pw - 32;
        drawButton(busy ? "Signing in..." : "Login", bx, btnY(0), bw, Ui.ACCENT, Ui.ACCENT_HOVER, mx, my, 0xFFFFFFFF);
        int i = 1;
        if (SiteAccount.isLoggedIn()) {
            drawButton("Sign out", bx, btnY(i++), bw, Ui.BTN, Ui.BTN_HOVER, mx, my, 0xFFFFFFFF);
        }
        drawButton("Cancel", bx, btnY(i), bw, Ui.DANGER, Ui.DANGER_HOVER, mx, my, 0xFFFF8A8A);
    }

    private void setMsg(String m, boolean error) {
        msg = m;
        msgColor = error ? 0xFFFF6B6B : 0xFF6BE675;
    }

    private void doLogin() {
        if (busy) return;
        final String u = user.text.trim();
        if (!SiteAccount.USERNAME_RE.matcher(u).matches()) { setMsg("Username: 3-20 letters, digits or _", true); return; }
        if (pass.text.isEmpty()) { setMsg("Enter your password.", true); return; }
        busy = true;
        msg = "";
        SiteAccount.signIn(u, pass.text, new SiteAccount.Callback() {
            public void done(boolean ok, String message) {
                busy = false;
                setMsg(message, !ok);
                if (ok) { pass.text = ""; closeAt = System.currentTimeMillis() + 800; }
            }
        });
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0 || busy) return;
        if (Ui.inside(mx, my, px + pw - 26, py + 10, 16, 16)) { mc.displayGuiScreen(parent); return; }
        if (Ui.inside(mx, my, px + 16, fieldY(0), pw - 32, 20)) { focus = user; return; }
        if (Ui.inside(mx, my, px + 16, fieldY(1), pw - 32, 20)) { focus = pass; return; }
        int cy = fieldY(1) + 28;
        if (Ui.inside(mx, my, px + 16, cy - 2, 110, 14)) { showPass = !showPass; return; }
        int bx = px + 16, bw = pw - 32;
        if (Ui.inside(mx, my, bx, btnY(0), bw, 20)) { doLogin(); return; }
        int i = 1;
        if (SiteAccount.isLoggedIn()) {
            if (Ui.inside(mx, my, bx, btnY(i++), bw, 20)) {
                SiteAccount.signOut();
                mc.displayGuiScreen(parent);
                return;
            }
        }
        if (Ui.inside(mx, my, bx, btnY(i), bw, 20)) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (busy) return;
        if (key == Keyboard.KEY_TAB) { focus = focus == user ? pass : user; return; }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { doLogin(); return; }
        if (key == Keyboard.KEY_BACK) {
            if (!focus.text.isEmpty()) focus.text = focus.text.substring(0, focus.text.length() - 1);
            return;
        }
        if (GuiScreen.isKeyComboCtrlV(key)) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null) {
                StringBuilder sb = new StringBuilder(focus.text);
                for (int i = 0; i < clip.length() && sb.length() < focus.max; i++) {
                    if (ChatAllowedCharacters.isAllowedCharacter(clip.charAt(i))) sb.append(clip.charAt(i));
                }
                focus.text = sb.toString();
            }
            return;
        }
        if (ChatAllowedCharacters.isAllowedCharacter(c) && focus.text.length() < focus.max) focus.text += c;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
