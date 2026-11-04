package io.github.clementgen.requestboard.network;

import io.github.clementgen.requestboard.RequestBoard;
import io.github.clementgen.requestboard.data.BoardRequest;
import io.github.clementgen.requestboard.data.Category;
import io.github.clementgen.requestboard.data.ObjectiveEntry;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** All packets exchanged between client and server. */
public final class RBPayloads {
	private RBPayloads() {
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(RequestBoard.MOD_ID, path));
	}

	// ---- Server -> client ----

	/** The server's config, as JSON. */
	public record SyncConfig(String json) implements CustomPacketPayload {
		public static final Type<SyncConfig> TYPE = typeOf("sync_config");
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfig> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(1 << 16), SyncConfig::json, SyncConfig::new
		);

		@Override
		public Type<SyncConfig> type() {
			return TYPE;
		}
	}

	/** Full list of open requests, plus the server clock so countdowns line up. */
	public record SyncRequests(List<BoardRequest> requests, long serverTime) implements CustomPacketPayload {
		public static final Type<SyncRequests> TYPE = typeOf("sync_requests");
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncRequests> CODEC = StreamCodec.composite(
			BoardRequest.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRequests::requests,
			ByteBufCodecs.VAR_LONG, SyncRequests::serverTime,
			SyncRequests::new
		);

		@Override
		public Type<SyncRequests> type() {
			return TYPE;
		}
	}

	/** Asks the client to open the board screen. */
	public record OpenBoard() implements CustomPacketPayload {
		public static final OpenBoard INSTANCE = new OpenBoard();
		public static final Type<OpenBoard> TYPE = typeOf("open_board");
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoard> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<OpenBoard> type() {
			return TYPE;
		}
	}

	/** Result of a create request, so the client can keep the form on error. */
	public record CreateResult(boolean success, Component message) implements CustomPacketPayload {
		public static final Type<CreateResult> TYPE = typeOf("create_result");
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateResult> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, CreateResult::success,
			ComponentSerialization.STREAM_CODEC, CreateResult::message,
			CreateResult::new
		);

		@Override
		public Type<CreateResult> type() {
			return TYPE;
		}
	}

	// ---- Client -> server ----

	/** A reward taken from an inventory slot (0-35). */
	public record RewardPick(int slot, int count) {
		public static final StreamCodec<RegistryFriendlyByteBuf, RewardPick> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RewardPick::slot,
			ByteBufCodecs.VAR_INT, RewardPick::count,
			RewardPick::new
		);
	}

	public record CreateRequest(Category category, String title, String details, List<ObjectiveEntry> objectives,
			List<RewardPick> rewards, int durationMinutes) implements CustomPacketPayload {
		public static final Type<CreateRequest> TYPE = typeOf("create_request");
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateRequest> CODEC = StreamCodec.composite(
			Category.STREAM_CODEC, CreateRequest::category,
			ByteBufCodecs.stringUtf8(512), CreateRequest::title,
			ByteBufCodecs.stringUtf8(8192), CreateRequest::details,
			ObjectiveEntry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), CreateRequest::objectives,
			RewardPick.CODEC.apply(ByteBufCodecs.list(64)), CreateRequest::rewards,
			ByteBufCodecs.VAR_INT, CreateRequest::durationMinutes,
			CreateRequest::new
		);

		@Override
		public Type<CreateRequest> type() {
			return TYPE;
		}
	}

	public enum Action { ACCEPT, LEAVE, CLOSE, DELETE }

	public record RequestAction(UUID requestId, Action action) implements CustomPacketPayload {
		public static final Type<RequestAction> TYPE = typeOf("request_action");
		public static final StreamCodec<RegistryFriendlyByteBuf, RequestAction> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RequestAction::requestId,
			ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal), RequestAction::action,
			RequestAction::new
		);

		@Override
		public Type<RequestAction> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SyncConfig.TYPE, SyncConfig.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncRequests.TYPE, SyncRequests.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenBoard.TYPE, OpenBoard.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CreateResult.TYPE, CreateResult.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CreateRequest.TYPE, CreateRequest.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestAction.TYPE, RequestAction.CODEC);
	}
}
