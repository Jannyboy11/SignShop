package org.wargamer2010.signshop.util;

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.Vault;
import org.wargamer2010.signshop.configuration.LinkableMaterial;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSDestroyedEventType;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.events.SSLinkEvent;
import org.wargamer2010.signshop.events.SSMoneyRequestType;
import org.wargamer2010.signshop.events.SSMoneyTransactionEvent;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopOperation;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.specialops.SignShopSpecialOp;

public class SignShopUtil {

    private SignShopUtil() {
    }

    public static String getOperation(Sign sign, boolean lowercase) {
        if (sign == null)
            return "";
        String sSignOperation = sign.getLine(0);
        if (sSignOperation.length() < 4) {
            return "";
        }
        String stripped = ChatColor.stripColor(sSignOperation);
        String temp = stripped.substring(1, stripped.length()-1);
        return (lowercase ? temp.toLowerCase() : temp);
    }

    public static String getOperation(String sSignOperation) {
        if (sSignOperation.length() < 4){
            return "";
        }
        String stripped = ChatColor.stripColor(sSignOperation);
        return stripped.substring(1, stripped.length()-1).toLowerCase();
    }

    public static void generateInteractEvent(Block bLever, Player player, BlockFace bfBlockface) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getItemInHand(), bLever, bfBlockface);
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    public static List<String> getParameters(String sOperation) {
        List<String> parts = new LinkedList<>();
        if (sOperation.contains("{") && sOperation.contains("}")) {
            parts.add(sOperation.substring(0, sOperation.indexOf('{')));
            String parameter = sOperation.substring(sOperation.indexOf('{')+1, (sOperation.lastIndexOf('}')));
            String[] parbits = parameter.split(",");
            if (parbits.length > 1)
                parts.addAll(Arrays.asList(parbits));
            else
                parts.add(parameter);
        }
        if (parts.isEmpty())
            parts.add(sOperation);
        return parts;
    }

    public static SignShopOperation getSignShopBlock(String blockName) {
        if (blockName == null)
            return null;

        return SignShopConfig.getOperationInstances().get(blockName);
    }

    public static boolean getPriceFromMoneyEvent(SignShopArguments ssArgs) {
        SSMoneyTransactionEvent moneyevent = SSEventFactory.generateMoneyEvent(ssArgs, ssArgs.getMoneyEventType(), SSMoneyRequestType.GetAmount);
        SignShop.callEvent(moneyevent);
        ssArgs.getPrice().set(moneyevent.getPrice());
        ssArgs.setMessagePart("!price", EconomyUtil.formatMoney(ssArgs.getPrice().get()));
        return ((!moneyevent.isCancelled()) && moneyevent.isHandled());
    }

    public static List<SignShopOperationListItem> getSignShopOps(List<String> operation) {
        List<SignShopOperationListItem> SignShopOperations = new LinkedList<>();
        for (String sSignShopOp : operation) {
            List<String> bits = getParameters(sSignShopOp);
            String op = bits.get(0);
            bits.remove(0);
            SignShopOperation ssOP = getSignShopBlock(op);
            if (ssOP == null)
                return null;
            else
                SignShopOperations.add(new SignShopOperationListItem(ssOP, bits));
        }
        return SignShopOperations;
    }

    public static List<SignShopSpecialOp> getSignShopSpecialOps() {
        return SignShopConfig.getSpecialOps();
    }

    @SuppressWarnings("deprecation") // Accepted for transition reasons
    public static Map<Enchantment, Integer> convertStringToEnchantments(String sEnchantments) {
        Map<Enchantment, Integer> mEnchantments = new HashMap<>();
        String saEnchantments[] = sEnchantments.split(";");
        if (saEnchantments.length == 0)
            return mEnchantments;
        for (int i = 0; i < saEnchantments.length; i++) {
            String[] sEnchantment = saEnchantments[i].split("\\|");
            int iEnchantment; int iEnchantmentLevel;
            if (sEnchantment.length < 2)
                continue;
            else {
                Enchantment eTemp;
                try {
                    iEnchantment = Integer.parseInt(sEnchantment[0]);
                    eTemp = Enchantment.getByName(sEnchantment[0]);
                } catch(NumberFormatException ex) {
                    eTemp = Enchantment.getByName(sEnchantment[0]);
                }
                if (eTemp == null)
                    continue;
                try {
                    iEnchantmentLevel = Integer.parseInt(sEnchantment[1]);
                    mEnchantments.put(eTemp, iEnchantmentLevel);
                } catch (NumberFormatException ex) { }
            }
        }
        return mEnchantments;
    }

    public static String convertEnchantmentsToString(Map<Enchantment, Integer> aEnchantments) {
        StringBuilder sEnchantments = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Enchantment, Integer> entry : aEnchantments.entrySet()) {
            if (first) first = false;
            else sEnchantments.append(";");
            sEnchantments.append(entry.getKey().getName()).append("|").append(entry.getValue());
        }
        return sEnchantments.toString();
    }

    public static String convertLocationToString(Location loc) {
        return loc.getBlockX() + "/" + loc.getBlockY() + "/" + loc.getBlockZ() + "/" + loc.getWorld().getName();
    }

    public static Location convertStringToLocation(String sLoc, World fallbackWorld) {
        String[] sCoords = sLoc.split("/");
        if (sCoords.length < 3)
            return null;
        try {
            World world = fallbackWorld;
            if(sCoords.length > 3 && Bukkit.getWorld(sCoords[3]) != null)
                world = Bukkit.getWorld(sCoords[3]);
            Location loc = new Location(world, Double.parseDouble(sCoords[0]), Double.parseDouble(sCoords[1]), Double.parseDouble(sCoords[2]));
            return loc;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static double getNumberFromThirdLine(Block bSign) {
        return getNumberFromLine(bSign, 2);
    }

    public static Double getNumberFromLine(Block bSign, int line) {
        Sign sign = (Sign)bSign.getState();
        String XPline = sign.getLines()[line];
        if (XPline == null)
            return 0.0d;
        return EconomyUtil.parsePrice(XPline);
    }

    public static List<Integer> getSharePercentages(String line) {
        List<String> bits = new LinkedList<>();
        List<Integer> percentages = new LinkedList<>();
        if (line == null)
            return percentages;
        if (line.contains("/"))
            bits = Arrays.asList(line.split("/"));
        else
            bits.add(line);
        for (int i = 0; i < bits.size() && i < 2; i++) {
            String bit = bits.get(i);
            try {
                percentages.add(Integer.parseInt(bit));
            } catch(NumberFormatException ex) {
                continue;
            }
        }
        return percentages;
    }

    public static String implode(String[] ary, String delim) {
        if (ary == null) return "";

        return String.join(delim, ary);
    }

    public static String validateShareSign(List<Block> clickedBlocks, SignShopPlayer ssPlayer) {
        List<String> blocklocations = new LinkedList<>();
        List<Integer> percentages = new LinkedList<>();
        for (Block sharesign : clickedBlocks) {
            if (ItemUtil.isSign(sharesign)) {
                Sign sign = (Sign)sharesign.getState();
                List<Integer> tempperc = SignShopUtil.getSharePercentages(sign.getLine(3));
                percentages.addAll(tempperc);
                blocklocations.add(SignShopUtil.convertLocationToString(sharesign.getLocation()));
                if (tempperc.size() == 2 && (lineIsEmpty(sign.getLine(1)) || lineIsEmpty(sign.getLine(2))))
                    ssPlayer.sendMessage("No usernames have been given on the second and third line so the Share sign will be ignored.");
                else if (tempperc.size() == 2 && (lineIsEmpty(sign.getLine(1)) || lineIsEmpty(sign.getLine(2))))
                    ssPlayer.sendMessage("The second percentage will be ignored as only one username is given.");
                else if (tempperc.size() == 1 && !lineIsEmpty(sign.getLine(2)))
                    ssPlayer.sendMessage("The second username will be ignored as only one percentage is given.");
            }
        }
        int sum = 0;
        for (Integer percentage : percentages) {
            if (percentage <= 0) {
                ssPlayer.sendMessage("Each percentage should be greater than 0, please adjust the number(s) on the fourth line.");
                return "";
            }
            sum += percentage;
        }
        if (sum > 100) {
            ssPlayer.sendMessage("Sum of the percentages can never be greater than 100, please adjust the number(s) on the fourth line.");
            return "";
        }
        String[] implodedLocations = new String[blocklocations.size()];
        blocklocations.toArray(implodedLocations);

        return SignShopUtil.implode(implodedLocations, SignShopArguments.seperator);
    }

    public static String validateRestrictSign(List<Block> clickedBlocks, SignShopPlayer player) {
        List<String> blocklocations = new LinkedList<>();
        List<String> permGroups = Arrays.asList(Vault.getPermission().getGroups());
        for (Block restrictedsign : clickedBlocks) {
            if (ItemUtil.isSign(restrictedsign)) {
                Sign sign = (Sign)restrictedsign.getState();
                boolean bValidGroup = false;
                for (int i = 1; i < 4; i++) {
                    if (!lineIsEmpty(sign.getLine(i)))
                        bValidGroup = true;
                    if (!lineIsEmpty(sign.getLine(i)) && !permGroups.contains(sign.getLine(i)))
                        player.sendMessage("The group " + sign.getLine(i) + " does not currently exist!");
                }
                if (bValidGroup)
                    blocklocations.add(SignShopUtil.convertLocationToString(restrictedsign.getLocation()));
            }
        }

        String[] implodedLocations = new String[blocklocations.size()];
        blocklocations.toArray(implodedLocations);

        return SignShopUtil.implode(implodedLocations, SignShopArguments.seperator);
    }

    public static String validateBankSign(List<Block> clickedBlocks, SignShopPlayer player) {
        List<String> blocklocations = new LinkedList<>();
        Map<String, String> messageParts = new LinkedHashMap<>();

        if (!Vault.getEconomy().hasBankSupport()) {
            player.sendMessage(SignShopConfig.getError("no_bank_support", messageParts));
            return "";
        }

        for (Block banksign : clickedBlocks) {
            if (ItemUtil.isSign(banksign)) {
                Sign sign = (Sign)banksign.getState();
                String bank = sign.getLine(1);
                if (!Vault.getEconomy().bankBalance(bank).transactionSuccess())
                    player.sendMessage("The bank called " + sign.getLine(1) + " probably does not exist!");
                else if (!Vault.getEconomy().isBankOwner(bank, player.getName()).transactionSuccess() && !Vault.getEconomy().isBankMember(bank, player.getName()).transactionSuccess()
                        && !player.isOp()) {
                    messageParts.put("!bank", bank);
                    player.sendMessage(SignShopConfig.getError("not_allowed_to_use_bank", messageParts));
                    continue;
                }

                blocklocations.add(SignShopUtil.convertLocationToString(banksign.getLocation()));
            }
        }

        String[] implodedLocations = new String[blocklocations.size()];
        blocklocations.toArray(implodedLocations);

        return SignShopUtil.implode(implodedLocations, SignShopArguments.seperator);
    }

    public static Boolean restrictedFromUsing(Shop shop, SignShopPlayer player) {
        List<Block> blocks = SignShopUtil.getSignsFromMisc(shop, "restrictedsigns");
        if (blocks.isEmpty())
            return false;
        List<String> permGroups = Arrays.asList(Vault.getPermission().getGroups());
        List<String> playerGroups = new LinkedList<String>();
        for (Block restrictedsign : blocks) {
            if (ItemUtil.isSign(restrictedsign)) {
                Sign sign = (Sign)restrictedsign.getState();
                for (int i = 1; i < 4; i++) {
                    if (!lineIsEmpty(sign.getLine(i)) && !permGroups.contains(sign.getLine(i))) {
                        player.sendMessage("The group " + sign.getLine(i) + " does not currently exist!");
                    } else if (!lineIsEmpty(sign.getLine(i)) && permGroups.contains(sign.getLine(i))) {
                        playerGroups.add(sign.getLine(i));
                    }
                }
            }
        }
        for (String group : playerGroups) {
            if (Vault.playerInGroupAnyWorld(player.getPlayer(), group)) {
                return false;
            }
        }
        if (playerGroups.size() > 0 && shop.isOwner(player)) {
            player.sendMessage(SignShopConfig.getError("restricted_but_owner", null));
            return false;
        } else
            return playerGroups.size() > 0 && !player.isOp();
    }

    public static Boolean lineIsEmpty(String line) {
        return (line == null || line.length() == 0);
    }

    public static List<Block> getSignsFromMisc(Shop shop, String miscprop) {
        List<Block> signs = new LinkedList<Block>();
        if (shop.hasMisc(miscprop)) {
            String imploded = shop.getMisc(miscprop);
            String[] exploded;
            if (imploded.contains(SignShopArguments.seperator))
                exploded = imploded.split(SignShopArguments.seperator);
            else {
                exploded = new String[1];
                exploded[0] = imploded;
            }
            List<String> tempList = Arrays.asList(exploded);
            signs = getBlocksFromLocStringList(tempList, Bukkit.getServer().getWorld(shop.getWorld()));
        }
        return signs;
    }

    public static List<Block> getBlocksFromLocStringList(List<String> sLocs, World world) {
        List<Block> blocklist = new LinkedList<Block>();
        for (String loc : sLocs) {
            Location temp = SignShopUtil.convertStringToLocation(loc, world);
            if (temp != null)
                blocklist.add(temp.getBlock());
        }
        return blocklist;
    }

    public static List<Entity> getEntitiesFromMisc(Shop shop, String miscprop) {
        List<Entity> entities = new LinkedList<>();
        if (shop.hasMisc(miscprop)) {
            String imploded = shop.getMisc(miscprop);
            String[] exploded;
            if(imploded.contains(SignShopArguments.seperator))
                exploded = imploded.split(SignShopArguments.seperator);
            else {
                exploded = new String[1];
                exploded[0] = imploded;
            }
            List<String> tempList = Arrays.asList(exploded);
            entities = getEntitiesFromLocStringList(tempList, Bukkit.getServer().getWorld(shop.getWorld()));
        }
        return entities;
    }

    public static List<Entity> getEntitiesFromLocStringList(List<String> sLocs, World world) {
        List<Entity> entities = new LinkedList<>();
        List<Entity> worldEntities = world.getEntities();
        for (String loc : sLocs) {
            Location temp = SignShopUtil.convertStringToLocation(loc, world);
            if (temp != null) {
                for (Entity ent : worldEntities) {
                    if (SignShopUtil.roughLocationCompare(temp, ent.getLocation())) {
                        entities.add(ent);
                    }
                }
            }
        }
        return entities;
    }

    public static boolean clickedSignShopMat(Block bBlock, SignShopPlayer ssPlayer) {
        return clickedSignShopMat(bBlock.getType(), ssPlayer);
    }

    //did the original author plan to have npc's as a way of interacting?! :O :D
    @Deprecated
    public static boolean clickedSignShopMat(Entity eEntity, SignShopPlayer ssPlayer) {
        return clickedSignShopMat(eEntity.getType().toString(), ssPlayer);
    }

    public static boolean clickedSignShopMat(Material material, SignShopPlayer ssPlayer) {
        return clickedSignShopMat(material.name(), ssPlayer);
    }

    public static boolean clickedSignShopMat(String mat, SignShopPlayer ssPlayer) {
        String materialName = null;
        for (LinkableMaterial linkable : SignShopConfig.getLinkableMaterials()) {
            if (linkable.getMaterialName().equalsIgnoreCase(mat))
                materialName = linkable.getAlias();
        }
        if (materialName != null) {
            if(materialName.isEmpty()) // Leaving the alias empty probably means denylink shouldn't be checked
                return true;
            if(!ssPlayer.isOp() && ssPlayer.hasPerm("SignShop.DenyLink." + materialName, true) && !ssPlayer.hasPerm("SignShop.AllowLink." + materialName, true)) {
                ssPlayer.sendMessage(SignShopConfig.getError("link_notallowed", null));
                return false;
            }
            return true;
        } else
            return false;
    }

    public static Boolean registerClickedMaterial(PlayerInteractEvent event) {
        return registerClickedMaterial(event, event.getPlayer(), event.getClickedBlock());
    }

    public static boolean registerClickedMaterial(Cancellable event, Player player, Block clickedBlock) {
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        boolean signshopMat = registerClickedMaterial(clickedBlock, ssPlayer);
        if (signshopMat)
            event.setCancelled(true);
        return signshopMat;
    }

    public static boolean registerClickedMaterial(Block clickedBlock, SignShopPlayer ssPlayer) {
        if (clickedSignShopMat(clickedBlock, ssPlayer)) {
            //SignShop.getInstance().getLogger().info("DEBUG registerClickedMaterial - clicked linkable material");

            if (Clicks.mClicksPerLocation.containsKey(clickedBlock.getLocation())) {
                //SignShop.getInstance().getLogger().info("DEBUG registerClickedMaterial - mClickPerLocation = true");

                Clicks.mClicksPerLocation.remove(clickedBlock.getLocation());
                ssPlayer.sendMessage(SignShopConfig.getError("removed_location", null));
            } else {
                //SignShop.getInstance().getLogger().info("DEBUG registerClickedMaterial - mClickPerLocation = false");

                SSLinkEvent event = SSEventFactory.generateLinkEvent(clickedBlock, ssPlayer, null);
                SignShop.callEvent(event);
                if (event.isCancelled()) {
                    SignShop.getInstance().getLogger().info("DEBUG registerClickedMaterial - SSLinkEvent got cancelled!");
                    return false;
                } else {
                    //SignShop.getInstance().getLogger().info("DEBUG registerClickedMaterial - SSLinkEvent is not cancelled, doing stuff!");
                    Clicks.mClicksPerLocation.put(clickedBlock.getLocation(), ssPlayer.getPlayer());
                    Map<String, String> messageParts = new LinkedHashMap<>();
                    messageParts.put("!block", ItemUtil.formatData(clickedBlock.getBlockData()));
                    if (clickedBlock.getState() instanceof InventoryHolder) {
                        List<Block> containables = new LinkedList<>();
                        containables.add(clickedBlock);
                        ItemStack[] allStacks = ItemUtil.getAllItemStacksForContainables(containables);
                        messageParts.put("!items", (allStacks.length == 0 ? "nothing" : ItemUtil.itemStackToString(allStacks)));
                        ssPlayer.sendMessage(SignShopConfig.getError("stored_location_containable", messageParts));
                    } else {
                        ssPlayer.sendMessage(SignShopConfig.getError("stored_location", messageParts));
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static double applyPriceMod(SignShopArguments ssArgs, boolean bBuyOperation) {
        if (ssArgs.tryToApplyPriceMod()) {
            double fPrice;
            if (ssArgs.hasPlayer()) {
                fPrice = applyPriceMod(ssArgs.getPlayer().get(), ssArgs.getPrice().get(), ssArgs.getOperation().get(), bBuyOperation);
            } else {
                fPrice = applyPriceMod(null, ssArgs.getPrice().get(), ssArgs.getOperation().get(), bBuyOperation);
            }
            ssArgs.getPrice().set(fPrice);
            ssArgs.setMessagePart("!price", EconomyUtil.formatMoney(fPrice));
        }
        return ssArgs.getPrice().get();
    }

    public static double applyPriceMod(SignShopPlayer player, double fPrice, String sOperation, boolean bBuyOperation) {
        if (player == null) return fPrice;

        double fPricemod = player.getPlayerPricemod(sOperation, bBuyOperation);
        return (fPrice * fPricemod);
    }

    public static boolean getSignshopBlocksFromList(SignShopPlayer ssPlayer, List<Block> containables, List<Block> activatables, Block bClicked) {
        boolean multiWorld = false;
        Set<Location> lClicked = getKeysByValue(Clicks.mClicksPerLocation, ssPlayer.getPlayer());
        int chestCounter = 0;
        for (Location loc : lClicked) {
            Block bBlockat = loc.getBlock();
            if (bBlockat.getLocation().equals(bClicked.getLocation()))
                continue;
            if (bBlockat.getState() instanceof InventoryHolder) {
                containables.add(bBlockat);

                chestCounter++;
                boolean exceeded = SignShopConfig.ExceedsMaxChestsPerShop(chestCounter);
                if (exceeded) {
                    Map<String, String> parts = new LinkedHashMap<>();
                    parts.put("!maxAmountOfChests", Integer.toString(SignShopConfig.getMaxChestsPerShop()));
                    ssPlayer.sendMessage(SignShopConfig.getError("exceeded_max_amount_of_chests_per_shop", parts));
                    return false;
                }
            } else if (SignShopUtil.clickedSignShopMat(bBlockat, ssPlayer)) {
                activatables.add(bBlockat);
                if(ItemUtil.isDoor(bBlockat)) {
                    Block otherpart = ItemUtil.getOtherDoorPart(bBlockat);
                    if(otherpart != null)
                        activatables.add(otherpart);
                }
            }
            if (!multiWorld && !bBlockat.getWorld().getName().equals(bClicked.getWorld().getName())) {
                if (SignShopConfig.getAllowMultiWorldShops()) {
                    multiWorld = true;
                } else {
                    ssPlayer.sendMessage(SignShopConfig.getError("multiworld_not_allowed", null));
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Shop> getShopsFromMiscSetting(String miscname, Block pBlock) {
        List<Block> shopsWithBlockInMisc = Storage.get().getShopsWithMiscSetting(miscname, SignShopUtil.convertLocationToString(pBlock.getLocation()));
        List<Shop> shops = new LinkedList<>();
        if (!shopsWithBlockInMisc.isEmpty()) {
            for (Block block : shopsWithBlockInMisc) {
                shops.add(Storage.get().getShop(block.getLocation()));
            }
        }
        return shops;
    }

    public static Map<Shop, SSDestroyedEventType> getRelatedShopsByBlock(Block block) {
        Map<Shop, SSDestroyedEventType> affectedShops = new LinkedHashMap<>();

        if (Storage.get().getShop(block.getLocation()) != null)
            affectedShops.put(Storage.get().getShop(block.getLocation()), SSDestroyedEventType.SIGN);
        if (ItemUtil.isSign(block)) {
            for (Shop shop : getShopsFromMiscSetting("sharesigns", block))
                affectedShops.put(shop, SSDestroyedEventType.MISC_BLOCK);
            for (Shop shop : getShopsFromMiscSetting("restrictedsigns", block))
                affectedShops.put(shop, SSDestroyedEventType.MISC_BLOCK);
        }
        for (Shop shop : Storage.get().getShopsByBlock(block))
            affectedShops.put(shop, SSDestroyedEventType.ATTACHABLE);

        return affectedShops;
    }

    /**
     * @deprecated poor performance - should be using a BiMap instead.
     */
    @Deprecated
    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new LinkedHashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static boolean checkDistance(Block a, Block b, int maxDistance) {
        if (maxDistance <= 0) {
            return true;
        }
        int xDiff = Math.abs(a.getX() - b.getX());
        int yDiff = Math.abs(a.getY() - b.getY());
        int zDiff = Math.abs(a.getZ() - b.getZ());

        return xDiff <= maxDistance && yDiff <= maxDistance && zDiff <= maxDistance;
    }

    public static String capFirstLetter(final String string) {
        if (string == null || string.isEmpty()) return string;

        String workwith = string.replace("_", " ");
        String[] spacesplit;
        if (workwith.contains(" ")) {
            spacesplit = workwith.split(" ");
        } else {
            spacesplit = new String[1];
            spacesplit[0] = workwith;
        }
        for (int i = 0; i < spacesplit.length; i++) {
            char[] arr = spacesplit[i].toCharArray();
            arr[0] = Character.toUpperCase(arr[0]);
            spacesplit[i] = new String(arr);
        }
        return implode(spacesplit, " ");
    }

    public static boolean hasOPForCommand(SignShopPlayer player) {
        if (player != null && !player.isOp()) {
            player.sendMessage(SignShopConfig.getError("must_be_op_to_run", null));
            return false;
        }

        return true;
    }

    public static boolean floorEquals(double doubleA, double doubleB) {
        return ((int) doubleA) == ((int) doubleB);
    }

    public static boolean roughLocationCompare(Location locA, Location locB) {
        return (floorEquals(locA.getX(), locB.getX()) && floorEquals(locA.getY(), locB.getY()) && floorEquals(locA.getZ(), locB.getZ()));
    }

    public static double calculateDurabilityModifier(ItemStack[] stacks) {
        if (stacks.length == 0)
            return 1.0f;

        double totalmod = 0.0f;
        double totalamount = 0;
        for (ItemStack stack : stacks) {
            double dur = stack.getDurability();
            double max = stack.getType().getMaxDurability();
            double amount = stack.getAmount();
            totalmod += ((dur/max) * amount);
            totalamount += amount;
        }
        return (1.0d - (totalmod / totalamount));
    }

    /**
     * Gets the first operation parameter or gets operation name, replaces the placeholders
     * and writes it to the !param message part.
     *
     * @param ssArgs SignShopArguments
     * @return The first operation parameter or the operation name
     */
    public static String getParam(SignShopArguments ssArgs) {
        String rawparam = ssArgs.getOperation().get().toLowerCase();
        if (ssArgs.hasOperationParameters())
            rawparam = ssArgs.getFirstOperationParameter().toLowerCase();

        rawparam = SignShopConfig.fillInBlanks(rawparam, ssArgs.getMessageParts());
        rawparam = SignShopConfig.fillInBlanks(rawparam, ssArgs.getMessageParts());
        if (rawparam != null && !rawparam.isEmpty())
            ssArgs.setMessagePart("!param", rawparam);

        return rawparam;
    }
}
