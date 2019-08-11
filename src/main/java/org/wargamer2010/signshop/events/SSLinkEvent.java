package org.wargamer2010.signshop.events;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.player.SignShopPlayer;

public class SSLinkEvent extends SSEvent {
    private static final HandlerList handlers = new HandlerList();

    private final SignShopPlayer ssPlayer;
    private final Block bBlock;
    private final Shop seShop;

    public SSLinkEvent(Block pBlock, SignShopPlayer pPlayer, Shop pShop) {
        ssPlayer = pPlayer;
        bBlock = pBlock;
        seShop = pShop;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public SignShopPlayer getPlayer() {
        return ssPlayer;
    }

    public Block getBlock() {
        return bBlock;
    }

    public Shop getShop() {
        return seShop;
    }
}
