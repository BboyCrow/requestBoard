package io.github.bboycrow.requestboard;

import io.github.bboycrow.requestboard.data.RequestStorage;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.11
import net.minecraft.server.permissions.Permissions;
//? if >=26.1
import net.minecraft.util.Prediction;

/** Small helpers hiding the differences between the supported Minecraft versions. */
public final class Compat {
	private Compat() {
	}

	public static RequestStorage storage(MinecraftServer server) {
		//? if >=26.1 {
		return server.getDataStorage().computeIfAbsent(RequestStorage.TYPE);
		//?} elif >=1.21.5 {
		/*return server.overworld().getDataStorage().computeIfAbsent(RequestStorage.TYPE);
		*///?} else {
		/*return server.overworld().getDataStorage().computeIfAbsent(RequestStorage.FACTORY, RequestBoard.MOD_ID + "_requests");
		*///?}
	}

	public static MinecraftServer server(ServerPlayer player) {
		//? if >=1.21.5 {
		return player.level().getServer();
		//?} else
		/*return player.server;*/
	}

	public static String name(Player player) {
		//? if >=1.21.5 {
		return player.getPlainTextName();
		//?} else
		/*return player.getGameProfile().getName();*/
	}

	public static boolean isOperator(Player player) {
		//? if >=1.21.11 {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		//?} else
		/*return player.hasPermissions(2);*/
	}

	/** Gives an item to a player, dropping it at their feet if the inventory is full. */
	public static void give(ServerPlayer player, ItemStack stack) {
		//? if >=26.1 {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		//?} else
		/*player.getInventory().placeItemBackInInventory(stack);*/
	}

	public static Holder<Item> holder(ItemStack stack) {
		//? if >=26.1 {
		return stack.typeHolder();
		//?} else
		/*return stack.getItemHolder();*/
	}
}
