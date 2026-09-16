package com.Maul.lotmmi.effect;

import com.Maul.lotmmi.LotmMysticalItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {

    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, LotmMysticalItems.MOD_ID);

    public static final Holder<net.minecraft.world.effect.MobEffect> PANIC = MOB_EFFECTS.register("panic",
            () -> new PanicEffect(MobEffectCategory.HARMFUL, 0xB03030));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
