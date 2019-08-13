package org.wargamer2010.signshop.listeners;

import java.util.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.bukkit.block.BlockFace;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Attachable;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSDestroyedEvent;
import org.wargamer2010.signshop.events.SSDestroyedEventType;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.events.SSItemMoveEvent;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.SignShopUtil;

public class SignShopBlockListener implements Listener {

    private List<Block> getAttachables(Block from) {
        List<Block> attachables = new ArrayList<>();
        List<BlockFace> checkFaces = new ArrayList<>();
        checkFaces.add(BlockFace.UP);
        checkFaces.add(BlockFace.NORTH);
        checkFaces.add(BlockFace.EAST);
        checkFaces.add(BlockFace.SOUTH);
        checkFaces.add(BlockFace.WEST);

        for (BlockFace face : checkFaces) {
            if (from.getRelative(face).getState().getData() instanceof Attachable) {
                Attachable att = (Attachable)from.getRelative(face).getState().getData();
                if (from.getRelative(face).getRelative(att.getAttachedFace()).equals(from))
                    attachables.add(from.getRelative(face));
            }
        }

        return attachables;
    }

    private boolean canBreakBlock(Block block, Player player, boolean recurseOverAttachables) {
        if (player == null) {
            //called on block burn
            return false;
        }

        Map<Shop, SSDestroyedEventType> affectedShops = SignShopUtil.getRelatedShopsByBlock(block);
        SignShopPlayer ssPlayer = new SignShopPlayer(player);

        for (Map.Entry<Shop, SSDestroyedEventType> destroyal : affectedShops.entrySet()) {
            SSDestroyedEvent event = new SSDestroyedEvent(block, ssPlayer, destroyal.getKey(), destroyal.getValue());
            SignShop.callEvent(event);
            if (event.isCancelled())
                return false;
        }

        if (recurseOverAttachables) {
            for (Block attached : getAttachables(block)) {
                if (!canBreakBlock(attached, player, false))
                    return false;
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        // We want to run at the Highest level so we can tell if other plugins cancelled the event
        // But we don't want to run at Monitor since we want to be able to cancel the event ourselves
        if (event.isCancelled())
            return;
        if (!canBreakBlock(event.getBlock(), event.getPlayer(), true))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBurn(BlockBurnEvent event) {
//        if(event.isCancelled())
//            return;

        //if(!canBreakBlock(event.getBlock(), null, true))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemTransfer(InventoryMoveItemEvent event) {
        //respect other plugins first
        boolean cancel = event.isCancelled();

        //TODO use constructor dependency injection instead of Storage.get()
        Storage store = Storage.get();

        InventoryHolder source = event.getSource().getHolder();
        InventoryHolder target = event.getDestination().getHolder();

        if (source instanceof BlockInventoryHolder) {
            BlockInventoryHolder blockInventoryHolder = (BlockInventoryHolder) source;
            Block block = blockInventoryHolder.getBlock();
            if (store.isShopContainer(block)) {
                Set<Shop> shops = store.getShopsByBlock(block);
                cancel |= SSEventFactory.callItemMoveEvent(shops, blockInventoryHolder, SSItemMoveEvent.Direction.OUT, event.getItem(), cancel);
            }
        }
        if (target instanceof BlockInventoryHolder) {
            BlockInventoryHolder blockInventoryHolder = (BlockInventoryHolder) source;
            Block block = blockInventoryHolder.getBlock();
            if (store.isShopContainer(block)) {
                Set<Shop> shops = store.getShopsByBlock(block);
                cancel |= SSEventFactory.callItemMoveEvent(shops, blockInventoryHolder, SSItemMoveEvent.Direction.IN, event.getItem(), cancel);
            }
        }

        event.setCancelled(cancel);
    }
}
