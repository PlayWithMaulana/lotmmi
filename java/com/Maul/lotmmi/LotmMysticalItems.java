package com.Maul.lotmmi;

import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.effect.ModEffects;
import com.Maul.lotmmi.gui.ModMenuTypes;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.loot.ModLootModifiers;
import com.Maul.lotmmi.network.ModPacketHandler;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LotmMysticalItems.MOD_ID)
public class LotmMysticalItems {

    public static final String MOD_ID = "lotmmi";

    public static final Logger LOGGER = LoggerFactory.getLogger(LotmMysticalItems.class);

    public static KeyMapping itemIntrospectKey;

    public LotmMysticalItems(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(ModPacketHandler::register);
    }
}
