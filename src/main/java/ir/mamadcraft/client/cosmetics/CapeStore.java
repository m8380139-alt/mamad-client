package ir.mamadcraft.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ir.mamadcraft.client.account.SiteAccount;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** کیپ‌های حساب سایت: فهرست، مالکیت، انتخاب (equip) — با همان جدول‌های سایت. */
public final class CapeStore {
    public static final class Cosmetic {
        public String id, name, previewUrl;
        public int price;
        public boolean freeNow;

        /** تکسچر واقعی کیپ کنار عکس پیش‌نمایش است: fire-preview.png -> fire.png */
        public String textureUrl() { return previewUrl == null ? null : previewUrl.replace("-preview.png", ".png"); }
    }

    private interface Action { void run() throws Exception; }

    public static volatile List<Cosmetic> items = new ArrayList<Cosmetic>();
    public static volatile Set<String> owned = new HashSet<String>();
    public static volatile String equippedId;
    public static volatile String status = "";
    public static volatile boolean busy;
    public static volatile boolean loadedOnce;

    private static ExecutorService pool;

    private CapeStore() {}

    private static synchronized ExecutorService pool() {
        if (pool == null) {
            pool = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MamadClient-CapeStore");
                t.setDaemon(true);
                return t;
            });
        }
        return pool;
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static void load() throws Exception {
        JsonObject d = SiteAccount.gql("query { cosmetics(order_by:{sort_order:asc}) { id name category price_irt preview_url free_for_now } "
                + "user_cosmetics { cosmetic_id } equipped_cosmetics { cosmetic_id category } }", new JsonObject());
        List<Cosmetic> list = new ArrayList<Cosmetic>();
        JsonArray cs = d.getAsJsonArray("cosmetics");
        for (JsonElement e : cs) {
            JsonObject o = e.getAsJsonObject();
            String cat = str(o, "category");
            if (cat == null || !cat.equalsIgnoreCase("cape")) continue;
            Cosmetic c = new Cosmetic();
            c.id = str(o, "id");
            c.name = str(o, "name");
            c.previewUrl = str(o, "preview_url");
            c.price = o.has("price_irt") && !o.get("price_irt").isJsonNull() ? o.get("price_irt").getAsInt() : 0;
            c.freeNow = o.has("free_for_now") && !o.get("free_for_now").isJsonNull() && o.get("free_for_now").getAsBoolean();
            list.add(c);
        }
        Set<String> own = new HashSet<String>();
        for (JsonElement e : d.getAsJsonArray("user_cosmetics")) own.add(str(e.getAsJsonObject(), "cosmetic_id"));
        String eq = null;
        for (JsonElement e : d.getAsJsonArray("equipped_cosmetics")) {
            JsonObject o = e.getAsJsonObject();
            String cat = str(o, "category");
            if (cat != null && cat.equalsIgnoreCase("cape")) eq = str(o, "cosmetic_id");
        }
        items = list;
        owned = own;
        equippedId = eq;
        loadedOnce = true;
    }

    private static String friendly(Exception e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("not-signed-in")) return "Session expired. Sign in again.";
        if (m.toLowerCase().contains("free")) return "This cape isn't free right now.";
        if (m.contains("Unable") || m.contains("connect") || m.contains("Connection") || m.contains("timed")) return "Can't reach the server. Check internet / VPN.";
        return "Something went wrong.";
    }

    private static void run(final Action a, final boolean refreshOwnCape) {
        if (busy) return;
        if (!SiteAccount.isLoggedIn()) { status = "Sign in to use capes."; return; }
        busy = true;
        status = "";
        pool().submit(new Runnable() {
            public void run() {
                try {
                    a.run();
                    load();
                    if (refreshOwnCape) {
                        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                            public void run() { CapeManager.invalidate(Minecraft.getMinecraft().getSession().getUsername()); }
                        });
                    }
                } catch (Exception e) {
                    status = friendly(e);
                } finally {
                    busy = false;
                }
            }
        });
    }

    public static void refresh() {
        run(new Action() { public void run() {} }, false);
    }

    public static void equip(final Cosmetic c) {
        run(new Action() {
            public void run() throws Exception {
                JsonObject v = new JsonObject();
                v.addProperty("id", c.id);
                if (!owned.contains(c.id)) {
                    SiteAccount.gql("mutation($id:uuid!){ insert_user_cosmetics_one(object:{cosmetic_id:$id}){ cosmetic_id } }", v);
                }
                SiteAccount.gql("mutation($id:uuid!){ insert_equipped_cosmetics_one(object:{cosmetic_id:$id}, "
                        + "on_conflict:{constraint:equipped_cosmetics_pkey, update_columns:[cosmetic_id]}){ category } }", v);
            }
        }, true);
    }

    public static void unequip() {
        run(new Action() {
            public void run() throws Exception {
                JsonObject v = new JsonObject();
                v.addProperty("c", "cape");
                SiteAccount.gql("mutation($c:String!){ delete_equipped_cosmetics(where:{category:{_eq:$c}}){ affected_rows } }", v);
            }
        }, true);
    }
}
