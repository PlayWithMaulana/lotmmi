package com.Maul.lotmmi.network;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.ChooseGrazeAbilityPacket;
import com.Maul.lotmmi.network.packets.toServer.FeedReserveSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenStaffIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffAbilityActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffEntityActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffItemActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffRenameSlotPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffSlotActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffTypedTeleportPacket;
import com.Maul.lotmmi.network.packets.toServer.ReorderAbilityPacket;
import com.Maul.lotmmi.network.packets.toServer.SpewSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.ToggleAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(LotmMysticalItems.MOD_ID)
                .versioned(PROTOCOL_VERSION);

        registrar.playToServer(
                SpewSoulPacket.TYPE,
                SpewSoulPacket.STREAM_CODEC,
                SpewSoulPacket::handle
        );

        registrar.playToServer(
                FeedReserveSoulPacket.TYPE,
                FeedReserveSoulPacket.STREAM_CODEC,
                FeedReserveSoulPacket::handle
        );

        registrar.playToServer(
                ChooseGrazeAbilityPacket.TYPE,
                ChooseGrazeAbilityPacket.STREAM_CODEC,
                ChooseGrazeAbilityPacket::handle
        );

        registrar.playToServer(
                ReorderAbilityPacket.TYPE,
                ReorderAbilityPacket.STREAM_CODEC,
                ReorderAbilityPacket::handle
        );

        registrar.playToServer(
                ToggleAbilityPacket.TYPE,
                ToggleAbilityPacket.STREAM_CODEC,
                ToggleAbilityPacket::handle
        );

        registrar.playToServer(
                OpenItemIntrospectPacket.TYPE,
                OpenItemIntrospectPacket.STREAM_CODEC,
                OpenItemIntrospectPacket::handle
        );

        registrar.playToClient(
                SyncItemIntrospectPacket.TYPE,
                SyncItemIntrospectPacket.STREAM_CODEC,
                SyncItemIntrospectPacket::handle
        );

        registrar.playToServer(
                OpenStaffIntrospectPacket.TYPE,
                OpenStaffIntrospectPacket.STREAM_CODEC,
                OpenStaffIntrospectPacket::handle
        );

        registrar.playToServer(
                StaffAbilityActionPacket.TYPE,
                StaffAbilityActionPacket.STREAM_CODEC,
                StaffAbilityActionPacket::handle
        );

        registrar.playToServer(
                StaffEntityActionPacket.TYPE,
                StaffEntityActionPacket.STREAM_CODEC,
                StaffEntityActionPacket::handle
        );

        registrar.playToServer(
                StaffItemActionPacket.TYPE,
                StaffItemActionPacket.STREAM_CODEC,
                StaffItemActionPacket::handle
        );

        registrar.playToServer(
                StaffSlotActionPacket.TYPE,
                StaffSlotActionPacket.STREAM_CODEC,
                StaffSlotActionPacket::handle
        );

        registrar.playToServer(
                StaffRenameSlotPacket.TYPE,
                StaffRenameSlotPacket.STREAM_CODEC,
                StaffRenameSlotPacket::handle
        );

        registrar.playToServer(
                StaffTypedTeleportPacket.TYPE,
                StaffTypedTeleportPacket.STREAM_CODEC,
                StaffTypedTeleportPacket::handle
        );
    }

    public static void sendToServer(CustomPacketPayload packet) {
        Minecraft.getInstance().getConnection().send(packet);
    }
}
