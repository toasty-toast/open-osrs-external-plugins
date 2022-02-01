package net.runelite.client.plugins;

import net.runelite.api.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.iutils.LegacyMenuEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CleanHerbsActivity extends ActivityBase {
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

    private CleanHerbsState state = CleanHerbsState.UNKNOWN;
    private long ticksToWait = 0;

    @Override
    public String getActivityName() {
        return "Clean Herbs";
    }

    @Override
    public String getActivityState() {
        return state.toString();
    }

    @Override
    protected void onGameTick() {
        state = getState();

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
                cleanHerbs();
                break;
            case OPEN_BANK:
                openBank();
                break;
            case DEPOSIT_ITEMS:
                depositItems();
                break;
            case WITHDRAW_HERBS:
                withdrawHerbs();
                break;
            case CLOSE_BANK:
                closeBank();
                break;
        }
    }

    private CleanHerbsState getState() {
        if (client.getLocalPlayer().isMoving()) {
            return CleanHerbsState.MOVING;
        }

        if (client.getLocalPlayer().getAnimation() != AnimationID.IDLE) {
            return CleanHerbsState.ANIMATING;
        }

        List<Herb> validHerbs = getHerbsPlayerCanClean();
        List<Integer> grimyHerbIds = validHerbs.stream().map(herb -> herb.grimyId).collect(Collectors.toList());
        if (bankUtils.isOpen()) {
            if (inventoryUtils.containsExcept(grimyHerbIds)) {
                return CleanHerbsState.DEPOSIT_ITEMS;
            }
            if (inventoryUtils.isEmpty()) {
                return CleanHerbsState.WITHDRAW_HERBS;
            }
            return CleanHerbsState.CLOSE_BANK;
        }

        if (inventoryUtils.containsItem(grimyHerbIds)) {
            return CleanHerbsState.CLEAN_HERBS;
        }

        return CleanHerbsState.OPEN_BANK;
    }

    private void cleanHerbs() {
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

    private void openBank() {
        GameObject bank = objectUtils.findNearestBank();
        if (bank == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("", "", bank.getId(), bankUtils.getBankMenuOpcode(bank.getId()), bank.getSceneMinLocation().getX(), bank.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(menuEntry, bank.getConvexHull().getBounds(), calculateSleepDelay());
    }

    private void depositItems() {
        bankUtils.depositAll();
    }

    private void withdrawHerbs() {
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
            requestStop();
        }
    }

    private void closeBank() {
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

    private enum CleanHerbsState {
        UNKNOWN("Unknown"),
        MOVING("Moving"),
        ANIMATING("Animating"),
        CLEAN_HERBS("Clean Herbs"),
        OPEN_BANK("Open Bank"),
        DEPOSIT_ITEMS("Deposit Items"),
        WITHDRAW_HERBS("Withdraw Herbs"),
        CLOSE_BANK("Close Bank");

        private String name;

        CleanHerbsState(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
