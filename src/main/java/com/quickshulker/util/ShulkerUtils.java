package com.quickshulker.util;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class ShulkerUtils {
    private static final int SHULKER_SIZE = 27;

    public static boolean isShulkerBox(ItemStack stack) {
        return stack.is(holder -> holder.is(ItemTags.SHULKER_BOXES));
    }

    public static List<ItemStack> getContents(ItemStack shulker) {
        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SIZE, ItemStack.EMPTY);
        ItemContainerContents container = shulker.get(DataComponents.CONTAINER);
        if (container != null) {
            container.copyInto(items);
        }
        List<ItemStack> contents = new ArrayList<>(SHULKER_SIZE);
        for (int i = 0; i < SHULKER_SIZE; i++) {
            contents.add(items.get(i).copy());
        }
        return contents;
    }

    public static ItemStack setContents(ItemStack shulker, List<ItemStack> contents) {
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return shulker;
    }

    public static SlotStatus getSlotStatus(ItemStack shulker, ItemStack heldItem) {
        if (heldItem.isEmpty()) return SlotStatus.NONE;

        ItemContainerContents container = shulker.get(DataComponents.CONTAINER);
        if (container == null) return SlotStatus.CAN_INSERT_NEW;

        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SIZE, ItemStack.EMPTY);
        container.copyInto(items);

        boolean hasEmpty = false;
        boolean canStack = false;

        for (ItemStack existing : items) {
            if (existing.isEmpty()) {
                hasEmpty = true;
            } else if (canCombine(existing, heldItem) && existing.getCount() < existing.getMaxStackSize()) {
                canStack = true;
            }
        }

        if (canStack) return SlotStatus.STACKABLE;
        if (hasEmpty) return SlotStatus.CAN_INSERT_NEW;
        return SlotStatus.FULL;
    }

    public static boolean canCombine(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public enum SlotStatus {
        NONE,
        CAN_INSERT_NEW,
        STACKABLE,
        FULL
    }
}
