package mekanism.common.util;

import cofh.redstoneflux.api.IEnergyReceiver;
import java.util.ArrayList;
import mekanism.api.Coord4D;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.common.base.IEnergyWrapper;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.integration.ic2.IC2Integration;
import net.darkhax.tesla.api.ITeslaConsumer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyEmitter {

    private IEnergyWrapper parent;
    private ArrayList<EmitTarget> targets = new ArrayList<>();

    public EnergyEmitter(IEnergyWrapper parent) {
        this.parent = parent;
    }

    // Update connectivity to all neighbors
    public void refresh() {
        Coord4D coord = Coord4D.get((TileEntity)parent);
        IBlockAccess world = ((TileEntity)parent).getWorld();
        targets.clear();

        // Walk around each of our sides, checking to see if this block outputs energy
        for (EnumFacing side: EnumFacing.values()) {
            if (parent.sideIsOutput(side)) {
                // Construct an emit target for the side in question; if the resulting target
                // is valid, add it to our list emitting
                EmitTarget t = new EmitTarget(coord.offset(side).getTileEntity(world), side.getOpposite());
                if (t.isValid()) {
                    targets.add(t);
                }
            }
        }
    }

    // Preconditions:
    // * Caller must have called refresh if any neighbors have changed
    // * Caller must not be calling this if not operational
    public void emit() {
        double energy = Math.min(parent.getEnergy(), parent.getMaxOutput());
        if (energy < 1 || targets.size() == 0) {
            return;
        }

        double availEnergy = energy;
        for (EmitTarget target: targets) {
            // TODO: Revisit for fairness of distribution
            availEnergy -= target.emit(parent, availEnergy);
            if (availEnergy < 1) {
                break;
            }
        }

        parent.setEnergy(parent.getEnergy() + (availEnergy - energy));
    }

    private static class EmitTarget {
        private static final int MEKANISM = 1;
        private static final int TESLA= 2;
        private static final int FORGE = 3;
        private static final int RF = 4;
        private static final int IC2 = 5;

        int type = 0;
        Object target;
        EnumFacing side;

        EmitTarget(ICapabilityProvider target, EnumFacing side) {
            this.side = side;
            if (hasCapability(target, Capabilities.ENERGY_ACCEPTOR_CAPABILITY) ||
                  hasCapability(target, Capabilities.GRID_TRANSMITTER_CAPABILITY)) {
                this.target = getCapability(target, Capabilities.ENERGY_ACCEPTOR_CAPABILITY);
                this.type = MEKANISM;
            } else if (MekanismUtils.useTesla() && hasCapability(target, Capabilities.TESLA_CONSUMER_CAPABILITY)) {
                this.target = getCapability(target, Capabilities.TESLA_CONSUMER_CAPABILITY);
                this.type = TESLA;
            } else if (MekanismUtils.useForge() && hasCapability(target, CapabilityEnergy.ENERGY)) {
                this.target = getCapability(target, CapabilityEnergy.ENERGY);
                this.type = FORGE;
            } else if (MekanismUtils.useRF() && target instanceof IEnergyReceiver) {
                // TODO: Use capabilities here?
                this.target = target;
                this.type = RF;
            } else if (MekanismUtils.useIC2()) {
                this.target = target;
                this.type = IC2;
            }
        }

        boolean isValid() { return type != 0; }

        double emit(IEnergyWrapper sender, double energy) {
            long output;
            switch (type) {
                case MEKANISM:
                    IStrictEnergyAcceptor acceptor = (IStrictEnergyAcceptor)target;
                    if (acceptor.canReceiveEnergy(side)) {
                        return acceptor.acceptEnergy(side, energy, false);
                    }
                    return 0;
                case TESLA:
                    ITeslaConsumer tesla = (ITeslaConsumer)target;
                    output = Math.round(Math.round(energy * general.TO_TESLA));
                    return tesla.givePower(output, false) * general.FROM_TESLA;
                case FORGE:
                    IEnergyStorage forge = (IEnergyStorage)target;
                    output = Math.round(Math.min(Integer.MAX_VALUE, energy * general.TO_FORGE));
                    return forge.receiveEnergy((int)output, false) * general.FROM_FORGE;
                case RF:
                    // TODO: Check with KL as to whether we need the canConnectEnergy check here
                    IEnergyReceiver rf = (IEnergyReceiver)target;
                    output = Math.round(Math.min(Integer.MAX_VALUE, energy * general.TO_RF));
                    return rf.receiveEnergy(side, (int)output, false) * general.TO_RF;
                case IC2:
                    return IC2Integration.emitEnergy(sender, (TileEntity)target, side, energy);
                default:
                    return 0;
            }
        }

        boolean hasCapability(ICapabilityProvider provider, Capability<?> cap) {
            if (provider == null || cap == null) {
                return false;
            }

            return provider.hasCapability(cap, side);
        }

        <T> T getCapability(ICapabilityProvider provider, Capability<T> cap) {
            if (provider == null || cap == null) {
                return null;
            }

            return provider.getCapability(cap, side);
        }
    }
}
