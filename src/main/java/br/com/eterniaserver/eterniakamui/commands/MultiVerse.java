package br.com.eterniaserver.eterniakamui.commands;

import br.com.eterniaserver.acf.BaseCommand;
import br.com.eterniaserver.acf.CommandHelp;
import br.com.eterniaserver.acf.annotation.CommandAlias;
import br.com.eterniaserver.acf.annotation.CommandCompletion;
import br.com.eterniaserver.acf.annotation.CommandPermission;
import br.com.eterniaserver.acf.annotation.Default;
import br.com.eterniaserver.acf.annotation.Description;
import br.com.eterniaserver.acf.annotation.HelpCommand;
import br.com.eterniaserver.acf.annotation.Subcommand;
import br.com.eterniaserver.acf.annotation.Syntax;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.enums.Messages;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.core.queries.Delete;
import br.com.eterniaserver.eternialib.core.queries.Insert;
import br.com.eterniaserver.paperlib.PaperLib;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@CommandAlias("mv|multiverse")
@CommandPermission("eternia.world")
public class MultiVerse extends BaseCommand {

    private final EterniaKamui plugin;

    public MultiVerse(final EterniaKamui plugin) {
        this.plugin = plugin;

        try (Connection connection = SQL.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + plugin.getString(Strings.TABLE_WORLDS) + ";");
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                final String worldName = resultSet.getString("name").toLowerCase();
                plugin.invClear.put(worldName, resultSet.getInt("invclear"));
                createWorld(worldName, resultSet.getString("enviroment").toLowerCase(), resultSet.getString("type").toLowerCase());
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    @Default
    @HelpCommand
    @Syntax("<página>")
    @Description(" Mostra ajudas para os comandos de multiworld")
    public void help(CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("create")
    @Syntax("<nome> <enviroment> <type> <invclear>(0 ou 1)")
    @CommandCompletion("world_teste @worldenv @worldtyp 0")
    @Description(" Crie e/ou carregue um mundo")
    public void onCreateWorld(Player player, String worldName, String worldEnviroment, String worldType, Integer invClear) {
        worldName = worldName.toLowerCase();
        worldEnviroment = worldEnviroment.toLowerCase();
        worldType = worldType.toLowerCase();

        if (plugin.worlds.contains(worldName)) {
            plugin.sendMessage(player, Messages.WORLD_EXISTS, worldName);
        }

        createWorld(worldName, worldEnviroment, worldType);
        plugin.invClear.put(worldName, invClear);

        final Insert insert = new Insert("ek_worlds");
        insert.columns.set("name", "enviroment", "type", "invclear");
        insert.values.set(worldName, worldEnviroment, worldType, invClear);
        SQL.executeAsync(insert);

        plugin.sendMessage(player, Messages.WORLD_CREATED, worldName);
    }

    @Subcommand("remove")
    @Syntax("<nome>")
    @CommandCompletion("@worlds")
    @Description(" Remova um mundo")
    public void onRemoveWorld(Player player, String worldName) {
        worldName = worldName.toLowerCase();

        if (removeWorld(player, worldName)) return;

        Bukkit.getServer().unloadWorld(worldName, true);
        plugin.sendMessage(player, Messages.WORLD_REMOVED, worldName);
    }

    @Subcommand("delete")
    @Syntax("<nome>")
    @CommandCompletion("@worlds")
    @Description(" Remova e delete um mundo")
    public void onDeleteWorld(Player player, String worldName) {
        worldName = worldName.toLowerCase();

        if (removeWorld(player, worldName)) return;

        Bukkit.getServer().unloadWorld(worldName, true);
        deleteDir(new File("." + File.separator + worldName));

        final Delete delete = new Delete("ek_worlds");
        delete.where.set("name", worldName);
        SQL.executeAsync(delete);

        plugin.sendMessage(player, Messages.WORLD_DELETED, worldName);
    }

    public boolean removeWorld(Player player, String worldName) {
        if (plugin.baseWorlds.contains(worldName)) {
            plugin.sendMessage(player, Messages.WORLD_BASE);
            return true;
        }

        if (!plugin.worlds.contains(worldName)) {
            plugin.sendMessage(player, Messages.WORLD_NOT_FOUND, worldName);
            return true;
        }

        return false;
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    @Subcommand("teleport|tp")
    @Syntax("<x> <y> <z> <nome>")
    @CommandCompletion("0 0 0 @worlds")
    @Description(" Teleporta-se até um mundo")
    public void onTp(Player player, Double x, Double y, Double z, String worldName) {
        worldName = worldName.toLowerCase();
        if (!plugin.worlds.contains(worldName)) {
            plugin.sendMessage(player, Messages.WORLD_NOT_FOUND, worldName);
            return;
        }

        PaperLib.teleportAsync(player, new Location(Bukkit.getWorld(worldName), x, y, z, 0, 0));
    }

    private void createWorld(String worldName, String worldEnviroment, String worldType) {
        WorldCreator worldCreator = WorldCreator.name(worldName);
        worldCreator.environment(getEnv(worldEnviroment));
        Bukkit.createWorld(getType(worldCreator, worldType));
        plugin.worlds.add(worldName);
    }

    private WorldCreator getType(WorldCreator worldCreator, String type) {
        switch (type) {
            case "flat":
                worldCreator.type(WorldType.FLAT);
                return worldCreator;
            case "amplified":
                worldCreator.type(WorldType.AMPLIFIED);
                return worldCreator;
            case "large_biomes":
                worldCreator.type(WorldType.LARGE_BIOMES);
                return worldCreator;
            default:
                worldCreator.type(WorldType.NORMAL);
                return worldCreator;
        }
    }

    private World.Environment getEnv(String name) {
        switch (name) {
            case "nether":
                return World.Environment.NETHER;
            case "end":
                return World.Environment.THE_END;
            default:
                return World.Environment.NORMAL;
        }
    }
    
}
