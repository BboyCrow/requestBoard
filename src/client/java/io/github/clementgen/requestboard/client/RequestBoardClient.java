package io.github.clementgen.requestboard.client;

import io.github.clementgen.requestboard.client.screen.RequestBoardScreen;
import io.github.clementgen.requestboard.config.RBConfig;
import io.github.clementgen.requestboard.network.RBPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class RequestBoardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.SyncConfig.TYPE, (payload, context) -> {
			RBConfig.set(RBConfig.fromJson(payload.json()));
			if (context.client().gui.screen() instanceof RequestBoardScreen screen) {
				screen.onConfigUpdated();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.SyncRequests.TYPE, (payload, context) -> {
			ClientBoardState.update(payload.requests(), payload.serverTime());
			if (context.client().gui.screen() instanceof RequestBoardScreen screen) {
				screen.onRequestsUpdated();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.OpenBoard.TYPE,
			(payload, context) -> context.client().gui.setScreen(new RequestBoardScreen()));
		ClientPlayNetworking.registerGlobalReceiver(RBPayloads.CreateResult.TYPE, (payload, context) -> {
			if (context.client().gui.screen() instanceof RequestBoardScreen screen) {
				screen.onCreateResult(payload.success(), payload.message());
			} else if (context.client().player != null) {
				context.client().player.sendSystemMessage(payload.message());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientBoardState.clear());
	}
}
