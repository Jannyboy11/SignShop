package org.wargamer2010.signshop;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.wargamer2010.signshop.blocks.SignShopBooks;
import org.wargamer2010.signshop.blocks.SignShopItemMeta;
import org.wargamer2010.signshop.commands.*;
import org.wargamer2010.signshop.configuration.ColorUtil;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.listeners.*;
import org.wargamer2010.signshop.listeners.sslisteners.*;
import org.wargamer2010.signshop.metrics.SetupMetrics;
import org.wargamer2010.signshop.money.MoneyModifierManager;
import org.wargamer2010.signshop.player.PlayerIdentifier;
import org.wargamer2010.signshop.player.PlayerMetadata;
import org.wargamer2010.signshop.timing.TimeManager;
import org.wargamer2010.signshop.util.WebUtil;
import org.wargamer2010.signshop.util.CommandUtil;
import org.wargamer2010.skript.EvtSSPretransaction;

public class SignShop extends JavaPlugin {

    private static SignShop instance;

    //no longer needed
    //private static final Logger logger = Logger.getLogger("Minecraft");
    private static final Logger transactionlogger = Logger.getLogger("SignShop_Transactions");

    //Listeners
    private final SignShopPlayerListener playerListener = new SignShopPlayerListener();
    private final SignShopBlockListener blockListener = new SignShopBlockListener();
    private final SignShopLoginListener loginListener = new SignShopLoginListener();

    //Statics
    private static Storage store;
    private static TimeManager manager = null;

    //Permissions
    private static boolean USE_PERMISSIONS = false;

    // Vault
    private Vault vault = null;
    private SetupMetrics metricsSetup = null;

    // Skript
    private boolean registeredWithSkript = false;

    // Commands
    private static CommandDispatcher commandDispatcher = new CommandDispatcher();

    //COnstructor
    public SignShop() {
        instance = this;
    }

    //Logging
    public void log(String message, Level level, int tag) {
        if (message != null && !message.trim().isEmpty())
            getLogger().log(level, "[" + tag + "] " + message);
    }

    //TODO un-static this
    public static void log(String message, Level level) {
        if (message != null && !message.trim().isEmpty()) {
            instance.getLogger().log(level, message);
        }
    }

    //TODO un-static this
    public static void logTransaction(String customer, String owner, String Operation, String items, String Price) {
        if (SignShopConfig.getTransactionLog()) {
            String fixedItems = (items.isEmpty() ? "none" : items);
            String message = ("Customer: " + customer + ", Owner: " + owner + ", Operation: " + Operation + ", Items: " + fixedItems + ", Price: " + Price);
            transactionlogger.log(Level.INFO, message);
        }
    }

    private void disableSignShop() {
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.disablePlugin(this);
    }


    public static SignShop getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Shop.class, "Shop");
        ConfigurationSerialization.registerClass(PlayerIdentifier.class, "PlayerId");

        if (!this.getDataFolder().exists()) {
            if (!this.getDataFolder().mkdir())
                log("Could not create plugins/SignShop folder.", Level.SEVERE);
        }

        metricsSetup = new SetupMetrics(this);
        if (!metricsSetup.isOptOut()) {
            if (metricsSetup.setup())
                log("Succesfully started Metrics, see http://mcstats.org for more information.", Level.INFO);
            else
                log("Could not start Metrics, see http://mcstats.org for more information.", Level.INFO);
        }

        setupVault();

        setupCommands();

        SignShopConfig.init();
        SignShopBooks.init();
        PlayerMetadata.init();
        SignShopItemMeta.init();
        WebUtil.init();
        ColorUtil.init();
        MoneyModifierManager.init();

        // TODO is this still needed? it's been over 5 years since UUIDs were introduced (minecraft 1.7.6).
        // Convert legacy player names to UUID
        PlayerMetadata.convertToUuid(this);

        // Create a storage locker for shops
        store = Storage.init(new File(this.getDataFolder(), "sellers.yml"));
        manager = new TimeManager(new File(this.getDataFolder(), "timing.yml"));

        if (SignShopConfig.getTransactionLog()) {
            try {
                FileHandler fh = new FileHandler("plugins/SignShop/Transaction.log", true);
                TransferFormatter formatter = new TransferFormatter();
                fh.setFormatter(formatter);
                fh.setLevel(Level.FINEST);
                transactionlogger.addHandler(fh);
                transactionlogger.setLevel(Level.INFO);
                transactionlogger.setParent(getLogger());
                transactionlogger.setUseParentHandlers(false);
            } catch (IOException ex) {
                log("Failed to create transaction log", Level.INFO);
            }
        }

        PluginDescriptionFile pdfFile = this.getDescription();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(loginListener, this);

        if (Vault.isVaultFound()) {
            // Register events
            pm.registerEvents(playerListener, this);
            pm.registerEvents(blockListener, this);
            pm.registerEvents(new SignShopWorthListener(), this);
            if (SignShopConfig.getDisableEssentialsSigns()) {
                SignShopServerListener SListener = new SignShopServerListener(getServer());
                pm.registerEvents(SListener, this);
            }
            if (!registeredWithSkript && pm.getPlugin("Skript") != null) {
                EvtSSPretransaction.register();
                registeredWithSkript = true;
            }
            registerSSListeners();
            log("v" + pdfFile.getVersion() + " Enabled", Level.INFO);
        } else {
            disableSignShop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String args[]) {
        String commandName = cmd.getName().toLowerCase();
        if (!commandName.equalsIgnoreCase("signshop"))
            return true;
        return CommandUtil.handleCommand(sender, cmd, commandLabel, args, commandDispatcher);
    }

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static String getLogPrefix() {
        PluginDescriptionFile pdfFile = SignShop.getInstance().getDescription();
        String prefix = ChatColor.GOLD + "[SignShop] [" + pdfFile.getVersion() + "]" + ChatColor.RED;
        return prefix;
    }

    private void closeHandlers() {
        Handler[] handlers;
        if (transactionlogger == null || (handlers = transactionlogger.getHandlers()) == null) return;

        for (Handler handler : handlers) {
            handler.close();
        }
    }

    @Override
    public void onDisable() {
        closeHandlers();
        if (store != null) {
            store.save();
            store.dispose();
        }
        if (manager != null)
            manager.stop();
        log("Disabled", Level.INFO);
    }

    private void setupVault() {
        vault = new Vault();
        //vault.setupChat(); chat not necessary!

        boolean vaultPermissions = vault.setupPermissions();
        //TODO SuperPerms is bukkit's native permission system - which is fine.
        //TODO find out whether vault uses that as a default or not.
        //TODO we want to use bukkit's permission system anyway so.. i'm not sure why we're checking Vault permissions
        //TODO The economy functionality from Vault is useful though :)
        if (!vaultPermissions || vault.getPermission().getName().equals("SuperPerms")) {
            log("Vault's permissions not found, defaulting to OP.", Level.INFO);
            USE_PERMISSIONS = false;
        } else
            USE_PERMISSIONS = true;
        boolean vault_Economy = vault.setupEconomy();
        if (!vault_Economy)
            log("Could not hook into Vault's Economy!", Level.WARNING);
    }

    private void setupCommands() {
        commandDispatcher.registerHandler("stats", StatsHandler.getInstance());
        commandDispatcher.registerHandler("version", StatsHandler.getInstance());
        commandDispatcher.registerHandler("about", StatsHandler.getInstance());
        commandDispatcher.registerHandler("reload", ReloadHandler.getInstance());
        commandDispatcher.registerHandler("tutorial", TutorialHandler.getInstance());
        commandDispatcher.registerHandler("help", HelpHandler.getInstance());
        commandDispatcher.registerHandler("sign", HelpHandler.getInstance());
        commandDispatcher.registerHandler("list", HelpHandler.getInstance());
        commandDispatcher.registerHandler("unlink", UnlinkHandler.getInstance());
        commandDispatcher.registerHandler("", HelpHandler.getInstance());
    }

    private void registerSSListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SimpleBlacklister(), this);
        pm.registerEvents(new SimpleMessenger(), this);
        pm.registerEvents(new SimpleRestricter(), this);
        pm.registerEvents(new SimpleShopLimiter(), this);
        pm.registerEvents(new SimpleShopProtector(), this);
        pm.registerEvents(new SimpleBlockProtector(), this);
        pm.registerEvents(new PermissionChecker(), this);
        pm.registerEvents(new PermitChecker(), this);
        pm.registerEvents(new ShopUpdater(), this);
        pm.registerEvents(new GetPriceFromWorth(), this);
        pm.registerEvents(new ShopCooldown(), this);
        pm.registerEvents(new NotificationsHooker(), this);
        pm.registerEvents(new StockChecker(), this);
        pm.registerEvents(new TimedCommandListener(), this);
        pm.registerEvents(new MoneyModifierListener(), this);

        DynmapManager dmm = new DynmapManager();
        if (SignShopConfig.getEnableDynmapSupport())
            pm.registerEvents(dmm, this);
        if (SignShopConfig.getEnableShopPlotSupport()) {
            //pm.registerEvents(new WorldGuardChecker(), this);
            pm.registerEvents(new TownyChecker(), this);
        }

        // Money Transactions Types
        pm.registerEvents(new DefaultMoneyTransaction(), this);
        pm.registerEvents(new BankTransaction(), this);
        pm.registerEvents(new SharedMoneyTransaction(), this);
    }

    public static boolean usePermissions() {
        return USE_PERMISSIONS;
    }

    public static TimeManager getTimeManager() {
        return manager;
    }

    public static CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }

    private class TransferFormatter extends Formatter {
        private final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder(1000);
            builder.append(df.format(new Date(record.getMillis()))).append(" - ");
            builder.append("[").append(record.getLevel()).append("] - ");
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }

        @Override
        public String getHead(Handler h) {
            return super.getHead(h);
        }

        @Override
        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }
}
