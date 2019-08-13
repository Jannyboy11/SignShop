
package org.wargamer2010.signshop.listeners.sslisteners;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.*;
import org.wargamer2010.signshop.hooks.HookManager;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.EconomyUtil;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;

public class SimpleShopProtector implements Listener {

    private boolean canDestroy(Player player, Block block) {
        if (player == null || block == null) return false;

        SignShopPlayer ssPlayer = new SignShopPlayer(player);
        if (ItemUtil.isSign(block)) {
            Shop shop = Storage.get().getShop(block.getLocation());
            return shop == null || shop.isOwner(ssPlayer) || SignShopPlayer.isOp(player) || !SignShopConfig.getEnableShopOwnerProtection();
        }
        return true;
    }

    private void cleanUpMiscStuff(String miscname, Block block) {
        List<Block> shopsWithSharesign = Storage.get().getShopsWithMiscSetting(miscname, SignShopUtil.convertLocationToString(block.getLocation()));
        for (Block bTemp : shopsWithSharesign) {
            Shop shop = Storage.get().getShop(bTemp.getLocation());
            String temp = shop.getMisc(miscname);
            temp = temp.replace(SignShopUtil.convertLocationToString(block.getLocation()), "");
            temp = temp.replace(SignShopArguments.seperator + SignShopArguments.seperator, SignShopArguments.seperator);
            if (temp.length() > 0) {
                if (temp.endsWith(SignShopArguments.seperator))
                    temp = temp.substring(0, temp.length() - 1);
                if (temp.length() > 1 && temp.charAt(0) == SignShopArguments.seperator.charAt(0))
                    temp = temp.substring(1, temp.length());
            }
            if (temp.length() == 0)
                shop.removeMisc(miscname);
            else
                shop.addMisc(miscname, temp);
        }
    }

    private boolean isShopBrokenAfterBlockUnlink(Shop shop, Block toUnlink) {
        List<Block> containables = new LinkedList<>();
        containables.addAll(shop.getContainables());
        List<Block> activatables = new LinkedList<>();
        activatables.addAll(shop.getActivatables());

        if (containables.contains(toUnlink))
            containables.remove(toUnlink);
        if (activatables.contains(toUnlink))
            activatables.remove(toUnlink);

        SignShopPlayer ssPlayer = new SignShopPlayer(shop.getOwner().getIdentifier());
        ssPlayer.setIgnoreMessages(true);

        if (!(shop.getSign().getState() instanceof Sign))
            return true;
        Sign sign = (Sign) shop.getSign().getState();

        SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sign.getLines()[3]), shop.getItems(), containables, activatables,
                ssPlayer, shop.getOwner(), shop.getSign(), shop.getOperation(), BlockFace.DOWN, Action.LEFT_CLICK_BLOCK, SignShopArgumentsType.Setup);

        List<String> operation = SignShopConfig.getIndividualOperations(shop.getOperation());
        List<SignShopOperationListItem> SignShopOperations = SignShopUtil.getSignShopOps(operation);
        if (SignShopOperations == null)
            return true;

        Boolean bSetupOK = false;
        for (SignShopOperationListItem ssOperation : SignShopOperations) {
            List<String> params = ssOperation.getParameters();
            params.addAll(ssOperation.getParameters());
            params.add("allowemptychest");
            params.add("allowNoChests");
            ssArgs.setOperationParameters(params);
            bSetupOK = ssOperation.getOperation().setupOperation(ssArgs);
            if (!bSetupOK) {
                return true;
            }
        }
        if (!bSetupOK) {
            return true;
        }

        SSCreatedEvent createdevent = SSEventFactory.generateCreatedEvent(ssArgs);
        SignShop.callEvent(createdevent);
        if (createdevent.isCancelled()) {
            return true;
        }

        Storage.get().updateShop(shop.getSign(), containables, activatables);
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSSDestroyEvent(SSDestroyedEvent event) {
        if (event.isCancelled() || !event.canBeCancelled())
            return;

        // Check if the shop is being destroyed by something other than a player
        // If that is the case, we'd like to cancel it as shops shouldn't burn to the ground
        SignShopPlayer ssPlayer = event.getPlayer();
        if (ssPlayer == null || ssPlayer.getPlayer() == null) {
            event.setCancelled(true);
            return;
        }

        if (ssPlayer.getPlayer().getGameMode() == GameMode.CREATIVE
                && SignShopConfig.getProtectShopsInCreative()
                && !ssPlayer.hasItemInHand(SignShopConfig.getDestroyMaterial())) {
            event.setCancelled(true);

            if (event.getShop().isOwner(ssPlayer) || ssPlayer.isOp()) {
                Map<String, String> temp = new LinkedHashMap<>();
                temp.put("!destroymaterial", SignShopUtil.capFirstLetter(SignShopConfig.getDestroyMaterial().name().toLowerCase()));

                if (event.hasPlayer()) {
                    event.getPlayer().sendMessage(SignShopConfig.getError("use_item_to_destroy_shop", temp));
                }
            }

            return;
        }

        boolean bCanDestroy = canDestroy(ssPlayer.getPlayer(), event.getBlock());
        if (!bCanDestroy) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSSDestroyCleanup(SSDestroyedEvent event) {
        if (event.isCancelled())
            return;

        if (event.getReason() == SSDestroyedEventType.SIGN) {
            if (event.getShop() != null && event.getShop().getSign() != null)
                ItemUtil.setSignStatus(event.getShop().getSign(), ChatColor.BLACK);
            Storage.get().removeShop(event.getBlock().getLocation());
        } else if (event.getReason() == SSDestroyedEventType.MISC_BLOCK) {
            cleanUpMiscStuff("sharesigns", event.getBlock());
            cleanUpMiscStuff("restrictedsigns", event.getBlock());
        } else if (event.getReason() == SSDestroyedEventType.ATTACHABLE) {
            // More shops might be attached to this attachable, but the event will be fired multiple times
            if (isShopBrokenAfterBlockUnlink(event.getShop(), event.getBlock())) {
                // Shop can not be updated without breaking functionality
                ItemUtil.setSignStatus(event.getShop().getSign(), ChatColor.BLACK);
                Storage.get().removeShop(event.getShop().getSignLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSSCreatedEvent(SSCreatedEvent event) {
        if (event.isCancelled() || !SignShopConfig.getEnableAutomaticLock())
            return;

        for (Block containable : event.getContainables()) {
            if (HookManager.protectBlock(event.getPlayer().getPlayer(), containable))
                event.getPlayer().sendMessage(SignShopConfig.getError("shop_is_now_protected", event.getMessageParts()));
        }
    }

    @EventHandler
    public void onSSItemMove(SSItemMoveEvent event) {
        //TODO make this configurable?! allow hoppers/droppers to be marked as allowed transfer blocks?
        //TODO can even customize this using permissions since the owner of a shop is a player
        event.setCancelled(true);
    }

    @EventHandler
    public void onSSShopTouch(SSTouchShopEvent event) {
        Shop touchedShop = event.getShop();
        SignShopPlayer ssPlayer = event.getPlayer();

        //cancel by default if the toucher is not the owner nor an admin
        if (!touchedShop.isOwner(ssPlayer) && !ssPlayer.hasPerm("SignShop.TouchShop", true)) {
            event.setCancelled(true);
        }
    }
}
