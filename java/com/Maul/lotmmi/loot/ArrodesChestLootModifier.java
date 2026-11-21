package com.Maul.lotmmi.loot;

import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.loottables.ChestLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Supplier;

public class ArrodesChestLootModifier extends LootModifier {

    public static final Supplier<MapCodec<ArrodesChestLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ArrodesChestLootModifier::new));

    private static final Random RANDOM = new Random();

    public ArrodesChestLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // The same roll as the Staff of the Stars (StaffOfStarsChestLootModifier): 45% of chests roll for an artifact,
        // the sequence has to come out 7 or weaker, then 1 in 4 and 1 in 3. About 2.6% of chests. The setting can only
        // make it rarer (1 = exactly the Staff's rate, 0 = never).
        if (context.getQueriedLootTableId().getPath().contains("chests/")) {
            if (context.getRandom().nextFloat() < 0.45f) {
                int sequence = ChestLootModifier.getWeightedHighSequence();

                if (sequence >= 7 && RANDOM.nextInt(4) == 0 && RANDOM.nextInt(3) == 0
                        && context.getRandom().nextDouble() < LotmmiConfig.ARRODES_CHEST_RARITY.get()) {
                    generatedLoot.add(new ItemStack(ModItems.ARRODES.get()));
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
