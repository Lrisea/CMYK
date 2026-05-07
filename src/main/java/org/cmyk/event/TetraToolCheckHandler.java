package org.cmyk.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.cmyk.compat.tetra.TetraCompat;
import org.cmyk.config.TetraToolConfig;

import java.util.List;

/**
 * Tetra 工具挖掘要求检查处理器
 */
public class TetraToolCheckHandler {

    /**
     * 检查玩家手持工具是否符合目标方块的 Tetra 挖掘要求
     *
     * @param player 玩家
     * @param stack  手持物品
     * @param block  目标方块
     * @param state  目标方块状态
     * @return 拒绝消息，若通过检查返回 null
     */
    public static Component check(Player player, ItemStack stack, Block block, BlockState state) {
        // Tetra 未加载时直接放行
        if (!TetraCompat.isTetraLoaded()) {
            return null;
        }

        TetraToolConfig.Requirement req = TetraToolConfig.getRequirement(block);

        // 没有特殊要求时放行
        if (!TetraToolConfig.hasSpecificRequirement(block)) {
            return null;
        }

        // 如果不是 Tetra 工具，检查是否存在硬性要求
        // 当配置要求存在时，非 Tetra 工具默认不满足（除非配置允许）
        boolean isTetra = TetraCompat.isTetraTool(stack);

        // 检查效率要求
        if (req.hasEfficiencyCheck()) {
            float efficiency = isTetra
                ? TetraCompat.getEffectiveEfficiency(stack, state)
                : getVanillaEfficiency(stack, state);
            if (efficiency < req.getMinEfficiency()) {
                return buildDenyMessage(req, "工具效率不足 (需要 ≥ " + req.getMinEfficiency() + ", 实际 " + String.format("%.2f", efficiency) + ")");
            }
        }

        // 检查等级要求
        if (req.hasTierCheck()) {
            int tier = isTetra
                ? TetraCompat.getHarvestTier(stack)
                : getVanillaHarvestTier(stack);
            if (tier < req.getMinTier()) {
                return buildDenyMessage(req, "工具等级不足 (需要 ≥ " + req.getMinTier() + ", 实际 " + tier + ")");
            }
        }

        // 检查模块类型要求 (仅对 Tetra 工具有效)
        if (req.hasModuleCheck()) {
            if (!isTetra) {
                return buildDenyMessage(req, "需要 Tetra 模块化工具");
            }
            List<String> installedModules = TetraCompat.getInstalledModules(stack);
            List<String> requiredModules = req.getRequiredModules();
            boolean hasRequired = requiredModules.stream()
                .anyMatch(required -> installedModules.stream()
                    .anyMatch(installed -> installed.equalsIgnoreCase(required)));
            if (!hasRequired) {
                return buildDenyMessage(req, "缺少必要模块: " + String.join(", ", requiredModules));
            }
        }

        return null;
    }

    /**
     * 获取原版工具效率作为回退
     */
    private static float getVanillaEfficiency(ItemStack stack, BlockState state) {
        // 原版工具在合适方块上的基础效率约为 1.0f
        if (stack.isCorrectToolForDrops(state)) {
            return 1.0f;
        }
        return 0.0f;
    }

    /**
     * 获取原版工具挖掘等级作为回退
     */
    private static int getVanillaHarvestTier(ItemStack stack) {
        // 原版工具没有明确等级 API，返回 0 表示无法精确判断
        return 0;
    }

    private static Component buildDenyMessage(TetraToolConfig.Requirement req, String detail) {
        String base = req.getDenyMessage();
        if (base == null || base.isEmpty()) {
            base = "工具不符合挖掘要求";
        }
        return Component.literal(base + " — " + detail);
    }
}
