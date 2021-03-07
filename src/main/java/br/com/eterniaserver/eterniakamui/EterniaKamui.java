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

import br.com.eterniaserver.eterniakamui.commands.Flags;
import br.com.eterniaserver.eterniakamui.commands.MultiVerse;
import br.com.eterniaserver.eterniakamui.configurations.ConfigsCfg;
import br.com.eterniaserver.eterniakamui.configurations.ConstantsCfg;
import br.com.eterniaserver.eterniakamui.configurations.MessagesCfg;
import br.com.eterniaserver.eterniakamui.core.UpdateWorldListTask;
import br.com.eterniaserver.eterniakamui.enums.*;
import br.com.eterniaserver.eterniakamui.handlers.PlayerHandler;
import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;
import br.com.eterniaserver.eternialib.CommandManager;
import br.com.eterniaserver.eternialib.EterniaLib;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EterniaKamui extends JavaPlugin {


    private final String[] strings = new String[Strings.values().length];
    private final String[] messages = new String[Messages.values().length];

    public final Map<Long, ClaimFlag> claimFlags = new HashMap<>();

    public final Set<String> worlds = new HashSet<>();
    public final List<String> baseWorlds = List.of("world", "world_nether", "world_the_end");
    public final List<String> types = List.of("flat", "amplified", "large_biomes", "normal");
    public final List<String> enviroments = List.of("nether", "end", "normal");

    public final Map<String, Integer> invClear = new HashMap<>();

    public String getString(final Strings entry) {
        return strings[entry.ordinal()];
    }

    //initializes well...   everything
    public void onEnable() {
        final ConstantsCfg constantsCfg = new ConstantsCfg(strings);
        final ConfigsCfg configsCfg = new ConfigsCfg(this, strings);
        final MessagesCfg messagesCfg = new MessagesCfg(messages);

        EterniaLib.addReloadableConfiguration("eterniakamui", "constants", constantsCfg);
        EterniaLib.addReloadableConfiguration("eterniakamui", "config", configsCfg);
        EterniaLib.addReloadableConfiguration("eterniakamui", "messages", messagesCfg);

        constantsCfg.executeConfig();
        configsCfg.executeConfig();
        messagesCfg.executeConfig();
        configsCfg.executeCritical();

        CommandManager.getCommandCompletions().registerStaticCompletion("worldenv", enviroments);
        CommandManager.getCommandCompletions().registerStaticCompletion("worldtyp", types);

        CommandManager.registerCommand(new MultiVerse(this));
        CommandManager.registerCommand(new Flags(this));

        new UpdateWorldListTask(this).runTaskTimer(this, 0L, 20L * 60);

        this.getServer().getPluginManager().registerEvents(new PlayerHandler(this), this);
    }

    public void sendMessage(CommandSender sender, Messages messagesId, String... args) {
        sendMessage(sender, messagesId, true, args);
    }

    public void sendMessage(CommandSender sender, Messages messagesId, boolean prefix, String... args) {
        sender.sendMessage(getMessage(messagesId, prefix, args));
    }

    public String getMessage(Messages messagesId, boolean prefix, String... args) {
        String message = messages[messagesId.ordinal()];

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }

        if (prefix) {
            return strings[Strings.CONS_SERVER_PREFIX.ordinal()] + message;
        }

        return message;
    }

}