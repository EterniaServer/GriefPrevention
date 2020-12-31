package br.com.eterniaserver.eterniakamui.objects;

public class ClaimFlag {

    private boolean keepLevel = false;
    private boolean explosions = false;
    private boolean creatureSpawn = true;
    private boolean allowPvP = false;
    private boolean liquidFluid = true;

    public boolean isKeepLevel() {
        return keepLevel;
    }

    public boolean isExplosions() {
        return explosions;
    }

    public boolean isCreatureSpawn() {
        return creatureSpawn;
    }

    public boolean isAllowPvP() {
        return allowPvP;
    }

    public boolean isLiquidFluid() {
        return liquidFluid;
    }

    public void setKeepLevel(int value) {
        keepLevel = value == 1;
    }

    public void setExplosions(int value) {
        explosions = value == 1;
    }

    public void setCreatureSpawn(int value) {
        creatureSpawn = value == 1;
    }

    public void setAllowPvP(int value) {
        allowPvP = value == 1;
    }

    public void setLiquidFluid(int value) {
        liquidFluid = value == 1;
    }

}
