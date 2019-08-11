package org.wargamer2010.signshop.util;

import org.getspout.spoutapi.material.Material;
import org.getspout.spoutapi.material.MaterialData;

/**
 * @deprecated Spout still uses item IDs, which no longer exists since Minecraft 1.13+
 */
@Deprecated
public class SpoutUtil {

    private SpoutUtil() {
        //who even uses spout still?!
    }

    public static String getName(org.bukkit.material.MaterialData data, Short durability) {
        if(data == null)
            return null;
        Material spoutmat = MaterialData.getMaterial(data.getItemType().getId(), durability);
        if(spoutmat != null)
            return spoutmat.getName();
        else
            return null;
    }
}
