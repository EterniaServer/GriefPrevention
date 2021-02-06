/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

package br.com.eterniaserver.eterniakamui.handlers;

import br.com.eterniaserver.eterniakamui.*;
import br.com.eterniaserver.eterniakamui.enums.*;
import br.com.eterniaserver.eterniakamui.events.ClaimInspectionEvent;
import br.com.eterniaserver.eterniakamui.events.VisualizationEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventHandler implements Listener {
    private final DataStore dataStore;
    private final EterniaKamui instance;


    //typical constructor, yawn
    public PlayerEventHandler(DataStore dataStore, EterniaKamui plugin) {
        this.dataStore = dataStore;
        this.instance = plugin;
    }

    //when a player uses a slash command...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");

        String command = args[0].toLowerCase();

        Player player = event.getPlayer();
        PlayerData playerData = null;

        //if in pvp, block any pvp-banned slash commands
        if (playerData == null) playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());

        if (playerData.inPvpCombat() && instance.config_pvp_blockedCommands.contains(command)) {
            event.setCancelled(true);
            EterniaKamui.sendMessage(event.getPlayer(), TextMode.Err, Messages.CommandBannedInPvP);
            return;
        }

        //if requires access trust, check for permission
        boolean isMonitoredCommand = false;
        String lowerCaseMessage = message.toLowerCase();
        for (String monitoredCommand : instance.config_claims_commandsRequiringAccessTrust) {
            if (lowerCaseMessage.startsWith(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand) {
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;
                String reason = claim.allowAccess(player);
                if (reason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, reason);
                    event.setCancelled(true);
                }
            }
        }
    }

    static int longestNameLength = 10;

    static void makeSocialLogEntry(String name, String message) {

        longestNameLength = Math.max(longestNameLength, name.length());
        //TODO: cleanup static
        String entryBuilder = name + " ".repeat(Math.max(0, longestNameLength - name.length())) +
                ": " + message;
        EterniaKamui.AddLogEntry(entryBuilder, CustomLogEntryTypes.SocialActivity, true);
    }

    //when a player attempts to join the server...
    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        //remember the player's ip address
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        playerData.ipAddress = event.getAddress();
    }

    //when a player successfully joins the server...

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();

        //note login time
        Date nowDate = new Date();
        long now = nowDate.getTime();
        PlayerData playerData = this.dataStore.getPlayerData(playerID);
        playerData.lastSpawn = now;

        //if newish, prevent chat until he's moved a bit to prove he's not a bot
        if (EterniaKamui.isNewToServer(player)) {
            playerData.noChatLocation = player.getLocation();
        }

        //if player has never played on the server before...
        if (!player.hasPlayedBefore()) {
            //may need pvp protection
            instance.checkPvpProtectionNeeded(player);

            //if in survival claims mode, send a message about the claim basics video (except for admins - assumed experts)
            if (EterniaKamui.getClaimsWorldModes(player.getWorld()) == ClaimsMode.Survival && !player.hasPermission("griefprevention.adminclaims") && this.dataStore.claims.size() > 10) {
                WelcomeTask task = new WelcomeTask(player);
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, task, instance.config_claims_manualDeliveryDelaySeconds * 20L);
            }
        }

        //in case player has changed his name, on successful login, update UUID > Name mapping
        EterniaKamui.cacheUUIDNamePair(player.getUniqueId(), player.getName());

        //is he stuck in a portal frame?
        if (player.hasMetadata("GP_PORTALRESCUE")) {
            //If so, let him know and rescue him in 10 seconds. If he is in fact not trapped, hopefully chunks will have loaded by this time so he can walk out.
            EterniaKamui.sendMessage(player, TextMode.Info, Messages.NetherPortalTrapDetectionMessage, 20L);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getPortalCooldown() > 8 && player.hasMetadata("GP_PORTALRESCUE")) {
                        EterniaKamui.AddLogEntry("Rescued " + player.getName() + " from a nether portal.\nTeleported from " + player.getLocation().toString() + " to " + player.getMetadata("GP_PORTALRESCUE").get(0).value().toString(), CustomLogEntryTypes.Debug);
                        player.teleport((Location) player.getMetadata("GP_PORTALRESCUE").get(0).value());
                        player.removeMetadata("GP_PORTALRESCUE", instance);
                    }
                }
            }.runTaskLater(instance, 200L);
        }
        //Otherwise just reset cooldown, just in case they happened to logout again...
        else
            player.setPortalCooldown(0);

    }

    //when a player spawns, conditionally apply temporary pvp protection
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
        playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
        playerData.lastPvpTimestamp = 0;  //no longer in pvp combat

        //also send him any messaged from grief prevention he would have received while dead
        if (playerData.messageOnRespawn != null) {
            EterniaKamui.sendMessage(player, ChatColor.RESET /*color is alrady embedded in message in this case*/, playerData.messageOnRespawn, 40L);
            playerData.messageOnRespawn = null;
        }

        instance.checkPvpProtectionNeeded(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerDeath(PlayerDeathEvent event) {
        //FEATURE: prevent death message spam by implementing a "cooldown period" for death messages
        Player player = event.getEntity();

        //these are related to locking dropped items on death to prevent theft
        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
        playerData.dropsAreUnlocked = false;
        playerData.receivedDropUnlockAdvertisement = false;

        Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
        if (claim != null && PluginVars.claimFlags.containsKey(claim.getID())) {
            if (PluginVars.claimFlags.get(claim.getID()).isKeepLevel()) {
                event.setDroppedExp(0);
                event.setKeepLevel(true);
            }
        }
    }

    //when a player gets kicked...
    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerKicked(PlayerKickEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        playerData.wasKicked = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        PlayerData playerData = this.dataStore.getPlayerData(playerID);
        boolean isBanned;

        //If player is not trapped in a portal and has a pending rescue task, remove the associated metadata
        //Why 9? No idea why, but this is decremented by 1 when the player disconnects.
        if (player.getPortalCooldown() < 9) {
            player.removeMetadata("GP_PORTALRESCUE", instance);
        }

        if (playerData.wasKicked) {
            isBanned = player.isBanned();
        } else {
            isBanned = false;
        }

        //silence notifications when the player is banned
        if (isBanned) {
            event.setQuitMessage(null);
        }

        //make sure his data is all saved - he might have accrued some claim blocks while playing that were not saved immediately
        else {
            this.dataStore.savePlayerData(player.getUniqueId(), playerData);
        }

        //FEATURE: players in pvp combat when they log out will die
        if (instance.config_pvp_punishLogout && playerData.inPvpCombat()) {
            player.setHealth(0);
        }

        //FEATURE: during a siege, any player who logs out dies and forfeits the siege

        //drop data about this player
        this.dataStore.clearCachedPlayerData(playerID);

    }

    //when a player drops an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        //in creative worlds, dropping items is blocked
        if (instance.creativeRulesApply(player.getLocation())) {
            event.setCancelled(true);
            return;
        }

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide
        //them or give them away to other players before they are defeated

        //if in combat, don't let him drop it
        if (!instance.config_pvp_allowCombatItemDrop && playerData.inPvpCombat()) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
            event.setCancelled(true);
        }

    }

    //when a player teleports
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: prevent players from using ender pearls to gain access to secured claims
        TeleportCause cause = event.getCause();
        if (cause == TeleportCause.CHORUS_FRUIT || (cause == TeleportCause.ENDER_PEARL && EterniaKamui.getBool(Booleans.CLAIMS_ENDERPEARLS_REQUIRE_ACCESSTRUST))) {
            Claim toClaim = this.dataStore.getClaimAt(event.getTo(), playerData.lastClaim);
            if (toClaim != null) {
                playerData.lastClaim = toClaim;
                String noAccessReason = toClaim.allowAccess(player);
                if (noAccessReason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, noAccessReason);
                    event.setCancelled(true);
                    if (cause == TeleportCause.ENDER_PEARL)
                        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                }
            }
        }

    }

    //when a player triggers a raid (in a claim)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTriggerRaid(RaidTriggerEvent event) {
        if (!EterniaKamui.getBool(Booleans.CLAIMS_RAID_TRIGGERS_REQUIRE_BUILDTRUST)) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        Claim toClaim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
        if (toClaim == null)
            return;

        playerData.lastClaim = toClaim;
        if (toClaim.allowBuild(player, Material.AIR) == null)
            return;

        event.setCancelled(true);
    }

    //when a player interacts with a specific part of entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        //treat it the same as interacting with an entity in general
        if (event.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            this.onPlayerInteractEntity(event);
        }
    }

    //when a player interacts with an entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!instance.claimsEnabledForWorld(entity.getWorld())) return;

        //allow horse protection to be overridden to allow management from other plugins
        if (!EterniaKamui.getBool(Booleans.CLAIMS_PROTECT_HORSES) && entity instanceof AbstractHorse) return;
        if (!EterniaKamui.getBool(Booleans.CLAIMS_PROTECT_DONKEYS) && (entity instanceof Donkey || entity instanceof Mule)) return;
        if (!EterniaKamui.getBool(Booleans.CLAIMS_PROTECT_LLAMAS) && entity instanceof Llama) return;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //if entity is tameable and has an owner, apply special rules
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed()) {
                if (tameable.getOwner() != null) {
                    UUID ownerID = tameable.getOwner().getUniqueId();

                    //if the player interacting is the owner or an admin in ignore claims mode, always allow
                    if (player.getUniqueId().equals(ownerID) || playerData.ignoreClaims) {
                        //if giving away pet, do that instead
                        if (playerData.petGiveawayRecipient != null) {
                            tameable.setOwner(playerData.petGiveawayRecipient);
                            playerData.petGiveawayRecipient = null;
                            EterniaKamui.sendMessage(player, TextMode.Success, Messages.PetGiveawayConfirmation);
                            event.setCancelled(true);
                        }

                        return;
                    }
                    if (!instance.pvpRulesApply(entity.getLocation().getWorld()) || instance.config_pvp_protectPets) {
                        //otherwise disallow
                        OfflinePlayer owner = instance.getServer().getOfflinePlayer(ownerID);
                        String ownerName = owner.getName();
                        if (ownerName == null) ownerName = "someone";
                        String message = EterniaKamui.getMessage(Messages.NotYourPet, ownerName);
                        if (player.hasPermission("griefprevention.ignoreclaims"))
                            message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                        EterniaKamui.sendMessage(player, TextMode.Err, message);
                        event.setCancelled(true);
                        return;
                    }
                }
            } else  //world repair code for a now-fixed GP bug //TODO: necessary anymore?
            {
                //ensure this entity can be tamed by players
                tameable.setOwner(null);
                if (tameable instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) tameable;
                    holder.getInventory().clear();
                }
            }
        }

        //don't allow interaction with item frames or armor stands in claimed areas without build permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Hanging) {
            String noBuildReason = instance.allowBuild(player, entity.getLocation(), Material.ITEM_FRAME);
            if (noBuildReason != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                event.setCancelled(true);
                return;
            }
        }

        //limit armor placements when entity count is too high
        if (entity.getType() == EntityType.ARMOR_STAND && instance.creativeRulesApply(player.getLocation())) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), playerData.lastClaim);
            if (claim == null) return;

            String noEntitiesReason = claim.allowMoreEntities(false);
            if (noEntitiesReason != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, noEntitiesReason);
                event.setCancelled(true);
                return;
            }
        }

        //always allow interactions when player is in ignore claims mode
        if (playerData.ignoreClaims) return;

        //don't allow container access during pvp combat
        if ((entity instanceof StorageMinecart || entity instanceof PoweredMinecart)) {

            if (playerData.inPvpCombat()) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }
        }

        //if the entity is a vehicle and we're preventing theft in claims
        if (EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && entity instanceof Vehicle) {
            //if the entity is in a claim
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), null);
            if (claim != null) {
                //for storage entities, apply container rules (this is a potential theft)
                if (entity instanceof InventoryHolder) {
                    String noContainersReason = claim.allowContainers(player);
                    if (noContainersReason != null) {
                        EterniaKamui.sendMessage(player, TextMode.Err, noContainersReason);
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        //if the entity is an animal, apply container rules
        if ((EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && (entity instanceof Animals || entity instanceof Fish)) || (entity.getType() == EntityType.VILLAGER && EterniaKamui.getBool(Booleans.CLAIMS_VILLAGER_TRADING_REQUIRES_TRUST))) {
            //if the entity is in a claim
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), null);
            if (claim != null) {
                if (claim.allowContainers(player) != null) {
                    String message = EterniaKamui.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + EterniaKamui.getMessage(Messages.IgnoreClaimsAdvertisement);
                    EterniaKamui.sendMessage(player, TextMode.Err, message);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        //if preventing theft, prevent leashing claimed creatures
        if (EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && entity instanceof Creature && instance.getItemInHand(player, event.getHand()).getType() == Material.LEAD) {
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), playerData.lastClaim);
            if (claim != null) {
                String failureReason = claim.allowContainers(player);
                if (failureReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, failureReason);
                }
            }
        }
    }

    //when a player reels in his fishing rod
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        Entity entity = event.getCaught();
        if (entity == null) return;  //if nothing pulled, uninteresting event

        //if should be protected from pulling in land claims without permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Animals) {
            Player player = event.getPlayer();
            PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = instance.dataStore.getClaimAt(entity.getLocation(), playerData.lastClaim);
            if (claim != null) {
                //if no permission, cancel
                String errorMessage = claim.allowContainers(player);
                if (errorMessage != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
                }
            }
        }
    }

    //when a player picks up an item...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        //FEATURE: lock dropped items to player who dropped them

        //who owns this stack?
        Item item = event.getItem();
        List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");
        if (data != null && data.size() > 0) {
            UUID ownerID = (UUID) data.get(0).value();

            //has that player unlocked his drops?
            OfflinePlayer owner = instance.getServer().getOfflinePlayer(ownerID);
            String ownerName = EterniaKamui.lookupPlayerName(ownerID);
            if (owner.isOnline() && !player.equals(owner)) {
                PlayerData playerData = this.dataStore.getPlayerData(ownerID);

                //if locked, don't allow pickup
                if (!playerData.dropsAreUnlocked) {
                    event.setCancelled(true);

                    //if hasn't been instructed how to unlock, send explanatory messages
                    if (!playerData.receivedDropUnlockAdvertisement) {
                        EterniaKamui.sendMessage(owner.getPlayer(), TextMode.Instr, Messages.DropUnlockAdvertisement);
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.PickupBlockedExplanation, ownerName);
                        playerData.receivedDropUnlockAdvertisement = true;
                    }

                    return;
                }
            }
        }

        //the rest of this code is specific to pvp worlds
        if (!instance.pvpRulesApply(player.getWorld())) return;

        //if we're preventing spawn camping and the player was previously empty handed...
        if (instance.config_pvp_protectFreshSpawns && (instance.getItemInHand(player, EquipmentSlot.HAND).getType() == Material.AIR)) {
            //if that player is currently immune to pvp
            PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
            if (playerData.pvpImmune) {
                //if it's been less than 10 seconds since the last time he spawned, don't pick up the item
                long now = Calendar.getInstance().getTimeInMillis();
                long elapsedSinceLastSpawn = now - playerData.lastSpawn;
                if (elapsedSinceLastSpawn < 10000) {
                    event.setCancelled(true);
                    return;
                }

                //otherwise take away his immunity. he may be armed now.  at least, he's worth killing for some loot
                playerData.pvpImmune = false;
                EterniaKamui.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }
    }

    //when a player switches in-hand items
    @EventHandler(ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        //if he's switching to the golden shovel
        int newSlot = event.getNewSlot();
        ItemStack newItemStack = player.getInventory().getItem(newSlot);
        if (newItemStack != null && newItemStack.getType() == EterniaKamui.getMaterials(Materials.MODIFICATION_TOOL)) {
            //give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
            if (instance.claimsEnabledForWorld(player.getWorld())) {
                EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
                instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, task, 15L);  //15L is approx. 3/4 of a second
            }
        }
    }

    //block use of buckets within other players' claims
    private final Set<Material> commonAdjacentBlocks_water = EnumSet.of(Material.WATER, Material.FARMLAND, Material.DIRT, Material.STONE);
    private final Set<Material> commonAdjacentBlocks_lava = EnumSet.of(Material.LAVA, Material.DIRT, Material.STONE);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent bucketEvent) {
        if (!instance.claimsEnabledForWorld(bucketEvent.getBlockClicked().getWorld())) return;

        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
        int minLavaDistance = 10;

        // Fixes #1155:
        // Prevents waterlogging blocks placed on a claim's edge.
        // Waterlogging a block affects the clicked block, and NOT the adjacent location relative to it.
        if (bucketEvent.getBucket() == Material.WATER_BUCKET && bucketEvent.getBlockClicked().getBlockData() instanceof Waterlogged) {
            block = bucketEvent.getBlockClicked();
        }

        //make sure the player is allowed to build at the location
        String noBuildReason = instance.allowBuild(player, block.getLocation(), Material.WATER);
        if (noBuildReason != null) {
            EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
            bucketEvent.setCancelled(true);
            return;
        }

        //if the bucket is being used in a claim, allow for dumping lava closer to other players
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(block.getLocation(), playerData.lastClaim);
        if (claim != null) {
            minLavaDistance = 3;
        }

        //otherwise no wilderness dumping in creative mode worlds
        else if (instance.creativeRulesApply(block.getLocation())) {
            if (block.getY() >= instance.getSeaLevel(block.getWorld()) - 5 && !player.hasPermission("griefprevention.lava")) {
                if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoWildernessBuckets);
                    bucketEvent.setCancelled(true);
                    return;
                }
            }
        }

        //lava buckets can't be dumped near other players unless pvp is on
        if (!doesAllowLavaProximityInWorld(block.getWorld()) && !player.hasPermission("griefprevention.lava")) {
            if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {
                List<Player> players = block.getWorld().getPlayers();
                for (Player otherPlayer : players) {
                    Location location = otherPlayer.getLocation();
                    if (!otherPlayer.equals(player) && otherPlayer.getGameMode() == GameMode.SURVIVAL && player.canSee(otherPlayer) && block.getY() >= location.getBlockY() - 1 && location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoLavaNearOtherPlayer, "another player");
                        bucketEvent.setCancelled(true);
                        return;
                    }
                }
            }
        }

        //log any suspicious placements (check sea level, world type, and adjacent blocks)
        if (block.getY() >= instance.getSeaLevel(block.getWorld()) - 5 && !player.hasPermission("griefprevention.lava") && block.getWorld().getEnvironment() != Environment.NETHER) {
            //if certain blocks are nearby, it's less suspicious and not worth logging
            Set<Material> exclusionAdjacentTypes;
            if (bucketEvent.getBucket() == Material.WATER_BUCKET)
                exclusionAdjacentTypes = this.commonAdjacentBlocks_water;
            else
                exclusionAdjacentTypes = this.commonAdjacentBlocks_lava;

            boolean makeLogEntry = true;
            BlockFace[] adjacentDirections = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN};
            for (BlockFace direction : adjacentDirections) {
                Material adjacentBlockType = block.getRelative(direction).getType();
                if (exclusionAdjacentTypes.contains(adjacentBlockType)) {
                    makeLogEntry = false;
                    break;
                }
            }

            if (makeLogEntry) {
                EterniaKamui.AddLogEntry(player.getName() + " placed suspicious " + bucketEvent.getBucket().name() + " @ " + EterniaKamui.getfriendlyLocationString(block.getLocation()), CustomLogEntryTypes.SuspiciousActivity, true);
            }
        }
    }

    private boolean doesAllowLavaProximityInWorld(World world) {
        if (EterniaKamui.instance.pvpRulesApply(world)) {
            return EterniaKamui.instance.config_pvp_allowLavaNearPlayers;
        } else {
            return EterniaKamui.instance.config_pvp_allowLavaNearPlayers_NonPvp;
        }
    }

    //see above
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent bucketEvent) {
        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked();

        if (!instance.claimsEnabledForWorld(block.getWorld())) return;

        //make sure the player is allowed to build at the location
        String noBuildReason = instance.allowBuild(player, block.getLocation(), Material.AIR);
        if (noBuildReason != null) {
            //exemption for cow milking (permissions will be handled by player interact with entity event instead)
            Material blockType = block.getType();
            if (blockType == Material.AIR)
                return;
            if (blockType.isSolid()) {
                BlockData blockData = block.getBlockData();
                if (!(blockData instanceof Waterlogged) || !((Waterlogged) blockData).isWaterlogged())
                    return;
            }

            EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
            bucketEvent.setCancelled(true);
        }
    }

    //when a player interacts with the world
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        //not interested in left-click-on-air actions
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock(); //null returned here means interacting with air

        Material clickedBlockType;
        if (clickedBlock != null) {
            clickedBlockType = clickedBlock.getType();
        } else {
            clickedBlockType = Material.AIR;
        }

        PlayerData playerData = null;

        //Turtle eggs
        if (action == Action.PHYSICAL) {
            if (clickedBlockType != Material.TURTLE_EGG)
                return;
            playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noAccessReason = claim.allowBreak(player, clickedBlockType);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        //don't care about left-clicking on most blocks, this is probably a break action
        if (action == Action.LEFT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.getY() < clickedBlock.getWorld().getMaxHeight() - 1 || event.getBlockFace() != BlockFace.UP) {
                Block adjacentBlock = clickedBlock.getRelative(event.getBlockFace());
                byte lightLevel = adjacentBlock.getLightFromBlocks();
                if (lightLevel == 15 && adjacentBlock.getType() == Material.FIRE) {
                    if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
                    if (claim != null) {
                        playerData.lastClaim = claim;

                        String noBuildReason = claim.allowBuild(player, Material.AIR);
                        if (noBuildReason != null) {
                            event.setCancelled(true);
                            EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                            player.sendBlockChange(adjacentBlock.getLocation(), adjacentBlock.getType(), adjacentBlock.getData());
                            return;
                        }
                    }
                }
            }

            //exception for blocks on a specific watch list
            if (!this.onLeftClickWatchList(clickedBlockType)) {
                return;
            }
        }

        if (clickedBlock != null && EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && (event.getAction() == Action.RIGHT_CLICK_BLOCK) && (clickedBlock.getType() == Material.PLAYER_HEAD)) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());

            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noContainersReason = claim.allowContainers(player);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noContainersReason);
                    return;
                }
            }
        }
        
        //apply rules for containers and crafting blocks
        if (clickedBlock != null && EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && (
                event.getAction() == Action.RIGHT_CLICK_BLOCK && (
                        (this.isInventoryHolder(clickedBlock) && clickedBlock.getType() != Material.LECTERN) ||
                                clickedBlockType == Material.CAULDRON ||
                                clickedBlockType == Material.JUKEBOX ||
                                clickedBlockType == Material.ANVIL ||
                                clickedBlockType == Material.CHIPPED_ANVIL ||
                                clickedBlockType == Material.DAMAGED_ANVIL ||
                                clickedBlockType == Material.CAKE ||
                                clickedBlockType == Material.SWEET_BERRY_BUSH ||
                                clickedBlockType == Material.BEE_NEST ||
                                clickedBlockType == Material.BEEHIVE ||
                                clickedBlockType == Material.BEACON ||
                                clickedBlockType == Material.BELL ||
                                clickedBlockType == Material.STONECUTTER ||
                                clickedBlockType == Material.GRINDSTONE ||
                                clickedBlockType == Material.CARTOGRAPHY_TABLE ||
                                clickedBlockType == Material.LOOM ||
                                clickedBlockType == Material.RESPAWN_ANCHOR
                ))) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());

            //block container use during pvp combat, same reason
            if (playerData.inPvpCombat()) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }

            //otherwise check permissions for the claim the player is in
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noContainersReason = claim.allowContainers(player);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noContainersReason);
                    return;
                }
            }

            //if the event hasn't been cancelled, then the player is allowed to use the container
            //so drop any pvp protection
            if (playerData.pvpImmune) {
                playerData.pvpImmune = false;
                EterniaKamui.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }

        //otherwise apply rules for doors and beds, if configured that way
        else if (clickedBlock != null &&
                (EterniaKamui.getBool(Booleans.CLAIMS_LOCK_WOODEN_DOORS) && Tag.WOODEN_DOORS.isTagged(clickedBlockType) ||
                        EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_BUTTONS_SWITCHES) && Tag.BEDS.isTagged(clickedBlockType) ||
                        EterniaKamui.getBool(Booleans.CLAIMS_LOCK_TRAP_DOORS) && Tag.WOODEN_TRAPDOORS.isTagged(clickedBlockType) ||
                        instance.config_claims_lecternReadingRequiresAccessTrust && clickedBlockType == Material.LECTERN ||
                        EterniaKamui.getBool(Booleans.CLAIMS_LOCK_FENCE_GATES) && Tag.FENCE_GATES.isTagged(clickedBlockType))) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noAccessReason = claim.allowAccess(player);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noAccessReason);
                }
            }
        }

        //otherwise apply rules for buttons and switches
        else if (clickedBlock != null && EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_BUTTONS_SWITCHES) && (Tag.BUTTONS.isTagged(clickedBlockType) || clickedBlockType == Material.LEVER)) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noAccessReason = claim.allowAccess(player);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noAccessReason);
                }
            }
        }

        //otherwise apply rule for cake
        else if (clickedBlock != null && EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_THEFT) && clickedBlockType == Material.CAKE) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                String noContainerReason = claim.allowAccess(player);
                if (noContainerReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noContainerReason);
                }
            }
        }

        //apply rule for note blocks and repeaters and daylight sensors //RoboMWM: Include flower pots
        else if (clickedBlock != null &&
                (
                        clickedBlockType == Material.NOTE_BLOCK ||
                                clickedBlockType == Material.REPEATER ||
                                clickedBlockType == Material.DRAGON_EGG ||
                                clickedBlockType == Material.DAYLIGHT_DETECTOR ||
                                clickedBlockType == Material.COMPARATOR ||
                                clickedBlockType == Material.REDSTONE_WIRE ||
                                Tag.FLOWER_POTS.isTagged(clickedBlockType)
                )) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
            if (claim != null) {
                String noBuildReason = claim.allowBuild(player, clickedBlockType);
                if (noBuildReason != null) {
                    event.setCancelled(true);
                    EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                }
            }
        }

        //otherwise handle right click (shovel, string, bonemeal) //RoboMWM: flint and steel
        else {
            //ignore all actions except right-click on a block or in the air
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            //what's the player holding?
            EquipmentSlot hand = event.getHand();
            ItemStack itemInHand = instance.getItemInHand(player, hand);
            Material materialInHand = itemInHand.getType();

            Set<Material> spawn_eggs = new HashSet<>();
            Set<Material> dyes = new HashSet<>();

            for (Material material : Material.values()) {
                if (material.isLegacy()) continue;
                if (material.name().endsWith("_SPAWN_EGG"))
                    spawn_eggs.add(material);
                else if (material.name().endsWith("_DYE"))
                    dyes.add(material);
            }


            //if it's bonemeal, armor stand, spawn egg, etc - check for build permission //RoboMWM: also check flint and steel to stop TNT ignition
            if (clickedBlock != null && (materialInHand == Material.BONE_MEAL
                    || materialInHand == Material.ARMOR_STAND
                    || (spawn_eggs.contains(materialInHand) && EterniaKamui.getBool(Booleans.CLAIMS_PREVENT_GLOBAL_MONSTER_EGGS))
                    || materialInHand == Material.END_CRYSTAL
                    || materialInHand == Material.FLINT_AND_STEEL
                    || dyes.contains(materialInHand))) {
                String noBuildReason = instance
                        .allowBuild(player, clickedBlock
                                        .getLocation(),
                                clickedBlockType);
                if (noBuildReason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                    event.setCancelled(true);
                }

                return;
            } else if (clickedBlock != null && Tag.ITEMS_BOATS.isTagged(materialInHand)) {
                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
                if (claim != null) {
                    String reason = claim.allowContainers(player);
                    if (reason != null) {
                        EterniaKamui.sendMessage(player, TextMode.Err, reason);
                        event.setCancelled(true);
                    }
                }

                return;
            }

            //survival world minecart placement requires container trust, which is the permission required to remove the minecart later
            else if (clickedBlock != null &&
                    (materialInHand == Material.MINECART ||
                            materialInHand == Material.FURNACE_MINECART ||
                            materialInHand == Material.CHEST_MINECART ||
                            materialInHand == Material.TNT_MINECART ||
                            materialInHand == Material.HOPPER_MINECART) &&
                    !instance.creativeRulesApply(clickedBlock.getLocation())) {
                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
                if (claim != null) {
                    String reason = claim.allowContainers(player);
                    if (reason != null) {
                        EterniaKamui.sendMessage(player, TextMode.Err, reason);
                        event.setCancelled(true);
                    }
                }

                return;
            }

            //if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
            else if (clickedBlock != null && (materialInHand == Material.MINECART ||
                    materialInHand == Material.FURNACE_MINECART ||
                    materialInHand == Material.CHEST_MINECART ||
                    materialInHand == Material.TNT_MINECART ||
                    materialInHand == Material.ARMOR_STAND ||
                    materialInHand == Material.ITEM_FRAME ||
                    spawn_eggs.contains(materialInHand) ||
                    materialInHand == Material.INFESTED_STONE ||
                    materialInHand == Material.INFESTED_COBBLESTONE ||
                    materialInHand == Material.INFESTED_STONE_BRICKS ||
                    materialInHand == Material.INFESTED_MOSSY_STONE_BRICKS ||
                    materialInHand == Material.INFESTED_CRACKED_STONE_BRICKS ||
                    materialInHand == Material.INFESTED_CHISELED_STONE_BRICKS ||
                    materialInHand == Material.HOPPER_MINECART) &&
                    instance.creativeRulesApply(clickedBlock.getLocation())) {
                //player needs build permission at this location
                String noBuildReason = instance.allowBuild(player, clickedBlock.getLocation(), Material.MINECART);
                if (noBuildReason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                    event.setCancelled(true);
                    return;
                }

                //enforce limit on total number of entities in this claim
                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
                if (claim == null) return;

                String noEntitiesReason = claim.allowMoreEntities(false);
                if (noEntitiesReason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, noEntitiesReason);
                    event.setCancelled(true);
                    return;
                }

                return;
            }

            //if he's investigating a claim
            else if (materialInHand == EterniaKamui.getMaterials(Materials.INVESTIGATION_TOOL) && hand == EquipmentSlot.HAND) {
                //if claims are disabled in this world, do nothing
                if (!instance.claimsEnabledForWorld(player.getWorld())) return;

                //if holding shift (sneaking), show all claims in area
                if (player.isSneaking() && player.hasPermission("griefprevention.visualizenearbyclaims")) {
                    //find nearby claims
                    Set<Claim> claims = this.dataStore.getNearbyClaims(player.getLocation());

                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, null, claims, true);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    //visualize boundaries
                    Visualization visualization = Visualization.fromClaims(claims, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, claims, true));

                    Visualization.Apply(player, visualization);

                    EterniaKamui.sendMessage(player, TextMode.Info, Messages.ShowNearbyClaims, String.valueOf(claims.size()));

                    return;
                }

                //FEATURE: shovel and stick can be used from a distance away
                if (action == Action.RIGHT_CLICK_AIR) {
                    //try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player);
                    clickedBlockType = clickedBlock.getType();
                }

                //if no block, stop here
                if (clickedBlock == null) {
                    return;
                }

                //air indicates too far away
                if (clickedBlockType == Material.AIR) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.TooFarAway);

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, null, Collections.emptySet()));

                    Visualization.Revert(player);
                    return;
                }

                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);

                //no claim case
                if (claim == null) {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, null);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    EterniaKamui.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, null, Collections.emptySet()));

                    Visualization.Revert(player);
                }

                //claim case
                else {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, claim);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    playerData.lastClaim = claim;
                    EterniaKamui.sendMessage(player, TextMode.Info, Messages.BlockClaimed, claim.getOwnerName());

                    //visualize boundary
                    Visualization visualization = Visualization.FromClaim(claim, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, claim));

                    Visualization.Apply(player, visualization);

                    if (player.hasPermission("griefprevention.seeclaimsize")) {
                        EterniaKamui.sendMessage(player, TextMode.Info, "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
                    }

                    //if permission, tell about the player's offline time
                    if (!claim.isAdminClaim() && (player.hasPermission("griefprevention.deleteclaims") || player.hasPermission("griefprevention.seeinactivity"))) {
                        if (claim.parent != null) {
                            claim = claim.parent;
                        }
                        Date lastLogin = new Date(Bukkit.getOfflinePlayer(claim.ownerID).getLastPlayed());
                        Date now = new Date();
                        long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24);

                        EterniaKamui.sendMessage(player, TextMode.Info, Messages.PlayerOfflineTime, String.valueOf(daysElapsed));

                        //drop the data we just loaded, if the player isn't online
                        if (instance.getServer().getPlayer(claim.ownerID) == null)
                            this.dataStore.clearCachedPlayerData(claim.ownerID);
                    }
                }

                return;
            }

            //if it's a golden shovel
            else if (materialInHand != EterniaKamui.getMaterials(Materials.MODIFICATION_TOOL) || hand != EquipmentSlot.HAND) return;

            event.setCancelled(true);  //GriefPrevention exclusively reserves this tool  (e.g. no grass path creation for golden shovel)

            //FEATURE: shovel and stick can be used from a distance away
            if (action == Action.RIGHT_CLICK_AIR) {
                //try to find a far away non-air block along line of sight
                clickedBlock = getTargetBlock(player);
                clickedBlockType = clickedBlock.getType();
            }

            //if no block, stop here
            if (clickedBlock == null) {
                return;
            }

            //can't use the shovel from too far away
            if (clickedBlockType == Material.AIR) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.TooFarAway);
                return;
            }

            //if the player is in restore nature mode, do only that
            UUID playerID = player.getUniqueId();
            playerData = this.dataStore.getPlayerData(player.getUniqueId());
            if (playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive) {
                //if the clicked block is in a claim, visualize that claim and deliver an error message
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);
                if (claim != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.BlockClaimed, claim.getOwnerName());
                    Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, claim));

                    Visualization.Apply(player, visualization);

                    return;
                }

                //figure out which chunk to repair
                Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
                //start the repair process

                //set boundaries for processing
                int miny = clickedBlock.getY();

                //if not in aggressive mode, extend the selection down to a little below sea level
                if (!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive)) {
                    if (miny > instance.getSeaLevel(chunk.getWorld()) - 10) {
                        miny = instance.getSeaLevel(chunk.getWorld()) - 10;
                    }
                }

                instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);

                return;
            }

            //if in restore nature fill mode
            if (playerData.shovelMode == ShovelMode.RestoreNatureFill) {
                ArrayList<Material> allowedFillBlocks = new ArrayList<>();
                Environment environment = clickedBlock.getWorld().getEnvironment();
                if (environment == Environment.NETHER) {
                    allowedFillBlocks.add(Material.NETHERRACK);
                } else if (environment == Environment.THE_END) {
                    allowedFillBlocks.add(Material.END_STONE);
                } else {
                    allowedFillBlocks.add(Material.GRASS);
                    allowedFillBlocks.add(Material.DIRT);
                    allowedFillBlocks.add(Material.STONE);
                    allowedFillBlocks.add(Material.SAND);
                    allowedFillBlocks.add(Material.SANDSTONE);
                    allowedFillBlocks.add(Material.ICE);
                }

                int maxHeight = clickedBlock.getY();
                int minx = clickedBlock.getX() - playerData.fillRadius;
                int maxx = clickedBlock.getX() + playerData.fillRadius;
                int minz = clickedBlock.getZ() - playerData.fillRadius;
                int maxz = clickedBlock.getZ() + playerData.fillRadius;
                int minHeight = maxHeight - 10;
                if (minHeight < 0) minHeight = 0;

                Claim cachedClaim = null;
                for (int x = minx; x <= maxx; x++) {
                    for (int z = minz; z <= maxz; z++) {
                        //circular brush
                        Location location = new Location(clickedBlock.getWorld(), x, clickedBlock.getY(), z);
                        if (location.distance(clickedBlock.getLocation()) > playerData.fillRadius) continue;

                        //default fill block is initially the first from the allowed fill blocks list above
                        Material defaultFiller = allowedFillBlocks.get(0);

                        //prefer to use the block the player clicked on, if it's an acceptable fill block
                        if (allowedFillBlocks.contains(clickedBlock.getType())) {
                            defaultFiller = clickedBlock.getType();
                        }

                        //if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
                        else if (clickedBlock.getType() == Material.WATER) {
                            Block block = clickedBlock.getWorld().getBlockAt(clickedBlock.getLocation());
                            while (!allowedFillBlocks.contains(block.getType()) && block.getY() > clickedBlock.getY() - 10) {
                                block = block.getRelative(BlockFace.DOWN);
                            }
                            if (allowedFillBlocks.contains(block.getType())) {
                                defaultFiller = block.getType();
                            }
                        }

                        //fill bottom to top
                        for (int y = minHeight; y <= maxHeight; y++) {
                            Block block = clickedBlock.getWorld().getBlockAt(x, y, z);

                            //respect claims
                            Claim claim = this.dataStore.getClaimAt(block.getLocation(), cachedClaim);
                            if (claim != null) {
                                cachedClaim = claim;
                                break;
                            }

                            //only replace air, spilling water, snow, long grass
                            if (block.getType() == Material.AIR || block.getType() == Material.SNOW || (block.getType() == Material.WATER && ((Levelled) block.getBlockData()).getLevel() != 0) || block.getType() == Material.GRASS) {
                                //if the top level, always use the default filler picked above
                                if (y == maxHeight) {
                                    block.setType(defaultFiller);
                                }

                                //otherwise look to neighbors for an appropriate fill block
                                else {
                                    Block eastBlock = block.getRelative(BlockFace.EAST);
                                    Block westBlock = block.getRelative(BlockFace.WEST);
                                    Block northBlock = block.getRelative(BlockFace.NORTH);
                                    Block southBlock = block.getRelative(BlockFace.SOUTH);

                                    //first, check lateral neighbors (ideally, want to keep natural layers)
                                    if (allowedFillBlocks.contains(eastBlock.getType())) {
                                        block.setType(eastBlock.getType());
                                    } else if (allowedFillBlocks.contains(westBlock.getType())) {
                                        block.setType(westBlock.getType());
                                    } else if (allowedFillBlocks.contains(northBlock.getType())) {
                                        block.setType(northBlock.getType());
                                    } else if (allowedFillBlocks.contains(southBlock.getType())) {
                                        block.setType(southBlock.getType());
                                    }

                                    //if all else fails, use the default filler selected above
                                    else {
                                        block.setType(defaultFiller);
                                    }
                                }
                            }
                        }
                    }
                }

                return;
            }

            //if the player doesn't have claims permission, don't do anything
            if (!player.hasPermission("griefprevention.createclaims")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoCreateClaimPermission);
                return;
            }

            //if he's resizing a claim and that claim hasn't been deleted since he started resizing it
            if (playerData.claimResizing != null && playerData.claimResizing.inDataStore) {
                if (clickedBlock.getLocation().equals(playerData.lastShovelLocation)) return;

                //figure out what the coords of his new claim would be
                int newx1, newx2, newz1, newz2;
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX()) {
                    newx1 = clickedBlock.getX();
                    newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
                } else {
                    newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
                    newx2 = clickedBlock.getX();
                }

                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ()) {
                    newz1 = clickedBlock.getZ();
                    newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
                } else {
                    newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
                    newz2 = clickedBlock.getZ();
                }

                this.dataStore.resizeClaimWithChecks(player, playerData, newx1, newx2, newz1, newz2);

                return;
            }

            //otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), playerData.lastClaim);

            //if within an existing claim, he's not creating a new one
            if (claim != null) {
                //if the player has permission to edit the claim or subdivision
                String noEditReason = claim.allowEdit(player);
                if (noEditReason == null) {
                    //if he clicked on a corner, start resizing it
                    if ((clickedBlock.getX() == claim.getLesserBoundaryCorner().getBlockX() || clickedBlock.getX() == claim.getGreaterBoundaryCorner().getBlockX()) && (clickedBlock.getZ() == claim.getLesserBoundaryCorner().getBlockZ() || clickedBlock.getZ() == claim.getGreaterBoundaryCorner().getBlockZ())) {
                        playerData.claimResizing = claim;
                        playerData.lastShovelLocation = clickedBlock.getLocation();
                        EterniaKamui.sendMessage(player, TextMode.Instr, Messages.ResizeStart);
                    }

                    //if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
                    else if (playerData.shovelMode == ShovelMode.Subdivide) {
                        //if it's the first click, he's trying to start a new subdivision
                        if (playerData.lastShovelLocation == null) {
                            //if the clicked claim was a subdivision, tell him he can't start a new subdivision here
                            if (claim.parent != null) {
                                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapSubdivision);
                            }

                            //otherwise start a new subdivision
                            else {
                                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SubdivisionStart);
                                playerData.lastShovelLocation = clickedBlock.getLocation();
                                playerData.claimSubdividing = claim;
                            }
                        }

                        //otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
                        else {
                            //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                            if (!playerData.lastShovelLocation.getWorld().equals(clickedBlock.getWorld())) {
                                playerData.lastShovelLocation = null;
                                this.onPlayerInteract(event);
                                return;
                            }

                            //try to create a new claim (will return null if this subdivision overlaps another)
                            CreateClaimResult result = this.dataStore.createClaim(
                                    player.getWorld(),
                                    playerData.lastShovelLocation.getBlockX(), clickedBlock.getX(),
                                    playerData.lastShovelLocation.getBlockZ(), clickedBlock.getZ(),
                                    null,  //owner is not used for subdivisions
                                    playerData.claimSubdividing,
                                    null, player);

                            //if it didn't succeed, tell the player why
                            if (!result.succeeded) {
                                EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateSubdivisionOverlap);

                                Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());

                                // alert plugins of a visualization
                                Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, result.claim));

                                Visualization.Apply(player, visualization);

                                return;
                            }

                            //otherwise, advise him on the /trust command and show him his new subdivision
                            else {
                                EterniaKamui.sendMessage(player, TextMode.Success, Messages.SubdivisionSuccess);
                                Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());

                                // alert plugins of a visualization
                                Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, result.claim));

                                Visualization.Apply(player, visualization);
                                playerData.lastShovelLocation = null;
                                playerData.claimSubdividing = null;
                            }
                        }
                    }

                    //otherwise tell him he can't create a claim here, and show him the existing claim
                    //also advise him to consider /abandonclaim or resizing the existing claim
                    else {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlap);
                        Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());

                        // alert plugins of a visualization
                        Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, claim));

                        Visualization.Apply(player, visualization);
                    }
                }

                //otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
                else {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName());
                    Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, claim));

                    Visualization.Apply(player, visualization);
                }

                return;
            }

            //otherwise, the player isn't in an existing claim!

            //if he hasn't already start a claim with a previous shovel action
            Location lastShovelLocation = playerData.lastShovelLocation;
            if (lastShovelLocation == null) {
                //if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
                if (!instance.claimsEnabledForWorld(player.getWorld())) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
                    return;
                }

                //if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
                if (EterniaKamui.getInt(Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER) > 0 &&
                        !player.hasPermission("griefprevention.overrideclaimcountlimit") &&
                        playerData.getClaims().size() >= EterniaKamui.getInt(Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER)) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
                    return;
                }

                //remember it, and start him on the new claim
                playerData.lastShovelLocation = clickedBlock.getLocation();
                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.ClaimStart);

                //show him where he's working
                Claim newClaim = new Claim(clickedBlock.getLocation(), clickedBlock.getLocation(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
                Visualization visualization = Visualization.FromClaim(newClaim, clickedBlock.getY(), VisualizationType.RestoreNature, player.getLocation());

                // alert plugins of a visualization
                Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, newClaim));

                Visualization.Apply(player, visualization);
            }

            //otherwise, he's trying to finish creating a claim by setting the other boundary corner
            else {
                //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                if (!lastShovelLocation.getWorld().equals(clickedBlock.getWorld())) {
                    playerData.lastShovelLocation = null;
                    this.onPlayerInteract(event);
                    return;
                }

                //apply pvp rule
                if (playerData.inPvpCombat()) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoClaimDuringPvP);
                    return;
                }

                //apply minimum claim dimensions rule
                int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getX()) + 1;
                int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getZ()) + 1;

                if (playerData.shovelMode != ShovelMode.Admin) {
                    if (newClaimWidth < EterniaKamui.getInt(Integers.CLAIMS_MIN_WIDTH) || newClaimHeight < EterniaKamui.getInt(Integers.CLAIMS_MIN_WIDTH)) {
                        //this IF block is a workaround for craftbukkit bug which fires two events for one interaction
                        if (newClaimWidth != 1 && newClaimHeight != 1) {
                            EterniaKamui.sendMessage(player, TextMode.Err, Messages.NewClaimTooNarrow, String.valueOf(EterniaKamui.getInt(Integers.CLAIMS_MIN_WIDTH)));
                        }
                        return;
                    }

                    int newArea = newClaimWidth * newClaimHeight;
                    if (newArea < EterniaKamui.getInt(Integers.CLAIMS_MIN_AREA)) {
                        if (newArea != 1) {
                            EterniaKamui.sendMessage(player, TextMode.Err, Messages.ResizeClaimInsufficientArea, String.valueOf(EterniaKamui.getInt(Integers.CLAIMS_MIN_AREA)));
                        }

                        return;
                    }
                }

                //if not an administrative claim, verify the player has enough claim blocks for this new claim
                if (playerData.shovelMode != ShovelMode.Admin) {
                    int newClaimArea = newClaimWidth * newClaimHeight;
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    if (newClaimArea > remainingBlocks) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks, String.valueOf(newClaimArea - remainingBlocks));
                        instance.dataStore.tryAdvertiseAdminAlternatives(player);
                        return;
                    }
                } else {
                    playerID = null;
                }

                //try to create a new claim
                CreateClaimResult result = this.dataStore.createClaim(
                        player.getWorld(),
                        lastShovelLocation.getBlockX(), clickedBlock.getX(),
                        lastShovelLocation.getBlockZ(), clickedBlock.getZ(),
                        playerID,
                        null, null,
                        player);

                //if it didn't succeed, tell the player why
                if (!result.succeeded) {
                    if (result.claim != null) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);

                        Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());

                        // alert plugins of a visualization
                        Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, result.claim));

                        Visualization.Apply(player, visualization);
                    } else {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapRegion);
                    }

                }

                //otherwise, advise him on the /trust command and show him his new claim
                else {
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());

                    // alert plugins of a visualization
                    Bukkit.getPluginManager().callEvent(new VisualizationEvent(player, visualization, result.claim));

                    Visualization.Apply(player, visualization);
                    playerData.lastShovelLocation = null;

                    //if it's a big claim, tell the player about subdivisions
                    if (!player.hasPermission("griefprevention.adminclaims") && result.claim.getArea() >= 1000) {
                        EterniaKamui.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
                        EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L, DataStore.SUBDIVISION_VIDEO_URL);
                    }

                }
            }
        }
    }

    // Stops an untrusted player from removing a book from a lectern
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTakeBook(PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(event.getLectern().getLocation(), playerData.lastClaim);
        if (claim != null) {
            playerData.lastClaim = claim;
            String noContainerReason = claim.allowContainers(player);
            if (noContainerReason != null) {
                event.setCancelled(true);
                player.closeInventory();
                EterniaKamui.sendMessage(player, TextMode.Err, noContainerReason);
            }
        }
    }

    //determines whether a block type is an inventory holder.  uses a caching strategy to save cpu time
    private final ConcurrentHashMap<Material, Boolean> inventoryHolderCache = new ConcurrentHashMap<>();

    private boolean isInventoryHolder(Block clickedBlock) {

        Material cacheKey = clickedBlock.getType();
        Boolean cachedValue = this.inventoryHolderCache.get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;

        } else {
            boolean isHolder = clickedBlock.getState() instanceof InventoryHolder;
            this.inventoryHolderCache.put(cacheKey, isHolder);
            return isHolder;
        }
    }

    private boolean onLeftClickWatchList(Material material) {
        switch (material) {
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case STONE_BUTTON:
            case LEVER:
            case REPEATER:
            case CAKE:
            case DRAGON_EGG:
                return true;
            default:
                return false;
        }
    }

    static Block getTargetBlock(Player player) throws IllegalStateException {
        Location eye = player.getEyeLocation();
        Material eyeMaterial = eye.getBlock().getType();
        boolean passThroughWater = (eyeMaterial == Material.WATER);
        BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), 100);
        Block result = player.getLocation().getBlock().getRelative(BlockFace.UP);
        while (iterator.hasNext()) {
            result = iterator.next();
            Material type = result.getType();
            if (type != Material.AIR &&
                    (!passThroughWater || type != Material.WATER) &&
                    type != Material.GRASS &&
                    type != Material.SNOW) return result;
        }

        return result;
    }
}
