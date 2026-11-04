package io.github.clementgen.requestboard;

import io.github.clementgen.requestboard.block.BoardRoof;
import io.github.clementgen.requestboard.block.BoardWood;
import io.github.clementgen.requestboard.block.RequestBoardBlock;
import io.github.clementgen.requestboard.config.RBConfig;
import io.github.clementgen.requestboard.network.RBPayloads;
import io.github.clementgen.requestboard.server.RequestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestBoard implements ModInitializer {
	public static final String MOD_ID = "requestboard";
	public static final Logger LOGGER = LoggerFactory.getLogger("RequestBoard");

	public static final ResourceKey<Block> BOARD_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("request_board"));
	public static final ResourceKey<Item> BOARD_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("request_board"));

	public static final Block REQUEST_BOARD = Registry.register(BuiltInRegistries.BLOCK, BOARD_BLOCK_KEY,
		new RequestBoardBlock(BlockBehaviour.Properties.of()
			.setId(BOARD_BLOCK_KEY)
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava()));

	public static final Item REQUEST_BOARD_ITEM = Registry.register(BuiltInRegistries.ITEM, BOARD_ITEM_KEY,
		new BlockItem(REQUEST_BOARD, new Item.Properties().setId(BOARD_ITEM_KEY).useBlockDescriptionPrefix()));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		RBConfig.load();
		RBPayloads.register();

		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.CreateRequest.TYPE,
			(payload, context) -> RequestManager.create(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.RequestAction.TYPE,
			(payload, context) -> RequestManager.handleAction(context.player(), payload.requestId(), payload.action()));

		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> RequestManager.onJoin(listener.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(RequestManager::tick);

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reload").executes(ctx -> {
					RBConfig.load();
					RequestManager.broadcastConfig(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.translatable("requestboard.msg.config_reloaded"), true);
					return 1;
				}))));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			for (BoardWood wood : BoardWood.values()) {
				output.accept(RequestBoardBlock.withVariant(new ItemStack(REQUEST_BOARD_ITEM), wood, BoardRoof.STONE));
			}
		});

		LOGGER.info("RequestBoard ready");
	}
}
