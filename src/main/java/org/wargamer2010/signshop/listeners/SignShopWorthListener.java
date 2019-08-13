package org.wargamer2010.signshop.listeners;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.wargamer2010.signshop.SignShop;

public class SignShopWorthListener implements Listener {
    private static final String IEssentialsClass = "com.earth2me.essentials.IEssentials";
    private static final String WorthClass = "com.earth2me.essentials.Worth";

    private static final String essName = "Essentials"; //EssentialsX's name is still Essentials!
    private static Plugin plEssentials = null;
    private static Object wWorth = null;

    //TODO why does this eagerly load Essentials classes? Specifically: IEssentials
    public SignShopWorthListener() {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled(essName)) {
            itEnabled();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnabled(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals(essName))
            itEnabled();
    }

    private final void itEnabled() {
        plEssentials = Bukkit.getServer().getPluginManager().getPlugin(essName);
        if (plEssentials == null)
            return;

        if (wWorth != null)
            try {
                wWorth.getClass().getMethod("reloadConfig").invoke(wWorth);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            }
        else
            loadWorth();
    }

    public static double getPrice(ItemStack stack) {
        try {
            Class<?> iEssentialsClass = Class.forName(IEssentialsClass);
            Method m = wWorth.getClass().getMethod("getPrice", iEssentialsClass, ItemStack.class);
            Object result = m.invoke(wWorth, plEssentials, stack);
            return (Double) result;
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return -1D;
    }

    public static boolean essLoaded() {
        return (plEssentials != null);
    }

    private void loadWorth() {
        if (plEssentials == null)
            return;
        File worthfile = new File(plEssentials.getDataFolder(), "worth.yml");
        if (!worthfile.exists()) {
            SignShop.log("Essentials was found but no worth.yml was found in it's plugin folder.", Level.WARNING);
            return;
        }

        try {
            Constructor<?> ctr = Class.forName(WorthClass).getConstructor(File.class);
            wWorth = ctr.newInstance(plEssentials.getDataFolder());
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
