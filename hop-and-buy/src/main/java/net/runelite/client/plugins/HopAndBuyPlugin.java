package net.runelite.client.plugins;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.iutils.*;
import org.pf4j.Extension;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
        name = "Hop & Buy",
        description = ""
)
@Slf4j
public class HopAndBuyPlugin extends Plugin {
    private final int COINS_ITEM_ID = 995;
    private final int SHOP_ITEMS_CONTAINER_WIDGET_ID = 16;

    private final Pattern SHOP_ITEM_NAME_PATTERN = Pattern.compile("<col=.*>(.*)</col>");

    private boolean isPluginRunning;

    private long ticksToWait = 0;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private iUtils iUtils;

    @Inject
    private BankUtils bankUtils;

    @Inject
    private MenuUtils menuUtils;

    @Inject
    private ObjectUtils objectUtils;

    @Inject
    private MouseUtils mouseUtils;

    @Inject
    private InventoryUtils inventoryUtils;

    @Inject
    private NPCUtils npcUtils;

    @Inject
    private CalculationUtils calcUtils;

    @Inject
    private HopAndBuyConfig config;

    @Provides
    HopAndBuyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HopAndBuyConfig.class);
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("HopAndBuy")) {
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

        PluginState state = getState();
        log.info("State: " + state + ", Tick delay: " + ticksToWait);

        if (ticksToWait != 0) {
            ticksToWait--;
            return;
        }

        switch (state) {
            case MOVING:
                return;
            case GO_TO_BANK:
                handleGoToBankState();
                break;
            case DEPOSIT_ITEMS:
                handleDepositItemsState();
                break;
            case CLOSE_BANK:
                handleCloseBankState();
                break;
            case OPEN_SHOP:
                handleOpenShopState();
                break;
            case BUY_ITEMS:
                handleBuyItemsState();
                break;
        }

        ticksToWait = calculateTickDelay();
    }

    private PluginState getState() {
        if (client.getLocalPlayer().isMoving()) {
            return PluginState.MOVING;
        }

        if (isShopOpen()) {
            if (inventoryUtils.isFull()) {
                return PluginState.GO_TO_BANK;
            } else if (getShopItemWidgetsToBuy().size() == 0) {
                return PluginState.CLOSE_SHOP_AND_HOP;
            } else {
                return PluginState.BUY_ITEMS;
            }
        }

        if (inventoryUtils.isFull()) {
            if (!bankUtils.isOpen()) {
                return PluginState.GO_TO_BANK;
            }
            if (inventoryUtils.containsExcept(Arrays.asList(COINS_ITEM_ID))) {
                return PluginState.DEPOSIT_ITEMS;
            }
            return PluginState.CLOSE_BANK;
        }

        if (!isShopOpen()) {
            return PluginState.OPEN_SHOP;
        }

        return PluginState.BUY_ITEMS;
    }

    private void handleGoToBankState() {
        GameObject bankObject = objectUtils.findNearestBank();
        if (bankObject == null) {
            log.error("No bank found");
            isPluginRunning = false;
            return;
        }

        MenuEntry bankMenuEntry = new MenuEntry("Bank", bankObject.getName(), bankObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), bankObject.getSceneMinLocation().getX(), bankObject.getSceneMinLocation().getY(), false);
        iUtils.doActionMsTime(bankMenuEntry, bankObject.getConvexHull().getBounds(), calculateSleepDelay());
    }

    private void handleDepositItemsState() {
        bankUtils.depositAllExcept(Arrays.asList(COINS_ITEM_ID));
    }

    private void handleCloseBankState() {
        bankUtils.close();
    }

    private void handleOpenShopState() {
        NPC shopNpc = npcUtils.findNearestNpc(config.shopNpcId());
        if (shopNpc == null) {
            log.error("No shop NPC found");
            isPluginRunning = false;
            return;
        }

        MenuEntry tradeMenuEntry = new MenuEntry("", "", shopNpc.getIndex(), MenuAction.NPC_THIRD_OPTION.getId(), 0, 0, false);
        iUtils.doActionMsTime(tradeMenuEntry, shopNpc.getConvexHull().getBounds(), calculateSleepDelay());
    }

    private void handleBuyItemsState() {
        List<Widget> shopItemWidgetsToBuy = getShopItemWidgetsToBuy();

        for (Widget widget : shopItemWidgetsToBuy) {
            MenuEntry buyMenuEntry = new MenuEntry("Buy 50", widget.getName(), 5, MenuAction.CC_OP.getId(), widget.getIndex(), widget.getId(), false);
            iUtils.doActionMsTime(buyMenuEntry, widget.getBounds(), calculateSleepDelay());
        }
    }

    private String getShopItemName(Widget shopItemWidget) {
        if (shopItemWidget == null) {
            return null;
        }

        Matcher matcher = SHOP_ITEM_NAME_PATTERN.matcher(shopItemWidget.getName());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<Widget> getShopItemWidgetsToBuy() {
        List<Widget> widgets = new ArrayList<>();

        Widget shopItemsContainerWidget = client.getWidget(WidgetID.SHOP_GROUP_ID, SHOP_ITEMS_CONTAINER_WIDGET_ID);
        if (shopItemsContainerWidget == null) {
            return widgets;
        }

        Widget[] shopItemWidgets = shopItemsContainerWidget.getChildren();
        if (shopItemWidgets == null) {
            return widgets;
        }

        Set<String> itemsToBuy = Arrays.stream(config.itemNames().split(","))
                .map(s -> s.trim())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (Widget shopItemWidget : shopItemWidgets) {
            String itemName = getShopItemName(shopItemWidget);
            if (itemName == null) {
                continue;
            }
            if (shopItemWidget.getItemQuantity() <= 0) {
                continue;
            }

            if (itemsToBuy.contains(itemName.toLowerCase(Locale.ROOT))) {
                widgets.add(shopItemWidget);
            }
        }

        return widgets;
    }

    private boolean isShopOpen() {
        return client.getWidget(WidgetID.SHOP_GROUP_ID, SHOP_ITEMS_CONTAINER_WIDGET_ID) != null;
    }

    private long calculateTickDelay() {
        return calcUtils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
    }

    private long calculateSleepDelay() {
        return calcUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
    }
}
