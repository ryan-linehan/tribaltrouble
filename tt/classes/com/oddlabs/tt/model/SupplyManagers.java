package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public strictfp class SupplyManagers {
    private final Map supply_managers = new HashMap();

    public final void debugSpawn() {
        Iterator it = supply_managers.keySet().iterator();
        while (it.hasNext()) {
            SupplyManager manager = (SupplyManager) supply_managers.get(it.next());
            manager.debugSpawnSupply();
        }
    }

    public SupplyManagers(World world) {
        supply_managers.put(TreeSupply.class, new SupplyManager(world));
        supply_managers.put(RockSupply.class, new SupplyManager(world));
        supply_managers.put(IronSupply.class, new SupplyManager(world));
        supply_managers.put(RubberSupply.class, new RubberSupplyManager(world));
    }

    public final SupplyManager getSupplyManager(Class type) {
        return (SupplyManager) supply_managers.get(type);
    }
}
