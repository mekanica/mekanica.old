package mekanism.client.gui.element;

import mekanism.api.Coord4D;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.IRedstoneControl.RedstoneControl;
import mekanism.common.network.PacketRedstoneControl.RedstoneControlMessage;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiRedstoneControl extends GuiTileEntityElement<TileEntity> {

    public GuiRedstoneControl(IGuiWrapper gui, TileEntity tile, ResourceLocation def) {
        super(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiRedstoneControl.png"), gui, def, tile);
    }

    @Override
    public Rectangle4i getBounds(int guiWidth, int guiHeight) {
        return new Rectangle4i(guiWidth + 176, guiHeight + 138, 26, 26);
    }

    @Override
    protected boolean inBounds(int xAxis, int yAxis) {
        return xAxis >= 179 && xAxis <= 197 && yAxis >= 142 && yAxis <= 160;
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        mc.renderEngine.bindTexture(RESOURCE);
        guiObj.drawTexturedRect(guiWidth + 176, guiHeight + 138, 0, 0, 26, 26);
        IRedstoneControl control = (IRedstoneControl) tileEntity;
        int renderX = 26 + (18 * control.getControlType().ordinal());
        guiObj.drawTexturedRect(guiWidth + 179, guiHeight + 142, renderX, inBounds(xAxis, yAxis) ? 0 : 18, 18, 18);
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void renderForeground(int xAxis, int yAxis) {
        mc.renderEngine.bindTexture(RESOURCE);
        IRedstoneControl control = (IRedstoneControl) tileEntity;
        if (inBounds(xAxis, yAxis)) {
            displayTooltip(control.getControlType().getDisplay(), xAxis, yAxis);
        }
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void preMouseClicked(int xAxis, int yAxis, int button) {
    }

    @Override
    public void mouseClicked(int xAxis, int yAxis, int button) {
        IRedstoneControl control = (IRedstoneControl) tileEntity;

        if (button == 0 && inBounds(xAxis, yAxis)) {
            RedstoneControl current = control.getControlType();
            int ordinalToSet = current.ordinal() < (RedstoneControl.values().length - 1) ? current.ordinal() + 1 : 0;
            if (ordinalToSet == RedstoneControl.PULSE.ordinal() && !control.canPulse()) {
                ordinalToSet = 0;
            }

            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
            Mekanism.packetHandler.sendToServer(
                  new RedstoneControlMessage(Coord4D.get(tileEntity), RedstoneControl.values()[ordinalToSet]));
        }
    }
}