package io.github.clementgen.requestboard.server;

import io.github.clementgen.requestboard.RequestBoard;
import io.github.clementgen.requestboard.data.RequestStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants the mod's advancements (data/requestboard/advancement). They use an "impossible" criterion,
 * so only this class can complete them. Granting is idempotent.
 */
public final class BoardAdvancements {
	public static final String FIRST_REQUEST = "first_request";
	public static final String REQUESTS_10 = "requests_10";
	public static final String REQUESTS_100 = "requests_100";
	public static final String ACCEPT_REQUEST = "accept_request";
	public static final String MISSION_ACCOMPLISHED = "mission_accomplished";
	public static final String PATRON = "patron";
	public static final String COMMUNITY_PILLAR = "community_pillar";
	public static final String VILLAGE_LEGEND = "village_legend";

	private BoardAdvancements() {
	}

	public static void grant(ServerPlayer player, String name) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(RequestBoard.id(name));
		if (holder != null) {
			player.getAdvancements().award(holder, "done");
		}
	}

	/** Grants every counter-based advancement the player has earned (also used when they log in). */
	public static void sync(ServerPlayer player, RequestStorage.PlayerStats stats) {
		if (stats.posted() >= 1) grant(player, FIRST_REQUEST);
		if (stats.posted() >= 10) grant(player, REQUESTS_10);
		if (stats.posted() >= 100) grant(player, REQUESTS_100);
		if (stats.rewarded()) grant(player, MISSION_ACCOMPLISHED);
		if (stats.completed() >= 10) grant(player, COMMUNITY_PILLAR);
		if (stats.completed() >= 100) grant(player, VILLAGE_LEGEND);
	}
}
