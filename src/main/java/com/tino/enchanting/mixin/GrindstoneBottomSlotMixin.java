package com.tino.enchanting.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$3")
public abstract class GrindstoneBottomSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void onMayPlace(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.is(Items.BOOK)) {
            cir.setReturnValue(true);
        }
    }
}
