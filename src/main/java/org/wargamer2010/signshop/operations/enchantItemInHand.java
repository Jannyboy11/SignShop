package org.wargamer2010.signshop.operations;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.wargamer2010.signshop.SignShop;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;
import org.wargamer2010.signshop.configuration.SignShopConfig;

public class enchantItemInHand implements SignShopOperation {
    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        if(ssArgs.getContainables().isEmpty()) {
            if(ssArgs.isOperationParameter("allowNoChests"))
                return true;
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_missing", ssArgs.getMessageParts()));
            return false;
        }

        Map<Enchantment, Integer> AllEnchantments = new HashMap<Enchantment, Integer>();
        Map<Enchantment, Integer> TempEnchantments;

        for(Block bHolder : ssArgs.getContainables().get()) {
            if(bHolder.getState() instanceof InventoryHolder) {
                InventoryHolder Holder = (InventoryHolder)bHolder.getState();
                for(ItemStack item : Holder.getInventory().getContents()) {
                    if(item != null && item.getAmount() > 0) {
                        if(item.getType() == Material.ENCHANTED_BOOK && item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta) {
                            TempEnchantments = ((EnchantmentStorageMeta)item.getItemMeta()).getStoredEnchants();
                        } else {
                            TempEnchantments = item.getEnchantments();
                        }
                        
                        if(TempEnchantments.isEmpty()) continue;
                        for(Map.Entry<Enchantment, Integer> enchantment : TempEnchantments.entrySet()) {
                            if(AllEnchantments.containsKey(enchantment.getKey()) && AllEnchantments.get(enchantment.getKey()) > enchantment.getValue())
                                TempEnchantments.remove(enchantment.getKey());
                        }
                        if(!TempEnchantments.isEmpty())
                            AllEnchantments.putAll(TempEnchantments);
                    }
                }
            }
        }
        if(AllEnchantments.isEmpty()) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("enchantment_missing", ssArgs.getMessageParts()));
            return false;
        }
        ssArgs.miscSettings.put("enchantmentInHand", SignShopUtil.convertEnchantmentsToString(AllEnchantments));
        ssArgs.setMessagePart("!enchantments", ItemUtil.enchantmentsToMessageFormat(AllEnchantments));
        return true;
    }

    @Override
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
        if(ssArgs.getPlayer().get().getPlayer() == null)
            return true;
        if(!ssArgs.miscSettings.containsKey("enchantmentInHand")) {
            SignShop.log(("misc property enchantmentInHand was not found for shop @ " + SignShopUtil.convertLocationToString(ssArgs.getSign().get().getLocation())), Level.WARNING);
            return false;
        }
        Map<Enchantment, Integer> enchantments = SignShopUtil.convertStringToEnchantments(ssArgs.miscSettings.get("enchantmentInHand"));
        String enchantmentsString = ItemUtil.enchantmentsToMessageFormat(enchantments);
        ssArgs.setMessagePart("!enchantments", enchantmentsString);
        ItemStack isInHand = ssArgs.getPlayer().get().getItemInHand();
        ItemStack isBackup = ItemUtil.getBackupItemStack(isInHand);
        if(isInHand == null) {
            ssArgs.sendFailedRequirementsMessage("item_not_enchantable");
            return false;
        } else if(!ItemUtil.needsEnchantment(isBackup, enchantments)) {
            ssArgs.sendFailedRequirementsMessage("item_already_enchanted");
            return false;
        }
        
        if(!ItemUtil.safelyAddEnchantments(isBackup, enchantments)) {
            ssArgs.sendFailedRequirementsMessage("item_not_enchantable");
            return false;
        }
        return true;
    }

    @Override
    public Boolean runOperation(SignShopArguments ssArgs) {
        ItemStack isInHand = ssArgs.getPlayer().get().getItemInHand();
        Map<Enchantment, Integer> enchantments = SignShopUtil.convertStringToEnchantments(ssArgs.miscSettings.get("enchantmentInHand"));
        
        if(isInHand.getAmount() > 1) {
            ItemStack single = ItemUtil.getBackupItemStack(isInHand);
            single.setAmount(1);
            
            ItemStack[] singleStacks = new ItemStack[] { single };
            Map<Integer, ItemStack> taken = ssArgs.getPlayer().get().takePlayerItems(singleStacks);
            if(!taken.isEmpty()) {
                ssArgs.sendFailedRequirementsMessage("no_item_in_hand");
                return false;
            }
            
            boolean didEnchantment = ItemUtil.safelyAddEnchantments(single, enchantments);
            if(!didEnchantment)
                return false;
            
            Map<Integer, ItemStack> given = ssArgs.getPlayer().get().givePlayerItems(singleStacks);
            if(!given.isEmpty()) {
                ItemStack singleOriginal = ItemUtil.getBackupItemStack(isInHand);
                singleOriginal.setAmount(1);
                given = ssArgs.getPlayer().get().givePlayerItems(new ItemStack[] { singleOriginal });
                
                ssArgs.sendFailedRequirementsMessage("player_overstocked");
                
                if(!given.isEmpty()) {
                    String message = "Failed enchantment of item in hand because inventory is too full for player."
                                        + " Player: " + ssArgs.getPlayer().get().getName() 
                                        + ", Shop: (" + ssArgs.getSign().get().getX() + ", " + ssArgs.getSign().get().getY() + ")";
                    
                    SignShop.log(message, Level.WARNING);
                }

                return false;
            }
            
            return true;
        } else {
            return ItemUtil.safelyAddEnchantments(isInHand, enchantments);
        }
    }
}
