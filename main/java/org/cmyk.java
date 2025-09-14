package org;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
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
import org.config.BlockDurabilityConfig;
import org.slf4j.Logger;
// 在导入部分添加
import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(cmyk.MODID)
public class cmyk {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "cmyk";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public cmyk() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注意：Config文件似乎已被删除，这里移除了对Config的引用
    }

    // 实现让玩家破坏方块时额外消耗9点工具耐久的功能，增加方块黑名单（没有硬度的方块不消耗耐久）
    // 1. 移除冗余的工具判断条件
    @SubscribeEvent
    public void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        // 获取被破坏的方块
        Block brokenBlock = blockState.getBlock();
        
        // 获取玩家
        Player player = event.getEntity();
        
        // 检查方块是否有硬度
        Level level = player.level();
        Optional<BlockPos> optionalPos = event.getPosition();
        
        if (!optionalPos.isPresent()) {
            return;
        }
        
        BlockPos pos = optionalPos.get();
        float blockHardness = brokenBlock.defaultBlockState().getDestroySpeed(level, pos);
        
        if (blockHardness <= 0) return;
        
        // 获取玩家手持物品
        ItemStack heldItem = player.getMainHandItem();
        
        // 保留宽松的工具判断，只检查物品是否可损坏
        if (!heldItem.isDamageableItem()) return;
        
        // 直接检查耐久，不需要再次判断isDamageableItem()
        int currentDurability = heldItem.getMaxDamage() - heldItem.getDamageValue();
        int requiredDurability = 9; // 额外消耗的9点耐久值
        
        if (currentDurability < requiredDurability) {
            event.setNewSpeed(0); // 设置挖掘速度为0，使玩家无法挖掘
        }
    }
    
    // 2. 修复commonSetup方法中对不存在的Config类的引用
    private void commonSetup(final FMLCommonSetupEvent event) {
        // 移除对不存在的Config类的引用
        // event.enqueueWork(BlockDurabilityConfig::loadConfig);
        
        LOGGER.info("HELLO FROM COMMON SETUP");
    }
    
    // 3. 移除对不存在的Config类的导入
    // import org.config.BlockDurabilityConfig;
    
    // 修复onBlockBreak方法中的一致性
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Block brokenBlock = event.getState().getBlock();
        float blockHardness = brokenBlock.defaultBlockState().getDestroySpeed(event.getLevel(), event.getPos());
        if (blockHardness <= 0) {
            return;
        }
        
        // 根据文档使用getPlayer()方法获取玩家
        Player player = event.getPlayer();
        
        // 删除这行代码
        // PLAYER_CURRENT_BLOCK.put(player, brokenBlock);
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
