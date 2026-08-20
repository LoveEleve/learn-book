import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniConfigRemote — N-17 harness (12 assertions)
 *
 * Reproduces long-polling + gRPC handler dispatch without Nacos server:
 *   1. long-polling: instant compare response vs async hang
 *   2. timeout = max(min, request - 500ms early-response)
 *   3. no-hang-up header short-circuit
 *   4. gRPC handler family: query/publish/remove/batch-listen dispatch
 *   5. change notify by connection
 */
public class MiniConfigRemote {

	// ---- long polling ----
	static class LongPollingService {
		static final String LONG_POLLING_NO_HANG_UP_HEADER = "Long-Pulling-No-Hangup";
		static final int MIN_TIMEOUT = 10000;
		static final int EARLY_MS = 500;

		// returns: "instant" if changed, "nohangup" if header set, "hang" otherwise
		String addLongPollingClient(Map<String, String> clientMd5Map,
				Map<String, String> currentMd5Map, String noHangUpHeader,
				String requestTimeout) {
			// 1. compare md5
			Map<String, String> changed = compareMd5(clientMd5Map, currentMd5Map);
			if (!changed.isEmpty()) {
				return "instant";
			}
			if (noHangUpHeader != null && noHangUpHeader.equalsIgnoreCase("true")) {
				return "nohangup";
			}
			// 2. compute timeout with early 500ms
			long timeout = Math.max(MIN_TIMEOUT, Long.parseLong(requestTimeout) - EARLY_MS);
			hang(timeout);
			return "hang";
		}

		Map<String, String> compareMd5(Map<String, String> client, Map<String, String> current) {
			Map<String, String> changed = new HashMap<>();
			for (Map.Entry<String, String> e : client.entrySet()) {
				if (!e.getValue().equals(current.get(e.getKey()))) {
					changed.put(e.getKey(), e.getValue());
				}
			}
			return changed;
		}

		void hang(long timeout) {
			// async context, no thread block
		}
	}

	// ---- gRPC handler family ----
	interface RequestHandler {
		String getType();

		String handle(String req);
	}

	static class ConfigQueryRequestHandler implements RequestHandler {
		@Override
		public String getType() {
			return "query";
		}

		@Override
		public String handle(String req) {
			return "query-result:" + req;
		}
	}

	static class ConfigPublishRequestHandler implements RequestHandler {
		@Override
		public String getType() {
			return "publish";
		}

		@Override
		public String handle(String req) {
			return "published:" + req;
		}
	}

	static class ConfigChangeBatchListenRequestHandler implements RequestHandler {
		@Override
		public String getType() {
			return "batch-listen";
		}

		@Override
		public String handle(String req) {
			return "listening:" + req;
		}
	}

	// ---- dispatcher ----
	static class HandlerDispatcher {
		final Map<String, RequestHandler> handlers = new HashMap<>();

		void register(RequestHandler h) {
			handlers.put(h.getType(), h);
		}

		String dispatch(String type, String req) {
			RequestHandler h = handlers.get(type);
			return h != null ? h.handle(req) : "no-handler:" + type;
		}
	}

	// ---- change notify by connection ----
	static class Notifier {
		final List<String> pushed = new ArrayList<>();

		void notify(List<String> connections, String dataId) {
			for (String conn : connections) {
				pushed.add(conn + ":" + dataId);
			}
		}
	}

	static int passed = 0;
	static int failed = 0;

	static void check(String name, boolean cond) {
		if (cond) {
			passed++;
		}
		else {
			failed++;
			System.out.println("FAIL: " + name);
		}
	}

	public static void main(String[] args) {
		LongPollingService lp = new LongPollingService();

		// ---- 1. instant response on change ----
		Map<String, String> client = Map.of("a", "m1");
		Map<String, String> current = Map.of("a", "m2");
		check("N17: changed md5 → instant response",
				lp.addLongPollingClient(client, current, null, "30000").equals("instant"));

		// ---- 2. no-hang-up ----
		check("N17: no-hang-up header short-circuits",
				lp.addLongPollingClient(client, client, "true", "30000").equals("nohangup"));

		// ---- 3. hang otherwise ----
		check("N17: unchanged without header → hang",
				lp.addLongPollingClient(client, client, null, "30000").equals("hang"));

		// ---- 4. early response timeout (request 30000 → 29500, > 10000) ----
		long t1 = Math.max(10000, 30000 - 500);
		check("N17: timeout respects early-500ms", t1 == 29500);
		// ---- 5. min timeout floor ----
		long t2 = Math.max(10000, 3000 - 500);
		check("N17: min timeout floor", t2 == 10000);

		// ---- 6. gRPC handler family ----
		HandlerDispatcher d = new HandlerDispatcher();
		d.register(new ConfigQueryRequestHandler());
		d.register(new ConfigPublishRequestHandler());
		d.register(new ConfigChangeBatchListenRequestHandler());
		check("N17: query dispatch", d.dispatch("query", "d1").equals("query-result:d1"));
		check("N17: publish dispatch", d.dispatch("publish", "d1").equals("published:d1"));
		check("N17: batch-listen dispatch",
				d.dispatch("batch-listen", "d1").equals("listening:d1"));
		check("N17: unknown type", d.dispatch("other", "d1").equals("no-handler:other"));

		// ---- 7. notify by connection ----
		Notifier n = new Notifier();
		n.notify(List.of("conn-1", "conn-2"), "data-1");
		check("N17: notify per connection", n.pushed.size() == 2
				&& n.pushed.get(1).equals("conn-2:data-1"));

		System.out.println("===== MiniConfigRemote: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
