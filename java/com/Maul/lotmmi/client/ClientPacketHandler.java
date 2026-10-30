package com.Maul.lotmmi.client;

import com.Maul.lotmmi.gui.ItemIntrospectMenu;
import com.Maul.lotmmi.gui.ScepterIntrospectMenu;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toClient.SyncScepterIntrospectPacket;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {

    public static void syncItemIntrospect(SyncItemIntrospectPacket packet) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof ItemIntrospectMenu menu) {
            menu.applySync(packet.soulsRaw(), packet.foodRaw(), packet.abilityOrderRaw(), packet.feedRemaining(),
                    packet.prayerRemaining(), packet.activeSoul());
        }
    }

    public static void syncScepterIntrospect(SyncScepterIntrospectPacket packet) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof ScepterIntrospectMenu menu) {
            menu.applySync(packet.lines(), packet.prayerLines(), packet.canSummon(), packet.canGaze(), packet.charmCost());
        }
    }
}
