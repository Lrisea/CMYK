package org.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BlockDurabilityConfig {
    // 修改配置路径引用
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("cmyk-durabilityOverhaul/block_durability_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_CONFIG_RESOURCE = "defaultConfig/default_block_durability_config.json";
    private static Map<String, Integer> blockDurabilityCosts = new HashMap<>();
    private static int defaultCost = 10;

    public static void loadConfig() {
        // 如果配置文件不存在，创建默认配置文件
        if (!Files.exists(CONFIG_PATH)) {
            try {
                // 创建目录
                Files.createDirectories(CONFIG_PATH.getParent());
                
                // 从资源文件复制默认配置
                copyDefaultConfig();
                
                System.out.println("已使用Forge推荐方法创建默认配置: " + CONFIG_PATH);
            } catch (IOException e) {
                System.err.println("创建默认配置文件时出错: " + e.getMessage());
                e.printStackTrace();
                // 如果出错，使用最小默认配置
                initMinimalDefaultConfig();
            }
        }
        
        // 读取配置文件
        try {
            String jsonContent = new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8);
            JsonObject config = GSON.fromJson(jsonContent, JsonObject.class);
            
            // 读取默认消耗
            if (config.has("defaultCost")) {
                defaultCost = config.get("defaultCost").getAsInt();
            }
            
            // 读取方块特定消耗
            if (config.has("blockDurabilityCosts")) {
                JsonObject blockCosts = config.getAsJsonObject("blockDurabilityCosts");
                for (String blockId : blockCosts.keySet()) {
                    blockDurabilityCosts.put(blockId, blockCosts.get(blockId).getAsInt());
                }
            }
        } catch (IOException e) {
            System.err.println("读取配置文件时出错: " + e.getMessage());
            e.printStackTrace();
            // 如果出错，使用最小默认配置
            initMinimalDefaultConfig();
        }
    }
    
    /**
     * 使用Forge推荐的方法从资源文件复制默认配置
     */
    private static void copyDefaultConfig() {
        try {
            // 使用Forge的ResourceLocation概念访问资源
            InputStream inputStream = BlockDurabilityConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE);
            if (inputStream != null) {
                // 读取资源文件内容并写入配置文件
                String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Files.writeString(CONFIG_PATH, jsonContent, StandardCharsets.UTF_8);
                inputStream.close();
            } else {
                System.err.println("无法找到默认配置资源: " + DEFAULT_CONFIG_RESOURCE);
                // 创建基本的默认配置
                createBasicDefaultConfig();
            }
        } catch (IOException e) {
            System.err.println("复制默认配置文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建基本的默认配置文件
     */
    private static void createBasicDefaultConfig() {
        try {
            // 创建一个基本的默认配置对象
            JsonObject config = new JsonObject();
            config.addProperty("defaultCost", 10);
            
            JsonObject blockCosts = new JsonObject();
            blockCosts.addProperty("#minecraft:ores", 100);
            blockCosts.addProperty("#minecraft:deepslate_ores", 200);
            blockCosts.addProperty("#minecraft:mineable/pickaxe", 10);
            blockCosts.addProperty("#minecraft:mineable/axe", 10);
            blockCosts.addProperty("#minecraft:mineable/shovel", 10);
            
            config.add("blockDurabilityCosts", blockCosts);
            
            // 写入配置文件
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("创建基本默认配置文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void initMinimalDefaultConfig() {
        // 设置最小默认值，确保功能正常
        blockDurabilityCosts.clear();
        defaultCost = 10;
    }
    
    // 获取指定方块的耐久消耗值
    public static int getDurabilityCost(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId != null && blockDurabilityCosts.containsKey(blockId.toString())) {
            return blockDurabilityCosts.get(blockId.toString());
        }
        return defaultCost;
    }
}