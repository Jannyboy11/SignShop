
package org.wargamer2010.signshop.events;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.timing.IExpirable;

import java.util.Set;

public class SSEventFactory {

    private SSEventFactory() {

    }

    public static SSCreatedEvent generateCreatedEvent(SignShopArguments ssArgs) {
        return new SSCreatedEvent(ssArgs.getPrice().get(),
                                                            ssArgs.getItems().get(),
                                                            ssArgs.getContainables().getInner(),
                                                            ssArgs.getActivatables().getInner(),
                                                            ssArgs.getPlayer().get(),
                                                            ssArgs.getSign().get(),
                                                            ssArgs.getOperation().get(),
                                                            ssArgs.getRawMessageParts(),
                                                            ssArgs.miscSettings);
    }

    public static SSPreTransactionEvent generatePreTransactionEvent(SignShopArguments ssArgs, Shop pShop, Action pAction, boolean pRequirementsOK) {
        return new SSPreTransactionEvent(ssArgs.getPrice().get(),
                                                            ssArgs.getItems().get(),
                                                            ssArgs.getContainables().getInner(),
                                                            ssArgs.getActivatables().getInner(),
                                                            ssArgs.getPlayer().get(),
                                                            ssArgs.getOwner().get(),
                                                            ssArgs.getSign().get(),
                                                            ssArgs.getOperation().get(),
                                                            ssArgs.getRawMessageParts(),
                pShop,
                                                            pAction,
                                                            pRequirementsOK);
    }

    public static SSPostTransactionEvent generatePostTransactionEvent(SignShopArguments ssArgs, Shop pShop, Action pAction) {
        return new SSPostTransactionEvent(ssArgs.getPrice().get(),
                                                            ssArgs.getItems().get(),
                                                            ssArgs.getContainables().getInner(),
                                                            ssArgs.getActivatables().getInner(),
                                                            ssArgs.getPlayer().get(),
                                                            ssArgs.getOwner().get(),
                                                            ssArgs.getSign().get(),
                                                            ssArgs.getOperation().get(),
                                                            ssArgs.getRawMessageParts(),
                pShop,
                                                            pAction,
                                                            true);
    }

//    public static SSTouchShopEvent generateTouchShopEvent(SignShopPlayer pPlayer, Shop pShop, Action pAction, Block pBlock) {
//        return new SSTouchShopEvent(pPlayer, pShop, pAction, pBlock);
//    }
//
//    public static SSDestroyedEvent generateDestroyedEvent(Block pSign, SignShopPlayer pPlayer, Shop pShop, SSDestroyedEventType pReason) {
//        return new SSDestroyedEvent(pSign, pPlayer, pShop, pReason);
//    }

    public static SSLinkEvent generateLinkEvent(Block pSign, SignShopPlayer pPlayer, Shop pShop) {
        return new SSLinkEvent(pSign, pPlayer, pShop);
    }

    public static SSExpiredEvent generateExpiredEvent(IExpirable pExpirable) {
        return new SSExpiredEvent(pExpirable);
    }

    public static SSMoneyTransactionEvent generateMoneyEvent(SignShopArguments ssArgs, SSMoneyEventType type, SSMoneyRequestType pRequestType) {
        SSMoneyTransactionEvent event = new SSMoneyTransactionEvent(ssArgs.getPlayer().get(),
                                            Storage.get().getShop(ssArgs.getSign().get().getLocation()),
                                            ssArgs.getPrice().get(),
                                            ssArgs.getSign().get(),
                                            ssArgs.getOperation().get(),
                                            ssArgs.getItems().get(),
                                            ssArgs.isLeftClicking(),
                                            type,
                                            ssArgs.getRawMessageParts(),
                                            pRequestType);
        event.setArguments(ssArgs);
        return event;
    }

    public static boolean callItemMoveEvent(Set<Shop> shops, BlockInventoryHolder container, SSItemMoveEvent.Direction direction, ItemStack item, boolean cancelledInitially) {
        boolean cancel = cancelledInitially;

        for (Shop shop : shops) {
            SSItemMoveEvent event = new SSItemMoveEvent(shop, container, direction, item);
            event.setCancelled(cancel);
            Bukkit.getPluginManager().callEvent(event);
            cancel = event.isCancelled();
        }

        return cancel;
    }

}
