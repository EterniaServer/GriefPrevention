package me.ryanhamshire.GriefPrevention;

public class ClaimFlag {

    private boolean keepLevel = false;
    private boolean explosions = false;
    private boolean creatureSpawn = true;
    private boolean allowPvP = false;
    private boolean liquidFluid = true;
    private String enterMessage;
    private String exitMessage;

    protected boolean isKeepLevel() {
        return keepLevel;
    }

    protected boolean isExplosions() {
        return explosions;
    }

    protected boolean isCreatureSpawn() {
        return creatureSpawn;
    }

    protected boolean isAllowPvP() {
        return allowPvP;
    }

    protected boolean isLiquidFluid() {
        return liquidFluid;
    }

    protected String getEnterMessage() {
        return enterMessage != null ? enterMessage : "";
    }

    protected String getExitMessage() {
        return exitMessage != null ? exitMessage : "";
    }

    protected void setKeepLevel(int value) {
        keepLevel = value == 1;
    }

    protected void setExplosions(int value) {
        explosions = value == 1;
    }

    protected void setCreatureSpawn(int value) {
        creatureSpawn = value == 1;
    }

    protected void setAllowPvP(int value) {
        allowPvP = value == 1;
    }

    protected void setLiquidFluid(int value) {
        liquidFluid = value == 1;
    }

    protected void setEnterMessage(String message) {
        if (message != null) {
            enterMessage = message;
        }
    }

    protected void setExitMessage(String message) {
        if (message != null) {
            exitMessage = message;
        }
    }

}
