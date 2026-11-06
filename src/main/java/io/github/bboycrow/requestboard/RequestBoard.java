package io.github.bboycrow.requestboard;

import io.github.bboycrow.requestboard.block.BoardRoof;
import io.github.bboycrow.requestboard.block.BoardWood;
import io.github.bboycrow.requestboard.block.RequestBoardBlock;
import io.github.bboycrow.requestboard.block.RequestBoardItem;
import io.github.bboycrow.requestboard.config.RBConfig;
import io.github.bboycrow.requestboard.network.RBPayloads;
import io.github.bboycrow.requestboard.server.RequestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?}
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
import net.minecraft.world.item.CreativeModeTab;
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
	public static final ResourceKey<Item> ICON_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("advancement_icon"));

	public static final Block REQUEST_BOARD = Registry.register(BuiltInRegistries.BLOCK, BOARD_BLOCK_KEY,
		new RequestBoardBlock(BlockBehaviour.Properties.of()
			//? if >=1.21.2
			.setId(BOARD_BLOCK_KEY)
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava()));

	public static final Item REQUEST_BOARD_ITEM = Registry.register(BuiltInRegistries.ITEM, BOARD_ITEM_KEY,
		new RequestBoardItem(REQUEST_BOARD, itemProperties(BOARD_ITEM_KEY, true)));

	/** Hidden item only used as the icon of the mod's advancements (look picked by custom_model_data). */
	public static final Item ADVANCEMENT_ICON = Registry.register(BuiltInRegistries.ITEM, ICON_ITEM_KEY,
		new Item(itemProperties(ICON_ITEM_KEY, false).stacksTo(1)));

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("request_boards")),
		//? if >=26.1 {
		FabricCreativeModeTab.builder()
		//?} else
		/*FabricItemGroup.builder()*/
			.title(Component.translatable("itemGroup.requestboard.request_boards"))
			.icon(() -> RequestBoardBlock.withVariant(new ItemStack(REQUEST_BOARD_ITEM), BoardWood.OAK, BoardRoof.STONE))
			.displayItems((parameters, output) -> {
				for (BoardWood wood : BoardWood.values()) {
					for (BoardRoof roof : BoardRoof.values()) {
						output.accept(RequestBoardBlock.withVariant(new ItemStack(REQUEST_BOARD_ITEM), wood, roof));
					}
				}
			})
			.build());

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Oak board with a stone roof (created lazily: item stacks can't be built during mod init). */
	private static ItemStack defaultBoard() {
		return RequestBoardBlock.withVariant(new ItemStack(REQUEST_BOARD_ITEM), BoardWood.OAK, BoardRoof.STONE);
	}

	private static Item.Properties itemProperties(ResourceKey<Item> key, boolean blockItem) {
		Item.Properties properties = new Item.Properties();
		//? if >=1.21.2 {
		properties = properties.setId(key);
		if (blockItem) {
			properties = properties.useBlockDescriptionPrefix();
		}
		//?}
		return properties;
	}

	@Override
	public void onInitialize() {
		RBConfig.load();
		RBPayloads.register();

		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.CreateRequest.TYPE,
			(payload, context) -> RequestManager.create(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.RequestAction.TYPE,
			(payload, context) -> RequestManager.handleAction(context.player(), payload.requestId(), payload.action()));
		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.UpdateContribution.TYPE,
			(payload, context) -> RequestManager.updateContribution(context.player(), payload.requestId(), payload.target(), payload.contribution()));
		ServerPlayNetworking.registerGlobalReceiver(RBPayloads.SaveConfig.TYPE,
			(payload, context) -> RequestManager.saveConfig(context.player(), payload.json()));

		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> RequestManager.onJoin(listener.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(RequestManager::tick);

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal(MOD_ID)
				//? if >=1.21.11 {
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				//?} else
				/*.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))*/
				.then(Commands.literal("reload").executes(ctx -> {
					RBConfig.load();
					RequestManager.broadcastConfig(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.translatable("requestboard.msg.config_reloaded"), true);
					return 1;
				}))
				.then(Commands.literal("config").executes(ctx -> {
					RequestManager.openConfigEditor(ctx.getSource().getPlayerOrException());
					return 1;
				}))));

		//? if >=26.1 {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(defaultBoard()));
		//?} else
		/*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(defaultBoard()));*/

		LOGGER.info("RequestBoard ready");
	}
}
