package cmyk.mixin;

import cmyk.config.FoodConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class PumpkinSeedsEatMixin {

    // 潜行右键开始“进食”流程
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void cmyk$startEatWhenSneak(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && "minecraft:pumpkin_seeds".equals(key.toString()) && !player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(hand)));
        }
    }

    // 使用动画改为进食
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void cmyk$useAnim(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && "minecraft:pumpkin_seeds".equals(key.toString())) {
            cir.setReturnValue(UseAnim.EAT);
        }
    }

    // 进食用时：40 tick = 2 秒
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void cmyk$useDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && "minecraft:pumpkin_seeds".equals(key.toString())) {
            cir.setReturnValue(40);
        }
    }

    // 对方块右键：非潜行改为进入进食，潜行保持原版（允许种植）
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void cmyk$useOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        ItemStack stack = ctx.getItemInHand();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !"minecraft:pumpkin_seeds".equals(key.toString())) return;
        if (player.isShiftKeyDown()) return; // 潜行时保留原版种植

        player.startUsingItem(ctx.getHand());
        cir.setReturnValue(InteractionResult.CONSUME);
    }

    // 完成进食时应用配置的饱食/饱和，并消耗物品
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void cmyk$finish(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !"minecraft:pumpkin_seeds".equals(key.toString())) return;

        String foodId = key.toString();
        FoodConfig.FoodProperty cfg = FoodConfig.getFoodProperty(foodId);
        if (cfg == null) return;

        FoodData stats = player.getFoodData();
        int newFood = stats.getFoodLevel() + Math.max(0, cfg.getHunger());
        newFood = Math.min(20, Math.max(0, newFood));
        float newSat = stats.getSaturationLevel() + Math.max(0f, cfg.getSaturation());
        newSat = Math.min(newFood, Math.max(0f, newSat));
        stats.setFoodLevel(newFood);
        stats.setSaturation(newSat);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);

        cir.setReturnValue(stack);
        cir.cancel();
    }
}
