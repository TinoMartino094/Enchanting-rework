package com.tino.enchanting.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "onEnchantmentPerformed", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyEnchantmentCost(int cost, ItemStack itemStack) {
        if (!itemStack.is(Items.BOOK)) {
            return 0; // Free for everything except books
        }
        return cost;
    }

    @Inject(method = "onEnchantmentPerformed", at = @At("HEAD"))
    private void tagFreeEnchantedItem(ItemStack itemStack, int cost, CallbackInfo ci) {
        if (!itemStack.is(Items.BOOK)) {
            CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> {
                tag.putBoolean("enchanting_rework:free", true);
            });
        }
    }
}
