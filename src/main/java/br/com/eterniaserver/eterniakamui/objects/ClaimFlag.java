package br.com.eterniaserver.eterniakamui.objects;

public class ClaimFlag {

    private int keepLevel = 0;
    private int explosions = 0;
    private int creatureSpawn = 0;
    private int allowPvP = 0;
    private int liquidFluid = 0;

    public boolean isKeepLevel() {
        return keepLevel == 1;
    }

    public boolean isExplosions() {
        return explosions == 1;
    }

    public boolean isCreatureSpawn() {
        return creatureSpawn == 1;
    }

    public boolean isAllowPvP() {
        return allowPvP == 1;
    }

    public boolean isLiquidFluid() {
        return liquidFluid == 1;
    }

    public void setKeepLevel(int value) {
        keepLevel = value;
    }

    public void setExplosions(int value) {
        explosions = value;
    }

    public void setCreatureSpawn(int value) {
        creatureSpawn = value;
    }

    public void setAllowPvP(int value) {
        allowPvP = value;
    }

    public void setLiquidFluid(int value) {
        liquidFluid = value;
    }

}
