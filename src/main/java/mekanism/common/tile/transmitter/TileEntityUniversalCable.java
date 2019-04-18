package mekanism.common.tile.transmitter;

import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.TileNetworkList;
import mekanism.api.energy.EnergyStack;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Tier;
import mekanism.common.Tier.BaseTier;
import mekanism.common.Tier.CableTier;
import mekanism.common.base.EnergyAcceptorWrapper;
import mekanism.common.block.states.BlockStateTransmitter.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.CapabilityWrapperManager;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.integration.forgeenergy.ForgeEnergyCableIntegration;
import mekanism.common.transmitters.grid.EnergyNetwork;
import mekanism.common.util.CableUtils;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class TileEntityUniversalCable extends TileEntityTransmitter<EnergyAcceptorWrapper, EnergyNetwork> implements
      IStrictEnergyAcceptor, IStrictEnergyStorage {

    public Tier.CableTier tier = CableTier.BASIC;

    public double currentPower = 0;
    public double lastWrite = 0;

    public EnergyStack buffer = new EnergyStack(0);
    private CapabilityWrapperManager forgeEnergyManager = new CapabilityWrapperManager<>(getClass(),
          ForgeEnergyCableIntegration.class);

    @Override
    public BaseTier getBaseTier() {
        return tier.getBaseTier();
    }

    @Override
    public void setBaseTier(BaseTier baseTier) {
        tier = Tier.CableTier.get(baseTier);
    }

    @Override
    public void update() {
        if (getWorld().isRemote) {
            double targetPower =
                  getTransmitter().hasTransmitterNetwork() ? getTransmitter().getTransmitterNetwork().clientEnergyScale
                        : 0;

            if (Math.abs(currentPower - targetPower) > 0.01) {
                currentPower = (9 * currentPower + targetPower) / 10;
            }
        } else {
            updateShare();

            List<EnumFacing> sides = getConnections(ConnectionType.PULL);

            if (!sides.isEmpty()) {
                TileEntity[] connectedOutputters = CableUtils.getConnectedOutputters(getPos(), getWorld());
                double canDraw = tier.cableCapacity;

                for (EnumFacing side : sides) {
                    if (connectedOutputters[side.ordinal()] != null) {
                        TileEntity outputter = connectedOutputters[side.ordinal()];

                        if (CapabilityUtils.hasCapability(outputter, CapabilityEnergy.ENERGY, side.getOpposite())) {
                            IEnergyStorage storage = CapabilityUtils
                                  .getCapability(outputter, CapabilityEnergy.ENERGY, side.getOpposite());
                            double toDraw = storage.extractEnergy((int) Math.round(canDraw * general.TO_RF), true)
                                  * general.FROM_RF;

                            if (toDraw > 0) {
                                toDraw -= takeEnergy(toDraw, true);
                            }

                            storage.extractEnergy((int) Math.round(toDraw * general.TO_RF), false);
                        }
                    }
                }
            }
        }

        super.update();
    }

    @Override
    public void updateShare() {
        if (getTransmitter().hasTransmitterNetwork() && getTransmitter().getTransmitterNetworkSize() > 0) {
            double last = getSaveShare();

            if (last != lastWrite) {
                lastWrite = last;
                markDirty();
            }
        }
    }

    private double getSaveShare() {
        if (getTransmitter().hasTransmitterNetwork()) {
            return EnergyNetwork.round(getTransmitter().getTransmitterNetwork().buffer.amount * (1F / getTransmitter()
                  .getTransmitterNetwork().getSize()));
        } else {
            return buffer.amount;
        }
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.UNIVERSAL_CABLE;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTags) {
        super.readFromNBT(nbtTags);

        buffer.amount = nbtTags.getDouble("cacheEnergy");
        if (buffer.amount < 0) {
            buffer.amount = 0;
        }

        if (nbtTags.hasKey("tier")) {
            tier = Tier.CableTier.values()[nbtTags.getInteger("tier")];
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTags) {
        super.writeToNBT(nbtTags);

        nbtTags.setDouble("cacheEnergy", lastWrite);
        nbtTags.setInteger("tier", tier.ordinal());

        return nbtTags;
    }

    @Override
    public TransmissionType getTransmissionType() {
        return TransmissionType.ENERGY;
    }

    @Override
    public EnergyNetwork createNetworkByMerging(Collection<EnergyNetwork> networks) {
        return new EnergyNetwork(networks);
    }

    @Override
    public boolean isValidAcceptor(TileEntity acceptor, EnumFacing side) {
        return CableUtils.isValidAcceptorOnSide(getWorld().getTileEntity(getPos()), acceptor, side);
    }

    @Override
    public EnergyNetwork createNewNetwork() {
        return new EnergyNetwork();
    }

    @Override
    public Object getBuffer() {
        return buffer;
    }

    @Override
    public void takeShare() {
        if (getTransmitter().hasTransmitterNetwork()) {
            getTransmitter().getTransmitterNetwork().buffer.amount -= lastWrite;
            buffer.amount = lastWrite;
        }
    }

    @Override
    public int getCapacity() {
        return tier.cableCapacity;
    }

    @Override
    public double acceptEnergy(EnumFacing side, double amount, boolean simulate) {
        double toUse = Math.min(getMaxEnergy() - getEnergy(), amount);

        if (toUse < 0.0001 || (side != null && !canReceiveEnergy(side))) {
            return 0;
        }

        if (!simulate) {
            setEnergy(getEnergy() + toUse);
        }

        return toUse;
    }

    @Override
    public boolean canReceiveEnergy(EnumFacing side) {
        if (side == null) {
            return true;
        }

        return getConnectionType(side) == ConnectionType.NORMAL;
    }

    @Override
    public double getMaxEnergy() {
        if (getTransmitter().hasTransmitterNetwork()) {
            return getTransmitter().getTransmitterNetwork().getCapacity();
        } else {
            return getCapacity();
        }
    }

    @Override
    public double getEnergy() {
        if (getTransmitter().hasTransmitterNetwork()) {
            return getTransmitter().getTransmitterNetwork().buffer.amount;
        } else {
            return buffer.amount;
        }
    }

    @Override
    public void setEnergy(double energy) {
        if (getTransmitter().hasTransmitterNetwork()) {
            getTransmitter().getTransmitterNetwork().buffer.amount = energy;
        } else {
            buffer.amount = energy;
        }
    }

    public double takeEnergy(double energy, boolean doEmit) {
        if (getTransmitter().hasTransmitterNetwork()) {
            return getTransmitter().getTransmitterNetwork().emit(energy, doEmit);
        } else {
            double used = Math.min(getCapacity() - buffer.amount, energy);

            if (doEmit) {
                buffer.amount += used;
            }

            return energy - used;
        }
    }

    @Override
    public EnergyAcceptorWrapper getCachedAcceptor(EnumFacing side) {
        return EnergyAcceptorWrapper.get(getCachedTile(side), side.getOpposite());
    }

    @Override
    public boolean upgrade(int tierOrdinal) {
        if (tier.ordinal() < BaseTier.ULTIMATE.ordinal() && tierOrdinal == tier.ordinal() + 1) {
            tier = CableTier.values()[tier.ordinal() + 1];

            markDirtyTransmitters();
            sendDesc = true;

            return true;
        }

        return false;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) throws Exception {
        tier = CableTier.values()[dataStream.readInt()];

        super.handlePacketData(dataStream);
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        data.add(tier.ordinal());

        super.getNetworkedData(data);

        return data;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) forgeEnergyManager.getWrapper(this, facing);
        }

        return super.getCapability(capability, facing);
    }
}
