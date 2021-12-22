package net.runelite.client.plugins;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.eventbus.Subscribe;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Herbologist",
        description = "Does Herblore stuff"
)
@Slf4j
public class HerbologistPlugin extends Plugin {
    private static final Herb[] GRIMY_HERBS = new Herb[]{
            new Herb(ItemID.GRIMY_GUAM_LEAF, ItemID.GUAM_LEAF, 3),
            new Herb(ItemID.GRIMY_MARRENTILL, ItemID.MARRENTILL, 5),
            new Herb(ItemID.GRIMY_TARROMIN, ItemID.TARROMIN, 11),
            new Herb(ItemID.GRIMY_HARRALANDER, ItemID.HARRALANDER, 20),
            new Herb(ItemID.GRIMY_RANARR_WEED, ItemID.RANARR_WEED, 25),
            new Herb(ItemID.GRIMY_TOADFLAX, ItemID.TOADFLAX, 30),
            new Herb(ItemID.GRIMY_IRIT_LEAF, ItemID.IRIT_LEAF, 40),
            new Herb(ItemID.GRIMY_AVANTOE, ItemID.AVANTOE, 48),
            new Herb(ItemID.GRIMY_KWUARM, ItemID.KWUARM, 54),
            new Herb(ItemID.GRIMY_SNAPDRAGON, ItemID.SNAPDRAGON, 59),
            new Herb(ItemID.GRIMY_CADANTINE, ItemID.CADANTINE, 65),
            new Herb(ItemID.GRIMY_LANTADYME, ItemID.LANTADYME, 67),
            new Herb(ItemID.GRIMY_DWARF_WEED, ItemID.DWARF_WEED, 70),
            new Herb(ItemID.GRIMY_TORSTOL, ItemID.TORSTOL, 75)
    };

    private boolean isPluginRunning;

    private long ticksToWait = 0;

    @Inject
    private iUtils iUtils;

    @Inject
    private InventoryUtils inventoryUtils;

    @Inject
    private BankUtils bankUtils;

    @Inject
    private CalculationUtils calcUtils;

    @Inject
    private ObjectUtils objectUtils;

    @Inject
    private ActionQueue actionQueue;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ExecutorService executorService;

    @Inject
    private MouseUtils mouseUtils;

    @Inject
    private HerbologistConfig config;

    @Provides
    HerbologistConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbologistConfig.class);
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("Herbologist")) {
            return;
        }

        if (!configButtonClicked.getKey().equals("startButton")) {
            return;
        }

        if (!isPluginRunning) {
            ticksToWait = 0;
        }
        isPluginRunning = !isPluginRunning;
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (!isPluginRunning) {
            return;
        }

        HerbologistState state = getState();

        if (ticksToWait > 0) {
            ticksToWait--;
            return;
        }

        ticksToWait = calculateTickDelay();

        switch (state) {
            case UNKNOWN:
            case MOVING:
            case ANIMATING:
                break;
            case CLEAN_HERBS:
                handleCleanHerbsState();
                break;
            case OPEN_BANK:
                handleOpenBankState();
                break;
            case DEPOSIT_ITEMS:
                handleDepositItemsState();
                break;
            case WITHDRAW_HERBS:
                handleWithdrawHerbsState();
                break;
            case CLOSE_BANK:
                handleCloseBankState();
                break;
        }
    }

    /**
     * Gets the current plugin state.
     */
    private HerbologistState getState() {
        if (client.getLocalPlayer().isMoving()) {
            return HerbologistState.MOVING;
        }

        if (client.getLocalPlayer().getAnimation() != AnimationID.IDLE) {
            return HerbologistState.ANIMATING;
        }

        List<Herb> validHerbs = getHerbsPlayerCanClean();
        List<Integer> grimyHerbIds = validHerbs.stream().map(herb -> herb.grimyId).collect(Collectors.toList());
        if (bankUtils.isOpen()) {
            if (inventoryUtils.containsExcept(grimyHerbIds)) {
                return HerbologistState.DEPOSIT_ITEMS;
            }
            if (inventoryUtils.isEmpty()) {
                return HerbologistState.WITHDRAW_HERBS;
            }
            return HerbologistState.CLOSE_BANK;
        }

        if (inventoryUtils.containsItem(grimyHerbIds)) {
            return HerbologistState.CLEAN_HERBS;
        }

        return HerbologistState.OPEN_BANK;
    }

    private void handleCleanHerbsState() {
        // If all the clean actions are already queued, just return and wait for them to finish.
        if (!actionQueue.delayedActions.isEmpty()) {
            return;
        }

        List<Integer> grimyHerbIds = getHerbsPlayerCanClean().stream().map(herb -> herb.grimyId).collect(Collectors.toList());
        List<WidgetItem> herbWidgets = inventoryUtils.getItems(grimyHerbIds);
        long sleep = 0;
        for (WidgetItem herbWidget : herbWidgets) {
            LegacyMenuEntry menuEntry = new LegacyMenuEntry("", "", herbWidget.getId(), MenuAction.ITEM_FIRST_OPTION.getId(), herbWidget.getIndex(), WidgetInfo.INVENTORY.getId(), true);
            sleep += calculateSleepDelay();
            iUtils.doActionMsTime(menuEntry, herbWidget.getCanvasBounds().getBounds(), sleep);
        }
    }

    private void handleOpenBankState() {
        GameObject bankObject = objectUtils.findNearestBank();
        if (bankObject == null) {
            log.error("No bank found");
            isPluginRunning = false;
            return;
        }

        iUtils.doGameObjectActionMsTime(bankObject, MenuAction.GAME_OBJECT_SECOND_OPTION.getId(), calculateSleepDelay());
    }

    private void handleDepositItemsState() {
        bankUtils.depositAll();
    }

    private void handleWithdrawHerbsState() {
        if (inventoryUtils.isFull()) {
            return;
        }

        boolean anyHerbsToClean = false;
        List<Integer> herbsToWithdraw = getHerbsPlayerCanClean().stream().map(herb -> herb.grimyId).collect(Collectors.toList());
        for (Integer herbId : herbsToWithdraw) {
            if (bankUtils.contains(herbId, 1)) {
                bankUtils.withdrawAllItem(herbId);
                anyHerbsToClean = true;
                break;
            }
        }

        if (!anyHerbsToClean) {
            isPluginRunning = false;
        }
    }

    private void handleCloseBankState() {
        if (bankUtils.isOpen()) {
            bankUtils.close();
        }
    }

    /**
     * Gets the grimy herbs the player has the level to clean.
     */
    private List<Herb> getHerbsPlayerCanClean() {
        List<Herb> herbs = new ArrayList<>();
        for (Herb herb : GRIMY_HERBS) {
            if (client.getBoostedSkillLevel(Skill.HERBLORE) >= herb.levelRequirement) {
                herbs.add(herb);
            }
        }
        return herbs;
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