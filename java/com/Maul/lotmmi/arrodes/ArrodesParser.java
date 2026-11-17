package com.Maul.lotmmi.arrodes;

import com.Maul.lotmmi.arrodes.ArrodesKnowledge.MobMatch;
import com.Maul.lotmmi.arrodes.ArrodesKnowledge.PathwayMatch;
import com.Maul.lotmmi.arrodes.ArrodesKnowledge.SequenceMatch;
import com.Maul.lotmmi.arrodes.ArrodesKnowledge.SpawnInfo;
import com.Maul.lotmmi.config.LotmmiConfig;
import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.attachments.ModAttachments;
import de.jakob.lotm.attachments.SefirotData;
import de.jakob.lotm.attachments.UniquenessComponent;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.potions.BeyonderCharacteristicItem;
import de.jakob.lotm.beyonders.potions.BeyonderPotion;
import de.jakob.lotm.beyonders.potions.PotionRecipe;
import de.jakob.lotm.beyonders.potions.PotionRecipeItem;
import de.jakob.lotm.entity.custom.uniqueness.UniquenessEntity;
import de.jakob.lotm.gamerule.ModGameRules;
import de.jakob.lotm.item.PotionIngredient;
import de.jakob.lotm.item.custom.UniquenessItem;
import de.jakob.lotm.network.packets.toServer.RequestUniquenessApotheosisPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArrodesParser {

    public enum Kind { ANSWER, REFUSE, CLARIFY, UNKNOWN, SMITE }

    public record Result(Kind kind, List<String> lines) {
        static Result answer(List<String> lines) {
            return new Result(Kind.ANSWER, lines);
        }

        static Result answer(String line) {
            return new Result(Kind.ANSWER, List.of(line));
        }

        static Result refuse(String line) {
            return new Result(Kind.REFUSE, List.of(line));
        }

        static Result clarify(String line) {
            return new Result(Kind.CLARIFY, List.of(line));
        }

        static Result smite(String line) {
            return new Result(Kind.SMITE, List.of(line));
        }
    }

    private enum Intent { RECIPE, ABILITIES, SEQ_NAME, SOURCE, USES, STRUCTURE, NEARBY, UNIQUENESS, DEATH, PLAYER, MOB, APPRAISE, DANGER, ABOUT, NPC, LORE }

    private record Context(Intent intent, String pathway, int sequence, long expires) {
    }

    private static final Map<UUID, Context> CONTEXT = new ConcurrentHashMap<>();

    private ArrodesParser() {
    }

    public static void forget(UUID id) {
        CONTEXT.remove(id);
    }

    public static Result parse(ServerPlayer player, String text, boolean owner) {
        MinecraftServer server = player.getServer();
        if (server == null) return Result.refuse("The mirror is clouded.");
        ServerLevel level = player.serverLevel();
        long now = server.overworld().getGameTime();

        String normalized = ArrodesText.normalize(text);
        List<String> tokens = ArrodesText.tokens(text);
        String address = owner ? "Great Master" : "mortal";
        if (tokens.isEmpty()) return new Result(Kind.UNKNOWN, List.of("Speak, " + address + "."));

        Context context = CONTEXT.get(player.getUUID());
        if (context != null && context.expires() < now) {
            CONTEXT.remove(player.getUUID());
            context = null;
        }

        PathwayMatch pathwayMatch = ArrodesKnowledge.findPathway(tokens);
        int sequence = ArrodesText.findSequence(tokens);
        String pathway = pathwayMatch == null ? null : pathwayMatch.id();

        List<SequenceMatch> ambiguous = List.of();
        if (pathwayMatch == null) {
            List<SequenceMatch> named = ArrodesKnowledge.findAllSequencesByName(tokens);
            if (sequence >= 0 && named.size() > 1) {
                final int wanted = sequence;
                List<SequenceMatch> narrowed = new ArrayList<>();
                for (SequenceMatch candidate : named) if (candidate.sequence() == wanted) narrowed.add(candidate);
                if (!narrowed.isEmpty()) named = narrowed;
            }
            if (named.size() == 1) {
                SequenceMatch byName = named.get(0);
                pathway = byName.pathway();
                if (sequence < 0) sequence = byName.sequence();
            } else if (named.size() > 1) {
                ambiguous = named;
            }
        } else if (sequence < 0) {
            List<String> masked = new ArrayList<>(tokens);
            for (int i = pathwayMatch.start(); i < pathwayMatch.end(); i++) masked.set(i, "_");
            SequenceMatch byName = ArrodesKnowledge.findSequenceByName(masked, pathway);
            if (byName != null) sequence = byName.sequence();
        }
        boolean explicitPathway = pathway != null || !ambiguous.isEmpty();

        boolean recipeW = ArrodesText.hasAny(tokens, normalized, "recipe", "recipes", "formula", "brew", "ingredients", "ingredient", "concoct", "potion", "craft", "make");
        boolean sourceW = ArrodesText.hasAny(tokens, normalized, "where", "get", "find", "obtain", "drop", "drops", "farm", "source", "acquire", "collect", "gather", "harvest", "locate", "buy", "trade");
        boolean nameW = ArrodesText.hasAny(tokens, normalized, "name", "names", "called", "title", "titles");
        boolean abilityW = ArrodesText.hasAny(tokens, normalized, "abilities", "ability", "powers", "power", "skills", "skill", "spells", "spell");
        boolean whereW = ArrodesText.hasAny(tokens, normalized, "where", "find", "locate", "nearest", "closest", "near");
        boolean nearbyW = ArrodesText.hasAny(tokens, normalized, "who", "anyone", "anybody", "beyonders", "beyonder", "nearby", "around", "lurking", "close");
        boolean deathW = ArrodesText.hasAny(tokens, normalized, "died", "die", "dead", "killed", "last death", "my death", "my grave");
        boolean uniqW = ArrodesText.hasAny(tokens, normalized, "uniqueness", "uniquenesses");
        boolean apprW = ArrodesText.hasAny(tokens, normalized, "appraise", "identify", "examine", "inspect", "what is this", "this item", "in my hand", "what does this do");
        boolean dangerW = ArrodesText.hasAny(tokens, normalized, "danger", "dangers", "dangerous", "safe", "threat", "threats", "warn", "warning", "peril", "enemies", "hostile");
        boolean usesW = ArrodesText.hasAny(tokens, normalized, "use", "uses", "used", "using", "serve", "serves", "require", "requires", "required",
                "goes into", "go into", "part of");
        boolean godW = ArrodesText.hasAny(tokens, normalized, "god", "gods", "deity", "deities", "creator", "evernight", "ouroboros", "amon", "adam");
        boolean puzzleW = ArrodesText.hasAny(tokens, normalized, "paradox", "paradoxes", "unsolved", "unproven", "conjecture", "conjectures",
                "riemann", "goldbach", "collatz", "millennium", "poincare", "navier", "zeno", "schrodinger", "russell",
                "halting problem", "twin prime", "twin primes", "p vs np", "p versus np", "ship of theseus", "trolley problem",
                "mathematical problem", "math problem", "chicken or the egg");
        boolean illW = ArrodesText.hasAny(tokens, normalized, "kill", "kills", "killing", "murder", "assassinate", "assassination", "curse",
                "eliminate", "hunt", "hunting", "ambush", "rob", "robbing", "slay", "stab", "poison", "torture", "enslave", "backstab", "harass");
        boolean playerW = ArrodesText.hasAny(tokens, normalized, "player", "players", "someone", "somebody", "person", "people", "victim", "victims",
                "rival", "rivals", "pvp", "pk", "griefer");
        boolean trickW = ArrodesText.hasAny(tokens, normalized, "loophole", "loopholes", "jailbreak", "ignore your rules", "ignore previous",
                "ignore all previous", "ignore your instructions", "forget your rules", "forget your instructions", "override your");
        boolean closeW = ArrodesText.hasAny(tokens, normalized, "nearby", "around", "lurking", "close", "near");
        boolean aboutW = ArrodesText.hasAny(tokens, normalized, "who are you", "what are you", "who is arrodes", "what is arrodes",
                "tell me about yourself", "about yourself", "introduce yourself", "what can you do", "what do you do", "what can arrodes do",
                "your purpose", "who made you", "who created you", "where do you come from", "where did you come from", "where are you from",
                "your origin", "your origins", "your history", "your creator", "your maker", "where were you born", "your birthplace", "your homeland",
                "where do you live", "how were you made", "how did you come to be");
        boolean infoW = ArrodesText.hasAny(tokens, normalized, "pathway", "pathways", "path", "sequence", "seq", "rank", "level", "tier",
                "strong", "stronger", "strongest", "strength", "powerful", "weak", "weaker", "beyonder", "beyonders", "details", "detail",
                "info", "information", "identity", "about", "know", "walks", "walk", "who is", "what is");
        boolean mobW = ArrodesText.hasAny(tokens, normalized, "spawn", "spawns", "spawned", "spawning", "appear", "appears", "roam", "roams",
                "habitat", "live", "lives", "often", "rare", "rarely", "common", "commonly", "frequent", "frequently", "chance", "biome",
                "biomes", "about", "know", "yield", "yields", "hunting", "encounter", "who is", "what is");
        boolean explicit = recipeW || sourceW || nameW || abilityW || whereW || nearbyW || deathW || uniqW || apprW || dangerW;

        String structurePrefix = ArrodesKnowledge.structurePrefixFor(normalized, level);
        ServerPlayer target = findTarget(server, player, normalized);
        Item ingredient = ArrodesKnowledge.findIngredient(tokens);
        ArrodesNpcs.Npc npc = target == null ? ArrodesNpcs.find(tokens) : null;
        ArrodesLore.Topic loreTopic = ArrodesLore.find(tokens, normalized);
        MobMatch mobMatch = ingredient == null && !explicitPathway && npc == null ? ArrodesKnowledge.findMobs(level, tokens) : null;
        boolean apotheosisAsk = loreTopic == ArrodesLore.Topic.APOTHEOSIS && !explicitPathway && ingredient == null && target == null
                && npc == null && !(uniqW && (whereW || sourceW));

        Intent intent = null;
        if (aboutW && !explicitPathway && ingredient == null) intent = Intent.ABOUT;
        else if (npc != null) intent = Intent.NPC;
        else if (apprW) intent = Intent.APPRAISE;
        else if (apotheosisAsk) intent = Intent.LORE;
        else if (loreTopic != null && !explicitPathway && ingredient == null && target == null && !whereW && !sourceW && !closeW) intent = Intent.LORE;
        else if (deathW && !explicitPathway) intent = Intent.DEATH;
        else if (dangerW && !explicitPathway && ingredient == null) intent = Intent.DANGER;
        else if (uniqW) intent = Intent.UNIQUENESS;
        else if (structurePrefix != null && !explicitPathway) intent = Intent.STRUCTURE;
        else if (target != null && (whereW || sourceW || infoW)) intent = Intent.PLAYER;
        else if (mobMatch != null && (mobMatch.exact() || (!nearbyW && (whereW || sourceW || mobW)))) intent = Intent.MOB;
        else if (explicitPathway) {
            if (recipeW) intent = Intent.RECIPE;
            else if (abilityW) intent = Intent.ABILITIES;
            else intent = Intent.SEQ_NAME;
        } else if (ingredient != null) {
            intent = (recipeW || usesW) && !sourceW ? Intent.USES : Intent.SOURCE;
        } else if (nearbyW) intent = Intent.NEARBY;
        else if (target != null) intent = Intent.PLAYER;

        boolean punish = !owner && LotmmiConfig.ARRODES_SMITE_ILL_INTENT.get();
        if (trickW && punish) return smite(level, true);
        if (puzzleW && !explicitPathway && ingredient == null && npc == null) {
            return Result.refuse(ArrodesVoice.pick(ArrodesVoice.PUZZLE_REFUSALS, level.random));
        }
        boolean aimedAtPeople = target != null || playerW || intent == Intent.PLAYER || intent == Intent.NEARBY;
        if (illW && aimedAtPeople && intent != Intent.NPC) {
            if (punish) return smite(level, false);
            return Result.refuse("I smell ill intent, " + address + ". I will not help you hunt.");
        }
        if (godW && !apotheosisAsk && !explicitPathway && ingredient == null
                && (intent == null || intent == Intent.SOURCE || intent == Intent.STRUCTURE || intent == Intent.PLAYER || intent == Intent.NEARBY || intent == Intent.LORE)) {
            return Result.refuse(ArrodesVoice.pick(ArrodesVoice.REFUSALS, level.random));
        }

        if (context != null) {
            boolean pathwayIntent = context.intent() == Intent.RECIPE || context.intent() == Intent.ABILITIES || context.intent() == Intent.SEQ_NAME;
            if (pathwayIntent && (!explicit || intent == Intent.SEQ_NAME) && (intent == null || intent == Intent.SEQ_NAME)) {
                intent = context.intent();
            }
            if (pathwayIntent && intent == context.intent()) {
                if (pathway == null) pathway = context.pathway();
                if (sequence < 0) sequence = context.sequence();
            }
        }

        if (!ambiguous.isEmpty() && pathway == null
                && (intent == Intent.RECIPE || intent == Intent.ABILITIES || intent == Intent.SEQ_NAME)) {
            List<String> names = new ArrayList<>();
            for (SequenceMatch candidate : ambiguous) names.add(ArrodesKnowledge.pathwayName(candidate.pathway()));
            int shared = ambiguous.get(0).sequence();
            for (SequenceMatch candidate : ambiguous) if (candidate.sequence() != shared) shared = -1;
            return clarify(player, intent, null, shared, now,
                    "More than one pathway has a Sequence called that: " + String.join(", ", names) + ". Which pathway do you mean?");
        }

        if (intent == null) {
            CONTEXT.remove(player.getUUID());
            List<String> menu = new ArrayList<>();
            menu.add("I do not understand the question, " + address + ".");
            menu.add("Ask me about a pathway or sequence, a recipe, what an ingredient serves, where an ingredient or a creature is found, a structure, Beyonders nearby, a named Beyonder, a Uniqueness, another player, or where you last died."
                    + (owner ? " Or ask of the lore of this world, how to become a God, or the dangers around you." : ""));
            return new Result(Kind.UNKNOWN, menu);
        }

        Result result = switch (intent) {
            case SEQ_NAME -> sequenceName(player, pathway, sequence, now);
            case RECIPE -> recipe(player, pathway, sequence, now, owner);
            case ABILITIES -> abilities(player, pathway, sequence, now);
            case SOURCE -> source(level, ingredient, owner);
            case USES -> uses(ingredient, owner);
            case STRUCTURE -> structure(player, structurePrefix);
            case NEARBY -> nearby(player, level);
            case UNIQUENESS -> uniqueness(server, explicitPathway ? pathway : null);
            case DEATH -> death(player);
            case PLAYER -> findPlayer(server, target, whereW || sourceW, infoW || !(whereW || sourceW), owner);
            case MOB -> mob(level, mobMatch, owner);
            case APPRAISE -> appraise(player, owner);
            case DANGER -> owner ? danger(player, level) : Result.refuse(ArrodesVoice.DANGER_DENIED);
            case ABOUT -> about(owner, tokens, normalized);
            case NPC -> npc(npc, owner);
            case LORE -> owner ? lore(level, loreTopic, pathway == null ? sequence : -1) : Result.refuse(ArrodesVoice.LORE_DENIED);
        };
        if (result.kind() != Kind.CLARIFY) CONTEXT.remove(player.getUUID());
        return result;
    }

    private static ServerPlayer findTarget(MinecraftServer server, ServerPlayer asker, String normalized) {
        String padded = " " + normalized + " ";
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == asker) continue;
            String name = ArrodesText.normalize(other.getGameProfile().getName());
            if (!name.isEmpty() && padded.contains(" " + name + " ")) return other;
        }
        return null;
    }

    private static Result clarify(ServerPlayer player, Intent intent, String pathway, int sequence, long now, String question) {
        CONTEXT.put(player.getUUID(), new Context(intent, pathway, sequence, now + 2400L));
        return Result.clarify(question);
    }

    private static Result sequenceName(ServerPlayer player, String pathway, int sequence, long now) {
        if (pathway == null) return clarify(player, Intent.SEQ_NAME, null, sequence, now, "Which pathway do you mean?");
        if (sequence < 0) {
            List<String> parts = new ArrayList<>();
            for (int seq = 9; seq >= 0; seq--) {
                String name = ArrodesKnowledge.sequenceName(pathway, seq);
                if (name != null) parts.add(seq + ": " + name);
            }
            if (parts.isEmpty()) return Result.answer("The " + ArrodesKnowledge.pathwayName(pathway) + " pathway is hidden from me.");
            List<String> lines = new ArrayList<>();
            lines.add("The " + ArrodesKnowledge.pathwayName(pathway) + " pathway:");
            int half = (parts.size() + 1) / 2;
            lines.add(String.join(", ", parts.subList(0, half)));
            if (parts.size() > half) lines.add(String.join(", ", parts.subList(half, parts.size())));
            return Result.answer(lines);
        }
        String name = ArrodesKnowledge.sequenceName(pathway, sequence);
        if (name == null) return Result.answer("I know no such sequence in the " + ArrodesKnowledge.pathwayName(pathway) + " pathway.");
        return Result.answer(ArrodesKnowledge.pathwayName(pathway) + " Sequence " + sequence + " is called " + name + ".");
    }

    private static Result recipe(ServerPlayer player, String pathway, int sequence, long now, boolean owner) {
        if (pathway == null) return clarify(player, Intent.RECIPE, null, sequence, now, "The formula of which pathway?");
        if (sequence < 0) return clarify(player, Intent.RECIPE, pathway, -1, now, "Which sequence of the " + ArrodesKnowledge.pathwayName(pathway) + " pathway?");
        if (ArrodesKnowledge.formulaHidden(sequence)) {
            return Result.refuse(owner ? ArrodesVoice.OWNER_FORMULA_PROTECTED : ArrodesVoice.FORMULA_PROTECTED);
        }
        PotionRecipe recipe = ArrodesKnowledge.recipeFor(pathway, sequence);
        if (recipe == null) return Result.answer("The formula for " + ArrodesKnowledge.describeSequence(pathway, sequence) + " is not known to me.");
        List<String> lines = new ArrayList<>();
        lines.add("Formula for " + ArrodesKnowledge.describeSequence(pathway, sequence) + ":");
        lines.addAll(ArrodesKnowledge.describeRecipe(recipe));
        if (owner && recipe.mainIngredient() != null && !recipe.mainIngredient().isEmpty()) {
            String where = ArrodesKnowledge.firstSource(recipe.mainIngredient().getItem());
            if (where != null) {
                lines.add("A gift for my Master: " + ArrodesKnowledge.itemName(recipe.mainIngredient().getItem()) + " - " + where);
            }
        }
        return Result.answer(lines);
    }

    private static Result abilities(ServerPlayer player, String pathway, int sequence, long now) {
        if (pathway == null) return clarify(player, Intent.ABILITIES, null, sequence, now, "The abilities of which pathway?");
        if (sequence < 0) return clarify(player, Intent.ABILITIES, pathway, -1, now, "Which sequence of the " + ArrodesKnowledge.pathwayName(pathway) + " pathway?");
        List<String> names = new ArrayList<>();
        try {
            for (Ability ability : LOTMCraft.abilityHandler.getByPathwayAndSequenceExactOrdered(pathway, sequence)) {
                String name = ability.getName().getString();
                if (name.startsWith("ability.") || name.contains(".")) name = ArrodesText.pretty(ability.getId());
                names.add(name);
            }
        } catch (Throwable ignored) {
        }
        if (names.isEmpty()) return Result.answer("No abilities are tied to " + ArrodesKnowledge.describeSequence(pathway, sequence) + " that I can see.");
        return Result.answer(List.of("Abilities of " + ArrodesKnowledge.describeSequence(pathway, sequence) + ":", String.join(", ", names)));
    }

    private static Result source(ServerLevel level, Item ingredient, boolean owner) {
        String name = ArrodesKnowledge.itemName(ingredient);
        List<String> lines = new ArrayList<>();
        String hint = ArrodesKnowledge.hintOf(ingredient);
        List<String> rules = hint == null || owner ? ArrodesKnowledge.sourcesOf(ingredient) : List.of();
        if (hint == null && rules.isEmpty()) {
            lines.add(name + ": I cannot tell where it comes from. Perhaps a merchant, or a ritual.");
        } else {
            lines.add(name + ":");
            if (hint != null) lines.add(hint);
            if (hint != null && !rules.isEmpty()) lines.add("The exact rules, for my Master:");
            lines.addAll(rules);
            int shown = 0;
            for (EntityType<?> type : ArrodesKnowledge.mobsFor(ingredient)) {
                if (shown >= 3) break;
                List<String> areas = new ArrayList<>();
                for (SpawnInfo info : ArrodesKnowledge.spawnInfo(level, type)) {
                    areas.add(info.where() + " (" + ArrodesKnowledge.rarity(info.averagePercent()) + ", about " + info.percentText()
                            + " in every 100 " + info.pool() + " that spawn)");
                }
                if (areas.isEmpty()) continue;
                lines.add("Where to find " + ArrodesKnowledge.mobName(type) + ": " + String.join("; ", areas) + ".");
                shown++;
            }
        }
        lines.addAll(servesLines(ingredient, 3, owner));
        return Result.answer(lines);
    }

    private static List<String> servesLines(Item ingredient, int limit, boolean owner) {
        List<String> lines = new ArrayList<>();
        if (ingredient instanceof PotionIngredient potionIngredient) {
            if (ArrodesKnowledge.formulaHidden(potionIngredient.getSequence())) {
                lines.add(owner ? ArrodesVoice.OWNER_SERVES_PROTECTED : ArrodesVoice.SERVES_PROTECTED);
            } else {
                List<String> pathways = new ArrayList<>();
                for (String id : potionIngredient.getPathways()) pathways.add(ArrodesKnowledge.pathwayName(id));
                lines.add("It serves Sequence " + potionIngredient.getSequence() + " of: " + String.join(", ", pathways)
                        + (potionIngredient.isMainIngredient() ? " (a main ingredient)." : "."));
            }
        }
        List<String> uses = ArrodesKnowledge.recipesUsing(ingredient);
        if (!uses.isEmpty()) lines.add("It is used in " + String.join("; ", uses.subList(0, Math.min(limit, uses.size()))) + ".");
        return lines;
    }

    private static Result uses(Item ingredient, boolean owner) {
        String name = ArrodesKnowledge.itemName(ingredient);
        List<String> lines = new ArrayList<>(servesLines(ingredient, Integer.MAX_VALUE, owner));
        if (lines.isEmpty()) return Result.answer("I know of no formula that uses " + name + ".");
        if (lines.size() == 1 && (lines.get(0).equals(ArrodesVoice.SERVES_PROTECTED) || lines.get(0).equals(ArrodesVoice.OWNER_SERVES_PROTECTED))) {
            return Result.refuse(lines.get(0));
        }
        lines.add(0, name + ":");
        return Result.answer(lines);
    }

    private static Result smite(ServerLevel level, boolean trick) {
        return Result.smite(ArrodesVoice.pick(trick ? ArrodesVoice.SMITE_TRICK : ArrodesVoice.SMITE_ILL, level.random));
    }

    private static Result npc(ArrodesNpcs.Npc npc, boolean owner) {
        if (ArrodesNpcs.isProtected(npc)) return Result.refuse(owner ? ArrodesVoice.OWNER_BEING_PROTECTED : ArrodesVoice.BEING_PROTECTED);
        if (!ArrodesNpcs.exists(npc)) {
            return Result.refuse("I have heard the name " + npc.name() + ", but I see no sign of them in this world.");
        }
        return Result.answer(ArrodesNpcs.describe(npc, owner));
    }

    private static int apotheosisStacks(ServerLevel level) {
        try {
            return level.getGameRules().getInt(ModGameRules.CHARSTACK_REQUIRED_FOR_APOTHEOSIS);
        } catch (Throwable ignored) {
            return 2;
        }
    }

    private static Result lore(ServerLevel level, ArrodesLore.Topic topic, int sequence) {
        if (topic == ArrodesLore.Topic.APOTHEOSIS) {
            return Result.answer(ArrodesLore.apotheosis(apotheosisStacks(level), RequestUniquenessApotheosisPacket.KILLS_REQUIRED_FOR_APOTHEOSIS));
        }
        List<String> pathwayNames = new ArrayList<>();
        if (topic == ArrodesLore.Topic.PATHWAY) {
            for (String id : ArrodesKnowledge.pathways().keySet()) pathwayNames.add(ArrodesKnowledge.pathwayName(id));
        }
        return Result.answer(ArrodesLore.answer(topic, sequence, pathwayNames));
    }

    private static Result structure(ServerPlayer player, String prefix) {
        String found = ArrodesKnowledge.locateStructure(player, prefix);
        if (found == null) return Result.answer("I see no such place within my sight in this realm.");
        return Result.answer(found);
    }

    private static String tier(int sequence) {
        if (sequence >= 8) return "a novice";
        if (sequence >= 6) return "an adept";
        if (sequence == 5) return "a veteran";
        if (sequence == 4) return "a demigod";
        return "a saint";
    }

    private static Result nearby(ServerPlayer player, ServerLevel level) {
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(128.0D),
                e -> e != player && e.isAlive() && BeyonderData.isBeyonder(e) && !ArrodesKnowledge.beingProtected(BeyonderData.getSequence(e))));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (found.isEmpty()) return Result.answer("No Beyonder stirs within 128 blocks of you.");
        List<String> lines = new ArrayList<>();
        lines.add("Beyonders within 128 blocks: " + found.size());
        for (LivingEntity entity : found.subList(0, Math.min(6, found.size()))) {
            long distance = Math.round(Math.sqrt(entity.distanceToSqr(player)));
            String where = distance + " blocks " + ArrodesText.direction(entity.getX() - player.getX(), entity.getZ() - player.getZ());
            int sequence = BeyonderData.getSequence(entity);
            lines.add("- " + ArrodesKnowledge.pathwayName(BeyonderData.getPathway(entity)) + ", " + tier(sequence) + ", " + where);
        }
        return Result.answer(lines);
    }

    private static String region(double x, double z, ServerLevel level) {
        long rx = Math.round(x / 100.0D) * 100L;
        long rz = Math.round(z / 100.0D) * 100L;
        return "near X " + rx + ", Z " + rz + " in " + ArrodesText.pretty(level.dimension().location().getPath());
    }

    private static List<String> uniquenessPathways() {
        Set<String> out = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof UniquenessItem uniqueness) out.add(uniqueness.getPathway());
        }
        return new ArrayList<>(out);
    }

    private static String locateUniqueness(MinecraftServer server, String pathway) {
        Integer entityId = UniquenessEntity.ACTIVE_ENTITIES.get(pathway);
        if (entityId != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(entityId);
                if (entity instanceof UniquenessEntity && !entity.isRemoved()) {
                    return "lies unclaimed " + region(entity.getX(), entity.getZ(), level);
                }
            }
        }
        for (ServerPlayer holder : server.getPlayerList().getPlayers()) {
            boolean has = false;
            UniquenessComponent component = holder.getData(ModAttachments.UNIQUENESS_COMPONENT);
            if (component.hasUniqueness() && pathway.equalsIgnoreCase(component.getUniquenessPathway())) has = true;
            for (ItemStack stack : holder.getInventory().items) {
                if (stack.getItem() instanceof UniquenessItem uniqueness && pathway.equalsIgnoreCase(uniqueness.getPathway())) has = true;
            }
            if (has) {
                if (BeyonderData.isBeyonder(holder) && ArrodesKnowledge.beingProtected(BeyonderData.getSequence(holder))) {
                    return "is held by one who is protected against divination and prophecy";
                }
                return "is held by someone, " + region(holder.getX(), holder.getZ(), holder.serverLevel());
            }
        }
        return null;
    }

    private static Result uniqueness(MinecraftServer server, String pathway) {
        List<String> targets = pathway != null ? List.of(pathway) : uniquenessPathways();
        List<String> lines = new ArrayList<>();
        for (String id : targets) {
            String where = locateUniqueness(server, id);
            if (where != null) lines.add("The " + ArrodesKnowledge.pathwayName(id) + " Uniqueness " + where + ".");
            else if (pathway != null) lines.add("The " + ArrodesKnowledge.pathwayName(id) + " Uniqueness is nowhere I can see.");
        }
        if (lines.isEmpty()) lines.add("No Uniqueness stirs in this world.");
        return Result.answer(lines);
    }

    private static Result death(ServerPlayer player) {
        Optional<GlobalPos> last = player.getLastDeathLocation();
        if (last.isEmpty()) return Result.answer("You have not died, as far as I can tell. Not yet.");
        GlobalPos pos = last.get();
        return Result.answer("You last died in " + ArrodesText.pretty(pos.dimension().location().getPath())
                + " at X " + pos.pos().getX() + ", Y " + pos.pos().getY() + ", Z " + pos.pos().getZ() + ".");
    }

    private static Result findPlayer(MinecraftServer server, ServerPlayer target, boolean wantLocation, boolean wantDetails, boolean owner) {
        if (!LotmmiConfig.ARRODES_FIND_PLAYER.get()) return Result.refuse("I will not pry into the living for you.");
        String name = target.getGameProfile().getName();
        boolean beyonder = BeyonderData.isBeyonder(target);
        int sequence = beyonder ? BeyonderData.getSequence(target) : 10;
        if (beyonder && ArrodesKnowledge.beingProtected(sequence)) {
            return Result.refuse(owner ? ArrodesVoice.OWNER_BEING_PROTECTED : ArrodesVoice.BEING_PROTECTED);
        }
        if (SefirotData.get(server).isInSefirot(target)) {
            return Result.answer(name + " is hidden within a Sefirah Castle. The mirror finds nothing.");
        }

        List<String> lines = new ArrayList<>();
        if (wantDetails) {
            if (!beyonder) {
                lines.add(name + " walks no Beyonder path. A mere mortal, for now.");
            } else if (sequence <= 3) {
                lines.add(name + " is a Beyonder whose path is veiled from me.");
            } else {
                String pathway = BeyonderData.getPathway(target);
                String sequenceName = ArrodesKnowledge.sequenceName(pathway, sequence);
                lines.add(name + " walks the " + ArrodesKnowledge.pathwayName(pathway) + " pathway, at Sequence " + sequence
                        + (sequenceName == null ? "" : ", " + sequenceName) + ".");
            }
        }
        if (wantLocation) {
            long rx = Math.round(target.getX() / 250.0D) * 250L;
            long rz = Math.round(target.getZ() / 250.0D) * 250L;
            lines.add(name + " is somewhere near X " + rx + ", Z " + rz
                    + " in " + ArrodesText.pretty(target.serverLevel().dimension().location().getPath()) + ".");
        }
        return Result.answer(lines);
    }

    private static Result mob(ServerLevel level, MobMatch match, boolean owner) {
        if (match.types().size() > 1) {
            List<String> names = new ArrayList<>();
            for (EntityType<?> type : match.types()) names.add(ArrodesKnowledge.mobName(type));
            return Result.clarify("More than one creature answers to that: " + String.join(", ", names) + ". Name the one you mean.");
        }
        EntityType<?> type = match.types().get(0);
        String name = ArrodesKnowledge.mobName(type);
        List<SpawnInfo> infos = ArrodesKnowledge.spawnInfo(level, type);
        List<String> lines = new ArrayList<>();
        if (infos.isEmpty()) {
            lines.add(name + " does not roam this world on its own. It comes through a ritual, a place it guards, or someone's will.");
        } else {
            lines.add(name + ":");
            for (SpawnInfo info : infos) {
                lines.add("Found in " + info.where() + ": " + ArrodesKnowledge.rarity(info.averagePercent()) + ", about "
                        + info.percentText() + " of every 100 " + info.pool() + " that spawn there, in groups of " + info.groupText() + ".");
                if (owner) lines.add("A gift for my Master: its spawn weight there is " + info.weight() + ".");
            }
        }
        List<String> drops = ArrodesKnowledge.dropsOf(type);
        if (!drops.isEmpty()) lines.add("It may yield: " + String.join(", ", drops.subList(0, Math.min(4, drops.size()))) + ".");
        return Result.answer(lines);
    }

    private static Result appraise(ServerPlayer player, boolean owner) {
        ItemStack stack = ItemStack.EMPTY;
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof com.Maul.lotmmi.item.custom.ArrodesItem) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        } else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof com.Maul.lotmmi.item.custom.ArrodesItem) {
            stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        }
        if (stack.isEmpty()) return Result.answer("Hold the thing you wish appraised in your other hand.");

        Item item = stack.getItem();
        String name = ArrodesKnowledge.itemName(item);
        if (item instanceof BeyonderCharacteristicItem characteristic) {
            return Result.answer("A Beyonder Characteristic of " + ArrodesKnowledge.describeSequence(characteristic.getPathway(), characteristic.getSequence()) + ".");
        }
        if (item instanceof BeyonderPotion potion) {
            return Result.answer("A potion of " + ArrodesKnowledge.describeSequence(potion.getPathway(), potion.getSequence()) + ".");
        }
        if (item instanceof PotionIngredient potionIngredient) {
            return source(player.serverLevel(), item, owner);
        }
        if (item instanceof UniquenessItem uniqueness) {
            return Result.answer("A Uniqueness of the " + ArrodesKnowledge.pathwayName(uniqueness.getPathway()) + " pathway. Handle it with fear.");
        }
        if (item instanceof PotionRecipeItem recipeItem && recipeItem.getRecipe() != null && recipeItem.getRecipe().potion() != null) {
            BeyonderPotion potion = recipeItem.getRecipe().potion();
            if (ArrodesKnowledge.formulaHidden(potion.getSequence())) {
                return Result.refuse(owner ? ArrodesVoice.OWNER_FORMULA_PROTECTED : ArrodesVoice.FORMULA_PROTECTED);
            }
            List<String> lines = new ArrayList<>();
            lines.add("A written formula for " + ArrodesKnowledge.describeSequence(potion.getPathway(), potion.getSequence()) + ":");
            lines.addAll(ArrodesKnowledge.describeRecipe(recipeItem.getRecipe()));
            return Result.answer(lines);
        }
        if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(com.Maul.lotmmi.LotmMysticalItems.MOD_ID)) {
            return Result.answer(name + ": a mystical thing of its own kind. I will not say more.");
        }
        return Result.answer(name + ": mundane. Nothing stirs in it.");
    }

    private static Result about(boolean owner, List<String> tokens, String normalized) {
        if (!owner) return Result.refuse(ArrodesVoice.ABOUT_DENIED);
        boolean all = ArrodesText.hasAny(tokens, normalized, "tell me about yourself", "about yourself", "introduce yourself");
        boolean origin = ArrodesText.hasAny(tokens, normalized, "origin", "origins", "come from", "came from", "made you", "created you",
                "creator", "maker", "history", "where are you from", "where were you born", "birthplace", "homeland", "where do you live",
                "how were you made", "how did you come to be");
        boolean can = ArrodesText.hasAny(tokens, normalized, "what can you do", "what do you do", "what can arrodes do", "purpose");
        List<String> lines = new ArrayList<>();
        if (all || (!origin && !can) || ArrodesText.hasAny(tokens, normalized, "who are you", "what are you", "who is arrodes", "what is arrodes")) {
            lines.addAll(ArrodesVoice.ABOUT_IDENTITY);
        }
        if (all || origin) lines.addAll(ArrodesVoice.ABOUT_ORIGIN);
        if (all || can) lines.addAll(ArrodesVoice.ABOUT_CAN);
        return Result.answer(lines);
    }

    public static String tip(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        String first = danger(player, level).lines().get(0);
        if (!first.startsWith("I see no danger")) return "Great Master, a warning: " + first;
        return ArrodesVoice.pick(ArrodesVoice.OWNER_TIPS, level.random);
    }

    private static Result danger(ServerPlayer player, ServerLevel level) {
        List<String> lines = new ArrayList<>();
        List<Mob> hunters = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D),
                m -> m.isAlive() && (m.getTarget() == player || (m instanceof Monster && m.distanceToSqr(player) < 24.0D * 24.0D)));
        if (!hunters.isEmpty()) lines.add(hunters.size() + " hostile creature" + (hunters.size() == 1 ? "" : "s") + " lurk within reach of you.");

        int own = BeyonderData.isBeyonder(player) ? BeyonderData.getSequence(player) : 10;
        List<LivingEntity> stronger = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(128.0D),
                e -> e != player && e.isAlive() && BeyonderData.isBeyonder(e) && BeyonderData.getSequence(e) < own
                        && !ArrodesKnowledge.beingProtected(BeyonderData.getSequence(e)));
        stronger.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (!stronger.isEmpty()) {
            LivingEntity nearest = stronger.get(0);
            lines.add("A Beyonder stronger than you stands " + Math.round(Math.sqrt(nearest.distanceToSqr(player))) + " blocks to the "
                    + ArrodesText.direction(nearest.getX() - player.getX(), nearest.getZ() - player.getZ()) + ".");
        }
        if (level.isThundering()) lines.add("A storm gathers over you, and lightning is not particular.");
        if (lines.isEmpty()) lines.add("I see no danger near you. Do not grow careless.");
        return Result.answer(lines);
    }
}
