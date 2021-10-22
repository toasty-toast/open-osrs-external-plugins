package net.runelite.client.plugins;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Singleton
public class GroupManager {
    private static final File GROUP_IRON_PANEL_DIR = new File(RUNELITE_DIR, "group-iron-panel");
    private static final File GROUP_INFO_FILE = new File(GROUP_IRON_PANEL_DIR, "group-info.txt");

    private GroupInformation groupInformation;

    public synchronized GroupInformation getGroupInformation() {
        if (groupInformation == null) {
            loadGroupInformation();
        }
        if (groupInformation == null) {
            groupInformation = new GroupInformation();
            groupInformation.setName("");
            groupInformation.setMembers(new ArrayList<>());
        }
        return groupInformation;
    }

    public synchronized boolean setGroupInformation(GroupInformation groupInformation) {
        if (groupInformation == null || groupInformation.getName() == null || groupInformation.getMembers() == null) {
            return false;
        }
        GroupInformation currentGroupInformation = getGroupInformation();
        if (groupInformation.getName().equals(currentGroupInformation.getName()) && areGroupMembersSame(currentGroupInformation.getMembers(), groupInformation.getMembers())) {
            return false;
        }

        GroupInformation newGroupInformation = new GroupInformation();
        newGroupInformation.setName(groupInformation.getName());
        newGroupInformation.setMembers(new ArrayList<>(groupInformation.getMembers()));
        this.groupInformation = newGroupInformation;

        saveGroupInformation();

        return true;
    }

    private boolean areGroupMembersSame(List<String> first, List<String> second) {
        if (first == null && second != null) {
            return false;
        }
        if (second == null && first != null) {
            return false;
        }
        if (first.size() != second.size()) {
            return false;
        }

        List<String> firstCopy = new ArrayList<>(first);
        List<String> secondCopy = new ArrayList<>(second);
        Collections.sort(firstCopy);
        Collections.sort(secondCopy);
        for (int i = 0; i < firstCopy.size(); i++) {
            if (!Objects.equals(firstCopy.get(i), secondCopy.get(i))) {
                return false;
            }
        }

        return true;
    }

    private synchronized void saveGroupInformation() {
        log.info("Writing file");
        try {
            GROUP_IRON_PANEL_DIR.mkdir();

            final BufferedWriter file = new BufferedWriter(new FileWriter(String.valueOf(GROUP_INFO_FILE), false));
            file.append(groupInformation.getName());
            file.newLine();
            for (final String member : this.groupInformation.getMembers()) {
                file.append(member);
                file.newLine();
            }
            file.close();
        } catch (IOException ex) {
            log.warn("Error writing group info to {}: {}", GROUP_INFO_FILE, ex.getMessage());
        }
    }

    private synchronized void loadGroupInformation() {
        try {
            List<String> lines = Files.readAllLines(Path.of(String.valueOf(GROUP_INFO_FILE)));
            if (lines == null || lines.size() < 1) {
                return;
            }
            GroupInformation groupInformation = new GroupInformation();
            groupInformation.setName(lines.get(0));
            groupInformation.setMembers(lines.stream().skip(1).collect(Collectors.toList()));
            this.groupInformation = groupInformation;
        } catch (IOException ex) {
            log.warn("Error reading group info from {}: {}", GROUP_INFO_FILE, ex.getMessage());
        }
    }
}
