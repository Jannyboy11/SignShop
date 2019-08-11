package org.wargamer2010.signshop.events;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.player.SignShopPlayer;

public class SSDestroyedEvent extends SSEvent {
    private static final HandlerList handlers = new HandlerList();

    private final SignShopPlayer ssPlayer;
    private final Block bBlock;
    private final Shop seShop;
    private final SSDestroyedEventType reason;

    public SSDestroyedEvent(Block pBlock, SignShopPlayer pPlayer, Shop pShop, SSDestroyedEventType pReason) {
        ssPlayer = pPlayer;

        bBlock = pBlock;
        if (pShop != null)
            seShop = pShop;
        else if (pReason == SSDestroyedEventType.SIGN)
            seShop = Storage.get().getShop(pBlock.getLocation());
        else
            seShop = null;

        reason = pReason;
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

    public boolean hasPlayer() {
        return ssPlayer != null;
    }

    public Block getBlock() {
        return bBlock;
    }

    public Shop getShop() {
        return seShop;
    }

    public SSDestroyedEventType getReason() {
        return reason;
    }
}
