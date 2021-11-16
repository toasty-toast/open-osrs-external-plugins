package net.runelite.client.plugins;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.iutils.*;
import org.pf4j.Extension;


@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Always Make All",
        description = "Automatically selects \"Make All\" in multi-skill menus that have only one option. This plugin requires the iUtils plugin from Illumine Plugins"
)
@Slf4j
public class AlwaysMakeAllPlugin extends Plugin {
    private static final int MULTI_SKILL_MENU_MAKE_ALL_ID = 12;
    private static final int MULTI_SKILL_MENU_OPTIONS_CONTAINER_ID = 13;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private iUtils iUtils;

    @Inject
    private iUtilsConfig iUtilsConfig;

    @Inject
    private CalculationUtils calcUtils;

    @Inject
    private MouseUtils mouseUtils;

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        Widget makeAllWidget = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, MULTI_SKILL_MENU_MAKE_ALL_ID);
        if (makeAllWidget == null || makeAllWidget.isHidden()) {
            return;
        }

        Widget makeOptionsContainer = client.getWidget(WidgetID.MULTISKILL_MENU_GROUP_ID, MULTI_SKILL_MENU_OPTIONS_CONTAINER_ID);
        if (makeOptionsContainer == null || makeAllWidget.isHidden()) {
            return;
        }

        Widget makeOption = getSingleMakeOption(makeOptionsContainer);
        if (makeOption == null) {
            return;
        }

        // If the "make all" button has action text it means it is not selected.
        // Click it now, and we'll click the actual "make" button on the next tick.
        if (makeAllWidget.getActions() != null) {
            mouseUtils.delayMouseClick(makeAllWidget.getBounds(), getSleepDelay());
            return;
        }

        // If the button for the item we want to make has action text it means we haven't clicked it yet.
        // We have to make this check because after we click it, it remains visible with the hour glass icon for a moment.
        if (makeOption.getActions() != null) {
            log.info("Clicking");
            log.info(String.join(", ", makeOption.getActions()));
            mouseUtils.delayMouseClick(makeOption.getBounds(), getSleepDelay());
            return;
        }
    }

    private Widget getSingleMakeOption(Widget makeOptionsContainer) {
        Widget[] makeOptions = makeOptionsContainer.getStaticChildren();
        if (makeOptions == null) {
            return null;
        }

        Widget singleMakeOption = null;
        int optionCount = 0;
        for (Widget makeOption : makeOptions) {
            if (makeOption.getChildren() != null) {
                optionCount++;
                singleMakeOption = makeOption;
            }
        }

        return optionCount == 1 ? singleMakeOption : null;
    }

    private long getSleepDelay() {
        return calcUtils.randomDelay(iUtilsConfig.sleepWeightedDistribution(), iUtilsConfig.sleepMin(), iUtilsConfig.sleepMax(), iUtilsConfig.sleepDeviation(), iUtilsConfig.sleepTarget());
    }
}