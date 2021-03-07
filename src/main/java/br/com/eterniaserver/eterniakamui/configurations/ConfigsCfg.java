package br.com.eterniaserver.eterniakamui.configurations;

import br.com.eterniaserver.eterniakamui.Constants;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.configurations.load.LoadFlags;
import br.com.eterniaserver.eterniakamui.enums.*;
import br.com.eterniaserver.eternialib.EterniaLib;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.core.enums.ConfigurationCategory;
import br.com.eterniaserver.eternialib.core.interfaces.ReloadableConfiguration;
import br.com.eterniaserver.eternialib.core.queries.CreateTable;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigsCfg implements ReloadableConfiguration {

    private final EterniaKamui plugin;

    private final String[] strings;

    public ConfigsCfg(final EterniaKamui plugin,
                      final String[] strings) {
        this.plugin = plugin;
        this.strings = strings;
    }


    @Override
    public ConfigurationCategory category() {
        return ConfigurationCategory.WARNING_ADVICE;
    }

    @Override
    public void executeConfig() {
        // Load the configuration
        final FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(Constants.CONFIG_FILE_PATH));

        strings[Strings.TABLE_WORLDS.ordinal()] = configuration.getString("sql.table-worlds", "ek_worlds");
        strings[Strings.TABLE_FLAGS.ordinal()] = configuration.getString("sql.table-flags", "ek_claim_flags");

        // Save the configuration
        final FileConfiguration outConfiguration = new YamlConfiguration();

        outConfiguration.set("sql.table-worlds", strings[Strings.TABLE_WORLDS.ordinal()]);
        outConfiguration.set("sql.table-flags", strings[Strings.TABLE_FLAGS.ordinal()]);

        try {
            outConfiguration.save(Constants.CONFIG_FILE_PATH);
        } catch (IOException ignored) {

        }

    }

    @Override
    public void executeCritical() {
        createTables();

        new LoadFlags(plugin);
    }

    private void createTables() {
        CreateTable createTable;
        if (EterniaLib.getMySQL()) {
            createTable = new CreateTable(plugin.getString(Strings.TABLE_FLAGS));
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "claimid BIGINT(20)", "flag1 TINYINT(1)",
                    "flag2 TINYINT(1)", "flag3 TINYINT(1)", "flag4 TINYINT(1)", "flag5 TINYINT(1)", "flag6 TINYINT(1)", "flag7 TINYINT(1)",
                    "flag8 TINYINT(1)", "flag9 TINYINT(1)", "flag10 TINYINT(1)", "flag11 TINYINT(1)", "flag12 TINYINT(1)", "flag13 TINYINT(1)");
            SQL.execute(createTable);

            createTable = new CreateTable(plugin.getString(Strings.TABLE_WORLDS));
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "name VARCHAR(36)", "enviroment VARCHAR(36)",
                    "type VARCHAR(36)", "invclear TINYINT(1)");
            SQL.execute(createTable);
        } else {
            createTable = new CreateTable(plugin.getString(Strings.TABLE_FLAGS));
            createTable.columns.set("claimid BIGINT(20)", "flag1 TINYINT(1)",
                    "flag2 TINYINT(1)", "flag3 TINYINT(1)", "flag4 TINYINT(1)", "flag5 TINYINT(1)", "flag6 TINYINT(1)", "flag7 TINYINT(1)",
                    "flag8 TINYINT(1)", "flag9 TINYINT(1)", "flag10 TINYINT(1)", "flag11 TINYINT(1)", "flag12 TINYINT(1)", "flag13 TINYINT(1)");
            SQL.execute(createTable);

            createTable = new CreateTable(plugin.getString(Strings.TABLE_WORLDS));
            createTable.columns.set("name VARCHAR(36)", "enviroment VARCHAR(36)",
                    "type VARCHAR(36)", "invclear TINYINT(1)");
            SQL.execute(createTable);
        }

        SQL.execute(createTable);
    }

}
