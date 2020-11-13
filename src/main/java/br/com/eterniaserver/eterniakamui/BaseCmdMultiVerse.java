package br.com.eterniaserver.eterniakamui;

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
import br.com.eterniaserver.eterniakamui.enums.Messages;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.sql.queries.Delete;
import br.com.eterniaserver.eternialib.sql.queries.Insert;
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
public class BaseCmdMultiVerse extends BaseCommand {

    public BaseCmdMultiVerse() {

        try (Connection connection = SQL.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM ek_worlds;");
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                final String worldName = resultSet.getString("name").toLowerCase();
                PluginVars.invClear.put(worldName, resultSet.getInt("invclear"));
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
        if (!PluginVars.worlds.contains(worldName)) {
            createWorld(worldName, worldEnviroment, worldType);
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.WorldCreated, worldName, worldEnviroment, worldType, String.valueOf(invClear));
            PluginVars.invClear.put(worldName, invClear);

            Insert insert = new Insert("ek_worlds");
            insert.columns.set("name", "enviroment", "type", "invclear");
            insert.values.set(worldName, worldEnviroment, worldType, invClear);
            SQL.executeAsync(insert);
        } else {
            EterniaKamui.sendMessage(player, TextMode.Success, Messages.WorldAlready, worldName);
        }
    }

    @Subcommand("remove")
    @Syntax("<nome>")
    @CommandCompletion("@worlds")
    @Description(" Remova um mundo")
    public void onRemoveWorld(Player player, String worldName) {
        worldName = worldName.toLowerCase();

        if (removeWorld(player, worldName)) return;

        Bukkit.getServer().unloadWorld(worldName, true);
        EterniaKamui.sendMessage(player, TextMode.Success, Messages.WorldRemoved, worldName);
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

        Delete delete = new Delete("ek_worlds");
        delete.where.set("name", worldName);
        SQL.executeAsync(delete);

        EterniaKamui.sendMessage(player, TextMode.Success, Messages.WorldRemoved, worldName);
    }

    public boolean removeWorld(Player player, String worldName) {
        if (PluginVars.baseWorlds.contains(worldName)) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.WorldBase);
            return true;
        }

        if (!PluginVars.worlds.contains(worldName)) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.WorldNoExists);
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
        if (!PluginVars.worlds.contains(worldName)) {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.WorldNoExists);
            return;
        }

        PaperLib.teleportAsync(player, new Location(Bukkit.getWorld(worldName), x, y, z, 0, 0));
    }

    private void createWorld(String worldName, String worldEnviroment, String worldType) {
        WorldCreator worldCreator = WorldCreator.name(worldName);
        worldCreator.environment(getEnv(worldEnviroment));
        Bukkit.createWorld(getType(worldCreator, worldType));
        PluginVars.worlds.add(worldName);
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
