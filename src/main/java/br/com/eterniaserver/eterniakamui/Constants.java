package br.com.eterniaserver.eterniakamui;

import java.io.File;

public class Constants {

    public static final String DATA_LAYER_FOLDER_PATH = "plugins" + File.separator + "EterniaKamui";
    public static final String DATA_LOCALE_FOLDER_PATH = Constants.DATA_LAYER_FOLDER_PATH + File.separator + "locales";

    public static final String CONFIG_FILE_PATH = Constants.DATA_LAYER_FOLDER_PATH + File.separator + "config.yml";
    public static final String MESSAGES_FILE_PATH = DATA_LOCALE_FOLDER_PATH + File.separator + "messages.yml";
    public static final String ERRORS_FILE_PATH = DATA_LOCALE_FOLDER_PATH + File.separator + "errors.yml";

}
