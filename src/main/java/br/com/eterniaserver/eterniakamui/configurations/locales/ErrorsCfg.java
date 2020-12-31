package br.com.eterniaserver.eterniakamui.configurations.locales;

import br.com.eterniaserver.eterniakamui.Constants;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ErrorsCfg {

    public ErrorsCfg() {

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(Constants.CONFIG_FILE_PATH));

        configuration.set("00F7E9E731.portuguese", "Erro ao tentar gerar uma nova pasta.");
        configuration.set("00F7E9E731.english", "Error when trying to generate a new folder.");

        configuration.set("001D5DFF85.portuguese", "Erro ao salvar arquivo.");
        configuration.set("001D5DFF85.english", "Error when saving file.");

        configuration.set("007F503D84.portuguese", "Modo de proteção inválido, as opções são: Survival, Creative e Disabled.");
        configuration.set("007F503D84.english", "Invalid claim mode, options are Survival, Creative and Disabled.");

        if (new File(Constants.DATA_LOCALE_FOLDER_PATH).mkdir()) {
            EterniaKamui.AddLogEntry("Erro ao tentar gerar uma nova pasta.");
            EterniaKamui.AddLogEntry("Error when trying to generate a new folder.");
        }

        try {
            configuration.save(Constants.ERRORS_FILE_PATH);
        } catch (IOException ignored) {
            EterniaKamui.AddLogEntry("Erro ao salvar arquivo.");
            EterniaKamui.AddLogEntry("Error when saving file.");
        }

    }

}
