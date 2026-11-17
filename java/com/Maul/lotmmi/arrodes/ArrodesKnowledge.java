package com.Maul.lotmmi.arrodes;

import com.mojang.datafixers.util.Pair;
import de.jakob.lotm.beyonders.potions.BeyonderPotion;
import de.jakob.lotm.beyonders.potions.PotionRecipe;
import de.jakob.lotm.beyonders.potions.PotionRecipes;
import de.jakob.lotm.item.ModIngredients;
import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.data.PathwayInfos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ArrodesKnowledge {

    public record PathwayMatch(String id, String name, int start, int end) {
    }

    public record SequenceMatch(String pathway, int sequence) {
    }

    private record IngredientEntry(Item item, String path, List<String> tokens) {
    }

    private static final Map<String, String> PATHWAY_EXTRAS = Map.of(
            "wheel_of_fortune", "monster wof",
            "hanged_man", "shepherd",
            "white_tower", "reader"
    );

    private static final Map<String, String> VANILLA_STRUCTURES = new LinkedHashMap<>();

    static {
        VANILLA_STRUCTURES.put("woodland mansion", "mansion");
        VANILLA_STRUCTURES.put("ocean monument", "monument");
        VANILLA_STRUCTURES.put("desert temple", "desert_pyramid");
        VANILLA_STRUCTURES.put("desert pyramid", "desert_pyramid");
        VANILLA_STRUCTURES.put("jungle temple", "jungle_pyramid");
        VANILLA_STRUCTURES.put("jungle pyramid", "jungle_pyramid");
        VANILLA_STRUCTURES.put("pillager outpost", "pillager_outpost");
        VANILLA_STRUCTURES.put("nether fortress", "fortress");
        VANILLA_STRUCTURES.put("trial chambers", "trial_chambers");
        VANILLA_STRUCTURES.put("trial chamber", "trial_chambers");
        VANILLA_STRUCTURES.put("ocean ruins", "ocean_ruin");
        VANILLA_STRUCTURES.put("ocean ruin", "ocean_ruin");
        VANILLA_STRUCTURES.put("buried treasure", "buried_treasure");
        VANILLA_STRUCTURES.put("ruined portal", "ruined_portal");
        VANILLA_STRUCTURES.put("ancient city", "ancient_city");
        VANILLA_STRUCTURES.put("trail ruins", "trail_ruins");
        VANILLA_STRUCTURES.put("end city", "end_city");
        VANILLA_STRUCTURES.put("witch hut", "swamp_hut");
        VANILLA_STRUCTURES.put("swamp hut", "swamp_hut");
        VANILLA_STRUCTURES.put("stronghold", "stronghold");
        VANILLA_STRUCTURES.put("shipwreck", "shipwreck");
        VANILLA_STRUCTURES.put("mineshaft", "mineshaft");
        VANILLA_STRUCTURES.put("bastion", "bastion_remnant");
        VANILLA_STRUCTURES.put("outpost", "pillager_outpost");
        VANILLA_STRUCTURES.put("fortress", "fortress");
        VANILLA_STRUCTURES.put("mansion", "mansion");
        VANILLA_STRUCTURES.put("monument", "monument");
        VANILLA_STRUCTURES.put("pyramid", "desert_pyramid");
        VANILLA_STRUCTURES.put("village", "village_");
        VANILLA_STRUCTURES.put("igloo", "igloo");
    }

    private static final String HINT_PREFIX = "ingredient.lotm.hint.";

    private static List<IngredientEntry> ingredientCache;
    private static List<SequenceName> sequenceCache;
    private static int sequenceCacheSize = -1;

    private record SequenceName(String pathway, int sequence, List<String> tokens) {
    }

    public record MobMatch(List<EntityType<?>> types, boolean exact) {
    }

    public record SpawnInfo(String where, int minPercent, int maxPercent, int minGroup, int maxGroup, int weight, String pool) {
        public int averagePercent() {
            return (minPercent + maxPercent) / 2;
        }

        public String percentText() {
            return minPercent == maxPercent ? String.valueOf(minPercent) : minPercent + " to " + maxPercent;
        }

        public String groupText() {
            return minGroup == maxGroup ? String.valueOf(minGroup) : minGroup + " to " + maxGroup;
        }
    }

    private record MobEntry(EntityType<?> type, List<String> tokens) {
    }

    private static final Set<String> GENERIC_MOB_WORDS = Set.of("spirit", "entity");

    private static List<MobEntry> mobCache;

    /** Creatures and structures from other mods share words with ours ("dragon"), so only our own mods are searched. */
    private static final Set<String> LOTM_NAMESPACES = Set.of("lotmcraft", "lotmextra", "lotmmi");

    static boolean isLotmNamespace(String namespace) {
        return LOTM_NAMESPACES.contains(namespace);
    }

    private ArrodesKnowledge() {
    }

    public static Map<String, PathwayInfos> pathways() {
        return BeyonderData.pathwayInfos;
    }

    public static String pathwayName(String id) {
        PathwayInfos info = BeyonderData.pathwayInfos.get(id);
        if (info != null) {
            try {
                String name = info.getName();
                if (name != null && !name.isBlank()) return name.replaceAll("§.", "");
            } catch (Throwable ignored) {
            }
        }
        return ArrodesText.pretty(id);
    }

    public static String sequenceName(String pathway, int sequence) {
        try {
            String name = BeyonderData.getSequenceName(pathway, sequence);
            if (name != null && !name.isBlank() && !name.equals("Unknown")) return name.replaceAll("§.", "");
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String describeSequence(String pathway, int sequence) {
        String name = sequenceName(pathway, sequence);
        return pathwayName(pathway) + " Sequence " + sequence + (name == null ? "" : " (" + name + ")");
    }

    public static PathwayMatch findPathway(List<String> tokens) {
        PathwayMatch best = null;
        int bestLength = 0;
        for (Map.Entry<String, PathwayInfos> entry : BeyonderData.pathwayInfos.entrySet()) {
            String id = entry.getKey();
            List<String> aliases = new ArrayList<>();
            aliases.add(ArrodesText.normalize(id));
            aliases.add(ArrodesText.normalize(pathwayName(id)));
            String extra = PATHWAY_EXTRAS.get(id);
            if (extra != null) for (String alias : extra.split(" ")) aliases.add(alias);
            if (id.equals("wheel_of_fortune")) aliases.add("wheel of fortune");
            for (String alias : aliases) {
                if (alias.isEmpty()) continue;
                String[] parts = alias.split(" ");
                for (int i = 0; i + parts.length <= tokens.size(); i++) {
                    boolean ok = true;
                    for (int j = 0; j < parts.length && ok; j++) {
                        ok = ArrodesText.fuzzyEquals(tokens.get(i + j), parts[j]);
                    }
                    int length = alias.length();
                    if (ok && length > bestLength) {
                        best = new PathwayMatch(id, pathwayName(id), i, i + parts.length);
                        bestLength = length;
                    }
                }
            }
        }
        return best;
    }

    private static List<SequenceName> sequenceNames() {
        if (sequenceCache != null && sequenceCacheSize == BeyonderData.pathwayInfos.size()) return sequenceCache;
        List<SequenceName> out = new ArrayList<>();
        for (String id : BeyonderData.pathwayInfos.keySet()) {
            for (int seq = 0; seq <= 9; seq++) {
                String name = sequenceName(id, seq);
                if (name == null) continue;
                List<String> tokens = new ArrayList<>();
                for (String token : ArrodesText.tokens(name)) if (!ArrodesText.isStop(token)) tokens.add(token);
                if (!tokens.isEmpty()) out.add(new SequenceName(id, seq, tokens));
            }
        }
        sequenceCache = out;
        sequenceCacheSize = BeyonderData.pathwayInfos.size();
        return out;
    }

    public static SequenceMatch findSequenceByName(List<String> tokens, String onlyPathway) {
        SequenceMatch best = null;
        int bestLength = 0;
        for (SequenceName entry : sequenceNames()) {
            if (onlyPathway != null && !entry.pathway().equals(onlyPathway)) continue;
            List<String> parts = entry.tokens();
            for (int i = 0; i + parts.size() <= tokens.size(); i++) {
                boolean ok = true;
                for (int j = 0; j < parts.size() && ok; j++) ok = ArrodesText.fuzzyEquals(tokens.get(i + j), parts.get(j));
                int length = String.join(" ", parts).length();
                if (ok && length > bestLength) {
                    best = new SequenceMatch(entry.pathway(), entry.sequence());
                    bestLength = length;
                }
            }
        }
        return best;
    }

    public static List<SequenceMatch> findAllSequencesByName(List<String> tokens) {
        List<SequenceMatch> out = new ArrayList<>();
        int bestLength = 0;
        for (SequenceName entry : sequenceNames()) {
            List<String> parts = entry.tokens();
            for (int i = 0; i + parts.size() <= tokens.size(); i++) {
                boolean ok = true;
                for (int j = 0; j < parts.size() && ok; j++) ok = ArrodesText.fuzzyEquals(tokens.get(i + j), parts.get(j));
                if (!ok) continue;
                int length = String.join(" ", parts).length();
                if (length > bestLength) {
                    out.clear();
                    bestLength = length;
                }
                if (length == bestLength) {
                    SequenceMatch match = new SequenceMatch(entry.pathway(), entry.sequence());
                    if (!out.contains(match)) out.add(match);
                }
                break;
            }
        }
        return out;
    }

    public static String hintOf(Item item) {
        try {
            String key = HINT_PREFIX + BuiltInRegistries.ITEM.getKey(item).getPath();
            Language language = Language.getInstance();
            if (!language.has(key)) return null;
            String text = language.getOrDefault(key).replaceAll("§.", "").replaceAll("\\s+", " ").trim();
            return text.isEmpty() ? null : text;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String firstSource(Item item) {
        String hint = hintOf(item);
        if (hint != null) return hint;
        List<String> rules = sourcesOf(item);
        return rules.isEmpty() ? null : rules.get(0);
    }

    private static List<IngredientEntry> ingredients() {
        if (ingredientCache != null) return ingredientCache;
        List<IngredientEntry> out = new ArrayList<>();
        for (var holder : ModIngredients.ITEMS.getEntries()) {
            Item item = holder.get();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            List<String> tokens = new ArrayList<>();
            for (String token : ArrodesText.tokens(path.replace('_', ' '))) if (!ArrodesText.isStop(token)) tokens.add(token);
            if (!tokens.isEmpty()) out.add(new IngredientEntry(item, path, tokens));
        }
        ingredientCache = out;
        return out;
    }

    public static Item findIngredient(List<String> tokens) {
        Item best = null;
        double bestScore = 0.0D;
        int bestSize = 0;
        for (IngredientEntry entry : ingredients()) {
            int matched = 0;
            for (String part : entry.tokens()) {
                for (String token : tokens) {
                    if (ArrodesText.fuzzyEquals(token, part)) {
                        matched++;
                        break;
                    }
                }
            }
            double score = (double) matched / entry.tokens().size();
            boolean oneWordShort = matched >= 3 && entry.tokens().size() - matched == 1;
            if ((score >= 0.8D || oneWordShort) && (score > bestScore || (score == bestScore && entry.tokens().size() > bestSize))) {
                best = entry.item();
                bestScore = score;
                bestSize = entry.tokens().size();
            }
        }
        return best;
    }

    public static String itemName(Item item) {
        return ArrodesText.pretty(BuiltInRegistries.ITEM.getKey(item).getPath());
    }

    private static void ensureRecipes() {
        if (!PotionRecipes.initialized) PotionRecipes.initPotionRecipes();
    }

    public static PotionRecipe recipeFor(String pathway, int sequence) {
        ensureRecipes();
        for (PotionRecipe recipe : PotionRecipes.RECIPES) {
            BeyonderPotion potion = recipe.potion();
            if (potion != null && potion.getPathway().equals(pathway) && potion.getSequence() == sequence) return recipe;
        }
        return null;
    }

    public static List<String> describeRecipe(PotionRecipe recipe) {
        List<String> lines = new ArrayList<>();
        ItemStack main = recipe.mainIngredient();
        if (main != null && !main.isEmpty()) lines.add("Main ingredient: " + stackName(main));
        List<String> supplementary = new ArrayList<>();
        for (ItemStack stack : recipe.supplementaryIngredients()) {
            if (stack != null && !stack.isEmpty()) supplementary.add(stackName(stack));
        }
        if (!supplementary.isEmpty()) lines.add("Supplementary: " + String.join(", ", supplementary));
        return lines;
    }

    private static String stackName(ItemStack stack) {
        String name = itemName(stack.getItem());
        return stack.getCount() > 1 ? stack.getCount() + "x " + name : name;
    }

    public static final int HIDDEN_FORMULA_SEQUENCE = 3;
    public static final int PROTECTED_BEING_SEQUENCE = 2;

    public static boolean formulaHidden(int sequence) {
        return sequence >= 0 && sequence <= HIDDEN_FORMULA_SEQUENCE;
    }

    public static boolean beingProtected(int sequence) {
        return sequence >= 0 && sequence <= PROTECTED_BEING_SEQUENCE;
    }

    public static List<String> recipesUsing(Item item) {
        ensureRecipes();
        List<String> out = new ArrayList<>();
        for (PotionRecipe recipe : PotionRecipes.RECIPES) {
            if (recipe.potion() != null && formulaHidden(recipe.potion().getSequence())) continue;
            boolean uses = recipe.mainIngredient() != null && recipe.mainIngredient().is(item);
            for (ItemStack stack : recipe.supplementaryIngredients()) {
                if (stack != null && stack.is(item)) uses = true;
            }
            if (uses && recipe.potion() != null) {
                out.add(describeSequence(recipe.potion().getPathway(), recipe.potion().getSequence()));
            }
        }
        return out;
    }

    private static List<?> staticList(String className, String field) throws Exception {
        Field f = Class.forName(className).getDeclaredField(field);
        f.setAccessible(true);
        return (List<?>) f.get(null);
    }

    private static int staticInt(String className, String field) {
        try {
            Field f = Class.forName(className).getDeclaredField(field);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static Object call(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static boolean resultMatches(Object rule, String path) throws Exception {
        String result = String.valueOf(call(rule, "result"));
        return result.equals(path) || result.endsWith(":" + path) || result.endsWith("/" + path);
    }

    private static int percent(Object rule) {
        try {
            return Math.max(1, Math.round(((Number) call(rule, "chance")).floatValue() * 100.0F));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String entityName(Object type) {
        try {
            return ArrodesText.pretty(EntityType.getKey((EntityType<?>) type).getPath());
        } catch (Throwable ignored) {
            return "something";
        }
    }

    public static List<String> sourcesOf(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        List<String> out = new ArrayList<>();
        String drops = "de.jakob.lotm.item.IngredientDrops";
        String sources = "de.jakob.lotm.item.IngredientSources";

        try {
            for (Object rule : staticList(drops, "MOB_DROPS")) {
                if (!resultMatches(rule, path)) continue;
                boolean bottle = (Boolean) call(rule, "bottle");
                out.add("Taken from " + entityName(call(rule, "type")) + (bottle ? " with a glass bottle" : " when it dies")
                        + " (about " + percent(rule) + "%)");
            }
        } catch (Throwable ignored) {
        }

        try {
            int shovel = staticInt(drops, "SHOVEL");
            int shears = staticInt(drops, "SHEARS");
            for (Object rule : staticList(drops, "BLOCK_DROPS")) {
                if (!resultMatches(rule, path)) continue;
                List<String> blocks = new ArrayList<>();
                for (Object block : (Iterable<?>) call(rule, "blocks")) {
                    if (blocks.size() >= 4) break;
                    blocks.add(ArrodesText.pretty(BuiltInRegistries.BLOCK.getKey((Block) block).getPath()));
                }
                int tool = ((Number) call(rule, "tool")).intValue();
                String how = tool == shovel ? " with a shovel" : tool == shears ? " with shears" : "";
                boolean natural = (Boolean) call(rule, "natural");
                out.add("Dropped by breaking " + String.join(" or ", blocks) + how + (natural ? " (naturally generated only)" : "")
                        + " (about " + percent(rule) + "%)");
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Object rule : staticList(drops, "LOOT_DROPS")) {
                if (!resultMatches(rule, path)) continue;
                List<String> tables = new ArrayList<>();
                for (Object table : (Iterable<?>) call(rule, "tables")) {
                    if (tables.size() >= 4) break;
                    String text = String.valueOf(table);
                    int slash = Math.max(text.lastIndexOf('/'), text.lastIndexOf(':'));
                    text = text.substring(slash + 1).replace("]", "").trim();
                    tables.add(ArrodesText.pretty(text));
                }
                out.add("Found in chests of: " + String.join(", ", tables) + " (about " + percent(rule) + "%)");
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Object rule : staticList(sources, "RULES")) {
                if (!resultMatches(rule, path)) continue;
                boolean fluid = (Boolean) call(rule, "fluid");
                boolean bottle = (Boolean) call(rule, "waterBottle");
                out.add((bottle ? "Gathered by using a water bottle where the conditions are right"
                        : fluid ? "Collected from a fluid in the right place"
                        : "Gathered by a deed in the right place") + " (about " + percent(rule) + "%)");
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Object rule : staticList(sources, "ENTITY_RULES")) {
                if (!resultMatches(rule, path)) continue;
                boolean sneak = (Boolean) call(rule, "sneak");
                out.add("Taken from " + entityName(call(rule, "type")) + " by " + (sneak ? "sneaking and " : "")
                        + "using a glass bottle on it (about " + percent(rule) + "%)");
            }
        } catch (Throwable ignored) {
        }

        return out;
    }

    private static List<MobEntry> mobs() {
        if (mobCache != null) return mobCache;
        List<MobEntry> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!isLotmNamespace(key.getNamespace()) || type.getCategory() == MobCategory.MISC) continue;
            List<String> tokens = new ArrayList<>();
            for (String token : ArrodesText.tokens(key.getPath().replace('_', ' '))) {
                if (!ArrodesText.isStop(token)) tokens.add(token);
            }
            if (!tokens.isEmpty()) out.add(new MobEntry(type, tokens));
        }
        mobCache = out;
        return out;
    }

    private static boolean isMob(ServerLevel level, EntityType<?> type) {
        try {
            Entity entity = type.create(level);
            if (entity == null) return false;
            boolean mob = entity instanceof Mob;
            entity.discard();
            return mob;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static MobMatch findMobs(ServerLevel level, List<String> tokens) {
        double best = 0.0D;
        List<EntityType<?>> found = new ArrayList<>();
        for (MobEntry entry : mobs()) {
            int core = 0;
            int matched = 0;
            for (String part : entry.tokens()) {
                if (GENERIC_MOB_WORDS.contains(part)) continue;
                core++;
                for (String token : tokens) {
                    if (ArrodesText.fuzzyEquals(token, part)) {
                        matched++;
                        break;
                    }
                }
            }
            if (core == 0 || matched == 0) continue;
            double score = (double) matched / core;
            if (score < 0.5D) continue;
            if (score > best + 1.0E-9D) {
                best = score;
                found.clear();
            }
            if (Math.abs(score - best) < 1.0E-9D) found.add(entry.type());
        }
        List<EntityType<?>> real = new ArrayList<>();
        for (EntityType<?> type : found) {
            if (!spawnInfo(level, type).isEmpty() || isMob(level, type)) real.add(type);
        }
        if (real.isEmpty()) return null;
        return new MobMatch(real, best >= 1.0D - 1.0E-9D);
    }

    public static String mobName(EntityType<?> type) {
        try {
            String name = type.getDescription().getString();
            if (name != null && !name.isBlank() && !name.startsWith("entity.")) return name;
        } catch (Throwable ignored) {
        }
        return ArrodesText.pretty(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }

    public static List<EntityType<?>> mobsFor(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        List<EntityType<?>> out = new ArrayList<>();
        collectMobs(out, "de.jakob.lotm.item.IngredientDrops", "MOB_DROPS", path);
        collectMobs(out, "de.jakob.lotm.item.IngredientSources", "ENTITY_RULES", path);
        return out;
    }

    private static void collectMobs(List<EntityType<?>> out, String className, String field, String path) {
        try {
            for (Object rule : staticList(className, field)) {
                if (!resultMatches(rule, path)) continue;
                if (call(rule, "type") instanceof EntityType<?> type && !out.contains(type)) out.add(type);
            }
        } catch (Throwable ignored) {
        }
    }

    public static List<String> dropsOf(EntityType<?> type) {
        List<String> out = new ArrayList<>();
        collectDrops(out, "de.jakob.lotm.item.IngredientDrops", "MOB_DROPS", type);
        collectDrops(out, "de.jakob.lotm.item.IngredientSources", "ENTITY_RULES", type);
        return out;
    }

    private static void collectDrops(List<String> out, String className, String field, EntityType<?> type) {
        try {
            for (Object rule : staticList(className, field)) {
                if (call(rule, "type") != type) continue;
                String name = ArrodesText.pretty(String.valueOf(call(rule, "result")));
                if (!name.isEmpty() && !out.contains(name)) out.add(name);
            }
        } catch (Throwable ignored) {
        }
    }

    public static String rarity(int percent) {
        if (percent >= 30) return "very common";
        if (percent >= 12) return "common";
        if (percent >= 5) return "uncommon";
        if (percent >= 2) return "rare";
        return "very rare";
    }

    private static String poolName(MobCategory category) {
        return switch (category) {
            case MONSTER -> "monsters";
            case CREATURE -> "creatures";
            case AMBIENT -> "ambient creatures";
            case WATER_CREATURE -> "water creatures";
            case WATER_AMBIENT -> "water ambient creatures";
            default -> "mobs";
        };
    }

    private static boolean isSpirit(ResourceLocation id) {
        return id.getNamespace().equals("lotmcraft") && id.getPath().startsWith("spirit_");
    }

    private static Set<ResourceLocation> tagMembers(Registry<Biome> registry, String tagId) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        try {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", tagId));
            Optional<HolderSet.Named<Biome>> named = registry.getTag(tag);
            if (named.isPresent()) {
                for (Holder<Biome> holder : named.get()) {
                    holder.unwrapKey().ifPresent(key -> out.add(key.location()));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String biomeNames(Collection<ResourceLocation> ids, String strip) {
        List<String> names = new ArrayList<>();
        for (ResourceLocation id : ids) {
            String path = id.getPath();
            if (!strip.isEmpty() && path.startsWith(strip)) path = path.substring(strip.length());
            names.add(ArrodesText.pretty(path));
        }
        if (names.size() <= 5) return String.join(", ", names);
        return String.join(", ", names.subList(0, 5)) + " and " + (names.size() - 5) + " more";
    }

    private static String describeBiomes(Registry<Biome> registry, Set<ResourceLocation> hit) {
        List<String> parts = new ArrayList<>();
        Set<ResourceLocation> left = new LinkedHashSet<>(hit);

        Set<ResourceLocation> allSpirit = new LinkedHashSet<>();
        for (ResourceKey<Biome> key : registry.registryKeySet()) {
            if (isSpirit(key.location())) allSpirit.add(key.location());
        }
        Set<ResourceLocation> spirit = new LinkedHashSet<>();
        for (ResourceLocation id : hit) {
            if (isSpirit(id)) spirit.add(id);
        }
        if (!spirit.isEmpty()) {
            parts.add(spirit.size() == allSpirit.size()
                    ? "every region of the Spirit World"
                    : "the Spirit World (" + biomeNames(spirit, "spirit_") + ")");
            left.removeAll(spirit);
        }

        Set<ResourceLocation> overworld = tagMembers(registry, "is_overworld");
        if (!overworld.isEmpty()) {
            int covered = 0;
            for (ResourceLocation id : overworld) {
                if (left.contains(id)) covered++;
            }
            double share = (double) covered / overworld.size();
            if (share >= 0.85D) {
                parts.add(share >= 0.99D ? "anywhere in the Overworld" : "almost anywhere in the Overworld");
                left.removeAll(overworld);
            }
        }

        Set<ResourceLocation> ocean = tagMembers(registry, "is_ocean");
        if (!ocean.isEmpty() && left.containsAll(ocean)) {
            parts.add("every ocean");
            left.removeAll(ocean);
        }
        Set<ResourceLocation> nether = tagMembers(registry, "is_nether");
        if (!nether.isEmpty() && left.containsAll(nether)) {
            parts.add("the whole Nether");
            left.removeAll(nether);
        }
        Set<ResourceLocation> end = tagMembers(registry, "is_end");
        if (!end.isEmpty() && left.containsAll(end)) {
            parts.add("the End");
            left.removeAll(end);
        }

        if (!left.isEmpty()) parts.add(biomeNames(left, ""));
        return parts.isEmpty() ? "places unknown to me" : String.join("; ", parts);
    }

    private record BiomeSpawn(int percent, int minCount, int maxCount, int weight) {
    }

    public static List<SpawnInfo> spawnInfo(ServerLevel level, EntityType<?> type) {
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Map<ResourceLocation, BiomeSpawn> found = new LinkedHashMap<>();
        for (ResourceKey<Biome> key : registry.registryKeySet()) {
            Biome biome = registry.get(key);
            if (biome == null) continue;
            MobSpawnSettings settings = biome.getMobSettings();
            for (MobCategory pool : MobCategory.values()) {
                int total = 0;
                MobSpawnSettings.SpawnerData mine = null;
                for (MobSpawnSettings.SpawnerData data : settings.getMobs(pool).unwrap()) {
                    total += data.getWeight().asInt();
                    if (data.type == type) mine = data;
                }
                if (mine == null || total <= 0) continue;
                int percent = Math.max(1, (int) Math.round(100.0D * mine.getWeight().asInt() / total));
                found.put(key.location(), new BiomeSpawn(percent, mine.minCount, mine.maxCount, mine.getWeight().asInt()));
            }
        }

        Set<ResourceLocation> spirit = new LinkedHashSet<>();
        Set<ResourceLocation> rest = new LinkedHashSet<>();
        for (ResourceLocation id : found.keySet()) {
            if (isSpirit(id)) spirit.add(id);
            else rest.add(id);
        }

        List<SpawnInfo> out = new ArrayList<>();
        for (Set<ResourceLocation> area : List.of(spirit, rest)) {
            if (area.isEmpty()) continue;
            int minPercent = 101;
            int maxPercent = 0;
            int minGroup = Integer.MAX_VALUE;
            int maxGroup = 0;
            int weight = 0;
            for (ResourceLocation id : area) {
                BiomeSpawn spawn = found.get(id);
                minPercent = Math.min(minPercent, spawn.percent());
                maxPercent = Math.max(maxPercent, spawn.percent());
                minGroup = Math.min(minGroup, spawn.minCount());
                maxGroup = Math.max(maxGroup, spawn.maxCount());
                weight = Math.max(weight, spawn.weight());
            }
            out.add(new SpawnInfo(describeBiomes(registry, area), minPercent, maxPercent, minGroup, maxGroup, weight,
                    poolName(type.getCategory())));
        }
        return out;
    }

    public static String structurePrefixFor(String normalizedText, ServerLevel level) {
        String padded = " " + normalizedText + " ";
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<String, String> aliases = new LinkedHashMap<>(VANILLA_STRUCTURES);
        for (ResourceKey<Structure> key : registry.registryKeySet()) {
            if (key.location().getNamespace().equals("minecraft") || !isLotmNamespace(key.location().getNamespace())) continue;
            String path = key.location().getPath();
            aliases.put(path.replace('_', ' '), path);
        }
        String best = null;
        int bestLength = 0;
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String alias = entry.getKey();
            boolean hit = padded.contains(" " + alias + " ") || padded.contains(" " + alias + "s ");
            if (hit && alias.length() > bestLength) {
                best = entry.getValue();
                bestLength = alias.length();
            }
        }
        return best;
    }

    public static String locateStructure(ServerPlayer player, String prefix) {
        ServerLevel level = player.serverLevel();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();
        for (ResourceKey<Structure> key : registry.registryKeySet()) {
            String namespace = key.location().getNamespace();
            if (!namespace.equals("minecraft") && !isLotmNamespace(namespace)) continue;
            if (key.location().getPath().startsWith(prefix)) holders.add(registry.getHolderOrThrow(key));
        }
        if (holders.isEmpty()) return null;

        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holders), player.blockPosition(), 64, false);
        if (found == null) return null;

        BlockPos pos = found.getFirst();
        double dx = pos.getX() - player.getX();
        double dz = pos.getZ() - player.getZ();
        String name = found.getSecond().unwrapKey().map(k -> ArrodesText.pretty(k.location().getPath())).orElse("structure");
        return "The nearest " + name + " lies about " + Math.round(Math.sqrt(dx * dx + dz * dz))
                + " blocks to the " + ArrodesText.direction(dx, dz) + ", near X " + pos.getX() + ", Z " + pos.getZ() + ".";
    }
}
