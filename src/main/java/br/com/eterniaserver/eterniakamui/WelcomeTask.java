package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.eterniakamui.enums.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class WelcomeTask implements Runnable {
    private final Player player;

    public WelcomeTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        //abort if player has logged out since this task was scheduled
        if (!this.player.isOnline()) return;

        //offer advice and a helpful link
        EterniaKamui.sendMessage(player, TextMode.Instr, Messages.AvoidGriefClaimLand);
        EterniaKamui.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);

        //give the player a reference book for later
        if (EterniaKamui.instance.config_claims_supplyPlayerManual) {
            ItemFactory factory = Bukkit.getItemFactory();
            BookMeta meta = (BookMeta) factory.getItemMeta(Material.WRITTEN_BOOK);

            DataStore datastore = EterniaKamui.instance.dataStore;
            meta.setAuthor(datastore.getMessage(Messages.BookAuthor));
            meta.setTitle(datastore.getMessage(Messages.BookTitle));

            StringBuilder page1 = new StringBuilder();
            String URL = datastore.getMessage(Messages.BookLink, DataStore.SURVIVAL_VIDEO_URL);
            String intro = datastore.getMessage(Messages.BookIntro);

            page1.append(URL).append("\n\n");
            page1.append(intro).append("\n\n");
            String editToolName = EterniaKamui.instance.config_claims_modificationTool.name().replace('_', ' ').toLowerCase();
            String infoToolName = EterniaKamui.instance.config_claims_investigationTool.name().replace('_', ' ').toLowerCase();
            String configClaimTools = datastore.getMessage(Messages.BookTools, editToolName, infoToolName);
            page1.append(configClaimTools);
            if (EterniaKamui.instance.config_claims_automaticClaimsForNewPlayersRadius < 0) {
                page1.append(datastore.getMessage(Messages.BookDisabledChestClaims));
            }

            String page2 = datastore.getMessage(Messages.BookUsefulCommands) + "\n\n" +
                    "/Trust /UnTrust /TrustList\n" +
                    "/ClaimsList\n" +
                    "/AbandonClaim\n\n" +
                    "/Claim /ExtendClaim\n" +
                    "/IgnorePlayer\n\n" +
                    "/SubdivideClaims\n" +
                    "/AccessTrust\n" +
                    "/ContainerTrust\n" +
                    "/PermissionTrust";
            meta.setPages(page1.toString(), page2);

            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            item.setItemMeta(meta);
            player.getInventory().addItem(item);
        }

    }

}