package net.runelite.client.plugins;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import org.pf4j.Extension;


@Extension
@PluginDescriptor(
        name = "Always Make All",
        description = "Automatically selects \"Make All\" in multi-skill menus that have only one option"
)
@Slf4j
public class AlwaysMakeAllPlugin extends Plugin {
    private static final int MULTI_SKILL_MENU_MAKE_COUNT_OPTIONS_CONTAINER = 6;
    private static final int MULTI_SKILL_MENU_OPTIONS_CONTAINER_ID = 13;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        Widget makeAllWidget = getMakeAllWidget();
        if (makeAllWidget == null || makeAllWidget.isHidden()) {
            return;
        }

        // If the target widget has actions available it means it is not selected.
        // Select it now and on the next game tick we'll take care of actually starting to make the items.
        String[] makeAllActions = makeAllWidget.getActions();
        if (makeAllActions != null && makeAllActions.length > 0) {
            clientThread.invoke(() -> {
                client.invokeMenuAction(makeAllActions[0],
                        "",
                        1,
                        MenuAction.CC_OP.getId(),
                        -1,
                        makeAllWidget.getId());
            });
            return;
        }

        Widget makeOption = getSingleMakeOption();
        if (makeOption == null) {
            return;
        }

        String[] actions = makeOption.getActions();
        if (actions == null || actions.length == 0) {
            return;
        }

        // Finally we invoke the menu option to start making the item.
        clientThread.invoke(() -> {
            client.invokeMenuAction(actions[0],
                    makeOption.getRSName(),
                    1,
                    MenuAction.CC_OP.getId(),
                    -1,
                    makeOption.getId());
        });
    }

    /**
     * Gets the "Make All" widget from the multi-skill selection menu.
     * If "All" is not an option, the widget for creating the largest amount of items possible is returned.
     * If no suitable widget is found in the game, null is returned.
     */
    private Widget getMakeAllWidget() {
        Widget container = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, MULTI_SKILL_MENU_MAKE_COUNT_OPTIONS_CONTAINER);
        if (container == null) {
            return null;
        }

        Widget[] children = container.getStaticChildren();
        if (children == null) {
            return null;
        }

        // Moving from left to right in the list of widgets, the last one we come to that has text in it
        // is the highest quantity we can make of an item. For some items this is "All", for others it is "10".
        // Either way we want to select the highest amount available.
        Widget target = null;
        for (Widget child : children) {
            String childText = getFirstTextInWidget(child);
            if (childText != null && !childText.equals("X")) {
                target = child;
            }
        }

        return target;
    }

    /**
     * Returns the first occurrence of text in a widget or any of its children.
     */
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

    /**
     * Finds the widget for a single item to create from the multi-skill menu.
     * If no widgets are found or if more than one widget is found, null is returned instead.
     */
    private Widget getSingleMakeOption() {
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
            // We want to return either a single visible item, or null if there are 0 or more than 1 visible items.
            if (makeOption.getChildren() != null) {
                optionCount++;
                singleMakeOption = makeOption;
            }
        }

        return optionCount == 1 ? singleMakeOption : null;
    }
}