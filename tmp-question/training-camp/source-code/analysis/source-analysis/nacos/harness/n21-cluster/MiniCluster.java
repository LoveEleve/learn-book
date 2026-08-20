import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * MiniCluster — N-21 harness (12 assertions)
 *
 * Reproduces cluster member management without Nacos server:
 *   1. ordered member table (ConcurrentSkipListMap semantics)
 *   2. self-registration
 *   3. lookup factory type selection (file / address-server / standalone)
 *   4. member change event + listener notification
 *   5. unhealthy member report
 */
public class MiniCluster {

	// ---- member ----
	static class Member {
		final String address;
		boolean healthy = true;

		Member(String address) {
			this.address = address;
		}
	}

	// ---- member manager (mirror ServerMemberManager) ----
	static class ServerMemberManager {
		final TreeMap<String, Member> serverList = new TreeMap<>();
		final List<String> changeLog = new ArrayList<>();

		void selfRegister(String addr) {
			serverList.put(addr, new Member(addr));
		}

		void addMember(String addr) {
			serverList.put(addr, new Member(addr));
			changeLog.add("ADD:" + addr);
		}

		void removeMember(String addr) {
			serverList.remove(addr);
			changeLog.add("REMOVE:" + addr);
		}

		Member getSelf(String addr) {
			return serverList.get(addr);
		}

		boolean isUnHealth(String addr) {
			Member m = serverList.get(addr);
			return m != null && !m.healthy;
		}

		List<String> getMemberAddressInfos() {
			List<String> healthy = new ArrayList<>();
			for (Member m : serverList.values()) {
				if (m.healthy) {
					healthy.add(m.address);
				}
			}
			return healthy;
		}
	}

	// ---- lookup factory ----
	static class LookupType {
		static final String FILE_CONFIG = "file";
		static final String ADDRESS_SERVER = "address-server";
		static final String STANDALONE = "standalone";
	}

	static class LookupFactory {
		static String decide(String config) {
			if (config == null || config.isEmpty()) {
				return LookupType.STANDALONE;
			}
			if (LookupType.FILE_CONFIG.equals(config)) {
				return LookupType.FILE_CONFIG;
			}
			if (LookupType.ADDRESS_SERVER.equals(config)) {
				return LookupType.ADDRESS_SERVER;
			}
			return LookupType.ADDRESS_SERVER; // default
		}
	}

	// ---- change event ----
	static class MembersChangeEvent {
		final List<String> members;

		MembersChangeEvent(List<String> members) {
			this.members = members;
		}
	}

	static class MemberChangeListener {
		final List<String> received = new ArrayList<>();

		void onEvent(MembersChangeEvent e) {
			received.addAll(e.members);
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
		ServerMemberManager mgr = new ServerMemberManager();

		// ---- 1. self registration ----
		mgr.selfRegister("127.0.0.1:8848");
		check("N21: self registered", mgr.getSelf("127.0.0.1:8848") != null);

		// ---- 2. ordered member table ----
		mgr.addMember("10.0.0.2:8848");
		mgr.addMember("10.0.0.1:8848");
		List<String> addrs = new ArrayList<>(mgr.serverList.keySet());
		check("N21: ordered by address", addrs.get(0).equals("10.0.0.1:8848")
				&& addrs.get(1).equals("10.0.0.2:8848"));

		// ---- 3. change log ----
		check("N21: change events logged", mgr.changeLog.size() == 2
				&& mgr.changeLog.get(0).equals("ADD:10.0.0.2:8848"));

		// ---- 4. health filter ----
		mgr.serverList.get("10.0.0.1:8848").healthy = false;
		List<String> healthy = mgr.getMemberAddressInfos();
		check("N21: unhealthy excluded", healthy.size() == 2
				&& !healthy.contains("10.0.0.1:8848"));
		check("N21: isUnHealth", mgr.isUnHealth("10.0.0.1:8848")
				&& !mgr.isUnHealth("10.0.0.2:8848"));

		// ---- 5. lookup factory ----
		check("N21: empty → standalone",
				LookupFactory.decide("").equals(LookupType.STANDALONE));
		check("N21: file config → file",
				LookupFactory.decide("file").equals(LookupType.FILE_CONFIG));
		check("N21: address-server → address-server",
				LookupFactory.decide("address-server").equals(LookupType.ADDRESS_SERVER));
		check("N21: unknown → default address-server",
				LookupFactory.decide("xyz").equals(LookupType.ADDRESS_SERVER));

		// ---- 6. change event + listener ----
		MemberChangeListener listener = new MemberChangeListener();
		listener.onEvent(new MembersChangeEvent(List.of("m1", "m2")));
		check("N21: listener receives members", listener.received.size() == 2);

		// ---- 7. remove ----
		mgr.removeMember("10.0.0.1:8848");
		check("N21: removed member gone", mgr.serverList.size() == 2
				&& mgr.changeLog.get(2).equals("REMOVE:10.0.0.1:8848"));

		System.out.println("===== MiniCluster: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
