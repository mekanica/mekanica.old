package mekanism.common.integration.forgeenergy;

import mekanism.common.base.IEnergyWrapper;
import mekanism.common.config.MekanismConfig.general;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.IEnergyStorage;

public class ForgeEnergyIntegration implements IEnergyStorage {

    public IEnergyWrapper tileEntity;

    public EnumFacing side;

    public ForgeEnergyIntegration(IEnergyWrapper tile, EnumFacing facing) {
        tileEntity = tile;
        side = facing;
    }

    public static double forgeToMek(int forge) {
        return forge * general.FROM_RF;
    }

    public static int mekToForge(double mek) {
        return (int) Math.round(mek * general.FROM_RF);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return mekToForge(tileEntity.acceptEnergy(side, forgeToMek(maxReceive), simulate));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return mekToForge(tileEntity.pullEnergy(side, forgeToMek(maxExtract), simulate));
    }

    @Override
    public int getEnergyStored() {
        return Math.min(Integer.MAX_VALUE, mekToForge(tileEntity.getEnergy()));
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.min(Integer.MAX_VALUE, mekToForge(tileEntity.getMaxEnergy()));
    }

    @Override
    public boolean canExtract() {
        return tileEntity.sideIsOutput(side);
    }

    @Override
    public boolean canReceive() {
        return tileEntity.sideIsConsumer(side);
    }
}
