package org.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BlockDurabilityConfig {
    private static final String CONFIG_PATH = "config/cmyk/block_durability_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_CONFIG_RESOURCE = "/defaultConfig/default_block_durability_config.json";
    private static Map<String, Integer> blockDurabilityCosts = new HashMap<>();
    private static int defaultCost = 10;

    public static void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        
        // 如果配置文件不存在，直接从资源文件加载默认配置
        if (!configFile.exists()) {
            try {
                // 创建目录
                configFile.getParentFile().mkdirs();
                
                // 从资源文件读取默认配置
                try (InputStream inputStream = BlockDurabilityConfig.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                    // 读取资源文件内容并写入配置文件
                    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    FileUtils.writeStringToFile(configFile, jsonContent, StandardCharsets.UTF_8);
                    System.out.println("已从资源文件创建默认配置: " + CONFIG_PATH);
                }
            } catch (IOException e) {
                System.err.println("创建默认配置文件时出错: " + e.getMessage());
                e.printStackTrace();
                // 如果出错，使用最小默认配置
                initMinimalDefaultConfig();
            }
        }
        
        // 读取配置文件
        try {
            String jsonContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
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