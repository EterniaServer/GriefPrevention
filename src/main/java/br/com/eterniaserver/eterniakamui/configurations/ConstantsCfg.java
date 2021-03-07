package br.com.eterniaserver.eterniakamui.configurations;

import br.com.eterniaserver.eterniakamui.Constants;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eternialib.core.enums.ConfigurationCategory;
import br.com.eterniaserver.eternialib.core.interfaces.ReloadableConfiguration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConstantsCfg implements ReloadableConfiguration {

    private final String[] strings;

    public ConstantsCfg(final String[] strings) {
        this.strings = strings;
    }

    @Override
    public ConfigurationCategory category() {
        return ConfigurationCategory.GENERIC;
    }

    @Override
    public void executeConfig() {
        // Load the configuration
        final FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(Constants.CONSTANTS_FILE_PATH));

        strings[Strings.CONS_SERVER_PREFIX.ordinal()] = configuration.getString("server.prefix", "$8[$aE$9K$8]$7 ").replace('$', (char) 0x00A7);
        strings[Strings.CONS_FLAG_DISABLED.ordinal()] = configuration.getString("flags.lore.disabled", "&cDesativado");
        strings[Strings.CONS_FLAG_ENABLED.ordinal()] = configuration.getString("flags.lore.enabled", "&aAtivado");
        strings[Strings.CONS_FLAG_MONSTER_SPAWN.ordinal()] = configuration.getString("flags.monster-spawn", "&7Monster Spawn");
        strings[Strings.CONS_FLAG_PVP.ordinal()] = configuration.getString("flags.pvp", "&7PvP");

        // Save the configuration
        final FileConfiguration outConfiguration = new YamlConfiguration();

        outConfiguration.set("server.prefix", strings[Strings.CONS_SERVER_PREFIX.ordinal()]);
        outConfiguration.set("flags.lore.disabled", strings[Strings.CONS_FLAG_DISABLED.ordinal()]);
        outConfiguration.set("flags.lore.enabled", strings[Strings.CONS_FLAG_ENABLED.ordinal()]);
        outConfiguration.set("flags.monster-spawn", strings[Strings.CONS_FLAG_MONSTER_SPAWN.ordinal()]);
        outConfiguration.set("flags.pvp", strings[Strings.CONS_FLAG_PVP.ordinal()]);

        if (new File(Constants.DATA_LOCALE_FOLDER_PATH).mkdir()) {
            // todo
        }

        try {
            outConfiguration.save(Constants.CONSTANTS_FILE_PATH);
        } catch (IOException ignored) {
            // todo
        }
    }

    @Override
    public void executeCritical() {

    }
}
