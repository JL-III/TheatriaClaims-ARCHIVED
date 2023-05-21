package com.jliii.theatriaclaims.enums;

import net.kyori.adventure.text.format.NamedTextColor;

//just a few constants for chat color codes
public enum TextMode {
    Info(NamedTextColor.AQUA),
    Instr(NamedTextColor.YELLOW),
    Warn(NamedTextColor.GOLD),
    Err(NamedTextColor.RED),
    Success(NamedTextColor.GREEN);

    private final NamedTextColor color;

    TextMode(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor getColor() {
        return color;
    }
}
