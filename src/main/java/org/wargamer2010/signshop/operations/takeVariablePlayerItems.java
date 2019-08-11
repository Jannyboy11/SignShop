package org.wargamer2010.signshop.operations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bukkit.inventory.ItemStack;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.bukkit.Material;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.SignShopUtil;

public class takeVariablePlayerItems implements SignShopOperation {

    private boolean doDurabilityNullification(SignShopArguments ssArgs) {
        ItemStack[] inv_stacks = ssArgs.getPlayer().get().getInventoryContents();
        if(ssArgs.isOperationParameter("acceptdamaged")) {
            short nodamage = 0;
            Material mat;
            Map<ItemStack, Integer> map = ItemUtil.stacksToMap(ssArgs.getItems().get());
            if(map.size() > 1) {
                ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("damaged_items_shop_homogeneous", ssArgs.getMessageParts()));
                return false;
            }
            ItemStack arr[] = new ItemStack[1]; map.keySet().toArray(arr);
            mat = arr[0].getType();
            boolean didnull = false;

            for(ItemStack stack : inv_stacks) {
                if(stack != null && stack.getType() == mat && stack.getType().getMaxDurability() >= 30 && stack.getDurability() != nodamage) {
                    stack.setDurability(nodamage);
                    didnull = true;
                }
            }
            return didnull;
        }
        return false;
    }

    private ItemStack[] getRealItemStack(ItemStack[] playerinv, ItemStack[] actual) {
        List<ItemStack> sortedByDurability = StreamSupport.stream(Spliterators.<ItemStack>spliterator(playerinv, 0), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //TODO how to do this the appropiate way in mc 1.13+? why does it need to be sorted anyway?
        sortedByDurability.sort(Comparator.comparing(ItemStack::getDurability)); //sorts in-place because Collectors.toList() supplies an ArrayList :)

        Map<ItemStack, Integer> map = ItemUtil.stacksToMap(actual);
        ItemStack neededstack;
        int needed;
        List<ItemStack> toTakeForReal = new LinkedList<>();

        for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
            neededstack = entry.getKey();
            needed = entry.getValue();
            for (ItemStack stackFromInventory : sortedByDurability) {
                if (ItemUtil.isItemStackSimilar(stackFromInventory, neededstack, true)) {
                    ItemStack backupSingleItemStack = ItemUtil.getBackupItemStack(stackFromInventory);
                    if (backupSingleItemStack.getAmount() >= needed)
                        backupSingleItemStack.setAmount(needed);
                    toTakeForReal.add(backupSingleItemStack);
                    needed -= backupSingleItemStack.getAmount();
                    if (needed <= 0)
                        break;
                }
            }
        }

        return toTakeForReal.toArray(new ItemStack[toTakeForReal.size()]);
    }

    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        if(ssArgs.getContainables().isEmpty()) {
            if(ssArgs.isOperationParameter("allowNoChests"))
                return true;
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_missing", ssArgs.getMessageParts()));
            return false;
        }
        ItemStack[] isTotalItems = ItemUtil.getAllItemStacksForContainables(ssArgs.getContainables().get());

        if (isTotalItems.length == 0) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("chest_empty", ssArgs.getMessageParts()));
            return false;
        }

        ssArgs.getItems().set(isTotalItems);
        ssArgs.setMessagePart("!items", ItemUtil.itemStackToString(ssArgs.getItems().get()));
        return true;
    }

    @Override
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
        if (!ssArgs.isPlayerOnline())
            return true;
        if (ssArgs.getItems().get() == null) {
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("no_items_defined_for_shop", ssArgs.getMessageParts()));
            return false;
        }

        //don't call the getBackupItemStack(ItemStack[]) overload because it filters null elements - we wan't the null stacks too!
        ItemStack[] backupInventory = Arrays
                .stream(ssArgs.getPlayer().get().getInventoryContents())
                .map(ItemUtil::getBackupItemStack)
                .toArray(ItemStack[]::new);
        boolean didNullifyDurability = doDurabilityNullification(ssArgs);

        SignShopPlayer ssPlayer = ssArgs.getPlayer().get();

        ssArgs.setMessagePart("!items", ItemUtil.itemStackToString(ssArgs.getItems().get()));
        HashMap<ItemStack[], Double> variableAmount = ssPlayer.getVirtualInventory().variableAmount(ssArgs.getItems().get());
        //TODO why an arbitrary value from the map? it seems like HashMap may not have been the right type after all
        Double iCount = (Double)variableAmount.values().toArray()[0]; //TODO I suspect the Map only ever has one entry.

        //TODO why is this even needed? the other methods don't alter the player's inventory contents, do they?
        //TODO the virtual inventory shouldn't touch the player's inventory
        ssArgs.getPlayer().get().setInventoryContents(backupInventory);

        ItemStack[] isActual = (ItemStack[]) variableAmount.keySet().toArray()[0]; //TODO similar here
        double pricemod = 1.0d;
        if (didNullifyDurability) {
            ItemStack[] temp = getRealItemStack(backupInventory, isActual);
            if (temp.length > 0) {
                isActual = temp;
                pricemod = SignShopUtil.calculateDurabilityModifier(isActual);
            }
        }

        ssArgs.getItems().set(isActual);
        ssArgs.setMessagePart("!items", ItemUtil.itemStackToString(ssArgs.getItems().get()));
        if (Double.doubleToRawLongBits(iCount) == Double.doubleToRawLongBits(0d))
            ssArgs.getPrice().set(ssArgs.getPrice().get() * iCount * pricemod);
        else
            ssArgs.getPrice().set(ssArgs.getPrice().get() * pricemod);

        if (Double.doubleToRawLongBits(iCount) == Double.doubleToRawLongBits(0d)) {
            ssArgs.sendFailedRequirementsMessage("player_doesnt_have_items");
            return false;
        }
        return true;
    }

    @Override
    public Boolean runOperation(SignShopArguments ssArgs) {
        if (!checkRequirements(ssArgs, true))
            return false;

        boolean transactedAll = ssArgs.getPlayer().get().takePlayerItems(ssArgs.getItems().get()).isEmpty();
        if (!transactedAll)
            ssArgs.getPlayer().get().sendMessage(SignShopConfig.getError("could_not_complete_operation", null));

        return transactedAll;
    }

}
