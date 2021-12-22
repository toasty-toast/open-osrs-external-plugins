package net.runelite.client.plugins;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.iutils.*;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Karambwan Fisher",
        description = "Fishes Karambwan and banks them in Zanaris"
)
@Slf4j
public class KarambwanFisherPlugin extends Plugin {
    private static final int FISH_BARREL_ID = 25582;
    private static final int OPEN_FISH_BARREL_ID = 25584;

    private static final int KARAMBWAN_FISHING_SPOT_ID = 4712;

    private static final int KARAMJA_FAIRY_RING_ID = 29495;
    private static final int ZANARIS_FAIRY_RING_ID = 29560;

    private boolean isPluginRunning;

    private long ticksToWait;

    @Inject
    private Client client;

    @Inject
    private iUtils iUtils;

    @Inject
    private InventoryUtils inventoryUtils;

    @Inject
    private PlayerUtils playerUtils;

    @Inject
    private NPCUtils npcUtils;

    @Inject
    private BankUtils bankUtils;

    @Inject
    private ObjectUtils objectUtils;

    @Inject
    private CalculationUtils calcUtils;

    @Inject
    private KarambwanFisherConfig config;

    @Inject
    private WalkUtils walkUtils;

    @Provides
    KarambwanFisherConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(KarambwanFisherConfig.class);
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("KarambwanFisher")) {
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

        if (!verifyPluginRequirements()) {
            isPluginRunning = false;
            return;
        }

        KarambwanFisherState state = getState();

        if (ticksToWait > 0) {
            ticksToWait--;
            return;
        }

        ticksToWait = calculateTickDelay();

        switch (state) {
            case MOVING:
            default:
                break;
            case OPEN_FISH_BARREL:
                openFishBarrel();
                break;
            case FISH_KARAMBWAN:
                startFishing();
                break;
            case GO_TO_ZANARIS:
                goToZanaris();
                break;
            case OPEN_BANK:
                openBank();
                break;
            case DEPOSIT_ITEMS:
                depositItems();
                break;
            case CLOSE_BANK:
                closeBank();
                break;
            case GO_TO_KARAMJA:
                goToKaramja();
                break;
        }
    }

    /**
     * Determines the current plugin state.
     */
    private KarambwanFisherState getState() {
        if (client.getLocalPlayer().isMoving()) {
            return KarambwanFisherState.MOVING;
        }
        if (client.getLocalPlayer().getAnimation() != -1) {
            return KarambwanFisherState.ANIMATING;
        }

        if (inventoryUtils.containsItem(FISH_BARREL_ID) && !bankUtils.isOpen()) {
            return KarambwanFisherState.OPEN_FISH_BARREL;
        }

        NPC karambwanFishingSpot = npcUtils.findNearestNpc(KARAMBWAN_FISHING_SPOT_ID);
        if (karambwanFishingSpot != null) {
            if (inventoryUtils.isFull()) {
                return KarambwanFisherState.GO_TO_ZANARIS;
            } else {
                return KarambwanFisherState.FISH_KARAMBWAN;
            }
        }

        if (bankUtils.isOpen()) {
            if (inventoryUtils.isFull()) {
                return KarambwanFisherState.DEPOSIT_ITEMS;
            } else {
                return KarambwanFisherState.CLOSE_BANK;
            }
        }

        GameObject bank = objectUtils.findNearestBank();
        if (bank != null) {
            if (inventoryUtils.isFull()) {
                return KarambwanFisherState.OPEN_BANK;
            } else {
                return KarambwanFisherState.GO_TO_KARAMJA;
            }
        }

        return KarambwanFisherState.UNKNOWN;
    }

    /**
     * Opens the fish barrel in the player's inventory.
     */
    private void openFishBarrel() {
        WidgetItem fishBarrel = getFishBarrelInventoryItem();
        if (fishBarrel == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("Open", "", fishBarrel.getId(), MenuAction.ITEM_THIRD_OPTION.getId(), fishBarrel.getIndex(), WidgetInfo.INVENTORY.getId(), false);
        iUtils.doActionMsTime(menuEntry, fishBarrel.getCanvasBounds().getBounds(), calculateSleepDelay());
    }

    /**
     * Starts fishing at the Karambwan fishing spot.
     */
    private void startFishing() {
        NPC karambwanFishingSpot = npcUtils.findNearestNpc(KARAMBWAN_FISHING_SPOT_ID);
        if (karambwanFishingSpot == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("Fish", "", karambwanFishingSpot.getIndex(), MenuAction.NPC_FIRST_OPTION.getId(), 0, 0, false);
        iUtils.doActionMsTime(menuEntry, karambwanFishingSpot.getConvexHull().getBounds(), calculateSleepDelay());
    }

    /**
     * Uses the Karamja fishing spot fairy ring to travel to Zanaris.
     */
    private void goToZanaris() {
        GameObject fairyRing = objectUtils.findNearestGameObject(KARAMJA_FAIRY_RING_ID);
        if (fairyRing == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("Zanaris", "", fairyRing.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), fairyRing.getSceneMinLocation().getX(), fairyRing.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(menuEntry, fairyRing.getConvexHull().getBounds(), calculateSleepDelay());
    }

    /**
     * Uses the Zanaris fairy ring to go to the Karambwan fishing area on Karamja.
     */
    private void goToKaramja() {
        GameObject fairyRing = objectUtils.findNearestGameObject(ZANARIS_FAIRY_RING_ID);
        if (fairyRing == null) {
            return;
        }

        if (!isFairyRingCodeCached("DKP")) {
            iUtils.sendGameMessage("Fairy ring code DKP must already be the \"last destination\"");
            isPluginRunning = false;
            return;
        }

        useCachedFairyRingCode();
    }

    /**
     * Opens the nearest bank.
     */
    private void openBank() {
        GameObject bank = objectUtils.findNearestBank();
        if (bank == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("", "", bank.getId(), bankUtils.getBankMenuOpcode(bank.getId()), bank.getSceneMinLocation().getX(), bank.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(menuEntry, bank.getConvexHull().getBounds(), calculateSleepDelay());
    }

    /**
     * Deposits all items not necessary for fishing.
     */
    private void depositItems() {
        WidgetItem fishBarrel = getFishBarrelInventoryItem();
        if (fishBarrel != null) {
            LegacyMenuEntry menuEntry = new LegacyMenuEntry("Empty", "", 9, MenuAction.CC_OP_LOW_PRIORITY.getId(), fishBarrel.getIndex(), WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId(), false);
            iUtils.doActionMsTime(menuEntry, fishBarrel.getCanvasBounds().getBounds(), calculateSleepDelay());
        }
        bankUtils.depositAllExcept(new ArrayList<>(Arrays.asList(ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159, ItemID.RAW_KARAMBWANJI, FISH_BARREL_ID, OPEN_FISH_BARREL_ID)));
    }

    /**
     * Closes the bank interface.
     */
    private void closeBank() {
        if (bankUtils.isOpen()) {
            bankUtils.close();
        }
    }

    /**
     * Uses whatever the cached "last destination" fairy ring code on the nearest fairy ring.
     */
    private void useCachedFairyRingCode() {
        GameObject fairyRing = new GameObjectQuery()
                .result(client)
                .list
                .stream()
                .filter(gameObj ->
                {
                    ObjectComposition objComp = client.getObjectDefinition(gameObj.getId());
                    if (objComp.getImpostorIds() != null) {
                        objComp = objComp.getImpostor();
                    }
                    return objComp != null && objComp.getName() != null && objComp.getName().equalsIgnoreCase("Fairy ring");
                })
                .findFirst()
                .orElse(null);
        if (fairyRing == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("", "", fairyRing.getId(), MenuAction.GAME_OBJECT_THIRD_OPTION, fairyRing.getSceneMinLocation().getX(), fairyRing.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(menuEntry, fairyRing.getConvexHull().getBounds(), calculateSleepDelay());
    }

    /**
     * Checks whether a fairy ring code is cached as the "last destination".
     */
    private boolean isFairyRingCodeCached(String fairyRingCode) {
        String[] actions = new GameObjectQuery()
                .result(client)
                .list
                .stream()
                .map(gameObj -> client.getObjectDefinition(gameObj.getId()))
                .map(objDef -> objDef.getImpostorIds() != null ? objDef.getImpostor() : objDef)
                .filter(java.util.Objects::nonNull)
                .filter(objDef -> objDef.getName().equalsIgnoreCase("Fairy Ring"))
                .findFirst()
                .map(objDef -> objDef.getActions())
                .orElse(null);
        if (actions == null) {
            return false;
        }
        return Arrays.stream(actions)
                .filter(java.util.Objects::nonNull)
                .anyMatch(action -> action.equalsIgnoreCase("Last-destination (" + fairyRingCode + ")"));
    }

    /**
     * Gets the fish barrel item in the player's inventory if there is one.
     */
    private WidgetItem getFishBarrelInventoryItem() {
        List<WidgetItem> fishBarrelItems = inventoryUtils.getItems(new ArrayList<>(Arrays.asList(FISH_BARREL_ID, OPEN_FISH_BARREL_ID)));
        if (fishBarrelItems.isEmpty()) {
            return null;
        }
        return fishBarrelItems.get(0);
    }

    /**
     * Checks that the player equipment and inventory meets requirements for the plugin to work.
     */
    private boolean verifyPluginRequirements() {
        if (client.getVarbitValue(Varbits.DIARY_LUMBRIDGE_ELITE.getId()) == 0 && !playerUtils.getEquippedItems().stream().anyMatch(item -> item.getId() == ItemID.DRAMEN_STAFF || item.getId() == ItemID.LUNAR_STAFF)) {
            iUtils.sendGameMessage("Dramen or Lunar staff must be equipped in order to use fairy rings");
            return false;
        }
        if (!inventoryUtils.containsItem(Collections.singletonList(ItemID.KARAMBWAN_VESSEL_3159))) {
            iUtils.sendGameMessage("Missing Karambwan Vessel");
            return false;
        }
        if (!inventoryUtils.containsItem(Collections.singletonList(ItemID.RAW_KARAMBWANJI))) {
            iUtils.sendGameMessage("Out of Karambwanji");
            return false;
        }
        return true;
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