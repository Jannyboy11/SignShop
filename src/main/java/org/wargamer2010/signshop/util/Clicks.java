package org.wargamer2010.signshop.util;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.Map;
import java.util.LinkedHashMap;
import org.bukkit.entity.Entity;
import org.wargamer2010.signshop.player.PlayerIdentifier;

public class Clicks {
    //TODO make these private and only give get/set access
    public static Map<Location, Player> mClicksPerLocation = new LinkedHashMap<>();
    public static Map<PlayerIdentifier, Player> mClicksPerPlayerId = new LinkedHashMap<>();
    public static Map<Entity, Player> mClicksPerEntity = new LinkedHashMap<>(); //TODO these are unused. what to do with them?

    private Clicks() {

    }

    public static void removePlayerFromClickMap(Player player) {
        mClicksPerLocation.values().remove(player);
    }

    public static void removePlayerFromEntityMap(Player player) {
        mClicksPerEntity.values().remove(player);
    }
}
