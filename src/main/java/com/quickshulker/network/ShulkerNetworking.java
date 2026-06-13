package com.quickshulker.network;

import com.quickshulker.network.payload.ShulkerExtractPayload;
import com.quickshulker.network.payload.ShulkerInsertPayload;
import com.quickshulker.network.payload.ShulkerSyncPayload;
import com.quickshulker.util.ShulkerUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

public final class ShulkerNetworking {

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ShulkerExtractPayload.TYPE, ShulkerExtractPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShulkerInsertPayload.TYPE, ShulkerInsertPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShulkerSyncPayload.TYPE, ShulkerSyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ShulkerExtractPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                handleExtract(player, payload.inventorySlot(), payload.shulkerSlot());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ShulkerInsertPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                handleInsert(player, payload.inventorySlot());
            });
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ShulkerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null) return;
                AbstractContainerMenu handler = context.client().player.containerMenu;
                if (handler == null) return;
                int slotIndex = payload.inventorySlot();
                if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                    handler.getSlot(slotIndex).set(payload.updatedShulker());
                }
            });
        });
    }

    private static void handleExtract(ServerPlayer player, int screenSlot, int shulkerSlot) {
        if (shulkerSlot < 0 || shulkerSlot >= 27) return;

        GameType gameMode = player.gameMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.CREATIVE) return;

        AbstractContainerMenu handler = player.containerMenu;
        if (handler == null) return;

        Slot slot = handler.getSlot(screenSlot);
        if (slot == null || !slot.hasItem()) return;
        if (slot.container != player.getInventory()) return;

        ItemStack shulkerStack = slot.getItem();
        if (!ShulkerUtils.isShulkerBox(shulkerStack)) return;

        List<ItemStack> contents = ShulkerUtils.getContents(shulkerStack);
        if (shulkerSlot >= contents.size()) return;

        ItemStack toExtract = contents.get(shulkerSlot);
        if (toExtract.isEmpty()) return;

        ItemStack extracted = toExtract.copy();

        if (!hasInventorySpace(player.getInventory(), extracted)) {
            player.sendSystemMessage(Component.translatable("container.isFull"));
            return;
        }

        player.getInventory().add(extracted);

        contents.set(shulkerSlot, ItemStack.EMPTY);
        ShulkerUtils.setContents(shulkerStack, contents);
        slot.set(shulkerStack);

        ServerPlayNetworking.send(player, new ShulkerSyncPayload(screenSlot, shulkerStack.copy()));
        syncCursor(player, handler);
    }

    private static void handleInsert(ServerPlayer player, int screenSlot) {
        GameType gameMode = player.gameMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.CREATIVE) return;

        AbstractContainerMenu handler = player.containerMenu;
        if (handler == null) return;

        Slot slot = handler.getSlot(screenSlot);
        if (slot == null || !slot.hasItem()) return;
        if (slot.container != player.getInventory()) return;

        ItemStack shulkerStack = slot.getItem();
        if (!ShulkerUtils.isShulkerBox(shulkerStack)) return;

        ItemStack cursorStack = handler.getCarried();
        if (cursorStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("block.minecraft.air"));
            return;
        }

        List<ItemStack> contents = ShulkerUtils.getContents(shulkerStack);
        int remaining = cursorStack.getCount();

        for (int i = 0; i < 27 && remaining > 0; i++) {
            ItemStack existing = contents.get(i);
            if (existing.isEmpty()) {
                int toAdd = Math.min(remaining, cursorStack.getMaxStackSize());
                ItemStack newStack = cursorStack.copy();
                newStack.setCount(toAdd);
                contents.set(i, newStack);
                remaining -= toAdd;
            } else if (ShulkerUtils.canCombine(existing, cursorStack) && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(remaining, space);
                existing.grow(toAdd);
                remaining -= toAdd;
            }
        }

        if (remaining == cursorStack.getCount()) {
            player.sendSystemMessage(Component.translatable("container.isFull"));
            return;
        }

        cursorStack.shrink(cursorStack.getCount() - remaining);
        if (cursorStack.isEmpty()) {
            handler.setCarried(ItemStack.EMPTY);
        }

        ShulkerUtils.setContents(shulkerStack, contents);
        slot.set(shulkerStack);

        ServerPlayNetworking.send(player, new ShulkerSyncPayload(screenSlot, shulkerStack.copy()));
        syncCursor(player, handler);
    }

    private static void syncCursor(ServerPlayer player, AbstractContainerMenu handler) {
        player.connection.send(new ClientboundSetCursorItemPacket(handler.getCarried()));
    }

    private static boolean hasInventorySpace(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slotStack, stack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
}
