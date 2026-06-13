package com.quickshulker.network.payload;

import com.quickshulker.QuickShulker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerExtractPayload(int inventorySlot, int shulkerSlot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShulkerExtractPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(QuickShulker.MOD_ID, "extract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerExtractPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShulkerExtractPayload::inventorySlot,
            ByteBufCodecs.VAR_INT,
            ShulkerExtractPayload::shulkerSlot,
            ShulkerExtractPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
