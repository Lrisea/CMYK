package cmyk.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FOOD_COOLDOWN;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_FOOD_COOLDOWN = builder
                .comment("Enable cooldown assignment after consuming food")
                .define("enableFoodCooldown", true);

        SPEC = builder.build();
    }
}
