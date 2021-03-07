package br.com.eterniaserver.eterniakamui.core;

import br.com.eterniaserver.eterniakamui.EterniaKamui;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateWorldListTask extends BukkitRunnable {

    private final EterniaKamui plugin;

    public UpdateWorldListTask(final EterniaKamui plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (final World world : plugin.getServer().getWorlds()) {
            this.plugin.worlds.add(world.getName());
        }
    }
}
