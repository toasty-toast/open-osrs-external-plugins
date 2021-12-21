package net.runelite.client.plugins;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.iutils.*;
import org.pf4j.Extension;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Karambwan Fisher",
        description = ""
)
@Slf4j
public class KarambwanFisherPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private iUtils iUtils;

    @Inject
    private InventoryUtils inventoryUtils;

    @Inject
    private BankUtils bankUtils;

    @Inject
    private CalculationUtils calcUtils;

    @Inject
    private KarambwanFisherConfig config;

    @Provides
    KarambwanFisherConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(KarambwanFisherConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {

    }

    private boolean doesInventoryContainFishBarrel() {
        return inventoryUtils.containsItem(ItemID.FISH_BARREL);
    }

    /**
     * Calculates the tick delay until the next plugin action is taken.
     */
    private long calculateTickDelay() {
        return calcUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
    }

    /**
     * Calculates a random delay before an action is performed.
     */
    private long calculateSleepDelay() {
        return calcUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
    }
}