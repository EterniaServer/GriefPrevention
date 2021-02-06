package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.eterniakamui.objects.ClaimFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginVars {

    private PluginVars() {
        throw new IllegalStateException("Utility class");
    }

    public static final List<String> baseWorlds = List.of("world", "world_nether", "world_the_end");
    public static final List<String> types = List.of("flat", "amplified", "large_biomes", "normal");
    public static final List<String> enviroments = List.of("nether", "end", "normal");

    public static final List<String> worlds = new ArrayList<>();
    public static final Map<String, Integer> invClear = new HashMap<>();
    public static final Map<Long, ClaimFlag> claimFlags = new HashMap<>();

}
