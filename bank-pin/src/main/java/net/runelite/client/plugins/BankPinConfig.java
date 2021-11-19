package net.runelite.client.plugins;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BankPinConfig")
public interface BankPinConfig extends Config {
    @ConfigItem(
            keyName = "pin",
            name = "PIN",
            description = "Your bank PIN",
            position = 0,
            secret = true
    )
    default String pin() {
        return "";
    }

    @ConfigItem(
            keyName = "highlightNextPin",
            name = "Highlight Next Digit",
            description = "Highlights the next digit of your PIN you need to click",
            position = 0,
            secret = true
    )
    default boolean highlightNextPin() { return true; }

    @ConfigItem(
            keyName = "autoEnterPin",
            name = "Auto Enter PIN",
            description = "Automatically enters your PIN for you",
            position = 0,
            secret = true
    )
    default boolean autoEnterPin() { return false; }
}