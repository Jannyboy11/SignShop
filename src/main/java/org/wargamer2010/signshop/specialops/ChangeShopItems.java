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
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.*;

public class ChangeShopItems implements SignShopSpecialOp {

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
        if(ssPlayer.getItemInHand() == null || ssPlayer.getItemInHand().getType() != SignShopConfig.getUpdateMaterial())
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

        SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sLines[3]), null, containables, activatables,
                ssPlayer, ssOwner, bClicked, sOperation, event.getBlockFace(), event.getAction(), SignShopArgumentsType.Setup);

        Boolean bSetupOK = false;
        for (SignShopOperationListItem ssOperation : SignShopOperations) {
            ssArgs.setOperationParameters(ssOperation.getParameters());
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

        Storage.get().updateShop(bClicked, shop.getContainables(), shop.getActivatables(), ssArgs.getItems().get());

        if (!ssArgs.bDoNotClearClickmap) {
            Clicks.removePlayerFromClickMap(player);
        }

        ssPlayer.sendMessage(SignShopConfig.getError("updated_shop", ssArgs.getMessageParts()));

        return true;
    }
}
