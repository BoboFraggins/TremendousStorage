package net.bobofraggins.intellistore.external.structurepoolapi;

import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;

/**
 * Registers IntelliStore village house pieces with the Structure Pool API so they are
 * injected into the appropriate vanilla template pools at server startup. Weight 1 makes
 * each building uncommon — roughly one per village on average.
 */
public final class StructurePoolIntegration {

    private StructurePoolIntegration() {}

    public static void register() {
        StructurePoolConfig config = new StructurePoolConfig();
        config.entries.add(
                new StructurePoolConfig.Entry("minecraft:village/plains/houses", "intellistore:village/plain", 1));
        config.entries.add(
                new StructurePoolConfig.Entry("minecraft:village/desert/houses", "intellistore:village/desert", 1));
        config.entries.add(
                new StructurePoolConfig.Entry("minecraft:village/savanna/houses", "intellistore:village/savanna", 1));
        config.entries.add(
                new StructurePoolConfig.Entry("minecraft:village/snowy/houses", "intellistore:village/snowy", 1));
        config.entries.add(
                new StructurePoolConfig.Entry("minecraft:village/taiga/houses", "intellistore:village/taiga", 1));
        StructurePoolAPI.injectAll(config);
    }
}
