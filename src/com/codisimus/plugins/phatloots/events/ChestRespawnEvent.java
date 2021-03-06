package com.codisimus.plugins.phatloots.events;

import com.codisimus.plugins.phatloots.PhatLootChest;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a PhatLootChest respawns
 *
 * @author Cody
 */
public class ChestRespawnEvent extends Event implements Cancellable {
    public static enum RespawnReason { INITIAL, DELAYED, PLUGIN_DISABLED, OTHER }
    private static final HandlerList handlers = new HandlerList();
    private PhatLootChest chest;
    private long delay;
    private RespawnReason reason;
    private boolean cancelled;

    /**
     * Creates a new event with the given data
     *
     * @param chest The PhatLootChest that respawned
     * @param delay The amount of time to delay the respawn process
     */
    public ChestRespawnEvent(PhatLootChest chest, long delay, RespawnReason reason) {
        this.chest = chest;
        this.delay = delay;
        this.reason = reason;
    }

    /**
     * Returns the chest that respawned
     *
     * @return The PhatLootChest that has respawned
     */
    public PhatLootChest getChest() {
        return chest;
    }

    /**
     * Returns the amount of time (in ticks) until the chest respawns
     *
     * @return The amount of time to delay the respawn process
     */
    public long getRespawnTime() {
        return delay;
    }

    /**
     * Sets how long to delay the chest respawning
     *
     * @param time The new time in ticks
     */
    public void setRespawnTime(long time) {
        delay = time;
    }

    /**
     * Returns the reason that the chest has respawned
     *
     * @return The reason for the respawning of the PhatLootChest
     */
    public RespawnReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean arg) {
        cancelled = arg;
    }
}
