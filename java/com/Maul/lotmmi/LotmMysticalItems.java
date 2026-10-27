package com.Maul.lotmmi;

import com.Maul.lotmmi.config.LotmmiConfig;
import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.effect.ModEffects;
import com.Maul.lotmmi.entity.ModEntities;
import com.Maul.lotmmi.gui.ModMenuTypes;
import com.Maul.lotmmi.item.ModCreativeTabs;
import com.Maul.lotmmi.item.ModItems;
import com.Maul.lotmmi.loot.ModLootModifiers;
import com.Maul.lotmmi.network.ModPacketHandler;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(LotmMysticalItems.MOD_ID)
public class LotmMysticalItems {

    public static final String MOD_ID = "lotmmi";

    public static KeyMapping itemIntrospectKey;

    public LotmMysticalItems(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(ModPacketHandler::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, LotmmiConfig.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
