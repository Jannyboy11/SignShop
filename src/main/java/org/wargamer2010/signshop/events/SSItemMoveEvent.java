package org.wargamer2010.signshop.events;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.Shop;

import java.util.Objects;

/**
 * Called when a hopper/dropper tries to move an item into or out of a container owned by a shop.
 * This event can also be called by other plugins to simulate items being moved (or magically being created/deleted) without player interaction.
 */
public class SSItemMoveEvent extends SSEvent {

    public enum Direction {
        /** The item moves into the container of the shop */
        IN,
        /** The item moves out of the container of the shop */
        OUT;
    }

    private static final HandlerList handlers = new HandlerList();

    private final Shop shop;
    private final BlockInventoryHolder container;
    private final Direction direction;
    private final ItemStack itemStack;

    public SSItemMoveEvent(Shop shop, BlockInventoryHolder container, Direction direction, ItemStack item) {
        this.shop = Objects.requireNonNull(shop, "Shop cannot be null");
        this.container = Objects.requireNonNull(container, "Container cannot be null");
        this.direction = Objects.requireNonNull(direction, "Direction cannot be null");
        this.itemStack = Objects.requireNonNull(item, "Item cannot be null");
    }

    /**
     * The shop that the item is moved from/into.
     * @return the shop
     */
    public Shop getShop() {
        return shop;
    }

    /**
     * The container whose block is associated with the shop.
     * @return the container
     */
    public BlockInventoryHolder getContainer() {
        return container;
    }

    /**
     * The direction the item is moved in.
     * @return {@link Direction#OUT} if the item is moved out of the container, {@link Direction#IN} if the item is moved into the container
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * The item that is being moved.
     * @return the item stack
     */
    public ItemStack getItem() {
        return itemStack;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
