package mekanism.common.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import mekanism.api.Coord4D;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.base.IEnergyWrapper;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig.general;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

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
     * @param tileEntity - center TileEntity
     * @param sideFunction - set of sides to check
     * @return boolean[] of adjacent connections
     */
    private static boolean[] getConnections(TileEntity tileEntity, Function<EnumFacing, Boolean> sideFunction) {
        boolean[] connectable = new boolean[]{false, false, false, false, false, false};
        Coord4D coord = Coord4D.get(tileEntity);

        for (EnumFacing side : EnumFacing.values()) {
            if (sideFunction.apply(side)) {
                TileEntity tile = coord.offset(side).getTileEntity(tileEntity.getWorld());

                connectable[side.ordinal()] = isValidAcceptorOnSide(tileEntity, tile, side);
                connectable[side.ordinal()] |= isCable(tile);
            }
        }

        return connectable;
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

    public static void emit(IEnergyWrapper emitter) {
        if (!((TileEntity) emitter).getWorld().isRemote && MekanismUtils.canFunction((TileEntity) emitter)) {
            double energyToSend = Math.min(emitter.getEnergy(), emitter.getMaxOutput());

            if (energyToSend > 0) {
                List<EnumFacing> outputtingSides = new LinkedList<>();
                boolean[] connectable = getConnections((TileEntity) emitter, emitter::sideIsOutput);

                for (EnumFacing side : EnumFacing.values()) {
                    if (connectable[side.ordinal()]) {
                        outputtingSides.add(side);
                    }
                }

                if (!outputtingSides.isEmpty()) {
                    double sent = 0;
                    boolean tryAgain = false;
                    int i = 0;

                    do {
                        double prev = sent;
                        sent += emit_do(emitter, outputtingSides, energyToSend - sent);

                        tryAgain = energyToSend - sent > 0 && sent - prev > 0 && i < 100;

                        i++;
                    } while (tryAgain);

                    emitter.setEnergy(emitter.getEnergy() - sent);
                }
            }
        }
    }

    private static double emit_do(IEnergyWrapper emitter, List<EnumFacing> outputtingSides, double totalToSend) {
        double remains = totalToSend % outputtingSides.size();
        double splitSend = (totalToSend - remains) / outputtingSides.size();
        double sent = 0;

        for (Iterator<EnumFacing> it = outputtingSides.iterator(); it.hasNext(); ) {
            EnumFacing side = it.next();

            TileEntity tileEntity = Coord4D.get((TileEntity) emitter).offset(side)
                  .getTileEntity(((TileEntity) emitter).getWorld());
            double toSend = splitSend + remains;
            remains = 0;

            double prev = sent;

            if (CapabilityUtils.hasCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite())) {
                IEnergyStorage storage = CapabilityUtils.getCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite());
                sent += storage.receiveEnergy((int) Math.round(Math.min(Integer.MAX_VALUE, toSend * general.TO_RF)),
                            false) * general.FROM_RF;
            }

            if (sent - prev == 0) {
                it.remove();
            }
        }

        return sent;
    }

}
