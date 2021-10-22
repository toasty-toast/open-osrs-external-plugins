package net.runelite.client.plugins;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.hiscore.HiscoreClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GroupIronPanel extends PluginPanel {
    private static final int GROUP_IRON_MAX_GROUP_SIZE = 5;

    private GroupManager groupManager;

    private HiscoreClient hiscoreClient;

    private JLabel groupNameLabel;

    private List<PlayerStatsPanel> playerStatsPanels;

    public GroupIronPanel(GroupManager groupManager, HiscoreClient hiscoreClient) {
        this.groupManager = groupManager;
        this.hiscoreClient = hiscoreClient;

        rebuild();
        refreshPanel();
    }

    public void rebuild() {
        removeAll();

        JPanel groupNamePanel = new JPanel();
        groupNamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        groupNamePanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        groupNameLabel = new JLabel();
        groupNameLabel.setFont(new Font(groupNameLabel.getFont().getName(), Font.BOLD, 18));
        groupNamePanel.add(groupNameLabel);

        final BufferedImage refreshIcon = ImageUtil.loadImageResource(getClass(), "/Refresh.png");
        JLabel refreshButton = new JLabel();
        refreshButton.setIcon(new ImageIcon(refreshIcon));
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                refreshPanel();
            }
        });
        groupNamePanel.add(refreshButton);

        add(groupNamePanel);

        playerStatsPanels = new ArrayList<>();
        for (int i = 0; i < GROUP_IRON_MAX_GROUP_SIZE; i++) {
            PlayerStatsPanel statsPanel = new PlayerStatsPanel(hiscoreClient);
            statsPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
            playerStatsPanels.add(statsPanel);
            add(statsPanel);
        }
    }

    public void refreshPanel() {
        GroupInformation groupInformation = groupManager.getGroupInformation();
        groupNameLabel.setText(groupInformation.getName());

        List<String> groupMembers = groupInformation.getMembers();
        for (int i = 0; i < groupMembers.size() && i < GROUP_IRON_MAX_GROUP_SIZE; i++) {
            log.info(groupMembers.get(i));
            playerStatsPanels.get(i).setPlayer(groupMembers.get(i));
            playerStatsPanels.get(i).refreshStats();
        }
    }
}
