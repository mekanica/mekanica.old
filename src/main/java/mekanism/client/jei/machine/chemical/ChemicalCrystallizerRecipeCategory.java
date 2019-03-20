package mekanism.client.jei.machine.chemical;

import mekanism.api.gas.GasStack;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.recipe.machines.CrystallizerRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;

public class ChemicalCrystallizerRecipeCategory extends BaseRecipeCategory {

    private final IDrawable background;

    public ChemicalCrystallizerRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/nei/GuiChemicalCrystallizer.png", "chemical_crystallizer",
              "tile.MachineBlock2.ChemicalCrystallizer.name", null);
        xOffset = 5;
        yOffset = 3;
        background = guiHelper.createDrawable(guiLocation, xOffset, yOffset, 147, 79);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        super.drawExtras(minecraft);
        drawTexturedRect(53 - xOffset, 61 - yOffset, 176, 63, (int) (48 * ((float) timer.getValue() / 20F)), 8);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
        if (recipeWrapper instanceof ChemicalCrystallizerRecipeWrapper) {
            CrystallizerRecipe tempRecipe = ((ChemicalCrystallizerRecipeWrapper) recipeWrapper).getRecipe();
            IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
            itemStacks.init(0, false, 130 - xOffset, 56 - yOffset);
            itemStacks.set(0, tempRecipe.getOutput().output);
            IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
            initGas(gasStacks, 0, true, 6 - xOffset, 5 - yOffset, 16, 58, tempRecipe.getInput().ingredient, true);
        }
    }
}