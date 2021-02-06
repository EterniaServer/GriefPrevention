package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.eterniakamui.configurations.configs.TableCfg;
import br.com.eterniaserver.eterniakamui.enums.CustomLogEntryTypes;
import br.com.eterniaserver.eternialib.SQL;
import br.com.eterniaserver.eternialib.UUIDFetcher;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

//manages data stored in the file system
public class DatabaseDataStore extends DataStore {

    private static final String SQL_UPDATE_NAME =
            "UPDATE ek_playerdata SET name = ? WHERE name = ?";
    private static final String SQL_INSERT_CLAIM =
            "INSERT INTO ek_claimdata (id, owner, lessercorner, greatercorner, builders, containers, accessors, managers, inheritnothing, parentid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_DELETE_CLAIM =
            "DELETE FROM ek_claimdata WHERE id = ?";
    private static final String SQL_SELECT_PLAYER_DATA =
            "SELECT * FROM ek_playerdata WHERE name = ?";
    private static final String SQL_DELETE_PLAYER_DATA =
            "DELETE FROM ek_playerdata WHERE name = ?";
    private static final String SQL_INSERT_PLAYER_DATA =
            "INSERT INTO ek_playerdata (name, lastlogin, accruedblocks, bonusblocks) VALUES (?, ?, ?, ?)";
    private static final String SQL_SET_NEXT_CLAIM_ID =
            "INSERT INTO ek_nextclaimid VALUES (?)";
    private static final String SQL_DELETE_GROUP_DATA =
            "DELETE FROM ek_playerdata WHERE name = ?";
    private static final String SQL_INSERT_SCHEMA_VERSION =
            "INSERT INTO ek_schemaversion VALUES (?)";
    private static final String SQL_DELETE_NEXT_CLAIM_ID =
            "DELETE FROM ek_nextclaimid";
    private static final String SQL_DELETE_SCHEMA_VERSION =
            "DELETE FROM ek_schemaversion";
    private static final String SQL_SELECT_SCHEMA_VERSION =
            "SELECT * FROM ek_schemaversion";

    DatabaseDataStore() throws Exception {
        this.initialize();
    }

    @Override
    void initialize() throws Exception {
        new TableCfg();

        try (Connection connection = SQL.getConnection();
             ResultSet resultSet = connection.prepareStatement("SELECT * FROM ek_nextclaimid;").executeQuery()) {
            if (!resultSet.next()) {
                this.setSchemaVersion(latestSchemaVersion);
            }
        } catch (SQLException exception) {
            EterniaKamui.AddLogEntry("Não foi possivel criar o arquivo necessário na database:");
            EterniaKamui.AddLogEntry(exception.getMessage());
        }

        //load group data into memory
        try (Connection connection = SQL.getConnection();
             ResultSet resultSet = connection.prepareStatement("SELECT * FROM ek_playerdata;").executeQuery()) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");

                //ignore non-groups.  all group names start with a dollar sign.
                if (!name.startsWith("$")) continue;

                String groupName = name.substring(1);
                if (groupName == null || groupName.isEmpty()) continue;  //defensive coding, avoid unlikely cases

                int groupBonusBlocks = resultSet.getInt("bonusblocks");

                this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
            }
        } catch (SQLException exception) {
            EterniaKamui.AddLogEntry("Não foi possível carregar a database dos jogadores:");
            EterniaKamui.AddLogEntry(exception.getMessage());
        }

        try (Connection connection = SQL.getConnection();
             ResultSet resultSet = connection.prepareStatement("SELECT * FROM ek_nextclaimid;").executeQuery()) {

            if (!resultSet.next()) {
                connection.prepareStatement("INSERT INTO ek_nextclaimid VALUES(0);").execute();
                this.nextClaimID = 0L;
            } else {
                this.nextClaimID = resultSet.getLong("nextid");
            }
        } catch (SQLException exception) {
            EterniaKamui.AddLogEntry("Não foi possível carregar o id do próximo claim, detalhes:");
            EterniaKamui.AddLogEntry(exception.getMessage());
        }

        if (this.getSchemaVersion() == 0) {
            try (Connection connection = SQL.getConnection();
                 ResultSet resultSet = connection.prepareStatement("SELECT * FROM ek_playerdata;").executeQuery()) {
                Map<String, UUID> changes = new HashMap<>();
                List<String> namesToConvert = new ArrayList<>();

                while (resultSet.next()) {
                    //get the id
                    String playerName = resultSet.getString("name");

                    //add to list of names to convert to UUID
                    namesToConvert.add(playerName);
                }

                for (String playerName : namesToConvert) {
                    UUIDFetcher.getUUIDOf(playerName);
                }

                resultSet.beforeFirst();

                while (resultSet.next()) {
                    String playerName = resultSet.getString("name");
                    changes.put(playerName, UUIDFetcher.getUUIDOf(playerName));
                }

                for (String name : changes.keySet()) {
                    try (PreparedStatement updateStmnt = connection.prepareStatement(SQL_UPDATE_NAME)) {
                        updateStmnt.setString(1, changes.get(name).toString());
                        updateStmnt.setString(2, name);
                        updateStmnt.executeUpdate();
                    } catch (SQLException e) {
                        EterniaKamui.AddLogEntry("Impossível de converter os arquivos de " + name + ".  Pulando.");
                        EterniaKamui.AddLogEntry(e.getMessage());
                    }
                }
            } catch (SQLException exception) {
                EterniaKamui.AddLogEntry("Não foi possível carregar os arquivos dos jogadores, detalhes:");
                EterniaKamui.AddLogEntry(exception.getMessage());
                exception.printStackTrace();
            }
        }

        if (this.getSchemaVersion() <= 2) {
            try (Connection connection = SQL.getConnection()) {
                connection.prepareStatement("ALTER TABLE ek_claimdata ADD inheritNothing BOOLEAN DEFAULT 0 AFTER managers;").execute();
            } catch (SQLException exception) {
                EterniaKamui.AddLogEntry("A database está fechada.");
            }
        }

        try (Connection connection = SQL.getConnection(); ResultSet resultSet = connection.prepareStatement("SELECT * FROM ek_claimdata;").executeQuery()) {
            List<Claim> claimsToRemove = new ArrayList<>();
            List<Claim> subdivisionsToLoad = new ArrayList<>();
            List<World> validWorlds = Bukkit.getServer().getWorlds();

            long claimID;
            while (resultSet.next()) {
                try {
                    long parentId = resultSet.getLong("parentid");
                    claimID = resultSet.getLong("id");
                    boolean inheritNothing = resultSet.getBoolean("inheritNothing");
                    Location lesserBoundaryCorner;
                    Location greaterBoundaryCorner;
                    String lesserCornerString = "(location not available)";
                    try {
                        lesserCornerString = resultSet.getString("lessercorner");
                        lesserBoundaryCorner = this.locationFromString(lesserCornerString, validWorlds);
                        String greaterCornerString = resultSet.getString("greatercorner");
                        greaterBoundaryCorner = this.locationFromString(greaterCornerString, validWorlds);
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                            EterniaKamui.AddLogEntry("Failed to load a claim (ID:" + claimID + ") because its world isn't loaded (yet?).  Please delete the claim or contact the GriefPrevention developer with information about which plugin(s) you're using to load or create worlds.  " + lesserCornerString);
                            continue;
                        } else {
                            throw e;
                        }
                    }

                    String ownerName = resultSet.getString("owner");
                    UUID ownerID;
                    if (ownerName.isEmpty() || ownerName.startsWith("--")) {
                        ownerID = null;  //administrative land claim or subdivision
                    } else if (this.getSchemaVersion() < 1) {
                        try {
                            ownerID = UUIDFetcher.getUUIDOf(ownerName);
                        } catch (Exception ex) {
                            EterniaKamui.AddLogEntry("This owner name did not convert to a UUID: " + ownerName + ".");
                            EterniaKamui.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
                            ownerID = null;
                        }
                    } else {
                        try {
                            ownerID = UUID.fromString(ownerName);
                        } catch (Exception ex) {
                            EterniaKamui.AddLogEntry("This owner entry is not a UUID: " + ownerName + ".");
                            EterniaKamui.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
                            ownerID = null;
                        }
                    }

                    String buildersString = resultSet.getString("builders");
                    List<String> builderNames = Arrays.asList(buildersString.split(";"));
                    builderNames = this.convertNameListToUUIDList(builderNames);

                    String containersString = resultSet.getString("containers");
                    List<String> containerNames = Arrays.asList(containersString.split(";"));
                    containerNames = this.convertNameListToUUIDList(containerNames);

                    String accessorsString = resultSet.getString("accessors");
                    List<String> accessorNames = Arrays.asList(accessorsString.split(";"));
                    accessorNames = this.convertNameListToUUIDList(accessorNames);

                    String managersString = resultSet.getString("managers");
                    List<String> managerNames = Arrays.asList(managersString.split(";"));
                    managerNames = this.convertNameListToUUIDList(managerNames);
                    Claim claim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builderNames, containerNames, accessorNames, managerNames, inheritNothing, claimID);

                    if (parentId == -1) {
                        //top level claim
                        this.addClaim(claim, false);
                    } else {
                        //subdivision
                        subdivisionsToLoad.add(claim);
                    }
                } catch (SQLException e) {
                    EterniaKamui.AddLogEntry("Unable to load a claim.  Details: " + e.getMessage() + " ... " + resultSet.toString());
                    e.printStackTrace();
                }
            }

            //add subdivisions to their parent claims
            for (Claim childClaim : subdivisionsToLoad) {
                //find top level claim parent
                Claim topLevelClaim = this.getClaimAt(childClaim.getLesserBoundaryCorner(), null);

                if (topLevelClaim == null) {
                    claimsToRemove.add(childClaim);
                    EterniaKamui.AddLogEntry("Removing orphaned claim subdivision: " + childClaim.getLesserBoundaryCorner().toString());
                    continue;
                }

                //add this claim to the list of children of the current top level claim
                childClaim.parent = topLevelClaim;
                topLevelClaim.children.add(childClaim);
                childClaim.inDataStore = true;
            }

            for (Claim claim : claimsToRemove) {
                this.deleteClaimFromSecondaryStorage(claim);
            }

        } catch (SQLException exception) {
            EterniaKamui.AddLogEntry("Erro ao carregar claims");
        }

        if (this.getSchemaVersion() <= 2) {
            try (Connection connection = SQL.getConnection()) {
                connection.prepareStatement("DELETE FROM ek_claimdata WHERE id='-1';").execute();
            } catch (SQLException exception) {
                EterniaKamui.AddLogEntry("A database está fechada.");
            }
        }

        super.initialize();
    }

    @Override
    synchronized void writeClaimToStorage(Claim claim) {
        //wipe out any existing data about this claim
        this.deleteClaimFromSecondaryStorage(claim);

        //write claim data to the database
        this.writeClaimData(claim);
    }

    //actually writes claim data to the database
    synchronized private void writeClaimData(Claim claim) {
        String lesserCornerString = this.locationToString(claim.getLesserBoundaryCorner());
        String greaterCornerString = this.locationToString(claim.getGreaterBoundaryCorner());
        String owner = "";
        if (claim.ownerID != null) owner = claim.ownerID.toString();

        ArrayList<String> builders = new ArrayList<>();
        ArrayList<String> containers = new ArrayList<>();
        ArrayList<String> accessors = new ArrayList<>();
        ArrayList<String> managers = new ArrayList<>();

        claim.getPermissions(builders, containers, accessors, managers);

        String buildersString = this.storageStringBuilder(builders);
        String containersString = this.storageStringBuilder(containers);
        String accessorsString = this.storageStringBuilder(accessors);
        String managersString = this.storageStringBuilder(managers);
        boolean inheritNothing = claim.getSubclaimRestrictions();
        long parentId = claim.parent == null ? -1 : claim.parent.id;

        try (Connection connection = SQL.getConnection();
             PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT_CLAIM)) {

            insertStmt.setLong(1, claim.id);
            insertStmt.setString(2, owner);
            insertStmt.setString(3, lesserCornerString);
            insertStmt.setString(4, greaterCornerString);
            insertStmt.setString(5, buildersString);
            insertStmt.setString(6, containersString);
            insertStmt.setString(7, accessorsString);
            insertStmt.setString(8, managersString);
            insertStmt.setBoolean(9, inheritNothing);
            insertStmt.setLong(10, parentId);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            EterniaKamui.AddLogEntry("Unable to save data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
            EterniaKamui.AddLogEntry(e.getMessage());
        }
    }

    //deletes a claim from the database
    @Override
    synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
        try (Connection connection = SQL.getConnection();
             PreparedStatement deleteStmnt = connection.prepareStatement(SQL_DELETE_CLAIM)) {
            deleteStmnt.setLong(1, claim.id);
            deleteStmnt.executeUpdate();
        } catch (SQLException e) {
            EterniaKamui.AddLogEntry("Unable to delete data for claim " + claim.id + ".  Details:");
            EterniaKamui.AddLogEntry(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    PlayerData getPlayerDataFromStorage(UUID playerID) {
        PlayerData playerData = new PlayerData();
        playerData.playerID = playerID;

        try (Connection connection = SQL.getConnection();
             PreparedStatement selectStmnt = connection.prepareStatement(SQL_SELECT_PLAYER_DATA)) {
            selectStmnt.setString(1, playerID.toString());
            ResultSet results = selectStmnt.executeQuery();

            //if data for this player exists, use it
            if (results.next()) {
                playerData.setAccruedClaimBlocks(results.getInt("accruedblocks"));
                playerData.setBonusClaimBlocks(results.getInt("bonusblocks"));
            }
        } catch (SQLException e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            EterniaKamui.AddLogEntry(playerID + " " + errors.toString(), CustomLogEntryTypes.Exception);
        }

        return playerData;
    }

    //saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
    @Override
    public void overrideSavePlayerData(UUID playerID, PlayerData playerData) {
        //never save data for the "administrative" account.  an empty string for player name indicates administrative account
        if (playerID == null) return;

        this.savePlayerData(playerID.toString(), playerData);
    }

    private void savePlayerData(String playerID, PlayerData playerData) {
        try (Connection connection = SQL.getConnection();
             PreparedStatement deleteStmnt = connection.prepareStatement(SQL_DELETE_PLAYER_DATA);
             PreparedStatement insertStmnt = connection.prepareStatement(SQL_INSERT_PLAYER_DATA)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(playerID));

            SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sqlFormat.format(new Date(player.getLastSeen()));
            deleteStmnt.setString(1, playerID);
            deleteStmnt.executeUpdate();

            insertStmnt.setString(1, playerID);
            insertStmnt.setString(2, dateString);
            insertStmnt.setInt(3, playerData.getAccruedClaimBlocks());
            insertStmnt.setInt(4, playerData.getBonusClaimBlocks());
            insertStmnt.executeUpdate();
        } catch (SQLException e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            EterniaKamui.AddLogEntry(playerID + " " + errors.toString(), CustomLogEntryTypes.Exception);
        }
    }

    @Override
    synchronized void incrementNextClaimID() {
        this.setNextClaimID(this.nextClaimID + 1);
    }

    //sets the next claim ID.  used by incrementNextClaimID() above, and also while migrating data from a flat file data store
    synchronized void setNextClaimID(long nextID) {
        this.nextClaimID = nextID;

        try (Connection connection = SQL.getConnection();
             PreparedStatement deleteStmnt = connection.prepareStatement(SQL_DELETE_NEXT_CLAIM_ID);
             PreparedStatement insertStmnt = connection.prepareStatement(SQL_SET_NEXT_CLAIM_ID)) {
            deleteStmnt.execute();
            insertStmnt.setLong(1, nextID);
            insertStmnt.executeUpdate();
        } catch (SQLException e) {
            EterniaKamui.AddLogEntry("Unable to set next claim ID to " + nextID + ".  Details:");
            EterniaKamui.AddLogEntry(e.getMessage());
        }
    }

    //updates the database with a group's bonus blocks
    @Override
    synchronized void saveGroupBonusBlocks(String groupName, int currentValue) {
        //group bonus blocks are stored in the player data table, with player name = $groupName
        try (Connection connection = SQL.getConnection();
             PreparedStatement deleteStmnt = connection.prepareStatement(SQL_DELETE_GROUP_DATA);
             PreparedStatement insertStmnt = connection.prepareStatement(SQL_INSERT_PLAYER_DATA)) {
            SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sqlFormat.format(new Date());
            deleteStmnt.setString(1, '$' + groupName);
            deleteStmnt.executeUpdate();

            insertStmnt.setString(1, '$' + groupName);
            insertStmnt.setString(2, dateString);
            insertStmnt.setInt(3, 0);
            insertStmnt.setInt(4, currentValue);
            insertStmnt.executeUpdate();
        } catch (SQLException e) {
            EterniaKamui.AddLogEntry("Unable to save data for group " + groupName + ".  Details:");
            EterniaKamui.AddLogEntry(e.getMessage());
        }
    }
    @Override
    protected int getSchemaVersionFromStorage() {
        try (Connection connection = SQL.getConnection();
             PreparedStatement selectStmnt = connection.prepareStatement(SQL_SELECT_SCHEMA_VERSION)) {
            ResultSet results = selectStmnt.executeQuery();

            //if there's nothing yet, assume 0 and add it
            if (!results.next()) {
                this.setSchemaVersion(0);
                return 0;
            }
            //otherwise return the value that's in the table
            else {
                return results.getInt("version");
            }
        } catch (SQLException e) {
            EterniaKamui.AddLogEntry("Unable to retrieve schema version from database.  Details:");
            EterniaKamui.AddLogEntry(e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    protected void updateSchemaVersionInStorage(int versionToSet) {
        try (Connection connection = SQL.getConnection();
             PreparedStatement deleteStmnt = connection.prepareStatement(SQL_DELETE_SCHEMA_VERSION);
             PreparedStatement insertStmnt = connection.prepareStatement(SQL_INSERT_SCHEMA_VERSION)) {
            deleteStmnt.execute();
            insertStmnt.setInt(1, versionToSet);
            insertStmnt.executeUpdate();
        } catch (SQLException exception) {
            EterniaKamui.AddLogEntry("Erro ao definir a proxima schema version para " + versionToSet + ".  Detalhes:");
            EterniaKamui.AddLogEntry(exception.getMessage());
        }
    }

    /**
     * Concats an array to a string divided with the ; sign
     *
     * @param input Arraylist with strings to concat
     * @return String with all values from input array
     */
    private String storageStringBuilder(ArrayList<String> input) {
        StringBuilder output = new StringBuilder();
        for (String string : input) {
            output.append(string).append(";");
        }
        return output.toString();
    }

}