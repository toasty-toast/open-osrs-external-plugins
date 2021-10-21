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
}