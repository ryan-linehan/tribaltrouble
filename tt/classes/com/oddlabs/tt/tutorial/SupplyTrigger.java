package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;

import java.util.Iterator;
import java.util.Set;

public final strictfp class SupplyTrigger extends TutorialTrigger {
    private static final int TREE = 20;
    private static final int ROCK = 10;

    public SupplyTrigger(Player player) {
        super(.5f, 0f, "supply", new Object[] {new Integer(TREE), new Integer(ROCK)});
        player.enableHarvesting(true);
    }

    protected final void run(Tutorial tutorial) {
        Set set = tutorial.getViewer().getSelection().getCurrentSelection().getSet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Selectable s = (Selectable) it.next();
            if (s instanceof Building && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                Building armory = (Building) s;
                if (armory.getSupplyContainer(com.oddlabs.tt.model.RockSupply.class)
                                        .getNumSupplies()
                                >= ROCK
                        && armory.getSupplyContainer(com.oddlabs.tt.landscape.TreeSupply.class)
                                        .getNumSupplies()
                                >= TREE)
                    tutorial.next(new BuildMenuTrigger(tutorial.getViewer().getLocalPlayer()));
            }
        }
    }
}
