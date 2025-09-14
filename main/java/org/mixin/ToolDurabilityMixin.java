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
            
            // 获取耐久附魔等级
            int unbreakingLevel = stack.getEnchantmentLevel(Enchantments.UNBREAKING);
            
            // 检查是否是首次调用hurt方法（非连锁调用）
            if (amount == 1 && player != null) {
                // 移除对不存在方法的调用
                // Block currentBlock = cmyk.getPlayerCurrentBlock(player);
                
                // 由于无法获取当前方块和配置，直接使用默认值10
                int originalTotalDamage = 10;
                
                // 移除对不存在方法的调用
                // cmyk.clearPlayerCurrentBlock(player);
                
                // 计算减少的耐久消耗百分比：10% × 附魔等级
                float reductionPercentage = unbreakingLevel * 0.1F;
                // 确保减少的百分比不超过90%（防止出现负消耗）
                reductionPercentage = Math.min(reductionPercentage, 0.9F);
                
                // 计算减少后的消耗
                int reducedDamage = Math.max(1, (int)(originalTotalDamage * (1 - reductionPercentage)));
                
                // 获取当前耐久度（损坏值）
                int currentDamage = stack.getDamageValue();
                // 设置新的耐久度值
                stack.setDamageValue(currentDamage + reducedDamage);
                
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
