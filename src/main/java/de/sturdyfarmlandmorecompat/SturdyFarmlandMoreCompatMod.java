package de.sturdyfarmlandmorecompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(SturdyFarmlandMoreCompatMod.MOD_ID)
public class SturdyFarmlandMoreCompatMod {

    public static final String MOD_ID = "sturdy_farmland_more_compat";

    public SturdyFarmlandMoreCompatMod() {
        // leer, alles läuft über den EventBusSubscriber
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class Events {

        // Tag für unsere zusätzlichen Farmlands
        public static final TagKey<Block> EXTRA_FARMLAND_TAG =
                TagKey.create(Registries.BLOCK, new ResourceLocation(MOD_ID, "extra_farmland"));

        // Tags für Sprinkler-Tiers aus Sturdy Farmland - Growth Edition
        public static final TagKey<Block> SPRINKLER_TIER_1_TAG =
                TagKey.create(Registries.BLOCK, new ResourceLocation("dew_drop_farmland_growth", "sprinkler_tier_1"));
        public static final TagKey<Block> SPRINKLER_TIER_2_TAG =
                TagKey.create(Registries.BLOCK, new ResourceLocation("dew_drop_farmland_growth", "sprinkler_tier_2"));
        public static final TagKey<Block> SPRINKLER_TIER_3_TAG =
                TagKey.create(Registries.BLOCK, new ResourceLocation("dew_drop_farmland_growth", "sprinkler_tier_3"));
        public static final TagKey<Block> SPRINKLER_TIER_4_TAG =
                TagKey.create(Registries.BLOCK, new ResourceLocation("dew_drop_farmland_growth", "sprinkler_tier_4"));

        // Pro Dimension: wurde im aktuellen DayTime-Zyklus (5–15) schon resettet?
        private static final Map<ResourceKey<Level>, Boolean> hasResetThisCycle = new HashMap<>();

        // Optional: explizite Farmland -> Bodenblock-Mappings
        private static final Map<ResourceLocation, Block> farmlandFallbacks = new HashMap<>();

        // Mapping: Fertilizer-Item-ID -> Ziel-Farmland-Block-ID
        private static final Map<ResourceLocation, ResourceLocation> FERTILIZER_TO_SOIL_BLOCK = new HashMap<>();

        static {
            // Hier nehmen wir die offensichtlichen Item-IDs an.
            // Falls sie anders heißen, funktioniert es einfach nicht – aber es crasht nichts.
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "weak_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "weak_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "strong_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "strong_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "hyper_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "hyper_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "hydrating_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "hydrating_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "deluxe_hydrating_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "deluxe_hydrating_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "bountiful_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "bountiful_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "low_quality_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "low_quality_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "high_quality_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "high_quality_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    new ResourceLocation("dew_drop_farmland_growth", "pristine_quality_fertilizer"),
                    new ResourceLocation("dew_drop_farmland_growth", "pristine_quality_fertilized_farmland")
            );
        }

        // --- TICK-LOGIK ---

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel level)) return;

            ResourceKey<Level> dim = level.dimension();

            long gameTime = level.getGameTime();         // echte Ticks seit Welterstellung
            long dayTime  = level.getDayTime() % 24000L; // 0–23999, von Better Days beeinflusst

            // Reset-Fenster an Tagesanbruch (wie Sturdy Farmland: dailyTimeMin=5, innerhalb 10 Ticks)
            boolean inResetWindow = (dayTime >= 5L && dayTime <= 15L);

            boolean alreadyReset    = hasResetThisCycle.getOrDefault(dim, false);
            boolean doDailyResetNow = inResetWindow && !alreadyReset;

            // Wenn wir das Fenster verlassen, Reset-Flag für den nächsten Tag freigeben
            if (!inResetWindow && alreadyReset) {
                hasResetThisCycle.put(dim, false);
            }

            // Moisture-Stabilisierung ca. 1x pro Sekunde außerhalb des Reset-Fensters
            boolean isStabilizeTick        = (gameTime % 20L == 0L);
            boolean allowMoistureStabilize = !inResetWindow && isStabilizeTick;

            if (!doDailyResetNow && !allowMoistureStabilize) {
                return;
            }

            if (doDailyResetNow) {
                hasResetThisCycle.put(dim, true);
            }

            processExtraFarmland(level, doDailyResetNow, allowMoistureStabilize);
        }

        private static void processExtraFarmland(ServerLevel level,
                                                 boolean doDailyReset,
                                                 boolean stabilizeMoisture) {

            List<? extends Player> players = level.players();
            if (players.isEmpty()) return;

            int radius = 64; // Check-Umkreis um Spieler

            RegistryAccess registryAccess = level.registryAccess();

            for (Player player : players) {
                BlockPos center = player.blockPosition();
                BlockPos min = center.offset(-radius, -2, -radius);
                BlockPos max = center.offset(radius, 2, radius);

                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    BlockState state = level.getBlockState(pos);

                    if (!isExtraFarmland(state, registryAccess)) {
                        continue;
                    }

                    if (doDailyReset) {
                        handleFarmlandDailyReset(level, pos, state);
                    } else if (stabilizeMoisture) {
                        stabilizeFarmlandMoisture(level, pos, state);
                    }
                }
            }
        }

        private static boolean isExtraFarmland(BlockState state, RegistryAccess registryAccess) {
            return state.is(EXTRA_FARMLAND_TAG);
        }

        /**
         * Genau 1× pro DayTime-Zyklus im Fenster 5–15:
         *
         * - Wenn ein Sprinkler in Reichweite ist:
         *      → Pflanze wächst um 1 Stufe (ohne Regen-Bedingung)
         *      → Farmland wird voll bewässert (Moisture = MAX)
         *      → kein Austrocknen, kein Dirt-Fallback
         *
         * - Wenn KEIN Sprinkler:
         *      - Wenn Moisture > 0:
         *          → bei Regen wächst die Pflanze über dem Feld um 1 Stufe
         *          → Feld wird trocken (Moisture -> 0)
         *      - Wenn Moisture == 0 oder keine Property:
         *          → Feld wird zum Bodenblock (Dirt / passender Boden)
         */
        private static void handleFarmlandDailyReset(ServerLevel level, BlockPos pos, BlockState state) {
            boolean sprinklerProtected = isFarmlandProtectedBySprinkler(level, pos);

            if (sprinklerProtected) {
                // Sprinkler-Fall: Crop wachsen lassen + Farmland bewässern, Feld bleibt bestehen
                growCropAbove(level, pos);

                if (state.hasProperty(FarmBlock.MOISTURE)) {
                    if (state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                        BlockState newState = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
                        level.setBlockAndUpdate(pos, newState);
                    }
                }
                return;
            }

            // Kein Sprinkler → Standardlogik
            if (state.hasProperty(FarmBlock.MOISTURE)) {
                int moisture = state.getValue(FarmBlock.MOISTURE);

                if (moisture > 0) {
                    // Bewässert → wenn es regnet, Pflanze wachsen lassen
                    growCropAboveIfRaining(level, pos);

                    // dann austrocknen, Farmland bleibt bestehen
                    BlockState newState = state.setValue(FarmBlock.MOISTURE, 0);
                    level.setBlockAndUpdate(pos, newState);
                } else {
                    // Bereits trocken → wird zu Bodenblock
                    BlockState newState = getFallbackSoilState(level, state);
                    level.setBlockAndUpdate(pos, newState);
                }
            } else {
                // Kein Moisture-Property → direkt zu Bodenblock
                BlockState newState = getFallbackSoilState(level, state);
                level.setBlockAndUpdate(pos, newState);
            }
        }

        /**
         * Sprinkler-Reichweitenprüfung:
         * - Tier 1: 3x3 (Radius 1)
         * - Tier 2: 5x5 (Radius 2)
         * - Tier 3: 7x7 (Radius 3)
         * - Tier 4: 9x9 (Radius 4)
         * Es wird nach Sprinklern im Bereich von +/-4 Blöcken auf gleicher
         * oder einer Ebene darüber gesucht.
         */
        private static boolean isFarmlandProtectedBySprinkler(ServerLevel level, BlockPos farmlandPos) {
            int maxRadius = 4;

            for (int dy = 0; dy <= 1; dy++) { // gleiche Ebene und eine darüber
                for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                    for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                        BlockPos checkPos = farmlandPos.offset(dx, dy, dz);
                        BlockState checkState = level.getBlockState(checkPos);

                        int radius = 0;

                        if (checkState.is(SPRINKLER_TIER_1_TAG)) {
                            radius = 1;
                        } else if (checkState.is(SPRINKLER_TIER_2_TAG)) {
                            radius = 2;
                        } else if (checkState.is(SPRINKLER_TIER_3_TAG)) {
                            radius = 3;
                        } else if (checkState.is(SPRINKLER_TIER_4_TAG)) {
                            radius = 4;
                        }

                        if (radius > 0) {
                            int dxAbs = Math.abs(farmlandPos.getX() - checkPos.getX());
                            int dzAbs = Math.abs(farmlandPos.getZ() - checkPos.getZ());

                            if (dxAbs <= radius && dzAbs <= radius) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Generische Crop-Wachstumsfunktion: +1 Age, wenn möglich.
         */
        private static void growCropAbove(ServerLevel level, BlockPos farmlandPos) {
            BlockPos cropPos = farmlandPos.above();
            BlockState cropState = level.getBlockState(cropPos);

            if (!(cropState.getBlock() instanceof CropBlock cropBlock)) {
                return;
            }

            int age = cropBlock.getAge(cropState);
            int maxAge = cropBlock.getMaxAge();

            if (age < maxAge) {
                int newAge = age + 1;
                BlockState newCropState = cropBlock.getStateForAge(newAge);
                level.setBlockAndUpdate(cropPos, newCropState);
            }
        }

        /**
         * Lässt die Pflanze über dem Farmland um 1 Wachstumsstufe wachsen,
         * wenn es an dieser Position regnet und ein CropBlock darüber steht.
         */
        private static void growCropAboveIfRaining(ServerLevel level, BlockPos farmlandPos) {
            BlockPos cropPos = farmlandPos.above();

            if (!level.isRainingAt(cropPos)) {
                return;
            }

            growCropAbove(level, farmlandPos);
        }

        /**
         * Läuft ca. 1x pro Sekunde außerhalb des Reset-Fensters.
         * - Wenn Moisture == 0 und Regen -> Moisture = 7
         * - Wenn Moisture > 0 -> Moisture auf 7 halten
         */
        private static void stabilizeFarmlandMoisture(ServerLevel level, BlockPos pos, BlockState state) {
            if (!state.hasProperty(FarmBlock.MOISTURE)) return;

            int moisture = state.getValue(FarmBlock.MOISTURE);

            // 1) Regen kann trockenes Feld bewässern
            if (moisture == 0) {
                if (level.isRainingAt(pos.above())) {
                    BlockState newState = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
                    level.setBlockAndUpdate(pos, newState);
                }
                return;
            }

            // 2) Bewässertes Feld: Moisture auf MAX halten
            if (moisture < FarmBlock.MAX_MOISTURE) {
                BlockState newState = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
                level.setBlockAndUpdate(pos, newState);
            }
        }

        /**
         * Farmland → Ursprungsboden
         * Priorität:
         * 1) explizite Mod-Mappings
         * 2) heuristisch aus dem Namen ableiten
         * 3) Vanilla Dirt
         */
        private static BlockState getFallbackSoilState(ServerLevel level, BlockState farmlandState) {
            ResourceLocation id = farmlandState.getBlock().builtInRegistryHolder().key().location();
            String namespace = id.getNamespace();
            String path      = id.getPath();

            ResourceLocation soilId = null;

            // Immersive Weathering
            if (namespace.equals("immersive_weathering")) {
                switch (path) {
                    case "silty_farmland"        -> soilId = new ResourceLocation(namespace, "silt");
                    case "sandy_farmland"        -> soilId = new ResourceLocation(namespace, "sandy_dirt");
                    case "earthen_clay_farmland" -> soilId = new ResourceLocation(namespace, "earthen_clay");
                    case "loamy_farmland"        -> soilId = new ResourceLocation(namespace, "loam");
                }
            }

            // Farmer's Delight
            if (namespace.equals("farmersdelight")) {
                if (path.equals("rich_soil_farmland")) {
                    soilId = new ResourceLocation(namespace, "rich_soil");
                }
            }

            // Regions Unexplored
            if (namespace.equals("regions_unexplored")) {
                switch (path) {
                    case "peat_farmland" -> soilId = new ResourceLocation(namespace, "peat_dirt");
                    case "silt_farmland" -> soilId = new ResourceLocation(namespace, "silt_dirt");
                }
            }

            // Aquaculture 2: farmland -> Vanilla Dirt
            if (namespace.equals("aquaculture")) {
                if (path.equals("farmland")) {
                    soilId = new ResourceLocation("minecraft", "dirt");
                }
            }

            // explizites Mapping anwenden, falls vorhanden
            if (soilId != null) {
                Block mapped = ForgeRegistries.BLOCKS.getValue(soilId);
                if (mapped != null && mapped != Blocks.AIR) {
                    return mapped.defaultBlockState();
                }
            }

            // Heuristik: aus *_farmland einen *_dirt-Block o.ä. ableiten
            String[] candidates = {
                    path.replace("_farmland", "_dirt"),
                    path.replace("farmland", "dirt")
            };

            for (String candPath : candidates) {
                if (candPath.equals(path)) continue;

                ResourceLocation candId = new ResourceLocation(namespace, candPath);
                Block candBlock = ForgeRegistries.BLOCKS.getValue(candId);

                if (candBlock != null && candBlock != Blocks.AIR) {
                    return candBlock.defaultBlockState();
                }
            }

            // Fallback: Vanilla Dirt
            return Blocks.DIRT.defaultBlockState();
        }

        // --- FERTILIZER-SUPPORT ---

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Level level = event.getLevel();
            if (level.isClientSide()) return;

            if (!(level instanceof ServerLevel serverLevel)) return;

            BlockPos pos = event.getPos();
            BlockState state = serverLevel.getBlockState(pos);

            // Nur unsere extra Farmlands interessieren uns
            if (!state.is(EXTRA_FARMLAND_TAG)) return;

            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            Player player = event.getEntity();
            if (player == null) return;

            // Welches Item wird benutzt?
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) return;

            // Gibt es dafür ein Fertilizer-Mapping?
            ResourceLocation targetSoilId = FERTILIZER_TO_SOIL_BLOCK.get(itemId);
            if (targetSoilId == null) return;

            Block targetBlock = ForgeRegistries.BLOCKS.getValue(targetSoilId);
            if (targetBlock == null || targetBlock == Blocks.AIR) return;

            // Block ersetzen
            BlockState newState = targetBlock.defaultBlockState();
            serverLevel.setBlockAndUpdate(pos, newState);

            // Item verbrauchen (außer im Creative)
            if (!player.isCreative()) {
                stack.shrink(1);
            }

            // Event als handled markieren, damit nichts doppelt passiert
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
