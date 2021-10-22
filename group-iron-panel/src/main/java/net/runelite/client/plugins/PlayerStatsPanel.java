package net.runelite.client.plugins;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.hiscore.HiscoreClient;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import net.runelite.http.api.hiscore.HiscoreSkill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.StrokeBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.http.api.hiscore.HiscoreSkill.*;

@Slf4j
public class PlayerStatsPanel extends JPanel {
    private static final List<HiscoreSkill> SKILLS = ImmutableList.of(
            ATTACK, HITPOINTS, MINING,
            STRENGTH, AGILITY, SMITHING,
            DEFENCE, HERBLORE, FISHING,
            RANGED, THIEVING, COOKING,
            PRAYER, CRAFTING, FIREMAKING,
            MAGIC, FLETCHING, WOODCUTTING,
            RUNECRAFT, SLAYER, FARMING,
            CONSTRUCTION, HUNTER
    );

    private Map<HiscoreSkill, JLabel> skillToLabel = new HashMap<>();

    private HiscoreClient hiscoreClient;

    private String username;

    private JLabel usernameLabel;

    public PlayerStatsPanel(HiscoreClient hiscoreClient) {
        this.hiscoreClient = hiscoreClient;
        buildPanel();
    }

    public void refreshStats() {
        usernameLabel.setText(username);

        for (Map.Entry<HiscoreSkill, JLabel> entry : skillToLabel.entrySet()) {
            JLabel label = entry.getValue();
            if (label != null) {
                label.setText("-");
            }
        }

        hiscoreClient.lookupAsync(username, HiscoreEndpoint.NORMAL).whenCompleteAsync((result, ex) ->
                SwingUtilities.invokeLater(() ->
                {
                    if (result == null || ex != null) {
                        if (ex != null) {
                            log.warn("Error fetching hiscore data for " + username + " " + ex.getMessage());
                        }
                        return;
                    }

                    for (Map.Entry<HiscoreSkill, JLabel> entry : skillToLabel.entrySet()) {
                        HiscoreSkill skill = entry.getKey();
                        JLabel label = entry.getValue();

                        if (label == null) {
                            continue;
                        }

                        if (skill == null) {
                            int combatLevel = Experience.getCombatLevel(
                                    result.getAttack().getLevel(),
                                    result.getStrength().getLevel(),
                                    result.getDefence().getLevel(),
                                    result.getHitpoints().getLevel(),
                                    result.getMagic().getLevel(),
                                    result.getRanged().getLevel(),
                                    result.getPrayer().getLevel()
                            );
                            label.setText(Integer.toString(combatLevel));
                        } else {
                            label.setText(String.valueOf(result.getSkill(skill).getLevel()));
                        }
                    }
                }));
    }

    public void setPlayer(String username) {
        this.username = username;
        if (username == null || username.isEmpty()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
    }

    private void buildPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 5, 0);

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel usernamePanel = new JPanel();
        usernamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        usernameLabel = new JLabel();
        usernamePanel.add(usernameLabel);
        add(usernamePanel, constraints);
        constraints.gridy++;

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(8, 3));
        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        for (HiscoreSkill skill : SKILLS) {
            statsPanel.add(buildSkillPanel(skill));
        }
        add(statsPanel, constraints);
        constraints.gridy++;

        JPanel summaryStatsPanel = new JPanel();
        summaryStatsPanel.setLayout(new GridLayout(1, 2));
        summaryStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        summaryStatsPanel.add(buildSkillPanel(null));
        summaryStatsPanel.add(buildSkillPanel(OVERALL));
        add(summaryStatsPanel, constraints);
        constraints.gridy++;

        setVisible(false);
    }

    private JPanel buildSkillPanel(HiscoreSkill skill) {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        String skillName = (skill == null ? "combat" : skill.name().toLowerCase());

        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(ImageUtil.loadImageResource(RuneLite.class, "/skill_icons_small/" + skillName + ".png")));
        label.setText("-");
        skillToLabel.put(skill, label);
        panel.add(label);

        return panel;
    }
}
