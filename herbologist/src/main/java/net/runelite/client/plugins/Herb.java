package net.runelite.client.plugins;

import lombok.Getter;

public class Herb {
    public Herb(int grimyId, int cleanId, int levelRequirement) {
        this.grimyId = grimyId;
        this.cleanId = cleanId;
        this.levelRequirement = levelRequirement;
    }

    @Getter
    int grimyId;

    @Getter
    int cleanId;

    @Getter
    int levelRequirement;
}
