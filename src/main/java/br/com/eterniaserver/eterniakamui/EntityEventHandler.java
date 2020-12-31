/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.eterniakamui.enums.Booleans;
import br.com.eterniaserver.eterniakamui.events.PreventPvPEvent;
import br.com.eterniaserver.eterniakamui.events.ProtectDeathDropsEvent;

import br.com.eterniaserver.eterniakamui.enums.ClaimsMode;
import br.com.eterniaserver.eterniakamui.enums.Messages;
import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.sql.queries.Update;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalExitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;

//handles events related to entities
public class EntityEventHandler implements Listener {
    //convenience reference for the singleton datastore
    private final DataStore dataStore;
    final EterniaKamui instance;
    private final NamespacedKey luredByPlayer;

    private static Set<EntityType> monsters = Set.of(EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, 
        EntityType.ELDER_GUARDIAN, EntityType.ENDER_DRAGON, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER, 
        EntityType.EVOKER_FANGS, EntityType.GHAST, EntityType.GIANT, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK,
        EntityType.ILLUSIONER, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER,
        EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY, EntityType.STRIDER, EntityType.VEX, EntityType.VINDICATOR,
        EntityType.WITCH, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_HORSE,
        EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER);

    public EntityEventHandler(DataStore dataStore, EterniaKamui plugin) {
        this.dataStore = dataStore;
        instance = plugin;
        luredByPlayer = new NamespacedKey(plugin, "lured_by_player");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityFormBlock(EntityBlockFormEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            String noBuildReason = EterniaKamui.instance.allowBuild(player, event.getBlock().getLocation(), event.getNewState().getType());
            if (noBuildReason != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLightningStrike(LightningStrikeEvent event) {
        if (event.getCause() == LightningStrikeEvent.Cause.TRIDENT)
            event.getLightning().setMetadata("GP_TRIDENT", new FixedMetadataValue(EterniaKamui.instance, event.getLightning().getLocation()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityChangeBLock(EntityChangeBlockEvent event) {
        if (!EterniaKamui.instance.config_endermenMoveBlocks && event.getEntityType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
        } else if (!EterniaKamui.instance.config_silverfishBreakBlocks && event.getEntityType() == EntityType.SILVERFISH) {
            event.setCancelled(true);
        } else if (!EterniaKamui.instance.config_rabbitsEatCrops && event.getEntityType() == EntityType.RABBIT) {
            event.setCancelled(true);
        } else if (EterniaKamui.getClaimsWorldModes(event.getBlock().getWorld()) != ClaimsMode.Disabled) {
            if (event.getEntityType() == EntityType.WITHER) {
                Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), null);
                if (claim == null || !claim.areExplosivesAllowed || !EterniaKamui.instance.config_blockClaimExplosions) {
                    event.setCancelled(true);
                }
            } else if (!EterniaKamui.instance.config_claims_ravagersBreakBlocks && event.getEntityType() == EntityType.RAVAGER) {
                event.setCancelled(true);
            }

            //don't allow crops to be trampled, except by a player with build permission
            else if (event.getTo() == Material.DIRT && event.getBlock().getType() == Material.FARMLAND) {
                if (event.getEntityType() != EntityType.PLAYER) {
                    event.setCancelled(true);
                } else {
                    Player player = (Player) event.getEntity();
                    Block block = event.getBlock();
                    if (EterniaKamui.instance.allowBreak(player, block, block.getLocation()) != null) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        //Prevent breaking lilypads via collision with a boat. Thanks Jikoo.
        else if (event.getEntity() instanceof Vehicle && !event.getEntity().getPassengers().isEmpty()) {
            Entity driver = event.getEntity().getPassengers().get(0);
            if (driver instanceof Player) {
                Block block = event.getBlock();
                if (EterniaKamui.instance.allowBreak((Player) driver, block, block.getLocation()) != null) {
                    event.setCancelled(true);
                }
            }
        }

        //sand cannon fix - when the falling block doesn't fall straight down, take additional anti-grief steps
        else if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            FallingBlock entity = (FallingBlock) event.getEntity();
            Block block = event.getBlock();

            //if changing a block TO air, this is when the falling block formed.  note its original location
            if (event.getTo() == Material.AIR) {
                entity.setMetadata("GP_FALLINGBLOCK", new FixedMetadataValue(EterniaKamui.instance, block.getLocation()));
            }
            //otherwise, the falling block is forming a block.  compare new location to original source
            else {
                List<MetadataValue> values = entity.getMetadata("GP_FALLINGBLOCK");
                //if we're not sure where this entity came from (maybe another plugin didn't follow the standard?), allow the block to form
                //Or if entity fell through an end portal, allow it to form, as the event is erroneously fired twice in this scenario.
                if (values.size() < 1) return;

                Location originalLocation = (Location) (values.get(0).value());
                Location newLocation = block.getLocation();

                //if did not fall straight down
                if (originalLocation.getBlockX() != newLocation.getBlockX() || originalLocation.getBlockZ() != newLocation.getBlockZ()) {
                    //in creative mode worlds, never form the block
                    if (EterniaKamui.getClaimsWorldModes(newLocation.getWorld()) == ClaimsMode.Creative) {
                        event.setCancelled(true);
                        return;
                    }

                    //in other worlds, if landing in land claim, only allow if source was also in the land claim
                    Claim claim = this.dataStore.getClaimAt(newLocation, null);
                    if (claim != null && !claim.contains(originalLocation, false)) {
                        //when not allowed, drop as item instead of forming a block
                        event.setCancelled(true);

                        ItemStack itemStack = new ItemStack(entity.getMaterial(), 1);
                        Item item = block.getWorld().dropItem(entity.getLocation(), itemStack);
                        item.setVelocity(new Vector());
                    }
                }
            }
        }
    }

    //Used by "sand cannon" fix to ignore fallingblocks that fell through End Portals
    //This is largely due to a CB issue with the above event
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFallingBlockEnterPortal(EntityPortalEnterEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK)
            return;
        event.getEntity().removeMetadata("GP_FALLINGBLOCK", instance);
    }

    //Don't let people drop in TNT through end portals
    //Necessarily this shouldn't be an issue anyways since the platform is obsidian...
    @EventHandler(ignoreCancelled = true)
    void onTNTExitPortal(EntityPortalExitEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT)
            return;
        if (event.getTo().getWorld().getEnvironment() != Environment.THE_END)
            return;
        event.getEntity().remove();
    }

    //don't allow zombies to break down doors
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onZombieBreakDoor(EntityBreakDoorEvent event) {
        if (!EterniaKamui.instance.config_zombiesBreakDoors) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("EterniaFlags")) {
            menuGui((Player) e.getWhoClicked(), e.getSlot(), e);
            e.setCancelled(true);
        }
    }

    private void menuGui(final Player player, int slotInt, InventoryClickEvent e) {
        Claim claim = EterniaKamui.instance.dataStore.getClaimAt(player.getLocation(), null);
        ClaimFlag claimFlag = PluginVars.claimFlags.get(claim.getID());
        int value = 0;
        Update update;
        switch (slotInt) {
            case 2:
                if (claimFlag.isCreatureSpawn()) {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensDisable.get(2));
                } else {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensEnable.get(2));
                    value = 1;
                }
                update = new Update("ek_flags");
                update.set.set("mobspawn", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setCreatureSpawn(value);
                break;
            case 3:
                if (claimFlag.isAllowPvP()) {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensDisable.get(3));
                } else {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensEnable.get(3));
                    value = 1;
                }
                update = new Update("ek_flags");
                update.set.set("pvp", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setAllowPvP(value);
                break;
            case 4:
                if (claimFlag.isExplosions()) {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensDisable.get(4));
                } else {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensEnable.get(4));
                    value = 1;
                }
                update = new Update("ek_flags");
                update.set.set("explosions", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setExplosions(value);
                break;
            case 5:
                if (claimFlag.isLiquidFluid()) {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensDisable.get(5));
                } else {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensEnable.get(5));
                    value = 1;
                }
                update = new Update("ek_flags");
                update.set.set("fluid", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setLiquidFluid(value);
                break;
            case 6:
                if (claimFlag.isKeepLevel()) {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensDisable.get(6));
                } else {
                    e.getView().setItem(slotInt, BaseCmdFlags.guiItensEnable.get(9));
                    value = 1;
                }
                update = new Update("ek_flags");
                update.set.set("keeplevel", value);
                update.where.set("claimid", claim.getID());
                SQL.executeAsync(update);
                claimFlag.setKeepLevel(value);
                break;
            default:
                break;
        }
    }

    //don't allow entities to trample crops
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event) {
        Material material = event.getBlock().getType();
        if (material == Material.FARMLAND) {
            if (!EterniaKamui.instance.config_creaturesTrampleCrops) {
                event.setCancelled(true);
            } else {
                Entity rider = event.getEntity().getPassenger();
                if (rider != null && rider.getType() == EntityType.PLAYER) {
                    event.setCancelled(true);
                }
            }
        }
    }

    //when an entity explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent explodeEvent) {
        this.handleExplosion(explodeEvent.getLocation(), explodeEvent.getEntity(), explodeEvent.blockList());
    }

    //when a block explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent explodeEvent) {
        this.handleExplosion(explodeEvent.getBlock().getLocation(), null, explodeEvent.blockList());
    }

    void handleExplosion(Location location, Entity entity, List<Block> blocks) {
        //only applies to claims-enabled worlds
        World world = location.getWorld();

        if (!EterniaKamui.instance.claimsEnabledForWorld(world)) return;

        //FEATURE: explosions don't destroy surface blocks by default
        boolean isCreeper = (entity != null && entity.getType() == EntityType.CREEPER);

        boolean applySurfaceRules = world.getEnvironment() == Environment.NORMAL && ((isCreeper && EterniaKamui.instance.config_blockSurfaceCreeperExplosions) || (!isCreeper && EterniaKamui.instance.config_blockSurfaceOtherExplosions));

        //special rule for creative worlds: explosions don't destroy anything
        if (EterniaKamui.instance.creativeRulesApply(location)) {
            for (int i = 0; i < blocks.size(); i++) {
                blocks.remove(i--);
            }
            return;
        }

        //make a list of blocks which were allowed to explode
        List<Block> explodedBlocks = new ArrayList<>();
        Claim cachedClaim = null;
        for (Block block : blocks) {
            //always ignore air blocks
            if (block.getType() == Material.AIR) continue;

            //is it in a land claim?
            Claim claim = this.dataStore.getClaimAt(block.getLocation(), cachedClaim);
            if (claim != null) {
                cachedClaim = claim;
            }

            //if yes, apply claim exemptions if they should apply
            if (claim != null && (claim.areExplosivesAllowed || !EterniaKamui.instance.config_blockClaimExplosions || (PluginVars.claimFlags.containsKey(claim.getID()) && PluginVars.claimFlags.get(claim.getID()).isExplosions()))) {
                explodedBlocks.add(block);
                continue;
            }

            //if no, then also consider surface rules
            if (claim == null) {
                if (!applySurfaceRules || block.getLocation().getBlockY() < EterniaKamui.instance.getSeaLevel(world) - 7) {
                    explodedBlocks.add(block);
                }
            }
        }

        //clear original damage list and replace with allowed damage list
        blocks.clear();
        blocks.addAll(explodedBlocks);
    }

    //when an item spawns...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        //if in a creative world, cancel the event (don't drop items on the ground)
        if (EterniaKamui.instance.creativeRulesApply(event.getLocation())) {
            event.setCancelled(true);
        }

        //if item is on watch list, apply protection
        List<PendingItemProtection> watchList = EterniaKamui.getPendingItemWatchList();
        Item newItem = event.getEntity();
        Long now = null;
        for (int i = 0; i < watchList.size(); i++) {
            PendingItemProtection pendingProtection = watchList.get(i);
            //ignore and remove any expired pending protections
            if (now == null) now = System.currentTimeMillis();
            if (pendingProtection.expirationTimestamp < now) {
                watchList.remove(i--);
                continue;
            }
            //skip if item stack doesn't match
            if (pendingProtection.itemStack.getAmount() != newItem.getItemStack().getAmount() ||
                    pendingProtection.itemStack.getType() != newItem.getItemStack().getType()) {
                continue;
            }

            //skip if new item location isn't near the expected spawn area
            Location spawn = event.getLocation();
            Location expected = pendingProtection.location;
            if (!spawn.getWorld().equals(expected.getWorld()) ||
                    spawn.getX() < expected.getX() - 5 ||
                    spawn.getX() > expected.getX() + 5 ||
                    spawn.getZ() < expected.getZ() - 5 ||
                    spawn.getZ() > expected.getZ() + 5 ||
                    spawn.getY() < expected.getY() - 15 ||
                    spawn.getY() > expected.getY() + 3) {
                continue;
            }

            //otherwise, mark item with protection information
            newItem.setMetadata("GP_ITEMOWNER", new FixedMetadataValue(EterniaKamui.instance, pendingProtection.owner));

            //and remove pending protection data
            watchList.remove(i);
            break;
        }
    }

    //when an experience bottle explodes...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onExpBottle(ExpBottleEvent event) {
        //if in a creative world, cancel the event (don't drop exp on the ground)
        if (EterniaKamui.instance.creativeRulesApply(event.getEntity().getLocation())) {
            event.setExperience(0);
        }
    }

    // Acionado quando uma criatura vai nascer
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(PreCreatureSpawnEvent event) {
        Claim claim = this.dataStore.getClaimAt(event.getSpawnLocation(), null);
        
        // Ignora caso não possua terreno ou caso o terreno não possua uma flag
        if (claim == null || !PluginVars.claimFlags.containsKey(claim.getID())) {
            return;
        }

        // Ignora caso o terreno tenha a flag de spawn de monstros ativados
        if (PluginVars.claimFlags.get(claim.getID()).isCreatureSpawn()) {
            return;
        }

        // Se contem esse tipo na lista de monstros o spawn é cancelado
        if (monsters.contains(event.getType())) {
            event.setCancelled(true);
        }
    }

    //when an entity dies...
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        //don't do the rest in worlds where claims are not enabled
        if (!EterniaKamui.instance.claimsEnabledForWorld(entity.getWorld())) return;

        //special rule for creative worlds: killed entities don't drop items or experience orbs
        if (EterniaKamui.instance.creativeRulesApply(entity.getLocation())) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }

        //FEATURE: when a player is involved in a siege (attacker or defender role)
        //his death will end the siege

        if (entity.getType() != EntityType.PLAYER) return;  //only tracking players

        Player player = (Player) entity;
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: lock dropped items to player who dropped them

        World world = entity.getWorld();

        //decide whether or not to apply this feature to this situation (depends on the world where it happens)
        boolean isPvPWorld = EterniaKamui.instance.pvpRulesApply(world);
        if ((isPvPWorld && EterniaKamui.instance.config_lockDeathDropsInPvpWorlds) ||
                (!isPvPWorld && EterniaKamui.instance.config_lockDeathDropsInNonPvpWorlds)) {
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
            ProtectDeathDropsEvent protectionEvent = new ProtectDeathDropsEvent(claim);
            Bukkit.getPluginManager().callEvent(protectionEvent);
            if (!protectionEvent.isCancelled()) {
                //remember information about these drops so that they can be marked when they spawn as items
                long expirationTime = System.currentTimeMillis() + 3000;  //now + 3 seconds
                Location deathLocation = player.getLocation();
                UUID playerID = player.getUniqueId();
                List<ItemStack> drops = event.getDrops();
                for (ItemStack stack : drops) {
                    EterniaKamui.getPendingItemWatchList().add(new PendingItemProtection(deathLocation, playerID, expirationTime, stack));
                }

                //allow the player to receive a message about how to unlock any drops
                playerData.dropsAreUnlocked = false;
                playerData.receivedDropUnlockAdvertisement = false;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onItemMerge(ItemMergeEvent event) {
        Item item = event.getEntity();
        List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");
        event.setCancelled(data != null && data.size() > 0);
    }

    //when an entity picks up an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityChangeBlockEvent event) {
        //FEATURE: endermen don't steal claimed blocks

        //if its an enderman
        if (event.getEntity().getType() == EntityType.ENDERMAN) {
            //and the block is claimed
            if (this.dataStore.getClaimAt(event.getBlock().getLocation(), null) != null) {
                //he doesn't get to steal it
                event.setCancelled(true);
            }
        }
    }

    //when a painting is broken
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakEvent event) {
        //don't track in worlds where claims are not enabled
        if (!EterniaKamui.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;

        //Ignore cases where itemframes should break due to no supporting blocks
        if (event.getCause() == RemoveCause.PHYSICS) return;

        //FEATURE: claimed paintings are protected from breakage

        //explosions don't destroy hangings
        if (event.getCause() == RemoveCause.EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        //only allow players to break paintings, not anything else (like water and explosions)
        if (!(event instanceof HangingBreakByEntityEvent)) {
            event.setCancelled(true);
            return;
        }

        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;

        //who is removing it?
        Entity remover = entityEvent.getRemover();

        //again, making sure the breaker is a player
        if (remover.getType() != EntityType.PLAYER) {
            event.setCancelled(true);
            return;
        }

        //if the player doesn't have build permission, don't allow the breakage
        Player playerRemover = (Player) entityEvent.getRemover();
        String noBuildReason = EterniaKamui.instance.allowBuild(playerRemover, event.getEntity().getLocation(), Material.AIR);
        if (noBuildReason != null) {
            event.setCancelled(true);
            EterniaKamui.sendMessage(playerRemover, TextMode.Err, noBuildReason);
        }
    }

    //when a painting is placed...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPaintingPlace(HangingPlaceEvent event) {
        //don't track in worlds where claims are not enabled
        if (!EterniaKamui.instance.claimsEnabledForWorld(event.getBlock().getWorld())) return;

        //FEATURE: similar to above, placing a painting requires build permission in the claim

        //if the player doesn't have permission, don't allow the placement
        String noBuildReason = EterniaKamui.instance.allowBuild(event.getPlayer(), event.getEntity().getLocation(), Material.PAINTING);
        if (noBuildReason != null) {
            event.setCancelled(true);
            EterniaKamui.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
        }

        //otherwise, apply entity-count limitations for creative worlds
        else if (EterniaKamui.instance.creativeRulesApply(event.getEntity().getLocation())) {
            PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
            Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), playerData.lastClaim);
            if (claim == null) return;

            String noEntitiesReason = claim.allowMoreEntities(false);
            if (noEntitiesReason != null) {
                EterniaKamui.sendMessage(event.getPlayer(), TextMode.Err, noEntitiesReason);
                event.setCancelled(true);
            }
        }
    }

    private boolean isMonster(Entity entity) {
        if (entity instanceof Monster) return true;

        EntityType type = entity.getType();
        if (type == EntityType.GHAST || type == EntityType.MAGMA_CUBE || type == EntityType.SHULKER)
            return true;

        if (type == EntityType.SLIME)
            return ((Slime) entity).getSize() > 0;

        if (type == EntityType.RABBIT)
            return ((Rabbit) entity).getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY;

        if (type == EntityType.PANDA)
            return ((Panda) entity).getMainGene() == Panda.Gene.AGGRESSIVE;

        if (type == EntityType.HOGLIN || type == EntityType.POLAR_BEAR)
            return !entity.getPersistentDataContainer().has(luredByPlayer, PersistentDataType.BYTE) && ((Mob) entity).getTarget() != null;

        return false;
    }

    // Tag passive animals that can become aggressive so we can tell whether or not they are hostile later
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!EterniaKamui.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;

        EntityType entityType = event.getEntityType();
        if (entityType != EntityType.HOGLIN && entityType != EntityType.POLAR_BEAR)
            return;

        if (event.getReason() == EntityTargetEvent.TargetReason.TEMPT)
            event.getEntity().getPersistentDataContainer().set(luredByPlayer, PersistentDataType.BYTE, (byte) 1);
        else
            event.getEntity().getPersistentDataContainer().remove(luredByPlayer);

    }

    //when an entity is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        this.handleEntityDamageEvent(event, true);
    }

    //when an entity is set on fire
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        //handle it just like we would an entity damge by entity event, except don't send player messages to avoid double messages
        //in cases like attacking with a flame sword or flame arrow, which would ALSO trigger the direct damage event handler

        EntityDamageByEntityEvent eventWrapper = new EntityDamageByEntityEvent(event.getCombuster(), event.getEntity(), DamageCause.FIRE_TICK, event.getDuration());
        this.handleEntityDamageEvent(eventWrapper, false);
        event.setCancelled(eventWrapper.isCancelled());
    }

    private boolean isDay(World world, Claim claim) {
        return world.getName().equals("world") && (world.getTime() < 12300 || world.getTime() > 23850) && (claim == null || !claim.isAdminClaim());
    }

    private void handleEntityDamageEvent(EntityDamageEvent event, boolean sendErrorMessagesToPlayers) {
        if (isMonster(event.getEntity())) {
            return;
        }

        if (event.getEntityType() == EntityType.DROPPED_ITEM && event.getEntity().hasMetadata("GP_ITEMOWNER")) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Tameable && !EterniaKamui.instance.pvpRulesApply(event.getEntity().getWorld()) && ((Tameable) event.getEntity()).isTamed()) {
            DamageCause cause = event.getCause();
            if (cause != null && (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.FALLING_BLOCK 
                    || cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA 
                    || cause == DamageCause.SUFFOCATION || cause == DamageCause.CONTACT || cause == DamageCause.DROWNING)) {
                event.setCancelled(true);
                return;
            }
        }
        if (handleBlockExplosionDamage(event)) return;

        //the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }

        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

        if (subEvent.getDamager() instanceof LightningStrike && subEvent.getDamager().hasMetadata("GP_TRIDENT")) {
            event.setCancelled(true);
            return;
        }

        Player attacker = null;
        Player defender = null;
        Projectile arrow = null;
        Firework firework = null;
        Entity damageSource = subEvent.getDamager();

        if (damageSource != null) {
            if (damageSource.getType() == EntityType.PLAYER) {
                attacker = (Player) damageSource;
            }
            
            else if (damageSource instanceof Projectile) {
                arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player) attacker = (Player) arrow.getShooter();
            }

            else if (damageSource instanceof Firework && damageSource.hasMetadata("GP_FIREWORK")) {
                List<MetadataValue> data = damageSource.getMetadata("GP_FIREWORK");
                if (data != null && !data.isEmpty()) {
                    firework = (Firework) damageSource;
                    attacker = (Player) data.get(0).value();
                }
            }

            if (event.getEntityType() == EntityType.PLAYER) {
                defender = (Player) event.getEntity();
            }

            if (damageSource.getType() == EntityType.AREA_EFFECT_CLOUD && defender != null && EterniaKamui.instance.pvpRulesApply(defender.getWorld())) {
                PlayerData damagedData = EterniaKamui.instance.dataStore.getPlayerData(defender.getUniqueId());
                Claim claim = EterniaKamui.instance.dataStore.getClaimAt(defender.getLocation(), damagedData.lastClaim);

                if (isDay(defender.getWorld(), claim)) {
                    EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.NoPvPInDay);
                    damagedData.lastClaim = claim;
                    event.setCancelled(true);
                    return;
                }

                //case 1: recently spawned
                if (EterniaKamui.instance.config_pvp_protectFreshSpawns && damagedData.pvpImmune) {
                    event.setCancelled(true);
                    return;
                }

                else if (claim != null) {
                    damagedData.lastClaim = claim;
                    if (EterniaKamui.instance.claimIsPvPSafeZone(claim)) {
                        PreventPvPEvent pvpEvent = new PreventPvPEvent(claim);
                        Bukkit.getPluginManager().callEvent(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            if (PluginVars.claimFlags.containsKey(claim.getID()) && PluginVars.claimFlags.get(claim.getID()).isAllowPvP()) {
                                return;
                            }
                            event.setCancelled(true);
                        }
                        return;
                    }
                }
            }
        }

        if (firework != null && defender != null && !EterniaKamui.instance.pvpRulesApply(defender.getWorld())) {
            PlayerData damagedData = EterniaKamui.instance.dataStore.getPlayerData(defender.getUniqueId());
            Claim damagedClaim = EterniaKamui.instance.dataStore.getClaimAt(defender.getLocation(), damagedData.lastClaim);

            if (isDay(defender.getWorld(), damagedClaim)) {
                EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.NoPvPInDay);
                damagedData.lastClaim = damagedClaim;
                event.setCancelled(true);
                return;
            }

            if (PluginVars.claimFlags.containsKey(damagedClaim.getID()) && PluginVars.claimFlags.get(damagedClaim.getID()).isAllowPvP()) {
                return;
            }
        
            if (attacker != defender) {
                event.setCancelled(true);
                return;
            }
        }

        //if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && defender != null && EterniaKamui.instance.pvpRulesApply(defender.getWorld())) {
            PlayerData damagedData = EterniaKamui.instance.dataStore.getPlayerData(defender.getUniqueId());
            Claim damagedClaim = EterniaKamui.instance.dataStore.getClaimAt(defender.getLocation(), damagedData.lastClaim);

            if (isDay(defender.getWorld(), damagedClaim)) {
                EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.NoPvPInDay);
                damagedData.lastClaim = damagedClaim;
                event.setCancelled(true);
                return;
            }
            
            if (attacker != defender) {
                PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());

                //otherwise if protecting spawning players
                if (EterniaKamui.instance.config_pvp_protectFreshSpawns) {
                    if (damagedData.pvpImmune) {
                        event.setCancelled(true);
                        if (sendErrorMessagesToPlayers)
                            EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                        return;
                    }

                    if (attackerData.pvpImmune) {
                        event.setCancelled(true);
                        if (sendErrorMessagesToPlayers)
                            EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                        return;
                    }
                }

                //FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                if (EterniaKamui.instance.config_pvp_noCombatInPlayerLandClaims || EterniaKamui.instance.config_pvp_noCombatInAdminLandClaims) {
                    Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), attackerData.lastClaim);
                    if (!attackerData.ignoreClaims) {
                        if (attackerClaim != null && !attackerData.inPvpCombat() && EterniaKamui.instance.claimIsPvPSafeZone(attackerClaim)) {
                            attackerData.lastClaim = attackerClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                            Bukkit.getPluginManager().callEvent(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                if (PluginVars.claimFlags.containsKey(attackerClaim.getID()) && PluginVars.claimFlags.get(attackerClaim.getID()).isAllowPvP()) {
                                    return;
                                }
                                event.setCancelled(true);
                                if (sendErrorMessagesToPlayers) {
                                    EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                }
                            }
                            return;
                        }

                        Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), damagedData.lastClaim);
                        if (defenderClaim != null && !damagedData.inPvpCombat() && EterniaKamui.instance.claimIsPvPSafeZone(defenderClaim)) {
                            damagedData.lastClaim = defenderClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                            Bukkit.getPluginManager().callEvent(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                if (PluginVars.claimFlags.containsKey(defenderClaim.getID()) && PluginVars.claimFlags.get(defenderClaim.getID()).isAllowPvP()) {
                                    return;
                                }
                                event.setCancelled(true);
                                if (sendErrorMessagesToPlayers) {
                                    EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                                }
                            }
                            return;
                        }
                    }
                }
            }
        }

        if (event instanceof EntityDamageByEntityEvent) {
            //don't track in worlds where claims are not enabled
            if (!EterniaKamui.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;

            //protect players from being attacked by other players' pets when protected from pvp
            if (defender != null) {

                //if attacker is a pet
                Entity damager = subEvent.getDamager();
                if (damager instanceof Tameable) {
                    Tameable pet = (Tameable) damager;
                    if (pet.isTamed() && pet.getOwner() != null) {
                        PlayerData damagedData = EterniaKamui.instance.dataStore.getPlayerData(defender.getUniqueId());
                        Claim damagedClaim = EterniaKamui.instance.dataStore.getClaimAt(defender.getLocation(), damagedData.lastClaim);
            
                        if (isDay(defender.getWorld(), damagedClaim)) {
                            EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.NoPvPInDay);
                            damagedData.lastClaim = damagedClaim;
                            event.setCancelled(true);
                            return;
                        }
                        
                        //if defender is NOT in pvp combat and not immune to pvp right now due to recent respawn
                        PlayerData defenderData = EterniaKamui.instance.dataStore.getPlayerData(event.getEntity().getUniqueId());
                        if (!defenderData.pvpImmune && !defenderData.inPvpCombat()) {
                            //if defender is not in a protected area
                            Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), defenderData.lastClaim);
                            if (defenderClaim != null &&
                                    !defenderData.inPvpCombat() &&
                                    EterniaKamui.instance.claimIsPvPSafeZone(defenderClaim)) {
                                defenderData.lastClaim = defenderClaim;
                                PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                                Bukkit.getPluginManager().callEvent(pvpEvent);

                                //if other plugins aren't making an exception to the rule 
                                if (!pvpEvent.isCancelled()) {
                                    event.setCancelled(true);
                                    if (damager instanceof Creature) ((Creature) damager).setTarget(null);
                                }
                                return;
                            }
                        }
                    }
                }
            }

            //if the damaged entity is a claimed item frame or armor stand, the damager needs to be a player with build trust in the claim
            if (subEvent.getEntityType() == EntityType.ITEM_FRAME
                    || subEvent.getEntityType() == EntityType.ARMOR_STAND
                    || subEvent.getEntityType() == EntityType.VILLAGER
                    || subEvent.getEntityType() == EntityType.ENDER_CRYSTAL) {
                //allow for disabling villager protections in the config
                if (subEvent.getEntityType() == EntityType.VILLAGER && !EterniaKamui.getBool(Booleans.CLAIMS_PROTECT_CREATURES))
                    return;

                //don't protect polar bears, they may be aggressive
                if (subEvent.getEntityType() == EntityType.POLAR_BEAR) return;

                //decide whether it's claimed
                Claim cachedClaim = null;
                PlayerData playerData;
                if (attacker != null) {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }

                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), cachedClaim);

                //if it's claimed
                if (claim != null) {
                    //if attacker isn't a player, cancel
                    if (attacker == null) {
                        //exception case
                        if (event.getEntityType() == EntityType.VILLAGER && damageSource != null && damageSource instanceof Zombie) {
                            return;
                        }

                        event.setCancelled(true);
                        return;
                    }

                    //otherwise player must have container trust in the claim
                    String failureReason = claim.allowBuild(attacker, Material.AIR);
                    if (failureReason != null) {
                        event.setCancelled(true);
                        if (sendErrorMessagesToPlayers)
                            EterniaKamui.sendMessage(attacker, TextMode.Err, failureReason);
                        return;
                    }
                }
            }

            //if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
            if (((subEvent.getEntity() instanceof Creature || subEvent.getEntity() instanceof WaterMob) && EterniaKamui.getBool(Booleans.CLAIMS_PROTECT_CREATURES))) {
                //if entity is tameable and has an owner, apply special rules
                if (subEvent.getEntity() instanceof Tameable) {
                    Tameable tameable = (Tameable) subEvent.getEntity();
                    if (tameable.isTamed() && tameable.getOwner() != null) {
                        //limit attacks by players to owners and admins in ignore claims mode
                        if (attacker != null) {
                            UUID ownerID = tameable.getOwner().getUniqueId();

                            //if the player interacting is the owner, always allow
                            if (attacker.getUniqueId().equals(ownerID)) return;

                            //allow for admin override
                            PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                            if (attackerData.ignoreClaims) return;

                            //otherwise disallow in non-pvp worlds (and also pvp worlds if configured to do so)
                            if (!EterniaKamui.instance.pvpRulesApply(subEvent.getEntity().getLocation().getWorld()) || (EterniaKamui.instance.config_pvp_protectPets && subEvent.getEntityType() != EntityType.WOLF)) {
                                OfflinePlayer owner = EterniaKamui.instance.getServer().getOfflinePlayer(ownerID);
                                String ownerName = owner.getName();
                                if (ownerName == null) ownerName = "someone";
                                String message = EterniaKamui.getMessage(Messages.NoDamageClaimedEntity, ownerName);
                                if (attacker.hasPermission("griefprevention.ignoreclaims"))
                                    message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                                if (sendErrorMessagesToPlayers)
                                    EterniaKamui.sendMessage(attacker, TextMode.Err, message);
                                PreventPvPEvent pvpEvent = new PreventPvPEvent(new Claim(subEvent.getEntity().getLocation(), subEvent.getEntity().getLocation(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null));
                                Bukkit.getPluginManager().callEvent(pvpEvent);
                                if (!pvpEvent.isCancelled()) {
                                    event.setCancelled(true);
                                }
                                return;
                            }
                            //and disallow if attacker is pvp immune
                            else if (attackerData.pvpImmune) {
                                event.setCancelled(true);
                                if (sendErrorMessagesToPlayers)
                                    EterniaKamui.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                return;
                            }
                            // disallow players attacking tamed wolves (dogs) unless under attack by said wolf
                            else if (tameable.getType() == EntityType.WOLF) {
                                if (!tameable.getOwner().equals(attacker)) {
                                    if (tameable.getTarget() != null) {
                                        if (tameable.getTarget() == attacker) return;
                                    }
                                    event.setCancelled(true);
                                    String ownerName = EterniaKamui.instance.getServer().getOfflinePlayer(ownerID).getName();
                                    String message = EterniaKamui.getMessage(Messages.NoDamageClaimedEntity, ownerName);
                                    if (attacker.hasPermission("griefprevention.ignoreclaims"))
                                        message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                                    if (sendErrorMessagesToPlayers)
                                        EterniaKamui.sendMessage(attacker, TextMode.Err, message);
                                    return;
                                }
                            }
                        }
                    }
                }

                Claim cachedClaim = null;
                PlayerData playerData = null;

                //if not a player or an explosive, allow
                //RoboMWM: Or a lingering potion, or a witch
                if (attacker == null
                        && damageSource != null
                        && damageSource.getType() != EntityType.CREEPER
                        && damageSource.getType() != EntityType.WITHER
                        && damageSource.getType() != EntityType.ENDER_CRYSTAL
                        && damageSource.getType() != EntityType.AREA_EFFECT_CLOUD
                        && damageSource.getType() != EntityType.WITCH
                        && !(damageSource instanceof Projectile)
                        && !(damageSource instanceof Explosive)
                        && !(damageSource instanceof ExplosiveMinecart)) {
                    return;
                }

                if (attacker != null) {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }

                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), cachedClaim);

                //if it's claimed
                if (claim != null) {
                    //if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
                    //why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
                    //TODO: Discuss if this should only apply to admin claims...?
                    if (attacker == null) {
                        //exception case
                        if (event.getEntityType() == EntityType.VILLAGER && damageSource != null && (damageSource.getType() == EntityType.ZOMBIE || damageSource.getType() == EntityType.VINDICATOR || damageSource.getType() == EntityType.EVOKER || damageSource.getType() == EntityType.EVOKER_FANGS || damageSource.getType() == EntityType.VEX)) {
                        }

                        //all other cases
                        else {
                            event.setCancelled(true);
                            if (damageSource instanceof Projectile) {
                                damageSource.remove();
                            }
                        }
                    }

                    //otherwise the player damaging the entity must have permission, unless it's a dog in a pvp world
                    else if (!(event.getEntity().getWorld().getPVP() && event.getEntity().getType() == EntityType.WOLF)) {
                        String noContainersReason = claim.allowContainers(attacker);
                        if (noContainersReason != null) {
                            event.setCancelled(true);

                            //kill the arrow to avoid infinite bounce between crowded together animals //RoboMWM: except for tridents
                            if (arrow != null && arrow.getType() != EntityType.TRIDENT) arrow.remove();
                            if (damageSource != null && damageSource.getType() == EntityType.FIREWORK && event.getEntity().getType() != EntityType.PLAYER)
                                return;

                            if (sendErrorMessagesToPlayers) {
                                String message = EterniaKamui.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                                if (attacker.hasPermission("griefprevention.ignoreclaims"))
                                    message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                                EterniaKamui.sendMessage(attacker, TextMode.Err, message);
                            }
                            event.setCancelled(true);
                        }

                        //cache claim for later
                        if (playerData != null) {
                            playerData.lastClaim = claim;
                        }
                    }
                }
            }
        }
    }

    private boolean handleBlockExplosionDamage(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.BLOCK_EXPLOSION) return false;

        Entity entity = event.getEntity();

        // Skip players - does allow players to use block explosions to bypass PVP protections,
        // but also doesn't disable self-damage.
        if (entity instanceof Player) return false;

        Claim claim = EterniaKamui.instance.dataStore.getClaimAt(entity.getLocation(), null);

        // Only block explosion damage inside claims.
        if (claim == null) return false;

        event.setCancelled(true);
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCrossbowFireWork(EntityShootBowEvent shootEvent) {
        if (shootEvent.getEntity() instanceof Player && shootEvent.getProjectile() instanceof Firework) {
            shootEvent.getProjectile().setMetadata("GP_FIREWORK", new FixedMetadataValue(EterniaKamui.instance, shootEvent.getEntity()));
        }
    }

    //when an entity is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageMonitor(EntityDamageEvent event) {
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getEntity().getType() != EntityType.PLAYER) return;

        Player defender = (Player) event.getEntity();

        //only interested in entities damaging entities (ignoring environmental damage)
        if (!(event instanceof EntityDamageByEntityEvent)) return;

        //Ignore "damage" from snowballs, eggs, etc. from triggering the PvP timer
        if (event.getDamage() == 0) return;

        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

        //if not in a pvp rules world, do nothing
        if (!EterniaKamui.instance.pvpRulesApply(defender.getWorld())) return;

        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow;
        Entity damageSource = subEvent.getDamager();

        if (damageSource != null) {
            if (damageSource.getType() == EntityType.PLAYER) {
                attacker = (Player) damageSource;
            } else if (damageSource instanceof Projectile) {
                arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player) {
                    attacker = (Player) arrow.getShooter();
                }
            } else if (damageSource instanceof Firework) {
                if (damageSource.hasMetadata("GP_FIREWORK")) {
                    List<MetadataValue> data = damageSource.getMetadata("GP_FIREWORK");
                    if (data != null && data.size() > 0) {
                        attacker = (Player) data.get(0).value();
                    }
                }
            }
        }

        //if attacker not a player, do nothing
        if (attacker == null) return;

        PlayerData defenderData = this.dataStore.getPlayerData(defender.getUniqueId());
        PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());

        if (attacker != defender) {
            long now = Calendar.getInstance().getTimeInMillis();
            defenderData.lastPvpTimestamp = now;
            defenderData.lastPvpPlayer = attacker.getName();
            attackerData.lastPvpTimestamp = now;
            attackerData.lastPvpPlayer = defender.getName();
        }
    }

    //when a vehicle is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleDamage(VehicleDamageEvent event) {
        //all of this is anti theft code
        if (!EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT)) return;

        //input validation
        if (event.getVehicle() == null) return;

        //don't track in worlds where claims are not enabled
        if (!EterniaKamui.instance.claimsEnabledForWorld(event.getVehicle().getWorld())) return;

        //determine which player is attacking, if any
        Player attacker = null;
        Entity damageSource = event.getAttacker();
        EntityType damageSourceType = null;

        //if damage source is null or a creeper, don't allow the damage when the vehicle is in a land claim
        if (damageSource != null) {
            damageSourceType = damageSource.getType();

            if (damageSource.getType() == EntityType.PLAYER) {
                attacker = (Player) damageSource;
            } else if (damageSource instanceof Projectile) {
                Projectile arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player) {
                    attacker = (Player) arrow.getShooter();
                }
            } else if (damageSource instanceof Firework) {
                if (damageSource.hasMetadata("GP_FIREWORK")) {
                    List<MetadataValue> data = damageSource.getMetadata("GP_FIREWORK");
                    if (data != null && data.size() > 0) {
                        attacker = (Player) data.get(0).value();
                    }
                }
            }
        }

        //if not a player and not an explosion, always allow
        if (attacker == null && damageSourceType != EntityType.CREEPER && damageSourceType != EntityType.WITHER && damageSourceType != EntityType.PRIMED_TNT) {
            return;
        }

        //NOTE: vehicles can be pushed around.
        //so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
        Claim cachedClaim = null;
        PlayerData playerData = null;

        if (attacker != null) {
            playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
            cachedClaim = playerData.lastClaim;
        }

        Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), cachedClaim);

        //if it's claimed
        if (claim != null) {
            //if damaged by anything other than a player, cancel the event
            if (attacker == null) {
                event.setCancelled(true);
            }

            //otherwise the player damaging the entity must have permission
            else {
                String noContainersReason = claim.allowContainers(attacker);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    String message = EterniaKamui.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if (attacker.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                    EterniaKamui.sendMessage(attacker, TextMode.Err, message);
                    event.setCancelled(true);
                }

                //cache claim for later
                if (playerData != null) {
                    playerData.lastClaim = claim;
                }
            }
        }
    }

    //when a splash potion effects one or more entities...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        //ignore potions not thrown by players
        ProjectileSource projectileSource = potion.getShooter();
        if (projectileSource == null) return;
        Player thrower = null;
        if ((projectileSource instanceof Player))
            thrower = (Player) projectileSource;

        Collection<PotionEffect> effects = potion.getEffects();
        for (PotionEffect effect : effects) {
            PotionEffectType effectType = effect.getType();

            //restrict some potions on claimed animals (griefers could use this to kill or steal animals over fences) //RoboMWM: include villagers
            if (effectType.getName().equals("JUMP") || effectType.getName().equals("POISON")) {
                Claim cachedClaim = null;
                for (LivingEntity effected : event.getAffectedEntities()) {
                    if (effected.getType() == EntityType.VILLAGER || effected instanceof Animals) {
                        Claim claim = this.dataStore.getClaimAt(effected.getLocation(), cachedClaim);
                        if (claim != null) {
                            cachedClaim = claim;
                            if (thrower == null || claim.allowContainers(thrower) != null) {
                                event.setIntensity(effected, 0);
                                EterniaKamui.sendMessage(thrower, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
                                return;
                            }
                        }
                    }
                }
            }

            //Otherwise, ignore potions not thrown by players
            if (thrower == null) return;

            //otherwise, no restrictions for positive effects
            if (positiveEffects.contains(effectType)) continue;

            for (LivingEntity effected : event.getAffectedEntities()) {
                //always impact the thrower
                if (effected == thrower) continue;

                //always impact non players
                if (effected.getType() != EntityType.PLAYER) {
                }

                //otherwise if in no-pvp zone, stop effect
                //FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                else if (EterniaKamui.instance.config_pvp_noCombatInPlayerLandClaims || EterniaKamui.instance.config_pvp_noCombatInAdminLandClaims) {
                    Player effectedPlayer = (Player) effected;
                    PlayerData defenderData = this.dataStore.getPlayerData(effectedPlayer.getUniqueId());
                    PlayerData attackerData = this.dataStore.getPlayerData(thrower.getUniqueId());
                    Claim attackerClaim = this.dataStore.getClaimAt(thrower.getLocation(), attackerData.lastClaim);
                    if (attackerClaim != null && EterniaKamui.instance.claimIsPvPSafeZone(attackerClaim)) {
                        attackerData.lastClaim = attackerClaim;
                        PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                        Bukkit.getPluginManager().callEvent(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            event.setIntensity(effected, 0);
                            EterniaKamui.sendMessage(thrower, TextMode.Err, Messages.CantFightWhileImmune);
                        }
                        continue;
                    }

                    Claim defenderClaim = this.dataStore.getClaimAt(effectedPlayer.getLocation(), defenderData.lastClaim);
                    if (defenderClaim != null && EterniaKamui.instance.claimIsPvPSafeZone(defenderClaim)) {
                        defenderData.lastClaim = defenderClaim;
                        PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                        Bukkit.getPluginManager().callEvent(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            event.setIntensity(effected, 0);
                            EterniaKamui.sendMessage(thrower, TextMode.Err, Messages.PlayerInPvPSafeZone);
                        }
                    }
                }
            }
        }
    }

    public static final HashSet<PotionEffectType> positiveEffects = new HashSet<>(Arrays.asList(
            PotionEffectType.ABSORPTION,
            PotionEffectType.DAMAGE_RESISTANCE,
            PotionEffectType.FAST_DIGGING,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.HEAL,
            PotionEffectType.HEALTH_BOOST,
            PotionEffectType.INCREASE_DAMAGE,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.JUMP,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.REGENERATION,
            PotionEffectType.SATURATION,
            PotionEffectType.SPEED,
            PotionEffectType.WATER_BREATHING
    ));
}
