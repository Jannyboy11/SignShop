package org.wargamer2010.signshop.configuration;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Level;
import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.LinkedBlockingQueue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.player.PlayerIdentifier;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;

public class Storage implements Listener, Runnable {
    private File ymlfile;
    private LinkedBlockingQueue<FileConfiguration> saveQueue = new LinkedBlockingQueue<>();

    private static Storage instance = null;
    private int taskId = 0;

    private final Map<Location, Shop> shops = new HashMap<>();

    private static String itemSeperator = "&";

    private Map<String, Shop> invalidShops = new LinkedHashMap<>();

    private Storage(File ymlFile) {
        if(!ymlFile.exists()) {
            try {
                ymlFile.createNewFile();
            } catch(IOException ex) {
                SignShop.log("Could not create sellers.yml", Level.WARNING);
            }
        }
        ymlfile = ymlFile;

        // Load into memory, this also removes invalid signs (hence the backup)
        boolean needToSave = load();
        if (needToSave) {
            File backupTo = new File(ymlFile.getPath()+".bak");
            if (backupTo.exists())
                backupTo.delete();
            try {
                copyFile(ymlFile, backupTo);
            } catch (IOException ex) {
                SignShop.log(SignShopConfig.getError("backup_fail", null), Level.WARNING);
            }

            save();
        }
    }

    public static Storage init(File ymlFile) {
        if (instance == null) {
            instance = new Storage(ymlFile);
            instance.taskId = Bukkit.getScheduler().runTaskAsynchronously(SignShop.getInstance(), instance).getTaskId();
        }
        return instance;
    }

    public void dispose() {
        instance = null;
        if(taskId != 0) {
            try {
                Bukkit.getScheduler().cancelTask(taskId);
                SignShop.log("Successfully cancelled Async Storage task with ID: " + taskId, Level.INFO);
            } catch (Exception ex) {
                SignShop.log("Failed to cancel Storage task because: " + ex.getMessage(), Level.WARNING);
            }
        }
        taskId = 0;
    }

    public static Storage get() {
        return instance;
    }

    public int shopCount() {
        return shops.size();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (invalidShops.isEmpty())
            return;

        String worldName = event.getWorld().getName();
        List<String> loaded = new LinkedList<>();
        SignShop.log("Loading shops for world: " + worldName, Level.INFO);
        for (Map.Entry<String, Shop> invalidShop : invalidShops.entrySet()) {
            String shopLocationString = invalidShop.getKey();
            Shop shop = invalidShop.getValue();

            if (worldName.equalsIgnoreCase(shop.getWorld())) {
                loaded.add(shopLocationString);
            }
        }

        for (String locationString : loaded) {
            Location shopLocation = SignShopUtil.convertStringToLocation(locationString, event.getWorld());
            Shop shop = invalidShops.remove(shopLocation);
            this.shops.put(shopLocation, shop);
        }

        this.save();
    }

    private static List<String> getSetting(HashMap<String,List<String>> settings, String settingName) throws StorageException {
        List<String> setting = settings.get(settingName);
        if (setting == null) {
            throw new StorageException();
        } else {
            return setting;
        }
    }

//    @Deprecated
//    private boolean loadSellerFromSettings(String key, HashMap<String,List<String>> sellerSettings) {
//        Block seller_sign;
//        SignShopPlayer seller_owner;
//        List<Block> seller_activatables;
//        List<Block> seller_containables;
//        String seller_shopworld;
//        ItemStack[] seller_items;
//        Map<String, String> miscsettings;
//        StorageException storageex = new StorageException();
//
//        List<String> tempList;
//        try {
//            tempList = getSetting(sellerSettings, "shopworld");
//            if (tempList.isEmpty())
//                throw storageex;
//            seller_shopworld = tempList.get(0);
//            storageex.setWorld(seller_shopworld);
//            if (Bukkit.getServer().getWorld(seller_shopworld) == null)
//                throw storageex;
//            tempList = getSetting(sellerSettings, "owner");
//            if (tempList.isEmpty())
//                throw storageex;
//            seller_owner = PlayerIdentifier.getPlayerFromString(tempList.get(0)); //tempList.get(0) is the player's uuid or username
//            if (seller_owner == null)
//                throw storageex;
//            tempList = getSetting(sellerSettings, "sign");
//            if (tempList.isEmpty())
//                throw storageex;
//
//            World world = Bukkit.getServer().getWorld(seller_shopworld);
//
//            try {
//                seller_sign = SignShopUtil.convertStringToLocation(tempList.get(0), world).getBlock();
//            } catch(Exception ex) {
//                SignShop.log("Caught an unexpected exception: " + ex.getMessage(), Level.WARNING);
//                // May have caught a FileNotFoundException originating from the chunkloader
//                // In any case, the shop can not be loaded at this point so let's assume it's invalid
//                throw storageex;
//            }
//
//            if (!ItemUtil.isSign(seller_sign))
//                throw storageex;
//            seller_activatables = SignShopUtil.getBlocksFromLocStringList(getSetting(sellerSettings, "activatables"), world);
//            seller_containables = SignShopUtil.getBlocksFromLocStringList(getSetting(sellerSettings, "containables"), world);
//            seller_items = ItemUtil.convertStringtoItemStacks(getSetting(sellerSettings, "items"));
//            miscsettings = new HashMap<>();
//            if (sellerSettings.containsKey("misc")) {
//                for (String miscsetting : sellerSettings.get("misc")) {
//                    String[] miscbits = miscsetting.split(":", 2);
//                    if (miscbits.length == 2)
//                        miscsettings.put(miscbits[0].trim(), miscbits[1].trim());
//                }
//            }
//        } catch (StorageException caughtex) {
//            if (!caughtex.hasWorld()) {
//                for (World temp : Bukkit.getWorlds()) {
//                    if (temp.getName().equalsIgnoreCase(caughtex.getWorld()) && temp.getLoadedChunks().length == 0) {
//                        invalidShops.put(key, sellerSettings);
//                        return true; // World might not be loaded yet
//                    }
//                }
//            }
//
//            try {
//                SignShop.log(getInvalidError(
//                        SignShopConfig.getError("shop_removed", null), getSetting(sellerSettings, "sign").get(0), getSetting(sellerSettings, "shopworld").get(0)), Level.INFO);
//            } catch (StorageException lastex) {
//                SignShop.log(SignShopConfig.getError("shop_removed", null), Level.INFO);
//            }
//            invalidShops.put(key, sellerSettings);
//            return false;
//        }
//
//        if (SignShopConfig.ExceedsMaxChestsPerShop(seller_containables.size())) {
//            Map<String, String> parts = new LinkedHashMap<String>();
//            int x = seller_sign.getX(), y = seller_sign.getY(), z = seller_sign.getZ();
//            parts.put("!world", seller_shopworld);
//            parts.put("!x", String.valueOf(x));
//            parts.put("!y", String.valueOf(y));
//            parts.put("!z", String.valueOf(z));
//
//            SignShop.log(SignShopConfig.getError("this_shop_exceeded_max_amount_of_chests", parts), Level.WARNING);
//        }
//
//        addSeller(seller_owner.getIdentifier(), seller_shopworld, seller_sign, seller_containables, seller_activatables, seller_items, miscsettings, false);
//        return true;
//    }

    /**
     * Verify that a shop has all the required properties to not fail at runtime.
     *
     * @param locationString the shop's location as described by {@link SignShopUtil#convertLocationToString(Location)}
     * @param shop the shop
     * @return the shop's location
     * @throws StorageException it the shop did not have all the required properties
     */
    private Location verifyShop(String locationString, Shop shop) throws StorageException {
        //Things to verify:
        // non-nullnes of: owner, world, sign-location, containables,
        // World must exist
        // Sign location must be a sign
        // Containables must be InventoryHolders
        // TODO verify something about the activatables? non-nullness? I'm not even sure how/why activatables exist - there is only one sign ever, right?
        // Shop contents size must not exceed InventoryHolder storage size (keep double chests into account!)

        if (shop == null) {
            throw new StorageException("Shop is null (does not exist)");
        }
        if (shop.getOwner() == null) {
            throw new StorageException("Shop has no owner");
        }
        String world = shop.getWorld();
        if (world == null) {
            throw new StorageException("Shop has no world");
        }
        if (Bukkit.getWorld(world) == null) {
            throw new StorageException("Shop world does not exist", world);
        }
        Location signLocation = shop.getSignLocation();
        if (signLocation == null) {
            throw new StorageException("Shop has no sign-location", world);
        }
        if (!ItemUtil.isSign(signLocation.getBlock())) {
            throw new StorageException("The sign-location does not host a sign", world);
        }
        ItemStack[] shopContents = shop.getItems();
        if (shopContents == null || shopContents.length == 0) {
            throw new StorageException("Shop has no contents");
        }
        Collection<Block> containables = shop.getContainables();
        if (containables == null || containables.isEmpty()) throw new StorageException("The shop has no container-locations", world);
        int storageContentsSize = 0;

        Set<Block> containersAlreadyCovered = new HashSet<>();
        for (Block containable : containables) {
            if (containersAlreadyCovered.contains(containable)) continue;

            if (containable.getState() instanceof InventoryHolder) {
                if (containable.getState() instanceof BlockInventoryHolder) {
                    BlockInventoryHolder blockInventoryHolder = (BlockInventoryHolder) containable.getState();
                    Block containerBlock = blockInventoryHolder.getBlock();
                    if (isDoubleChest(containerBlock)) {
                        //double chest - add 54 to the storage size and skip the other half
                        storageContentsSize += 6 * 9;
                        containersAlreadyCovered.add(getDoubleChestOtherHalf(containable));
                    } else {
                        //not a double chest - just add the storage size
                        storageContentsSize += blockInventoryHolder.getInventory().getStorageContents().length;
                    }
                } else {
                    //weird case - it's a Block, and an InventoryHolder, but not a BlockInventoryHolder
                    //I think this is dead code but I can't verify this to be true in future minecraft versions
                    //just add the storage size
                    storageContentsSize += ((InventoryHolder) containable.getState()).getInventory().getStorageContents().length;
                }
            } else {
                throw new StorageException("One of the shop's container location does not host a container block", world);
            }

            containersAlreadyCovered.add(containable);
        }
        if (storageContentsSize < shopContents.length) {
            throw new StorageException("The shop's containers don't have enough space for the shop contents", world);
        }

        if (SignShopConfig.ExceedsMaxChestsPerShop(containables.size())) {
            Map<String, String> parts = new HashMap<>();
            int x = signLocation.getBlockX();
            int y = signLocation.getBlockY();
            int z = signLocation.getBlockZ();
            parts.put("!world", world);
            parts.put("!x", String.valueOf(x));
            parts.put("!y", String.valueOf(y));
            parts.put("!z", String.valueOf(z));

            SignShop.log(SignShopConfig.getError("this_shop_exceeded_max_amount_of_chests", parts), Level.WARNING);
        }

        Location shopLocation = SignShopUtil.convertStringToLocation(locationString, Bukkit.getWorlds().get(0));
        if (shopLocation == null) {
            throw new StorageException("Shop's location does not exist or is invalid", world);
        }

        return shopLocation;
    }

    /**
     * Load shops from the yaml configuration file
     * @return whether the config needs to be saved again (because some shops did not load correctly)
     */
    @SuppressWarnings("unchecked")
    private boolean load() {
        FileConfiguration yml = YamlConfiguration.loadConfiguration(ymlfile);
        ConfigurationSection sellersSection = yml.getConfigurationSection("sellers");
        if (sellersSection == null)
            return false;

        for (Map.Entry<String, Object> entry : sellersSection.getValues(true).entrySet()) {
            String locationString = entry.getKey();
            Shop shop = (Shop) entry.getValue();

            try {
                Location shopLocation = verifyShop(locationString, shop);

                //when verified successfully - cache the shop!
                this.shops.put(shopLocation, shop);
            } catch (StorageException e) {
                SignShop.getInstance().getLogger().log(Level.SEVERE, "could not load a shop at location " + locationString + "!", e);

                invalidShops.put(locationString, shop);

                World world = Bukkit.getWorld(e.getWorld());
                if (world == null) {
                    //world does not exist
                    return true;
                }

                return false;
            }
        }

        Bukkit.getPluginManager().registerEvents(this, SignShop.getInstance());
        return false;

//        Map<String,HashMap<String,List<String>>> tempSellers = configUtil.fetchHashmapInHashmapwithList("sellers", yml);
//        if (tempSellers == null) {
//            SignShop.log("Invalid sellers.yml format detected. Old sellers format is no longer supported."
//                    + " Visit http://tiny.cc/signshop for more information.",
//                    Level.SEVERE);
//            return false;
//        }
//        if (tempSellers.isEmpty()) {
//            return false;
//        }
//        boolean needSave = false;
//        for (Map.Entry<String, HashMap<String,List<String>>> shopSettings : tempSellers.entrySet()) {
//            needSave = needSave || loadSellerFromSettings(shopSettings.getKey(), shopSettings.getValue());
//        }
    }

    private String getInvalidError(String template, String location, String world) {
        String[] locations = new String[4]; //using a String[] instead of a Location allows for non-existant worlds.
        String[] coords = location.split("/");
        locations[0] = world;
        if (coords.length > 2) {
            locations[1] = coords[0];
            locations[2] = coords[1];
            locations[3] = coords[2];
            return this.getInvalidError(template, locations);
        }
        return template;
    }

    private String getInvalidError(String template, String[] locations) {
        if (locations.length == 0) {
            return "";
        } else if (locations.length < 4) {
            return template.replace("!world", locations[0]);
        } else {
            return template
                .replace("!world", locations[0])
                .replace("!x", locations[1])
                .replace("!y", locations[2])
                .replace("!z", locations[3]);
        }
    }

    public final void save() {
        Map<String, Object> tempSellers = new HashMap<>();
        FileConfiguration config = new YamlConfiguration();

        for (Shop shop : shops.values()) {
            // YML Parser really does not like dots in the name
            String signLocation = SignShopUtil.convertLocationToString(shop.getSignLocation()).replace(".", "");

            tempSellers.put(signLocation, shop);
        }

        config.set("sellers", tempSellers);
        // We can not run the logic above async but we can save to disc on another thread
        queueSave(config);
    }

    public void addShop(PlayerIdentifier playerId, String sWorld, Block bSign, List<Block> containables, List<Block> activatables, ItemStack[] isItems, Map<String, String> misc) {
        addShop(playerId, sWorld, bSign, containables, activatables, isItems, misc, true);
    }

    public void addShop(PlayerIdentifier playerId, String sWorld, Block bSign, List<Block> containables, List<Block> activatables, ItemStack[] isItems, Map<String, String> misc, Boolean save) {
        shops.put(bSign.getLocation(), new Shop(playerId, sWorld, containables, activatables, isItems, bSign.getLocation(), misc, save));
        if(save)
            this.save();
    }

    public void updateShop(Block bSign, List<Block> containables, List<Block> activatables) {
        Shop shop = shops.get(bSign.getLocation());
        shop.setActivatables(activatables);
        shop.setContainables(containables);
    }

    public void updateShop(Block bSign, List<Block> containables, List<Block> activatables, ItemStack[] isItems) {
        Shop shop = shops.get(bSign.getLocation());
        shop.setActivatables(activatables);
        shop.setContainables(containables);
        shop.setItems(isItems);
    }

    public Shop getShop(Location lKey) {
        return shops.get(lKey);
    }

    public Collection<Shop> getShops() {
        return Collections.unmodifiableCollection(shops.values());
    }

    public void removeShop(Location lKey) {
        Shop shop = shops.remove(lKey);
        if (shop != null) {
            shop.cleanUp();
            save();
        }
    }

    public Integer countLocations(SignShopPlayer player) {
        int count = 0;
        for (Map.Entry<Location, Shop> entry : this.shops.entrySet())
            if (entry.getValue().isOwner(player)) {
                Block bSign = Bukkit.getServer().getWorld(entry.getValue().getWorld()).getBlockAt(entry.getKey());
                if (ItemUtil.isSign(bSign)) {
                    String[] sLines = ((Sign) bSign.getState()).getLines();
                    List<String> operation = SignShopConfig.getBlocks(SignShopUtil.getOperation(sLines[0]));
                    if (operation.isEmpty())
                        continue;
                    // Not isOP. No need to count OP signs here because admins aren't really their owner
                    if (!operation.contains("playerIsOp"))
                        count++;
                }
            }
        return count;
    }

    //TODO move to some other place?
    public static Block getDoubleChestOtherHalf(Block bBlock) {
        Chest chestData = (Chest) bBlock.getBlockData();
        Block otherHalf = null;

        switch (chestData.getType()) {
            case LEFT:
                switch (chestData.getFacing()) {
                    case NORTH:
                        otherHalf = bBlock.getRelative(BlockFace.EAST);
                        break;
                    case SOUTH:
                        otherHalf = bBlock.getRelative(BlockFace.WEST);
                        break;
                    case EAST:
                        otherHalf = bBlock.getRelative(BlockFace.SOUTH);
                        break;
                    case WEST:
                        otherHalf = bBlock.getRelative(BlockFace.NORTH);
                        break;
                }
                break;
            case RIGHT:
                switch (chestData.getFacing()) {
                    case NORTH:
                        otherHalf = bBlock.getRelative(BlockFace.WEST);
                        break;
                    case SOUTH:
                        otherHalf = bBlock.getRelative(BlockFace.EAST);
                        break;
                    case EAST:
                        otherHalf = bBlock.getRelative(BlockFace.NORTH);
                        break;
                    case WEST:
                        otherHalf = bBlock.getRelative(BlockFace.SOUTH);
                        break;
                }
                break;
        }

        return otherHalf;
    }

    public List<Block> getSignsFromHolder(Block bHolder) {
        List<Block> signs = new LinkedList<>();
        for (Map.Entry<Location, Shop> entry : shops.entrySet())
            if (entry.getValue().getContainables().contains(bHolder))
                signs.add(Bukkit.getServer().getWorld(entry.getValue().getWorld()).getBlockAt(entry.getKey()));
        return signs;
    }

    //TODO move to some other place
    public static boolean isDoubleChest(Block block) {
        if (block == null) return false;

        return block.getBlockData() instanceof Chest && ((Chest) block.getBlockData()).getType() != Chest.Type.SINGLE;
    }

    public boolean isShopContainer(Block block) {
        if (block == null) return false;
        if (!(block.getState() instanceof InventoryHolder)) return false;

        return shops.values().stream()
                .flatMap(shop -> shop.getContainables().stream())
                .anyMatch(b -> b.equals(block));
    }

    public Set<Shop> getShopsByBlock(Block block) {
        return getShopsByBlock(block, true);
    }

    public Set<Shop> getShopsByBlock(Block bBlock, boolean checkDoubleChestOtherHalf) {
        Set<Shop> shops = new HashSet<>();

        for (Map.Entry<Location, Shop> entry : this.shops.entrySet()) {
            if (entry.getValue().getActivatables().contains(bBlock) || entry.getValue().getContainables().contains(bBlock)) {
                shops.add(entry.getValue());
            }
        }

        if (checkDoubleChestOtherHalf) {
            if (isDoubleChest(bBlock)) {
                shops.addAll(getShopsByBlock(getDoubleChestOtherHalf(bBlock), false));
            }
        }

        return shops;
    }

    public List<Block> getShopsWithMiscSetting(String key, String value) {
        List<Block> blocks = new LinkedList<>();
        for (Map.Entry<Location, Shop> entry : this.shops.entrySet()) {
            if (entry.getValue().hasMisc(key)) {
                if (entry.getValue().getMisc(key).contains(value))
                    blocks.add(entry.getKey().getBlock());
            }
        }
        return blocks;
    }

    /**
     * @deprecated unused, will be removed in a future version
     * @return the item separator
     */
    @Deprecated
    public static String getItemSeperator() {
        return itemSeperator;
    }

    @Override
    public void run() {
        saveToFile();
    }

    private void queueSave(FileConfiguration config) {
        if (config == null)
            return;

        try {
            saveQueue.put(config);
        } catch (InterruptedException ex) {
            SignShop.log("Failed to save sellers.yml", Level.WARNING);
        }
    }

    private void saveToFile() {
        while (true) {
            try {
                FileConfiguration config = saveQueue.take();
                config.save(ymlfile);
            } catch(InterruptedException | IOException ex) {
                SignShop.log("Failed to save sellers.yml", Level.WARNING);
            }
        }
    }

    private void copyFile(File in, File out) throws IOException
    {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            inChannel.close();
            outChannel.close();
        }
    }

    private static class StorageException extends Exception {
        private static final long serialVersionUID = 1L;

        private String world = "";

        public StorageException() {

        }

        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, String world) {
            this(message);
            setWorld(world);
        }

        public StorageException(String message, World world) {
            this(message);
            setWorld(world);
        }

        public StorageException(World world) {
            this();
            setWorld(world);
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(World world) {
            setWorld(world.getName());
        }

        public void setWorld(String world) {
            this.world = world;
        }

        public boolean hasWorld() {
            return world != null && !world.isEmpty();
        }
    }
}
