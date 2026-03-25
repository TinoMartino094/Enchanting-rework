package com.tino.enchanting.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @org.spongepowered.asm.mixin.Shadow
    private net.minecraft.world.inventory.DataSlot cost;
    @org.spongepowered.asm.mixin.Shadow
    @org.jspecify.annotations.Nullable
    private String itemName;

    private boolean isEnchantedForFree(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return false;
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty())
            return false;
        return customData.copyTag().getBoolean("enchanting_rework:free").orElse(false);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void modifyAnvilRepairCost(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack input1 = self.getSlot(0).getItem();
        ItemStack input2 = self.getSlot(1).getItem();
        ItemStack result = self.getSlot(2).getItem();

        if (result != null && !result.isEmpty()) {
            // 1. Tag Inheritance
            if (isEnchantedForFree(input1) || isEnchantedForFree(input2)) {
                CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
                    tag.putBoolean("enchanting_rework:free", true);
                });
            }

            // 2. Free Repair Logic
            // Check if it's a repair or a combine operation
            if (result.isDamageableItem() && (input1.isValidRepairItem(input2) || input1.is(input2.getItem()))) {
                // Set cost to 1 (consistent and avoids 'Too Expensive' bugs at high levels)
                this.cost.set(1);

                // Remove the repair penalty increase
                int originalPenalty = input1.getOrDefault(DataComponents.REPAIR_COST, 0);
                result.set(DataComponents.REPAIR_COST, originalPenalty);
            }
        }
    }
}
