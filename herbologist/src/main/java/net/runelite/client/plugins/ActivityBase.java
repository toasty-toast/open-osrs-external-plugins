package net.runelite.client.plugins;

import net.runelite.api.Client;
import net.runelite.client.plugins.iutils.*;

import javax.inject.Inject;

public abstract class ActivityBase {
    private boolean isStopRequested = false;

    @Inject
    protected net.runelite.client.plugins.iutils.iUtils iUtils;

    @Inject
    protected InventoryUtils inventoryUtils;

    @Inject
    protected BankUtils bankUtils;

    @Inject
    protected CalculationUtils calcUtils;

    @Inject
    protected ObjectUtils objectUtils;

    @Inject
    protected ActionQueue actionQueue;

    @Inject
    protected Client client;

    @Inject
    protected HerbologistConfig config;

    public abstract String getActivityName();

    public abstract String getActivityState();

    protected abstract void onGameTick();

    protected long calculateTickDelay() {
        return calcUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
    }

    /**
     * Calculates a random delay before an action is performed.
     */
    protected long calculateSleepDelay() {
        return calcUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
    }

    protected void requestStop() {
        isStopRequested = true;
    }

    public boolean isStopRequested() {
        return isStopRequested;
    }

    public void reset() {
        isStopRequested = false;
    }
}
