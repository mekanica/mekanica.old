package mekanism.client.gui.filter;

import mekanism.client.gui.GuiMekanism;
import mekanism.common.inventory.container.ContainerFilter;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public abstract class GuiFilter<TILE extends TileEntityContainerBlock> extends GuiMekanism<TILE> {

    protected GuiFilter(TILE tile, Container container) {
        super(tile, container);
    }

    protected GuiFilter(EntityPlayer player, TILE tile) {
        super(tile, new ContainerFilter(player.inventory, tile));
    }

    protected abstract void addButtons(int guiWidth, int guiHeight);

    protected abstract void sendPacketToServer(int guiID);
}