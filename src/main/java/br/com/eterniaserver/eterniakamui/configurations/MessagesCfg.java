package br.com.eterniaserver.eterniakamui.configurations;

import br.com.eterniaserver.eterniakamui.Constants;
import br.com.eterniaserver.eterniakamui.enums.Messages;
import br.com.eterniaserver.eternialib.core.baseobjects.CustomizableMessage;
import br.com.eterniaserver.eternialib.core.enums.ConfigurationCategory;
import br.com.eterniaserver.eternialib.core.interfaces.ReloadableConfiguration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessagesCfg implements ReloadableConfiguration {

    private final String[] messages;

    private final CustomizableMessage[] defaults = new CustomizableMessage[Messages.values().length];

    public MessagesCfg(final String[] messages) {
        this.messages = messages;
        addDefault(Messages.CLAIM_NOT_FOUND, "Não foi possível encontrar nenhuma proteção nessa localizaçao$8.");
        addDefault(Messages.WITHOUT_PERM, "Você não pode editar as flags de um terreno que não possui permissiontrust$8.");
        addDefault(Messages.WORLD_EXISTS, "Já existe um mundo com o nome de $3{0}$8.");
        addDefault(Messages.WORLD_CREATED, "O mundo $3{0}$7 foi criado$8.");
        addDefault(Messages.WORLD_REMOVED, "O mundo $3{0}$7 foi removido$8.");
        addDefault(Messages.WORLD_DELETED, "O mundo $3{0}$7 foi deletado$8.");
        addDefault(Messages.WORLD_NOT_FOUND, "O mundo $3{0}$7 não foi encontrado$8.");
        addDefault(Messages.WORLD_BASE, "O mundo é um mundo base$8.");
        addDefault(Messages.NO_PVP_IN_DAY, "Você não pode realizar um combate durante o dia no mundo normal$8!");
    }

    private void addDefault(final Messages id, final String text) {
        defaults[id.ordinal()] = new CustomizableMessage(text, null);
    }

    @Override
    public ConfigurationCategory category() {
        return ConfigurationCategory.GENERIC;
    }

    @Override
    public void executeConfig() {
        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(new File(Constants.MESSAGES_FILE_PATH));

        for (final Messages messageID : Messages.values()) {
            final CustomizableMessage messageData = defaults[messageID.ordinal()];

            messages[messageID.ordinal()] = messagesConfig.getString(messageID.name(), messageData.text).replace('$', (char) 0x00A7);
            messagesConfig.set(messageID.name(), messages[messageID.ordinal()]);
        }

        try {
            messagesConfig.save(Constants.MESSAGES_FILE_PATH);
        } catch (IOException ignored) {
            // todo
        }
    }

    @Override
    public void executeCritical() { }

}
