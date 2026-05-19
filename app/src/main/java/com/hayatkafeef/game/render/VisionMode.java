package com.hayatkafeef.game.render;

public enum VisionMode {
    TOTAL,        // black screen
    LOW,          // blocky big shapes
    BLUR,         // gaussian-ish blur
    CENTRAL,      // tunnel vision
    PERIPHERAL,   // ring vision
    SIGHTED;      // full

    public static VisionMode fromId(int id) {
        VisionMode[] v = values();
        if (id < 0 || id >= v.length) return SIGHTED;
        return v[id];
    }

    public int id() { return ordinal(); }
}
