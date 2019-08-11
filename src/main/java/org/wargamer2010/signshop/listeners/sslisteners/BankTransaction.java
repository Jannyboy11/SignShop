package org.wargamer2010.signshop.listeners.sslisteners;

import java.util.LinkedList;
import java.util.List;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.wargamer2010.signshop.Shop;
import org.wargamer2010.signshop.Vault;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.events.SSMoneyRequestType;
import org.wargamer2010.signshop.events.SSMoneyTransactionEvent;
import org.wargamer2010.signshop.player.SignShopPlayer;
import org.wargamer2010.signshop.util.ItemUtil;
import static org.wargamer2010.signshop.util.SignShopUtil.getSignsFromMisc;

public class BankTransaction implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSSMoneyTransaction(SSMoneyTransactionEvent event) {
        if(event.isHandled() || event.isCancelled() || event.getPlayer() == null || event.getPlayer().getPlayer() == null)
            return;
        if(event.getShop() == null || !event.getShop().hasMisc("banksigns"))
            return;
        if(!event.isBalanceOrExecution())
            return;

        if(!Vault.getEconomy().hasBankSupport()) {
            event.getPlayer().sendMessage("The current Economy plugin offers no Bank support!");
            return;
        }

        List<String> banks = getBanks(event.getShop());
        if(banks.isEmpty())
            return;

        List<String> ownedBanks = new LinkedList<String>();
        SignShopPlayer ssOwner = event.getShop().getOwner();
        for(String bank : banks) {
            String owner = event.getShop().getOwner().getName();
            if(Vault.getEconomy().isBankOwner(bank, owner).transactionSuccess() || Vault.getEconomy().isBankMember(bank, owner).transactionSuccess()
                    || ssOwner.isOp(event.getPlayer().getWorld())) {
                ownedBanks.add(bank);
            } else {
                event.setMessagePart("!bank", bank);
                ssOwner.sendMessage(SignShopConfig.getError("not_allowed_to_use_bank", event.getMessageParts()));
            }
        }

        if(ownedBanks.isEmpty()) {
            event.setHandled(true);
            event.setCancelled(true);
            event.getPlayer().sendMessage(SignShopConfig.getError("no_bank_available", event.getMessageParts()));
            return;
        }


        if(event.getRequestType() == SSMoneyRequestType.CheckBalance) {
            switch(event.getTransactionType()) {
                case GiveToOwner:
                    // Does a Bank have a money cap?
                break;
                case TakeFromOwner:
                    boolean hasTheMoney = true;
                    for(String bank : banks) {
                        EconomyResponse response = Vault.getEconomy().bankHas(bank, event.getPrice());
                        if(response.transactionSuccess()) {
                            hasTheMoney = true;
                            break;
                        }
                    }
                    if(!hasTheMoney)
                        event.setCancelled(true);
                break;
                case GiveToPlayer:
                    return;
                case TakeFromPlayer:
                    return;
                case Unknown:
                    return;
            }
        } else {
            boolean bTransaction = false;

            switch(event.getTransactionType()) {
                case GiveToOwner:
                    for(String bank : banks) {
                        EconomyResponse response = Vault.getEconomy().bankDeposit(bank, event.getPrice());
                        if(response.transactionSuccess()) {
                            bTransaction = true;
                            break;
                        }
                    }
                break;
                case TakeFromOwner:
                    for(String bank : banks) {
                        EconomyResponse response = Vault.getEconomy().bankHas(bank, event.getPrice());
                        if(response.transactionSuccess()) {
                            EconomyResponse second = Vault.getEconomy().bankWithdraw(bank, event.getPrice());
                            if(second.transactionSuccess()) {
                                bTransaction = true;
                                break;
                            }
                        }
                    }
                break;
                case GiveToPlayer:
                    return;
                case TakeFromPlayer:
                    return;
                case Unknown:
                    return;
            }

            if(!bTransaction) {
                event.getPlayer().sendMessage("The money transaction failed, please contact the System Administrator");
                event.setCancelled(true);
            }
        }

        event.setHandled(true);
    }

    private static List<String> getBanks(Shop shop) {
        List<Block> bankSigns = getSignsFromMisc(shop, "banksigns");
        List<String> banks = new LinkedList<String>();
        if(!bankSigns.isEmpty()) {
            for(Block banksign : bankSigns) {
                if(ItemUtil.isSign(banksign)) {
                    Sign sign = (Sign) banksign.getState();
                    if(sign.getLine(1) != null)
                        banks.add(sign.getLine(1));
                }
            }
        }
        return banks;
    }
}
