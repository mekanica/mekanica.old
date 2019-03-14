package mekanism.client.gui;

import java.util.Arrays;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiFluidGauge;
import mekanism.client.gui.element.GuiGasGauge;
import mekanism.client.gui.element.GuiGauge;
import mekanism.client.gui.element.GuiPowerBar;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.GuiProgress.IProgressInfoHandler;
import mekanism.client.gui.element.GuiProgress.ProgressBar;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.GuiSecurityTab;
import mekanism.client.gui.element.GuiSideConfigurationTab;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.GuiTransporterConfigTab;
import mekanism.client.gui.element.GuiUpgradeTab;
import mekanism.common.inventory.container.ContainerPRC;
import mekanism.common.tile.TileEntityPRC;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiPRC extends GuiMekanism {

    public TileEntityPRC tileEntity;

    public GuiPRC(InventoryPlayer inventory, TileEntityPRC tentity) {
        super(tentity, new ContainerPRC(inventory, tentity));
        tileEntity = tentity;

        guiElements.add(new GuiRedstoneControl(this, tileEntity,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements
              .add(new GuiSecurityTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements.add(new GuiSideConfigurationTab(this, tileEntity,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements.add(new GuiTransporterConfigTab(this, 34, tileEntity,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements
              .add(new GuiUpgradeTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements.add(new GuiEnergyInfo(() ->
        {
            double extra = tileEntity.getRecipe() != null ? tileEntity.getRecipe().extraEnergy : 0;
            String multiplier = MekanismUtils.getEnergyDisplay(
                  MekanismUtils.getEnergyPerTick(tileEntity, tileEntity.BASE_ENERGY_PER_TICK + extra));
            return Arrays.asList(LangUtils.localize("gui.using") + ": " + multiplier + "/t",
                  LangUtils.localize("gui.needed") + ": " + MekanismUtils
                        .getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy()));
        }, this, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png")));
        guiElements.add(new GuiFluidGauge(() -> tileEntity.inputFluidTank, GuiGauge.Type.STANDARD_YELLOW, this,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 5, 10));
        guiElements.add(new GuiGasGauge(() -> tileEntity.inputGasTank, GuiGauge.Type.STANDARD_RED, this,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 28, 10));
        guiElements.add(new GuiGasGauge(() -> tileEntity.outputGasTank, GuiGauge.Type.SMALL_BLUE, this,
              MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 140, 40));
        guiElements
              .add(new GuiPowerBar(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 164,
                    15));

        guiElements
              .add(new GuiSlot(SlotType.INPUT, this, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 53,
                    34));
        guiElements
              .add(new GuiSlot(SlotType.POWER, this, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 140,
                    18).with(SlotOverlay.POWER));
        guiElements
              .add(new GuiSlot(SlotType.OUTPUT, this, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 115,
                    34));

        guiElements.add(new GuiProgress(new IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, getProgressType(), this, MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"), 75, 37));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        fontRenderer
              .drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2),
                    6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);

        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiBlank.png"));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;
        drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

        int xAxis = mouseX - guiWidth;
        int yAxis = mouseY - guiHeight;

        super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
    }

    public ProgressBar getProgressType() {
        return ProgressBar.MEDIUM;
    }
}
