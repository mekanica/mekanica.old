package mekanism.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;

public final class StackUtils {

    public static boolean diffIgnoreNull(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }

        return stack1.getItem() != stack2.getItem() || stack1.getItemDamage() != stack2.getItemDamage();
    }

    public static boolean equalsWildcard(ItemStack wild, ItemStack check) {
        if (wild.isEmpty() || check.isEmpty()) {
            return check == wild;
        }

        return wild.getItem() == check.getItem() && (wild.getItemDamage() == OreDictionary.WILDCARD_VALUE
              || check.getItemDamage() == OreDictionary.WILDCARD_VALUE || wild.getItemDamage() == check
              .getItemDamage());
    }

    public static boolean equalsWildcardWithNBT(ItemStack wild, ItemStack check) {
        boolean wildcard = equalsWildcard(wild, check);

        if (wild.isEmpty() || check.isEmpty()) {
            return wildcard;
        }

        return wildcard && (!wild.hasTagCompound() ? !check.hasTagCompound()
              : (wild.getTagCompound() == check.getTagCompound() || wild.getTagCompound()
                    .equals(check.getTagCompound())));
    }

    //could be inlined at this point probably
    public static Pair<ItemStack, ItemStack> even(ItemStack stack1, ItemStack stack2) {
        int count = stack1.getCount() + stack2.getCount();
        ItemStack stack = stack1.isEmpty() ? stack2 : stack1;
        return Pair.of(size(stack, (count + 1)/2), size(stack, count/2));
    }

    public static ItemStack subtract(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty()) {
            return ItemStack.EMPTY;
        } else if (stack2.isEmpty()) {
            return stack1;
        }

        return size(stack1, stack1.getCount() - stack2.getCount());
    }

    public static ItemStack size(ItemStack stack, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack ret = stack.copy();
        ret.setCount(size);

        return ret;
    }

    public static List<ItemStack> getMergeRejects(NonNullList<ItemStack> orig, NonNullList<ItemStack> toAdd) {
        List<ItemStack> ret = new ArrayList<>();

        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).isEmpty()) {
                ItemStack reject = getMergeReject(orig.get(i), toAdd.get(i));

                if (!reject.isEmpty()) {
                    ret.add(reject);
                }
            }
        }

        return ret;
    }

    public static void merge(NonNullList<ItemStack> orig, NonNullList<ItemStack> toAdd) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).isEmpty()) {
                orig.set(i, merge(orig.get(i), toAdd.get(i)));
            }
        }
    }

    private static ItemStack merge(ItemStack orig, ItemStack toAdd) {
        if (orig.isEmpty()) {
            return toAdd;
        }

        if (toAdd.isEmpty()) {
            return orig;
        }

        if (!orig.isItemEqual(toAdd) || !ItemStack.areItemStackTagsEqual(orig, toAdd)) {
            return orig;
        }

        return size(orig, Math.min(orig.getMaxStackSize(), orig.getCount() + toAdd.getCount()));
    }

    private static ItemStack getMergeReject(ItemStack orig, ItemStack toAdd) {
        if (orig.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (toAdd.isEmpty()) {
            return orig;
        }

        if (!orig.isItemEqual(toAdd) || !ItemStack.areItemStackTagsEqual(orig, toAdd)) {
            return orig;
        }

        int newSize = orig.getCount() + toAdd.getCount();

        if (newSize > orig.getMaxStackSize()) {
            return size(orig, newSize - orig.getMaxStackSize());
        } else {
            return size(orig, newSize);
        }
    }

    //nice hash magic
    public static int hashItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }

        int nameHash = Objects.requireNonNull(stack.getItem().getRegistryName()).hashCode();
        return nameHash << 8 | stack.getMetadata();
    }
}
