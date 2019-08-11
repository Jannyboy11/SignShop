package org.wargamer2010.signshop.specialops;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Sign;
import java.util.List;
import java.util.LinkedList;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSCreatedEvent;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.events.SSLinkEvent;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.*;

public class LinkAdditionalBlocks implements SignShopSpecialOp {

    private List<Block> updateList(final List<Block> masterBlocks, final List<Block> newBlocks, final SignShopPlayer ssPlayer, final Shop pShop) {
        List<Block> updatedList = newBlocks;
        for (Block masterBlock : masterBlocks) {
            if (newBlocks.contains(masterBlock)) {
                ssPlayer.sendMessage("Attempting to unlink " + ItemUtil.formatData(masterBlock.getBlockData()) + " from shop.");
                updatedList.remove(masterBlock);
            } else {
                updatedList.add(masterBlock);
            }
        }
        for (Block newBlock : newBlocks) {
            if (!masterBlocks.contains(newBlock)) {
                //TODO debug this?!
                SSLinkEvent event = SSEventFactory.generateLinkEvent(newBlock, ssPlayer, pShop);
                SignShop.callEvent(event);
                if(event.isCancelled()) {
                    ssPlayer.sendMessage("You are not allowed to link this " + ItemUtil.formatData(newBlock.getBlockData()) + " to the shop.");
                    updatedList.remove(newBlock);
                } else {
                    ssPlayer.sendMessage("Attempting to link " + ItemUtil.formatData(newBlock.getBlockData()) + " to the shop.");
                }

            }
        }
        return updatedList;
    }

    @Override
    public Boolean runOperation(List<Block> clickedBlocks, PlayerInteractEvent event, Boolean ranSomething) {
        if(ranSomething)
            return false;
        Player player = event.getPlayer();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        Block bClicked = event.getClickedBlock();
        Shop shop = Storage.get().getShop(bClicked.getLocation());
        String sOperation = SignShopUtil.getOperation(((Sign) bClicked.getState()).getLine(0));
        if(shop == null)
            return false;
        if(ssPlayer.getItemInHand() == null || ssPlayer.getItemInHand().getType() != SignShopConfig.getLinkMaterial())
            return false;
        SignShopPlayer ssOwner = shop.getOwner();
        List<String> operation = SignShopConfig.getBlocks(sOperation);
        String[] sLines = ((Sign) bClicked.getState()).getLines();

        if (!shop.isOwner(ssPlayer) && !ssPlayer.isOp()) {
            ssPlayer.sendMessage(SignShopConfig.getError("no_permission", null));
            return true;
        }

        List<Block> containables = new LinkedList<Block>();
        List<Block> activatables = new LinkedList<Block>();
        Boolean wentOK = SignShopUtil.getSignshopBlocksFromList(ssPlayer, containables, activatables, event.getClickedBlock());
        if (!wentOK)
            return false;
        if(containables.isEmpty() && activatables.isEmpty())
            return false;

        List<SignShopOperationListItem> SignShopOperations = SignShopUtil.getSignShopOps(operation);
        if (SignShopOperations == null) {
            ssPlayer.sendMessage(SignShopConfig.getError("invalid_operation", null));
            return false;
        }

        containables = this.updateList(shop.getContainables(), containables, ssPlayer, shop);
        activatables = this.updateList(shop.getActivatables(), activatables, ssPlayer, shop);

        SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sLines[3]), shop.getItems(), containables, activatables,
                ssPlayer, ssOwner, bClicked, sOperation, event.getBlockFace(), event.getAction(), SignShopArgumentsType.Setup);

        Boolean bSetupOK = false;
        for (SignShopOperationListItem ssOperation : SignShopOperations) {
            List<String> params = ssOperation.getParameters();
            params.addAll(ssOperation.getParameters());
            params.add("allowemptychest");
            params.add("allowNoChests");
            ssArgs.setOperationParameters(params);

            bSetupOK = ssOperation.getOperation().setupOperation(ssArgs);
            if (!bSetupOK) {
                ssPlayer.sendMessage(SignShopConfig.getError("failed_to_update_shop", ssArgs.getMessageParts()));
                return true;
            }
        }
        if (!bSetupOK) {
            ssPlayer.sendMessage(SignShopConfig.getError("failed_to_update_shop", ssArgs.getMessageParts()));
            return true;
        }

        if(!SignShopUtil.getPriceFromMoneyEvent(ssArgs)) {
            ssPlayer.sendMessage(SignShopConfig.getError("failed_to_update_shop", ssArgs.getMessageParts()));
            return true;
        }

        SSCreatedEvent createdevent = SSEventFactory.generateCreatedEvent(ssArgs);
        SignShop.callEvent(createdevent);
        if(createdevent.isCancelled()) {
            ssPlayer.sendMessage(SignShopConfig.getError("failed_to_update_shop", ssArgs.getMessageParts()));
            return true;
        }

        Storage.get().updateShop(bClicked, containables, activatables, ssArgs.getItems().getInner());

        if (!ssArgs.bDoNotClearClickmap) {
            Clicks.removePlayerFromClickMap(player);
        }

        ssPlayer.sendMessage(SignShopConfig.getError("updated_shop", ssArgs.getMessageParts()));

        return true;
    }
}
