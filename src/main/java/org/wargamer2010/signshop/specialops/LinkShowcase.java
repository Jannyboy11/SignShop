package org.wargamer2010.signshop.specialops;

//import com.kellerkindt.scs.shops.DisplayShop;
//import com.kellerkindt.scs.shops.Shop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;
import org.bukkit.Material;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.*;

import org.bukkit.Bukkit;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;

public class LinkShowcase implements SignShopSpecialOp {
    @Override
    public Boolean runOperation(List<Block> clickedBlocks, PlayerInteractEvent event, Boolean ranSomething) {
        Player player = event.getPlayer();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        Block shopSign = event.getClickedBlock();
        Shop shop = Storage.get().getShop(shopSign.getLocation());
        if(shop == null)
            return false;
        if(shop.getContainables().isEmpty())
            return false;
        if(!ItemUtil.isSign(shopSign))
            return false;

        Block bStep = null;
        for(Block bTemp : clickedBlocks) {
            if(bTemp.getType() == Material.getMaterial("STEP"))
                bStep = bTemp;
        }
        if(bStep == null)
            return false;

        ItemStack showcasing;
        if(shop.getItems() == null || shop.getItems().length == 0 || shop.getItems()[0] == null) {
            ssPlayer.sendMessage(SignShopConfig.getError(("chest_empty"), null));
            return false;
        }
        showcasing = shop.getItems()[0];

        if(Bukkit.getServer().getPluginManager().getPlugin("ShowCaseStandalone") == null)
            return false;
//        if(!PlayerIdentifier.GetUUIDSupport()) {
//            SignShop.log("No UUID support detected but ShowCaseStandalone requires it. Please downgrade ShowCaseStandalone or upgrade Bukkit.", Level.WARNING);
//            return false;
//        }

        shop.addMisc("showcaselocation", SignShopUtil.convertLocationToString(bStep.getLocation()));
        ssPlayer.sendMessage("Showcase has been successfully created.");
        return true;
    }
}
