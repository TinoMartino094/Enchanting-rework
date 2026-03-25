package com.tino.enchanting.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {

    @Final
    @Shadow
    private GrindstoneMenu this$0;

    @Inject(method = "getExperienceFromItem", at = @At("HEAD"), cancellable = true)
    private void onGetExperienceFromItem(ItemStack item, CallbackInfoReturnable<Integer> cir) {
        if (item != null && !item.isEmpty()) {
            CustomData customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (!customData.isEmpty() && customData.copyTag().getBoolean("enchanting_rework:free").orElse(false)) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void onTake(Player player, ItemStack result, CallbackInfo ci) {
        Container repairSlots = ((GrindstoneMenuAccessor) this$0).getRepairSlots();
        ItemStack input = repairSlots.getItem(0);
        ItemStack additional = repairSlots.getItem(1);

        // Check if this is a Book Split operation
        if (input.is(Items.ENCHANTED_BOOK) && additional.is(Items.BOOK) && additional.getCount() >= 1) {
            ItemEnchantments enchantments = input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchantments.isEmpty()) {
                List<Entry<Holder<Enchantment>>> list = new ArrayList<>(enchantments.entrySet());
                
                if (list.size() > 1) {
                    // Scenario: Multi-enchant. Move remainder to TOP for convenience.
                    ItemStack remainderBook = new ItemStack(Items.ENCHANTED_BOOK);
                    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    for (int i = 0; i < list.size() - 1; i++) {
                        mutable.set(list.get(i).getKey(), list.get(i).getIntValue());
                    }
                    remainderBook.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                    remainderBook.set(DataComponents.REPAIR_COST, 0); 

                    if (isEnchantedForFree(input)) {
                       CustomData.update(DataComponents.CUSTOM_DATA, remainderBook, tag -> tag.putBoolean("enchanting_rework:free", true));
                    }

                    repairSlots.setItem(0, remainderBook);
                    
                    // Consume one book from the bottom
                    ItemStack newAdditional = additional.copy();
                    newAdditional.shrink(1);
                    repairSlots.setItem(1, newAdditional);
                } else {
                    // Scenario: Single-enchant. The original book is gone.
                    repairSlots.setItem(0, ItemStack.EMPTY);
                    
                    // Consume one book from the bottom
                    ItemStack newAdditional = additional.copy();
                    newAdditional.shrink(1);
                    repairSlots.setItem(1, newAdditional);
                }

                // Finalize and Sync
                this$0.broadcastChanges();
                if (player instanceof net.minecraft.server.level.ServerPlayer) {
                    this$0.sendAllDataToRemote();
                }
                
                ci.cancel();
            }
        }
    }

    private boolean isEnchantedForFree(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) return false;
        return customData.copyTag().getBoolean("enchanting_rework:free").orElse(false);
    }
}
