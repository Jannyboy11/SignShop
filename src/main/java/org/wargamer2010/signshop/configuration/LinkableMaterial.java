
package org.wargamer2010.signshop.configuration;

import org.bukkit.Material;

public class LinkableMaterial {
    private final String materialName;
    private final String alias;

    public LinkableMaterial(Material material) {
        this(material, material.name());
    }

    public LinkableMaterial(Material material, String alias) {
        this(material.name(), alias);
    }

    public LinkableMaterial(String materialName, String alias) {
        this.materialName = materialName;
        this.alias = alias;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getAlias() {
        return alias;
    }

}
