package io.github.bboycrow.requestboard.client;

import io.github.bboycrow.requestboard.client.screen.RequestBoardScreen;
import io.github.bboycrow.requestboard.config.RBConfig;
import io.github.bboycrow.requestboard.network.RBPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class RequestBoardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.SyncConfig.TYPE, (payload, context) -> {
			RBConfig.set(RBConfig.fromJson(payload.json()));
			if (screen(context.client()) instanceof RequestBoardScreen screen) {
				screen.onConfigUpdated();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.SyncRequests.TYPE, (payload, context) -> {
			ClientBoardState.update(payload.requests(), payload.serverTime());
			if (screen(context.client()) instanceof RequestBoardScreen screen) {
				screen.onRequestsUpdated();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.OpenBoard.TYPE,
			(payload, context) -> context.client().execute(() -> openScreen(context.client())));
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.CreateResult.TYPE, (payload, context) -> {
			if (screen(context.client()) instanceof RequestBoardScreen screen) {
				screen.onCreateResult(payload.success(), payload.message());
			} else if (context.client().player != null) {
				//? if >=26.1 {
				context.client().player.sendSystemMessage(payload.message());
				//?} else
				/*context.client().player.displayClientMessage(payload.message(), false);*/
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientBoardState.clear());

		//? if <1.21.4 {
		/*// Before 1.21.4 item models can't read the block_state component: pick the wood/roof model with an override.
		net.minecraft.client.renderer.item.ItemProperties.register(io.github.bboycrow.requestboard.RequestBoard.REQUEST_BOARD_ITEM,
			io.github.bboycrow.requestboard.RequestBoard.id("variant"), (stack, level, entity, seed) -> {
				int wood = io.github.bboycrow.requestboard.block.RequestBoardItem.wood(stack).ordinal();
				int roof = io.github.bboycrow.requestboard.block.RequestBoardItem.roof(stack).ordinal();
				return (wood * io.github.bboycrow.requestboard.block.BoardRoof.values().length + roof) / 1000.0F;
			});
		*///?}
	}

	private static net.minecraft.client.gui.screens.Screen screen(net.minecraft.client.Minecraft client) {
		//? if >=26.1 {
		return client.gui.screen();
		//?} else
		/*return client.screen;*/
	}

	private static void openScreen(net.minecraft.client.Minecraft client) {
		//? if >=26.1 {
		client.gui.setScreen(new RequestBoardScreen());
		//?} else
		/*client.setScreen(new RequestBoardScreen());*/
	}
}
