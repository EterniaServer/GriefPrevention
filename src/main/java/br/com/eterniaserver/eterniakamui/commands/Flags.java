package br.com.eterniaserver.eterniakamui.commands;

import br.com.eterniaserver.acf.BaseCommand;
import br.com.eterniaserver.acf.annotation.CommandAlias;
import br.com.eterniaserver.acf.annotation.CommandPermission;
import br.com.eterniaserver.acf.annotation.Description;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.enums.Messages;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;
import br.com.eterniaserver.eternialib.SQL;

import br.com.eterniaserver.eternialib.core.queries.Insert;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Flags extends BaseCommand {

    private final EterniaKamui plugin;

    public static final Map<Integer, ItemStack> guiItensDisable = new HashMap<>();
    public static final Map<Integer, ItemStack> guiItensEnable = new HashMap<>();

    public Flags(final EterniaKamui plugin) {
        this.plugin = plugin;
    }

    @CommandAlias("flags")
    @CommandPermission("eternia.claim.user")
    @Description(" Altere as flags do terreno")
    public void onFlag(final Player player) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, false, GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).lastClaim);

        if (claim == null) {
            plugin.sendMessage(player, Messages.CLAIM_NOT_FOUND);
            return;
        }

        if (claim.allowGrantPermission(player) != null) {
            plugin.sendMessage(player, Messages.WITHOUT_PERM);
            return;
        }

        openFlags(claim, player);
    }

    public void openFlags(final Claim claim, final Player player) {
        if (!plugin.claimFlags.containsKey(claim.getID())) {
            plugin.claimFlags.put(claim.getID(), new ClaimFlag());

            final Insert insert = new Insert(plugin.getString(Strings.TABLE_FLAGS));
            insert.columns.set("claimid", "flag1", "flag2", "flag3", "flag4", "flag5");
            insert.values.set(claim.getID(), 0, 0, 1, 1, 1);
            SQL.executeAsync(insert);
        }

        final ClaimFlag claimFlag = plugin.claimFlags.get(claim.getID());
        final Inventory gui = Bukkit.getServer().createInventory(player, 9, "EterniaFlags");

        gui.setItem(0, guiItensDisable.get(0));
        gui.setItem(1, guiItensDisable.get(1));
        gui.setItem(7, guiItensDisable.get(7));
        gui.setItem(8, guiItensDisable.get(8));

        if (claimFlag.isCreatureSpawn()) {
            gui.setItem(2, guiItensEnable.get(2));
        } else {
            gui.setItem(2, guiItensDisable.get(2));
        }

        if (claimFlag.isAllowPvP()) {
            gui.setItem(3, guiItensEnable.get(3));
        } else {
            gui.setItem(3, guiItensDisable.get(3));
        }

        if (claimFlag.isExplosions()) {
            gui.setItem(4, guiItensEnable.get(4));
        } else {
            gui.setItem(4, guiItensDisable.get(4));
        }

        if (claimFlag.isLiquidFluid()) {
            gui.setItem(5, guiItensEnable.get(5));
        } else {
            gui.setItem(5, guiItensDisable.get(5));
        }

        if (claimFlag.isKeepLevel()) {
            gui.setItem(6, guiItensEnable.get(6));
        } else {
            gui.setItem(6, guiItensDisable.get(6));
        }

        player.closeInventory();
        player.openInventory(gui);
    }

}