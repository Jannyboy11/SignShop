package org.wargamer2010.signshop.operations;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import java.util.HashMap;

public class giveShopItems implements SignShopOperation {
    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        if(ssArgs.getContainables().isEmpty()) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_missing", ssArgs.getMessageParts()));
            return false;
        }
        ItemStack[] isTotalItems = ItemUtil.getAllItemStacksForContainables(ssArgs.getContainables().get());

        if(!ssArgs.isOperationParameter("allowemptychest") && isTotalItems.length == 0) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_empty", ssArgs.getMessageParts()));
            return false;
        }
        if(isTotalItems.length > 0)
            ssArgs.getItems().set(isTotalItems);
        ssArgs.setMessagePart("!items", ItemUtil.itemStackToString(ssArgs.getItems().get()));
        return true;
    }

    @Override
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
        if(!ssArgs.isOperationParameter("allowemptychest") && ssArgs.getItems().get() == null) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("no_items_defined_for_shop", ssArgs.getMessageParts()));
            return false;
        }

        Boolean bStockOK = ItemUtil.stockOKForContainables(ssArgs.getContainables().get(), ssArgs.getItems().get(), false);
        if(!bStockOK)
            ssArgs.sendFailedRequirementsMessage("overstocked");
        if(activeCheck && !bStockOK)
            ItemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_RED);
        else if(activeCheck)
            ItemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_BLUE);
        ssArgs.setMessagePart("!items", ItemUtil.itemStackToString(ssArgs.getItems().get()));
        return bStockOK;
    }

    @Override
    public Boolean runOperation(SignShopArguments ssArgs) {
        InventoryHolder Holder = ItemUtil.getFirstStockOKForContainables(ssArgs.getContainables().get(), ssArgs.getItems().get(), false);
        if(Holder == null)
            return false;
        HashMap<Integer, ItemStack> isLeftOver = Holder.getInventory().addItem(ssArgs.getItems().get());
        if(!ItemUtil.stockOKForContainables(ssArgs.getContainables().get(), ssArgs.getItems().get(), false))
            ItemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_RED);
        else
            ItemUtil.updateStockStatus(ssArgs.getSign().get(), ChatColor.DARK_BLUE);
        return (isLeftOver.isEmpty());
    }
}
