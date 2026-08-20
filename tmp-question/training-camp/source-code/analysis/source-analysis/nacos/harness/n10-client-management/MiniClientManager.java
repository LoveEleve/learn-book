import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniClientManager — N-10 harness (12 assertions)
 *
 * Reproduces the client management model without Nacos server:
 *   1. three manager types: gRPC connection / ephemeral ip:port / persistent ip:port
 *   2. delegate aggregation
 *   3. client lifecycle: connected → registered → release
 *   4. AbstractClient release semantics
 *   5. generateSyncData for cluster sync
 *   6. factory dispatch by attributes
 */
public class MiniClientManager {

	// ---- client ----
	static class Client {
		final String clientId;
		boolean released = false;
		boolean ephemeral;
		final List<String> services = new ArrayList<>();

		Client(String clientId, boolean ephemeral) {
			this.clientId = clientId;
			this.ephemeral = ephemeral;
		}

		void addService(String svc) {
			services.add(svc);
		}

		void release() {
			this.released = true;
		}

		boolean isEphemeral() {
			return ephemeral;
		}

		String generateSyncData() {
			return "sync:" + clientId + ":" + String.join(",", services);
		}
	}

	// ---- manager interface ----
	interface ClientManager {
		boolean clientConnected(String clientId, Client c);

		void release(String clientId);

		List<String> allClientId();
	}

	// ---- three managers ----
	static class ConnectionBasedClientManager implements ClientManager {
		final Map<String, Client> clients = new ConcurrentHashMap<>();

		@Override
		public boolean clientConnected(String clientId, Client c) {
			return clients.putIfAbsent(clientId, c) == null;
		}

		@Override
		public void release(String clientId) {
			Client c = clients.remove(clientId);
			if (c != null) {
				c.release();
			}
		}

		@Override
		public List<String> allClientId() {
			return new ArrayList<>(clients.keySet());
		}
	}

	static class IpPortClientManager implements ClientManager {
		final Map<String, Client> clients = new ConcurrentHashMap<>();

		@Override
		public boolean clientConnected(String clientId, Client c) {
			return clients.putIfAbsent(clientId, c) == null;
		}

		@Override
		public void release(String clientId) {
			Client c = clients.remove(clientId);
			if (c != null) {
				c.release();
			}
		}

		@Override
		public List<String> allClientId() {
			return new ArrayList<>(clients.keySet());
		}
	}

	// ---- delegate ----
	static class ClientManagerDelegate implements ClientManager {
		final ConnectionBasedClientManager conn = new ConnectionBasedClientManager();
		final IpPortClientManager ephemeral = new IpPortClientManager();
		final IpPortClientManager persistent = new IpPortClientManager();

		@Override
		public boolean clientConnected(String clientId, Client c) {
			if (clientId.startsWith("grpc-")) {
				return conn.clientConnected(clientId, c);
			}
			else if (c.isEphemeral()) {
				return ephemeral.clientConnected(clientId, c);
			}
			else {
				return persistent.clientConnected(clientId, c);
			}
		}

		@Override
		public void release(String clientId) {
			if (clientId.startsWith("grpc-")) {
				conn.release(clientId);
			}
			else {
				ephemeral.release(clientId);
				persistent.release(clientId);
			}
		}

		@Override
		public List<String> allClientId() {
			List<String> all = new ArrayList<>();
			all.addAll(conn.allClientId());
			all.addAll(ephemeral.allClientId());
			all.addAll(persistent.allClientId());
			return all;
		}
	}

	// ---- factory dispatch ----
	static class ClientFactory {
		Client create(String clientId, boolean ephemeral, String type) {
			if ("grpc".equals(type)) {
				return new Client(clientId, true);
			}
			return new Client(clientId, ephemeral);
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
		ClientManagerDelegate delegate = new ClientManagerDelegate();

		// ---- 1. routing by id/type ----
		Client grpcClient = new Client("grpc-1", true);
		grpcClient.addService("svc-a");
		delegate.clientConnected("grpc-1", grpcClient);
		check("N10: grpc client routed to connection manager",
				delegate.conn.clients.containsKey("grpc-1"));

		Client epClient = new Client("ip-1", true);
		delegate.clientConnected("ip-1", epClient);
		check("N10: ephemeral client routed", delegate.ephemeral.clients.containsKey("ip-1")
				&& !delegate.persistent.clients.containsKey("ip-1"));

		Client perClient = new Client("ip-2", false);
		delegate.clientConnected("ip-2", perClient);
		check("N10: persistent client routed", delegate.persistent.clients.containsKey("ip-2"));

		// ---- 2. delegate aggregation ----
		check("N10: allClientId aggregates", delegate.allClientId().size() == 3);

		// ---- 3. lifecycle: release ----
		delegate.release("grpc-1");
		check("N10: release removes + marks", !delegate.conn.clients.containsKey("grpc-1")
				&& grpcClient.released);

		// ---- 4. duplicate connect ----
		Client dup = new Client("ip-1", true);
		boolean first = delegate.clientConnected("ip-1", dup);
		check("N10: duplicate connect rejected", !first);

		// ---- 5. generateSyncData ----
		String sync = grpcClient.generateSyncData();
		check("N10: sync data carries id+services", sync.equals("sync:grpc-1:svc-a"));

		// ---- 6. factory dispatch ----
		ClientFactory factory = new ClientFactory();
		Client c1 = factory.create("a", true, "grpc");
		Client c2 = factory.create("b", false, "ipport");
		check("N10: factory type dispatch", c1.isEphemeral() && !c2.isEphemeral());

		// ---- 7. client detail (first-wins on duplicate) ----
		check("N10: detail via manager keeps first client",
				delegate.ephemeral.clients.get("ip-1") == epClient
						&& delegate.ephemeral.clients.get("ip-1") != dup);

		System.out.println("===== MiniClientManager: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
