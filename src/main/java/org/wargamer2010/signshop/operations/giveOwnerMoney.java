package org.wargamer2010.signshop.operations;

import org.wargamer2010.signshop.util.EconomyUtil;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.events.SSEventFactory;
import org.wargamer2010.signshop.events.SSMoneyEventType;
import org.wargamer2010.signshop.events.SSMoneyRequestType;
import org.wargamer2010.signshop.events.SSMoneyTransactionEvent;

public class giveOwnerMoney implements SignShopOperation {
    @Override
    public Boolean setupOperation(SignShopArguments ssArgs) {
        ssArgs.setMoneyEventType(SSMoneyEventType.GiveToOwner);
        ssArgs.setMessagePart("!price", EconomyUtil.formatMoney(ssArgs.getPrice().get()));
        return true;
    }

    @Override
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
        SSMoneyTransactionEvent event = SSEventFactory.generateMoneyEvent(ssArgs, SSMoneyEventType.GiveToOwner, SSMoneyRequestType.CheckBalance);
        SignShop.callEvent(event);
        ssArgs.getPrice().set(event.getPrice());
        return (!event.isCancelled() && event.isHandled());
    }

    @Override
    public Boolean runOperation(SignShopArguments ssArgs) {
        SSMoneyTransactionEvent event = SSEventFactory.generateMoneyEvent(ssArgs, SSMoneyEventType.GiveToOwner, SSMoneyRequestType.ExecuteTransaction);
        SignShop.callEvent(event);
        ssArgs.getPrice().set(event.getPrice());
        return (!event.isCancelled() && event.isHandled());
    }
}
