package net.runelite.client.plugins;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.hiscore.HiscoreClient;
import okhttp3.OkHttpClient;
import org.pf4j.Extension;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Extension
@PluginDescriptor(
        name = "Group Iron Panel",
        description = "Adds a panel showing your group members' stats"
)
@Slf4j
public class GroupIronPanelPlugin extends Plugin {
    private static final Pattern GIM_USERNAME_PATTERN = Pattern.compile("^<.*>(.*)<.*>$");

    private GroupIronPanel panel;

    private NavigationButton toolbarButton;

    private boolean done = false;

    private HiscoreClient hiscoreClient;

    @Inject
    private Client client;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private GroupManager groupManager;

    @Override
    protected void startUp() {
        hiscoreClient = new HiscoreClient(okHttpClient);

        panel = new GroupIronPanel(groupManager, hiscoreClient);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/GroupIron.png");
        toolbarButton = NavigationButton.builder()
                .tooltip("Group Iron")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(toolbarButton);
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(toolbarButton);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) throws IOException {
        tryReloadGroupInfo();
    }

    /**
     * Tries to reload the group name and members.
     * This update will only succeed if the group ironman tab is open.
     */
    private void tryReloadGroupInfo() {
        Widget groupNameContainerWidget = client.getWidget(WidgetID.GROUP_IRON_GROUP_ID, 1);
        if (groupNameContainerWidget == null) {
            return;
        }
        Widget[] groupNameWidgets = groupNameContainerWidget.getChildren();
        String groupName = null;
        if (groupNameWidgets != null) {
            for (Widget child : groupNameWidgets) {
                String widgetText = child.getText();
                if (widgetText != null && widgetText.length() > 0) {
                    groupName = widgetText;
                }
            }
        }

        Widget groupMembersContainerWidget = client.getWidget(WidgetID.GROUP_IRON_GROUP_ID, 6);
        if (groupMembersContainerWidget == null) {
            return;
        }
        Widget[] groupMemberWidgets = groupMembersContainerWidget.getChildren();
        List<String> groupMembers = new ArrayList<>();
        if (groupMemberWidgets != null) {
            for (Widget child : groupMemberWidgets) {
                String widgetName = child.getName();
                Matcher matcher = GIM_USERNAME_PATTERN.matcher(widgetName);
                if (matcher.find()) {
                    groupMembers.add(matcher.group(1));
                }
            }
        }

        GroupInformation groupInformation = new GroupInformation();
        groupInformation.setName(groupName);
        groupInformation.setMembers(groupMembers);
        if (groupManager.setGroupInformation(groupInformation)) {
            panel.refreshPanel();
        }
    }
}