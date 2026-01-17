package com.oddlabs.tt.gui;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployContainer;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Quad;

public final strictfp class DeploySpinner extends IconSpinner {
    private final PlayerInterface player_interface;
    private Class supply_type;
    private int deploy_type;
    private Building current_building;
    private int num_orders = 0;
    private int order_size = 0;
    private int recall_count = 0;

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
        this.recall_count = 0;
        if (!current_building.isDead())
            num_orders = current_building.getDeployContainer(deploy_type).getNumOrders();
    }

    private Class getHarvestSupplyType() {
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

    private boolean isHarvestType() {
        return getHarvestSupplyType() != null;
    }

    private int getActiveHarvesterCount() {
        Class harvestType = getHarvestSupplyType();
        if (harvestType != null && current_building != null && !current_building.isDead()) {
            return ((Player) current_building.getOwner()).getHarvesterCount(harvestType);
        }
        return 0;
    }

    public final int computeCount() {
        if (current_building != null && !current_building.isDead()) {
            DeployContainer deploy_container = current_building.getDeployContainer(deploy_type);
            int deployingCount =
                    StrictMath.min(
                            deploy_container.getMaxSupplyCount(),
                            StrictMath.max(0, deploy_container.getNumSupplies() + getOrderDiff()));

            if (isHarvestType()) {
                return StrictMath.max(0, getActiveHarvesterCount() + deployingCount - recall_count);
            }
            return deployingCount;
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
            if (isHarvestType()) {
                int activeCount = getActiveHarvesterCount();
                int deployingCount =
                        current_building.getDeployContainer(deploy_type).getNumSupplies()
                                + getOrderDiff();

                int cancelAmount = StrictMath.min(amount, StrictMath.max(0, deployingCount));
                if (cancelAmount > 0) {
                    order_size -= cancelAmount;
                    num_orders -= cancelAmount;
                    amount -= cancelAmount;
                }

                if (amount > 0) {
                    recall_count +=
                            StrictMath.min(amount, StrictMath.max(0, activeCount - recall_count));
                }
                return;
            }

            int num_units = current_building.getDeployContainer(deploy_type).getNumSupplies();
            if (num_units > -getOrderDiff()) {
                if (amount > num_units + getOrderDiff()) {
                    amount = num_units + getOrderDiff();
                }
                order_size -= amount;
                num_orders -= amount;
            }
        }
    }

    protected final void release() {
        if (isHarvestType() && recall_count > 0) {
            executeRecalls();
        }
        order(order_size);
        order_size = 0;
        recall_count = 0;
    }

    private void executeRecalls() {
        Class harvestType = getHarvestSupplyType();
        if (harvestType == null || current_building == null || current_building.isDead()) {
            return;
        }
        Player owner = (Player) current_building.getOwner();
        float buildingX = current_building.getPositionX();
        float buildingY = current_building.getPositionY();

        for (int i = 0; i < recall_count; i++) {
            Unit nearest = owner.findNearestHarvester(harvestType, buildingX, buildingY);
            if (nearest != null && !nearest.isDead()) {
                player_interface.withdrawHarvester(nearest, current_building);
            }
        }
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
