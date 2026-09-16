package com.Maul.lotmmi.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.Maul.lotmmi.item.ModItems;
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

public class StaffOfStarsChestLootModifier extends LootModifier {

    public static final Supplier<MapCodec<StaffOfStarsChestLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, StaffOfStarsChestLootModifier::new));

    private static final Random RANDOM = new Random();

    public StaffOfStarsChestLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getQueriedLootTableId().getPath().contains("chests/")) {
            if (context.getRandom().nextFloat() < 0.45f) {
                int sequence = ChestLootModifier.getWeightedHighSequence();

                if (sequence >= 7 && RANDOM.nextInt(4) == 0 && RANDOM.nextInt(3) == 0) {
                    generatedLoot.add(new ItemStack(ModItems.STAFF_OF_THE_STARS.get()));
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
