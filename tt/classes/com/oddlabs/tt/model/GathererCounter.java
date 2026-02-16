package com.oddlabs.tt.model;

/**
 * Tracks the number of peons actively gathering a specific resource type for a building.
 * This counter is local to the client and does not need network synchronization.
 */
public strictfp class GathererCounter {
    private final Building building;
    private final Class supply_type;
    private int active_gatherers = 0;

    public GathererCounter(Building building, Class supply_type) {
        this.building = building;
        this.supply_type = supply_type;
    }

    /**
     * Increment the count of active gatherers.
     * Called when a peon is deployed to gather this resource type.
     */
    public void incrementGatherers() {
        active_gatherers++;
    }

    /**
     * Decrement the count of active gatherers.
     * Called when a peon stops gathering (dies, interrupted, returns, etc).
     */
    public void decrementGatherers() {
        if (active_gatherers > 0) {
            active_gatherers--;
        }
    }

    /**
     * Get the current number of active gatherers.
     */
    public int getActiveGatherers() {
        if (!building.isDead()) {
            return active_gatherers;
        } else {
            return 0;
        }
    }

    /**
     * Reset the counter to zero.
     * Useful when building is destroyed or for cleanup.
     */
    public void reset() {
        active_gatherers = 0;
    }

    public Building getBuilding() {
        return building;
    }

    public Class getSupplyType() {
        return supply_type;
    }
}
