package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.acf.BaseCommand;
import br.com.eterniaserver.acf.annotation.CommandAlias;
import br.com.eterniaserver.acf.annotation.CommandPermission;
import br.com.eterniaserver.acf.annotation.Description;
import br.com.eterniaserver.eternialib.EQueries;
import br.com.eterniaserver.eternialib.EterniaLib;
import br.com.eterniaserver.eternialib.sql.Connections;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseCmdFlags extends BaseCommand {

    protected static final Map<Integer, ItemStack> guiItensDisable = new HashMap<>();
    protected static final Map<Integer, ItemStack> guiItensEnable = new HashMap<>();

    public BaseCmdFlags() {

        if (EterniaLib.getMySQL()) {
            EterniaLib.getConnections().executeSQLQuery(connection -> {
                final PreparedStatement getHashMap = connection.prepareStatement("SELECT * FROM ek_flags;");
                final ResultSet resultSet = getHashMap.executeQuery();
                getFlags(resultSet);
                getHashMap.close();
                resultSet.close();
            });
        } else {
            try (PreparedStatement getHashMap = Connections.getSQLite().prepareStatement("SELECT * FROM ek_flags;"); ResultSet resultSet = getHashMap.executeQuery()) {
                getFlags(resultSet);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        ItemStack itemStack = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7Monster Spawn"));
        List<String> lore = List.of(getColor("&cDesativado"));
        List<String> lore2 = List.of(getColor("&aAtivado"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        guiItensDisable.put(2, itemStack);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore2);
        itemStack = new ItemStack(itemStack);
        itemStack.setItemMeta(meta);
        guiItensEnable.put(2, itemStack);
        itemStack = new ItemStack(Material.DIAMOND_SWORD);
        meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7PvP"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        guiItensDisable.put(3, itemStack);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore2);
        itemStack = new ItemStack(itemStack);
        itemStack.setItemMeta(meta);
        guiItensEnable.put(3, itemStack);
        itemStack = new ItemStack(Material.TNT);
        meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7Explosões"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        guiItensDisable.put(4, itemStack);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore2);
        itemStack = new ItemStack(itemStack);
        itemStack.setItemMeta(meta);
        guiItensEnable.put(4, itemStack);
        itemStack = new ItemStack(Material.WATER_BUCKET);
        meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7Liquid Flow"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        guiItensDisable.put(5, itemStack);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore2);
        itemStack = new ItemStack(itemStack);
        itemStack.setItemMeta(meta);
        guiItensEnable.put(5, itemStack);
        itemStack = new ItemStack(Material.EXPERIENCE_BOTTLE);
        meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7Keep Level"));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        guiItensDisable.put(6, itemStack);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore2);
        itemStack = new ItemStack(itemStack);
        itemStack.setItemMeta(meta);
        guiItensEnable.put(6, itemStack);
        itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        meta = itemStack.getItemMeta();
        meta.setDisplayName(getColor("&7Eternia Flags"));
        itemStack.setItemMeta(meta);
        guiItensDisable.put(0, itemStack);
        guiItensDisable.put(1, itemStack);
        guiItensDisable.put(7, itemStack);
        guiItensDisable.put(8, itemStack);

    }

    private void getFlags(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            ClaimFlag claimFlag = new ClaimFlag();
            claimFlag.setAllowPvP(resultSet.getInt("pvp"));
            claimFlag.setCreatureSpawn(resultSet.getInt("mobspawn"));
            claimFlag.setExplosions(resultSet.getInt("explosions"));
            claimFlag.setKeepLevel(resultSet.getInt("keeplevel"));
            claimFlag.setLiquidFluid(resultSet.getInt("fluid"));
            claimFlag.setEnterMessage(resultSet.getString("enterm"));
            claimFlag.setExitMessage(resultSet.getString("exitm"));
            PluginVars.claimFlags.put(resultSet.getLong("claimid"), claimFlag);
        }
    }

    private String getColor(String name) {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    @CommandAlias("flags")
    @CommandPermission("eternia.claim.user")
    @Description(" Altere as flags do terreno")
    public void onFlag(Player player) {
        Claim claim = EterniaKamui.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim != null) {
            if (claim.getOwnerName().equals(player.getName())) {
                if (!PluginVars.claimFlags.containsKey(claim.getID())) {
                    PluginVars.claimFlags.put(claim.getID(), new ClaimFlag());
                    EQueries.executeQuery("INSERT INTO ek_flags (claimid, mobspawn, fluid) VALUES ('" + claim.getID() + "', '" + 1 + "', '" + 1 + "');");
                }
                ClaimFlag claimFlag = PluginVars.claimFlags.get(claim.getID());
                Inventory gui = Bukkit.getServer().createInventory(player, 9, "EterniaFlags");
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
            } else {
                EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoOwner);
            }
        } else {
            EterniaKamui.sendMessage(player, TextMode.Err, Messages.NoClaim);
        }
    }

}