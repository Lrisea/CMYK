package org.cmyk.durability_overhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.NoteBlockEvent.Play;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import cmyk.util.BlockTracker;
import org.config.BlockDurabilityConfig;

import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("cmyk_durability_overhaul")
public class CMYKDurabilityOverhaul {
    public static final String MODID = "cmyk_durability_overhaul";
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
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("CMYK Durability Overhaul mod initialized");
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
}
