package mekanism.common.multipart;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.Range4D;
import mekanism.common.Mekanism;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;

public class PartDiversionTransporter extends PartLogisticalTransporter
{
	public int[] modes = {0, 0, 0, 0, 0, 0};

	@Override
	public ResourceLocation getType()
	{
		return new ResourceLocation("mekanism:diversion_transporter");
	}

	@Override
	public TransmitterType getTransmitterType()
	{
		return TransmitterType.DIVERSION_TRANSPORTER;
	}
	
	@Override
	public boolean renderCenter()
	{
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		modes = nbtTags.getIntArray("modes");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setIntArray("modes", modes);
		
		return nbtTags;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream) throws Exception
	{
		super.handlePacketData(dataStream);
		
		if(getWorld().isRemote)
		{
			modes[0] = dataStream.readInt();
			modes[1] = dataStream.readInt();
			modes[2] = dataStream.readInt();
			modes[3] = dataStream.readInt();
			modes[4] = dataStream.readInt();
			modes[5] = dataStream.readInt();
		}
	}

	@Override
	public ArrayList<Object> getNetworkedData(ArrayList<Object> data)
	{
		data = super.getNetworkedData(data);

		data.add(modes[0]);
		data.add(modes[1]);
		data.add(modes[2]);
		data.add(modes[3]);
		data.add(modes[4]);
		data.add(modes[5]);

		return data;
	}

	@Override
	public ArrayList<Object> getSyncPacket(TransporterStack stack, boolean kill)
	{
		ArrayList<Object> data = super.getSyncPacket(stack, kill);

		data.add(modes[0]);
		data.add(modes[1]);
		data.add(modes[2]);
		data.add(modes[3]);
		data.add(modes[4]);
		data.add(modes[5]);

		return data;
	}

	@Override
	protected EnumActionResult onConfigure(EntityPlayer player, int part, EnumFacing side)
	{
		int newMode = (modes[side.ordinal()] + 1) % 3;
		String description = "ERROR";

		modes[side.ordinal()] = newMode;

		switch(newMode)
		{
			case 0:
				description = LangUtils.localize("control.disabled.desc");
				break;
			case 1:
				description = LangUtils.localize("control.high.desc");
				break;
			case 2:
				description = LangUtils.localize("control.low.desc");
				break;
		}

		refreshConnections();
		notifyPartUpdate();
		notifyTileChange();
		player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " " + LangUtils.localize("tooltip.configurator.toggleDiverter") + ": " + EnumColor.RED + description));
		Coord4D coord = new Coord4D(getPos(), getWorld());
		Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(coord, getNetworkedData(new ArrayList<Object>())), new Range4D(coord));

		return EnumActionResult.SUCCESS;
	}

	@Override
	public boolean canConnect(EnumFacing side)
	{
		if(!super.canConnect(side))
		{
			return false;
		}

		int mode = modes[side.ordinal()];
		boolean redstone = MekanismUtils.isGettingPowered(getWorld(), new Coord4D(getPos(), getWorld()));

		if((mode == 2 && redstone) || (mode == 1 && !redstone))
		{
			return false;
		}

		return true;
	}
	
	@Override
	public EnumColor getRenderColor()
	{
		return null;
	}
}
