package org.atcgroup.netcompat;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public interface Config {
    ModConfigSpec.Builder CL_BUILDER = new ModConfigSpec.Builder();

    ModConfigSpec.BooleanValue CL_ALLOW_UNSAFE = CL_BUILDER
            .comment("Enforce modded client to connect to vanilla server")
            .define("unsafe", true);

    ModConfigSpec.ConfigValue<String> CL_VHOST = CL_BUILDER
            .comment("The v-host we use to connect, can work with velocity 'forced-hosts'.")
            .define("client-vhost", "test-router.example.com");

    ModConfigSpec CLIENT_SPEC = CL_BUILDER.build();

    ModConfigSpec.Builder SV_BUILDER = new ModConfigSpec.Builder();

    ModConfigSpec.BooleanValue SV_ALLOW_UNSAFE = SV_BUILDER
            .comment("Allow unsafe(Has mods as our super-set) clients to connect")
            .define("allow-unsafe", true);

    ModConfigSpec SERVER_SPEC = SV_BUILDER.build();
}
