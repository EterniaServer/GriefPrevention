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

import br.com.eterniaserver.eterniakamui.configurations.configs.ConfigsCfg;
import br.com.eterniaserver.eterniakamui.configurations.locales.ErrorsCfg;
import br.com.eterniaserver.eterniakamui.configurations.locales.MessagesCfg;
import br.com.eterniaserver.eterniakamui.enums.*;
import br.com.eterniaserver.eterniakamui.events.PreventBlockBreakEvent;
import br.com.eterniaserver.eterniakamui.events.SaveTrappedPlayerEvent;
import br.com.eterniaserver.eterniakamui.events.TrustChangedEvent;
import br.com.eterniaserver.eterniakamui.handlers.BlockEventHandler;
import br.com.eterniaserver.eterniakamui.handlers.EconomyHandler;
import br.com.eterniaserver.eterniakamui.handlers.EntityEventHandler;
import br.com.eterniaserver.eterniakamui.handlers.PlayerEventHandler;
import br.com.eterniaserver.eterniakamui.objects.BlockSnapshot;
import br.com.eterniaserver.eternialib.CommandManager;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EterniaKamui extends JavaPlugin {

    private static final String[] strings = new String[Strings.values().length];
    private static final Integer[] integers = new Integer[Integers.values().length];
    private static final Boolean[] booleans = new Boolean[Booleans.values().length];
    private static final String[] messages = new String[Messages.values().length];
    private static final Double[] doubles = new Double[Doubles.values().length];
    private static final Material[] materials = new Material[Materials.values().length];

    private static final Map<World, ClaimsMode> claimsWorldModes = new ConcurrentHashMap<>();

    private static final List<PendingItemProtection> pendingItemWatchList = new ArrayList<>();


    //for convenience, a reference to the instance of this plugin
    public static EterniaKamui instance;

    //for logging to the console and log file
    private static Logger log;

    //this handles data storage, like player and region data
    public DataStore dataStore;


    //log entry manager for GP's custom log files
    CustomLogger customLogger;

    private EconomyHandler economyHandler;


    public ArrayList<String> config_claims_commandsRequiringAccessTrust; //the list of slash commands requiring access trust when in a claim
    public boolean config_claims_supplyPlayerManual;                //whether to give new players a book with land claim help in it
    public int config_claims_manualDeliveryDelaySeconds;            //how long to wait before giving a book to a new player

    public boolean config_claims_firespreads;                        //whether fire will spread in claims
    public boolean config_claims_firedamages;                        //whether fire will damage in claims

    public boolean config_claims_lecternReadingRequiresAccessTrust;                    //reading lecterns requires access trust

    HashMap<World, Boolean> config_pvp_specifiedWorlds;                //list of worlds where pvp anti-grief rules apply, according to the config file
    public boolean config_pvp_protectFreshSpawns;                    //whether to make newly spawned players immune until they pick up an item
    public boolean config_pvp_punishLogout;                            //whether to kill players who log out during PvP combat
    public int config_pvp_combatTimeoutSeconds;                        //how long combat is considered to continue after the most recent damage
    public boolean config_pvp_allowCombatItemDrop;                    //whether a player can drop items during combat to hide them
    public ArrayList<String> config_pvp_blockedCommands;            //list of commands which may not be used during pvp combat
    public boolean config_pvp_noCombatInPlayerLandClaims;            //whether players may fight in player-owned land claims
    public boolean config_pvp_noCombatInAdminLandClaims;            //whether players may fight in admin-owned land claims
    public boolean config_pvp_noCombatInAdminSubdivisions;          //whether players may fight in subdivisions of admin-owned land claims
    public boolean config_pvp_allowLavaNearPlayers;                 //whether players may dump lava near other players in pvp worlds
    public boolean config_pvp_allowLavaNearPlayers_NonPvp;            //whather this applies in non-PVP rules worlds <ArchdukeLiamus>
    public boolean config_pvp_allowFireNearPlayers;                 //whether players may start flint/steel fires near other players in pvp worlds
    public boolean config_pvp_allowFireNearPlayers_NonPvp;            //whether this applies in non-PVP rules worlds <ArchdukeLiamus>
    public boolean config_pvp_protectPets;                          //whether players may damage pets outside of land claims in pvp worlds

    public boolean config_lockDeathDropsInPvpWorlds;                //whether players' dropped on death items are protected in pvp worlds
    public boolean config_lockDeathDropsInNonPvpWorlds;             //whether players' dropped on death items are protected in non-pvp worlds

    public int config_economy_claimBlocksMaxBonus;                  //max "bonus" blocks a player can buy.  set to zero for no limit.
    public double config_economy_claimBlocksPurchaseCost;            //cost to purchase a claim block.  set to zero to disable purchase.
    public double config_economy_claimBlocksSellValue;                //return on a sold claim block.  set to zero to disable sale.

    public boolean config_blockClaimExplosions;                     //whether explosions may destroy claimed blocks
    public boolean config_blockSurfaceCreeperExplosions;            //whether creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceOtherExplosions;                //whether non-creeper explosions near or above the surface destroy blocks
    public boolean config_blockSkyTrees;                            //whether players can build trees on platforms in the sky

    public boolean config_fireSpreads;                                //whether fire spreads outside of claims
    public boolean config_fireDestroys;                                //whether fire destroys blocks outside of claims

    public boolean config_endermenMoveBlocks;                        //whether or not endermen may move blocks around
    public boolean config_claims_ravagersBreakBlocks;                //whether or not ravagers may break blocks in claims
    public boolean config_silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean config_creaturesTrampleCrops;                    //whether or not non-player entities may trample crops
    public boolean config_rabbitsEatCrops;                          //whether or not rabbits may eat crops
    public boolean config_zombiesBreakDoors;                        //whether or not hard-mode zombies may break down wooden doors

    public HashMap<String, Integer> config_seaLevelOverride;        //override for sea level, because bukkit doesn't report the right value for all situations

    public boolean config_limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside
    public PistonMode config_pistonMovement;

    public int config_advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int config_advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days

    //custom log settings
    public int config_logs_daysToKeep;
    public boolean config_logs_socialEnabled;
    public boolean config_logs_suspiciousEnabled;
    public boolean config_logs_adminEnabled;
    public boolean config_logs_debugEnabled;

    public static String getString(Strings entry) {
        return strings[entry.ordinal()];
    }

    public static int getInt(Integers entry) {
        return integers[entry.ordinal()];
    }

    public static boolean getBool(Booleans entry) {
        return booleans[entry.ordinal()];
    }

    public static double getDouble(Doubles entry) {
        return doubles[entry.ordinal()];
    }

    public static Material getMaterials(Materials entry) {
        return materials[entry.ordinal()];
    }

    public static ClaimsMode getClaimsWorldModes(World world) {
        return claimsWorldModes.get(world);
    }

    public static List<PendingItemProtection> getPendingItemWatchList() {
        return pendingItemWatchList;
    }

    //adds a server log entry
    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType, boolean excludeFromServerLogs) {
        if (customLogType != null && EterniaKamui.instance.customLogger != null) {
            EterniaKamui.instance.customLogger.AddEntry(entry, customLogType);
        }
        if (!excludeFromServerLogs) log.info(entry);
    }

    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType) {
        AddLogEntry(entry, customLogType, false);
    }

    public static synchronized void AddLogEntry(String entry) {
        AddLogEntry(entry, CustomLogEntryTypes.Debug);
    }

    //initializes well...   everything
    public void onEnable() {
        log = this.getLogger();

        instance = this;

        PluginVars.worlds.add("world");
        PluginVars.worlds.add("world_nether");
        PluginVars.worlds.add("world_the_end");

        CommandManager.getCommandCompletions().registerStaticCompletion("worldenv", PluginVars.enviroments);
        CommandManager.getCommandCompletions().registerStaticCompletion("worldtyp", PluginVars.types);
        CommandManager.registerCommand(new BaseCmdMultiVerse());
        CommandManager.registerCommand(new BaseCmdFlags());

        this.loadConfig();

        new ErrorsCfg();

        ConfigsCfg configsCfg = new ConfigsCfg(strings, booleans, integers, doubles, materials);
        configsCfg.loadWorldClaimsModeMap(claimsWorldModes, booleans);
        configsCfg.saveConfiguration();

        new MessagesCfg(messages);

        this.customLogger = new CustomLogger();

        AddLogEntry("Finished loading configuration.");

        try {
            this.dataStore = new DatabaseDataStore();
        } catch (Exception e) {
            EterniaKamui.AddLogEntry("Aconteceu algum problema na hora de carregar a database, por isso o EterniaKamui foi desativado.");
            EterniaKamui.AddLogEntry(e.getMessage());
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //unless claim block accrual is disabled, start the recurring per 10 minute event to give claim blocks to online players
        //20L ~ 1 second
        if (getInt(Integers.CLAIMS_BLOCKS_ACCRUED_PER_HOUR) > 0) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this);
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        this.getServer().getScheduler().scheduleSyncDelayedTask(EterniaKamui.instance, task, 20L * 60 * 2);

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60, 20L * config_advanced_claim_expiration_check_rate);

        //register for events
        PluginManager pluginManager = this.getServer().getPluginManager();

        //player events
        PlayerEventHandler playerEventHandler = new PlayerEventHandler(this.dataStore, this);
        pluginManager.registerEvents(playerEventHandler, this);

        //block events
        BlockEventHandler blockEventHandler = new BlockEventHandler(this.dataStore);
        pluginManager.registerEvents(blockEventHandler, this);

        //entity events
        EntityEventHandler entityEventHandler = new EntityEventHandler(this.dataStore, this);
        pluginManager.registerEvents(entityEventHandler, this);

        economyHandler = new EconomyHandler(this);
        pluginManager.registerEvents(economyHandler, this);

        //cache offline players
        OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(offlinePlayers, this.playerNameToIDMap);
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        AddLogEntry("Boot finished.");

    }

    synchronized public static String getMessage(Messages messageID, String... args) {
        String message = messages[messageID.ordinal()];

        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            message = message.replace("{" + i + "}", param);
        }

        return message;
    }


    private void loadConfig() {
        //load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();
        outConfig.options().header("Default values are perfect for most servers.  If you want to customize and have a question, look for the answer here first: http://dev.bukkit.org/bukkit-plugins/grief-prevention/pages/setup-and-configuration/");

        //read configuration settings (note defaults)

        //get (deprecated node) claims world names from the config file
        List<World> worlds = this.getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");

        //validate that list
        for (int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++) {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if (world == null) {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated) pvp fire placement proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowFireNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers", false);
        //get (deprecated) pvp lava dump proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowLavaNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers", false);


        //pvp worlds list
        this.config_pvp_specifiedWorlds = new HashMap<>();
        for (World world : worlds) {
            boolean pvpWorld = config.getBoolean("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), world.getPVP());
            this.config_pvp_specifiedWorlds.put(world, pvpWorld);
        }

        //sea level
        this.config_seaLevelOverride = new HashMap<>();
        for (World element : worlds) {
            int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + element.getName(), -1);
            outConfig.set("GriefPrevention.SeaLevelOverrides." + element.getName(), seaLevelOverride);
            this.config_seaLevelOverride.put(element.getName(), seaLevelOverride);
        }

        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");
        this.config_claims_supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        this.config_claims_manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        this.config_claims_ravagersBreakBlocks = config.getBoolean("GriefPrevention.Claims.RavagersBreakBlocks", true);

        this.config_claims_firespreads = config.getBoolean("GriefPrevention.Claims.FireSpreadsInClaims", false);
        this.config_claims_firedamages = config.getBoolean("GriefPrevention.Claims.FireDamagesInClaims", false);
        this.config_claims_lecternReadingRequiresAccessTrust = config.getBoolean("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", true);

        this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");

        this.config_economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);

        this.config_lockDeathDropsInPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", false);
        this.config_lockDeathDropsInNonPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", true);

        this.config_blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        this.config_blockSurfaceCreeperExplosions = config.getBoolean("GriefPrevention.BlockSurfaceCreeperExplosions", true);
        this.config_blockSurfaceOtherExplosions = config.getBoolean("GriefPrevention.BlockSurfaceOtherExplosions", true);
        this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        this.config_limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        this.config_pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean("GriefPrevention.LimitPistonsToLandClaims")) {
            this.config_pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        }
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean("GriefPrevention.CheckPistonMovement")) {
            this.config_pistonMovement = PistonMode.IGNORED;
        }


        this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        this.config_rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);

        this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", true);
        this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", true);
        this.config_pvp_noCombatInAdminSubdivisions = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", true);
        this.config_pvp_allowLavaNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds", true);
        this.config_pvp_allowLavaNearPlayers_NonPvp = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds", false);
        this.config_pvp_allowFireNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds", true);
        this.config_pvp_allowFireNearPlayers_NonPvp = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds", false);
        this.config_pvp_protectPets = config.getBoolean("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", false);

        this.config_advanced_claim_expiration_check_rate = config.getInt("GriefPrevention.Advanced.ClaimExpirationCheckRate", 60);
        this.config_advanced_offlineplayer_cache_days = config.getInt("GriefPrevention.Advanced.OfflinePlayer_cache_days", 90);

        //custom logger settings
        this.config_logs_daysToKeep = config.getInt("GriefPrevention.Abridged Logs.Days To Keep", 7);
        this.config_logs_socialEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Social Activity", true);
        this.config_logs_suspiciousEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity", true);
        this.config_logs_adminEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity", false);
        this.config_logs_debugEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Debug", false);

        outConfig.set("GriefPrevention.Claims.CommandsRequiringAccessTrust", accessTrustSlashCommands);
        outConfig.set("GriefPrevention.Claims.DeliverManuals", config_claims_supplyPlayerManual);
        outConfig.set("GriefPrevention.Claims.ManualDeliveryDelaySeconds", config_claims_manualDeliveryDelaySeconds);
        outConfig.set("GriefPrevention.Claims.RavagersBreakBlocks", config_claims_ravagersBreakBlocks);

        outConfig.set("GriefPrevention.Claims.FireSpreadsInClaims", config_claims_firespreads);
        outConfig.set("GriefPrevention.Claims.FireDamagesInClaims", config_claims_firedamages);
        outConfig.set("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", config_claims_lecternReadingRequiresAccessTrust);

        for (World world : worlds) {
            outConfig.set("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), this.pvpRulesApply(world));
        }
        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", this.config_pvp_noCombatInAdminSubdivisions);
        outConfig.set("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds", this.config_pvp_allowLavaNearPlayers);
        outConfig.set("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds", this.config_pvp_allowLavaNearPlayers_NonPvp);
        outConfig.set("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds", this.config_pvp_allowFireNearPlayers);
        outConfig.set("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds", this.config_pvp_allowFireNearPlayers_NonPvp);
        outConfig.set("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", this.config_pvp_protectPets);

        outConfig.set("GriefPrevention.Economy.ClaimBlocksMaxBonus", this.config_economy_claimBlocksMaxBonus);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);

        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", this.config_lockDeathDropsInPvpWorlds);
        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", this.config_lockDeathDropsInNonPvpWorlds);

        outConfig.set("GriefPrevention.BlockLandClaimExplosions", this.config_blockClaimExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceCreeperExplosions", this.config_blockSurfaceCreeperExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceOtherExplosions", this.config_blockSurfaceOtherExplosions);
        outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);
        outConfig.set("GriefPrevention.LimitTreeGrowth", this.config_limitTreeGrowth);
        outConfig.set("GriefPrevention.PistonMovement", this.config_pistonMovement.name());
        outConfig.set("GriefPrevention.CheckPistonMovement", null);
        outConfig.set("GriefPrevention.LimitPistonsToLandClaims", null);

        outConfig.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
        outConfig.set("GriefPrevention.FireDestroys", this.config_fireDestroys);

        outConfig.set("GriefPrevention.EndermenMoveBlocks", this.config_endermenMoveBlocks);
        outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.config_silverfishBreakBlocks);
        outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);
        outConfig.set("GriefPrevention.RabbitsEatCrops", this.config_rabbitsEatCrops);
        outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", this.config_zombiesBreakDoors);

        outConfig.set("GriefPrevention.Advanced.ClaimExpirationCheckRate", this.config_advanced_claim_expiration_check_rate);
        outConfig.set("GriefPrevention.Advanced.OfflinePlayer_cache_days", this.config_advanced_offlineplayer_cache_days);

        //custom logger settings
        outConfig.set("GriefPrevention.Abridged Logs.Days To Keep", this.config_logs_daysToKeep);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Social Activity", this.config_logs_socialEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity", this.config_logs_suspiciousEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity", this.config_logs_adminEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Debug", this.config_logs_debugEnabled);

        try {
            outConfig.save(DataStore.configFilePath);
        } catch (IOException exception) {
            AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
        }

        //try to parse the list of commands requiring access trust in land claims
        this.config_claims_commandsRequiringAccessTrust = new ArrayList<>();
        String[] commands = accessTrustSlashCommands.split(";");
        for (String item : commands) {
            if (!item.isEmpty()) {
                this.config_claims_commandsRequiringAccessTrust.add(item.trim().toLowerCase());
            }
        }

        //try to parse the list of commands which should be banned during pvp combat
        this.config_pvp_blockedCommands = new ArrayList<>();
        commands = bannedPvPCommandsList.split(";");
        for (String command : commands) {
            this.config_pvp_blockedCommands.add(command.trim().toLowerCase());
        }
    }

    //handles slash commands
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        //claim
        if (cmd.getName().equalsIgnoreCase("claim") && player != null) {
            if (!EterniaKamui.instance.claimsEnabledForWorld(player.getWorld())) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
                return true;
            }

            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

            //if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
            if (getInt(Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER) > 0 &&
                    !player.hasPermission("griefprevention.overrideclaimcountlimit") &&
                    playerData.getClaims().size() >= getInt(Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER)) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
                return true;
            }

            //default is chest claim radius, unless -1
            int radius = EterniaKamui.getInt(Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS);
            if (radius < 0) radius = (int) Math.ceil(Math.sqrt(EterniaKamui.getInt(Integers.CLAIMS_MIN_AREA)) / 2);

            //if player has any claims, respect claim minimum size setting
            if (playerData.getClaims().size() > 0) {
                //if player has exactly one land claim, this requires the claim modification tool to be in hand (or creative mode player)
                if (playerData.getClaims().size() == 1 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != EterniaKamui.getMaterials(Materials.MODIFICATION_TOOL)) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.MustHoldModificationToolForThat);
                    return true;
                }

                radius = (int) Math.ceil(Math.sqrt(EterniaKamui.getInt(Integers.CLAIMS_MIN_AREA)) / 2);
            }

            //allow for specifying the radius
            if (args.length > 0) {
                if (playerData.getClaims().size() < 2 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != EterniaKamui.getMaterials(Materials.MODIFICATION_TOOL)) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.RadiusRequiresGoldenShovel);
                    return true;
                }

                int specifiedRadius;
                try {
                    specifiedRadius = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    return false;
                }

                if (specifiedRadius < radius) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.MinimumRadius, String.valueOf(radius));
                    return true;
                } else {
                    radius = specifiedRadius;
                }
            }

            if (radius < 0) radius = 0;

            Location lc = player.getLocation().add(-radius, 0, -radius);
            Location gc = player.getLocation().add(radius, 0, radius);

            //player must have sufficient unused claim blocks
            int area = Math.abs((gc.getBlockX() - lc.getBlockX() + 1) * (gc.getBlockZ() - lc.getBlockZ() + 1));
            int remaining = playerData.getRemainingClaimBlocks();
            if (remaining < area) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks, String.valueOf(area - remaining));
                EterniaKamui.instance.dataStore.tryAdvertiseAdminAlternatives(player);
                return true;
            }

            CreateClaimResult result = this.dataStore.createClaim(lc.getWorld(),
                    lc.getBlockX(), gc.getBlockX(),
                    lc.getBlockZ(), gc.getBlockZ(),
                    player.getUniqueId(), null, null, player);
            if (!result.succeeded) {
                if (result.claim != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);

                    Visualization visualization = Visualization.FromClaim(result.claim, player.getEyeLocation().getBlockY(), VisualizationType.ErrorClaim, player.getLocation());
                    Visualization.Apply(player, visualization);
                } else {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapRegion);
                }
            } else {
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);

                //link to a video demo of land claiming, based on world type
                if (EterniaKamui.instance.creativeRulesApply(player.getLocation())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                } else if (EterniaKamui.instance.claimsEnabledForWorld(player.getLocation().getWorld())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                Visualization visualization = Visualization.FromClaim(result.claim, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());
                Visualization.Apply(player, visualization);
                playerData.claimResizing = null;
                playerData.lastShovelLocation = null;
            }

            return true;
        }

        //extendclaim
        if (cmd.getName().equalsIgnoreCase("extendclaim") && player != null) {
            if (args.length < 1) {
                //link to a video demo of land claiming, based on world type
                if (EterniaKamui.instance.creativeRulesApply(player.getLocation())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                } else if (EterniaKamui.instance.claimsEnabledForWorld(player.getLocation().getWorld())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                //link to a video demo of land claiming, based on world type
                if (EterniaKamui.instance.creativeRulesApply(player.getLocation())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                } else if (EterniaKamui.instance.claimsEnabledForWorld(player.getLocation().getWorld())) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            //requires claim modification tool in hand
            if (player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != EterniaKamui.getMaterials(Materials.MODIFICATION_TOOL)) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.MustHoldModificationToolForThat);
                return true;
            }

            //must be standing in a land claim
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
            if (claim == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.StandInClaimToResize);
                return true;
            }

            //must have permission to edit the land claim you're in
            String errorMessage = claim.allowEdit(player);
            if (errorMessage != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
                return true;
            }

            //determine new corner coordinates
            org.bukkit.util.Vector direction = player.getLocation().getDirection();
            if (direction.getY() > .75) {
                EterniaKamui.sendMessage(player, TextMode.Info, Messages.ClaimsExtendToSky);
                return true;
            }

            if (direction.getY() < -.75) {
                EterniaKamui.sendMessage(player, TextMode.Info, Messages.ClaimsAutoExtendDownward);
                return true;
            }

            Location lc = claim.getLesserBoundaryCorner();
            Location gc = claim.getGreaterBoundaryCorner();
            int newx1 = lc.getBlockX();
            int newx2 = gc.getBlockX();
            int newz1 = lc.getBlockZ();
            int newz2 = gc.getBlockZ();

            //if changing Z only
            if (Math.abs(direction.getX()) < .3) {
                if (direction.getZ() > 0) {
                    newz2 += amount;  //north
                } else {
                    newz1 -= amount;  //south
                }
            }

            //if changing X only
            else if (Math.abs(direction.getZ()) < .3) {
                if (direction.getX() > 0) {
                    newx2 += amount;  //east
                } else {
                    newx1 -= amount;  //west
                }
            }

            //diagonals
            else {
                if (direction.getX() > 0) {
                    newx2 += amount;
                } else {
                    newx1 -= amount;
                }

                if (direction.getZ() > 0) {
                    newz2 += amount;
                } else {
                    newz1 -= amount;
                }
            }

            //attempt resize
            playerData.claimResizing = claim;
            this.dataStore.resizeClaimWithChecks(player, playerData, newx1, newx2, newz1, newz2);
            playerData.claimResizing = null;

            return true;
        }

        //abandonclaim
        if (cmd.getName().equalsIgnoreCase("abandonclaim") && player != null) {
            return this.abandonClaimHandler(player, false);
        }

        //abandontoplevelclaim
        if (cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null) {
            return this.abandonClaimHandler(player, true);
        }

        //ignoreclaims
        if (cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

            playerData.ignoreClaims = !playerData.ignoreClaims;

            //toggle ignore claims mode on or off
            if (!playerData.ignoreClaims) {
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
            } else {
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
            }

            return true;
        }

        //abandonallclaims
        else if (cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null) {
            if (args.length > 1) return false;

            if (args.length != 1 || !"confirm".equalsIgnoreCase(args[0])) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ConfirmAbandonAllClaims);
                return true;
            }

            //count claims
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            int originalClaimCount = playerData.getClaims().size();

            //check count
            if (originalClaimCount == 0) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
                return true;
            }

            if (getDouble(Doubles.CLAIMS_ABANDON_RETURN_RATIO) != 1.0D) {
                //adjust claim blocks
                for (Claim claim : playerData.getClaims()) {
                    playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - getDouble(Doubles.CLAIMS_ABANDON_RETURN_RATIO)))));
                }
            }


            //delete them
            this.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);

            //inform the player
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

            //revert any current visualization
            Visualization.Revert(player);

            return true;
        }

        //restore nature
        else if (cmd.getName().equalsIgnoreCase("restorenature") && player != null) {
            //change shovel mode
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.RestoreNature;
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
            return true;
        }

        //restore nature aggressive mode
        else if (cmd.getName().equalsIgnoreCase("restorenatureaggressive") && player != null) {
            //change shovel mode
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
            EterniaKamui.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
            return true;
        }

        //restore nature fill mode
        else if (cmd.getName().equalsIgnoreCase("restorenaturefill") && player != null) {
            //change shovel mode
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.RestoreNatureFill;

            //set radius based on arguments
            playerData.fillRadius = 2;
            if (args.length > 0) {
                try {
                    playerData.fillRadius = Integer.parseInt(args[0]);
                } catch (Exception ignored) {
                }
            }

            if (playerData.fillRadius < 0) playerData.fillRadius = 2;

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
            return true;
        }

        //trust <player>
        else if (cmd.getName().equalsIgnoreCase("trust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //most trust commands use this helper method, it keeps them consistent
            this.handleTrustCommand(player, ClaimPermission.Build, args[0]);

            return true;
        }

        //transferclaim <player>
        else if (cmd.getName().equalsIgnoreCase("transferclaim") && player != null) {
            //which claim is the user in?
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
                return true;
            }

            //check additional permission for admin claims
            if (claim.isAdminClaim() && !player.hasPermission("griefprevention.adminclaims")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
                return true;
            }

            UUID newOwnerID = null;  //no argument = make an admin claim
            String ownerName = "admin";

            if (args.length > 0) {
                OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
                if (targetPlayer == null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                    return true;
                }
                newOwnerID = targetPlayer.getUniqueId();
                ownerName = targetPlayer.getName();
            }

            //change ownerhsip
            try {
                this.dataStore.changeClaimOwner(claim, newOwnerID);
            } catch (NoTransferException e) {
                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
                return true;
            }

            //confirm
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
            EterniaKamui.AddLogEntry(player.getName() + " transferred a claim at " + EterniaKamui.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".", CustomLogEntryTypes.AdminActivity);

            return true;
        }

        //trustlist
        else if (cmd.getName().equalsIgnoreCase("trustlist") && player != null) {
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), null);

            //if no claim here, error message
            if (claim == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
                return true;
            }

            //if no permission to manage permissions, error message
            String errorMessage = claim.allowGrantPermission(player);
            if (errorMessage != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, errorMessage);
                return true;
            }

            //otherwise build a list of explicit permissions by permission level
            //and send that to the player
            ArrayList<String> builders = new ArrayList<>();
            ArrayList<String> containers = new ArrayList<>();
            ArrayList<String> accessors = new ArrayList<>();
            ArrayList<String> managers = new ArrayList<>();
            claim.getPermissions(builders, containers, accessors, managers);

            EterniaKamui.sendMessage(player, TextMode.Info, Messages.TrustListHeader);

            StringBuilder permissions = new StringBuilder();
            permissions.append(ChatColor.GOLD).append(">");

            if (managers.size() > 0) {
                for (String manager : managers) permissions.append(this.trustEntryToPlayerName(manager)).append(" ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.YELLOW).append(">");

            if (builders.size() > 0) {
                for (String builder : builders) permissions.append(this.trustEntryToPlayerName(builder)).append(" ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.GREEN).append(">");

            if (containers.size() > 0) {
                for (String container : containers)
                    permissions.append(this.trustEntryToPlayerName(container)).append(" ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.BLUE).append(">");

            if (accessors.size() > 0) {
                for (String accessor : accessors) permissions.append(this.trustEntryToPlayerName(accessor)).append(" ");
            }

            player.sendMessage(permissions.toString());

            player.sendMessage(
                    ChatColor.GOLD + getMessage(Messages.Manage) + " " +
                            ChatColor.YELLOW + getMessage(Messages.Build) + " " +
                            ChatColor.GREEN + getMessage(Messages.Containers) + " " +
                            ChatColor.BLUE + getMessage(Messages.Access));

            if (claim.getSubclaimRestrictions()) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.HasSubclaimRestriction);
            }

            return true;
        }

        //untrust <player> or untrust [<group>]
        else if (cmd.getName().equalsIgnoreCase("untrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //determine which claim the player is standing in
            Claim claim = this.dataStore.getClaimAt(player.getLocation() , null);

            //bracket any permissions
            if (args[0].contains(".") && !args[0].startsWith("[") && !args[0].endsWith("]")) {
                args[0] = "[" + args[0] + "]";
            }

            //determine whether a single player or clearing permissions entirely
            boolean clearPermissions = false;
            OfflinePlayer otherPlayer = null;
            if (args[0].equals("all")) {
                if (claim == null || claim.allowEdit(player) == null) {
                    clearPermissions = true;
                } else {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
                    return true;
                }
            } else {
                //validate player argument or group argument
                if (!args[0].startsWith("[") || !args[0].endsWith("]")) {
                    otherPlayer = this.resolvePlayerByName(args[0]);
                    if (!clearPermissions && otherPlayer == null && !args[0].equals("public")) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                        return true;
                    }

                    //correct to proper casing
                    if (otherPlayer != null)
                        args[0] = otherPlayer.getName();
                }
            }

            //if no claim here, apply changes to all his claims
            if (claim == null) {
                PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

                String idToDrop = args[0];
                if (otherPlayer != null) {
                    idToDrop = otherPlayer.getUniqueId().toString();
                }

                //calling event
                TrustChangedEvent event = new TrustChangedEvent(player, playerData.getClaims(), null, false, idToDrop);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return true;
                }

                //dropping permissions
                for (int i = 0; i < playerData.getClaims().size(); i++) {
                    claim = playerData.getClaims().get(i);

                    //if untrusting "all" drop all permissions
                    if (clearPermissions) {
                        claim.clearPermissions();
                    }

                    //otherwise drop individual permissions
                    else {
                        claim.dropPermission(idToDrop);
                        claim.managers.remove(idToDrop);
                    }

                    //save changes
                    this.dataStore.saveClaim(claim);
                }

                //beautify for output
                if (args[0].equals("public")) {
                    args[0] = "the public";
                }

                //confirmation message
                if (!clearPermissions) {
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
                } else {
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
                }
            }

            //otherwise, apply changes to only this claim
            else if (claim.allowGrantPermission(player) != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return true;
            } else {
                //if clearing all
                if (clearPermissions) {
                    //requires owner
                    if (claim.allowEdit(player) != null) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.UntrustAllOwnerOnly);
                        return true;
                    }

                    //calling the event
                    TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, args[0]);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return true;
                    }

                    claim.clearPermissions();
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
                }

                //otherwise individual permission drop
                else {
                    String idToDrop = args[0];
                    if (otherPlayer != null) {
                        idToDrop = otherPlayer.getUniqueId().toString();
                    }
                    boolean targetIsManager = claim.managers.contains(idToDrop);
                    if (targetIsManager && claim.allowEdit(player) != null)  //only claim owners can untrust managers
                    {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.ManagersDontUntrustManagers, claim.getOwnerName());
                        return true;
                    } else {
                        //calling the event
                        TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, idToDrop);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return true;
                        }

                        claim.dropPermission(idToDrop);
                        claim.managers.remove(idToDrop);

                        //beautify for output
                        if (args[0].equals("public")) {
                            args[0] = "the public";
                        }

                        EterniaKamui.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
                    }
                }

                //save changes
                this.dataStore.saveClaim(claim);
            }

            return true;
        }

        //accesstrust <player>
        else if (cmd.getName().equalsIgnoreCase("accesstrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            this.handleTrustCommand(player, ClaimPermission.Access, args[0]);

            return true;
        }

        //containertrust <player>
        else if (cmd.getName().equalsIgnoreCase("containertrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            this.handleTrustCommand(player, ClaimPermission.Inventory, args[0]);

            return true;
        }

        //permissiontrust <player>
        else if (cmd.getName().equalsIgnoreCase("permissiontrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            this.handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method

            return true;
        }

        //restrictsubclaim
        else if (cmd.getName().equalsIgnoreCase("restrictsubclaim") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);
            if (claim == null || claim.parent == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.StandInSubclaim);
                return true;
            }

            // If player has /ignoreclaims on, continue
            // If admin claim, fail if this user is not an admin
            // If not an admin claim, fail if this user is not the owner
            if (!playerData.ignoreClaims && (claim.isAdminClaim() ? !player.hasPermission("griefprevention.adminclaims") : !player.getUniqueId().equals(claim.parent.ownerID))) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.OnlyOwnersModifyClaims, claim.getOwnerName());
                return true;
            }

            if (claim.getSubclaimRestrictions()) {
                claim.setSubclaimRestrictions(false);
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.SubclaimUnrestricted);
            } else {
                claim.setSubclaimRestrictions(true);
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.SubclaimRestricted);
            }
            this.dataStore.saveClaim(claim);
            return true;
        }

        //buyclaimblocks
        else if (cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null) {
            //if economy is disabled, don't do anything
            EconomyHandler.EconomyWrapper economyWrapper = economyHandler.getWrapper();
            if (economyWrapper == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
                return true;
            }

            if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
                return true;
            }

            //if purchase disabled, send error message
            if (EterniaKamui.instance.config_economy_claimBlocksPurchaseCost == 0) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
                return true;
            }

            Economy economy = economyWrapper.getEconomy();

            //if no parameter, just tell player cost per block and balance
            if (args.length != 1) {
                EterniaKamui.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(EterniaKamui.instance.config_economy_claimBlocksPurchaseCost), String.valueOf(economy.getBalance(player)));
                return false;
            } else {
                PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

                //try to parse number of blocks
                int blockCount;
                try {
                    blockCount = Integer.parseInt(args[0]);
                } catch (NumberFormatException numberFormatException) {
                    return false;  //causes usage to be displayed
                }

                if (blockCount <= 0) {
                    return false;
                }

                //if the player can't afford his purchase, send error message
                double balance = economy.getBalance(player);
                double totalCost = blockCount * EterniaKamui.instance.config_economy_claimBlocksPurchaseCost;
                if (totalCost > balance) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost), String.valueOf(balance));
                }

                //otherwise carry out transaction
                else {
                    int newBonusClaimBlocks = playerData.getBonusClaimBlocks() + blockCount;

                    //if the player is going to reach max bonus limit, send error message
                    int bonusBlocksLimit = EterniaKamui.instance.config_economy_claimBlocksMaxBonus;
                    if (bonusBlocksLimit != 0 && newBonusClaimBlocks > bonusBlocksLimit) {
                        EterniaKamui.sendMessage(player, TextMode.Err, Messages.MaxBonusReached, String.valueOf(blockCount), String.valueOf(bonusBlocksLimit));
                        return true;
                    }

                    //withdraw cost
                    economy.withdrawPlayer(player, totalCost);

                    //add blocks
                    playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
                    this.dataStore.savePlayerData(player.getUniqueId(), playerData);

                    //inform player
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
                }

                return true;
            }
        }

        //sellclaimblocks <amount>
        else if (cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null) {
            //if economy is disabled, don't do anything
            EconomyHandler.EconomyWrapper economyWrapper = economyHandler.getWrapper();
            if (economyWrapper == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
                return true;
            }

            if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
                return true;
            }

            //if disabled, error message
            if (EterniaKamui.instance.config_economy_claimBlocksSellValue == 0) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
                return true;
            }

            //load player data
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            int availableBlocks = playerData.getRemainingClaimBlocks();

            //if no amount provided, just tell player value per block sold, and how many he can sell
            if (args.length != 1) {
                EterniaKamui.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(EterniaKamui.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
                return false;
            }

            //parse number of blocks
            int blockCount;
            try {
                blockCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            if (blockCount <= 0) {
                return false;
            }

            //if he doesn't have enough blocks, tell him so
            if (blockCount > availableBlocks) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
            }

            //otherwise carry out the transaction
            else {
                //compute value and deposit it
                double totalValue = blockCount * EterniaKamui.instance.config_economy_claimBlocksSellValue;
                economyWrapper.getEconomy().depositPlayer(player, totalValue);

                //subtract blocks
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
                this.dataStore.savePlayerData(player.getUniqueId(), playerData);

                //inform player
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
            }

            return true;
        }

        //adminclaims
        else if (cmd.getName().equalsIgnoreCase("adminclaims") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Admin;
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);

            return true;
        }

        //basicclaims
        else if (cmd.getName().equalsIgnoreCase("basicclaims") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Basic;
            playerData.claimSubdividing = null;
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);

            return true;
        }

        //subdivideclaims
        else if (cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Subdivide;
            playerData.claimSubdividing = null;
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, DataStore.SUBDIVISION_VIDEO_URL);

            return true;
        }

        //deleteclaim
        else if (cmd.getName().equalsIgnoreCase("deleteclaim") && player != null) {
            //determine which claim the player is standing in
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), null);

            if (claim == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
            } else {
                //deleting an admin claim additionally requires the adminclaims permission
                if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims")) {
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                        EterniaKamui.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                        playerData.warnedAboutMajorDeletion = true;
                    } else {
                        claim.removeSurfaceFluids(null);
                        this.dataStore.deleteClaim(claim, true, true);

                        //if in a creative mode world, /restorenature the claim
                        if (EterniaKamui.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) || EterniaKamui.getBool(Booleans.CLAIMS_SURVIVAL_AUTO_NATURE_RESTORATION)) {
                            EterniaKamui.instance.restoreClaim(claim, 0);
                        }

                        EterniaKamui.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                        EterniaKamui.AddLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + EterniaKamui.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                        //revert any current visualization
                        Visualization.Revert(player);

                        playerData.warnedAboutMajorDeletion = false;
                    }
                } else {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
                }
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("claimexplosions") && player != null) {
            //determine which claim the player is standing in
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), null);

            if (claim == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
            } else {
                String noBuildReason = claim.allowBuild(player, Material.STONE);
                if (noBuildReason != null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, noBuildReason);
                    return true;
                }

                if (claim.areExplosivesAllowed) {
                    claim.areExplosivesAllowed = false;
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
                } else {
                    claim.areExplosivesAllowed = true;
                    EterniaKamui.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
                }
            }

            return true;
        }

        //deleteallclaims <player>
        else if (cmd.getName().equalsIgnoreCase("deleteallclaims")) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //try to find that player
            OfflinePlayer otherPlayer = this.resolvePlayerByName(args[0]);
            if (otherPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            //delete all that player's claims
            this.dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccess, otherPlayer.getName());
            if (player != null) {
                EterniaKamui.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".", CustomLogEntryTypes.AdminActivity);

                //revert any current visualization
                Visualization.Revert(player);
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("deleteclaimsinworld")) {
            //must be executed at the console
            if (player != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.WorldNotFound);
                return true;
            }

            //delete all claims in that world
            this.dataStore.deleteClaimsInWorld(world, true);
            EterniaKamui.AddLogEntry("Deleted all claims in world: " + world.getName() + ".", CustomLogEntryTypes.AdminActivity);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("deleteuserclaimsinworld")) {
            //must be executed at the console
            if (player != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.WorldNotFound);
                return true;
            }

            //delete all USER claims in that world
            this.dataStore.deleteClaimsInWorld(world, false);
            EterniaKamui.AddLogEntry("Deleted all user claims in world: " + world.getName() + ".", CustomLogEntryTypes.AdminActivity);
            return true;
        }

        //claimbook
        else if (cmd.getName().equalsIgnoreCase("claimbook")) {
            //requires one parameter
            if (args.length != 1) return false;

            //try to find the specified player
            Player otherPlayer = this.getServer().getPlayer(args[0]);
            if (otherPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
            } else {
                WelcomeTask task = new WelcomeTask(otherPlayer);
                task.run();
            }
            return true;
        }

        //claimslist or claimslist <player>
        else if (cmd.getName().equalsIgnoreCase("claimslist")) {
            //at most one parameter
            if (args.length > 1) return false;

            //player whose claims will be listed
            OfflinePlayer otherPlayer;

            //if another player isn't specified, assume current player
            if (args.length < 1) {
                if (player != null)
                    otherPlayer = player;
                else
                    return false;
            }

            //otherwise if no permission to delve into another player's claims data
            else if (player != null && !player.hasPermission("griefprevention.claimslistother")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
                return true;
            }

            //otherwise try to find the specified player
            else {
                otherPlayer = this.resolvePlayerByName(args[0]);
                if (otherPlayer == null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                    return true;
                }
            }

            //load the target player's data
            PlayerData playerData = this.dataStore.getPlayerData(otherPlayer.getUniqueId());
            Vector<Claim> claims = playerData.getClaims();
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.StartBlockMath,
                    String.valueOf(playerData.getAccruedClaimBlocks()),
                    String.valueOf((playerData.getBonusClaimBlocks() + this.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))),
                    String.valueOf((playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks() + this.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))));
            if (claims.size() > 0) {
                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.ClaimsListHeader);
                for (int i = 0; i < playerData.getClaims().size(); i++) {
                    Claim claim = playerData.getClaims().get(i);
                    EterniaKamui.sendMessage(player, TextMode.Instr, getfriendlyLocationString(claim.getLesserBoundaryCorner()) + getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea())));
                }

                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks()));
            }

            //drop the data we just loaded, if the player isn't online
            if (!otherPlayer.isOnline())
                this.dataStore.clearCachedPlayerData(otherPlayer.getUniqueId());

            return true;
        }

        //adminclaimslist
        else if (cmd.getName().equalsIgnoreCase("adminclaimslist")) {
            //find admin claims
            Vector<Claim> claims = new Vector<>();
            for (Claim claim : this.dataStore.claims) {
                if (claim.ownerID == null)  //admin claim
                {
                    claims.add(claim);
                }
            }
            if (claims.size() > 0) {
                EterniaKamui.sendMessage(player, TextMode.Instr, Messages.ClaimsListHeader);
                for (Claim claim : claims) {
                    EterniaKamui.sendMessage(player, TextMode.Instr, getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                }
            }

            return true;
        }

        //unlockItems
        else if (cmd.getName().equalsIgnoreCase("unlockdrops") && player != null) {
            PlayerData playerData;

            if (player.hasPermission("griefprevention.unlockothersdrops") && args.length == 1) {
                Player otherPlayer = Bukkit.getPlayer(args[0]);
                if (otherPlayer == null) {
                    EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                    return true;
                }

                playerData = this.dataStore.getPlayerData(otherPlayer.getUniqueId());
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.DropUnlockOthersConfirmation, otherPlayer.getName());
            } else {
                playerData = this.dataStore.getPlayerData(player.getUniqueId());
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.DropUnlockConfirmation);
            }

            playerData.dropsAreUnlocked = true;

            return true;
        }

        //deletealladminclaims
        else if (player != null && cmd.getName().equalsIgnoreCase("deletealladminclaims")) {
            if (!player.hasPermission("griefprevention.deleteclaims")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
                return true;
            }

            //delete all admin claims
            this.dataStore.deleteClaimsForPlayer(null, true);  //null for owner id indicates an administrative claim

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
            if (player != null) {
                EterniaKamui.AddLogEntry(player.getName() + " deleted all administrative claims.", CustomLogEntryTypes.AdminActivity);

                //revert any current visualization
                Visualization.Revert(player);
            }

            return true;
        }

        //adjustbonusclaimblocks <player> <amount> or [<permission>] amount
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks")) {
            //requires exactly two parameters, the other player or group's name and the adjustment
            if (args.length != 2) return false;

            //parse the adjustment amount
            int adjustment;
            try {
                adjustment = Integer.parseInt(args[1]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            //if granting blocks to all players with a specific permission
            if (args[0].startsWith("[") && args[0].endsWith("]")) {
                String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
                int newTotal = this.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

                EterniaKamui.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
                if (player != null)
                    EterniaKamui.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

                return true;
            }

            //otherwise, find the specified player
            OfflinePlayer targetPlayer;
            try {
                UUID playerID = UUID.fromString(args[0]);
                targetPlayer = this.getServer().getOfflinePlayer(playerID);

            } catch (IllegalArgumentException e) {
                targetPlayer = this.resolvePlayerByName(args[0]);
            }

            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            //give blocks to player
            PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getUniqueId());
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
            this.dataStore.savePlayerData(targetPlayer.getUniqueId(), playerData);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
            if (player != null)
                EterniaKamui.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".", CustomLogEntryTypes.AdminActivity);

            return true;
        }

        //adjustbonusclaimblocksall <amount>
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocksall")) {
            //requires exactly one parameter, the amount of adjustment
            if (args.length != 1) return false;

            //parse the adjustment amount
            int adjustment;
            try {
                adjustment = Integer.parseInt(args[0]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            //for each online player
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) this.getServer().getOnlinePlayers();
            StringBuilder builder = new StringBuilder();
            for (Player onlinePlayer : players) {
                UUID playerID = onlinePlayer.getUniqueId();
                PlayerData playerData = this.dataStore.getPlayerData(playerID);
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
                this.dataStore.savePlayerData(playerID, playerData);
                builder.append(onlinePlayer.getName()).append(" ");
            }

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.AdjustBlocksAllSuccess, String.valueOf(adjustment));
            EterniaKamui.AddLogEntry("Adjusted all " + players.size() + "players' bonus claim blocks by " + adjustment + ".  " + builder.toString(), CustomLogEntryTypes.AdminActivity);

            return true;
        }

        //setaccruedclaimblocks <player> <amount>
        else if (cmd.getName().equalsIgnoreCase("setaccruedclaimblocks")) {
            //requires exactly two parameters, the other player's name and the new amount
            if (args.length != 2) return false;

            //parse the adjustment amount
            int newAmount;
            try {
                newAmount = Integer.parseInt(args[1]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            //find the specified player
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            //set player's blocks
            PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getUniqueId());
            playerData.setAccruedClaimBlocks(newAmount);
            this.dataStore.savePlayerData(targetPlayer.getUniqueId(), playerData);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.SetClaimBlocksSuccess);
            if (player != null)
                EterniaKamui.AddLogEntry(player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount + ".", CustomLogEntryTypes.AdminActivity);

            return true;
        }

        //trapped
        else if (cmd.getName().equalsIgnoreCase("trapped") && player != null) {
            //FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves

            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), playerData.lastClaim);

            //if another /trapped is pending, ignore this slash command
            if (playerData.pendingTrapped) {
                return true;
            }

            //if the player isn't in a claim or has permission to build, tell him to man up
            if (claim == null || claim.allowBuild(player, Material.AIR) == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);
                return true;
            }

            //rescue destination may be set by GPFlags or other plugin, ask to find out
            SaveTrappedPlayerEvent event = new SaveTrappedPlayerEvent(claim);
            Bukkit.getPluginManager().callEvent(event);

            //if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
            if (player.getWorld().getEnvironment() != Environment.NORMAL && event.getDestination() == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
                return true;
            }

            //if the player is in an administrative claim and AllowTrappedInAdminClaims is false, he should contact an admin
            if (!EterniaKamui.getBool(Booleans.CLAIMS_ALLOW_TRAPPED_IN_ADMINCLAIMS) && claim.isAdminClaim() && event.getDestination() == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
                return true;
            }
            //send instructions
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.RescuePending);

            //create a task to rescue this player in a little while
            PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation(), event.getDestination());
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 200L);  //20L ~ 1 second

            return true;
        } else if (cmd.getName().equalsIgnoreCase("gpreload")) {
            this.loadConfig();
            if (player != null) {
                EterniaKamui.sendMessage(player, TextMode.Success, "Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            } else {
                EterniaKamui.AddLogEntry("Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            }

            return true;
        }

        //givepet
        else if (cmd.getName().equalsIgnoreCase("givepet") && player != null) {
            //requires one parameter
            if (args.length < 1) return false;

            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

            //special case: cancellation
            if (args[0].equalsIgnoreCase("cancel")) {
                playerData.petGiveawayRecipient = null;
                EterniaKamui.sendMessage(player, TextMode.Success, Messages.PetTransferCancellation);
                return true;
            }

            //find the specified player
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            //remember the player's ID for later pet transfer
            playerData.petGiveawayRecipient = targetPlayer;

            //send instructions
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.ReadyToTransferPet);

            return true;
        }

        //gpblockinfo
        else if (cmd.getName().equalsIgnoreCase("gpblockinfo") && player != null) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            player.sendMessage("In Hand: " + inHand.getType().name());

            Block inWorld = player.getTargetBlockExact(300, FluidCollisionMode.ALWAYS);
            if (inWorld == null) inWorld = player.getEyeLocation().getBlock();
            player.sendMessage("In World: " + inWorld.getType().name());

            return true;
        }

        //ignoreplayer
        else if (cmd.getName().equalsIgnoreCase("ignoreplayer") && player != null) {
            //requires target player name
            if (args.length < 1) return false;

            //validate target player
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            this.setIgnoreStatus(player, targetPlayer, IgnoreMode.StandardIgnore);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.IgnoreConfirmation);

            return true;
        }

        //unignoreplayer
        else if (cmd.getName().equalsIgnoreCase("unignoreplayer") && player != null) {
            //requires target player name
            if (args.length < 1) return false;

            //validate target player
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Boolean ignoreStatus = playerData.ignoredPlayers.get(targetPlayer.getUniqueId());
            if (ignoreStatus == null || ignoreStatus) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NotIgnoringPlayer);
                return true;
            }

            this.setIgnoreStatus(player, targetPlayer, IgnoreMode.None);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.UnIgnoreConfirmation);

            return true;
        }

        //ignoredplayerlist
        else if (cmd.getName().equalsIgnoreCase("ignoredplayerlist") && player != null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            StringBuilder builder = new StringBuilder();
            for (Entry<UUID, Boolean> entry : playerData.ignoredPlayers.entrySet()) {
                if (entry.getValue() != null) {
                    //if not an admin ignore, add it to the list
                    if (!entry.getValue()) {
                        builder.append(EterniaKamui.lookupPlayerName(entry.getKey()));
                        builder.append(" ");
                    }
                }
            }

            String list = builder.toString().trim();
            if (list.isEmpty()) {
                EterniaKamui.sendMessage(player, TextMode.Info, Messages.NotIgnoringAnyone);
            } else {
                EterniaKamui.sendMessage(player, TextMode.Info, list);
            }

            return true;
        }

        //separateplayers
        else if (cmd.getName().equalsIgnoreCase("separate")) {
            //requires two player names
            if (args.length < 2) return false;

            //validate target players
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            OfflinePlayer targetPlayer2 = this.resolvePlayerByName(args[1]);
            if (targetPlayer2 == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            this.setIgnoreStatus(targetPlayer, targetPlayer2, IgnoreMode.AdminIgnore);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.SeparateConfirmation);

            return true;
        }

        //unseparateplayers
        else if (cmd.getName().equalsIgnoreCase("unseparate")) {
            //requires two player names
            if (args.length < 2) return false;

            //validate target players
            OfflinePlayer targetPlayer = this.resolvePlayerByName(args[0]);
            if (targetPlayer == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            OfflinePlayer targetPlayer2 = this.resolvePlayerByName(args[1]);
            if (targetPlayer2 == null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }

            this.setIgnoreStatus(targetPlayer, targetPlayer2, IgnoreMode.None);
            this.setIgnoreStatus(targetPlayer2, targetPlayer, IgnoreMode.None);

            EterniaKamui.sendMessage(player, TextMode.Success, Messages.UnSeparateConfirmation);

            return true;
        }
        return false;
    }

    void setIgnoreStatus(OfflinePlayer ignorer, OfflinePlayer ignoree, IgnoreMode mode) {
        PlayerData playerData = this.dataStore.getPlayerData(ignorer.getUniqueId());
        if (mode == IgnoreMode.None) {
            playerData.ignoredPlayers.remove(ignoree.getUniqueId());
        } else {
            playerData.ignoredPlayers.put(ignoree.getUniqueId(), mode != IgnoreMode.StandardIgnore);
        }

        playerData.ignoreListChanged = true;
        if (!ignorer.isOnline()) {
            this.dataStore.savePlayerData(ignorer.getUniqueId(), playerData);
            this.dataStore.clearCachedPlayerData(ignorer.getUniqueId());
        }
    }

    public enum IgnoreMode {None, StandardIgnore, AdminIgnore}

    private String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        } else {
            return EterniaKamui.lookupPlayerName(entry);
        }
    }

    public static String getfriendlyLocationString(Location location) {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    private boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //which claim is being abandoned?
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), null);
        if (claim == null) {
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
            return false;
        }

        //verify ownership
        else if (claim.allowEdit(player) != null) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
            return false;
        }

        //warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            EterniaKamui.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return true;
        } else {
            //delete it
            claim.removeSurfaceFluids(null);
            this.dataStore.deleteClaim(claim, true, false);

            //if in a creative mode world, restore the claim area
            if (EterniaKamui.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                EterniaKamui.AddLogEntry(player.getName() + " abandoned a claim @ " + EterniaKamui.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                EterniaKamui.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                EterniaKamui.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            //adjust claim blocks when abandoning a top level claim
            if (getDouble(Doubles.CLAIMS_ABANDON_RETURN_RATIO) != 1.0D && claim.parent == null && claim.ownerID.equals(playerData.playerID)) {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - getDouble(Doubles.CLAIMS_ABANDON_RETURN_RATIO)))));
            }

            //tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));

            //revert any current visualization
            Visualization.Revert(player);

            playerData.warnedAboutMajorDeletion = false;
        }

        return true;

    }

    //helper method keeps the trust commands consistent and eliminates duplicate code
    private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) {
        //determine which claim the player is standing in
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), null);

        //validate player or group argument
        String permission = null;
        OfflinePlayer otherPlayer;
        UUID recipientID = null;
        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
                return;
            }
        } else if (recipientName.contains(".")) {
            permission = recipientName;
        } else {
            otherPlayer = this.resolvePlayerByName(recipientName);
            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all")) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return;
            }

            if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
                recipientID = otherPlayer.getUniqueId();
            } else {
                recipientName = "public";
            }
        }

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            targetClaims.addAll(playerData.getClaims());
        } else {
            //check permission here
            if (claim.allowGrantPermission(player) != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return;
            }

            //see if the player has the level of permission he's trying to grant
            String errorMessage;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case Access:
                        errorMessage = claim.allowAccess(player);
                        break;
                    case Inventory:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player, Material.AIR);
                }
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
                return;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
            return;
        }

        String identifierToAdd = recipientName;
        if (permission != null) {
            identifierToAdd = "[" + permission + "]";
        } else if (recipientID != null) {
            identifierToAdd = recipientID.toString();
        }

        //calling the event
        TrustChangedEvent event = new TrustChangedEvent(player, targetClaims, permissionLevel, true, identifierToAdd);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        //apply changes
        for (Claim currentClaim : targetClaims) {
            if (permissionLevel == null) {
                if (!currentClaim.managers.contains(identifierToAdd)) {
                    currentClaim.managers.add(identifierToAdd);
                }
            } else {
                currentClaim.setPermission(identifierToAdd, permissionLevel);
            }
            this.dataStore.saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = getMessage(Messages.AccessPermission);
        } else //ClaimPermission.Inventory
        {
            permissionDescription = getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = getMessage(Messages.LocationAllClaims);
        } else {
            location = getMessage(Messages.LocationCurrentClaim);
        }

        EterniaKamui.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    //helper method to resolve a player by name
    final ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();

    //thread to build the above cache
    private class CacheOfflinePlayerNamesThread extends Thread {
        private final OfflinePlayer[] offlinePlayers;
        private final ConcurrentHashMap<String, UUID> playerNameToIDMap;

        CacheOfflinePlayerNamesThread(OfflinePlayer[] offlinePlayers, ConcurrentHashMap<String, UUID> playerNameToIDMap) {
            this.offlinePlayers = offlinePlayers;
            this.playerNameToIDMap = playerNameToIDMap;
        }

        public void run() {
            long now = System.currentTimeMillis();
            final long millisecondsPerDay = 1000 * 60 * 60 * 24;
            for (OfflinePlayer player : offlinePlayers) {
                try {
                    UUID playerID = player.getUniqueId();
                    if (playerID == null) continue;
                    long lastSeen = player.getLastSeen();

                    //if the player has been seen in the last 90 days, cache his name/UUID pair
                    long diff = now - lastSeen;
                    long daysDiff = diff / millisecondsPerDay;
                    if (daysDiff <= config_advanced_offlineplayer_cache_days) {
                        String playerName = player.getName();
                        if (playerName == null) continue;
                        this.playerNameToIDMap.put(playerName, playerID);
                        this.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public OfflinePlayer resolvePlayerByName(String name) {
        //try online players first
        Player targetPlayer = this.getServer().getPlayerExact(name);
        if (targetPlayer != null) return targetPlayer;

        UUID bestMatchID;

        //try exact match first
        bestMatchID = this.playerNameToIDMap.get(name);

        //if failed, try ignore case
        if (bestMatchID == null) {
            bestMatchID = this.playerNameToIDMap.get(name.toLowerCase());
        }
        if (bestMatchID == null) {
            return null;
        }

        return this.getServer().getOfflinePlayer(bestMatchID);
    }

    //helper method to resolve a player name from the player's UUID
    public static String lookupPlayerName(UUID playerID) {
        //parameter validation
        if (playerID == null) return "somebody";

        //check the cache
        OfflinePlayer player = EterniaKamui.instance.getServer().getOfflinePlayer(playerID);
        if (player.hasPlayedBefore() || player.isOnline()) {
            return player.getName();
        } else {
            return "someone(" + playerID.toString() + ")";
        }
    }

    //cache for player name lookups, to save searches of all offline players
    public static void cacheUUIDNamePair(UUID playerID, String playerName) {
        //store the reverse mapping
        EterniaKamui.instance.playerNameToIDMap.put(playerName, playerID);
        EterniaKamui.instance.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
    }

    //string overload for above helper
    static String lookupPlayerName(String playerID) {
        UUID id;
        try {
            id = UUID.fromString(playerID);
        } catch (IllegalArgumentException ex) {
            EterniaKamui.AddLogEntry("Error: Tried to look up a local player name for invalid UUID: " + playerID);
            return "someone";
        }

        return lookupPlayerName(id);
    }

    public void onDisable() {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) this.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = this.dataStore.getPlayerData(playerID);
            this.dataStore.savePlayerDataSync(playerID, playerData);
        }

        //dump any remaining unwritten log entries
        this.customLogger.WriteEntries();

        AddLogEntry("GriefPrevention disabled.");
    }

    //called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        //if anti spawn camping feature is not enabled, do nothing
        if (!this.config_pvp_protectFreshSpawns) return;

        //if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld())) return;

        //if player is in creative mode, do nothing
        if (player.getGameMode() == GameMode.CREATIVE) return;

        //if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) return;

        //check inventory for well, anything
        if (EterniaKamui.isInventoryEmpty(player)) {
            //if empty, apply immunity
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.pvpImmune = true;

            //inform the player after he finishes respawning
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart, 5L);

            //start a task to re-check this player's inventory every minute until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 1200L);
        }
    }

    static boolean isInventoryEmpty(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorStacks = inventory.getArmorContents();

        //check armor slots, stop if any items are found
        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack == null || armorStack.getType() == Material.AIR)) return false;
        }

        //check other slots, stop if any items are found
        ItemStack[] generalStacks = inventory.getContents();
        for (ItemStack generalStack : generalStacks) {
            if (!(generalStack == null || generalStack.getType() == Material.AIR)) return false;
        }

        return true;
    }
    //moves a player from the claim he's in to a nearby wilderness location
    public Location ejectPlayer(Player player) {
        //look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true) {
            Claim claim;
            claim = EterniaKamui.instance.dataStore.getClaimAt(candidateLocation, null);

            //if there's a claim here, keep looking
            if (claim != null) {
                candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
            }

            //otherwise find a safe place to teleport the player
            else {
                //find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
                Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
                player.teleport(destination);
                return destination;
            }
        }
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location) {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded()) {
            chunk.load(true);
        }
    }

    public static void sendMessage(Player player, Messages messageID, String... args) {
        sendMessage(player, TextMode.Success, messageID, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args) {
        String message = getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            EterniaKamui.AddLogEntry(color + message);
        } else {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0) {
            EterniaKamui.instance.getServer().getScheduler().runTaskLater(EterniaKamui.instance, task, delayInTicks);
        } else {
            task.run();
        }
    }

    //checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        ClaimsMode mode = claimsWorldModes.get(world);
        return mode != null && mode != ClaimsMode.Disabled;
    }

    //determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location location) {
        if (!getBool(Booleans.CREATIVE_WORLD_EXIST)) return false;

        return claimsWorldModes.get((location.getWorld())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location location, Material material) {
        if (!EterniaKamui.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null) {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || claimsWorldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                //exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (material != Material.CHEST || playerData.getClaims().size() > 0 || EterniaKamui.getInt(Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS) == -1) {
                    String reason = getMessage(Messages.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        reason += "  " + getMessage(Messages.IgnoreClaimsAdvertisement);
                    reason += "  " + getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                    return reason;
                } else {
                    return null;
                }
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        }

        //if not in the wilderness, then apply claim rules (permissions, etc)
        else {
            //cache the claim for later reference
            playerData.lastClaim = claim;
            return claim.allowBuild(player, material);
        }
    }

    public String allowBreak(Player player, Block block, Location location) {
        return this.allowBreak(player, block, location, null);
    }

    public String allowBreak(Player player, Block block, Location location, BlockBreakEvent breakEvent) {
        if (!EterniaKamui.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null) {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || claimsWorldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                String reason = getMessage(Messages.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + getMessage(Messages.IgnoreClaimsAdvertisement);
                reason += "  " + getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                return reason;
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        } else {
            //cache the claim for later reference
            playerData.lastClaim = claim;

            //if not in the wilderness, then apply claim rules (permissions, etc)
            String cancel = claim.allowBreak(player, block.getType());
            if (cancel != null && breakEvent != null) {
                PreventBlockBreakEvent preventionEvent = new PreventBlockBreakEvent(breakEvent);
                Bukkit.getPluginManager().callEvent(preventionEvent);
                if (preventionEvent.isCancelled()) {
                    cancel = null;
                }
            }

            return cancel;
        }
    }

    //restores nature in multiple chunks, as described by a claim instance
    //this restores all chunks which have ANY number of claim blocks from this claim in them
    //if the claim is still active (in the data store), then the claimed blocks will not be changed (only the area bordering the claim)
    public void restoreClaim(Claim claim, long delayInTicks) {
        //admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) return;

        //it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) return;

        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks) {
            this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
    }


    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization) {
        //build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
        int maxHeight = chunk.getWorld().getMaxHeight();
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
        Block startBlock = chunk.getBlock(0, 0, 0);
        Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
                    Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                    snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getType(), block.getBlockData());
                }
            }
        }

        //create task to process those data in another thread
        Location lesserBoundaryCorner = chunk.getBlock(0, 0, 0).getLocation();
        Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();

        //create task
        //when done processing, this task will create a main thread task to actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, EterniaKamui.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
        EterniaKamui.instance.getServer().getScheduler().runTaskLaterAsynchronously(EterniaKamui.instance, task, delayInTicks);
    }

    private Set<Material> parseMaterialListFromConfig(List<String> stringsToParse) {
        Set<Material> materials = EnumSet.noneOf(Material.class);

        //for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++) {
            //try to parse the string value into a material info
            String string = stringsToParse.get(i);

            //defensive coding
            if (string == null) continue;

            //try to parse the string value into a material
            Material material = Material.getMaterial(string.toUpperCase());

            //null value returned indicates an error parsing the string from the config file
            if (material == null) {
                if (!string.contains("can't"))
                {
                    stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see BukkitDev documentation");
                    //update string, which will go out to config file to help user find the error entry
                    stringsToParse.set(i, string + "     <-- can't understand this entry, see BukkitDev documentation");

                    //warn about invalid material in log
                    EterniaKamui.AddLogEntry(String.format("ERROR: Invalid material %s.  Please update your config.yml.", string));
                }
            }

            //otherwise material is valid, add it
            else {
                materials.add(material);
            }
        }
        return materials;
    }

    public int getSeaLevel(World world) {
        Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1) {
            return world.getSeaLevel();
        } else {
            return overrideValue;
        }
    }

    public boolean pvpRulesApply(World world) {
        Boolean configSetting = this.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null) return configSetting;
        return world.getPVP();
    }

    public static boolean isNewToServer(Player player) {
        if (player.getStatistic(Statistic.PICKUP, Material.OAK_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.SPRUCE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.BIRCH_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.JUNGLE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.ACACIA_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.DARK_OAK_LOG) > 0) return false;

        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
        return playerData.getClaims().size() <= 0;
    }

    public ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

    public boolean claimIsPvPSafeZone(Claim claim) {
        return claim.isAdminClaim() && claim.parent == null && EterniaKamui.instance.config_pvp_noCombatInAdminLandClaims ||
                claim.isAdminClaim() && claim.parent != null && EterniaKamui.instance.config_pvp_noCombatInAdminSubdivisions ||
                !claim.isAdminClaim() && EterniaKamui.instance.config_pvp_noCombatInPlayerLandClaims;
    }

}