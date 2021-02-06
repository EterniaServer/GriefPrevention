package br.com.eterniaserver.eterniakamui.configurations.configs;

import br.com.eterniaserver.eterniakamui.Constants;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.enums.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ConfigsCfg {

    private final FileConfiguration configuration;
    private final FileConfiguration outConfiguration;

    public ConfigsCfg(String[] strings, Boolean[] booleans, Integer[] integers, Double[] doubles, Material[] materials) {

        this.configuration = YamlConfiguration.loadConfiguration(new File(Constants.CONFIG_FILE_PATH));
        this.outConfiguration = new YamlConfiguration();

        strings[Strings.TABLE_WORLDS.ordinal()] = configuration.getString("sql.table-worlds", "ek_worlds");
        strings[Strings.TABLE_FLAGS.ordinal()] = configuration.getString("sql.table-flags", "ek_flags");

        booleans[Booleans.PISTON_EXPLOSION_SOUND.ordinal()] = configuration.getBoolean("claims.piston-explosion-sound", true);
        booleans[Booleans.CLAIMS_PROTECT_CREATURES.ordinal()] = configuration.getBoolean("claims.protect-creatures", true);
        booleans[Booleans.CLAIMS_PREVENT_THEFT.ordinal()] = configuration.getBoolean("claims.prevent-theft", true);
        booleans[Booleans.CLAIMS_PREVENT_GLOBAL_MONSTER_EGGS.ordinal()] = configuration.getBoolean("claims.prevent-global-monster-eggs", true);
        booleans[Booleans.CLAIMS_PROTECT_HORSES.ordinal()] = configuration.getBoolean("claims.protect-horses", true);
        booleans[Booleans.CLAIMS_PROTECT_DONKEYS.ordinal()] = configuration.getBoolean("claims.protect-donkeys", true);
        booleans[Booleans.CLAIMS_PROTECT_LLAMAS.ordinal()] = configuration.getBoolean("claims.protect-llamas", true);
        booleans[Booleans.CLAIMS_PREVENT_BUTTONS_SWITCHES.ordinal()] = configuration.getBoolean("claims.prevent-buttons-switches", true);
        booleans[Booleans.CLAIMS_LOCK_WOODEN_DOORS.ordinal()] = configuration.getBoolean("claims.lock-wooden-doors", false);
        booleans[Booleans.CLAIMS_LOCK_TRAP_DOORS.ordinal()] = configuration.getBoolean("claims.lock-trap-doors", false);
        booleans[Booleans.CLAIMS_LOCK_FENCE_GATES.ordinal()] = configuration.getBoolean("claims.lock-fence-gates", true);
        booleans[Booleans.CLAIMS_ENDERPEARLS_REQUIRE_ACCESSTRUST.ordinal()] = configuration.getBoolean("claims.enderpearls-require-accesstrust", true);
        booleans[Booleans.CLAIMS_RAID_TRIGGERS_REQUIRE_BUILDTRUST.ordinal()] = configuration.getBoolean("claims.raid-triggers-require-buildtrust", true);
        booleans[Booleans.CLAIMS_RESPECT_WORLDGUARD.ordinal()] = configuration.getBoolean("claims.respect-worldguard", true);
        booleans[Booleans.CLAIMS_VILLAGER_TRADING_REQUIRES_TRUST.ordinal()] = configuration.getBoolean("claims.villager-trading-requires-trust", true);
        booleans[Booleans.CLAIMS_SURVIVAL_AUTO_NATURE_RESTORATION.ordinal()] = configuration.getBoolean("claims.survival-auto-nature-restoration", false);
        booleans[Booleans.CLAIMS_ALLOW_TRAPPED_IN_ADMINCLAIMS.ordinal()] = configuration.getBoolean("claims.allow-trapped-in-adminclaims", true);

        integers[Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER.ordinal()] = configuration.getInt("claims.max-claims-per-player", 0);
        integers[Integers.CLAIMS_INITIAL_BLOCKS.ordinal()] = configuration.getInt("claims.initial-blocks", 100);
        integers[Integers.CLAIMS_BLOCKS_ACCRUED_PER_HOUR.ordinal()] = configuration.getInt("claims.blocks-accrued-per-hour", 100);
        integers[Integers.CLAIMS_MAX_ACCRUED_BLOCKS.ordinal()] = configuration.getInt("claims.max-accrued-blocks", 2000);
        integers[Integers.CLAIMS_ACCRUED_IDLE_THRESHOLD.ordinal()] = configuration.getInt("claims.accrued-idle-threshold", 0);
        integers[Integers.CLAIMS_ACCRUED_IDLE_PERCENT.ordinal()] = configuration.getInt("claims.accrued-idle-percent", 0);
        integers[Integers.CLAIMS_EXPIRATION_DAYS.ordinal()] = configuration.getInt("claims.expiration-days", 60);
        integers[Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS.ordinal()] = configuration.getInt("claims.automatic-claims-for-new-players-radius", 4);
        integers[Integers.CLAIMS_MIN_WIDTH.ordinal()] = configuration.getInt("claims.min-width", 5);
        integers[Integers.CLAIMS_MIN_AREA.ordinal()] = configuration.getInt("claims.min-area", 100);
        integers[Integers.CLAIMS_CHEST_CLAIM_EXPIRATION_DAYS.ordinal()] = configuration.getInt("claims.chest-claim-expiration-days", 7);
        integers[Integers.CLAIMS_UNUSED_CLAIM_EXPIRATION_DAYS.ordinal()] = configuration.getInt("claims.unused-claim-expiration-days", 14);

        doubles[Doubles.CLAIMS_ABANDON_RETURN_RATIO.ordinal()] = configuration.getDouble("claims.abandon-return-ratio", 1.0D);

        materials[Materials.INVESTIGATION_TOOL.ordinal()] = Material.getMaterial(configuration.getString("claims.investigation-tool", Material.STICK.name()));
        materials[Materials.MODIFICATION_TOOL.ordinal()] = Material.getMaterial(configuration.getString("claims.modification-tool", Material.GOLDEN_SHOVEL.name()));

        outConfiguration.set("sql.table-worlds", strings[Strings.TABLE_WORLDS.ordinal()]);
        outConfiguration.set("sql.table-flags", strings[Strings.TABLE_FLAGS.ordinal()]);

        outConfiguration.set("claims.protect-creatures", booleans[Booleans.CLAIMS_PROTECT_CREATURES.ordinal()]);
        outConfiguration.set("claims.prevent-theft", booleans[Booleans.CLAIMS_PREVENT_THEFT.ordinal()]);
        outConfiguration.set("claims.prevent-global-monster-eggs", booleans[Booleans.CLAIMS_PREVENT_GLOBAL_MONSTER_EGGS.ordinal()]);
        outConfiguration.set("claims.protect-horses", booleans[Booleans.CLAIMS_PROTECT_HORSES.ordinal()]);
        outConfiguration.set("claims.protect-donkeys", booleans[Booleans.CLAIMS_PROTECT_DONKEYS.ordinal()]);
        outConfiguration.set("claims.protect-llanas", booleans[Booleans.CLAIMS_PROTECT_LLAMAS.ordinal()]);
        outConfiguration.set("claims.prevent-buttons-switches", booleans[Booleans.CLAIMS_PREVENT_BUTTONS_SWITCHES.ordinal()]);
        outConfiguration.set("claims.lock-wooden-doors", booleans[Booleans.CLAIMS_LOCK_WOODEN_DOORS.ordinal()]);
        outConfiguration.set("claims.lock-trap-doors", booleans[Booleans.CLAIMS_LOCK_TRAP_DOORS.ordinal()]);
        outConfiguration.set("claims.lock-fence-gates", booleans[Booleans.CLAIMS_LOCK_FENCE_GATES.ordinal()]);
        outConfiguration.set("claims.enderpearls-require-accesstrust", booleans[Booleans.CLAIMS_ENDERPEARLS_REQUIRE_ACCESSTRUST.ordinal()]);
        outConfiguration.set("claims.raid-triggers-require-buildtrust", booleans[Booleans.CLAIMS_ENDERPEARLS_REQUIRE_ACCESSTRUST.ordinal()]);
        outConfiguration.set("claims.piston-explosion-sound", booleans[Booleans.PISTON_EXPLOSION_SOUND.ordinal()]);
        outConfiguration.set("claims.respect-worldguard", booleans[Booleans.CLAIMS_RESPECT_WORLDGUARD.ordinal()]);
        outConfiguration.set("claims.villager-trading-requires-trust", booleans[Booleans.CLAIMS_VILLAGER_TRADING_REQUIRES_TRUST.ordinal()]);
        outConfiguration.set("claims.survival-auto-nature-restoration", booleans[Booleans.CLAIMS_SURVIVAL_AUTO_NATURE_RESTORATION.ordinal()]);
        outConfiguration.set("claims.allow-trapped-in-adminclaims", booleans[Booleans.CLAIMS_ALLOW_TRAPPED_IN_ADMINCLAIMS.ordinal()]);

        outConfiguration.set("claims.max-claims-per-player", integers[Integers.CLAIMS_MAX_CLAIMS_PER_PLAYER.ordinal()]);
        outConfiguration.set("claims.initial-blocks", integers[Integers.CLAIMS_INITIAL_BLOCKS.ordinal()]);
        outConfiguration.set("claims.blocks-accrued-per-hour", integers[Integers.CLAIMS_BLOCKS_ACCRUED_PER_HOUR.ordinal()]);
        outConfiguration.set("claims.max-accrued-blocks", integers[Integers.CLAIMS_MAX_ACCRUED_BLOCKS.ordinal()]);
        outConfiguration.set("claims.accrued-idle-threshold", integers[Integers.CLAIMS_ACCRUED_IDLE_THRESHOLD.ordinal()]);
        outConfiguration.set("claims.accrued-idle-percent", integers[Integers.CLAIMS_ACCRUED_IDLE_PERCENT.ordinal()]);
        outConfiguration.set("claims.expiration-days", integers[Integers.CLAIMS_EXPIRATION_DAYS.ordinal()]);
        outConfiguration.set("claims.automatic-claims-for-new-players-radius", integers[Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS.ordinal()]);
        outConfiguration.set("claims.min-width", integers[Integers.CLAIMS_MIN_WIDTH.ordinal()]);
        outConfiguration.set("claims.min-area", integers[Integers.CLAIMS_MIN_AREA.ordinal()]);
        outConfiguration.set("claims.chest-claim-expiration-days", integers[Integers.CLAIMS_CHEST_CLAIM_EXPIRATION_DAYS.ordinal()]);
        outConfiguration.set("claims.unused-claim-expiration-days", integers[Integers.CLAIMS_UNUSED_CLAIM_EXPIRATION_DAYS.ordinal()]);

        outConfiguration.set("claims.abandon-return-ratio", doubles[Doubles.CLAIMS_ABANDON_RETURN_RATIO.ordinal()]);

        outConfiguration.set("claims.investigation-tool", materials[Materials.INVESTIGATION_TOOL.ordinal()].name());
        outConfiguration.set("claims.modification-tool", materials[Materials.MODIFICATION_TOOL.ordinal()].name());

    }

    public void loadWorldClaimsModeMap(Map<World, ClaimsMode> worldClaimsModeMap, Boolean[] booleans) {
        worldClaimsModeMap.clear();
        booleans[Booleans.CREATIVE_WORLD_EXIST.ordinal()] = false;

        for (World world : Bukkit.getServer().getWorlds()) {
            String configSetting = configuration.getString("claims.mode." + world.getName());

            if (configSetting != null) {
                ClaimsMode claimsMode = configStringToClaimsMode(configSetting);
                if (claimsMode != null) {
                    worldClaimsModeMap.put(world, claimsMode);
                    if (claimsMode == ClaimsMode.Creative) {
                        booleans[Booleans.CREATIVE_WORLD_EXIST.ordinal()] = true;
                    }
                    continue;
                }
                EterniaKamui.AddLogEntry("ERR: 007F503D84");
                worldClaimsModeMap.put(world, ClaimsMode.Creative);
                booleans[Booleans.CREATIVE_WORLD_EXIST.ordinal()] = true;
            } else if (world.getName().toLowerCase().contains("survival") || world.getEnvironment() == World.Environment.NORMAL) {
                worldClaimsModeMap.put(world, ClaimsMode.Survival);
            } else if (world.getName().toLowerCase().contains("creative") || Bukkit.getServer().getDefaultGameMode() == GameMode.CREATIVE) {
                worldClaimsModeMap.put(world, ClaimsMode.Creative);
                booleans[Booleans.CREATIVE_WORLD_EXIST.ordinal()] = true;
            } else {
                worldClaimsModeMap.put(world, ClaimsMode.Disabled);
            }
        }

        for (World world : worldClaimsModeMap.keySet()) {
            outConfiguration.set("claims.mode." + world.getName(), worldClaimsModeMap.get(world).name());
        }
    }

    public void saveConfiguration() {
        try {
            outConfiguration.save(Constants.CONFIG_FILE_PATH);
        } catch (IOException ignored) {
            EterniaKamui.AddLogEntry("ERR: 001D5DFF85");
        }
    }

    private ClaimsMode configStringToClaimsMode(String configSetting) {
        switch (configSetting.toLowerCase()) {
            case "survival":
                return ClaimsMode.Survival;
            case "creative":
                return ClaimsMode.Creative;
            case "disabled":
                return ClaimsMode.Disabled;
            case "survivalrequiringclaims":
                return ClaimsMode.SurvivalRequiringClaims;
            default:
                return null;
        }
    }

}
