package org.mixin;

// 修复导入语句
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// 修复导入语句格式
import org.cmyk;

@Mixin(ItemStack.class)
public class ToolDurabilityMixin {

    @Inject(method = {"hurt"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void onHurt(int amount, RandomSource random, ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 获取当前ItemStack实例
            ItemStack stack = (ItemStack)(Object)this;
            
            // 检查物品是否可被破坏
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                return;
            }
            
            // 获取当前耐久值
            int currentDamage = stack.getDamageValue();
            // 获取最大耐久值
            int maxDamage = stack.getMaxDamage();
            
            // 如果当前耐久已经为0或工具已经损坏，不阻止原始行为
            if (currentDamage >= maxDamage - 1) {
                return;
            }
            
            // 获取耐久附魔等级
            int unbreakingLevel = stack.getEnchantmentLevel(Enchantments.UNBREAKING);
            
            // 检查是否是首次调用hurt方法（非连锁调用）
            if (amount == 1 && player != null) {
                // 由于无法获取当前方块和配置，直接使用默认值10
                int originalTotalDamage = 10;
    
                // 每个耐久等级减少1点消耗，但至少保持1点消耗
                int reducedDamage = Math.max(1, originalTotalDamage - unbreakingLevel);
                
                // 在消耗耐久前检查工具的剩余耐久
                int remainingDurability = maxDamage - currentDamage;
                // 确保消耗的耐久不超过剩余耐久
                int actualDamage = Math.min(reducedDamage, remainingDurability);
                
                // 直接设置损坏值，避免递归调用
                stack.setDamageValue(currentDamage + actualDamage);
                
                // 取消原始的伤害处理
                cir.setReturnValue(false);
                cir.cancel();
            }
        } catch (Exception e) {
            System.err.println("ToolDurabilityMixin error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
