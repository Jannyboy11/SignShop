package org.wargamer2010.signshop.listeners;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.Storage;
import org.wargamer2010.signshop.events.SSCreatedEvent;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.events.SSPostTransactionEvent;
import org.wargamer2010.signshop.events.SSPreTransactionEvent;
import org.wargamer2010.signshop.events.SSTouchShopEvent;
import org.wargamer2010.signshop.operations.SignShopArguments;
import org.wargamer2010.signshop.operations.SignShopArgumentsType;
import org.wargamer2010.signshop.operations.SignShopOperationListItem;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.specialops.SignShopSpecialOp;
import org.wargamer2010.signshop.util.Clicks;
import org.wargamer2010.signshop.util.EconomyUtil;
import org.wargamer2010.signshop.util.ItemUtil;
import org.wargamer2010.signshop.util.SignShopUtil;

public class SignShopPlayerListener implements Listener {
    private static final String helpPrefix = "help_";
    private static final String anyHelp = "help_anyhelp";

    private boolean runSpecialOperations(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Set<Location> lClicked = SignShopUtil.getKeysByValue(Clicks.mClicksPerLocation, player);
        boolean ranSomething = false;

        List<SignShopSpecialOp> specialops = SignShopUtil.getSignShopSpecialOps();
        List<Block> clickedBlocks = new LinkedList<>();
        for (Location lTemp : lClicked) {
            clickedBlocks.add(player.getWorld().getBlockAt(lTemp));
        }
        if (!specialops.isEmpty()) {
            for(SignShopSpecialOp special : specialops) {
                ranSomething = special.runOperation(clickedBlocks, event, ranSomething) || ranSomething;
                if (ranSomething) {
                    break;
                }
            }
            if (ranSomething) {
                Clicks.removePlayerFromClickMap(player);
            }
        }

        return ranSomething;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void SSBugFix(BlockPlaceEvent event) {
        // credits go to Cat7373 for the fix below, https://github.com/wargamer/SignShop/issues/15
        if(event.isCancelled())
            return;
        Block block = event.getBlock();

        if(ItemUtil.isSign(block)) {
            Location location = block.getLocation();

            if(Storage.get().getShop(location) != null) {
                Storage.get().removeShop(location);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerVillagerTrade(PlayerInteractEntityEvent event) {
        Entity ent = event.getRightClicked();
        SignShopPlayer ssPlayer = new SignShopPlayer(event.getPlayer());
        if (SignShopConfig.getPreventVillagerTrade() && ent.getType() == EntityType.VILLAGER) {
            if (!event.isCancelled()) {
                ssPlayer.sendMessage(SignShopConfig.getError("villager_trading_disabled", null));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() != EntityType.PLAYER) return;

        Player player = (Player) event.getDamager();
        SignShopPlayer ssPlayer = new SignShopPlayer(player);

        if (ssPlayer.getItemInHand() == null || !SignShopConfig.isOPMaterial(ssPlayer.getItemInHand().getType())) return;

        if (event.getEntity().getType() == EntityType.PLAYER) {
            SignShopPlayer clickedPlayer = new SignShopPlayer((Player)event.getEntity());

            if (Clicks.mClicksPerPlayerId.containsKey(clickedPlayer.getIdentifier())) {
                ssPlayer.sendMessage("You have deselected a player with name: " + clickedPlayer.getName());
                Clicks.mClicksPerPlayerId.remove(clickedPlayer.getIdentifier());
            } else {
                ssPlayer.sendMessage("You hit a player with name: " + clickedPlayer.getName());
                Clicks.mClicksPerPlayerId.put(clickedPlayer.getIdentifier(), player);
            }

            event.setCancelled(true);
        }
    }

    //TODO refactor or document this more. this method is not understandable.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSignChange(SignChangeEvent event) {
        if(event.getPlayer() == null || !ItemUtil.isSign(event.getBlock()))
            return;
        String[] oldLines = ((Sign) event.getBlock().getState()).getLines();
        // Prevent the message from being shown when the top line remains the same
        if (oldLines[0].equals(event.getLine(0)))
            return;

        String[] sLines = event.getLines();
        String sOperation = SignShopUtil.getOperation(sLines[0]);
        if (SignShopConfig.getBlocks(sOperation).isEmpty())
            return;

        List<String> operation = SignShopConfig.getBlocks(sOperation);
        if (SignShopUtil.getSignShopOps(operation) == null)
            return;

        SignShopPlayer ssPlayer = new SignShopPlayer(event.getPlayer());
        if (SignShopConfig.getEnableTutorialMessages()) {
            if (!ssPlayer.hasMeta(helpPrefix + sOperation.toLowerCase()) && !ssPlayer.hasMeta(anyHelp)) {
                ssPlayer.setMeta(helpPrefix + sOperation.toLowerCase(), "1");
                String[] args = new String[] {
                    sOperation
                };
                SignShop.getCommandDispatcher().handle("sign", args, ssPlayer);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Respect protection plugins
        if (!event.hasBlock() || event.useInteractedBlock() == Event.Result.DENY) return;

        // Initialize needed variables
        final Block clickedBlock = event.getClickedBlock();
        final Player player = event.getPlayer();
        final SignShopPlayer ssPlayer = new SignShopPlayer(player);
        String[] sLines;
        String sOperation;
        final World world = player.getWorld();
        final Shop shop = Storage.get().getShop(clickedBlock.getLocation());

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.hasItem() && shop == null && SignShopConfig.isOPMaterial(event.getItem().getType())) {
            if (ItemUtil.isSign(clickedBlock) && event.getItem().getType() == SignShopConfig.getLinkMaterial()) {
                //CASE: LINK
                SignShop signShop = SignShop.getInstance();
                signShop.getLogger().info("DEBUG - PlayerInterActEvent - Case: Link");

                sLines = ((Sign) clickedBlock.getState()).getLines();
                sOperation = SignShopUtil.getOperation(sLines[0]);
                if (SignShopConfig.getBlocks(sOperation).isEmpty()) {
                    if (!runSpecialOperations(event) && !SignShopUtil.registerClickedMaterial(event)) {
                        ssPlayer.sendMessage(SignShopConfig.getError("invalid_operation", null));
                    }
                    return;
                }

                List<String> operation = SignShopConfig.getBlocks(sOperation);
                List<SignShopOperationListItem> SignShopOperations = SignShopUtil.getSignShopOps(operation);
                if (SignShopOperations == null) {
                    ssPlayer.sendMessage(SignShopConfig.getError("invalid_operation", null));
                    return;
                }

                event.setCancelled(true);

                List<Block> containables = new LinkedList<>();
                List<Block> activatables = new LinkedList<>();
                boolean wentOK = SignShopUtil.getSignshopBlocksFromList(ssPlayer, containables, activatables, clickedBlock);
                if (!wentOK) {
                    return;
                }

                SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sLines[3]), null, containables, activatables,
                        ssPlayer, ssPlayer, clickedBlock, sOperation, event.getBlockFace(), event.getAction(), SignShopArgumentsType.Setup);

                Boolean bSetupOK = false;
                for (SignShopOperationListItem ssOperation : SignShopOperations) {
                    ssArgs.setOperationParameters(ssOperation.getParameters());
                    bSetupOK = ssOperation.getOperation().setupOperation(ssArgs);
                    if (!bSetupOK)
                        return;
                }

                if (!bSetupOK) {
                    return;
                }

                if (!SignShopUtil.getPriceFromMoneyEvent(ssArgs)) {
                    return;
                }

                SSCreatedEvent createdEvent = SSEventFactory.generateCreatedEvent(ssArgs);
                SignShop.callEvent(createdEvent);
                if (createdEvent.isCancelled()) {
                    ItemUtil.setSignStatus(clickedBlock, ChatColor.BLACK);
                    return;
                }

                Storage.get().addShop(
                        ssPlayer.getIdentifier(),
                        world.getName(),
                        ssArgs.getSign().get(),
                        ssArgs.getContainables().getInner(),
                        ssArgs.getActivatables().getInner(),
                        ssArgs.getItems().get(),
                        createdEvent.getMiscSettings());

                if (!ssArgs.bDoNotClearClickmap) {
                    Clicks.removePlayerFromClickMap(player);
                }

                return;
            }
            SignShopUtil.registerClickedMaterial(event);
        } else if (ItemUtil.isSign(clickedBlock)
                && shop != null
                && (event.getItem() == null || !SignShopConfig.isOPMaterial(event.getItem().getType()))) {
            //CASE: SELL

            SignShopPlayer ssOwner = shop.getOwner();
            sLines = ((Sign) clickedBlock.getState()).getLines();
            sOperation = SignShopUtil.getOperation(sLines[0]);

            // Verify the operation
            if (SignShopConfig.getBlocks(sOperation).isEmpty()){
                return;
            }

            List<String> operation = SignShopConfig.getBlocks(sOperation);

            List<SignShopOperationListItem> signShopOperations = SignShopUtil.getSignShopOps(operation);
            if (signShopOperations == null) {
                ssPlayer.sendMessage(SignShopConfig.getError("invalid_operation", null));
                return;
            }

            for (Block bContainable : shop.getContainables())
                ItemUtil.loadChunkByBlock(bContainable);
            for (Block bActivatable : shop.getActivatables())
                ItemUtil.loadChunkByBlock(bActivatable);

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
                event.setCancelled(true);
            }

            SignShopArguments ssArgs = new SignShopArguments(EconomyUtil.parsePrice(sLines[3]), shop.getItems(), shop.getContainables(), shop.getActivatables(),
                                                                ssPlayer, ssOwner, clickedBlock, sOperation, event.getBlockFace(), event.getAction(), SignShopArgumentsType.Check);

            if (shop.getRawMisc() != null)
                ssArgs.miscSettings = shop.getRawMisc();
            boolean bRequirementsOK = true;
            boolean bReqOKSolid = true;
            boolean bRunOK = false;

            // If left clicking, all blocks should get a chance to run checkRequirements
            for (SignShopOperationListItem ssOperation : signShopOperations) {
                ssArgs.setOperationParameters(ssOperation.getParameters());
                bRequirementsOK = ssOperation.getOperation().checkRequirements(ssArgs, true);
                if (!ssArgs.isLeftClicking() && !bRequirementsOK)
                    break;
                else if (!bRequirementsOK)
                    bReqOKSolid = false;
            }

            if (!bReqOKSolid)
                bRequirementsOK = false;

            SSPreTransactionEvent pretransactevent = SSEventFactory.generatePreTransactionEvent(ssArgs, shop, event.getAction(), bRequirementsOK);
            SignShop.callEvent(pretransactevent);

            // Skip the requirements check if we're left clicking
            // The confirm message should always be shown when left clicking
            if (!ssArgs.isLeftClicking() && (!bRequirementsOK || pretransactevent.isCancelled()))
                return;

            ssArgs.setArgumentType(SignShopArgumentsType.Run);
            ssArgs.getPrice().set(pretransactevent.getPrice());

            if (ssArgs.isLeftClicking()) {
                ssPlayer.sendMessage(SignShopConfig.getMessage("confirm", ssArgs.getOperation().get(), ssArgs.getMessageParts()));

                ssArgs.reset();
                return;
            }
            ssArgs.reset();

            for (SignShopOperationListItem ssOperation : signShopOperations) {
                ssArgs.setOperationParameters(ssOperation.getParameters());
                bRunOK = ssOperation.getOperation().runOperation(ssArgs);
                if (!bRunOK)
                    return;
            }
            if (!bRunOK)
                return;

            SSPostTransactionEvent postTransactionEvent = SSEventFactory.generatePostTransactionEvent(ssArgs, shop, event.getAction());
            SignShop.callEvent(postTransactionEvent);
            if (postTransactionEvent.isCancelled())
                return;

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Seems to still be needed.
                player.updateInventory();
            }

            List<String> chests = new LinkedList<>();
            for (Map.Entry<String, String> entry : ssArgs.getMessageParts().entrySet())
                if (entry.getKey().contains("chest"))
                    chests.add(entry.getValue());
            String[] sChests = chests.toArray(new String[chests.size()]);
            String items = !ssArgs.hasMessagePart("!items")
                    ? SignShopUtil.implode(sChests, " and ")
                    : ssArgs.getMessagePart("!items");
            SignShop.logTransaction(player.getName(), shop.getOwner().getName(), sOperation, items, EconomyUtil.formatMoney(ssArgs.getPrice().get()));
            return;
        }

        if (event.getItem() != null && shop != null && SignShopConfig.isOPMaterial(event.getItem().getType())) {
            if (!runSpecialOperations(event)) {
                SignShopUtil.registerClickedMaterial(event);
            }
        }

        Set<Shop> touchedShops = Storage.get().getShopsByBlock(clickedBlock);
        if (!touchedShops.isEmpty()) {
            for (Shop touchedShop : touchedShops) {
                SSTouchShopEvent touchShopEvent = new SSTouchShopEvent(ssPlayer, touchedShop, event.getAction(), clickedBlock);

                SignShop.callEvent(touchShopEvent);
                if (touchShopEvent.isCancelled()) {
                    event.setCancelled(true);
                    SignShopArguments ssArgs = new SignShopArguments(touchedShop, ssPlayer, SignShopArgumentsType.Check);

                    ssPlayer.sendMessage(SignShopConfig.getError("touch_notallowed", ssArgs.getMessageParts()));
                    break;
                }
            }
        }
    }

}