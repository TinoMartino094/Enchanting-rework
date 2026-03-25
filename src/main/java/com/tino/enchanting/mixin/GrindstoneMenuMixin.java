package com.tino.enchanting.mixin;

import com.tino.enchanting.config.ModConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Inject(method = "removeNonCursesFrom", at = @At("RETURN"))
    private void onRemoveNonCursesFrom(ItemStack item, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result != null && !result.isEmpty()) {
            CustomData customData = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (!customData.isEmpty()) {
                boolean isFree = customData.copyTag().getBoolean("enchanting_rework:free").orElse(false);
                if (isFree) {
                    CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
                        tag.remove("enchanting_rework:free");
                    });
                }
            }
        }
    }

    private boolean isEnchantedForFree(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return false;
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty())
            return false;
        return customData.copyTag().getBoolean("enchanting_rework:free").orElse(false);
    }

    private int getEnchantmentValue(ItemStack item) {
        int amount = 0;
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);

        for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchant = (Holder<Enchantment>) entry.getKey();
            int lvl = entry.getIntValue();
            if (!enchant.is(EnchantmentTags.CURSE)) {
                amount += enchant.value().getMinCost(lvl);
            }
        }

        return amount;
    }

    @Inject(method = "computeResult", at = @At("RETURN"), cancellable = true)
    private void onComputeResult(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
        // Feature: Book Splitting logic
        if (input.is(Items.ENCHANTED_BOOK) && additional.is(Items.BOOK) && additional.getCount() >= 1) {
            ItemEnchantments enchantments = input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchantments.size() >= 1) {
                // Take the LAST enchantment
                List<Entry<Holder<Enchantment>>> list = new ArrayList<>(enchantments.entrySet());
                Entry<Holder<Enchantment>> lastEntry = list.get(list.size() - 1);
                
                ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(lastEntry.getKey(), lastEntry.getIntValue());
                result.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                result.set(DataComponents.REPAIR_COST, 0); // Clean book

                // Tag inheritance for splitting (if the source book was "free", the extraction is "free" / penalty-free)
                if (isEnchantedForFree(input)) {
                    CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putBoolean("enchanting_rework:free", true));
                }

                cir.setReturnValue(result);
                return;
            }
        }

        ItemStack result = cir.getReturnValue();
        if (result != null && !result.isEmpty() && result.isDamageableItem()) {
            boolean isFree = isEnchantedForFree(input) || isEnchantedForFree(additional);
            if (isFree) {
                int totalValue = getEnchantmentValue(input) + getEnchantmentValue(additional);

                if (totalValue > 0) {
                    int maxDamage = result.getMaxDamage();
                    float basePercentage = ModConfig.grindstoneBaseDamagePercent + (totalValue * ModConfig.grindstoneValueMultiplier);

                    int damageToAdd = (int) (maxDamage * (basePercentage / 100.0f));
                    if (damageToAdd < 1)
                        damageToAdd = 1;

                    int newDamage = result.getDamageValue() + damageToAdd;
                    if (newDamage >= maxDamage) {
                        newDamage = maxDamage - 1;
                    }
                    result.setDamageValue(newDamage);
                }
            }
        }
    }
}
