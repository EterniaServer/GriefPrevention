package br.com.eterniaserver.eterniakamui;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

class PendingItemProtection {
    public final Location location;
    public final UUID owner;
    final long expirationTimestamp;
    final ItemStack itemStack;

    public PendingItemProtection(Location location, UUID owner, long expirationTimestamp, ItemStack itemStack) {
        this.location = location;
        this.owner = owner;
        this.expirationTimestamp = expirationTimestamp;
        this.itemStack = itemStack;
    }
}
