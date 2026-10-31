package com.Maul.lotmmi.network;

import com.Maul.lotmmi.LotmMysticalItems;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toClient.SyncStaffIntrospectPacket;
import com.Maul.lotmmi.network.packets.toClient.SyncStaffTeleportPacket;
import com.Maul.lotmmi.network.packets.toClient.SyncScepterIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.ChooseGrazeAbilityPacket;
import com.Maul.lotmmi.network.packets.toServer.CreateCharmPacket;
import com.Maul.lotmmi.network.packets.toServer.CycleSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.FollowerActionPacket;
import com.Maul.lotmmi.network.packets.toServer.FeedReserveSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenItemIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenScepterIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.OpenStaffIntrospectPacket;
import com.Maul.lotmmi.network.packets.toServer.PrayerActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffAbilityActionPacket;
import com.Maul.lotmmi.network.packets.toServer.StaffSlotActionPacket;
import com.Maul.lotmmi.network.packets.toServer.ReorderAbilityPacket;
import com.Maul.lotmmi.network.packets.toServer.SelectSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.SpewSoulPacket;
import com.Maul.lotmmi.network.packets.toServer.ToggleAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "8";

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

        registrar.playToClient(
                SyncScepterIntrospectPacket.TYPE,
                SyncScepterIntrospectPacket.STREAM_CODEC,
                SyncScepterIntrospectPacket::handle
        );

        registrar.playToServer(
                CreateCharmPacket.TYPE,
                CreateCharmPacket.STREAM_CODEC,
                CreateCharmPacket::handle
        );

        registrar.playToServer(
                FollowerActionPacket.TYPE,
                FollowerActionPacket.STREAM_CODEC,
                FollowerActionPacket::handle
        );

        registrar.playToServer(
                PrayerActionPacket.TYPE,
                PrayerActionPacket.STREAM_CODEC,
                PrayerActionPacket::handle
        );

        registrar.playToServer(
                SelectSoulPacket.TYPE,
                SelectSoulPacket.STREAM_CODEC,
                SelectSoulPacket::handle
        );

        registrar.playToServer(
                CycleSoulPacket.TYPE,
                CycleSoulPacket.STREAM_CODEC,
                CycleSoulPacket::handle
        );

        registrar.playToServer(
                OpenItemIntrospectPacket.TYPE,
                OpenItemIntrospectPacket.STREAM_CODEC,
                OpenItemIntrospectPacket::handle
        );

        registrar.playToServer(
                OpenScepterIntrospectPacket.TYPE,
                OpenScepterIntrospectPacket.STREAM_CODEC,
                OpenScepterIntrospectPacket::handle
        );

        registrar.playToClient(
                SyncItemIntrospectPacket.TYPE,
                SyncItemIntrospectPacket.STREAM_CODEC,
                SyncItemIntrospectPacket::handle
        );

        registrar.playToClient(
                SyncStaffIntrospectPacket.TYPE,
                SyncStaffIntrospectPacket.STREAM_CODEC,
                SyncStaffIntrospectPacket::handle
        );

        registrar.playToClient(
                SyncStaffTeleportPacket.TYPE,
                SyncStaffTeleportPacket.STREAM_CODEC,
                SyncStaffTeleportPacket::handle
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
                StaffSlotActionPacket.TYPE,
                StaffSlotActionPacket.STREAM_CODEC,
                StaffSlotActionPacket::handle
        );
    }

    public static void sendToServer(CustomPacketPayload packet) {
        Minecraft.getInstance().getConnection().send(packet);
    }
}
