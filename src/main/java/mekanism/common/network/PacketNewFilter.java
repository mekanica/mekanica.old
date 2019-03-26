package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.api.TileNetworkList;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.content.transporter.TransporterFilter;
import mekanism.common.network.PacketNewFilter.NewFilterMessage;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.TileEntityOredictionificator;
import mekanism.common.tile.TileEntityOredictionificator.OredictionificatorFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketNewFilter implements IMessageHandler<NewFilterMessage, IMessage> {

    @Override
    public IMessage onMessage(NewFilterMessage message, MessageContext context) {
        WorldServer worldServer = FMLCommonHandler.instance().getMinecraftServerInstance()
              .getWorld(message.coord4D.dimensionId);

        worldServer.addScheduledTask(() ->
        {
            if (message.type == 0 && message.coord4D.getTileEntity(worldServer) instanceof TileEntityLogisticalSorter) {
                TileEntityLogisticalSorter sorter = (TileEntityLogisticalSorter) message.coord4D
                      .getTileEntity(worldServer);

                sorter.filters.add(message.tFilter);

                for (EntityPlayer iterPlayer : sorter.playersUsing) {
                    Mekanism.packetHandler.sendTo(
                          new TileEntityMessage(Coord4D.get(sorter), sorter.getFilterPacket(new TileNetworkList())),
                          (EntityPlayerMP) iterPlayer);
                }
            } else if (message.type == 1 && message.coord4D
                  .getTileEntity(worldServer) instanceof TileEntityDigitalMiner) {
                TileEntityDigitalMiner miner = (TileEntityDigitalMiner) message.coord4D.getTileEntity(worldServer);

                miner.filters.add(message.mFilter);

                for (EntityPlayer iterPlayer : miner.playersUsing) {
                    Mekanism.packetHandler.sendTo(
                          new TileEntityMessage(Coord4D.get(miner), miner.getFilterPacket(new TileNetworkList())),
                          (EntityPlayerMP) iterPlayer);
                }
            } else if (message.type == 2 && message.coord4D
                  .getTileEntity(worldServer) instanceof TileEntityOredictionificator) {
                TileEntityOredictionificator oredictionificator = (TileEntityOredictionificator) message.coord4D
                      .getTileEntity(worldServer);

                oredictionificator.filters.add(message.oFilter);

                for (EntityPlayer iterPlayer : oredictionificator.playersUsing) {
                    Mekanism.packetHandler.sendTo(new TileEntityMessage(Coord4D.get(oredictionificator),
                          oredictionificator.getFilterPacket(new TileNetworkList())), (EntityPlayerMP) iterPlayer);
                }
            }
        });

        return null;
    }

    public static class NewFilterMessage implements IMessage {

        public Coord4D coord4D;

        public TransporterFilter tFilter;

        public MinerFilter mFilter;

        public OredictionificatorFilter oFilter;

        public byte type = -1;

        public NewFilterMessage() {
        }

        public NewFilterMessage(Coord4D coord, Object filter) {
            coord4D = coord;

            if (filter instanceof TransporterFilter) {
                tFilter = (TransporterFilter) filter;
                type = 0;
            } else if (filter instanceof MinerFilter) {
                mFilter = (MinerFilter) filter;
                type = 1;
            } else if (filter instanceof OredictionificatorFilter) {
                oFilter = (OredictionificatorFilter) filter;
                type = 2;
            }
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            coord4D.write(dataStream);

            dataStream.writeByte(type);

            TileNetworkList data = new TileNetworkList();

            if (type == 0) {
                tFilter.write(data);
            } else if (type == 1) {
                mFilter.write(data);
            } else if (type == 2) {
                oFilter.write(data);
            }

            PacketHandler.encode(data.toArray(), dataStream);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            coord4D = Coord4D.read(dataStream);
            type = dataStream.readByte();

            if (type == 0) {
                tFilter = TransporterFilter.readFromPacket(dataStream);
            } else if (type == 1) {
                mFilter = MinerFilter.readFromPacket(dataStream);
            } else if (type == 2) {
                oFilter = OredictionificatorFilter.readFromPacket(dataStream);
            }
        }
    }
}
