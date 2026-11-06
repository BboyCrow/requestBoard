package io.github.bboycrow.requestboard.network;

import io.github.bboycrow.requestboard.RequestBoard;
import io.github.bboycrow.requestboard.data.BoardRequest;
import io.github.bboycrow.requestboard.data.Category;
import io.github.bboycrow.requestboard.data.ObjectiveEntry;
import java.util.List;
import java.util.Optional;
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

	/**
	 * Creates a request, or edits one when {@code editId} is set. When editing, {@code replaceRewards}
	 * says whether {@code rewards} replace the current item rewards (which then go back to the creator).
	 */
	public record CreateRequest(Optional<UUID> editId, Category category, String title, String details, List<ObjectiveEntry> objectives,
			List<RewardPick> rewards, boolean replaceRewards, String rewardText, int durationMinutes) implements CustomPacketPayload {
		public static final Type<CreateRequest> TYPE = typeOf("create_request");
		private static final StreamCodec<RegistryFriendlyByteBuf, List<ObjectiveEntry>> OBJECTIVES = ObjectiveEntry.STREAM_CODEC.apply(ByteBufCodecs.list(64));
		private static final StreamCodec<RegistryFriendlyByteBuf, List<RewardPick>> PICKS = RewardPick.CODEC.apply(ByteBufCodecs.list(64));
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateRequest> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeBoolean(p.editId.isPresent());
			p.editId.ifPresent(id -> UUIDUtil.STREAM_CODEC.encode(buf, id));
			Category.STREAM_CODEC.encode(buf, p.category);
			ByteBufCodecs.stringUtf8(512).encode(buf, p.title);
			ByteBufCodecs.stringUtf8(8192).encode(buf, p.details);
			OBJECTIVES.encode(buf, p.objectives);
			PICKS.encode(buf, p.rewards);
			buf.writeBoolean(p.replaceRewards);
			ByteBufCodecs.stringUtf8(1024).encode(buf, p.rewardText);
			buf.writeVarInt(p.durationMinutes);
		}, buf -> new CreateRequest(
			buf.readBoolean() ? Optional.of(UUIDUtil.STREAM_CODEC.decode(buf)) : Optional.empty(),
			Category.STREAM_CODEC.decode(buf),
			ByteBufCodecs.stringUtf8(512).decode(buf),
			ByteBufCodecs.stringUtf8(8192).decode(buf),
			OBJECTIVES.decode(buf),
			PICKS.decode(buf),
			buf.readBoolean(),
			ByteBufCodecs.stringUtf8(1024).decode(buf),
			buf.readVarInt()
		));

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

	/** Server -> client: open the config editor with the current server config (JSON). */
	public record OpenConfig(String json) implements CustomPacketPayload {
		public static final Type<OpenConfig> TYPE = typeOf("open_config");
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfig> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(1 << 16), OpenConfig::json, OpenConfig::new
		);

		@Override
		public Type<OpenConfig> type() {
			return TYPE;
		}
	}

	/** Client -> server: save the config edited in game (operators only). */
	public record SaveConfig(String json) implements CustomPacketPayload {
		public static final Type<SaveConfig> TYPE = typeOf("save_config");
		public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfig> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(1 << 16), SaveConfig::json, SaveConfig::new
		);

		@Override
		public Type<SaveConfig> type() {
			return TYPE;
		}
	}

	/**
	 * Client -> server: amounts declared for each requested item. {@code target} is the sender (their own
	 * contribution), the creator's own stock (when the creator targets themselves), or a participant the
	 * creator corrects.
	 */
	public record UpdateContribution(UUID requestId, UUID target, List<ObjectiveEntry> contribution) implements CustomPacketPayload {
		public static final Type<UpdateContribution> TYPE = typeOf("update_contribution");
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateContribution> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, UpdateContribution::requestId,
			UUIDUtil.STREAM_CODEC, UpdateContribution::target,
			ObjectiveEntry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), UpdateContribution::contribution,
			UpdateContribution::new
		);

		@Override
		public Type<UpdateContribution> type() {
			return TYPE;
		}
	}

	public static void register() {
		//? if >=26.1 {
		var toClient = PayloadTypeRegistry.clientboundPlay();
		var toServer = PayloadTypeRegistry.serverboundPlay();
		//?} else {
		/*var toClient = PayloadTypeRegistry.playS2C();
		var toServer = PayloadTypeRegistry.playC2S();
		*///?}
		toClient.register(SyncConfig.TYPE, SyncConfig.CODEC);
		toClient.register(SyncRequests.TYPE, SyncRequests.CODEC);
		toClient.register(OpenBoard.TYPE, OpenBoard.CODEC);
		toClient.register(CreateResult.TYPE, CreateResult.CODEC);
		toClient.register(OpenConfig.TYPE, OpenConfig.CODEC);
		toServer.register(SaveConfig.TYPE, SaveConfig.CODEC);
		toServer.register(UpdateContribution.TYPE, UpdateContribution.CODEC);
		toServer.register(CreateRequest.TYPE, CreateRequest.CODEC);
		toServer.register(RequestAction.TYPE, RequestAction.CODEC);
	}
}
