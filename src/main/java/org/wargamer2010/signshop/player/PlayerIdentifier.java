
package org.wargamer2010.signshop.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;

@SerializableAs("PlayerId")
public class PlayerIdentifier implements ConfigurationSerializable {
    private UUID id = null;
    private String name = null;

    public PlayerIdentifier(OfflinePlayer player) {
        this(player.getUniqueId(), player.getName());
    }

    public PlayerIdentifier(UUID uuid, String username) {
        if (uuid == null && username == null) {
            throw new NullPointerException("uuid and username can't both be null");
        }

        this.id = uuid;
        this.name = username;
    }

    @Deprecated
    public PlayerIdentifier(UUID pId) {
        this.id = Objects.requireNonNull(pId);
    }

    @Deprecated
    public PlayerIdentifier(String pName) {
        name = pName;

        OfflinePlayer offplayer = getOfflinePlayer();
        if (offplayer != null) {
            id = offplayer.getUniqueId();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if (id != null) map.put("uuid", id.toString());
        if (name != null) map.put("username", name);
        return map;
    }

    public static PlayerIdentifier valueOf(Map<String, Object> map) {
        String name = (String) map.get("username");
        String uuid = (String) map.get("uuid");

        UUID id = uuid == null ? null : UUID.fromString(uuid);
        return new PlayerIdentifier(id, name);
    }

    public Player getPlayer() {
        Player player = Bukkit.getPlayer(id);
        if (player == null) player = Bukkit.getPlayerExact(name);

        if (player != null) {
            this.id = player.getUniqueId();
            this.name = player.getName();
            return player;
        }

        return null;
    }

    @Deprecated
    public final OfflinePlayer getOfflinePlayer() {
        OfflinePlayer player = getPlayer();
        if (player != null) {
            return player;
        } else if (id != null) {
            player = Bukkit.getOfflinePlayer(id);
            if (!player.getName().equals(id.toString())) {
                this.name = player.getName();
            }
        } else if (name != null) {
            player = Bukkit.getOfflinePlayer(name);
            this.id = player.getUniqueId();
        }

        return player;
    }

    public final String getName() {
        if (name != null && !name.isEmpty()) return name;

        OfflinePlayer offlinePlayer = getOfflinePlayer(); //if found, already sets the name field
        if (offlinePlayer != null) return offlinePlayer.getName();

        return null;
    }

    /**
     * @deprecated sets either the SignShopPlayer's UUID or Username, not both
     * @param string the player's UUID or Username
     * @return
     */
    //TODO move this to SignShopPlayer.java?
    public static SignShopPlayer getPlayerFromString(String string) {
        if (string == null || string.isEmpty())
            return null;

        try {
            return new SignShopPlayer(new PlayerIdentifier(UUID.fromString(string)));
        } catch(IllegalArgumentException ex) {
            //string was not a uuid, just continue and try by name
        }

        return getByName(string);
    }

    /**
     * This method should not be used, lookup by name has been deprecated
     * But it can be used for transition purposes
     *
     * Primarily used to convert the names to UUID in the sellers.yml
     *
     * @param name Player name
     * @return SignShopPlayer instance
     * @deprecated uses {@link Bukkit#getOfflinePlayer(String)}
     */
    @Deprecated
    public static SignShopPlayer getByName(String name) {
        if(name == null || name.isEmpty())
            return null;

        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        PlayerIdentifier id = new PlayerIdentifier(player);

        return new SignShopPlayer(id);
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            this.id = getOfflinePlayer().getUniqueId();
        }

        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof PlayerIdentifier)) return false;

        final PlayerIdentifier other = (PlayerIdentifier) obj;
        if (other.id != null && this.id != null) return this.id.equals(other.id);

        if (other.id == null && this.id == null) {
            if (Objects.equals(this.name, other.name)) {
                return true;
            }
        }

        //hopefully we don't get here.
        OfflinePlayer myOfflinePlayer = getOfflinePlayer();
        OfflinePlayer otherOfflinePlayer = other.getOfflinePlayer();
        if (otherOfflinePlayer == null) {
            return myOfflinePlayer == null;
        } else {
            return myOfflinePlayer != null && otherOfflinePlayer.getUniqueId().equals(myOfflinePlayer.getUniqueId());
        }
    }

    @Override
    public String toString() {
            return id == null ? name : id.toString();
    }

}
