package com.quickshulker.network.payload;

import com.quickshulker.QuickShulker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ShulkerSyncPayload(int inventorySlot, ItemStack updatedShulker) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShulkerSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(QuickShulker.MOD_ID, "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShulkerSyncPayload::inventorySlot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            ShulkerSyncPayload::updatedShulker,
            ShulkerSyncPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
