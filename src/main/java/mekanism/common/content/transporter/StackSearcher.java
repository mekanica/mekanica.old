package mekanism.common.content.transporter;

import mekanism.common.util.InventoryUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

public class StackSearcher {

    public int i = -1;
    public int[] slots;
    public TileEntity tileEntity;
    public EnumFacing side;

    public StackSearcher(TileEntity tile, EnumFacing direction) {
        tileEntity = tile;
        side = direction;

        if (InventoryUtils.isItemHandler(tile, direction.getOpposite())) {
            i = InventoryUtils.getItemHandler(tile, direction.getOpposite()).getSlots();
        } else if (tile instanceof ISidedInventory) {
            slots = ((ISidedInventory) tile).getSlotsForFace(side.getOpposite());
            i = slots.length;
        } else if (tile instanceof IInventory) {
            i = ((IInventory) tile).getSizeInventory();
        }
    }

    public InvStack takeTopStack(Finder id) {
        if (InventoryUtils.isItemHandler(tileEntity, side.getOpposite())) {
            IItemHandler inventory = InventoryUtils.getItemHandler(tileEntity, side.getOpposite());

            for (i = i - 1; i >= 0; i--) {
                ItemStack stack = inventory.extractItem(i, 64, true);

                if (!stack.isEmpty() && id.modifies(stack)) {
                    return new InvStack(tileEntity, i, stack, side.getOpposite());
                }
            }
        } else if (tileEntity instanceof ISidedInventory) {
            ISidedInventory inventory = (ISidedInventory) tileEntity;

            if (slots != null && slots.length != 0) {
                for (i = i - 1; i >= 0; i--) {
                    int slotID = slots[i];

                    if (!inventory.getStackInSlot(slotID).isEmpty() && id.modifies(inventory.getStackInSlot(slotID))) {
                        ItemStack toSend = inventory.getStackInSlot(slotID);

                        if (inventory.canExtractItem(slotID, toSend, side.getOpposite())) {
                            return new InvStack(tileEntity, slotID, toSend, side.getOpposite());
                        }
                    }
                }
            }
        } else if (tileEntity instanceof IInventory) {
            IInventory inventory = InventoryUtils.checkChestInv((IInventory) tileEntity);

            for (i = i - 1; i >= 0; i--) {
                if (!inventory.getStackInSlot(i).isEmpty() && id.modifies(inventory.getStackInSlot(i))) {
                    ItemStack toSend = inventory.getStackInSlot(i).copy();
                    return new InvStack(tileEntity, i, toSend, side.getOpposite());
                }
            }
        }

        return null;
    }

    public InvStack takeDefinedItem(ItemStack type, int min, int max) {
        InvStack ret = new InvStack(tileEntity, side.getOpposite());

        if (InventoryUtils.isItemHandler(tileEntity, side.getOpposite())) {
            IItemHandler inventory = InventoryUtils.getItemHandler(tileEntity, side.getOpposite());

            for (i = i - 1; i >= 0; i--) {
                if (InventoryUtils.takeItemHandler(type, max, ret, inventory, i)) {
                    return ret;
                }
            }
        } else if (tileEntity instanceof ISidedInventory) {
            ISidedInventory sidedInventory = (ISidedInventory) tileEntity;
            int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

            if (slots.length != 0) {
                for (i = i - 1; i >= 0; i--) {
                    if (InventoryUtils.takeItemSidedInv(side, type, max, ret, sidedInventory, slots, i)) {
                        return ret;
                    }
                }
            }
        } else if (tileEntity instanceof IInventory) {
            IInventory inventory = InventoryUtils.checkChestInv((IInventory) tileEntity);

            for (i = i - 1; i >= 0; i--) {
                if (InventoryUtils.takeItemInv(type, max, ret, inventory, i)) {
                    return ret;
                }
            }
        }

        if (!ret.getStack().isEmpty() && ret.getStack().getCount() >= min) {
            return ret;
        }

        return null;
    }
}
