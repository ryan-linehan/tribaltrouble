package com.oddlabs.tt.gui;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployContainer;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.TransferUnitController;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Quad;

import java.util.Iterator;
import java.util.Set;

public final strictfp class DeploySpinner extends IconSpinner {
    private final PlayerInterface player_interface;
    private Class supply_type;
    private int deploy_type;
    private Building current_building;
    private int num_orders = 0;
    private int order_size = 0;

    public DeploySpinner(
            WorldViewer viewer,
            PlayerInterface player_interface,
            IconQuad[] icon_quad,
            String tool_tip,
            Quad[] tool_tip_icons,
            String shortcut_key) {
        super(viewer, icon_quad, tool_tip, tool_tip_icons, shortcut_key);
        this.player_interface = player_interface;
    }

    public void setContainers(Building current_building, int deploy_type, Class supply_type) {
        this.current_building = current_building;
        this.deploy_type = deploy_type;
        this.supply_type = supply_type;
        if (!current_building.isDead())
            num_orders = current_building.getDeployContainer(deploy_type).getNumOrders();
    }

    public final int computeCount() {
        if (current_building != null && !current_building.isDead()) {
            DeployContainer deploy_container = current_building.getDeployContainer(deploy_type);
            return StrictMath.min(
                    deploy_container.getMaxSupplyCount(),
                    StrictMath.max(0, deploy_container.getNumSupplies() + getOrderDiff()));
        } else return 0;
    }

    public final boolean renderInfinite() {
        return false;
    }

    public final int getOrderDiff() {
        if (current_building != null && !current_building.isDead()) {
            return num_orders - current_building.getDeployContainer(deploy_type).getNumOrders();
        } else {
            return 0;
        }
    }

    private final void order(int num) {
        if (!current_building.isDead())
            player_interface.deployUnits(current_building, deploy_type, num);
    }

    protected final void increase(int amount) {
        if (!current_building.isDead()) {
            int num_units = current_building.getUnitContainer().getNumSupplies();
            int num_supplies = Integer.MAX_VALUE;
            if (supply_type != null) {
                num_supplies = current_building.getSupplyContainer(supply_type).getNumSupplies();
            }

            if (num_units > getOrderDiff() && num_supplies > getOrderDiff()) {
                if (amount > num_units - getOrderDiff()) {
                    amount = num_units - getOrderDiff();
                }
                if (supply_type != null && amount > num_supplies - getOrderDiff()) {
                    amount = num_supplies - getOrderDiff();
                }
                order_size += amount;
                num_orders += amount;
            }
        }
    }

    protected final void decrease(int amount) {
        if (!current_building.isDead() && computeCount() > 0) {
            // Check if this is a harvest deploy type - if so, try to recall active gatherers
            if (isHarvestDeployType(deploy_type)) {
                Class resource_type = getResourceTypeForDeployType(deploy_type);
                if (resource_type != null) {
                    // Try to recall active gatherers first
                    for (int i = 0; i < amount; i++) {
                        if (!recallNearestGatherer(resource_type)) {
                            break; // No more gatherers to recall
                        }
                    }
                }
            }

            int num_units = current_building.getDeployContainer(deploy_type).getNumSupplies();

            if (num_units > -getOrderDiff() /* && num_supplies > -getOrderDiff()*/) {
                if (amount > num_units + getOrderDiff()) {
                    amount = num_units + getOrderDiff();
                }
                /*
                if (supply_type != null && amount > num_supplies + getOrderDiff()) {
                	amount = num_supplies + getOrderDiff();
                }
                */
                order_size -= amount;
                num_orders -= amount;
            }
        }
    }

    private boolean isHarvestDeployType(int deploy_type) {
        return deploy_type == Building.KEY_DEPLOY_PEON_HARVEST_TREE
                || deploy_type == Building.KEY_DEPLOY_PEON_HARVEST_ROCK
                || deploy_type == Building.KEY_DEPLOY_PEON_HARVEST_IRON
                || deploy_type == Building.KEY_DEPLOY_PEON_HARVEST_RUBBER;
    }

    private Class getResourceTypeForDeployType(int deploy_type) {
        switch (deploy_type) {
            case Building.KEY_DEPLOY_PEON_HARVEST_TREE:
                return TreeSupply.class;
            case Building.KEY_DEPLOY_PEON_HARVEST_ROCK:
                return RockSupply.class;
            case Building.KEY_DEPLOY_PEON_HARVEST_IRON:
                return IronSupply.class;
            case Building.KEY_DEPLOY_PEON_HARVEST_RUBBER:
                return RubberSupply.class;
            default:
                return null;
        }
    }

    /**
     * Recalls the nearest active gatherer for the given resource type.
     * @return true if a gatherer was recalled, false if none were found
     */
    private boolean recallNearestGatherer(Class resource_type) {
        if (current_building.isDead()) {
            return false;
        }

        Set units = current_building.getOwner().getUnits().getSet();
        Unit nearest_gatherer = null;
        float nearest_distance_sq = Float.MAX_VALUE;

        float building_x = current_building.getPositionX();
        float building_y = current_building.getPositionY();

        // Find the nearest gatherer for this resource type
        Iterator it = units.iterator();
        while (it.hasNext()) {
            Selectable selectable = (Selectable) it.next();
            if (selectable instanceof Unit) {
                Unit unit = (Unit) selectable;
                // Check if this unit is gathering the resource type from this building
                if (!unit.isDead()
                        && unit.getHomeBuilding() == current_building
                        && unit.getCurrentController() instanceof GatherController) {
                    GatherController gc = (GatherController) unit.getCurrentController();
                    if (gc.getSupplyType() == resource_type) {
                        // Calculate distance to building
                        float dx = unit.getPositionX() - building_x;
                        float dy = unit.getPositionY() - building_y;
                        float dist_sq = dx * dx + dy * dy;

                        if (dist_sq < nearest_distance_sq) {
                            nearest_distance_sq = dist_sq;
                            nearest_gatherer = unit;
                        }
                    }
                }
            }
        }

        // If we found a gatherer, recall it
        if (nearest_gatherer != null) {
            // Swap the GatherController with TransferUnitController to send it back
            nearest_gatherer.swapController(new TransferUnitController(nearest_gatherer));
            return true;
        }

        return false;
    }

    protected final void release() {
        order(order_size);
        order_size = 0;
    }

    protected final int getOrderSize() {
        return order_size;
    }

    protected final float getProgress() {
        if (!current_building.isDead())
            return current_building.getDeployContainer(deploy_type).getBuildProgress();
        else return 0;
    }
}
