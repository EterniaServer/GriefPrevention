package br.com.eterniaserver.eterniakamui.configurations.configs;
import br.com.eterniaserver.eterniakamui.EterniaKamui;
import br.com.eterniaserver.eterniakamui.enums.Strings;
import br.com.eterniaserver.eternialib.EterniaLib;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.sql.queries.CreateTable;

public class TableCfg {

    public TableCfg() {
        CreateTable createTable;
        if (EterniaLib.getMySQL()) {
            createTable = new CreateTable(EterniaKamui.getString(Strings.TABLE_FLAGS));
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "claimid BIGINT(20)", "flag1 BOOLEAN",
                    "flag2 BOOLEAN", "flag3 BOOLEAN", "flag4 BOOLEAN", "flag5 BOOLEAN", "flag6 BOOLEAN", "flag7 BOOLEAN",
                    "flag8 BOOLEAN", "flag9 BOOLEAN", "flag10 BOOLEAN", "flag11 BOOLEAN", "flag12 BOOLEAN", "flag13 BOOLEAN");
            SQL.execute(createTable);

            createTable = new CreateTable(EterniaKamui.getString(Strings.TABLE_WORLDS));
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "name VARCHAR(36)", "enviroment VARCHAR(36)",
                    "type VARCHAR(36)", "invclear BOOLEAN");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_nextclaimid");
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "nextid INT(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_schemaversion");
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "version INT(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_claimdata");
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "id INT(15)", "owner VARCHAR(50)",
                    "lessercorner VARCHAR(100)", "greatercorner VARCHAR(100)", "builders TEXT", "containers TEXT",
                    "accessors TEXT", "managers TEXT", "inheritnothing BOOLEAN", "parentid INT(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_playerdata");
            createTable.columns.set("id INT AUTO_INCREMENT NOT NULL PRIMARY KEY", "name VARCHAR(50)", "lastlogin DATETIME",
                    "accruedblocks INT(15)", "bonusblocks INT(15)");
        } else {
            createTable = new CreateTable(EterniaKamui.getString(Strings.TABLE_FLAGS));
            createTable.columns.set("claimid BIGINT(20)", "flag1 BOOLEAN",
                    "flag2 BOOLEAN", "flag3 BOOLEAN", "flag4 BOOLEAN", "flag5 BOOLEAN", "flag6 BOOLEAN", "flag7 BOOLEAN",
                    "flag8 BOOLEAN", "flag9 BOOLEAN", "flag10 BOOLEAN", "flag11 BOOLEAN", "flag12 BOOLEAN", "flag13 BOOLEAN");
            SQL.execute(createTable);

            createTable = new CreateTable(EterniaKamui.getString(Strings.TABLE_WORLDS));
            createTable.columns.set("name VARCHAR(36)", "enviroment VARCHAR(36)",
                    "type VARCHAR(36)", "invclear BOOLEAN");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_nextclaimid");
            createTable.columns.set("nextid INTEGER(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_schemaversion");
            createTable.columns.set("version INTEGER(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_claimdata");
            createTable.columns.set("id INTEGER(15)", "owner VARCHAR(50)",
                    "lessercorner VARCHAR(100)", "greatercorner VARCHAR(100)", "builders TEXT", "containers TEXT",
                    "accessors TEXT", "managers TEXT", "inheritnothing BOOLEAN", "parentid INTEGER(15)");
            SQL.execute(createTable);

            createTable = new CreateTable("ek_playerdata");
            createTable.columns.set("name VARCHAR(50)", "lastlogin DATETIME",
                    "accruedblocks INTEGER(15)", "bonusblocks INTEGER(15)");
        }

        SQL.execute(createTable);

    }

}