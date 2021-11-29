package net.runelite.client.plugins;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import org.pf4j.Extension;


@Extension
@PluginDescriptor(
        name = "Karambwan Fisher",
        description = ""
)
@Slf4j
public class KarambwanFisherPlugin extends Plugin {
    @Inject
    private Client client;

    @Subscribe
    private void onGameTick(GameTick gameTick) {

    }
}