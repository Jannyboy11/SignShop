
package org.wargamer2010.signshop.blocks;

import org.bukkit.inventory.meta.BookMeta;

public class BookFactory {
    private BookFactory() {

    }

    private static IItemTags tags;
    
    /**
     * For unit testing purposes
     * @param newTags Tags to set
     */
    public static void setItemTags(IItemTags newTags) {
        tags = newTags;
    }

    public static IBookItem getBookItem(org.bukkit.inventory.ItemStack stack) {
        //both branches were doing the same.
        return new BookItem(stack);

//            try {
//                // Bukkit API 1.9.4+
//                BookMeta.Generation.class.isEnum();
//                return new BookItem(stack);
//            } catch( NoClassDefFoundError e ) {
//                // Bukkit API 1.8.8-
//                return new BookItem(stack);
//            }
    }

    public static IItemTags getItemTags() {
        //lazy init
        return tags == null ? tags = new ItemTags() : tags;
    }
}
