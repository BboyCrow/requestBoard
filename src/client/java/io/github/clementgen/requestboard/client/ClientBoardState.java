package io.github.clementgen.requestboard.client;

import io.github.clementgen.requestboard.data.BoardRequest;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Last board state received from the server. */
public final class ClientBoardState {
	private static List<BoardRequest> requests = List.of();
	private static long clockOffset;

	private ClientBoardState() {
	}

	public static void update(List<BoardRequest> newRequests, long serverTime) {
		requests = List.copyOf(newRequests);
		clockOffset = serverTime - System.currentTimeMillis();
	}

	public static void clear() {
		requests = List.of();
		clockOffset = 0;
	}

	public static List<BoardRequest> requests() {
		return requests;
	}

	public static @Nullable BoardRequest find(UUID id) {
		for (BoardRequest request : requests) {
			if (request.id().equals(id)) {
				return request;
			}
		}
		return null;
	}

	/** Current time on the server's clock, so countdowns match the server. */
	public static long now() {
		return System.currentTimeMillis() + clockOffset;
	}
}
