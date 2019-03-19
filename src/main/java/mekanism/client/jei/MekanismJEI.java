package mekanism.client.jei;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.client.gui.chemical.GuiChemicalCrystallizer;
import mekanism.client.gui.chemical.GuiChemicalDissolutionChamber;
import mekanism.client.gui.chemical.GuiChemicalInfuser;
import mekanism.client.gui.chemical.GuiChemicalInjectionChamber;
import mekanism.client.gui.chemical.GuiChemicalOxidizer;
import mekanism.client.gui.chemical.GuiChemicalWasher;
import mekanism.client.gui.GuiCombiner;
import mekanism.client.gui.GuiCrusher;
import mekanism.client.gui.GuiElectrolyticSeparator;
import mekanism.client.gui.GuiEnergizedSmelter;
import mekanism.client.gui.GuiEnrichmentChamber;
import mekanism.client.gui.GuiMetallurgicInfuser;
import mekanism.client.gui.GuiOsmiumCompressor;
import mekanism.client.gui.GuiPRC;
import mekanism.client.gui.GuiPrecisionSawmill;
import mekanism.client.gui.GuiPurificationChamber;
import mekanism.client.gui.GuiRotaryCondensentrator;
import mekanism.client.gui.GuiSolarNeutronActivator;
import mekanism.client.gui.GuiThermalEvaporationController;
import mekanism.client.gui.element.GuiProgress.ProgressBar;
import mekanism.client.jei.gas.GasStackRenderer;
import mekanism.client.jei.machine.AdvancedMachineRecipeCategory;
import mekanism.client.jei.machine.AdvancedMachineRecipeWrapper;
import mekanism.client.jei.machine.ChanceMachineRecipeCategory;
import mekanism.client.jei.machine.ChanceMachineRecipeWrapper;
import mekanism.client.jei.machine.DoubleMachineRecipeCategory;
import mekanism.client.jei.machine.DoubleMachineRecipeWrapper;
import mekanism.client.jei.machine.MachineRecipeCategory;
import mekanism.client.jei.machine.MachineRecipeWrapper;
import mekanism.client.jei.machine.chemical.ChemicalCrystallizerRecipeCategory;
import mekanism.client.jei.machine.chemical.ChemicalCrystallizerRecipeWrapper;
import mekanism.client.jei.machine.chemical.ChemicalDissolutionChamberRecipeCategory;
import mekanism.client.jei.machine.chemical.ChemicalDissolutionChamberRecipeWrapper;
import mekanism.client.jei.machine.chemical.ChemicalInfuserRecipeCategory;
import mekanism.client.jei.machine.chemical.ChemicalInfuserRecipeWrapper;
import mekanism.client.jei.machine.chemical.ChemicalOxidizerRecipeCategory;
import mekanism.client.jei.machine.chemical.ChemicalOxidizerRecipeWrapper;
import mekanism.client.jei.machine.chemical.ChemicalWasherRecipeCategory;
import mekanism.client.jei.machine.chemical.ChemicalWasherRecipeWrapper;
import mekanism.client.jei.machine.other.ElectrolyticSeparatorRecipeCategory;
import mekanism.client.jei.machine.other.ElectrolyticSeparatorRecipeWrapper;
import mekanism.client.jei.machine.other.MetallurgicInfuserRecipeCategory;
import mekanism.client.jei.machine.other.MetallurgicInfuserRecipeWrapper;
import mekanism.client.jei.machine.other.PRCRecipeCategory;
import mekanism.client.jei.machine.other.PRCRecipeWrapper;
import mekanism.client.jei.machine.other.RotaryCondensentratorRecipeCategory;
import mekanism.client.jei.machine.other.RotaryCondensentratorRecipeWrapper;
import mekanism.client.jei.machine.other.SolarNeutronRecipeCategory;
import mekanism.client.jei.machine.other.SolarNeutronRecipeWrapper;
import mekanism.client.jei.machine.other.ThermalEvaporationRecipeCategory;
import mekanism.client.jei.machine.other.ThermalEvaporationRecipeWrapper;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.MekanismItems;
import mekanism.common.Tier;
import mekanism.common.base.IFactory;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.base.ITierItem;
import mekanism.common.block.states.BlockStateBasic.BasicBlockType;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.config.MekanismConfig.general;
import mekanism.common.integration.crafttweaker.handlers.EnergizedSmelter;
import mekanism.common.inventory.container.ContainerFormulaicAssemblicator;
import mekanism.common.inventory.container.robit.ContainerRobitInventory;
import mekanism.common.item.ItemBlockEnergyCube;
import mekanism.common.item.ItemBlockGasTank;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.AdvancedMachineRecipe;
import mekanism.common.recipe.machines.BasicMachineRecipe;
import mekanism.common.recipe.machines.ChanceMachineRecipe;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.machines.CombinerRecipe;
import mekanism.common.recipe.machines.CrusherRecipe;
import mekanism.common.recipe.machines.CrystallizerRecipe;
import mekanism.common.recipe.machines.DissolutionRecipe;
import mekanism.common.recipe.machines.DoubleMachineRecipe;
import mekanism.common.recipe.machines.EnrichmentRecipe;
import mekanism.common.recipe.machines.InjectionRecipe;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.machines.MetallurgicInfuserRecipe;
import mekanism.common.recipe.machines.OsmiumCompressorRecipe;
import mekanism.common.recipe.machines.OxidationRecipe;
import mekanism.common.recipe.machines.PressurizedRecipe;
import mekanism.common.recipe.machines.PurificationRecipe;
import mekanism.common.recipe.machines.SawmillRecipe;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.machines.SmeltingRecipe;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import mekanism.common.recipe.machines.ThermalEvaporationRecipe;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.Loader;

@JEIPlugin
public class MekanismJEI implements IModPlugin {

    public static final IIngredientType<GasStack> TYPE_GAS = () -> GasStack.class;

    public static final ISubtypeInterpreter NBT_INTERPRETER = itemStack ->
    {
        String ret = Integer.toString(itemStack.getMetadata());

        if (itemStack.getItem() instanceof ITierItem) {
            ret += ":" + ((ITierItem) itemStack.getItem()).getBaseTier(itemStack).getSimpleName();
        }

        if (itemStack.getItem() instanceof IFactory) {
            ret += ":" + RecipeType.values()[((IFactory) itemStack.getItem()).getRecipeType(itemStack)].getName();
        }

        if (itemStack.getItem() instanceof ItemBlockGasTank) {
            GasStack gasStack = ((ItemBlockGasTank) itemStack.getItem()).getGas(itemStack);
            if (gasStack != null) {
                ret += ":" + gasStack.getGas().getName();
            }
        }

        if (itemStack.getItem() instanceof ItemBlockEnergyCube) {
            ret += ":" + (((ItemBlockEnergyCube) itemStack.getItem()).getEnergy(itemStack) > 0 ? "filled" : "empty");
        }

        return ret.toLowerCase(Locale.ROOT);
    };
    private static final GasStackRenderer GAS_RENDERER = new GasStackRenderer();
    private static final boolean CRAFTTWEAKER_LOADED = Loader.isModLoaded("crafttweaker");

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.EnergyCube), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.MachineBlock), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.MachineBlock2), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.MachineBlock3), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.BasicBlock), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.BasicBlock2), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.GasTank), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.CardboardBox), NBT_INTERPRETER);
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(MekanismBlocks.Transmitter), NBT_INTERPRETER);
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registry) {
        List<GasStack> list = GasRegistry.getRegisteredGasses().stream().filter(Gas::isVisible)
              .map(g -> new GasStack(g, Fluid.BUCKET_VOLUME)).collect(Collectors.toList());
        registry.register(MekanismJEI.TYPE_GAS, list, new GasStackHelper(), GAS_RENDERER);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {

        addRecipeCategory(registry, MachineType.CHEMICAL_CRYSTALLIZER,
              new ChemicalCrystallizerRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.CHEMICAL_DISSOLUTION_CHAMBER,
              new ChemicalDissolutionChamberRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.CHEMICAL_INFUSER, new ChemicalInfuserRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.CHEMICAL_OXIDIZER, new ChemicalOxidizerRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.CHEMICAL_WASHER, new ChemicalWasherRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.ELECTROLYTIC_SEPARATOR, new ElectrolyticSeparatorRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.METALLURGIC_INFUSER, new MetallurgicInfuserRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));
        addRecipeCategory(registry, MachineType.PRESSURIZED_REACTION_CHAMBER,
              new PRCRecipeCategory(registry.getJeiHelpers().getGuiHelper()));

        addRecipeCategory(registry, MachineType.ROTARY_CONDENSENTRATOR, new RotaryCondensentratorRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), true));
        addRecipeCategory(registry, MachineType.ROTARY_CONDENSENTRATOR, new RotaryCondensentratorRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), false));

        addRecipeCategory(registry, MachineType.SOLAR_NEUTRON_ACTIVATOR, new SolarNeutronRecipeCategory(
              registry.getJeiHelpers().getGuiHelper()));

        addRecipeCategory(registry, MachineType.COMBINER, new DoubleMachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.COMBINER.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.Combiner.name", ProgressBar.STONE));

        addRecipeCategory(registry, MachineType.PURIFICATION_CHAMBER, new AdvancedMachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.PURIFICATION_CHAMBER.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.PurificationChamber.name", ProgressBar.RED));
        addRecipeCategory(registry, MachineType.OSMIUM_COMPRESSOR, new AdvancedMachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.OSMIUM_COMPRESSOR.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.OsmiumCompressor.name", ProgressBar.RED));
        addRecipeCategory(registry, MachineType.CHEMICAL_INJECTION_CHAMBER, new AdvancedMachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(),
              Recipe.CHEMICAL_INJECTION_CHAMBER.name().toLowerCase(Locale.ROOT), "nei.chemicalInjectionChamber",
              ProgressBar.YELLOW));

        addRecipeCategory(registry, MachineType.PRECISION_SAWMILL, new ChanceMachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.PRECISION_SAWMILL.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock2.PrecisionSawmill.name", ProgressBar.PURPLE));

        addRecipeCategory(registry, MachineType.ENRICHMENT_CHAMBER, new MachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.ENRICHMENT_CHAMBER.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.EnrichmentChamber.name", ProgressBar.BLUE));
        addRecipeCategory(registry, MachineType.CRUSHER, new MachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.CRUSHER.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.Crusher.name", ProgressBar.CRUSH));
        addRecipeCategory(registry, MachineType.ENERGIZED_SMELTER, new MachineRecipeCategory(
              registry.getJeiHelpers().getGuiHelper(), Recipe.ENERGIZED_SMELTER.name().toLowerCase(Locale.ROOT),
              "tile.MachineBlock.EnergizedSmelter.name", ProgressBar.BLUE));

        //There is no config option to disable the thermal evaporation plant
        registry.addRecipeCategories(new ThermalEvaporationRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    private void addRecipeCategory(IRecipeCategoryRegistration registry, MachineType type,
          BaseRecipeCategory category) {
        if (general.machinesManager.isEnabled(type)) {
            registry.addRecipeCategories(category);
        }
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new GuiElementHandler());

        registry.getJeiHelpers().getIngredientBlacklist()
              .addIngredientToBlacklist(new ItemStack(MekanismItems.ItemProxy));
        registry.getJeiHelpers().getIngredientBlacklist()
              .addIngredientToBlacklist(new ItemStack(MekanismBlocks.BoundingBlock));

        registry.handleRecipes(EnrichmentRecipe.class, MachineRecipeWrapper::new, "mekanism.enrichment_chamber");
        addRecipes(registry, Recipe.ENRICHMENT_CHAMBER, BasicMachineRecipe.class, MachineRecipeWrapper.class,
              "mekanism.enrichment_chamber");

        registry.handleRecipes(CrusherRecipe.class, MachineRecipeWrapper::new, "mekanism.crusher");
        addRecipes(registry, Recipe.CRUSHER, BasicMachineRecipe.class, MachineRecipeWrapper.class, "mekanism.crusher");

        registry.handleRecipes(CombinerRecipe.class, DoubleMachineRecipeWrapper::new, "mekanism.combiner");
        addRecipes(registry, Recipe.COMBINER, DoubleMachineRecipe.class, DoubleMachineRecipeWrapper.class,
              "mekanism.combiner");

        registry.handleRecipes(PurificationRecipe.class, AdvancedMachineRecipeWrapper::new,
              "mekanism.purification_chamber");
        addRecipes(registry, Recipe.PURIFICATION_CHAMBER, AdvancedMachineRecipe.class,
              AdvancedMachineRecipeWrapper.class, "mekanism.purification_chamber");

        registry.handleRecipes(OsmiumCompressorRecipe.class, AdvancedMachineRecipeWrapper::new,
              "mekanism.osmium_compressor");
        addRecipes(registry, Recipe.OSMIUM_COMPRESSOR, AdvancedMachineRecipe.class, AdvancedMachineRecipeWrapper.class,
              "mekanism.osmium_compressor");

        registry.handleRecipes(InjectionRecipe.class, AdvancedMachineRecipeWrapper::new,
              "mekanism.chemical_injection_chamber");
        addRecipes(registry, Recipe.CHEMICAL_INJECTION_CHAMBER, AdvancedMachineRecipe.class,
              AdvancedMachineRecipeWrapper.class, "mekanism.chemical_injection_chamber");

        registry.handleRecipes(SawmillRecipe.class, ChanceMachineRecipeWrapper::new, "mekanism.precision_sawmill");
        addRecipes(registry, Recipe.PRECISION_SAWMILL, ChanceMachineRecipe.class, ChanceMachineRecipeWrapper.class,
              "mekanism.precision_sawmill");

        registry.handleRecipes(MetallurgicInfuserRecipe.class, MetallurgicInfuserRecipeWrapper::new,
              "mekanism.metallurgic_infuser");
        addRecipes(registry, Recipe.METALLURGIC_INFUSER, MetallurgicInfuserRecipe.class,
              MetallurgicInfuserRecipeWrapper.class, "mekanism.metallurgic_infuser");

        registry.handleRecipes(CrystallizerRecipe.class, ChemicalCrystallizerRecipeWrapper::new,
              "mekanism.chemical_crystallizer");
        addRecipes(registry, Recipe.CHEMICAL_CRYSTALLIZER, CrystallizerRecipe.class,
              ChemicalCrystallizerRecipeWrapper.class, "mekanism.chemical_crystallizer");

        registry.handleRecipes(DissolutionRecipe.class, ChemicalDissolutionChamberRecipeWrapper::new,
              "mekanism.chemical_dissolution_chamber");
        addRecipes(registry, Recipe.CHEMICAL_DISSOLUTION_CHAMBER, DissolutionRecipe.class,
              ChemicalDissolutionChamberRecipeWrapper.class, "mekanism.chemical_dissolution_chamber");

        registry.handleRecipes(ChemicalInfuserRecipe.class, ChemicalInfuserRecipeWrapper::new,
              "mekanism.chemical_infuser");
        addRecipes(registry, Recipe.CHEMICAL_INFUSER, ChemicalInfuserRecipe.class, ChemicalInfuserRecipeWrapper.class,
              "mekanism.chemical_infuser");

        registry.handleRecipes(OxidationRecipe.class, ChemicalOxidizerRecipeWrapper::new, "mekanism.chemical_oxidizer");
        addRecipes(registry, Recipe.CHEMICAL_OXIDIZER, OxidationRecipe.class, ChemicalOxidizerRecipeWrapper.class,
              "mekanism.chemical_oxidizer");

        registry.handleRecipes(WasherRecipe.class, ChemicalWasherRecipeWrapper::new, "mekanism.chemical_washer");
        addRecipes(registry, Recipe.CHEMICAL_WASHER, WasherRecipe.class, ChemicalWasherRecipeWrapper.class,
              "mekanism.chemical_washer");

        registry.handleRecipes(SolarNeutronRecipe.class, SolarNeutronRecipeWrapper::new,
              "mekanism.solar_neutron_activator");
        addRecipes(registry, Recipe.SOLAR_NEUTRON_ACTIVATOR, SolarNeutronRecipe.class, SolarNeutronRecipeWrapper.class,
              "mekanism.solar_neutron_activator");

        registry.handleRecipes(SeparatorRecipe.class, ElectrolyticSeparatorRecipeWrapper::new,
              "mekanism.electrolytic_separator");
        addRecipes(registry, Recipe.ELECTROLYTIC_SEPARATOR, SeparatorRecipe.class,
              ElectrolyticSeparatorRecipeWrapper.class, "mekanism.electrolytic_separator");

        registry.handleRecipes(ThermalEvaporationRecipe.class, ThermalEvaporationRecipeWrapper::new,
              "mekanism.thermal_evaporation_plant");
        addRecipes(registry, Recipe.THERMAL_EVAPORATION_PLANT, ThermalEvaporationRecipe.class,
              ThermalEvaporationRecipeWrapper.class, "mekanism.thermal_evaporation_plant");

        registry.handleRecipes(PressurizedRecipe.class, PRCRecipeWrapper::new, "mekanism.pressurized_reaction_chamber");
        addRecipes(registry, Recipe.PRESSURIZED_REACTION_CHAMBER, PressurizedRecipe.class, PRCRecipeWrapper.class,
              "mekanism.pressurized_reaction_chamber");

        List<RotaryCondensentratorRecipeWrapper> condensentratorRecipes = new ArrayList<>();
        List<RotaryCondensentratorRecipeWrapper> decondensentratorRecipes = new ArrayList<>();

        for (Gas gas : GasRegistry.getRegisteredGasses()) {
            if (gas.hasFluid()) {
                condensentratorRecipes.add(new RotaryCondensentratorRecipeWrapper(gas.getFluid(), gas, true));
                decondensentratorRecipes.add(new RotaryCondensentratorRecipeWrapper(gas.getFluid(), gas, false));
            }
        }
        registry.addRecipes(condensentratorRecipes, "mekanism.rotary_condensentrator_condensentrating");
        registry.addRecipes(decondensentratorRecipes, "mekanism.rotary_condensentrator_decondensentrating");

        registry.handleRecipes(SmeltingRecipe.class, MachineRecipeWrapper::new, "mekanism.energized_smelter");

        if (CRAFTTWEAKER_LOADED && EnergizedSmelter.hasRemovedRecipe()) // Removed / Removed + Added
        {
            // Add all recipes
            addRecipes(registry, Recipe.ENERGIZED_SMELTER, BasicMachineRecipe.class, MachineRecipeWrapper.class,
                  "mekanism.energized_smelter");
        } else if (CRAFTTWEAKER_LOADED && EnergizedSmelter.hasAddedRecipe()) // Added but not removed
        {
            // Only add added recipes
            HashMap<ItemStackInput, SmeltingRecipe> smeltingRecipes = Recipe.ENERGIZED_SMELTER.get();
            Collection<SmeltingRecipe> recipes = smeltingRecipes.entrySet().stream()
                  .filter(entry -> !FurnaceRecipes.instance().getSmeltingList().keySet()
                        .contains(entry.getKey().ingredient))
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values();

            addRecipes(registry, recipes, BasicMachineRecipe.class, MachineRecipeWrapper.class,
                  "mekanism.energized_smelter");
        }
        // else - Only use furnace list, so no extra registration.

        registry.addRecipeClickArea(GuiEnrichmentChamber.class, 79, 40, 24, 7, "mekanism.enrichment_chamber");
        registry.addRecipeClickArea(GuiCrusher.class, 79, 40, 24, 7, "mekanism.crusher");
        registry.addRecipeClickArea(GuiCombiner.class, 79, 40, 24, 7, "mekanism.combiner");
        registry.addRecipeClickArea(GuiPurificationChamber.class, 79, 40, 24, 7, "mekanism.purification_chamber");
        registry.addRecipeClickArea(GuiOsmiumCompressor.class, 79, 40, 24, 7, "mekanism.osmium_compressor");
        registry.addRecipeClickArea(GuiChemicalInjectionChamber.class, 79, 40, 24, 7,
              "mekanism.chemical_injection_chamber");
        registry.addRecipeClickArea(GuiPrecisionSawmill.class, 79, 40, 24, 7, "mekanism.precision_sawmill");
        registry.addRecipeClickArea(GuiMetallurgicInfuser.class, 72, 47, 32, 8, "mekanism.metallurgic_infuser");
        registry.addRecipeClickArea(GuiChemicalCrystallizer.class, 53, 62, 48, 8, "mekanism.chemical_crystallizer");
        registry.addRecipeClickArea(GuiChemicalDissolutionChamber.class, 64, 40, 48, 8,
              "mekanism.chemical_dissolution_chamber");
        registry.addRecipeClickArea(GuiChemicalInfuser.class, 47, 39, 28, 8, "mekanism.chemical_infuser");
        registry.addRecipeClickArea(GuiChemicalInfuser.class, 101, 39, 28, 8, "mekanism.chemical_infuser");
        registry.addRecipeClickArea(GuiChemicalOxidizer.class, 64, 40, 48, 8, "mekanism.chemical_oxidizer");
        registry.addRecipeClickArea(GuiChemicalWasher.class, 61, 39, 55, 8, "mekanism.chemical_washer");
        registry.addRecipeClickArea(GuiSolarNeutronActivator.class, 64, 39, 48, 8, "mekanism.solar_neutron_activator");
        registry.addRecipeClickArea(GuiElectrolyticSeparator.class, 80, 30, 16, 6, "mekanism.electrolytic_separator");
        registry.addRecipeClickArea(GuiThermalEvaporationController.class, 49, 20, 78, 38,
              "mekanism.thermal_evaporation_plant");
        registry.addRecipeClickArea(GuiPRC.class, 75, 37, 36, 10, "mekanism.pressurized_reaction_chamber");
        registry.addRecipeClickArea(GuiRotaryCondensentrator.class, 64, 39, 48, 8,
              "mekanism.rotary_condensentrator_condensentrating", "mekanism.rotary_condensentrator_decondensentrating");

        // Energized smelter
        if (CRAFTTWEAKER_LOADED && EnergizedSmelter.hasRemovedRecipe()) {
            registry.addRecipeClickArea(GuiEnergizedSmelter.class, 79, 40, 24, 7, "mekanism.energized_smelter");
        } else if (CRAFTTWEAKER_LOADED && EnergizedSmelter.hasAddedRecipe()) {
            registry.addRecipeClickArea(GuiEnergizedSmelter.class, 79, 40, 24, 7, VanillaRecipeCategoryUid.SMELTING,
                  "mekanism.energized_smelter");

            registerVanillaSmeltingRecipeCatalyst(registry);
        } else {
            registry.addRecipeClickArea(GuiEnergizedSmelter.class, 79, 40, 24, 7, VanillaRecipeCategoryUid.SMELTING);

            registerVanillaSmeltingRecipeCatalyst(registry);
        }

        registerRecipeItem(registry, MachineType.ENRICHMENT_CHAMBER);
        registerRecipeItem(registry, MachineType.CRUSHER);
        registerRecipeItem(registry, MachineType.ENERGIZED_SMELTER);
        registerRecipeItem(registry, MachineType.COMBINER);
        registerRecipeItem(registry, MachineType.PURIFICATION_CHAMBER);
        registerRecipeItem(registry, MachineType.OSMIUM_COMPRESSOR);
        registerRecipeItem(registry, MachineType.CHEMICAL_INJECTION_CHAMBER);
        registerRecipeItem(registry, MachineType.PRECISION_SAWMILL);
        registerRecipeItem(registry, MachineType.METALLURGIC_INFUSER);
        registerRecipeItem(registry, MachineType.CHEMICAL_CRYSTALLIZER);
        registerRecipeItem(registry, MachineType.CHEMICAL_DISSOLUTION_CHAMBER);
        registerRecipeItem(registry, MachineType.CHEMICAL_INFUSER);
        registerRecipeItem(registry, MachineType.CHEMICAL_OXIDIZER);
        registerRecipeItem(registry, MachineType.CHEMICAL_WASHER);
        registerRecipeItem(registry, MachineType.SOLAR_NEUTRON_ACTIVATOR);
        registerRecipeItem(registry, MachineType.ELECTROLYTIC_SEPARATOR);
        registerRecipeItem(registry, MachineType.PRESSURIZED_REACTION_CHAMBER);
        registry.addRecipeCatalyst(MachineType.ROTARY_CONDENSENTRATOR.getStack(),
              "mekanism.rotary_condensentrator_condensentrating", "mekanism.rotary_condensentrator_decondensentrating");

        registry.addRecipeCatalyst(BasicBlockType.THERMAL_EVAPORATION_CONTROLLER.getStack(1),
              "mekanism.thermal_evaporation_plant");
        registry.addRecipeCatalyst(MachineType.FORMULAIC_ASSEMBLICATOR.getStack(), VanillaRecipeCategoryUid.CRAFTING);

        registry.getRecipeTransferRegistry()
              .addRecipeTransferHandler(ContainerFormulaicAssemblicator.class, VanillaRecipeCategoryUid.CRAFTING, 20, 9,
                    35, 36);
        registry.getRecipeTransferRegistry()
              .addRecipeTransferHandler(ContainerRobitInventory.class, VanillaRecipeCategoryUid.CRAFTING, 1, 9, 10, 36);
    }

    private void registerRecipeItem(IModRegistry registry, MachineType type) {
        String category = "mekanism." + type.getName();
        registry.addRecipeCatalyst(type.getStack(), category);
        RecipeType factoryType = null;
        for (RecipeType t : RecipeType.values()) {
            if (t.getType() == type) {
                factoryType = t;
                break;
            }
        }
        if (factoryType != null) {
            for (Tier.FactoryTier tier : Tier.FactoryTier.values()) {
                if (tier.machineType.isEnabled()) {
                    registry.addRecipeCatalyst(MekanismUtils.getFactory(tier, factoryType), category);
                }
            }
        }
    }

    private void addRecipes(IModRegistry registry, Recipe type, Class<?> recipe,
          Class<? extends IRecipeWrapper> wrapper, String recipeCategoryUid) {
        addRecipes(registry, type.get().values(), recipe, wrapper, recipeCategoryUid);
    }

    private void addRecipes(IModRegistry registry, Collection recipeList, Class<?> recipe,
          Class<? extends IRecipeWrapper> wrapper, String recipeCategoryUid) {
        List<IRecipeWrapper> recipes = new ArrayList<>();

        //add all recipes with wrapper to the list
        for (Object obj : recipeList) {
            if (obj instanceof MachineRecipe) {
                try {
                    recipes.add(wrapper.getConstructor(recipe).newInstance(obj));
                } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    Mekanism.logger.fatal("Error registering JEI recipe: " + recipe.getName(), e);
                }
            }
        }

        registry.addRecipes(recipes, recipeCategoryUid);
    }

    private void registerVanillaSmeltingRecipeCatalyst(IModRegistry registry) {
        registry.addRecipeCatalyst(MachineType.ENERGIZED_SMELTER.getStack(), VanillaRecipeCategoryUid.SMELTING);
        for (Tier.FactoryTier tier : Tier.FactoryTier.values()) {
            if (tier.machineType.isEnabled()) {
                registry.addRecipeCatalyst(MekanismUtils.getFactory(tier, RecipeType.SMELTING),
                      VanillaRecipeCategoryUid.SMELTING);
            }
        }
    }
}
