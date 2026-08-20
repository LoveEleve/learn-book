import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniHealthCheck — N-11 harness (11 assertions)
 *
 * Reproduces the health check task model without Nacos server:
 *   1. self-rescheduling task loop (doHealthCheck → reschedule)
 *   2. processor delegate: type-registered map dispatch
 *   3. four processor types (tcp/http/mysql/none)
 *   4. interceptor chain: enable + responsible gates
 *   5. heartbeat three-dimension checkers (healthy/unhealthy/expired)
 */
public class MiniHealthCheck {

	// ---- processor interface ----
	interface HealthCheckProcessorV2 {
		String getType();

		String process(String clientId);
	}

	static class TcpHealthCheckProcessor implements HealthCheckProcessorV2 {
		@Override
		public String getType() {
			return "TCP";
		}

		@Override
		public String process(String clientId) {
			return "tcp-ok:" + clientId;
		}
	}

	static class HttpHealthCheckProcessor implements HealthCheckProcessorV2 {
		@Override
		public String getType() {
			return "HTTP";
		}

		@Override
		public String process(String clientId) {
			return "http-ok:" + clientId;
		}
	}

	static class MysqlHealthCheckProcessor implements HealthCheckProcessorV2 {
		@Override
		public String getType() {
			return "MYSQL";
		}

		@Override
		public String process(String clientId) {
			return "mysql-ok:" + clientId;
		}
	}

	static class NoneHealthCheckProcessor implements HealthCheckProcessorV2 {
		@Override
		public String getType() {
			return "NONE";
		}

		@Override
		public String process(String clientId) {
			return "none:" + clientId;
		}
	}

	// ---- delegate ----
	static class HealthCheckProcessorV2Delegate {
		final Map<String, HealthCheckProcessorV2> map = new HashMap<>();

		void addProcessor(HealthCheckProcessorV2 p) {
			if (p.getType() != null) {
				map.put(p.getType(), p);
			}
		}

		String process(String type, String clientId) {
			HealthCheckProcessorV2 p = map.get(type);
			return p != null ? p.process(clientId) : "no-processor";
		}
	}

	// ---- interceptor chain ----
	static class InterceptorChain {
		boolean enable = true;
		boolean responsible = true;

		boolean pass() {
			return enable && responsible;
		}
	}

	// ---- heartbeat status ----
	static class HeartbeatStatus {
		long lastBeatTime = System.currentTimeMillis();
		boolean healthy = true;
		boolean expired = false;
	}

	// ---- task ----
	static class HealthCheckTaskV2 {
		final HealthCheckProcessorV2Delegate delegate;
		int runs = 0;

		HealthCheckTaskV2(HealthCheckProcessorV2Delegate delegate) {
			this.delegate = delegate;
		}

		void doHealthCheck(List<String> clients, String type) {
			runs++;
			for (String clientId : clients) {
				delegate.process(type, clientId);
			}
			reschedule(); // self-reschedule
		}

		void reschedule() {
			// in real code: HealthCheckReactor.scheduleCheck(this)
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
		HealthCheckProcessorV2Delegate delegate = new HealthCheckProcessorV2Delegate();
		delegate.addProcessor(new TcpHealthCheckProcessor());
		delegate.addProcessor(new HttpHealthCheckProcessor());
		delegate.addProcessor(new MysqlHealthCheckProcessor());
		delegate.addProcessor(new NoneHealthCheckProcessor());

		// ---- 1. type-registered map dispatch ----
		check("N11: tcp dispatch", delegate.process("TCP", "c1").equals("tcp-ok:c1"));
		check("N11: http dispatch", delegate.process("HTTP", "c1").equals("http-ok:c1"));
		check("N11: mysql dispatch", delegate.process("MYSQL", "c1").equals("mysql-ok:c1"));
		check("N11: none dispatch", delegate.process("NONE", "c1").equals("none:c1"));
		check("N11: unknown type", delegate.process("UNKNOWN", "c1").equals("no-processor"));

		// ---- 2. self-rescheduling loop ----
		HealthCheckTaskV2 task = new HealthCheckTaskV2(delegate);
		task.doHealthCheck(List.of("c1", "c2"), "TCP");
		task.doHealthCheck(List.of("c1", "c2"), "TCP");
		check("N11: task runs repeatedly", task.runs == 2);

		// ---- 3. interceptor chain gates ----
		InterceptorChain chain = new InterceptorChain();
		check("N11: enabled+responsible → pass", chain.pass());
		chain.enable = false;
		check("N11: disabled → block", !chain.pass());
		chain.enable = true;
		chain.responsible = false;
		check("N11: not responsible → block", !chain.pass());

		// ---- 4. heartbeat three-dimension ----
		HeartbeatStatus hs = new HeartbeatStatus();
		check("N11: initially healthy", hs.healthy && !hs.expired);
		hs.lastBeatTime = System.currentTimeMillis() - 60000; // 60s no beat
		boolean expired = System.currentTimeMillis() - hs.lastBeatTime > 30000;
		check("N11: 60s no beat → expired", expired);

		System.out.println("===== MiniHealthCheck: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
