package com.quickshulker.client;

import com.quickshulker.network.payload.ShulkerExtractPayload;
import com.quickshulker.network.payload.ShulkerInsertPayload;
import com.quickshulker.util.ShulkerUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

@Environment(EnvType.CLIENT)
public final class ShulkerHoverRenderer {
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int PADDING = 6;
    private static final int TITLE_HEIGHT = 12;

    static final Map<Integer, Integer> selectedSlots = new HashMap<>();

    private static Slot currentHoveredShulker = null;
    private static List<ItemStack> currentContents = List.of();
    private static ItemStack lastContentsStack = ItemStack.EMPTY;

    private ShulkerHoverRenderer() {}

    public static void markDirty(Slot slot) {
        if (slot == currentHoveredShulker) {
            currentHoveredShulker = null;
        }
    }

    public static void render(GuiGraphicsExtractor context, AbstractContainerScreen<?> screen, Slot hoveredSlot, int mouseX, int mouseY, int guiX, int guiY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            currentHoveredShulker = null;
            currentContents = List.of();
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!ShulkerUtils.isShulkerBox(stack)) {
            currentHoveredShulker = null;
            currentContents = List.of();
            return;
        }

        if (hoveredSlot != currentHoveredShulker || !ItemStack.isSameItemSameComponents(stack, lastContentsStack)) {
            currentHoveredShulker = hoveredSlot;
            currentContents = ShulkerUtils.getContents(stack);
            lastContentsStack = stack.copy();
            selectedSlots.putIfAbsent(hoveredSlot.index, 0);
        }

        int selected = selectedSlots.getOrDefault(hoveredSlot.index, 0);

        renderPopup(context, currentContents, mouseX, mouseY, selected, stack);
        renderSlotIndicator(context, hoveredSlot, stack, client, guiX, guiY);
    }

    private static void renderPopup(GuiGraphicsExtractor context, List<ItemStack> contents, int mouseX, int mouseY, int selected, ItemStack shulkerStack) {
        int popupWidth = COLS * SLOT_SIZE + PADDING * 2;
        int gridHeight = ROWS * SLOT_SIZE;
        int popupHeight = PADDING + TITLE_HEIGHT + 2 + gridHeight + PADDING;

        int x = mouseX + 12;
        int y = mouseY - popupHeight / 2;

        if (x + popupWidth > context.guiWidth()) {
            x = mouseX - 12 - popupWidth;
        }
        if (y < 0) y = 4;
        if (y + popupHeight > context.guiHeight()) {
            y = context.guiHeight() - popupHeight - 4;
        }

        int bgColor = 0xF0100010;
        int borderColor = 0xFF505050;

        context.fill(x, y, x + popupWidth, y + popupHeight, borderColor);
        context.fill(x + 1, y + 1, x + popupWidth - 1, y + popupHeight - 1, bgColor);

        Minecraft client = Minecraft.getInstance();
        String displayName;
        if (shulkerStack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            displayName = shulkerStack.getHoverName().getString();
        } else {
            displayName = "Shulker";
        }

        int textX = x + PADDING;
        int textY = y + PADDING;
        int maxTextWidth = popupWidth - PADDING * 2;
        displayName = client.font.plainSubstrByWidth(displayName, maxTextWidth);

        context.text(client.font, displayName, textX, textY, 0xFFAAAAAA, false);

        int gridY = y + PADDING + TITLE_HEIGHT + 2;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                int slotX = x + PADDING + col * SLOT_SIZE;
                int slotY = gridY + row * SLOT_SIZE;

                context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF8B8B8B);
                context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0xFF373737);

                if (index == selected) {
                    context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
                }

                if (index < contents.size() && !contents.get(index).isEmpty()) {
                    ItemStack item = contents.get(index);
                    context.item(item, slotX + 1, slotY + 1);
                    context.itemDecorations(client.font, item, slotX + 1, slotY + 1);
                }
            }
        }
    }

    private static void renderSlotIndicator(GuiGraphicsExtractor context, Slot slot, ItemStack shulkerStack, Minecraft client, int guiX, int guiY) {
        ItemStack heldItem = client.player.containerMenu.getCarried();
        if (heldItem.isEmpty()) return;

        ShulkerUtils.SlotStatus status = ShulkerUtils.getSlotStatus(shulkerStack, heldItem);
        int indicatorColor = switch (status) {
            case CAN_INSERT_NEW -> 0x55FFFF00;
            case STACKABLE -> 0x5500FF00;
            case FULL -> 0x55FF0000;
            case NONE -> 0;
        };

        if (indicatorColor == 0) return;

        int slotX = guiX + slot.x;
        int slotY = guiY + slot.y;

        context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, indicatorColor);
    }

    public static int getSelectedSlot(int screenSlot) {
        return selectedSlots.getOrDefault(screenSlot, 0);
    }

    public static void sendExtract(int screenSlot, int shulkerSlot) {
        ClientPlayNetworking.send(new ShulkerExtractPayload(screenSlot, shulkerSlot));
    }

    public static void sendInsert(int screenSlot) {
        ClientPlayNetworking.send(new ShulkerInsertPayload(screenSlot));
    }

    public static boolean handleScroll(Slot hoveredSlot, double vertical) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;
        if (!ShulkerUtils.isShulkerBox(hoveredSlot.getItem())) return false;

        int delta = (int) Math.signum(vertical);
        int current = selectedSlots.getOrDefault(hoveredSlot.index, 0);
        int next = (current - delta + 27) % 27;
        selectedSlots.put(hoveredSlot.index, next);
        return true;
    }
}
