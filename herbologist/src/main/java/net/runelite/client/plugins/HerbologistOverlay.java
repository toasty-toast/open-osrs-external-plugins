package net.runelite.client.plugins;

import com.openosrs.client.ui.overlay.components.table.TableAlignment;
import com.openosrs.client.ui.overlay.components.table.TableComponent;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class HerbologistOverlay extends OverlayPanel {
    private HerbologistPlugin plugin;

    @Inject
    public HerbologistOverlay(final HerbologistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212"));
        panelComponent.setPreferredSize(new Dimension(250, 200));
        panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Herbologist")
                .color(ColorUtil.fromHex("#40C4FF"))
                .build());

        TableComponent statusTable = new TableComponent();
        statusTable.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        statusTable.addRow("Activity: ", plugin.getActivityName());
        statusTable.addRow("State: ", plugin.getActivityState());

        long secondsRunning = Duration.between(plugin.getPluginStartTime(), Instant.now()).getSeconds();
        String timeRunningString = "";
        if (secondsRunning > 3600) {
            timeRunningString = String.format("%d:%02d:%02d", secondsRunning / 3600, (secondsRunning % 3600) / 60, (secondsRunning % 60));
        } else {
            timeRunningString = String.format("%02d:%02d", secondsRunning / 60, (secondsRunning % 60));
        }
        statusTable.addRow("Time Running: ", timeRunningString);

        panelComponent.getChildren().add(statusTable);

        return super.render(graphics);
    }
}
