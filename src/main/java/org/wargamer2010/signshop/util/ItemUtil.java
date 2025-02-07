package org.wargamer2010.signshop.util;

import org.bukkit.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.event.block.Action;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.blocks.BookFactory;
import org.wargamer2010.signshop.blocks.BukkitSerialization;
import org.wargamer2010.signshop.blocks.IBookItem;
import org.wargamer2010.signshop.blocks.IItemTags;
import org.wargamer2010.signshop.blocks.SignShopItemMeta;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.VirtualInventory;

public class ItemUtil {
    private ItemUtil() {

    }

    /**
     * Returns the minimum amount of ItemStacks needed to function with RandomItem
     *
     * @param isItems Stacks to filter
     * @return The minimum ItemStacks needed
     */
    public static ItemStack[] getMinimumAmount(ItemStack[] isItems) {
        HashMap<ItemStack, Integer> materialByMaximumAmount = new LinkedHashMap<ItemStack, Integer>();

        for(ItemStack item: isItems) {
            ItemStack isBackup = getSingleAmountOfStack(item);
            if(!materialByMaximumAmount.containsKey(isBackup) || materialByMaximumAmount.get(isBackup) < item.getAmount())
                materialByMaximumAmount.put(isBackup, item.getAmount());
        }
        ItemStack[] isBackupToTake = new ItemStack[materialByMaximumAmount.size()];
        int i = 0;
        for(Map.Entry<ItemStack, Integer> entry : materialByMaximumAmount.entrySet()) {
            entry.getKey().setAmount(entry.getValue());
            isBackupToTake[i] = entry.getKey();
            i++;
        }
        return isBackupToTake;
    }

    /**
     * Creates a stream of all non-null items in the storage slots of an inventory
     * @param inventory the inventory
     * @return a Stream of non-null ItemStacks that are found in the storage slots of the inventory
     */
    public static Stream<ItemStack> streamStorageContents(Inventory inventory) {
        return Arrays.stream(inventory.getStorageContents()).filter(Objects::nonNull);
    }

    /**
     * Creates a stream of all the non-null items in an inventory.
     * @param inventory the inventory
     * @return a Stream of all items in the inventory
     */
    public static Stream<ItemStack> streamInventory(Inventory inventory) {
        return StreamSupport
                .stream(Spliterators
                        .spliterator(inventory.iterator(), inventory.getSize(),0), false)
                .filter(Objects::nonNull);
    }

    public static ItemStack[] getAllItemStacksForContainables(List<Block> containables) {
        return containables.stream()
                .map(Block::getState)
                .filter(InventoryHolder.class::isInstance)
                .map(InventoryHolder.class::cast)
                .map(InventoryHolder::getInventory)
                .flatMap(ItemUtil::streamInventory)
                .toArray(ItemStack[]::new);
    }

    //TODO fix this routine.
    public static boolean stockOKForContainables(List<Block> containables, ItemStack[] items, boolean bTakeOrGive) {
        return getFirstStockOKForContainables(containables, items, bTakeOrGive) != null;
    }

    public static InventoryHolder getFirstStockOKForContainables(List<Block> containables, ItemStack[] items, boolean bTakeOrGive) {
        for (Block bHolder : containables) {
            if (bHolder.getState() instanceof InventoryHolder) {
                InventoryHolder inventoryHolder = (InventoryHolder) bHolder.getState();
                VirtualInventory vInventory = new VirtualInventory(inventoryHolder.getInventory());
                if (vInventory.isStockOK(items, bTakeOrGive)) {
                    return inventoryHolder;
                }
            }
        }
        return null;
    }

    public static void fixBooks(ItemStack[] stacks) {
        if (stacks == null || !SignShopConfig.getEnableWrittenBookFix())
            return;

        for (ItemStack stack : stacks) {
            if (stack != null && stack.getType() == Material.WRITTEN_BOOK &&
                    stack.hasItemMeta() && stack.getItemMeta() instanceof BookMeta) {
                ItemStack copy = new ItemStack(Material.WRITTEN_BOOK);

                BookFactory.getBookItem(copy).copyFrom(BookFactory.getBookItem(stack));

                ItemMeta copyMeta = copy.getItemMeta();
                ItemMeta realMeta = stack.getItemMeta();

                copyMeta.setDisplayName(realMeta.getDisplayName());
                copyMeta.setLore(realMeta.getLore());

                for (Map.Entry<Enchantment, Integer> entry : realMeta.getEnchants().entrySet())
                    copyMeta.addEnchant(entry.getKey(), entry.getValue(), true);

                stack.setItemMeta(copyMeta);
            }
        }
    }

    public static String binaryToRoman(int binary) {
        final String[] RCODE = {"M", "CM", "D", "CD", "C", "XC", "L",
                                           "XL", "X", "IX", "V", "IV", "I"};
        final int[]    BVAL  = {1000, 900, 500, 400,  100,   90,  50,
                                               40,   10,    9,   5,   4,    1};
        if (binary <= 0 || binary >= 4000) {
            return "";
        }
        String roman = "";
        for (int i = 0; i < RCODE.length; i++) {
            while (binary >= BVAL[i]) {
                binary -= BVAL[i];
                roman  += RCODE[i];
            }
        }
        return roman;
    }

    /** @deprecated no longer useful after 'the flattening'
     * use {@link ItemUtil#formatData(Material)} instead.*/
    @Deprecated
    public static String formatData(MaterialData data) {
        short s = 0;
        return formatData(data, s);
    }

    /** @deprecated no longer useful after 'the flattening'
     * use {@link ItemUtil#formatData(BlockData)} instead.*/
    @Deprecated
    public static String formatData(MaterialData data, short durability) {
        String sData;
        // Lookup spout custom material
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Spout")) {
            sData = SpoutUtil.getName(data, durability);
            if (sData != null)
                return sData;
        }

        // For some reason running tostring on data when it's from an attachable material
        // will cause a NullPointerException, thus if we're dealing with an attachable, go the easy way :)
        if(data instanceof SimpleAttachableMaterialData)
            return stringFormat(data.getItemType().name());

        sData = data.toString().toLowerCase();

        Pattern p = Pattern.compile("\\(-?[0-9]+\\)");
        Matcher m = p.matcher(sData);
        sData = m.replaceAll("");
        sData = sData.replace("_", " ");

        StringBuffer sb = new StringBuffer(sData.length());
        p = Pattern.compile("(^|\\W)([a-z])");
        m = p.matcher(sData);
        while(m.find()) {
            m.appendReplacement(sb, m.group(1) + m.group(2).toUpperCase() );
        }

        m.appendTail(sb);

        return sb.toString();
    }


    public static String formatData(Material material) {
        return stringFormat(material.name());
    }

    public static String formatData(BlockData material) {
        return formatData(material.getMaterial());
        //TODO do we want to append more information that is in the blockdata?
    }

    private static String stringFormat(String sMaterial) {
        sMaterial = sMaterial.replace("_"," ");
        Pattern p = Pattern.compile("(^|\\W)([a-z])");
        Matcher m = p.matcher(sMaterial.toLowerCase());
        StringBuffer sb = new StringBuffer(sMaterial.length());

        while(m.find()){
            m.appendReplacement(sb, m.group(1) + m.group(2).toUpperCase() );
        }

        m.appendTail(sb);

        return sb.toString();
    }

    private static ItemStack getSingleAmountOfStack(ItemStack item) {
        if(item == null)
            return null;
        IItemTags tags = BookFactory.getItemTags();
        ItemStack isBackup = tags.getCraftItemstack(
            item.getType(),
            1,
            item.getDurability()
        );
        safelyAddEnchantments(isBackup, item.getEnchantments());
        if(item.getData() != null){
            isBackup.setData(item.getData());
        }
        return tags.copyTags(item, isBackup);
    }

    public static String itemStackToString(ItemStack[] itemStacks) {
        if(itemStacks == null || itemStacks.length == 0)
            return "";
        HashMap<ItemStack, Integer> items = new HashMap<>();
        HashMap<ItemStack, Map<Enchantment,Integer>> enchantments = new HashMap<>();
        StringBuilder sItems = new StringBuilder();
        boolean first = true;
        int tempAmount;
        for (ItemStack itemStack : itemStacks) {
            if(itemStack == null)
                continue;
            ItemStack isBackup = getSingleAmountOfStack(itemStack);

            if (itemStack.getEnchantments().size() > 0) {
                enchantments.put(isBackup, itemStack.getEnchantments());
            }
            if (items.containsKey(isBackup)) {
                tempAmount = items.get(isBackup) + itemStack.getAmount();
                items.put(isBackup, tempAmount);
            } else {
                items.put(isBackup, itemStack.getAmount());
            }
        }
        for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
            if (first) first = false;
            else sItems.append(", ");

            String newItemMeta = SignShopItemMeta.getName(entry.getKey());
            String count = (SignShopItemMeta.getTextColor() + entry.getValue().toString() + " ");
            if(newItemMeta.isEmpty())
                sItems.append(count + formatData(entry.getKey().getType()));
            else
                sItems.append(count + newItemMeta);
            if(ItemUtil.isWriteableBook(entry.getKey())) {
                IBookItem book = BookFactory.getBookItem(entry.getKey());
                if(book != null && (book.getAuthor() != null || book.getTitle() != null))
                    sItems.append(" (" + (book.getTitle() == null ? "Unknown" : book.getTitle())  + " by " + (book.getAuthor() == null ? "Unknown" : book.getAuthor()) + ")");
            }
            sItems.append(ChatColor.WHITE.toString());
        }

        return sItems.toString();
    }

    public static String enchantmentsToMessageFormat(Map<Enchantment,Integer> enchantments) {
        String enchantmentMessage = "";
        Boolean eFirst = true;

        enchantmentMessage += "(";
        for(Map.Entry<Enchantment,Integer> eEntry : enchantments.entrySet()) {
            if(eFirst) eFirst = false;
            else enchantmentMessage += ", ";
            enchantmentMessage += (stringFormat(eEntry.getKey().getName()) + " " + binaryToRoman(eEntry.getValue()));
        }
        enchantmentMessage += ")";
        return enchantmentMessage;
    }

    public static void setSignStatus(Block sign, ChatColor color) {
        if (isSign(sign)) {
            Sign signblock = ((Sign) sign.getState());
            String[] sLines = signblock.getLines();
            if (ChatColor.stripColor(sLines[0]).length() < 14) {
                signblock.setLine(0, (color + ChatColor.stripColor(sLines[0])));
                signblock.update();
            }
        }
    }

    public static Boolean needsEnchantment(ItemStack isEnchantMe, Map<Enchantment, Integer> enchantments) {
        if (enchantments.isEmpty())
            return false;
        Map<Enchantment, Integer> currentEnchantments = isEnchantMe.getEnchantments();
        
        for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
            if (!currentEnchantments.containsKey(enchantment.getKey()) || !currentEnchantments.get(enchantment.getKey()).equals(enchantment.getValue())) {
                return true;
            }
        }
        
        return false;
    }
    
    public static Boolean safelyAddEnchantments(ItemStack isEnchantMe, Map<Enchantment, Integer> enchantments) {
        if(enchantments.isEmpty())
            return true;

        try {
            isEnchantMe.addEnchantments(enchantments);
        } catch(IllegalArgumentException ex) {
            if(SignShopConfig.getAllowUnsafeEnchantments()) {
                try {
                    isEnchantMe.addUnsafeEnchantments(enchantments);
                } catch(IllegalArgumentException exfinal) {
                    return false;
                }
            } else
                return false;
        }
        return true;
    }

    public static HashMap<ItemStack, Integer> stacksToMap(ItemStack[] isStacks) {
        ItemStack[] clones = getBackupItemStack(isStacks);
        HashMap<ItemStack, Integer> mReturn = new HashMap<>();
        if (clones == null)
            return mReturn;
        int tempAmount;
        for (int i = 0; i < clones.length; i++) {
            if (clones[i] == null) continue;
            tempAmount = clones[i].getAmount();
            clones[i].setAmount(1);
            if (mReturn.containsKey(clones[i])) {
                tempAmount += mReturn.get(clones[i]);
                mReturn.remove(clones[i]);
                mReturn.put(clones[i], tempAmount);
            } else
                mReturn.put(clones[i], tempAmount);
        }
        return mReturn;
    }

    public static ItemStack[] getBackupItemStack(ItemStack[] isOriginal) {
        if (isOriginal == null)
            return null;

        return Arrays.stream(isOriginal)
                .filter(Objects::nonNull)
                .map(ItemUtil::getBackupItemStack)
                .toArray(ItemStack[]::new);
    }

    public static ItemStack getBackupItemStack(ItemStack isOriginal) {
        if (isOriginal == null) return null;

        return isOriginal.clone();
    }

    public static ItemStack[] filterStacks(ItemStack[] all, ItemStack[] filterby) {
        ItemStack[] filtered = new ItemStack[all.length];
        List<ItemStack> tempFiltered = new LinkedList<>();
        HashMap<ItemStack, Integer> mFilter = stacksToMap(filterby);
        for (ItemStack stack : all) {
            if (stack != null) {
                ItemStack temp = getBackupItemStack(stack);
                temp.setAmount(1);
                if (mFilter.containsKey(temp)) {
                    tempFiltered.add(stack);
                }
            }
        }

        return tempFiltered.toArray(filtered);
    }

    public static void updateStockStatusPerChest(Block bHolder, Block bIgnore) {
        List<Block> signs = Storage.get().getSignsFromHolder(bHolder);
        if(signs != null) {
            for (Block temp : signs) {
                if(temp == bIgnore)
                    continue;
                if(!isSign(temp))
                    continue;
                Shop shop = Storage.get().getShop(temp.getLocation());
                updateStockStatusPerShop(shop);
            }
        }
    }

    public static void updateStockStatusPerShop(Shop pShop) {
        if(pShop != null) {
            Block pSign = pShop.getSign();
            if(pSign == null || !(pSign.getState() instanceof Sign))
                return;
            String[] sLines = ((Sign) pSign.getState()).getLines();
            if(SignShopConfig.getIndividualOperations(SignShopUtil.getOperation(sLines[0])).isEmpty())
                return;
            List<String> operation = SignShopConfig.getIndividualOperations(SignShopUtil.getOperation(sLines[0]));
            List<SignShopOperationListItem> SignShopOperations = SignShopUtil.getSignShopOps(operation);
            if(SignShopOperations == null)
                return;
            SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sLines[3]), pShop.getItems(), pShop.getContainables(), pShop.getActivatables(),
                                                                null, null, pSign, SignShopUtil.getOperation(sLines[0]), null, Action.RIGHT_CLICK_BLOCK, SignShopArgumentsType.Check);
            if(pShop.getRawMisc() != null)
                ssArgs.miscSettings = pShop.getRawMisc();
            Boolean reqOK = true;
            for(SignShopOperationListItem ssOperation : SignShopOperations) {
                ssArgs.setOperationParameters(ssOperation.getParameters());
                reqOK = ssOperation.getOperation().checkRequirements(ssArgs, false);
                if(!reqOK) {
                    ItemUtil.setSignStatus(pSign, ChatColor.DARK_RED);
                    break;
                }
            }
            if(reqOK)
                ItemUtil.setSignStatus(pSign, ChatColor.DARK_BLUE);
        }
    }

    public static void updateStockStatus(Block bSign, ChatColor ccColor) {
        Shop seTemp = Storage.get().getShop(bSign.getLocation());
        if(seTemp != null) {
            List<Block> iChests = seTemp.getContainables();
            for(Block bHolder : iChests)
                updateStockStatusPerChest(bHolder, bSign);
        }
        setSignStatus(bSign, ccColor);
    }

    public static boolean isSign(Block bBlock) {
        return bBlock.getBlockData() instanceof org.bukkit.block.data.type.Sign;
    }

    public static boolean isDoor(Block bBlock) {
        return bBlock.getBlockData() instanceof Door;
    }

    public static Block getOtherDoorPart(Block bBlock) {
        if(!isDoor(bBlock))
            return null;
        Block up = bBlock.getWorld().getBlockAt(bBlock.getX(), bBlock.getY()+1, bBlock.getZ());
        Block down = bBlock.getWorld().getBlockAt(bBlock.getX(), bBlock.getY()-1, bBlock.getZ());

        Block otherpart = ((Door) bBlock.getBlockData()).getHalf() == Bisected.Half.TOP ? down : up;
        if(isDoor(otherpart))
            return otherpart;
        return null;
    }

    public static ItemStack[] convertStringtoItemStacks(List<String> sItems) {
        return sItems.stream()
                .map(itemStackArrayString -> {
                    try {
                        return BukkitSerialization.itemStackArrayFromBase64(itemStackArrayString);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ItemStack[0];
                    }
                })
                .flatMap(Arrays::stream)
                .toArray(ItemStack[]::new);

//        IItemTags tags = BookFactory.getItemTags();
//        ItemStack isItems[] = new ItemStack[sItems.size()];
//        int invalidItems = 0;
//
//        for(int i = 0; i < sItems.size(); i++) {
//            try {
//                String[] sItemprops = sItems.get(i).split(Storage.getItemSeperator());
//                if(sItemprops.length < 4) {
//                    invalidItems++;
//                    continue;
//                }
//
//                if(sItemprops.length <= 7) {
//                    if(i < (sItems.size() - 1) && sItems.get(i + 1).split(Storage.getItemSeperator()).length < 4) {
//                        // Bug detected, the next item will be the base64 string belonging to the current item
//                        // This bug will be fixed at the next save as the ~ will be replaced with a |
//                        sItemprops = (sItems.get(i) + "|" + sItems.get(i + 1)).split(Storage.getItemSeperator());
//                    }
//                }
//
//                if(sItemprops.length > 7) {
//                    String base64prop = sItemprops[7];
//                    // The ~ and | are used to differentiate between the old NBTLib and the BukkitSerialization
//                    if(base64prop != null && (base64prop.startsWith("~") || base64prop.startsWith("|"))) {
//                        String joined = Join(sItemprops, 7).substring(1);
//
//                        ItemStack[] convertedStacks = BukkitSerialization.itemStackArrayFromBase64(joined);
//                        if(convertedStacks.length > 0 && convertedStacks[0] != null) {
//                            isItems[i] = convertedStacks[0];
//                        }
//                    }
//                }
//
//                if(isItems[i] == null) {
//                    isItems[i] = tags.getCraftItemstack(
//                        Material.getMaterial(sItemprops[1]),
//                        Integer.parseInt(sItemprops[0]),
//                        Short.parseShort(sItemprops[2])
//                    );
//                    isItems[i].getData().setData(new Byte(sItemprops[3]));
//
//                    if(sItemprops.length > 4)
//                        safelyAddEnchantments(isItems[i], SignShopUtil.convertStringToEnchantments(sItemprops[4]));
//                }
//
//                if(sItemprops.length > 5) {
//                    try {
//                        isItems[i] = SignShopBooks.addBooksProps(isItems[i], Integer.parseInt(sItemprops[5]));
//                    } catch(NumberFormatException ex) {
//
//                    }
//                }
//                if(sItemprops.length > 6) {
//                    try {
//                        SignShopItemMeta.setMetaForID(isItems[i], Integer.parseInt(sItemprops[6]));
//                    } catch(NumberFormatException ex) {
//
//                    }
//                }
//            } catch(Exception ex) {
//                continue;
//            }
//        }
//
//        if(invalidItems > 0) {
//            ItemStack temp[] = new ItemStack[sItems.size() - invalidItems];
//            int counter = 0;
//            for(ItemStack i : isItems) {
//                if(i != null) {
//                    temp[counter] = i;
//                    counter++;
//                }
//            }
//
//            isItems = temp;
//        }
//
//
//        return isItems;
    }

    public static boolean isWriteableBook(ItemStack item) {
        if (item == null) return false;

        switch (item.getType()) {
            case WRITTEN_BOOK:
            case WRITABLE_BOOK:
                return true;
            default:
                return false;
        }
    }

    public static String[] convertItemStacksToString(ItemStack[] isItems) {
        List<String> sItems = new ArrayList<>();

        if(isItems == null)
            return new String[1];

        for(ItemStack isCurrent : isItems) {
            if(isCurrent != null) {
//                String ID = "";
//                if(ItemUtil.isWriteableBook(isCurrent))
//                    ID = SignShopBooks.getBookID(isCurrent).toString();
//                String metaID = SignShopItemMeta.getMetaID(isCurrent).toString();
//                if(metaID.equals("-1"))
//                    metaID = "";

//                String stringItem = (isCurrent.getAmount() + Storage.getItemSeperator()
//                        + isCurrent.getType().getId() + Storage.getItemSeperator()
//                        + isCurrent.getDurability() + Storage.getItemSeperator()
//                        + isCurrent.getData().getData() + Storage.getItemSeperator()
//                        + SignShopUtil.convertEnchantmentsToString(isCurrent.getEnchantments()) + Storage.getItemSeperator()
//                        + ID + Storage.getItemSeperator()
//                        + metaID + Storage.getItemSeperator()
//                        + "|" + BukkitSerialization.itemStackArrayToBase64(stacks));
                String stringItem = BukkitSerialization.itemStackArrayToBase64(new ItemStack[] {isCurrent});

                sItems.add(stringItem);
            }

        }

        return sItems.toArray(new String[sItems.size()]);
    }

    public static boolean isItemStackSimilar(ItemStack a, ItemStack b, boolean ignoreDamage) {
        ItemStack aClone = a.clone();
        ItemStack bClone = b.clone();

        if (ignoreDamage) {
            ItemMeta aMeta = aClone.getItemMeta();
            ItemMeta bMeta = bClone.getItemMeta();

            if (aMeta instanceof Damageable && bMeta instanceof Damageable) {
                Damageable ad = (Damageable) aMeta;
                Damageable bd = (Damageable) bMeta;

                ad.setDamage(0);
                bd.setDamage(0);

                aClone.setItemMeta(aMeta);
                bClone.setItemMeta(bMeta);
            }
        }

        return aClone.isSimilar(bClone);
    }

    public static boolean loadChunkByBlock(Block block, int radius) {
        final int chunkSize = 16;
        boolean ok = true;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ok = ok && loadChunkByBlock(block.getWorld().getBlockAt(
                                        block.getX() + (x * chunkSize),
                                        block.getY(),
                                        block.getZ() + (z * chunkSize)));
            }
        }

        return ok;
    }

    public static boolean loadChunkByBlock(Block block) {
        if (block == null) return false;

        Chunk chunk = block.getChunk();
        return chunk.isLoaded() || chunk.load();
    }
}
