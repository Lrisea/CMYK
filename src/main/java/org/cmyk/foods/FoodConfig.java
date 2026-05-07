package org.cmyk.foods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.fml.loading.FMLPaths;

public class FoodConfig {

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("cmyk/customs/food_properties.json");
    private static Map<String, FoodProperty> foodProperties = Collections.emptyMap();

    public static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            ensureParentDirectory();
            writeDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, FoodProperty>>() {}.getType();
            foodProperties = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FoodProperty getFoodProperty(String foodId) {
        return foodProperties.get(foodId);
    }

    public static class FoodProperty {
        private int hunger;
        private float saturation;
        private Integer cooldown; // ticks, nullable; if present, overrides auto cooldown
        private Integer useDuration; // ticks, nullable; if present, overrides default use duration

        public FoodProperty(int hunger, float saturation) {
            this.hunger = hunger;
            this.saturation = saturation;
        }

        public FoodProperty(int hunger, float saturation, Integer cooldown) {
            this.hunger = hunger;
            this.saturation = saturation;
            this.cooldown = cooldown;
        }

        public FoodProperty(int hunger, float saturation, Integer cooldown, Integer useDuration) {
            this.hunger = hunger;
            this.saturation = saturation;
            this.cooldown = cooldown;
            this.useDuration = useDuration;
        }

        public FoodProperty() {}

        public int getHunger() {
            return hunger;
        }

        public float getSaturation() {
            return saturation;
        }

        public Integer getCooldown() {
            return cooldown;
        }

        public Integer getUseDuration() {
            return useDuration;
        }
    }

    private static void ensureParentDirectory() {
        Path parent = CONFIG_PATH.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void writeDefaultConfig() {
        Map<String, Map<String, Object>> defaults = new HashMap<>();
        Map<String, Object> wheatSeeds = new HashMap<>();
        wheatSeeds.put("hunger", 0);
        wheatSeeds.put("saturation", 1.0f);
        wheatSeeds.put("useDuration", 50);
        defaults.put("minecraft:wheat_seeds", wheatSeeds);

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile(), StandardCharsets.UTF_8)) {
            writer.write(prettyGson.toJson(defaults));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, FoodProperty> runtimeDefaults = new HashMap<>();
        runtimeDefaults.put("minecraft:wheat_seeds", new FoodProperty(0, 1.0f, null, 50));
        foodProperties = runtimeDefaults;
    }
}