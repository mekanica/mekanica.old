package mekanism.common.util;

import java.util.ArrayList;
import mekanism.api.Coord4D;
import mekanism.common.config.MekanismConfig.general;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyEmitter {

    private TileEntity parent;
    private ArrayList<EmitTarget> targets = new ArrayList<>();

    public EnergyEmitter(TileEntity parent) {
        this.parent = parent;
    }

    // Update connectivity to all neighbors
    public void refresh() {
        Coord4D coord = Coord4D.get(parent);
        IBlockAccess world = parent.getWorld();
        targets.clear();

        // Walk around each of our sides, checking to see if this block outputs energy
        for (EnumFacing side: EnumFacing.values()) {
            IEnergyStorage source = parent.getCapability(CapabilityEnergy.ENERGY, side);
            if (source != null && source.canExtract()) {
                // Construct an emit target for the side in question, if the appropriate capability exists
                TileEntity targetTile = coord.offset(side).getTileEntity(world);
                if (targetTile != null && targetTile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
                    targets.add(new EmitTarget(source, targetTile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite())));
                }
            }
        }
    }

    // Preconditions:
    // * Caller must have called refresh if any neighbors have changed
    // * Caller must not be calling this if not operational
    public void emit(int energy) {
        if (energy < 1 || targets.size() == 0) {
            return;
        }

        int availEnergy = energy;
        for (EmitTarget target: targets) {
            // TODO: Revisit for fairness of distribution
            availEnergy -= target.emit(availEnergy);
            if (availEnergy < 1) {
                break;
            }
        }
    }

    private static class EmitTarget {
        IEnergyStorage source;
        IEnergyStorage target;

        EmitTarget(IEnergyStorage source, IEnergyStorage target) {
            this.source = source;
            this.target = target;
        }

        double emit(double energy) {
            int output = (int)Math.round(Math.min(Integer.MAX_VALUE, energy * general.TO_RF));
            int energyConsumed = (int)(this.target.receiveEnergy(output, false) * general.FROM_RF);
            return source.extractEnergy(energyConsumed, false);
        }
    }
}
