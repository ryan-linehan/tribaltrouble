package com.oddlabs.tt.delegate;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;

public enum SelectionFilter {
    ALL("All"),
    PEON("Peons"),
    WARRIORS_AND_CHIEF("Warriors + Chief"),
    WARRIOR_ROCK("Rock Warriors"),
    WARRIOR_IRON("Iron Warriors"),
    WARRIOR_RUBBER("Chicken Warriors"),
    CHIEF("Chief");

    private static final SelectionFilter[] VALUES = values();

    private final String displayName;

    SelectionFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SelectionFilter next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public boolean matches(Selectable selectable, Player localPlayer) {
        if (this == ALL) {
            return true;
        }
        if (!(selectable instanceof Unit)) {
            return false;
        }
        Unit unit = (Unit) selectable;
        Abilities abilities = unit.getAbilities();
        Race race = localPlayer.getRace();

        switch (this) {
            case PEON:
                return abilities.hasAbilities(Abilities.BUILD);
            case WARRIORS_AND_CHIEF:
                return !abilities.hasAbilities(Abilities.BUILD);
            case WARRIOR_ROCK:
                return unit.getUnitTemplate() == race.getUnitTemplate(Race.UNIT_WARRIOR_ROCK);
            case WARRIOR_IRON:
                return unit.getUnitTemplate() == race.getUnitTemplate(Race.UNIT_WARRIOR_IRON);
            case WARRIOR_RUBBER:
                return unit.getUnitTemplate() == race.getUnitTemplate(Race.UNIT_WARRIOR_RUBBER);
            case CHIEF:
                return abilities.hasAbilities(Abilities.MAGIC);
            default:
                return true;
        }
    }
}
