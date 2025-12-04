package de.sturdyfarmlandmorecompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod(SturdyFarmlandMoreCompatMod.MOD_ID)
public class SturdyFarmlandMoreCompatMod {

    public static final String MOD_ID = "sturdy_farmland_more_compat";

    public SturdyFarmlandMoreCompatMod() {
        // alles läuft über @EventBusSubscriber
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        // Tag für unsere zusätzlichen Farmlands (nur modded Farmlands!)
        public static final TagKey<Block> EXTRA_FARMLAND_TAG =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, "extra_farmland"));

        // Sprinkler-Tags aus Sturdy Farmland - Growth Edition
        public static final TagKey<Block> SPRINKLER_TIER_1_TAG =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "sprinkler_tier_1"));
        public static final TagKey<Block> SPRINKLER_TIER_2_TAG =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "sprinkler_tier_2"));
        public static final TagKey<Block> SPRINKLER_TIER_3_TAG =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "sprinkler_tier_3"));
        public static final TagKey<Block> SPRINKLER_TIER_4_TAG =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "sprinkler_tier_4"));

        // pro Dimension: wurde im aktuellen Tages-Zyklus schon resettet?
        private static final Map<ResourceKey<Level>, Boolean> hasResetThisCycle = new HashMap<>();

        // Mapping: Fertilizer-Item -> Ziel-Farmland-Block (Sturdy Farmland GE)
        private static final Map<ResourceLocation, ResourceLocation> FERTILIZER_TO_SOIL_BLOCK = new HashMap<>();

        // Tooltip-Texte für Fertilizer
        private static final Map<ResourceLocation, String> FERTILIZER_TOOLTIPS = new HashMap<>();

        static {
            // Block-Konvertierung
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "weak_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "weak_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "strong_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "strong_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hyper_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hyper_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hydrating_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hydrating_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "deluxe_hydrating_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "deluxe_hydrating_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "bountiful_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "bountiful_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "low_quality_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "low_quality_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "high_quality_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "high_quality_fertilized_farmland")
            );
            FERTILIZER_TO_SOIL_BLOCK.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "pristine_quality_fertilizer"),
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "pristine_quality_fertilized_farmland")
            );

            // Tooltips – kurz & auf den Punkt, basierend auf Originalbeschreibung + unseren Erweiterungen
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "weak_fertilizer"),
                    "Boosts initial crop growth by 1 day."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "strong_fertilizer"),
                    "Boosts initial crop growth by 2 days."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hyper_fertilizer"),
                    "Boosts initial crop growth by 3 days."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "hydrating_fertilizer"),
                    "Farmland stays wet until crops are half grown."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "deluxe_hydrating_fertilizer"),
                    "Farmland never dries while crops are planted."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "bountiful_fertilizer"),
                    "Harvests 6 crops per plant, no seeds returned."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "low_quality_fertilizer"),
                    "Slightly increases crop and seed yield."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "high_quality_fertilizer"),
                    "Moderately increases crop and seed yield."
            );
            FERTILIZER_TOOLTIPS.put(
                    ResourceLocation.fromNamespaceAndPath("dew_drop_farmland_growth", "pristine_quality_fertilizer"),
                    "Greatly increases crop and seed yield."
            );
        }

        // --------------------------------------------------------------------
        //  TICK-LOGIK (wie v1.0) – NUR für extra_farmland (modded Farmlands)
        // --------------------------------------------------------------------

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel level)) return;

            ResourceKey<Level> dim = level.dimension();

            long gameTime = level.getGameTime();          // echte Ticks
            long dayTime  = level.getDayTime() % 24000L;  // 0–23999

            boolean inResetWindow = (dayTime >= 5L && dayTime <= 15L);
            boolean alreadyReset  = hasResetThisCycle.getOrDefault(dim, false);
            boolean doDailyReset  = inResetWindow && !alreadyReset;

            if (!inResetWindow && alreadyReset) {
                hasResetThisCycle.put(dim, false);
            }

            boolean isStabilizeTick        = (gameTime % 20L == 0L);
            boolean allowMoistureStabilize = !inResetWindow && isStabilizeTick;

            if (!doDailyReset && !allowMoistureStabilize) {
                return;
            }

            if (doDailyReset) {
                hasResetThisCycle.put(dim, true);
            }

            processExtraFarmland(level, doDailyReset, allowMoistureStabilize);
        }

        private static void processExtraFarmland(ServerLevel level,
                                                 boolean doDailyReset,
                                                 boolean stabilizeMoisture) {

            List<? extends Player> players = level.players();
            if (players.isEmpty()) return;

            int radius = 64;
            RegistryAccess ra = level.registryAccess();

            for (Player player : players) {
                BlockPos center = player.blockPosition();
                BlockPos min = center.offset(-radius, -radius, -radius);
                BlockPos max = center.offset(radius, radius, radius);

                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(EXTRA_FARMLAND_TAG)) continue; // nur unsere modded Farmlands

                    if (doDailyReset) {
                        handleFarmlandDailyReset(level, pos, state);
                    } else if (stabilizeMoisture) {
                        stabilizeFarmlandMoisture(level, pos, state);
                    }
                }
            }
        }

        /**
         * Daily Reset wie in v1.0:
         *
         * - Sprinkler:
         *      → Pflanze wächst +1
         *      → Moisture = MAX
         * - Kein Sprinkler:
         *      - Moisture > 0:
         *          → bei Regen wächst Pflanze +1
         *          → danach Moisture -> 0
         *      - Moisture == 0:
         *          → Farmland wird zu Bodenblock (Fallback)
         * - Farmland ohne Moisture-Property:
         *      → immer direkt zu Bodenblock
         */
        private static void handleFarmlandDailyReset(ServerLevel level, BlockPos pos, BlockState state) {

            boolean sprinkler = isFarmlandProtectedBySprinkler(level, pos);
            boolean raining   = level.isRainingAt(pos.above());

            if (sprinkler) {
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
                    if (raining) {
                        growCropAbove(level, pos);
                    }

                    // dann austrocknen, Farmland bleibt bestehen
                    BlockState newState = state.setValue(FarmBlock.MOISTURE, 0);
                    level.setBlockAndUpdate(pos, newState);
                } else {
                    // Bereits trocken → wird zu Bodenblock
                    BlockState newState = getFallbackSoilState(state);
                    level.setBlockAndUpdate(pos, newState);
                }
            } else {
                // Farmland ohne Moisture-Property → immer zu Bodenblock
                BlockState newState = getFallbackSoilState(state);
                level.setBlockAndUpdate(pos, newState);
            }
        }

        /**
         * Läuft ca. 1x pro Sekunde außerhalb des Reset-Fensters.
         * - Wenn Moisture == 0 und Regen -> Moisture = 7
         * - Wenn Moisture > 0 -> Moisture auf 7 halten
         */
        private static void stabilizeFarmlandMoisture(ServerLevel level, BlockPos pos, BlockState state) {
            if (!state.hasProperty(FarmBlock.MOISTURE)) return;

            int moisture = state.getValue(FarmBlock.MOISTURE);

            // 1) Trocken + Regen -> wieder bewässern
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

        // --------------------------------------------------------
        //  Sprinkler-Check wie gehabt
        // --------------------------------------------------------

        private static boolean isFarmlandProtectedBySprinkler(ServerLevel level, BlockPos farmlandPos) {
            int maxRadius = 4;

            for (int dy = 0; dy <= 1; dy++) { // gleiche Ebene und eine darüber
                for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                    for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                        BlockPos checkPos = farmlandPos.offset(dx, dy, dz);
                        BlockState checkState = level.getBlockState(checkPos);

                        if (checkState.is(SPRINKLER_TIER_1_TAG) ||
                                checkState.is(SPRINKLER_TIER_2_TAG) ||
                                checkState.is(SPRINKLER_TIER_3_TAG) ||
                                checkState.is(SPRINKLER_TIER_4_TAG)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        // --------------------------------------------------------
        //  Wachstumshilfe-Funktionen für modded Farmland
        // --------------------------------------------------------

        private static void growCropAbove(ServerLevel level, BlockPos farmlandPos) {
            BlockPos cropPos = farmlandPos.above();
            BlockState cropState = level.getBlockState(cropPos);

            if (!(cropState.getBlock() instanceof CropBlock cropBlock)) {
                return;
            }

            int age = cropBlock.getAge(cropState);
            int maxAge = cropBlock.getMaxAge();

            if (age < maxAge) {
                BlockState newState = cropBlock.getStateForAge(age + 1);
                level.setBlockAndUpdate(cropPos, newState);
            }
        }

        private static BlockState getFallbackSoilState(BlockState farmlandState) {
            Block block = farmlandState.getBlock();
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id == null) {
                return Blocks.DIRT.defaultBlockState();
            }

            String namespace = id.getNamespace();
            String path = id.getPath();

            // 1) *_farmland -> *_dirt
            if (path.endsWith("_farmland")) {
                String base = path.substring(0, path.length() - "_farmland".length());
                ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(namespace, base + "_dirt");
                Block candBlock = ForgeRegistries.BLOCKS.getValue(candidate);
                if (candBlock != null && candBlock != Blocks.AIR) {
                    return candBlock.defaultBlockState();
                }
            }

            // 2) heuristische Spezialfälle
            String[] soilKeys = {
                    "silt", "sandy_dirt", "loam", "rich_soil", "peat_dirt"
            };

            for (String key : soilKeys) {
                if (path.contains(key)) {
                    ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(namespace, key);
                    Block candBlock = ForgeRegistries.BLOCKS.getValue(candidate);
                    if (candBlock != null && candBlock != Blocks.AIR) {
                        return candBlock.defaultBlockState();
                    }
                }
            }

            // Fallback: Vanilla Dirt
            return Blocks.DIRT.defaultBlockState();
        }

        // --------------------------------------------------------
        //  Right-Click: Fertilizer wandelt extra_farmland in
        //  Sturdy-Farmland-Felder um (Chakyl kontrolliert dann alles)
        // --------------------------------------------------------

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Level level = event.getLevel();
            if (level.isClientSide()) return;
            if (!(level instanceof ServerLevel serverLevel)) return;

            BlockPos pos = event.getPos();
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.is(EXTRA_FARMLAND_TAG)) return;

            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            Player player = event.getEntity();
            if (player == null) return;

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) return;

            ResourceLocation targetId = FERTILIZER_TO_SOIL_BLOCK.get(itemId);
            if (targetId == null) return;

            Block targetBlock = ForgeRegistries.BLOCKS.getValue(targetId);
            if (targetBlock == null || targetBlock == Blocks.AIR) return;

            BlockState newState = targetBlock.defaultBlockState();
            serverLevel.setBlockAndUpdate(pos, newState);

            if (!player.isCreative()) {
                stack.shrink(1);
            }

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }

        // --------------------------------------------------------
        //  Bountiful & Quality-Farmland: Ernte-Boni
        // --------------------------------------------------------

        private enum FertilizerTier {
            NONE,
            BOUNTIFUL,
            LOW_QUALITY,
            HIGH_QUALITY,
            PRISTINE
        }

        private static FertilizerTier getFertilizerTierFromFarmland(BlockState farmland) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(farmland.getBlock());
            if (id == null) return FertilizerTier.NONE;

            String path = id.getPath().toLowerCase(Locale.ROOT);

            if (path.contains("bountiful_fertilized_farmland"))   return FertilizerTier.BOUNTIFUL;
            if (path.contains("low_quality_fertilized_farmland")) return FertilizerTier.LOW_QUALITY;
            if (path.contains("high_quality_fertilized_farmland"))return FertilizerTier.HIGH_QUALITY;
            if (path.contains("pristine_quality_fertilized_farmland")) return FertilizerTier.PRISTINE;

            return FertilizerTier.NONE;
        }

        @SubscribeEvent
        public static void onCropBreak(BlockEvent.BreakEvent event) {
            if (!(event.getLevel() instanceof Level level)) return;
            if (level.isClientSide()) return;

            BlockPos pos = event.getPos();
            BlockState cropState = level.getBlockState(pos);

            if (!(cropState.getBlock() instanceof CropBlock cropBlock)) {
                return;
            }

            // Nur voll ausgewachsene Pflanzen
            int age = cropBlock.getAge(cropState);
            int maxAge = cropBlock.getMaxAge();
            if (age < maxAge) {
                return;
            }

            BlockPos belowPos = pos.below();
            BlockState farmlandState = level.getBlockState(belowPos);
            FertilizerTier tier = getFertilizerTierFromFarmland(farmlandState);
            if (tier == FertilizerTier.NONE) {
                return;
            }

            if (!(level instanceof ServerLevel serverLevel)) return;
            RandomSource random = serverLevel.getRandom();

            Block cropBlockRaw = cropState.getBlock();

            // ---------------- Bountiful: genau 6 Früchte, keine Seeds ----------------
            if (tier == FertilizerTier.BOUNTIFUL) {
                event.setCanceled(true);
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

                int fruitCount = 6; // fix 6

                if (isVanillaCrop(cropBlockRaw)) {
                    if (cropBlockRaw == Blocks.WHEAT) {
                        dropItem(serverLevel, pos, Items.WHEAT, fruitCount);
                    } else if (cropBlockRaw == Blocks.CARROTS) {
                        dropItem(serverLevel, pos, Items.CARROT, fruitCount);
                    } else if (cropBlockRaw == Blocks.POTATOES) {
                        dropItem(serverLevel, pos, Items.POTATO, fruitCount);
                    } else if (cropBlockRaw == Blocks.BEETROOTS) {
                        dropItem(serverLevel, pos, Items.BEETROOT, fruitCount);
                    } else {
                        Item fallbackFruit = cropBlockRaw.asItem();
                        if (fallbackFruit != Items.AIR) {
                            dropItem(serverLevel, pos, fallbackFruit, fruitCount);
                        }
                    }
                } else {
                    // Modded Crop: ebenfalls komplett eigener Drop, keine Seeds
                    Item fruit = cropBlockRaw.asItem();
                    if (fruit != Items.AIR) {
                        dropItem(serverLevel, pos, fruit, fruitCount);
                    }
                }
                return;
            }

            // ---------------- Low / High / Pristine ----------------

            // Vanilla-Crops: Seeds + Früchte komplett selbst definieren
            if (isVanillaCrop(cropBlockRaw)) {
                event.setCanceled(true);
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

                Item fruitItem;
                Item seedItem;

                if (cropBlockRaw == Blocks.WHEAT) {
                    fruitItem = Items.WHEAT;
                    seedItem  = Items.WHEAT_SEEDS;
                } else if (cropBlockRaw == Blocks.CARROTS) {
                    fruitItem = Items.CARROT;
                    seedItem  = Items.CARROT;
                } else if (cropBlockRaw == Blocks.POTATOES) {
                    fruitItem = Items.POTATO;
                    seedItem  = Items.POTATO;
                } else if (cropBlockRaw == Blocks.BEETROOTS) {
                    fruitItem = Items.BEETROOT;
                    seedItem  = Items.BEETROOT_SEEDS;
                } else {
                    fruitItem = cropBlockRaw.asItem();
                    seedItem  = fruitItem;
                }

                // Basismengen (stabil)
                int baseFruit = 1;
                int baseSeeds = 1;

                int bonusFruitMin;
                int bonusFruitMax;
                int bonusSeedMin;
                int bonusSeedMax;

                switch (tier) {
                    case LOW_QUALITY:
                        bonusFruitMin = 1; bonusFruitMax = 1; // +1
                        bonusSeedMin  = 1; bonusSeedMax  = 1; // +1
                        break;
                    case HIGH_QUALITY:
                        bonusFruitMin = 2; bonusFruitMax = 3; // +2–3
                        bonusSeedMin  = 2; bonusSeedMax  = 3; // +2–3
                        break;
                    case PRISTINE:
                        bonusFruitMin = 4; bonusFruitMax = 6; // +4–6
                        bonusSeedMin  = 4; bonusSeedMax  = 6; // +4–6
                        break;
                    default:
                        bonusFruitMin = bonusFruitMax = 0;
                        bonusSeedMin  = bonusSeedMax  = 0;
                        break;
                }

                int bonusFruit = bonusFruitMin + random.nextInt(bonusFruitMax - bonusFruitMin + 1);
                int bonusSeeds = bonusSeedMin  + random.nextInt(bonusSeedMax  - bonusSeedMin  + 1);

                int totalFruit = baseFruit + bonusFruit;
                int totalSeeds = baseSeeds + bonusSeeds;

                if (totalFruit > 0) {
                    dropItem(serverLevel, pos, fruitItem, totalFruit);
                }
                if (totalSeeds > 0) {
                    dropItem(serverLevel, pos, seedItem, totalSeeds);
                }

                return;
            }

            // Nicht-Vanilla-Crops:
            // Normale Drops bleiben, wir addieren nur extra Früchte obendrauf.
            Item fruit = cropBlockRaw.asItem();
            if (fruit == Items.AIR) {
                return;
            }

            int bonusMin;
            int bonusMax;

            switch (tier) {
                case LOW_QUALITY:
                    bonusMin = 1; // +1 Frucht
                    bonusMax = 1;
                    break;
                case HIGH_QUALITY:
                    bonusMin = 2; // +2–3 Früchte
                    bonusMax = 3;
                    break;
                case PRISTINE:
                    bonusMin = 4; // +4–6 Früchte
                    bonusMax = 6;
                    break;
                default:
                    return;
            }

            int bonusCount = bonusMin + random.nextInt(bonusMax - bonusMin + 1);
            if (bonusCount > 0) {
                dropItem(serverLevel, pos, fruit, bonusCount);
            }
        }

        private static boolean isVanillaCrop(Block block) {
            return block == Blocks.WHEAT ||
                    block == Blocks.CARROTS ||
                    block == Blocks.POTATOES ||
                    block == Blocks.BEETROOTS;
        }

        private static void dropItem(ServerLevel level, BlockPos pos, Item item, int count) {
            if (count <= 0) return;
            ItemStack stack = new ItemStack(item, count);
            Block.popResource(level, pos, stack);
        }

        // --------------------------------------------------------
        //  Tooltips für Fertilizer-Items
        // --------------------------------------------------------

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            Item item = stack.getItem();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) return;

            String text = FERTILIZER_TOOLTIPS.get(id);
            if (text == null || text.isEmpty()) return;

            List<Component> tooltip = event.getToolTip();
            TooltipFlag flag = event.getFlags();

            // Eine zusätzliche, graue Zeile ans Ende anhängen
            tooltip.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
        }
    }
}
