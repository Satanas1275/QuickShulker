package com.quickshulker.mixin;

import com.quickshulker.client.ShulkerHoverRenderer;
import com.quickshulker.util.ShulkerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow @Nullable protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void onExtractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ShulkerHoverRenderer.render(context, screen, hoveredSlot, mouseX, mouseY, this.leftPos, this.topPos);
        if (hoveredSlot != null && hoveredSlot.hasItem() && ShulkerUtils.isShulkerBox(hoveredSlot.getItem())) {
            ci.cancel();
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(Slot slot, int slotIndex, int button, ContainerInput containerInput, CallbackInfo ci) {
        if (slot == null || !slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        if (!stack.is(holder -> holder.is(net.minecraft.tags.ItemTags.SHULKER_BOXES))) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (slot.container != player.getInventory()) return;

        int selected = ShulkerHoverRenderer.getSelectedSlot(slot.index);

        if (button == 1) {
            ShulkerHoverRenderer.sendExtract(slot.index, selected);
            ShulkerHoverRenderer.markDirty(slot);
            ci.cancel();
        } else if (button == 0) {
            ItemStack cursor = player.containerMenu.getCarried();
            if (!cursor.isEmpty()) {
                ShulkerHoverRenderer.sendInsert(slot.index);
                ShulkerHoverRenderer.markDirty(slot);
                ci.cancel();
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerHoverRenderer.handleScroll(hoveredSlot, vertical)) {
            cir.setReturnValue(true);
        }
    }
}
