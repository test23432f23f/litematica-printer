package me.aleksilassila.litematica.printer.actions;

import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Direction;

public class PrepareAction extends Action {
    public final PrinterPlacementContext context;
    public boolean modifyYaw = true;
    public boolean modifyPitch = true;
    public float yaw = 0;
    public float pitch = 0;

    public PrepareAction(PrinterPlacementContext context) {
        this.context = context;
        Direction lookDirection = context.lookDirection;

        if (lookDirection != null && lookDirection.getAxis().isHorizontal()) {
            this.yaw = lookDirection.asRotation();
        } else {
            this.modifyYaw = false;
        }

        if (lookDirection == Direction.UP) {
            this.pitch = -90;
        } else if (lookDirection == Direction.DOWN) {
            this.pitch = 90;
        } else if (lookDirection != null) {
            this.pitch = 0;
        } else {
            this.modifyPitch = false;
        }
    }

    public PrepareAction(PrinterPlacementContext context, float yaw, float pitch) {
        this.context = context;

        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void send(MinecraftClient client, ClientPlayerEntity player) {
        ItemStack itemStack = context.getStack();
        int slot = context.requiredItemSlot;

        if (itemStack != null && client.interactionManager != null && itemStack.getCount() > 1) {
            PlayerInventory inventory = player.getInventory();

            // This thing is straight from MinecraftClient#doItemPick()
            if (player.getAbilities().creativeMode) {
                inventory.addPickBlock(itemStack);
                client.interactionManager.clickCreativeStack(player.getStackInHand(Hand.MAIN_HAND),
                        36 + inventory.selectedSlot);
            } else if (slot != -1) {
                if (PlayerInventory.isValidHotbarIndex(slot)) {
                    inventory.selectedSlot = slot;
                } else {
                    client.interactionManager.pickFromInventory(slot);
                }
            }
        }

        if (modifyPitch || modifyYaw) {
            float yaw = modifyYaw ? this.yaw : player.getYaw();
            float pitch = modifyPitch ? this.pitch : player.getPitch();

            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(), yaw,
                    pitch, player.isOnGround(), player.horizontalCollision);

            player.networkHandler.sendPacket(packet);
        }

        if (context.shouldSneak) {
            player.input.playerInput = new PlayerInput(player.input.playerInput.forward(), player.input.playerInput.backward(), player.input.playerInput.left(), player.input.playerInput.right(), player.input.playerInput.jump(), true, player.input.playerInput.sprint());
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        } else {
            player.input.playerInput = new PlayerInput(player.input.playerInput.forward(), player.input.playerInput.backward(), player.input.playerInput.left(), player.input.playerInput.right(), player.input.playerInput.jump(), false, player.input.playerInput.sprint());
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }

    @Override
    public String toString() {
        return "PrepareAction{" +
                "context=" + context +
                '}';
    }
}
