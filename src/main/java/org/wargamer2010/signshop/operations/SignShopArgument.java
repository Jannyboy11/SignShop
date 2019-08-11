package org.wargamer2010.signshop.operations;

import java.util.Collection;

public class SignShopArgument<E> {
    private E inner;
    private E special;
    private boolean bSpecial = false;
    private SignShopArguments collection;

    public SignShopArgument(SignShopArguments pCollection) {
        collection = pCollection;
    }

    public SignShopArguments getCollection() { 
        return collection; 
    }

    public E get() { 
        return (isSpecial() ? special : inner); 
    }

    public E getInner() {
        return inner; 
    }

    public void set(E pSpecial) { 
        setSpecial(true); special = pSpecial; 
    }

    public void setInner(E pInner) {
        inner = pInner; 
    }

    public void setSpecial(boolean pSpecial) {
        bSpecial = pSpecial;
    }

    public boolean isSpecial() {
        return bSpecial;
    }

    public boolean isEmpty() {
        E got = get();
        if (got instanceof Collection) {
            Collection coll = (Collection) got;
            return coll.isEmpty();
        }
        else if (got instanceof String) {
            String string = (String) got;
            return string.isEmpty();
        }
        else {
            return got == null;
        }
    }
}
