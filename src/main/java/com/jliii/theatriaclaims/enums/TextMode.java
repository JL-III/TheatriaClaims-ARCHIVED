/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jliii.theatriaclaims.enums;

import org.bukkit.ChatColor;

//just a few constants for chat color codes
public enum TextMode {
    Info(ChatColor.AQUA),
    Instr(ChatColor.YELLOW),
    Warn(ChatColor.GOLD),
    Err(ChatColor.RED),
    Success(ChatColor.GREEN);

    private final ChatColor color;

    TextMode(ChatColor color) {
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }
}
