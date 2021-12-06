package net.runelite.client.plugins;

import net.runelite.client.config.*;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup("HopAndBuy")
public interface HopAndBuyConfig extends Config {
    @ConfigSection(
            keyName = "delayConfig",
            name = "Sleep Delay Configuration",
            description = "Configure how the plugin handles sleep delays",
            position = 2
    )
    String delayConfig = "delayConfig";

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepMin",
            name = "Sleep Min",
            description = "",
            position = 3,
            section = "delayConfig"
    )
    default int sleepMin() {
        return 60;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepMax",
            name = "Sleep Max",
            description = "",
            position = 4,
            section = "delayConfig"
    )
    default int sleepMax() {
        return 350;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepTarget",
            name = "Sleep Target",
            description = "",
            position = 5,
            section = "delayConfig"
    )
    default int sleepTarget() {
        return 100;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepDeviation",
            name = "Sleep Deviation",
            description = "",
            position = 6,
            section = "delayConfig"
    )
    default int sleepDeviation() {
        return 10;
    }

    @ConfigItem(
            keyName = "sleepWeightedDistribution",
            name = "Sleep Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            position = 7,
            section = "delayConfig"
    )
    default boolean sleepWeightedDistribution() {
        return false;
    }

    @ConfigSection(
            keyName = "delayTickConfig",
            name = "Game Tick Configuration",
            description = "Configure how the bot handles game tick delays, 1 game tick equates to roughly 600ms",
            position = 8
    )
    String delayTickConfig = "delayTickConfig";

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayMin",
            name = "Game Tick Min",
            description = "",
            position = 9,
            section = "delayTickConfig"
    )
    default int tickDelayMin() {
        return 1;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayMax",
            name = "Game Tick Max",
            description = "",
            position = 10,
            section = "delayTickConfig"
    )
    default int tickDelayMax() {
        return 3;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayTarget",
            name = "Game Tick Target",
            description = "",
            position = 11,
            section = "delayTickConfig"
    )
    default int tickDelayTarget() {
        return 2;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayDeviation",
            name = "Game Tick Deviation",
            description = "",
            position = 12,
            section = "delayTickConfig"
    )
    default int tickDelayDeviation() {
        return 1;
    }

    @ConfigItem(
            keyName = "tickDelayWeightedDistribution",
            name = "Game Tick Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            position = 13,
            section = "delayTickConfig"
    )
    default boolean tickDelayWeightedDistribution() {
        return false;
    }

    @ConfigItem(
            keyName = "shopNpcId",
            name = "Shop NPC ID",
            description = "The ID of the NPC who is used to open the shop",
            position = 14
    )
    default int shopNpcId() {
        return -1;
    }

    @ConfigItem(
            keyName = "itemNames",
            name = "Item Names",
            description = "The names of the items to buy from the shop (comma-separated)",
            position = 15
    )
    default String itemNames() {
        return "";
    }

    @ConfigItem(
            keyName = "startButton",
            name = "Start/Stop",
            description = "Starts or stops the plugin",
            position = 200
    )
    default Button startButton() {
        return new Button();
    }
}
