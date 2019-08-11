package org.wargamer2010.signshop.specialops;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;
import java.util.Map;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.player.PlayerIdentifier;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.*;

public class ChangeOwner implements SignShopSpecialOp {
    @Override
    public Boolean runOperation(List<Block> clickedBlocks, PlayerInteractEvent event, Boolean ranSomething) {
        Player player = event.getPlayer();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        Block shopSign = event.getClickedBlock();
        Shop shop = Storage.get().getShop(shopSign.getLocation());
        if(shop == null)
            return false;
        if(!Clicks.mClicksPerPlayerId.containsValue(player))
            return false;
        if(!ssPlayer.hasPerm("SignShop.ChangeOwner", true)) {
            ssPlayer.sendMessage(SignShopConfig.getError("no_permission_changeowner", null));
            return false;
        }
        if(!shop.isOwner(ssPlayer) && !ssPlayer.hasPerm("SignShop.ChangeOwner.Others", true)) {
            ssPlayer.sendMessage(SignShopConfig.getError("no_permission_changeowner", null));
            return false;
        }
        PlayerIdentifier newOwnerId = null;
        for(Map.Entry<PlayerIdentifier, Player> entry : Clicks.mClicksPerPlayerId.entrySet()) {
            if(entry.getValue() == player) {
                newOwnerId = entry.getKey();
                break;
            }
        }
        if(newOwnerId == null)
            return false;

        shop.setOwner(new SignShopPlayer(newOwnerId));
        Storage.get().save();
        ssPlayer.sendMessage("Succesfully changed ownership of shop to " + newOwnerId.getName());
        Clicks.mClicksPerPlayerId.remove(newOwnerId);
        return true;
    }
}
