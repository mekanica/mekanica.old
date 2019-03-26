package mekanism.common.content.transporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.content.transporter.TransitRequest.TransitResponse;
import mekanism.common.content.transporter.TransporterStack.Path;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;

public class TransporterManager {

    public static Map<Coord4D, Set<TransporterStack>> flowingStacks = new HashMap<>();

    public static void reset() {
        flowingStacks.clear();
    }

    public static void add(TransporterStack stack) {
        Set<TransporterStack> set = new HashSet<>();
        set.add(stack);

        if (flowingStacks.get(stack.getDest()) == null) {
            flowingStacks.put(stack.getDest(), set);
        } else {
            flowingStacks.get(stack.getDest()).addAll(set);
        }
    }

    public static void remove(TransporterStack stack) {
        if (stack.hasPath() && stack.pathType != Path.NONE) {
            flowingStacks.get(stack.getDest()).remove(stack);
        }
    }

    public static List<TransporterStack> getStacksToDest(Coord4D dest) {
        List<TransporterStack> ret = new ArrayList<>();

        if (flowingStacks.containsKey(dest)) {
            for (TransporterStack stack : flowingStacks.get(dest)) {
                if (stack != null && stack.pathType != Path.NONE && stack.hasPath()) {
                    if (stack.getDest().equals(dest)) {
                        ret.add(stack);
                    }
                }
            }
        }

        return ret;
    }

    public static InventoryCopy copyInv(IItemHandler handler) {
        NonNullList<ItemStack> ret = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);

        for (int i = 0; i < handler.getSlots(); i++) {
            ret.set(i, handler.getStackInSlot(i));
        }

        return new InventoryCopy(ret);
    }

    public static void testInsert(TileEntity tile, InventoryCopy copy, EnumFacing side, TransporterStack stack) {
        ItemStack toInsert = stack.itemStack.copy();

        if (stack.pathType != Path.HOME && tile instanceof ISideConfiguration) {
            ISideConfiguration config = (ISideConfiguration) tile;
            EnumFacing tileSide = config.getOrientation();
            EnumColor configColor = config.getEjector()
                  .getInputColor(MekanismUtils.getBaseOrientation(side, tileSide).getOpposite());

            if (config.getEjector().hasStrictInput() && configColor != null && configColor != stack.color) {
                return;
            }
        }

        if (InventoryUtils.isItemHandler(tile, side.getOpposite())) {
            IItemHandler inv = InventoryUtils.getItemHandler(tile, side.getOpposite());

            for (int i = 0; i < inv.getSlots(); i++) {
                if (stack.pathType != Path.HOME) {
                    //Validate
                    if (!inv.isItemValid(i, toInsert)) {
                        continue;
                    }

                    //Simulate insert
                    ItemStack rejectStack = inv.insertItem(i, toInsert, true);

                    //If failed to insert, skip
                    if (!TransporterManager.didEmit(toInsert, rejectStack)) {
                        continue;
                    }
                }

                ItemStack inSlot = copy.inventory.get(i);

                if (inSlot.isEmpty()) {
                    if (toInsert.getCount() <= inv.getSlotLimit(i)) {
                        copy.inventory.set(i, toInsert);
                        return;
                    } else {
                        int rejects = toInsert.getCount() - inv.getSlotLimit(i);

                        ItemStack toSet = toInsert.copy();
                        toSet.setCount(inv.getSlotLimit(i));

                        ItemStack remains = toInsert.copy();
                        remains.setCount(rejects);

                        copy.inventory.set(i, toSet);

                        toInsert = remains;
                    }
                } else if (InventoryUtils.areItemsStackable(toInsert, inSlot) && inSlot.getCount() < Math
                      .min(inSlot.getMaxStackSize(), inv.getSlotLimit(i))) {
                    int max = Math.min(inSlot.getMaxStackSize(), inv.getSlotLimit(i));

                    if (inSlot.getCount() + toInsert.getCount() <= max) {
                        ItemStack toSet = toInsert.copy();
                        toSet.grow(inSlot.getCount());

                        copy.inventory.set(i, toSet);
                        return;
                    } else {
                        int rejects = (inSlot.getCount() + toInsert.getCount()) - max;

                        ItemStack toSet = toInsert.copy();
                        toSet.setCount(max);

                        ItemStack remains = toInsert.copy();
                        remains.setCount(rejects);

                        copy.inventory.set(i, toSet);

                        toInsert = remains;
                    }
                }
            }
        }
    }

    public static boolean didEmit(ItemStack stack, ItemStack returned) {
        return returned.isEmpty() || returned.getCount() < stack.getCount();
    }

    public static ItemStack getToUse(ItemStack stack, ItemStack returned) {
        if (returned.isEmpty() || returned.getCount() == 0) {
            return stack;
        }

        return MekanismUtils.size(stack, stack.getCount() - returned.getCount());
    }

    public static ItemStack getToUse(ItemStack stack, int rejected) {
        return MekanismUtils.size(stack, stack.getCount() - rejected);
    }

    /**
     * @return TransitResponse of expected items to use
     */
    public static TransitResponse getPredictedInsert(TileEntity tileEntity, EnumColor color, TransitRequest request,
          EnumFacing side) {
        if (tileEntity instanceof ISideConfiguration) {
            ISideConfiguration config = (ISideConfiguration) tileEntity;
            EnumFacing tileSide = config.getOrientation();
            EnumColor configColor = config.getEjector()
                  .getInputColor(MekanismUtils.getBaseOrientation(side, tileSide).getOpposite());

            if (config.getEjector().hasStrictInput() && configColor != null && configColor != color) {
                return TransitResponse.EMPTY;
            }
        }

        InventoryCopy copy = null;

        if (InventoryUtils.isItemHandler(tileEntity, side.getOpposite())) {
            copy = copyInv(InventoryUtils.getItemHandler(tileEntity, side.getOpposite()));
        }

        if (copy == null) {
            return TransitResponse.EMPTY;
        }

        List<TransporterStack> insertQueue = getStacksToDest(Coord4D.get(tileEntity));

        for (TransporterStack tStack : insertQueue) {
            testInsert(tileEntity, copy, side, tStack);
        }

        for (Map.Entry<ItemStack, Integer> requestEntry : request.itemMap.entrySet()) {
            ItemStack toInsert = requestEntry.getKey().copy();

            if (InventoryUtils.isItemHandler(tileEntity, side.getOpposite())) {
                IItemHandler inventory = InventoryUtils.getItemHandler(tileEntity, side.getOpposite());

                for (int i = 0; i < inventory.getSlots(); i++) {
                    //Validate
                    if (!inventory.isItemValid(i, toInsert)) {
                        continue;
                    }

                    //Simulate insert
                    ItemStack rejectStack = inventory.insertItem(i, toInsert, true);

                    //If didn't insert, skip
                    if (!TransporterManager.didEmit(toInsert, rejectStack)) {
                        continue;
                    }

                    ItemStack inSlot = copy.inventory.get(i);

                    if (rejectStack.isEmpty()) {
                        return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                    } else if (inSlot.isEmpty()) {
                        if (toInsert.getCount() <= inventory.getSlotLimit(i)) {
                            return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                        } else {
                            int rejects = toInsert.getCount() - inventory.getSlotLimit(i);

                            if (rejects < toInsert.getCount()) {
                                toInsert = StackUtils.size(toInsert, rejects);
                            }
                        }
                    } else if (InventoryUtils.areItemsStackable(toInsert, inSlot) && inSlot.getCount() < Math
                          .min(inSlot.getMaxStackSize(), inventory.getSlotLimit(i))) {
                        int max = Math.min(inSlot.getMaxStackSize(), inventory.getSlotLimit(i));

                        if (inSlot.getCount() + toInsert.getCount() <= max) {
                            return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                        } else {
                            int rejects = (inSlot.getCount() + toInsert.getCount()) - max;

                            if (rejects < toInsert.getCount()) {
                                toInsert = StackUtils.size(toInsert, rejects);
                            }
                        }
                    }
                }

                if (TransporterManager.didEmit(requestEntry.getKey(), toInsert)) {
                    return new TransitResponse(requestEntry.getValue(), getToUse(requestEntry.getKey(), toInsert));
                }
            }
        }

        return TransitResponse.EMPTY;
    }

    public static class InventoryCopy {

        public NonNullList<ItemStack> inventory;

        public int binAmount;

        public InventoryCopy(NonNullList<ItemStack> inv) {
            inventory = inv;
        }

        public InventoryCopy(NonNullList<ItemStack> inv, int amount) {
            this(inv);
            binAmount = amount;
        }
    }
}
