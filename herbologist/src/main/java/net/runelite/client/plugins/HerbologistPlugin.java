package net.runelite.client.plugins;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import java.time.Instant;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Herbologist",
        description = "Does Herblore stuff"
)
@Slf4j
public class HerbologistPlugin extends Plugin {
    private boolean isPluginRunning;

    private ActivityBase currentActivity;

    private Instant pluginStartTime = Instant.now();

    @Inject
    private CleanHerbsActivity cleanHerbsActivity;

    @Inject
    private MakePrayerPotionActivity makePrayerPotionActivity;

    @Inject
    private MakeSuperAttackPotionActivity makeSuperAttackPotionActivity;

    @Inject
    private MakeSuperDefencePotionActivity makeSuperDefencePotionActivity;

    @Inject
    private MakeSuperEnergyPotionActivity makeSuperEnergyPotionActivity;

    @Inject
    private MakeSuperStrengthPotionActivity makeSuperStrengthPotionActivity;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HerbologistOverlay overlay;

    @Inject
    private HerbologistConfig config;

    @Inject
    private Client client;

    @Provides
    HerbologistConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbologistConfig.class);
    }

    @Override
    protected void shutDown() {
        if (isPluginRunning) {
            stopPlugin();
        }
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("Herbologist") || !configButtonClicked.getKey().equals("startButton")) {
            return;
        }

        if (isPluginRunning) {
            stopPlugin();
        } else {
            startPlugin();
        }
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (!isPluginRunning || currentActivity == null) {
            return;
        }

        if (currentActivity.isStopRequested() || client.getGameState() != GameState.LOGGED_IN) {
            this.stopPlugin();
            return;
        }

        currentActivity.onGameTick();
    }

    private void startPlugin() {
        switch (config.activity()) {
            case CLEAN_HERBS:
                currentActivity = cleanHerbsActivity;
                break;
            case MAKE_POTIONS:
                switch (config.potion()) {
                    case PRAYER:
                        currentActivity = makePrayerPotionActivity;
                        break;
                    case SUPER_ATTACK:
                        currentActivity = makeSuperAttackPotionActivity;
                        break;
                    case SUPER_DEFENCE:
                        currentActivity = makeSuperDefencePotionActivity;
                        break;
                    case SUPER_ENERGY:
                        currentActivity = makeSuperEnergyPotionActivity;
                        break;
                    case SUPER_STRENGTH:
                        currentActivity = makeSuperStrengthPotionActivity;
                        break;
                }
                break;
        }

        if (currentActivity != null) {
            currentActivity.reset();
            overlayManager.add(overlay);
            pluginStartTime = Instant.now();
            isPluginRunning = true;
        }
    }

    private void stopPlugin() {
        if (currentActivity != null) {
            currentActivity.reset();
            currentActivity = null;
        }
        overlayManager.remove(overlay);
        isPluginRunning = false;
    }

    public String getActivityName() {
        return currentActivity == null ? "None" : currentActivity.getActivityName();
    }

    public String getActivityState() {
        return currentActivity == null ? "None" : currentActivity.getActivityState();
    }

    public Instant getPluginStartTime() {
        return pluginStartTime;
    }
}