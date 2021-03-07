package br.com.eterniaserver.eterniakamui.handlers;

import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.commands.Flags;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.core.queries.Update;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import me.ryanhamshire.GriefPrevention.events.PreventPvPEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PlayerHandler implements Listener {

    private final EterniaKamui plugin;

    public PlayerHandler(final EterniaKamui plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerDeath(PlayerDeathEvent event) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getEntity().getLocation(), false, false, null);
        if (claim != null && plugin.claimFlags.containsKey(claim.getID())) {
            if (plugin.claimFlags.get(claim.getID()).isKeepLevel()) {
                event.setDroppedExp(0);
                event.setKeepLevel(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        final Player defender = (Player) event.getEntity();

        if (!defender.getWorld().toString().equals("world")) {
            return;
        }

        long time = defender.getWorld().getTime();
        if (time < 12300 || time > 23850) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreventPvP(PreventPvPEvent event) {
        final Claim claim = event.getClaim();
        if (claim != null && plugin.claimFlags.containsKey(claim.getID())) {
            if (plugin.claimFlags.get(claim.getID()).isAllowPvP()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.setCancelled(handleExplosion(event.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(handleExplosion(event.getBlock().getLocation()));
    }

    private boolean handleExplosion(Location location) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, false, null);
        if (claim != null && plugin.claimFlags.containsKey(claim.getID())) {
            return !plugin.claimFlags.get(claim.getID()).isExplosions();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreaturePreSpawnEvent(PreCreatureSpawnEvent event) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getSpawnLocation(), false, false, null);
        if (claim != null && plugin.claimFlags.containsKey(claim.getID())) {
            if (!plugin.claimFlags.get(claim.getID()).isCreatureSpawn()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("EterniaFlags")) {
            menuGui((Player) e.getWhoClicked(), e.getSlot(), e);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getToBlock().getLocation(), false, false, null);

        if (claim != null && plugin.claimFlags.containsKey(claim.getID())) {
            if (!plugin.claimFlags.get(claim.getID()).isLiquidFluid()) {
                event.setCancelled(true);
            }
        }
    }

    private void menuGui(final Player player, int slotInt, InventoryClickEvent e) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, false, GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).lastClaim);
        final ClaimFlag claimFlag = plugin.claimFlags.get(claim.getID());
        int value = 0;
        Update update;
        switch (slotInt) {
            case 2:
                if (claimFlag.isCreatureSpawn()) {
                    e.getView().setItem(slotInt, Flags.guiItensDisable.get(2));
                } else {
                    e.getView().setItem(slotInt, Flags.guiItensEnable.get(2));
                    value = 1;
                }
                update = new Update(plugin.getString(Strings.TABLE_FLAGS));
                update.set.set("flag1", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setCreatureSpawn(value);
                break;
            case 3:
                if (claimFlag.isAllowPvP()) {
                    e.getView().setItem(slotInt, Flags.guiItensDisable.get(3));
                } else {
                    e.getView().setItem(slotInt, Flags.guiItensEnable.get(3));
                    value = 1;
                }
                update = new Update(plugin.getString(Strings.TABLE_FLAGS));
                update.set.set("flag2", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setAllowPvP(value);
                break;
            case 4:
                if (claimFlag.isExplosions()) {
                    e.getView().setItem(slotInt, Flags.guiItensDisable.get(4));
                } else {
                    e.getView().setItem(slotInt, Flags.guiItensEnable.get(4));
                    value = 1;
                }
                update = new Update(plugin.getString(Strings.TABLE_FLAGS));
                update.set.set("flag3", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setExplosions(value);
                break;
            case 5:
                if (claimFlag.isLiquidFluid()) {
                    e.getView().setItem(slotInt, Flags.guiItensDisable.get(5));
                } else {
                    e.getView().setItem(slotInt, Flags.guiItensEnable.get(5));
                    value = 1;
                }
                update = new Update(plugin.getString(Strings.TABLE_FLAGS));
                update.set.set("flag5", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setLiquidFluid(value);
                break;
            case 6:
                if (claimFlag.isKeepLevel()) {
                    e.getView().setItem(slotInt, Flags.guiItensDisable.get(6));
                } else {
                    e.getView().setItem(slotInt, Flags.guiItensEnable.get(9));
                    value = 1;
                }
                update = new Update(plugin.getString(Strings.TABLE_FLAGS));
                update.set.set("flag4", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setKeepLevel(value);
                break;
            default:
                break;
        }
    }


}
