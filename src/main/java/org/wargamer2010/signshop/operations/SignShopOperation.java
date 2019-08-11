package org.wargamer2010.signshop.operations;

//TODO use primitive booleans
public interface SignShopOperation {
    public Boolean setupOperation(SignShopArguments ssArgs);
    
    public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck);
    
    public Boolean runOperation(SignShopArguments ssArgs);
}
