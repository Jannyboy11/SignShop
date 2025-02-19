package org.wargamer2010.signshop.events;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.bukkit.event.block.Action;

public class SSTouchShopEvent extends SSEvent {
    private static final HandlerList handlers = new HandlerList();

    private SignShopPlayer ssPlayer = null;
    private Shop seShop = null;
    private Action aAction = Action.PHYSICAL;
    private Block bBlock = null;

    public SSTouchShopEvent(SignShopPlayer pPlayer, Shop pShop, Action pAction, Block pBlock) {
        ssPlayer = pPlayer;
        seShop = pShop;
        aAction = pAction;
        bBlock = pBlock;
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

    public Shop getShop() {
        return seShop;
    }

    public Action getAction() {
        return aAction;
    }

    public Block getBlock() {
        return bBlock;
    }


}
