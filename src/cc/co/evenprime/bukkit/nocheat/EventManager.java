package cc.co.evenprime.bukkit.nocheat;

import java.util.List;

import cc.co.evenprime.bukkit.nocheat.config.cache.ConfigurationCache;

public interface EventManager {

    public List<String> getActiveChecks(ConfigurationCache cc);
}
