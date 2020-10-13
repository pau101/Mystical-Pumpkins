package blueduck.mysticalpumpkins.network.message;

import blueduck.mysticalpumpkins.jei.infuser.InfusingRecipeTransferHandlerServer;
import blueduck.mysticalpumpkins.registry.RegisterHandler;
import blueduck.mysticalpumpkins.tileentity.InfusionTableRecipe;
import blueduck.mysticalpumpkins.utils.SpecialConstants;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InfusingMovingMessage implements IMessage<InfusingMovingMessage> {

	public Map<Integer, Integer> recipeMap;
	public List<Integer> craftingSlots;
	public List<Integer> inventorySlots;
	public InfusionTableRecipe recipeToBeMoved;
	private boolean maxTransfer;
	private boolean requireCompleteSets;

	public InfusingMovingMessage() {}

	public InfusingMovingMessage(Map<Integer, Integer> recipeMap, InfusionTableRecipe recipeToBeMoved, List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer, boolean requireCompleteSets) {
		this.recipeMap = recipeMap;
		this.craftingSlots = craftingSlots;
		this.inventorySlots = inventorySlots;
		this.maxTransfer = maxTransfer;
		this.requireCompleteSets = requireCompleteSets;
		this.recipeToBeMoved = recipeToBeMoved;
	}

	@Override
	public void encode(InfusingMovingMessage message, PacketBuffer buf) {
		buf.writeVarInt(message.recipeMap.size());
		for (Map.Entry<Integer, Integer> recipeMapEntry : message.recipeMap.entrySet()) {
			buf.writeVarInt(recipeMapEntry.getKey());
			buf.writeVarInt(recipeMapEntry.getValue());
		}
		buf.writeItemStack(message.recipeToBeMoved.getInput());
		buf.writeItemStack(new ItemStack(RegisterHandler.PUMPKIN_ESSENCE.get(), message.recipeToBeMoved.getEssenceAmount()));
		buf.writeItemStack(message.recipeToBeMoved.getSecondary());
		buf.writeItemStack(message.recipeToBeMoved.getOutput());
		buf.writeVarInt(message.craftingSlots.size());
		for (Integer craftingSlot : message.craftingSlots) {
			buf.writeVarInt(craftingSlot);
		}

		buf.writeVarInt(message.inventorySlots.size());
		for (Integer inventorySlot : message.inventorySlots) {
			buf.writeVarInt(inventorySlot);
		}

		buf.writeBoolean(message.maxTransfer);
		buf.writeBoolean(message.requireCompleteSets);
	}

	@Override
	public InfusingMovingMessage decode(PacketBuffer buf) {
		int recipeMapSize = buf.readVarInt();
		Map<Integer, Integer> recipeMap = new HashMap<>(recipeMapSize);
		for (int i = 0; i < recipeMapSize; i++) {
			int slotIndex = buf.readVarInt();
			int recipeItem = buf.readVarInt();
			recipeMap.put(slotIndex, recipeItem);
		}
		InfusionTableRecipe recipe = new InfusionTableRecipe(buf.readItemStack(), buf.readItemStack().getCount(), buf.readItemStack(), buf.readItemStack());
		int craftingSlotsSize = buf.readVarInt();
		List<Integer> craftingSlots = new ArrayList<>(craftingSlotsSize);
		for (int i = 0; i < craftingSlotsSize; i++) {
			int slotIndex = buf.readVarInt();
			craftingSlots.add(slotIndex);
		}

		int inventorySlotsSize = buf.readVarInt();
		List<Integer> inventorySlots = new ArrayList<>(inventorySlotsSize);
		for (int i = 0; i < inventorySlotsSize; i++) {
			int slotIndex = buf.readVarInt();
			inventorySlots.add(slotIndex);
		}

		boolean maxTransfer = buf.readBoolean();
		boolean requireCompleteSets = buf.readBoolean();

		return new InfusingMovingMessage(recipeMap, recipe, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets);
	}

	@Override
	public String toString() {
		return "InfusingMovingMessage{" +
				       "recipeMap=" + recipeMap +
				       ", craftingSlots=" + craftingSlots +
				       ", inventorySlots=" + inventorySlots +
				       ", recipeToBeMoved=" + recipeToBeMoved +
				       ", maxTransfer=" + maxTransfer +
				       ", requireCompleteSets=" + requireCompleteSets +
				       '}';
	}

	@Override
	public void handle(InfusingMovingMessage message, Supplier<NetworkEvent.Context> supplier) {
		supplier.get().enqueueWork(() -> {
			ServerPlayerEntity player = supplier.get().getSender();
			SpecialConstants.LOGGER.info(message.toString());
			InfusingRecipeTransferHandlerServer.setItems(player, message.recipeToBeMoved, message.recipeMap, message.craftingSlots, message.inventorySlots, message.maxTransfer, message.requireCompleteSets);
		});
		supplier.get().setPacketHandled(true);
	}
}
