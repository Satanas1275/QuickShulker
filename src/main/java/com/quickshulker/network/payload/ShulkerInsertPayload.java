package com.quickshulker.network.payload;

import com.quickshulker.QuickShulker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerInsertPayload(int inventorySlot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShulkerInsertPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(QuickShulker.MOD_ID, "insert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerInsertPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShulkerInsertPayload::inventorySlot,
            ShulkerInsertPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
