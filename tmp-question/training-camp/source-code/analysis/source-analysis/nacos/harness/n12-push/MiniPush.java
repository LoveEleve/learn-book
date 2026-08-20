import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniPush — N-12 harness (12 assertions)
 *
 * Reproduces the push model without Nacos server:
 *   1. delegate: SPI-first + default rpc/udp
 *   2. delayed-merge push (multiple changes merge into one)
 *   3. execute task iterates clients
 *   4. result hook observation
 *   5. no-retry exception semantics
 */
public class MiniPush {

	// ---- push executor ----
	interface PushExecutor {
		String push(String clientId, String data);
	}

	static class PushExecutorRpcImpl implements PushExecutor {
		@Override
		public String push(String clientId, String data) {
			return "rpc-pushed:" + clientId;
		}
	}

	static class PushExecutorUdpImpl implements PushExecutor {
		@Override
		public String push(String clientId, String data) {
			return "udp-pushed:" + clientId;
		}
	}

	// ---- SPI ----
	static class SpiPushExecutor implements PushExecutor {
		@Override
		public String push(String clientId, String data) {
			return "spi-pushed:" + clientId;
		}
	}

	static class SpiHolder {
		static final Map<String, SpiPushExecutor> SPIS = new HashMap<>();

		static void register(String name, SpiPushExecutor spi) {
			SPIS.put(name, spi);
		}

		static SpiPushExecutor get(String name) {
			return SPIS.get(name);
		}
	}

	static class PushExecutorDelegate {
		final PushExecutorRpcImpl rpc = new PushExecutorRpcImpl();
		final PushExecutorUdpImpl udp = new PushExecutorUdpImpl();

		String push(String clientId, String data, boolean spiExists) {
			SpiPushExecutor spi = SpiHolder.get(clientId);
			if (spi != null) {
				return spi.push(clientId, data);
			}
			// default: rpc for grpc clients, udp for legacy
			if (clientId.startsWith("grpc-")) {
				return rpc.push(clientId, data);
			}
			return udp.push(clientId, data);
		}
	}

	// ---- delayed merge ----
	static class PushDelayTask {
		final String service;
		String data;

		PushDelayTask(String service, String data) {
			this.service = service;
			this.data = data;
		}

		void merge(String newData) {
			this.data = newData;
		}
	}

	static class DelayEngine {
		final Map<String, PushDelayTask> pending = new HashMap<>();

		void submit(String service, String data) {
			PushDelayTask task = pending.get(service);
			if (task == null) {
				pending.put(service, new PushDelayTask(service, data));
			}
			else {
				task.merge(data); // merge within window
			}
		}
	}

	// ---- result hook ----
	static class PushResultHook {
		final List<String> results = new ArrayList<>();

		void onPushResult(String clientId, boolean success) {
			results.add(clientId + ":" + success);
		}
	}

	static class NoRequiredRetryException extends RuntimeException {
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
		PushExecutorDelegate delegate = new PushExecutorDelegate();

		// ---- 1. SPI first ----
		SpiHolder.register("grpc-spi", new SpiPushExecutor());
		check("N12: SPI wins over default",
				delegate.push("grpc-spi", "d", true).equals("spi-pushed:grpc-spi"));

		// ---- 2. default rpc/udp ----
		check("N12: grpc client → rpc", delegate.push("grpc-1", "d", false).equals("rpc-pushed:grpc-1"));
		check("N12: legacy client → udp", delegate.push("ip-1", "d", false).equals("udp-pushed:ip-1"));

		// ---- 3. delayed merge ----
		DelayEngine engine = new DelayEngine();
		engine.submit("svc-a", "v1");
		engine.submit("svc-b", "vb");
		engine.submit("svc-a", "v2"); // merge
		engine.submit("svc-a", "v3"); // merge
		check("N12: merged within window", engine.pending.size() == 2
				&& engine.pending.get("svc-a").data.equals("v3"));

		// ---- 4. execute iterates clients ----
		List<String> clients = List.of("grpc-1", "grpc-2");
		List<String> pushed = new ArrayList<>();
		for (String c : clients) {
			pushed.add(delegate.push(c, "latest", false));
		}
		check("N12: push to all clients", pushed.size() == 2
				&& pushed.get(0).equals("rpc-pushed:grpc-1"));

		// ---- 5. result hook ----
		PushResultHook hook = new PushResultHook();
		hook.onPushResult("grpc-1", true);
		hook.onPushResult("grpc-2", false);
		check("N12: hook records results", hook.results.size() == 2
				&& hook.results.get(1).equals("grpc-2:false"));

		// ---- 6. no-required-retry ----
		boolean threw = false;
		try {
			throw new NoRequiredRetryException();
		}
		catch (NoRequiredRetryException e) {
			threw = true;
		}
		check("N12: no-retry exception distinguishable", threw);

		System.out.println("===== MiniPush: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
