package org.cmyk.durability_overhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.cmyk.util.BlockTracker;
import org.cmyk.config.BlockDurabilityConfig;
import org.cmyk.foods.FoodConfig;
import org.cmyk.config.CommonConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("cmyk")
public class CMYKDurabilityOverhaul {
    public static final String MODID = "cmyk";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public CMYKDurabilityOverhaul() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
    
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // 只在构造函数中加载配置一次
        BlockDurabilityConfig.loadConfig();

        // Register Forge COMMON config (generates toml in config directory)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("CMYK Durability Overhaul mod initialized");
        FoodConfig.loadConfig();
    }
    
    // 检查工具是否在黑名单中
    public static boolean isToolBlacklisted(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        // 使用配置文件中的黑名单检查功能
        String itemName = stack.getItem().toString();
        return BlockDurabilityConfig.isToolBlacklisted(itemName);
    }

    // 实现让玩家破坏方块时额外消耗9点工具耐久的功能，增加方块黑名单（没有硬度的方块不消耗耐久）
    // 1. 移除冗余的工具判断条件
    @SubscribeEvent
    public void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        // 获取玩家
        Player player = event.getEntity();
        
        // 检查方块是否有硬度
        Level level = player.level();
        Optional<BlockPos> optionalPos = event.getPosition();
        
        if (!optionalPos.isPresent()) {
            return;
        }
        
        BlockPos pos = optionalPos.get();
        // 获取被破坏的方块状态
        BlockState blockState = level.getBlockState(pos);
        Block brokenBlock = blockState.getBlock();
        
        float blockHardness = brokenBlock.defaultBlockState().getDestroySpeed(level, pos);
        
        if (blockHardness <= 0) return;
        
        // 获取玩家手持物品
        ItemStack heldItem = player.getMainHandItem();
        
        // 检查工具是否在黑名单中
        if (isToolBlacklisted(heldItem)) {
            // 如果工具在黑名单中，不应用额外的耐久消耗
            return;
        }
        
        // 保留宽松的工具判断，只检查物品是否可损坏
        if (!heldItem.isDamageableItem()) return;
        
        // 直接检查耐久，不需要再次判断isDamageableItem()
        // 这里的检查不是耐久消耗的实际基础值，和耐久消耗没有关系

        int currentDurability = heldItem.getMaxDamage() - heldItem.getDamageValue();
        int requiredDurability = 9; // 额外消耗的9点耐久值
        
        if (currentDurability < requiredDurability) {
            event.setNewSpeed(0); // 设置挖掘速度为0，使玩家无法挖掘
        }
    }

    // 添加方块破坏事件监听器
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getState().getBlock();
        float blockHardness = brokenBlock.defaultBlockState().getDestroySpeed(event.getLevel(), event.getPos());
        
        if (blockHardness <= 0) {
            return;
        }
        
        // 获取玩家手持物品
        ItemStack heldItem = player.getMainHandItem();
        
        // 检查工具是否在黑名单中
        if (isToolBlacklisted(heldItem)) {
            // 如果工具在黑名单中，不记录目标方块
            return;
        }
        
        // 记录玩家正在破坏的方块
        BlockTracker.setTargetBlock(player, brokenBlock);
    }
    
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
        }
    }

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack foodStack = event.getItem();
        Item foodItem = foodStack.getItem();

        FoodProperties props = foodStack.getFoodProperties(player);
        if (ForgeRegistries.ITEMS.getKey(foodItem) == null) {
            return;
        }

        String foodId = ForgeRegistries.ITEMS.getKey(foodItem).toString();

        FoodConfig.FoodProperty foodProperty = FoodConfig.getFoodProperty(foodId);
        if (foodProperty != null && props != null) {
            // 仅当原生是食物时，按“原值-原版+配置”方式覆写，避免与南瓜种子混入的逻辑重复
            FoodData stats = player.getFoodData();
            stats.setFoodLevel(stats.getFoodLevel() - props.getNutrition() + foodProperty.getHunger());
            stats.setSaturation(stats.getSaturationLevel() - props.getSaturationModifier() + foodProperty.getSaturation());
        }

        // Apply cooldown after eating, if enabled
        if (CommonConfig.ENABLE_FOOD_COOLDOWN.get()) {
            Integer manualCooldown = (foodProperty != null) ? foodProperty.getCooldown() : null;
            int cooldownTicks;
            if (manualCooldown != null && manualCooldown > 0) {
                cooldownTicks = manualCooldown;
            } else {
                // Auto: prefer native props when present; otherwise, fallback to configured values if available
                if (props != null) {
                    int nutrition = props.getNutrition();
                    float saturationGain = nutrition * props.getSaturationModifier() * 2.0F;
                    cooldownTicks = Math.max(0, Math.round((nutrition + saturationGain) * 20f));
                } else if (foodProperty != null) {
                    int nutrition = Math.max(0, foodProperty.getHunger());
                    float saturationGain = Math.max(0f, foodProperty.getSaturation());
                    cooldownTicks = Math.max(0, Math.round((nutrition + saturationGain) * 20f));
                } else {
                    cooldownTicks = 0;
                }
            }

            if (cooldownTicks > 0) {
                // Do not stack manual and auto: we already chose one source. Apply once.
                player.getCooldowns().addCooldown(foodItem, cooldownTicks);
            }
        }
    }

    // 允许对方块右键时也触发种子进食：禁止方块交互，允许物品交互，并开始使用
    @SubscribeEvent
    public void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();
        var key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) return;
        String id = key.toString();
        // 仅对小麦种子保留方块右键进食流程；南瓜种子优先与方块互动
        if (!"minecraft:wheat_seeds".equals(id)) return;

        // 冷却中则不触发进食逻辑，保持与对空气右键一致（不食用）
        if (player.getCooldowns().isOnCooldown(item)) {
            return;
        }
        event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        player.startUsingItem(event.getHand());
    }

    // 添加方块破坏事件监听器

    

}
