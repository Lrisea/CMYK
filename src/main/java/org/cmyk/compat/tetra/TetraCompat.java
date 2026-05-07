package org.cmyk.compat.tetra;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Tetra 模组兼容层
 * 安全检测并提取 Tetra 工具的属性，避免强依赖导致崩溃
 */
public class TetraCompat {
    private static boolean checked = false;
    private static boolean tetraLoaded = false;

    // 反射缓存
    private static Class<?> modularItemClass;
    private static Method getEffectiveEfficiencyMethod;
    private static Method getHarvestCapabilityMethod;

    /**
     * 检查 Tetra 模组是否已加载
     */
    public static boolean isTetraLoaded() {
        if (!checked) {
            checked = true;
            tetraLoaded = ModList.get().isLoaded("tetra");
            if (tetraLoaded) {
                initReflection();
            }
        }
        return tetraLoaded;
    }

    private static void initReflection() {
        try {
            modularItemClass = Class.forName("se.mickelus.tetra.items.modular.ModularItem");
            // 尝试获取效率方法 (参数: ItemStack, BlockState)
            try {
                getEffectiveEfficiencyMethod = modularItemClass.getMethod("getEffectiveEfficiency", ItemStack.class, BlockState.class);
            } catch (NoSuchMethodException e) {
                // 尝试其他签名
                try {
                    getEffectiveEfficiencyMethod = modularItemClass.getMethod("getEfficiency", ItemStack.class);
                } catch (NoSuchMethodException e2) {
                    getEffectiveEfficiencyMethod = null;
                }
            }
            // 尝试获取挖掘能力/等级方法
            try {
                getHarvestCapabilityMethod = modularItemClass.getMethod("getHarvestCapability", ItemStack.class);
            } catch (NoSuchMethodException e) {
                getHarvestCapabilityMethod = null;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("TetraCompat: 无法找到 Tetra 核心类，将回退到 NBT 检测");
            tetraLoaded = false;
        }
    }

    /**
     * 判断物品是否是 Tetra 模块化工具
     */
    public static boolean isTetraTool(ItemStack stack) {
        if (stack.isEmpty() || !isTetraLoaded()) return false;
        return modularItemClass.isInstance(stack.getItem());
    }

    /**
     * 获取 Tetra 工具对方块的实际效率
     */
    public static float getEffectiveEfficiency(ItemStack stack, BlockState state) {
        if (!isTetraTool(stack)) return 0.0f;

        // 优先使用 API
        if (getEffectiveEfficiencyMethod != null) {
            try {
                Object result;
                if (getEffectiveEfficiencyMethod.getParameterCount() == 2) {
                    result = getEffectiveEfficiencyMethod.invoke(stack.getItem(), stack, state);
                } else {
                    result = getEffectiveEfficiencyMethod.invoke(stack.getItem(), stack);
                }
                if (result instanceof Number) {
                    return ((Number) result).floatValue();
                }
            } catch (Exception e) {
                // 反射失败，回退到 NBT
            }
        }

        // 回退: 从 NBT 读取效率属性
        return getEfficiencyFromNbt(stack);
    }

    /**
     * 获取工具的挖掘等级 (Tier)
     */
    public static int getHarvestTier(ItemStack stack) {
        if (!isTetraTool(stack)) return 0;

        if (getHarvestCapabilityMethod != null) {
            try {
                Object result = getHarvestCapabilityMethod.invoke(stack.getItem(), stack);
                if (result instanceof Number) {
                    return ((Number) result).intValue();
                }
            } catch (Exception e) {
                // 回退到 NBT
            }
        }

        return getTierFromNbt(stack);
    }

    /**
     * 获取工具已安装的模块类型列表 (如 pickaxe, axe, shovel 等)
     */
    public static List<String> getInstalledModules(ItemStack stack) {
        List<String> modules = new ArrayList<>();
        if (!isTetraTool(stack)) return modules;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("tetra", CompoundTag.TAG_COMPOUND)) {
            return modules;
        }

        CompoundTag tetraTag = tag.getCompound("tetra");

        // 读取部件 (1.x NBT 格式)
        if (tetraTag.contains("modules", CompoundTag.TAG_COMPOUND)) {
            CompoundTag modulesTag = tetraTag.getCompound("modules");
            for (String slotKey : modulesTag.getAllKeys()) {
                CompoundTag moduleData = modulesTag.getCompound(slotKey);
                if (moduleData.contains("id")) {
                    String moduleId = moduleData.getString("id");
                    // 提取模块类型前缀，如 "pickaxe/basic_pickaxe_head" -> "pickaxe"
                    String type = extractModuleType(moduleId);
                    if (!modules.contains(type)) {
                        modules.add(type);
                    }
                }
            }
        }

        // 读取部件 (旧版格式或替代路径)
        if (tetraTag.contains("moduleData", ListTag.TAG_LIST)) {
            ListTag moduleDataList = tetraTag.getList("moduleData", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < moduleDataList.size(); i++) {
                CompoundTag moduleData = moduleDataList.getCompound(i);
                if (moduleData.contains("id")) {
                    String moduleId = moduleData.getString("id");
                    String type = extractModuleType(moduleId);
                    if (!modules.contains(type)) {
                        modules.add(type);
                    }
                }
            }
        }

        return modules;
    }

    /**
     * 从 NBT 估算工具效率 (反射失败时的回退方案)
     */
    private static float getEfficiencyFromNbt(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("tetra")) return 1.0f;

        CompoundTag tetraTag = tag.getCompound("tetra");
        // Tetra 的 honing/progression 或属性数据可能包含效率值
        // 这是一个简化的回退估算：基于模块数量估算
        if (tetraTag.contains("modules")) {
            int moduleCount = tetraTag.getCompound("modules").getAllKeys().size();
            return 1.0f + (moduleCount * 0.5f);
        }
        return 1.0f;
    }

    /**
     * 从 NBT 估算工具等级 (反射失败时的回退方案)
     */
    private static int getTierFromNbt(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("tetra")) return 0;

        // 从模块 ID 推断等级
        // 例如: iron = 2, diamond = 3, netherite = 4
        List<String> modules = getInstalledModules(stack);
        if (modules.isEmpty()) return 0;

        CompoundTag tetraTag = tag.getCompound("tetra");
        if (tetraTag.contains("modules", CompoundTag.TAG_COMPOUND)) {
            CompoundTag modulesTag = tetraTag.getCompound("modules");
            int maxTier = 0;
            for (String key : modulesTag.getAllKeys()) {
                CompoundTag moduleData = modulesTag.getCompound(key);
                if (moduleData.contains("id")) {
                    String id = moduleData.getString("id").toLowerCase();
                    maxTier = Math.max(maxTier, inferTierFromMaterial(id));
                }
            }
            return maxTier;
        }
        return 0;
    }

    /**
     * 从模块 ID 提取类型前缀
     */
    private static String extractModuleType(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return "";
        int slashIndex = moduleId.indexOf('/');
        if (slashIndex > 0) {
            return moduleId.substring(0, slashIndex);
        }
        return moduleId;
    }

    /**
     * 从材料名称推断挖掘等级
     */
    private static int inferTierFromMaterial(String moduleId) {
        if (moduleId.contains("wood") || moduleId.contains("stone")) return 1;
        if (moduleId.contains("iron")) return 2;
        if (moduleId.contains("diamond")) return 3;
        if (moduleId.contains("netherite")) return 4;
        return 0;
    }
}
