package org.wargamer2010.signshop.specialops;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Sign;
import java.util.List;
import java.util.LinkedList;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.*;

public class LinkSpecialSign implements SignShopSpecialOp {
    @Override
    public Boolean runOperation(List<Block> clickedBlocks, PlayerInteractEvent event, Boolean ranSomething) {
        Block shopSign = event.getClickedBlock();
        if(!ItemUtil.isSign(shopSign))
            return false;
        Player player = event.getPlayer();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        Shop shop = Storage.get().getShop(shopSign.getLocation());
        String sOperation = SignShopUtil.getOperation(((Sign)shopSign.getState()).getLine(0));
        if(shop == null)
            return false;
        if(ssPlayer.getItemInHand() == null || ssPlayer.getItemInHand().getType() != SignShopConfig.getLinkMaterial() || !ItemUtil.isSign(shopSign))
            return false;
        if(isSupported(sOperation)) // Can't link a special sign to a special sign
            return false;

        // TODO: May have to consider working with multiple Special sign types at the same time
        // For now, let's support linking/unlinking a single special sign type at a time
        String SignName = null;
        for(Block bTemp : clickedBlocks) {
            if(ItemUtil.isSign(bTemp)) {
                Sign sign = (Sign)bTemp.getState();
                if(isSupported(SignShopUtil.getOperation(sign.getLine(0)))) {
                    SignName = SignShopUtil.getOperation(sign.getLine(0));
                }
            }
        }
        if(SignName == null)
            return false;

        String MiscSetting = ("**op**signs").replace("**op**", SignName);
        String UnlinkedMessage = ("unlinked_**op**_sign").replace("**op**", SignName);
        String LinkedMessage = ("linked_**op**_sign").replace("**op**", SignName);
        String NotAllowedMessage = ("not_allowed_to_link_**op**signs").replace("**op**", SignName);

        List<Block> newSigns = new LinkedList<Block>();

        List<Block> currentSigns = SignShopUtil.getSignsFromMisc(shop, MiscSetting);
        Boolean bUnlinked = false;
        for(Block bTemp : clickedBlocks) {
            if(currentSigns.contains(bTemp)) {
                ssPlayer.sendMessage(SignShopConfig.getError(UnlinkedMessage, null));
                bUnlinked = true;
                currentSigns.remove(bTemp);
            } else if(ItemUtil.isSign(bTemp)) {
                Sign sign = (Sign)bTemp.getState();
                if(SignShopUtil.getOperation(sign.getLine(0)).equals(SignName))
                    newSigns.add(bTemp);
            }
        }

        if((bUnlinked && newSigns.isEmpty()) || !newSigns.isEmpty()) {
            if(!shop.isOwner(ssPlayer) && !ssPlayer.isOp()) {
                ssPlayer.sendMessage(SignShopConfig.getError(NotAllowedMessage, null));
                return true;
            }
        }


        if(!bUnlinked && newSigns.isEmpty())
            return false;
        else if(newSigns.isEmpty()) {
            shop.removeMisc(MiscSetting);
            Storage.get().save();
            return true;
        }
        newSigns.addAll(currentSigns);

        String locations;
        if(SignName.equals("restricted"))
            locations = SignShopUtil.validateRestrictSign(newSigns, ssPlayer);
        else if(SignName.equals("share"))
            locations = SignShopUtil.validateShareSign(newSigns, ssPlayer);
        else if(SignName.equals("bank"))
            locations = SignShopUtil.validateBankSign(newSigns, ssPlayer);
        else
            return false;

        if(locations.isEmpty())
            return true;
        else {
            ssPlayer.sendMessage(SignShopConfig.getError(LinkedMessage, null));
            shop.addMisc(MiscSetting, locations);
            Storage.get().save();
        }

        return true;
    }

    private boolean isSupported(String name) {
        return (name.equals("restricted") || name.equals("share") || name.equals("bank"));
    }
}
