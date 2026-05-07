package org.cmyk.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Tetra 工具挖掘要求配置管理器
 * 配置文件: config/cmyk/customs/tetra_tool_requirements.json
 */
public class TetraToolConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("cmyk/customs/tetra_tool_requirements.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_CONFIG_RESOURCE = "defaultConfig/default_tetra_tool_requirements.json";

    private static Requirement defaultRequirements = new Requirement();
    private static Map<String, Requirement> blockRequirements = new HashMap<>();
    private static Map<String, Requirement> tagRequirements = new HashMap<>();

    public static class Requirement {
        private float minEfficiency = 0.0f;
        private int minTier = 0;
        private List<String> requiredModules = new ArrayList<>();
        private String denyMessage = null;

        public float getMinEfficiency() { return minEfficiency; }
        public int getMinTier() { return minTier; }
        public List<String> getRequiredModules() { return requiredModules; }
        public String getDenyMessage() { return denyMessage; }

        public boolean hasEfficiencyCheck() { return minEfficiency > 0; }
        public boolean hasTierCheck() { return minTier > 0; }
        public boolean hasModuleCheck() { return requiredModules != null && !requiredModules.isEmpty(); }
    }

    public static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                copyDefaultConfig();
            } catch (IOException e) {
                System.err.println("创建 Tetra 工具要求默认配置失败: " + e.getMessage());
                initMinimalDefaults();
            }
        }

        try {
            String jsonContent = new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8);
            JsonObject config = GSON.fromJson(jsonContent, JsonObject.class);
            parseConfig(config);
        } catch (IOException e) {
            System.err.println("读取 Tetra 工具要求配置失败: " + e.getMessage());
            initMinimalDefaults();
        }
    }

    private static void parseConfig(JsonObject config) {
        blockRequirements.clear();
        tagRequirements.clear();

        if (config.has("defaultRequirements")) {
            defaultRequirements = GSON.fromJson(config.getAsJsonObject("defaultRequirements"), Requirement.class);
        }

        if (config.has("blockRequirements")) {
            JsonObject blocks = config.getAsJsonObject("blockRequirements");
            for (String key : blocks.keySet()) {
                Requirement req = GSON.fromJson(blocks.get(key), Requirement.class);
                if (key.startsWith("#")) {
                    tagRequirements.put(key.substring(1), req);
                } else {
                    blockRequirements.put(key, req);
                }
            }
        }
    }

    private static void copyDefaultConfig() {
        try {
            InputStream inputStream = TetraToolConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE);
            if (inputStream != null) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Files.writeString(CONFIG_PATH, content, StandardCharsets.UTF_8);
                inputStream.close();
            } else {
                createBasicDefaultConfig();
            }
        } catch (IOException e) {
            System.err.println("复制默认配置失败: " + e.getMessage());
        }
    }

    private static void createBasicDefaultConfig() {
        JsonObject config = new JsonObject();
        config.add("defaultRequirements", GSON.toJsonTree(new Requirement()));

        JsonObject blocks = new JsonObject();

        JsonObject stoneReq = new JsonObject();
        stoneReq.addProperty("minEfficiency", 1.0f);
        stoneReq.addProperty("minTier", 1);
        blocks.add("minecraft:stone", stoneReq);

        JsonObject ironOreReq = new JsonObject();
        ironOreReq.addProperty("minEfficiency", 3.0f);
        ironOreReq.addProperty("minTier", 2);
        ironOreReq.addProperty("denyMessage", "需要更高效的镐类工具才能挖掘此方块");
        blocks.add("minecraft:iron_ore", ironOreReq);

        config.add("blockRequirements", blocks);

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("写入默认配置失败: " + e.getMessage());
        }
    }

    private static void initMinimalDefaults() {
        defaultRequirements = new Requirement();
        blockRequirements.clear();
        tagRequirements.clear();
    }

    /**
     * 获取方块对应的挖掘要求
     */
    public static Requirement getRequirement(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId == null) return defaultRequirements;
        String id = blockId.toString();

        // 1. 精确匹配
        if (blockRequirements.containsKey(id)) {
            return blockRequirements.get(id);
        }

        // 2. 标签匹配
        for (Map.Entry<String, Requirement> entry : tagRequirements.entrySet()) {
            ResourceLocation tagRl = ResourceLocation.tryParse(entry.getKey());
            if (tagRl != null) {
                TagKey<Block> tagKey = BlockTags.create(tagRl);
                Holder.Reference<Block> holder = ForgeRegistries.BLOCKS.getDelegateOrThrow(block);
                if (holder.is(tagKey)) {
                    return entry.getValue();
                }
            }
        }

        return defaultRequirements;
    }

    /**
     * 检查是否存在对该方块的要求（非默认）
     */
    public static boolean hasSpecificRequirement(Block block) {
        Requirement req = getRequirement(block);
        return req != defaultRequirements;
    }
}
