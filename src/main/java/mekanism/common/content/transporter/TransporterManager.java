package mekanism.common.content.transporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.content.transporter.TransitRequest.TransitResponse;
import mekanism.common.content.transporter.TransporterStack.Path;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StackUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
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

    public static InventoryCopy copyInvFromSide(IInventory inv, EnumFacing side) {
        NonNullList<ItemStack> ret = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        if (!(inv instanceof ISidedInventory)) {
            for (int i = 0; i <= inv.getSizeInventory() - 1; i++) {
                ret.set(i, !inv.getStackInSlot(i).isEmpty() ? inv.getStackInSlot(i).copy() : ItemStack.EMPTY);
            }
        } else {
            ISidedInventory sidedInventory = (ISidedInventory) inv;
            int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

            if (slots.length == 0) {
                return null;
            }

            for (int get = 0; get <= slots.length - 1; get++) {
                int slotID = slots[get];

                if (slotID >= ret.size()) {
                    Mekanism.logger.error("Inventory {} gave slot number >= the number of slots it reported! {} >= {} ",
                          inv.getClass().getName(), slotID, ret.size());
                    continue;
                }

                ret.set(slotID,
                      !sidedInventory.getStackInSlot(slotID).isEmpty() ? sidedInventory.getStackInSlot(slotID).copy()
                            : ItemStack.EMPTY);
            }

            if (inv instanceof TileEntityBin) {
                return new InventoryCopy(ret, ((TileEntityBin) inv).getItemCount());
            } else {
                return new InventoryCopy(ret);
            }
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

            for (int i = 0; i <= inv.getSlots() - 1; i++) {
                if (stack.pathType != Path.HOME) {
                    ItemStack rejectStack = inv.insertItem(i, toInsert, true);

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
        } else if (tile instanceof ISidedInventory) {
            ISidedInventory inv = (ISidedInventory) tile;
            int[] slots = inv.getSlotsForFace(side.getOpposite());

            if (slots.length != 0) {
                if (stack.pathType != Path.HOME && inv instanceof TileEntityBin
                      && side.getOpposite() == EnumFacing.DOWN) {
                    slots = inv.getSlotsForFace(EnumFacing.UP);
                }

                if (tile instanceof TileEntityBin) {
                    int slot = slots[0];
                    if (!inv.isItemValidForSlot(slot, toInsert) || !inv
                          .canInsertItem(slot, toInsert, side.getOpposite())) {
                        return;
                    }
                    int amountRemaining = ((TileEntityBin) inv).getMaxStoredCount() - copy.binAmount;
                    copy.binAmount += Math.min(amountRemaining, toInsert.getCount());
                    return;
                } else {
                    for (int get = 0; get <= slots.length - 1; get++) {
                        int slotID = slots[get];
                        if (stack.pathType == Path.HOME || (inv.isItemValidForSlot(slotID, toInsert) && inv
                              .canInsertItem(slotID, toInsert, side.getOpposite()))) {
                            toInsert = getItemStack(copy, toInsert, slotID, inv.getInventoryStackLimit());
                            if (toInsert == null) {
                                return;
                            }
                        }
                    }
                }
            }
        } else if (tile instanceof IInventory) {
            IInventory inv = InventoryUtils.checkChestInv((IInventory) tile);

            for (int i = 0; i <= inv.getSizeInventory() - 1; i++) {
                if (stack.pathType == Path.HOME || inv.isItemValidForSlot(i, toInsert)) {
                    toInsert = getItemStack(copy, toInsert, i, inv.getInventoryStackLimit());
                    if (toInsert == null) {
                        return;
                    }
                }
            }
        }
    }

    @Nullable
    private static ItemStack getItemStack(InventoryCopy copy, ItemStack toInsert, int slotID, int inventoryStackLimit) {
        ItemStack inSlot = copy.inventory.get(slotID);

        int max;
        int rejects;
        if (inSlot.isEmpty()) {
            if (toInsert.getCount() <= (max = inventoryStackLimit)) {
                copy.inventory.set(slotID, toInsert);
                return null;
            }
            rejects = toInsert.getCount() - max;
        } else if (InventoryUtils.areItemsStackable(toInsert, inSlot) && inSlot.getCount() < (max = Math
              .min(inSlot.getMaxStackSize(), inventoryStackLimit))) {
            if (inSlot.getCount() + toInsert.getCount() <= max) {
                ItemStack toSet = toInsert.copy();
                toSet.grow(inSlot.getCount());
                copy.inventory.set(slotID, toSet);
                return null;
            }
            rejects = (inSlot.getCount() + toInsert.getCount()) - max;
        } else {
            return toInsert;
        }
        ItemStack toSet = toInsert.copy();
        toSet.setCount(max);

        ItemStack remains = toInsert.copy();
        remains.setCount(rejects);

        copy.inventory.set(slotID, toSet);
        return remains;
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
        } else if (tileEntity instanceof IInventory) {
            copy = copyInvFromSide(InventoryUtils.checkChestInv((IInventory) tileEntity), side);
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

                for (int i = 0; i <= inventory.getSlots() - 1; i++) {
                    ItemStack rejectStack = inventory.insertItem(i, toInsert, true);

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
            } else if (tileEntity instanceof ISidedInventory) {
                ISidedInventory inventory = (ISidedInventory) tileEntity;
                int[] slots = inventory.getSlotsForFace(side.getOpposite());

                if (slots.length != 0) {
                    if (tileEntity instanceof TileEntityBin) {
                        int slot = slots[0];

                        if (!inventory.isItemValidForSlot(slot, toInsert) || !inventory
                              .canInsertItem(slot, toInsert, side.getOpposite())) {
                            continue;
                        }

                        int amountRemaining = ((TileEntityBin) tileEntity).getMaxStoredCount() - copy.binAmount;
                        ItemStack ret;

                        if (toInsert.getCount() <= amountRemaining) {
                            ret = toInsert;
                        } else {
                            ret = StackUtils.size(toInsert, amountRemaining);
                        }

                        return new TransitResponse(requestEntry.getValue(), ret);
                    } else {
                        for (int get = 0; get <= slots.length - 1; get++) {
                            int slotID = slots[get];

                            if (inventory.isItemValidForSlot(slotID, toInsert) && inventory
                                  .canInsertItem(slotID, toInsert, side.getOpposite())) {
                                ItemStack stack = getItemStack(copy, toInsert, inventory, slotID);
                                if (stack == null) {
                                    return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                                }
                                toInsert = stack;
                            }
                        }

                        if (TransporterManager.didEmit(requestEntry.getKey(), toInsert)) {
                            return new TransitResponse(requestEntry.getValue(),
                                  getToUse(requestEntry.getKey(), toInsert));
                        }
                    }
                }
            } else if (tileEntity instanceof IInventory) {
                IInventory inventory = InventoryUtils.checkChestInv((IInventory) tileEntity);

                for (int i = 0; i <= inventory.getSizeInventory() - 1; i++) {
                    if (inventory.isItemValidForSlot(i, toInsert)) {
                        ItemStack stack = getItemStack(copy, toInsert, inventory, i);
                        if (stack == null) {
                            return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                        }
                        toInsert = stack;
                    }
                }

                if (TransporterManager.didEmit(requestEntry.getKey(), toInsert)) {
                    return new TransitResponse(requestEntry.getValue(), getToUse(requestEntry.getKey(), toInsert));
                }
            }
        }

        return TransitResponse.EMPTY;
    }

    @Nullable
    private static ItemStack getItemStack(InventoryCopy copy, ItemStack toInsert, IInventory inventory, int i) {
        ItemStack inSlot = copy.inventory.get(i);

        int max;
        int rejects;
        if (toInsert.isEmpty()) {
            return null;
        } else if (inSlot.isEmpty()) {
            if (toInsert.getCount() <= (max = inventory.getInventoryStackLimit())) {
                return null;
            }
            rejects = toInsert.getCount() - max;
        } else if (InventoryUtils.areItemsStackable(toInsert, inSlot) && inSlot.getCount() < (max = Math
              .min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit()))) {
            if (inSlot.getCount() + toInsert.getCount() <= max) {
                return null;
            }
            rejects = (inSlot.getCount() + toInsert.getCount()) - max;
        } else {
            return toInsert;
        }
        if (rejects < toInsert.getCount()) {
            toInsert = StackUtils.size(toInsert, rejects);
        }
        return toInsert;
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
