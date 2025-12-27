package org.valkyrienskies.vscrews;

import net.minecraftforge.common.ForgeConfigSpec;

public class VSCrewsConfig {
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue ALLOW_NON_CREW_BREAK_HELM;
    public static final ForgeConfigSpec.BooleanValue HELM_WITHOUT_CREW_USABLE_BY_EVERYONE;
    public static final ForgeConfigSpec.BooleanValue ONLY_ONE_CREW_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue ALLOW_NON_OWNER_MANAGE_MEMBERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("vscrews");
        ALLOW_NON_CREW_BREAK_HELM = builder
                .comment("If true, players not in the helm's crew can break the helm. If false, only crew members can break.")
                .define("allowNonCrewBreakHelm", false);

        HELM_WITHOUT_CREW_USABLE_BY_EVERYONE = builder
                .comment("If true, helms placed by players not in a crew are usable by everyone. If false, only the placer can use them.")
                .define("helmWithoutCrewUsableByEveryone", false);

        ONLY_ONE_CREW_PER_PLAYER = builder
                .comment("If true, a player can only own one crew. Attempts to create another crew will be denied.")
                .define("onlyOneCrewPerPlayer", true);

        ALLOW_NON_OWNER_MANAGE_MEMBERS = builder
                .comment("If true, any crew member can add or remove members; if false, only the crew owner can.")
                .define("allowNonOwnerManageMembers", false);

        builder.pop();

        COMMON_SPEC = builder.build();
    }
}
