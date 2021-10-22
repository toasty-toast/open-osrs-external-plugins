package net.runelite.client.plugins;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class GroupInformation {
    @Getter
    @Setter
    public String name;

    @Getter
    @Setter
    List<String> members;
}
