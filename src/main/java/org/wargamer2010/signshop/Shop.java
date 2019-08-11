package org.wargamer2010.signshop;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;

import java.util.*;

import org.wargamer2010.signshop.util.ItemUtil;
import com.kellerkindt.scs.*;

import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.block.Sign;
import org.wargamer2010.signshop.blocks.SignShopBooks;
import org.wargamer2010.signshop.blocks.SignShopItemMeta;
import org.wargamer2010.signshop.player.PlayerIdentifier;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.SignShopUtil;

@SerializableAs("Shop")
public class Shop implements ConfigurationSerializable {
    private List<Block> containables; //TODO shouldn't this be a Set<Block> instead?
    private List<Block> activatables; //TODO shouldn't this be a Set<Block> instead?
    private ItemStack[] shopContents;
    private Location signLocation;
    private Map<String, String> miscProps = new HashMap<>();
    private Map<String, String> volatileProperties = new LinkedHashMap<>();
    //private Map<String, Object> serializedData = new HashMap<>();

    private SignShopPlayer owner;
    private String world;

    public Shop(PlayerIdentifier playerId, String sWorld, List<Block> pContainables, List<Block> pActivatables, ItemStack[] isChestItems, Location location,
                Map<String, String> pMiscProps, boolean save) {
        owner = new SignShopPlayer(playerId);
        world = sWorld;

        shopContents = ItemUtil.getBackupItemStack(isChestItems);
        containables = pContainables;
        activatables = pActivatables;
        signLocation = location;

        if (pMiscProps != null) {
            miscProps.putAll(pMiscProps);
        }
        if (save) {
            storeMeta(shopContents);
        }

        //calculateSerialization();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("owner", owner.getIdentifier());
        map.put("world", world);
        if (shopContents != null) map.put("contents", Arrays.asList(shopContents));
        if (containables != null) map.put("container-locations", containables.stream().map(Block::getLocation).collect(Collectors.toList()));
        if (activatables != null) map.put("activator-locations", activatables.stream().map(Block::getLocation).collect(Collectors.toList()));
        map.put("sign-location", signLocation);
        map.put("misc-properties", miscProps);
        map.put("volatile-properties", volatileProperties);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static Shop valueOf(Map<String, Object> map) {
        PlayerIdentifier playerId = (PlayerIdentifier) map.get("owner");
        String world = String.valueOf(map.get("world"));

        List<ItemStack> contents = (List<ItemStack>) map.get("contents");
        ItemStack[] shopContents = contents == null ? new ItemStack[0] : contents.toArray(new ItemStack[contents.size()]);


        List<Location> containerLocations = (List<Location>) map.get("container-locations");
        List<Block> containables = containerLocations == null ? new LinkedList<>() : containerLocations.stream().map(Location::getBlock).collect(Collectors.toList());

        List<Location> activatorLocations = (List<Location>) map.get("activator-locations");
        List<Block> activatables = activatorLocations == null ? new LinkedList<>() : activatorLocations.stream().map(Location::getBlock).collect(Collectors.toList());

        Location signLocation = (Location) map.get("sign-location");
        Map<String, String> miscProperties = (Map<String, String>) map.get("misc-properties");
        Map<String, String> volatileProperties = (Map<String, String>) map.get("volatile-properties");

        Shop shop = new Shop(playerId, world, containables, activatables, shopContents, signLocation, miscProperties, false);
        shop.volatileProperties = volatileProperties;

        return shop;
    }

    public ItemStack[] getItems() {
        return getItems(true);
    }

    public ItemStack[] getItems(boolean copy) {
        if (copy)
            return ItemUtil.getBackupItemStack(shopContents);
        else
            return shopContents;
    }

    public void setItems(ItemStack[] items) {
        shopContents = items;
    }

    public List<Block> getContainables() {
        if (containables == null) return Collections.emptyList();
        return containables;
    }

    public void setContainables(List<Block> blocklist) {
        containables = blocklist;
    }

    public List<Block> getActivatables() {
        if (activatables == null) return Collections.emptyList();
        return activatables;
    }

    public void setActivatables(List<Block> blocklist) {
        activatables = blocklist;
    }

    public SignShopPlayer getOwner() {
        return owner;
    }

    public void setOwner(SignShopPlayer newowner) {
        owner = newowner;
    }

    public boolean isOwner(SignShopPlayer player) {
        return player.equals(owner);
    }

    public String getWorld() {
        return world;
    }

    public boolean hasMisc(String key) {
        return miscProps.containsKey(key);
    }

    public void removeMisc(String key) {
        miscProps.remove(key);
    }

    public void addMisc(String key, String value) {
        miscProps.put(key, value);
    }

    public String getMisc(String key) {
        if (miscProps.containsKey(key))
            return miscProps.get(key);
        return null;
    }

    public Map<String, String> getRawMisc() {
        return miscProps;
    }

    public void cleanUp() {
        if (miscProps.containsKey("showcaselocation")) {
            if (Bukkit.getServer().getPluginManager().getPlugin("ShowCaseStandalone") == null)
                return;

            Location loc = SignShopUtil.convertStringToLocation(miscProps.get("showcaselocation"), Bukkit.getWorld(world));
            ShowCaseStandalone scs = (ShowCaseStandalone) Bukkit.getServer().getPluginManager().getPlugin("ShowCaseStandalone");
            com.kellerkindt.scs.shops.Shop shop;

            try {
                shop = scs.getShopHandler().getShop(Bukkit.getWorld(world).getBlockAt(loc));
            } catch (Exception ex) {
                SignShop.log(String.format("Caught an exception (%s) while attempting to remove showcase for shop at (%s, %s, %s)"
                        , ex.getMessage(), loc.getX(), loc.getY(), loc.getZ()), Level.WARNING);
                return;
            }

            if (shop != null)
                scs.getShopHandler().removeShop(shop);
        }
    }

    public static void storeMeta(ItemStack[] stacks) {
        if (stacks == null) return;

        for (ItemStack stack : stacks) {
            if (stack != null) {
                if (ItemUtil.isWriteableBook(stack)) {
                    SignShopBooks.addBook(stack);
                }

                SignShopItemMeta.storeMeta(stack);
            }
        }
    }

    public String getVolatile(String key) {
        return volatileProperties.get(key);
    }

    public void setVolatile(String key, String value) {
        volatileProperties.put(key, value);
    }

    public Block getSign() {
        if (signLocation == null) return null;

        return signLocation.getBlock();
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public String getOperation() {
        Block block = getSign();
        if (block == null)
            return "";

        if (ItemUtil.isSign(block)) {
            Sign sign = (Sign) block.getState();
            return SignShopUtil.getOperation(sign.getLine(0));
        }

        return "";
    }

    /**
     * @deprecated Blocks are live objects, any update will be reflected in the collections in the Shop
     */
    @Deprecated
    public void reloadBlocks() {
        //should not be needed since Block is a 'live' object

        List<Block> tempContainables = new LinkedList<>();
        List<Block> tempActivatables = new LinkedList<>();
        for (Block a : containables) {
            tempContainables.add(a.getWorld().getBlockAt(a.getX(), a.getY(), a.getZ()));
        }
        for (Block b : activatables) {
            tempActivatables.add(b.getWorld().getBlockAt(b.getX(), b.getY(), b.getZ()));
        }
        containables = tempContainables;
        activatables = tempActivatables;
        //calculateSerialization();
    }

//    /**
//     * @deprecated will be replaced by the {@link ConfigurationSerializable} functionality
//     */
//    @Deprecated
//    public Map<String, Object> getSerializedData() {
//        return serializedData;
//    }

//    /**
//     * @deprecated will be replaced by the {@link ConfigurationSerializable} functionality
//     */
//    @Deprecated
//    private void calculateSerialization() {
//        Map<String,Object> temp = new HashMap<>();
//
//        temp.put("shopworld", getWorld());
//        temp.put("owner", getOwner().getIdentifier().toString());
//        temp.put("items", ItemUtil.convertItemStacksToString(getItems(false)));
//
//        String[] sContainables = new String[containables.size()];
//        for (int i = 0; i < containables.size(); i++) {
//            sContainables[i] = SignShopUtil.convertLocationToString(containables.get(i).getLocation());
//        }
//        temp.put("containables", sContainables);
//
//        String[] sActivatables = new String[activatables.size()];
//        for (int i = 0; i < activatables.size(); i++) {
//            sActivatables[i] = SignShopUtil.convertLocationToString(activatables.get(i).getLocation());
//        }
//
//        temp.put("activatables", sActivatables);
//
//        temp.put("sign", SignShopUtil.convertLocationToString(getSignLocation()));
//
//        Map<String, String> misc = miscProps;
//        if (misc.size() > 0)
//            temp.put("misc", mapToList(misc));
//
//        serializedData = temp;
//    }

//    /**
//     * @deprecated will be replaced by the {@link ConfigurationSerializable} functionality
//     */
//    @Deprecated
//    private static List<String> mapToList(Map<String, String> map) {
//        List<String> returnList = new LinkedList<>();
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            returnList.add(entry.getKey() + ":" + entry.getValue());
//        }
//        return returnList;
//    }

}
