package br.com.eterniaserver.eterniakamui.configurations.load;

import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.commands.Flags;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;
import br.com.eterniaserver.eternialib.SQL;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LoadFlags {

    private final List<String> loreEnable;
    private final List<String> loreDisable;

    public LoadFlags(final EterniaKamui plugin) {
        this.loreEnable = List.of(getColor(plugin.getString(Strings.CONS_FLAG_ENABLED)));
        this.loreDisable = List.of(getColor(plugin.getString(Strings.CONS_FLAG_DISABLED)));

        // Load the flags into Database
        try (Connection connection = SQL.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + plugin.getString(Strings.TABLE_FLAGS) + ";");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                final ClaimFlag claimFlag = new ClaimFlag();

                long claimID = resultSet.getLong("claimid");
                claimFlag.setCreatureSpawn(resultSet.getInt("flag1"));
                claimFlag.setAllowPvP(resultSet.getInt("flag2"));
                claimFlag.setExplosions(resultSet.getInt("flag3"));
                claimFlag.setKeepLevel(resultSet.getInt("flag4"));
                claimFlag.setLiquidFluid(resultSet.getInt("flag5"));

                plugin.claimFlags.put(claimID, claimFlag);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        // Load the default Monster Spawn Tags
        loadDefaultItens(Material.CARVED_PUMPKIN, 2, plugin.getString(Strings.CONS_FLAG_MONSTER_SPAWN), false);
        loadDefaultItens(Material.CARVED_PUMPKIN, 2, plugin.getString(Strings.CONS_FLAG_MONSTER_SPAWN), true);

        // Load the default PvP Tags
        loadDefaultItens(Material.DIAMOND_SWORD, 3, plugin.getString(Strings.CONS_FLAG_PVP), false);
        loadDefaultItens(Material.DIAMOND_SWORD, 3, plugin.getString(Strings.CONS_FLAG_PVP), true);

        // Load the default Explosions Tags
        loadDefaultItens(Material.TNT, 4, "&7Explosões", false);
        loadDefaultItens(Material.TNT, 4, "&7Explosões", true);

        // Load the default Liquid Flow Tags
        loadDefaultItens(Material.WATER_BUCKET, 5, "&7Liquid Flow", false);
        loadDefaultItens(Material.WATER_BUCKET, 5, "&7Liquid Flow", true);

        // Load the default Keep Level Tags
        loadDefaultItens(Material.EXPERIENCE_BOTTLE, 6, "&7Keep Level", false);
        loadDefaultItens(Material.EXPERIENCE_BOTTLE, 6, "&7Keep Level", true);

        // Just complete the other positions with glass
        loadDefaultItens(Material.BLACK_STAINED_GLASS_PANE, 0, "&7EK Flags", false);
        loadDefaultItens(Material.BLACK_STAINED_GLASS_PANE, 1, "&7EK Flags", false);
        loadDefaultItens(Material.BLACK_STAINED_GLASS_PANE, 7, "&7EK Flags", false);
        loadDefaultItens(Material.BLACK_STAINED_GLASS_PANE, 8, "&7EK Flags", false);
    }

    private void loadDefaultItens(final Material material, final int position, final String name, final boolean enabled) {
        final ItemStack itemStack = new ItemStack(material);
        final ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(getColor(name));
        if (enabled) {
            meta.setLore(loreEnable);
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        else {
            meta.setLore(loreDisable);
        }

        itemStack.setItemMeta(meta);

        if (enabled) Flags.guiItensEnable.put(position, itemStack);
        else Flags.guiItensDisable.put(position, itemStack);
    }

    private String getColor(String name) {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

}
