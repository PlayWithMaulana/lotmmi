package com.Maul.lotmmi.gui;

import com.Maul.lotmmi.item.custom.ArrodesItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class ArrodesMenu extends AbstractContainerMenu {

    public record Reply(List<String> lines, int kind) {
    }

    private final boolean owner;
    private final boolean answering;
    private final String opening;
    private final Queue<Reply> replies = new ArrayDeque<>();

    public ArrodesMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBoolean(), buf.readBoolean(), buf.readUtf());
    }

    public ArrodesMenu(int containerId, boolean owner, boolean answering, String opening) {
        super(ModMenuTypes.ARRODES_MENU.get(), containerId);
        this.owner = owner;
        this.answering = answering;
        this.opening = opening;
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean isAnswering() {
        return answering;
    }

    public String getOpening() {
        return opening;
    }

    public void receive(List<String> lines, int kind) {
        replies.add(new Reply(lines, kind));
    }

    public Reply poll() {
        return replies.poll();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().getItem() instanceof ArrodesItem
                || player.getOffhandItem().getItem() instanceof ArrodesItem;
    }
}
