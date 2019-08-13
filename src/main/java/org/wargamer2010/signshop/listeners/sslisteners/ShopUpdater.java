
package org.wargamer2010.signshop.listeners.sslisteners;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSCreatedEvent;
import org.wargamer2010.signshop.events.SSItemMoveEvent;
import org.wargamer2010.signshop.events.SSPostTransactionEvent;
import org.wargamer2010.signshop.events.SSTouchShopEvent;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.operations.givePlayerItems;
import org.wargamer2010.signshop.operations.takePlayerItems;
import org.wargamer2010.signshop.player.VirtualInventory;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ShopUpdater implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSSPostCreatedEvent(SSCreatedEvent event) {
        if (!event.isCancelled())
            ItemUtil.setSignStatus(event.getSign(), ChatColor.DARK_BLUE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSSTouchShopEvent(SSTouchShopEvent event) {
        if (event.isCancelled())
            return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getBlock().getState() instanceof InventoryHolder) {
            ItemUtil.updateStockStatusPerShop(event.getShop());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSSSoldItemEvent(SSPostTransactionEvent event) {
        if (event.isCancelled())
            return;

        Collection<Block> containers = event.getContainables();
        Storage store = Storage.get();

        //can't simply use event.getShop because multiple shops can be linked to the same chest
        Set<Shop> shops = new HashSet<>();
        shops.add(event.getShop());
        for (Block container : containers) {
            shops.addAll(store.getShopsByBlock(container));
        }

        for (Shop shop : shops) {
           updateShopSign(shop);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopRestock(InventoryCloseEvent event) {
        //TODO call ShopRestockEvent or something?
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockInventoryHolder) {
            BlockInventoryHolder blockInventoryHolder = (BlockInventoryHolder) holder;

            Block block = blockInventoryHolder.getBlock();
            for (Shop shop : Storage.get().getShopsByBlock(block)) {
                updateShopSign(shop);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSSItemMove(SSItemMoveEvent event) {
        if (event.isCancelled()) return;

        updateShopSign(event.getShop());
    }

    private enum BuySell { BUY, SELL, AMBIGUOUS, UNKNOWN }

    private void updateShopSign(Shop shop) {
        Sign sign = (Sign) shop.getSign().getState();
        String firstLine = sign.getLine(0);
        List<SignShopOperationListItem> operations = SignShopUtil.getSignShopOps(Arrays.asList(firstLine));
        //TODO debug operations

        if (operations == null) return;

        BuySell buySell = BuySell.UNKNOWN;

        for (SignShopOperationListItem operation : operations) {
            if (operation.getOperation() instanceof givePlayerItems) {
                //BUY
                if (buySell == BuySell.UNKNOWN) buySell = BuySell.BUY;
                if (buySell == BuySell.SELL) buySell = BuySell.AMBIGUOUS;
            } else if (operation.getOperation() instanceof takePlayerItems) {
                //SELL
                if (buySell == BuySell.UNKNOWN) buySell = BuySell.SELL;
                if (buySell == BuySell.BUY) buySell = BuySell.AMBIGUOUS;
            }
        }

        ChatColor color = ChatColor.BLACK;
        if (buySell == BuySell.BUY) {
            color = hasEnoughItems(shop) ? ChatColor.DARK_BLUE : ChatColor.DARK_RED;
        } else if (buySell == BuySell.SELL) {
            color = checkEnoughSpace(shop) ? ChatColor.DARK_BLUE : ChatColor.DARK_RED;
        }

        if (buySell != BuySell.UNKNOWN) {
            //only update the color for buy and sell signs
            //make it black if it's ambiguous
            ItemUtil.setSignStatus(shop.getSign(), color);
        }
    }

    private static boolean isAir(ItemStack itemStack) {
        if (itemStack == null) return true;
        if (itemStack.getAmount() <= 0) return true;
        switch (itemStack.getType()) {
            case AIR:
            case VOID_AIR:
            case CAVE_AIR:
                return true;
            default:
                return false;
        }
    }

    private static void merge(ItemStack into, ItemStack attempt) {
        if (!into.isSimilar(attempt)) return;
        int howManyMerge = Math.min(into.getMaxStackSize() - into.getAmount(), attempt.getAmount());
        into.setAmount(howManyMerge);
        attempt.setAmount(attempt.getAmount() - howManyMerge);
    }

    /**
     * Check whether the shop has enough space in its containers
     * @param shop the shop
     * @return true if there is enough space, otherwise false
     */
    private static boolean checkEnoughSpace(Shop shop) {
        ItemStack[] shopItems = shop.getItems();

        //check empty spaces first.
        long emptySpaces = shop.getContainables().stream()
                .map(Block::getState)
                .filter(InventoryHolder.class::isInstance)
                .map(InventoryHolder.class::cast)
                .map(InventoryHolder::getInventory)
                .flatMap(ItemUtil::streamStorageContents)
                .filter(ShopUpdater::isAir)
                .count();
        if (shopItems.length <= emptySpaces) return true;

        //if that's not enough, check whether we can merge into other stacks.

        LinkedList<ItemStack> nonNullContents = shop.getContainables().stream()
                .map(Block::getState)
                .filter(InventoryHolder.class::isInstance)
                .map(InventoryHolder.class::cast)
                .map(InventoryHolder::getInventory)
                .flatMap(ItemUtil::streamStorageContents)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));

        LinkedList<ItemStack> shopItemList = new LinkedList<>(Arrays.asList(shopItems));
        while (!shopItemList.isEmpty()) {
            if (shopItemList.size() < emptySpaces) return true;

            ItemStack attempt = shopItemList.removeFirst();
            Iterator<ItemStack> nonNullStackIterator = nonNullContents.iterator();
            while (nonNullStackIterator.hasNext()) {
                ItemStack nonNullStack = nonNullStackIterator.next();
                merge(nonNullStack, attempt);
                if (nonNullStack.getAmount() == nonNullStack.getMaxStackSize())
                    nonNullStackIterator.remove();
                if (isAir(attempt))
                    break;
            }

            if (!isAir(attempt)) {
                //could not merge the entire stack
                //add it to the list of container contents so that other stacks from the shop can merge into it in the next iterations
                emptySpaces -= 1;
                nonNullContents.addLast(attempt);
            }

            if (emptySpaces < 0) return false;
        }

        return emptySpaces >= 0;
    }

    /**
     * Checks whether the shop has enough items in its containers;
     * @param shop the shop to check
     * @return true if the shop has the items in its containers, otherwise false
     */
    //this computation can be quite expensive if a shop has many containers linked.
    private static boolean hasEnoughItems(Shop shop) {
        ItemStack[] shopItems = shop.getItems(false);
        Map<ItemStack, Integer> itemsForSale = Arrays.stream(shopItems)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(is -> {ItemStack c = is.clone(); c.setAmount(1); return c;}, ItemStack::getAmount, Integer::sum));

        Map<ItemStack, Integer> availableItems = shop.getContainables().stream()
                .map(Block::getState)
                .filter(InventoryHolder.class::isInstance)
                .map(InventoryHolder.class::cast)
                .map(InventoryHolder::getInventory)
                .flatMap(ItemUtil::streamStorageContents)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(is -> {ItemStack c = is.clone(); c.setAmount(1); return c;}, ItemStack::getAmount, Integer::sum));

        //for every item that the shop can sell/buy
        for (Map.Entry<ItemStack, Integer> entry : itemsForSale.entrySet()) {
            ItemStack baseItem = entry.getKey();
            Integer amount = entry.getValue();

            //subtract it from the items that are in the chests
            Integer leftOver = availableItems.compute(baseItem, (is, count) -> {
                if (count == null) count = 0; //item was not present in the shop containers

                return count - amount;
            });

            //if the container items map has run out of the item, return false
            if (leftOver < 0) return false;
        }

        //we only get here when the shop has all the items that were in the stream
        return true;
    }

}
