package cmyk.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;

/**
 * 方块追踪器：记录玩家当前正在破坏的方块
 */
public class BlockTracker {
    // 存储玩家ID和他们正在破坏的方块的映射
    private static final Map<String, Block> PLAYER_TARGET_BLOCK = new HashMap<>();
    
    /**
     * 设置玩家正在破坏的方块
     */
    public static void setTargetBlock(Player player, Block block) {
        PLAYER_TARGET_BLOCK.put(player.getUUID().toString(), block);
    }
    
    /**
     * 获取玩家正在破坏的方块
     */
    public static Block getTargetBlock(Player player) {
        return PLAYER_TARGET_BLOCK.get(player.getUUID().toString());
    }
    
    /**
     * 清除玩家正在破坏的方块记录
     */
    public static void clearTargetBlock(Player player) {
        PLAYER_TARGET_BLOCK.remove(player.getUUID().toString());
    }
}