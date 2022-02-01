package net.runelite.client.plugins;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.iutils.LegacyMenuEntry;

import java.util.Arrays;
import java.util.List;

public abstract class MakePotionActivityBase extends ActivityBase {
    private static final int MULTI_SKILL_MENU_MAKE_COUNT_OPTIONS_CONTAINER = 6;
    private static final int MULTI_SKILL_MENU_OPTIONS_CONTAINER_ID = 13;

    private final String activityName;
    private final List<Integer> unfinishedPotionIngredients;
    private final List<Integer> finishedPotionIngredients;
    private final List<Integer> allIngredients;
    private MakePotionState state = MakePotionState.UNKNOWN;
    private long ticksToWait;

    protected MakePotionActivityBase(String activityName, int herbId, int secondaryId, int unfinishedPotionId) {
        this.activityName = activityName;
        unfinishedPotionIngredients = Arrays.asList(ItemID.VIAL_OF_WATER, herbId);
        finishedPotionIngredients = Arrays.asList(unfinishedPotionId, secondaryId);
        allIngredients = Arrays.asList(ItemID.VIAL_OF_WATER, herbId, secondaryId, unfinishedPotionId);
    }

    @Override
    public String getActivityName() {
        return activityName;
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
            case MOVING:
            case ANIMATING:
                break;
            case OPEN_BANK:
                openBank();
                break;
            case CLOSE_BANK:
                closeBank();
                break;
            case DEPOSIT_ITEMS:
                depositItems();
                break;
            case WITHDRAW_UNFINISHED_INGREDIENTS:
                withdrawUnfinishedPotionIngredients();
                break;
            case MAKE_UNFINISHED_POTION:
                makePotions(unfinishedPotionIngredients.get(0), unfinishedPotionIngredients.get(1));
                break;
            case WITHDRAW_FINISHED_INGREDIENTS:
                withdrawFinishedPotionIngredients();
                break;
            case MAKE_FINISHED_POTION:
                makePotions(finishedPotionIngredients.get(0), finishedPotionIngredients.get(1));
                break;
        }
    }

    private MakePotionState getState() {
        if (client.getLocalPlayer().isMoving()) {
            return MakePotionState.MOVING;
        }

        if (client.getLocalPlayer().getAnimation() != AnimationID.IDLE) {
            return MakePotionState.ANIMATING;
        }

        if (bankUtils.isOpen()) {
            // If we have all the ingredients to make unfinished or finished potions we can close the bank.
            if (unfinishedPotionIngredients.stream().allMatch(ingredient -> inventoryUtils.containsItem(ingredient))
                    || finishedPotionIngredients.stream().allMatch(ingredient -> inventoryUtils.containsItem(ingredient))) {
                return MakePotionState.CLOSE_BANK;
            }

            if (config.makeUnfinishedPotions()
                    && unfinishedPotionIngredients.stream().allMatch(ingredient -> bankUtils.contains(ingredient, 1) || inventoryUtils.containsItem(ingredient))
                    && (inventoryUtils.isEmpty() || inventoryUtils.onlyContains(unfinishedPotionIngredients))) {
                return MakePotionState.WITHDRAW_UNFINISHED_INGREDIENTS;
            }

            if (finishedPotionIngredients.stream().allMatch(ingredient -> bankUtils.contains(ingredient, 1) || inventoryUtils.containsItem(ingredient))
                    && (!config.makeUnfinishedPotions() || !unfinishedPotionIngredients.stream().allMatch(ingredient -> bankUtils.contains(ingredient, 1) || inventoryUtils.containsItem(ingredient)))
                    && (inventoryUtils.isEmpty() || inventoryUtils.onlyContains(finishedPotionIngredients))) {
                return MakePotionState.WITHDRAW_FINISHED_INGREDIENTS;
            }

            if (!inventoryUtils.isEmpty() && !inventoryUtils.containsAllOf(unfinishedPotionIngredients) && !inventoryUtils.containsAllOf(finishedPotionIngredients)) {
                return MakePotionState.DEPOSIT_ITEMS;
            }

            // We're out of ingredients so stop the plugin here.
            requestStop();
        }

        if (config.makeUnfinishedPotions() && inventoryUtils.containsAllOf(unfinishedPotionIngredients)) {
            return MakePotionState.MAKE_UNFINISHED_POTION;
        }
        if (inventoryUtils.containsAllOf(finishedPotionIngredients)) {
            return MakePotionState.MAKE_FINISHED_POTION;
        }

        return MakePotionState.OPEN_BANK;
    }

    private void openBank() {
        GameObject bank = objectUtils.findNearestBank();
        if (bank == null) {
            return;
        }

        LegacyMenuEntry menuEntry = new LegacyMenuEntry("", "", bank.getId(), bankUtils.getBankMenuOpcode(bank.getId()), bank.getSceneMinLocation().getX(), bank.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(menuEntry, bank.getConvexHull().getBounds(), calculateSleepDelay());
    }

    private void closeBank() {
        if (bankUtils.isOpen()) {
            bankUtils.close();
        }
    }

    private void depositItems() {
        bankUtils.depositAll();
    }

    private void withdrawUnfinishedPotionIngredients() {
        int itemToWithdraw = unfinishedPotionIngredients.stream().filter(item -> !inventoryUtils.containsItem(item)).findFirst().get();
        if (!bankUtils.contains(itemToWithdraw, 1)) {
            return;
        }
        bankUtils.withdrawItemAmount(itemToWithdraw, 14);
    }

    private void withdrawFinishedPotionIngredients() {
        int itemToWithdraw = finishedPotionIngredients.stream().filter(item -> !inventoryUtils.containsItem(item)).findFirst().get();
        if (!bankUtils.contains(itemToWithdraw, 1)) {
            return;
        }
        bankUtils.withdrawItemAmount(itemToWithdraw, 14);
    }

    private void makePotions(int ingredient1Id, int ingredient2Id) {
        if (!actionQueue.delayedActions.isEmpty()) {
            return;
        }

        Widget makePotionWidget = getMakePotionWidget();
        if (makePotionWidget != null) {
            Widget makeAllWidget = getMakeAllWidget();
            if (makeAllWidget != null) {
                String[] makeAllActions = makeAllWidget.getActions();
                if (makeAllActions != null && makeAllActions.length > 0) {
                    LegacyMenuEntry makeAllMenuEntry = new LegacyMenuEntry(makeAllActions[0], "", 1, MenuAction.CC_OP, -1, makeAllWidget.getId(), false);
                    iUtils.doActionMsTime(makeAllMenuEntry, makeAllWidget.getBounds(), calculateSleepDelay());
                    return;
                }
            }

            String[] makePotionActions = makePotionWidget.getActions();
            if (makePotionActions == null || makePotionActions.length == 0) {
                return;
            }
            LegacyMenuEntry makePotionMenuEntry = new LegacyMenuEntry(makePotionActions[0], makePotionWidget.getRSName(), 1, MenuAction.CC_OP, -1, makePotionWidget.getId(), false);
            iUtils.doActionMsTime(makePotionMenuEntry, makePotionWidget.getBounds(), calculateSleepDelay());
        } else {
            WidgetItem item1Widget = findInventoryItemWidget(ingredient1Id);
            WidgetItem item2Widget = findInventoryItemWidget(ingredient2Id);

            if (item1Widget == null || item2Widget == null) {
                return;
            }

            long action1SleepDelay = calculateSleepDelay();
            long action2SleepDelay = action1SleepDelay + calculateSleepDelay();

            LegacyMenuEntry useItemMenuEntry = new LegacyMenuEntry("Use", "Use", ingredient1Id, MenuAction.ITEM_USE, item1Widget.getIndex(), 9764864, false);
            iUtils.doActionMsTime(useItemMenuEntry, item1Widget.getCanvasBounds().getBounds(), action1SleepDelay);

            String useItemOnMenuTarget = "<col=ff9040>" + getItemName(ingredient1Id) + "<col=ffffff> -> <col=ff9040>" + getItemName(ingredient2Id);
            LegacyMenuEntry useItemOnMenuEntry = new LegacyMenuEntry("Use", useItemOnMenuTarget, ingredient2Id, MenuAction.ITEM_USE_ON_WIDGET_ITEM, item2Widget.getIndex(), 9764864, false);
            iUtils.doActionMsTime(useItemOnMenuEntry, item2Widget.getCanvasBounds().getBounds(), action2SleepDelay);
        }
    }

    private String getItemName(int itemId) {
        return client.getItemDefinition(itemId).getName();
    }

    private WidgetItem findInventoryItemWidget(int itemId) {
        final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        final List<WidgetItem> itemWidgetList = (List<WidgetItem>) inventoryWidget.getWidgetItems();
        for (WidgetItem itemWidget : itemWidgetList) {
            if (itemWidget.getId() == itemId) {
                return itemWidget;
            }
        }
        return null;
    }

    private Widget getMakePotionWidget() {
        Widget makeOptionsContainer = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, MULTI_SKILL_MENU_OPTIONS_CONTAINER_ID);
        if (makeOptionsContainer == null) {
            return null;
        }

        Widget[] makeOptions = makeOptionsContainer.getStaticChildren();
        if (makeOptions == null) {
            return null;
        }

        Widget singleMakeOption = null;
        int optionCount = 0;
        for (Widget makeOption : makeOptions) {
            // All the widgets that would show multiple items that can be made are always present,
            // but they will only have child widgets if they are actually visible being used to show an item.
            if (makeOption.getChildren() != null && !makeOption.isHidden()) {
                return makeOption;
            }
        }

        return null;
    }

    private Widget getMakeAllWidget() {
        Widget container = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, MULTI_SKILL_MENU_MAKE_COUNT_OPTIONS_CONTAINER);
        if (container == null) {
            return null;
        }

        Widget[] children = container.getStaticChildren();
        if (children == null) {
            return null;
        }

        for (Widget child : children) {
            String childText = getFirstTextInWidget(child);
            if (childText != null && childText.equals("All")) {
                return child;
            }
        }

        return null;
    }

    private String getFirstTextInWidget(Widget widget) {
        String widgetText = widget.getText();
        if (widgetText != null && widgetText.length() > 0) {
            return widget.getText();
        }

        Widget[] children = widget.getChildren();
        if (children == null) {
            return null;
        }

        for (Widget child : children) {
            String childText = child.getText();
            if (childText != null && childText.length() > 0) {
                return childText;
            }

            String childSubText = getFirstTextInWidget(child);
            if (childSubText != null) {
                return childSubText;
            }
        }

        return null;
    }

    private enum MakePotionState {
        UNKNOWN("Unknown"),
        MOVING("Moving"),
        ANIMATING("Animating"),
        OPEN_BANK("Open Bank"),
        WITHDRAW_UNFINISHED_INGREDIENTS("Withdraw Ingredients"),
        WITHDRAW_FINISHED_INGREDIENTS("Withdraw Ingredients"),
        DEPOSIT_ITEMS("Deposit Items"),
        CLOSE_BANK("Close Bank"),
        MAKE_UNFINISHED_POTION("Make Unfinished Potions"),
        MAKE_FINISHED_POTION("Make Finished Potions");

        private String name;

        MakePotionState(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
