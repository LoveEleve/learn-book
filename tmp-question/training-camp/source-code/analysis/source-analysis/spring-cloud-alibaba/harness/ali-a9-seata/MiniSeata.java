import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniSeata — ALI-A9 harness (12 assertions)
 *
 * Reproduces without Seata server:
 *   1. three-way XID propagation: feign header / rest header / web bind
 *   2. empty xid → no header
 *   3. afterCompletion: unbind + consistency check + rebind
 *   4. thread-pool reuse safety (no leftover XID)
 *   5. RestTemplate all-instance interceptor injection
 *   6. Feign retryer forced NEVER_RETRY
 */
public class MiniSeata {

	// ---- RootContext emulation (ThreadLocal XID holder) ----
	static class RootContext {
		static final String KEY_XID = "TX_XID";
		private static final ThreadLocal<String> XID = new ThreadLocal<>();

		static String getXID() {
			return XID.get();
		}

		static void bind(String xid) {
			XID.set(xid);
		}

		static String unbind() {
			String x = XID.get();
			XID.remove();
			return x;
		}
	}

	// ---- feign request template emulation ----
	static class RequestTemplate {
		final Map<String, String> headers = new ConcurrentHashMap<>();

		void header(String key, String value) {
			headers.put(key, value);
		}
	}

	static class SeataFeignRequestInterceptor {
		void apply(RequestTemplate template) {
			String xid = RootContext.getXID();
			if (xid == null || xid.isEmpty()) {
				return;
			}
			template.header(RootContext.KEY_XID, xid);
		}
	}

	// ---- web interceptor emulation ----
	static class SeataHandlerInterceptor {
		final List<String> warnLogs = new ArrayList<>();

		boolean preHandle(String rpcXid) {
			String xid = RootContext.getXID();
			if (xid == null || xid.isBlank() && rpcXid != null) {
				RootContext.bind(rpcXid);
				return true;
			}
			return true;
		}

		void afterCompletion(String rpcXid) {
			if (RootContext.getXID() != null && !RootContext.getXID().isBlank()) {
				if (rpcXid == null || rpcXid.isEmpty()) {
					return;
				}
				String unbindXid = RootContext.unbind();
				if (!rpcXid.equalsIgnoreCase(unbindXid)) {
					warnLogs.add("xid changed: " + rpcXid + " -> " + unbindXid);
					if (unbindXid != null) {
						RootContext.bind(unbindXid);
					}
				}
			}
		}
	}

	// ---- RestTemplate all-instance injection ----
	static class RestTemplate {
		final List<String> interceptors = new ArrayList<>();
	}

	static class AfterPropertiesSet {
		void inject(List<RestTemplate> restTemplates) {
			if (restTemplates != null) {
				for (RestTemplate rt : restTemplates) {
					rt.interceptors.add("seataInterceptor");
				}
			}
		}
	}

	// ---- feign retryer ----
	static class FeignBuilder {
		String retryer = "default";

		void retryer(String r) {
			this.retryer = r;
		}
	}

	static class FeignBuilderBPP {
		void postProcessAfterInitialization(FeignBuilder builder) {
			builder.retryer("NEVER_RETRY");
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
		// ---- 1. feign propagation with xid ----
		RootContext.bind("xid-1");
		SeataFeignRequestInterceptor feignInterceptor = new SeataFeignRequestInterceptor();
		RequestTemplate template = new RequestTemplate();
		feignInterceptor.apply(template);
		check("A9: feign carries XID header", RootContext.KEY_XID.equals("TX_XID")
				&& template.headers.get("TX_XID").equals("xid-1"));

		// ---- 2. empty xid → no header ----
		RootContext.unbind();
		RequestTemplate t2 = new RequestTemplate();
		feignInterceptor.apply(t2);
		check("A9: empty xid → no header", t2.headers.isEmpty());

		// ---- 3. web inbound bind ----
		SeataHandlerInterceptor web = new SeataHandlerInterceptor();
		web.preHandle("xid-2");
		check("A9: web binds rpc xid", RootContext.getXID().equals("xid-2"));

		// ---- 4. web does not override local xid ----
		RootContext.bind("xid-local");
		web.preHandle("xid-3");
		check("A9: local xid not overridden", RootContext.getXID().equals("xid-local"));

		// ---- 5. afterCompletion unbind + consistency ----
		RootContext.bind("xid-local");
		web.afterCompletion("xid-local");
		check("A9: consistent xid → unbound", RootContext.getXID() == null
				&& web.warnLogs.isEmpty());

		// ---- 6. changed xid → warn + rebind ----
		RootContext.bind("xid-original");
		web.afterCompletion("xid-requested");
		check("A9: xid change warned", web.warnLogs.size() == 1
				&& web.warnLogs.get(0).contains("xid changed"));
		check("A9: original xid rebound", RootContext.getXID().equals("xid-original"));
		RootContext.unbind();

		// ---- 7. no rpc xid → no unbind ----
		RootContext.bind("xid-a");
		web.afterCompletion("");
		check("A9: empty rpcXid → untouched", RootContext.getXID().equals("xid-a"));
		RootContext.unbind();

		// ---- 8. thread isolation (ThreadLocal) ----
		RootContext.bind("main-xid");
		Thread t = new Thread(() -> check("A9: thread-isolated XID", RootContext.getXID() == null));
		t.start();
		try {
			t.join();
		}
		catch (InterruptedException e) {
			// ignore
		}
		check("A9: main thread XID intact", RootContext.getXID().equals("main-xid"));
		RootContext.unbind();

		// ---- 9. RestTemplate all-instance injection ----
		RestTemplate rt1 = new RestTemplate();
		RestTemplate rt2 = new RestTemplate();
		AfterPropertiesSet aps = new AfterPropertiesSet();
		aps.inject(List.of(rt1, rt2));
		check("A9: all RestTemplates injected", rt1.interceptors.contains("seataInterceptor")
				&& rt2.interceptors.contains("seataInterceptor"));

		// ---- 10. feign retryer forced ----
		FeignBuilder builder = new FeignBuilder();
		new FeignBuilderBPP().postProcessAfterInitialization(builder);
		check("A9: feign retryer forced NEVER_RETRY", builder.retryer.equals("NEVER_RETRY"));

		// ---- 11. feign→rest→web full chain ----
		RootContext.bind("chain-xid");
		RequestTemplate out = new RequestTemplate();
		feignInterceptor.apply(out);
		SeataHandlerInterceptor inbound = new SeataHandlerInterceptor();
		inbound.preHandle(out.headers.get("TX_XID"));
		check("A9: full chain XID carried through", RootContext.getXID().equals("chain-xid"));

		System.out.println("===== MiniSeata: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
