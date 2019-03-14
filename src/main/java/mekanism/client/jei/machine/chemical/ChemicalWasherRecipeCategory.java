package mekanism.client.jei.machine.chemical;

import javax.annotation.Nullable;
import mekanism.api.gas.GasStack;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.tile.TileEntityChemicalWasher;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class ChemicalWasherRecipeCategory extends BaseRecipeCategory {

    private final IDrawable background;

    @Nullable
    private WasherRecipe tempRecipe;

    public ChemicalWasherRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/nei/GuiChemicalWasher.png", "chemical_washer",
              "tile.MachineBlock2.ChemicalWasher.name", null);

        xOffset = 3;
        yOffset = 3;

        background = guiHelper.createDrawable(new ResourceLocation(guiTexture), xOffset, yOffset, 170, 70);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        super.drawExtras(minecraft);

        drawTexturedRect(61 - xOffset, 39 - yOffset, 176, 63, 55, 8);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
        if (!(recipeWrapper instanceof ChemicalWasherRecipeWrapper)) {
            return;
        }

        tempRecipe = ((ChemicalWasherRecipeWrapper) recipeWrapper).getRecipe();

        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();

        fluidStacks.init(0, true, 6 - xOffset, 5 - yOffset, 16, 58, TileEntityChemicalWasher.WATER_USAGE, false,
              fluidOverlayLarge);
        fluidStacks.set(0, ingredients.getInputs(FluidStack.class).get(0));

        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(GasStack.class);

        initGas(gasStacks, 0, true, 27 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getInput().ingredient, true);
        initGas(gasStacks, 1, false, 134 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getOutput().output, true);
    }
}
