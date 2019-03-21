package mekanism.common.util;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.EnumColor;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.content.transporter.InvStack;
import mekanism.common.content.transporter.TransitRequest;
import mekanism.common.content.transporter.TransitRequest.TransitResponse;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityLogisticalSorter;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public final class InventoryUtils {

    public static final int[] EMPTY = new int[]{};

    public static int[] getIntRange(int start, int end) {
        int[] ret = new int[1 + end - start];

        for (int i = start; i <= end; i++) {
            ret[i - start] = i;
        }

        return ret;
    }

    public static IInventory checkChestInv(IInventory inv) {
        if (inv instanceof TileEntityChest) {
            TileEntityChest main = (TileEntityChest) inv;
            TileEntityChest adj = null;

            if (main.adjacentChestXNeg != null) {
                adj = main.adjacentChestXNeg;
            } else if (main.adjacentChestXPos != null) {
                adj = main.adjacentChestXPos;
            } else if (main.adjacentChestZNeg != null) {
                adj = main.adjacentChestZNeg;
            } else if (main.adjacentChestZPos != null) {
                adj = main.adjacentChestZPos;
            }

            if (adj != null) {
                return new InventoryLargeChest("", main, adj);
            }
        }

        return inv;
    }

    public static TransitResponse putStackInInventory(TileEntity tile, TransitRequest request, EnumFacing side,
          boolean force) {
        if (force && tile instanceof TileEntityLogisticalSorter) {
            return ((TileEntityLogisticalSorter) tile).sendHome(request.getSingleStack());
        }

        for (Map.Entry<ItemStack, Integer> requestEntry : request.itemMap.entrySet()) {
            ItemStack toInsert = requestEntry.getKey().copy();

            //prioritize other implementations first to allow item forcing
            if (isItemHandler(tile, side.getOpposite())) {
                IItemHandler inventory = getItemHandler(tile, side.getOpposite());

                for (int i = 0; i < inventory.getSlots(); i++) {
                    toInsert = inventory.insertItem(i, toInsert, false);
                    if (toInsert.isEmpty()) {
                        return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                    }
                }
            } else if (tile instanceof ISidedInventory) {
                ISidedInventory inventory = (ISidedInventory) tile;
                int[] slots = inventory.getSlotsForFace(side.getOpposite());

                if (slots.length != 0) {
                    if (force && inventory instanceof TileEntityBin && side == EnumFacing.UP) {
                        slots = inventory.getSlotsForFace(EnumFacing.UP);
                    }

                    for (int get = 0; get <= slots.length - 1; get++) {
                        int slotID = slots[get];
                        if (force || (inventory.isItemValidForSlot(slotID, toInsert) && inventory
                              .canInsertItem(slotID, toInsert, side.getOpposite()))) {
                            ItemStack stack = insertItem(inventory, toInsert, slotID);
                            if (stack == null) {
                                return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                            }
                            toInsert = stack;
                        }
                    }
                }
            } else if (tile instanceof IInventory) {
                IInventory inventory = checkChestInv((IInventory) tile);

                for (int i = 0; i <= inventory.getSizeInventory() - 1; i++) {
                    if (force || inventory.isItemValidForSlot(i, toInsert)) {
                        ItemStack stack = insertItem(inventory, toInsert, i);
                        if (stack == null) {
                            return new TransitResponse(requestEntry.getValue(), requestEntry.getKey());
                        }
                        toInsert = stack;
                    }
                }
            }

            if (TransporterManager.didEmit(requestEntry.getKey(), toInsert)) {
                return new TransitResponse(requestEntry.getValue(),
                      TransporterManager.getToUse(requestEntry.getKey(), toInsert));
            }
        }

        return TransitResponse.EMPTY;
    }

    @Nullable
    private static ItemStack insertItem(IInventory inventory, @Nonnull ItemStack toInsert, int slotID) {
        if (toInsert.isEmpty()) {
            return toInsert;
        }

        ItemStack inSlot = inventory.getStackInSlot(slotID);
        int max;
        int rejects;

        if (inSlot.isEmpty()) {
            if (toInsert.getCount() <= (max = inventory.getInventoryStackLimit())) {
                inventory.setInventorySlotContents(slotID, toInsert);
                inventory.markDirty();
                return null;
            }
            rejects = toInsert.getCount() - max;
        } else if (areItemsStackable(toInsert, inSlot) && inSlot.getCount() < (max = Math
              .min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit()))) {

            if (inSlot.getCount() + toInsert.getCount() <= max) {
                ItemStack toSet = toInsert.copy();
                toSet.grow(inSlot.getCount());
                inventory.setInventorySlotContents(slotID, toSet);
                inventory.markDirty();
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

        inventory.setInventorySlotContents(slotID, toSet);
        inventory.markDirty();
        return remains;
    }

    public static boolean areItemsStackable(ItemStack toInsert, ItemStack inSlot) {
        if (toInsert.isEmpty() || inSlot.isEmpty()) {
            return true;
        }

        return inSlot.isItemEqual(toInsert) && ItemStack.areItemStackTagsEqual(inSlot, toInsert);
    }

    public static InvStack takeDefinedItem(TileEntity tile, EnumFacing side, ItemStack type, int min, int max) {
        InvStack ret = new InvStack(tile, side.getOpposite());

        if (isItemHandler(tile, side.getOpposite())) {
            IItemHandler inventory = getItemHandler(tile, side.getOpposite());
            for (int i = inventory.getSlots() - 1; i >= 0; i--) {
                if (takeItemHandler(type, max, ret, inventory, i)) {
                    return ret;
                }
            }
        } else if (tile instanceof ISidedInventory) {
            ISidedInventory sidedInventory = (ISidedInventory) tile;
            int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());
            if (slots.length != 0) {
                for (int get = slots.length - 1; get >= 0; get--) {
                    if (takeItemSidedInv(side, type, max, ret, sidedInventory, slots, get)) {
                        return ret;
                    }
                }
            }
        } else if (tile instanceof IInventory) {
            IInventory inventory = checkChestInv((IInventory) tile);
            for (int i = inventory.getSizeInventory() - 1; i >= 0; i--) {
                if (takeItemInv(type, max, ret, inventory, i)) {
                    return ret;
                }
            }
        }

        if (!ret.getStack().isEmpty() && ret.getStack().getCount() >= min) {
            return ret;
        }

        return null;
    }

    public static boolean takeItemInv(ItemStack type, int max, InvStack ret, IInventory inventory, int i) {
        if (!inventory.getStackInSlot(i).isEmpty() && StackUtils.equalsWildcard(inventory.getStackInSlot(i), type)) {
            ItemStack stack = inventory.getStackInSlot(i);
            return appendStack(max, ret, i, stack, !ret.getStack().isEmpty() ? ret.getStack().getCount() : 0);
        }
        return false;
    }

    private static boolean appendStack(int max, InvStack ret, int i, ItemStack stack, int current) {
        if (current + stack.getCount() <= max) {
            ret.appendStack(i, stack.copy());
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(max - current);
            ret.appendStack(i, copy);
        }
        return !ret.getStack().isEmpty() && ret.getStack().getCount() == max;
    }

    public static boolean takeItemSidedInv(EnumFacing side, ItemStack type, int max, InvStack ret,
          ISidedInventory sidedInventory, int[] slots, int get) {
        int slotID = slots[get];
        if (!sidedInventory.getStackInSlot(slotID).isEmpty() && StackUtils
              .equalsWildcard(sidedInventory.getStackInSlot(slotID), type)) {
            ItemStack stack = sidedInventory.getStackInSlot(slotID);
            int current = !ret.getStack().isEmpty() ? ret.getStack().getCount() : 0;
            if (current + stack.getCount() <= max) {
                ItemStack copy = stack.copy();
                if (sidedInventory.canExtractItem(slotID, copy, side.getOpposite())) {
                    ret.appendStack(slotID, copy);
                }
            } else {
                ItemStack copy = stack.copy();
                if (sidedInventory.canExtractItem(slotID, copy, side.getOpposite())) {
                    copy.setCount(max - current);
                    ret.appendStack(slotID, copy);
                }
            }
            return !ret.getStack().isEmpty() && ret.getStack().getCount() == max;
        }
        return false;
    }

    public static boolean takeItemHandler(ItemStack type, int max, InvStack ret, IItemHandler inventory, int i) {
        ItemStack stack = inventory.extractItem(i, max, true);
        if (!stack.isEmpty() && StackUtils.equalsWildcard(stack, type)) {
            return appendStack(max, ret, i, stack, !ret.getStack().isEmpty() ? ret.getStack().getCount() : 0);
        }
        return false;
    }

    public static boolean canInsert(TileEntity tileEntity, EnumColor color, ItemStack itemStack, EnumFacing side,
          boolean force) {
        if (force && tileEntity instanceof TileEntityLogisticalSorter) {
            return ((TileEntityLogisticalSorter) tileEntity).canSendHome(itemStack);
        }

        if (!force && tileEntity instanceof ISideConfiguration) {
            ISideConfiguration config = (ISideConfiguration) tileEntity;
            EnumFacing tileSide = config.getOrientation();
            EnumColor configColor = config.getEjector()
                  .getInputColor(MekanismUtils.getBaseOrientation(side, tileSide).getOpposite());

            if (config.getEjector().hasStrictInput() && configColor != null && configColor != color) {
                return false;
            }
        }

        if (isItemHandler(tileEntity, side.getOpposite())) {
            IItemHandler inventory = getItemHandler(tileEntity, side.getOpposite());

            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack rejects = inventory.insertItem(i, itemStack, true);
                if (TransporterManager.didEmit(itemStack, rejects)) {
                    return true;
                }
            }
        } else if (tileEntity instanceof ISidedInventory) {
            ISidedInventory inventory = (ISidedInventory) tileEntity;
            int[] slots = inventory.getSlotsForFace(side.getOpposite());

            if (slots.length != 0) {
                if (force && inventory instanceof TileEntityBin && side == EnumFacing.UP) {
                    slots = inventory.getSlotsForFace(EnumFacing.UP);
                }
                for (int get = 0; get <= slots.length - 1; get++) {
                    int slotID = slots[get];
                    if ((force || inventory.isItemValidForSlot(slotID, itemStack)) && inventory
                          .canInsertItem(slotID, itemStack, side.getOpposite())) {
                        ItemStack inSlot = inventory.getStackInSlot(slotID);
                        if (canInsertInv(itemStack, inventory, inSlot)) {
                            return true;
                        }
                    }
                }
            }
        } else if (tileEntity instanceof IInventory) {
            IInventory inventory = checkChestInv((IInventory) tileEntity);
            for (int i = 0; i <= inventory.getSizeInventory() - 1; i++) {
                if (force || inventory.isItemValidForSlot(i, itemStack)) {
                    ItemStack inSlot = inventory.getStackInSlot(i);
                    if (canInsertInv(itemStack, inventory, inSlot)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean canInsertInv(ItemStack itemStack, IInventory inventory, ItemStack inSlot) {
        if (inSlot.isEmpty()) {
            if (itemStack.getCount() <= inventory.getInventoryStackLimit()) {
                return true;
            }
            return itemStack.getCount() - inventory.getInventoryStackLimit() < itemStack.getCount();
        } else if (areItemsStackable(itemStack, inSlot) && inSlot.getCount() < Math
              .min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit())) {
            int max = Math.min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit());
            if (inSlot.getCount() + itemStack.getCount() <= max) {
                return true;
            }
            return (inSlot.getCount() + itemStack.getCount()) - max < itemStack.getCount();
        }
        return false;
    }

    public static ItemStack loadFromNBT(NBTTagCompound nbtTags) {
        return new ItemStack(nbtTags);
    }

    public static boolean isItemHandler(TileEntity tile, EnumFacing side) {
        return CapabilityUtils.hasCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    }

    public static IItemHandler getItemHandler(TileEntity tile, EnumFacing side) {
        return CapabilityUtils.getCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    }

    /*TODO From CCLib -- go back to that version when we're using dependencies again*/
    public static boolean canStack(ItemStack stack1, ItemStack stack2) {
        return stack1.isEmpty() || stack2.isEmpty() ||
              (stack1.getItem() == stack2.getItem() &&
                    (!stack2.getHasSubtypes() || stack2.getItemDamage() == stack1.getItemDamage()) &&
                    ItemStack.areItemStackTagsEqual(stack2, stack1)) &&
                    stack1.isStackable();
    }
}
