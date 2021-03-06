package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;

import static com.github.manolo8.darkbot.Main.API;

public class StatsProxy extends Updatable {
    private int fps, memory;

    @Override
    public void update() {
        this.fps = API.readMemoryInt(address, 80, 32);
        this.memory = (int) API.readMemoryDouble(address, 80, 56);
    }

    public int getFps() {
        return fps;
    }

    public int getMemory() {
        return memory;
    }
}