package net.runelite.client.plugins;

import net.runelite.client.config.*;

@ConfigGroup("Herbologist")
public interface HerbologistConfig extends Config {
    @ConfigSection(
            keyName = "delayConfig",
            name = "Sleep Delay Configuration",
            description = "Configure how the plugin handles sleep delays",
            position = 1,
            closedByDefault = true
    )
    String delayConfig = "delayConfig";

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepMin",
            name = "Sleep Min",
            description = "",
            section = "delayConfig"
    )
    default int sleepMin() {
        return 150;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepMax",
            name = "Sleep Max",
            description = "",
            section = "delayConfig"
    )
    default int sleepMax() {
        return 500;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepTarget",
            name = "Sleep Target",
            description = "",
            section = "delayConfig"
    )
    default int sleepTarget() {
        return 200;
    }

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepDeviation",
            name = "Sleep Deviation",
            description = "",
            section = "delayConfig"
    )
    default int sleepDeviation() {
        return 25;
    }

    @ConfigItem(
            keyName = "sleepWeightedDistribution",
            name = "Sleep Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            section = "delayConfig"
    )
    default boolean sleepWeightedDistribution() {
        return false;
    }

    @ConfigSection(
            keyName = "delayTickConfig",
            name = "Game Tick Configuration",
            description = "Configure how the bot handles game tick delays, 1 game tick equates to roughly 600ms",
            position = 2,
            closedByDefault = true
    )
    String delayTickConfig = "delayTickConfig";

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayMin",
            name = "Game Tick Min",
            description = "",
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
            section = "delayTickConfig"
    )
    default int tickDelayDeviation() {
        return 1;
    }

    @ConfigItem(
            keyName = "tickDelayWeightedDistribution",
            name = "Game Tick Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            section = "delayTickConfig"
    )
    default boolean tickDelayWeightedDistribution() {
        return false;
    }

    @ConfigSection(
            keyName = "activityConfig",
            name = "Activity Configuration",
            description = "Configure the activity for the bot to perform",
            position = 3
    )
    String activityConfig = "activityConfig";

    @ConfigItem(
            keyName = "activity",
            name = "Activity",
            description = "The activity for the bot to perform",
            section = "activityConfig",
            position = 1
    )
    default HerbologistActivity activity() {
        return HerbologistActivity.CLEAN_HERBS;
    }

    @ConfigItem(
            keyName = "potion",
            name = "Potion",
            description = "The potion the bot should make",
            section = "activityConfig",
            hidden = true,
            unhide = "activity",
            unhideValue = "MAKE_POTIONS",
            position = 2
    )
    default HerbologistPotion potion() {
        return HerbologistPotion.SUPER_ENERGY;
    }

    @ConfigItem(
            keyName = "makeUnfinishedPotions",
            name = "Make Unfinished Potions",
            description = "Have the bot make the unfinished potions, then the finished ones",
            section = "activityConfig",
            hidden = true,
            unhide = "activity",
            unhideValue = "MAKE_POTIONS",
            position = 3
    )
    default boolean makeUnfinishedPotions() {
        return true;
    }

    @ConfigItem(
            keyName = "startButton",
            name = "Start/Stop",
            description = "Starts or stops the plugin",
            position = 4
    )
    default Button startButton() {
        return new Button();
    }
}
