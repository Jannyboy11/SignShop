package org.wargamer2010.signshop.specialops;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSCreatedEvent;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.EconomyUtil;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;

public class CopySign implements SignShopSpecialOp {

    @Override
    public Boolean runOperation(List<Block> clickedBlocks, PlayerInteractEvent event, Boolean ranSomething) {
        Player player = event.getPlayer();
        Block shopSign = event.getClickedBlock();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        if (!ItemUtil.isSign(shopSign))
            return false;
        if (!ssPlayer.hasItemInHand(SignShopConfig.getUpdateMaterial()))
            return false;

        Sign signNewSign = null;
        for (Block tempBlock : clickedBlocks) {
            if(ItemUtil.isSign(tempBlock)) {
                signNewSign = ((Sign) tempBlock.getState());
                break;
            }
        }
        if (signNewSign == null || Storage.get().getShop(signNewSign.getLocation()) != null)
            return false;

        Sign signToChange = ((Sign) shopSign.getState());
        String[] sNewSign = signNewSign.getLines();
        String[] sToChange = signToChange.getLines().clone();
        Shop shop = Storage.get().getShop(shopSign.getLocation());
        if (shop == null)
            return false;
        if ((!shop.isOwner(ssPlayer) || !ssPlayer.hasPerm("SignShop.CopyPaste", true)) && !ssPlayer.hasPerm("SignShop.CopyPaste.Others", true)) {
            ssPlayer.sendMessage(SignShopConfig.getError("no_permission", null));
            return true;
        }

        if (sNewSign[1] != null && sNewSign[1].length() > 0)
            signToChange.setLine(1, sNewSign[1]);
        if (sNewSign[2] != null && sNewSign[2].length() > 0)
            signToChange.setLine(2, sNewSign[2]);
        if (sNewSign[3] != null && sNewSign[3].length() > 0)
            signToChange.setLine(3, sNewSign[3]);
        signToChange.update();
        String price;
        if (sNewSign[3] != null && sNewSign[3].length() > 0)
            price = sNewSign[3];
        else
            price = sToChange[3];

        String sOperation;
        if (sNewSign[0] != null && sNewSign[0].length() > 0)
            sOperation = SignShopUtil.getOperation(sNewSign[0]);
        else
            sOperation = SignShopUtil.getOperation(sToChange[0]);

        if (!SignShopConfig.getBlocks(sOperation).isEmpty()) {
            List<String> operation = SignShopConfig.getBlocks(sOperation);
            if (!operation.contains("playerIsOp") && !ssPlayer.hasPerm(("SignShop.Signs."+sOperation), false)) {
                ssPlayer.sendMessage(SignShopConfig.getError("no_permission", null));
                return true;
            }
            List<SignShopOperationListItem> SignShopOperations = SignShopUtil.getSignShopOps(operation);
            if (SignShopOperations == null) {
                ssPlayer.sendMessage("The new operation does not exist!");
                revert(shopSign, sToChange);
                return true;
            }
            SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(price), shop.getItems(), shop.getContainables(), shop.getActivatables(),
                    ssPlayer, ssPlayer, shopSign, sOperation, event.getBlockFace(), event.getAction(), SignShopArgumentsType.Setup);

            Boolean bSetupOK = false;
            for (SignShopOperationListItem ssOperation : SignShopOperations) {
                ssArgs.setOperationParameters(ssOperation.getParameters());
                ssArgs.ignoreEmptyChest();
                bSetupOK = ssOperation.getOperation().setupOperation(ssArgs);
                if(!bSetupOK)
                    break;
            }
            if (!bSetupOK) {
                ssPlayer.sendMessage("The new and old operation are not compatible.");
                revert(shopSign, sToChange);
                return true;
            }

            if (!SignShopUtil.getPriceFromMoneyEvent(ssArgs)) {
                ssPlayer.sendMessage("The new and old operation are not compatible.");
                revert(shopSign, sToChange);
                return true;
            }

            SSCreatedEvent createdevent = SSEventFactory.generateCreatedEvent(ssArgs);
            SignShop.callEvent(createdevent);
            if (createdevent.isCancelled()) {
                ssPlayer.sendMessage("The new and old operation are not compatible.");
                revert(shopSign, sToChange);
                return true;
            }

            if (sNewSign[0] != null && sNewSign[0].length() > 0) {
                signToChange = ((Sign) shopSign.getState());
                signToChange.setLine(0, sNewSign[0]);
                signToChange.update();
            }
        } else {
            ssPlayer.sendMessage("The new operation does not exist!");
            revert(shopSign, sToChange);
            return true;
        }

        ItemUtil.setSignStatus(shopSign, ChatColor.DARK_BLUE);

        ssPlayer.sendMessage(SignShopConfig.getError("updated_shop", null));
        return true;
    }

    public void revert(Block bSign, String[] oldLines) {
        Sign sign = (Sign) bSign.getState();
        sign.setLine(1, oldLines[1]);
        sign.setLine(2, oldLines[2]);
        sign.setLine(3, oldLines[3]);
        sign.update();
    }
}
