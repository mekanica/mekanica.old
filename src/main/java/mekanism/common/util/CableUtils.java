package mekanism.common.util;

import mekanism.api.transmitters.TransmissionType;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

public final class CableUtils {

    private static boolean isCable(TileEntity tileEntity) {
        if (CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, null)) {
            return TransmissionType.checkTransmissionType(
                  CapabilityUtils.getCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, null),
                  TransmissionType.ENERGY);
        }

        return false;
    }

    /**
     * Gets the adjacent connections to a TileEntity, from a subset of its sides.
     *
     * @param cableEntity - TileEntity that's trying to connect
     * @param side - side to check
     * @return boolean whether the acceptor is valid
     */
    public static boolean isValidAcceptorOnSide(TileEntity cableEntity, TileEntity tile, EnumFacing side) {
        if (tile == null || isCable(tile)) {
            return false;
        }

        return isAcceptor(cableEntity, tile, side) || isOutputter(tile, side);
    }

    public static TileEntity[] getConnectedOutputters(BlockPos pos, World world) {
        TileEntity[] outputters = new TileEntity[]{null, null, null, null, null, null};

        for (EnumFacing orientation : EnumFacing.VALUES) {
            TileEntity outputter = world.getTileEntity(pos.offset(orientation));

            if (isOutputter(outputter, orientation)) {
                outputters[orientation.ordinal()] = outputter;
            }
        }

        return outputters;
    }

    private static boolean isOutputter(TileEntity tileEntity, EnumFacing side) {
        if (tileEntity != null && CapabilityUtils
              .hasCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite())) {
            return CapabilityUtils.getCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite()).canExtract();
        }

        return false;
    }

    private static boolean isAcceptor(TileEntity orig, TileEntity tileEntity, EnumFacing side) {
        if (CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite())) {
            return false;
        }

        if (CapabilityUtils.hasCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite())) {
            return CapabilityUtils.getCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite()).canReceive();
        }

        return false;
    }
}
