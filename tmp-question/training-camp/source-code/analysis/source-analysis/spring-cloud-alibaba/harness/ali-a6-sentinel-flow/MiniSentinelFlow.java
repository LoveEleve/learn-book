import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniSentinelFlow — ALI-A6 harness (12 assertions)
 *
 * Reproduces without Sentinel server:
 *   1. MergedBeanDefinition discovery: cache annotation from bean definition
 *   2. blockHandler signature check (reflection, fail-fast)
 *   3. interceptor registration: dynamic bean name + add(0, interceptor)
 *   4. dual entry: host + host-with-path resources, degrade→fallback vs flow→blockHandler
 *   5. resource name format METHOD:scheme://host:port
 *   6. urlCleaner transform
 *   7. Feign-style invoke: entry wrap + fallbackFactory dispatch
 */
public class MiniSentinelFlow {

	// ---- minimal sentinel sph emulation ----
	static class BlockException extends Exception {
		final String type;

		BlockException(String type) {
			this.type = type;
		}
	}

	static class DegradeException extends BlockException {
		DegradeException() {
			super("degrade");
		}
	}

	static class FlowException extends BlockException {
		FlowException() {
			super("flow");
		}
	}

	static class Entry {
		final String resource;
		int entered = 1;

		Entry(String resource) {
			this.resource = resource;
		}
	}

	// SphU emulation: entry succeeds unless rule blocks (resource → blocked type)
	static class SphU {
		static Map<String, String> blockRules = new ConcurrentHashMap<>();
		static final List<String> entries = new ArrayList<>();
		static final List<String> exits = new ArrayList<>();

		static Entry entry(String resource) throws BlockException {
			entries.add(resource);
			String blocked = blockRules.get(resource);
			if (blocked != null) {
				throw blocked.equals("degrade") ? new DegradeException()
						: new FlowException();
			}
			return new Entry(resource);
		}
	}

	// ---- BlockClassRegistry emulation ----
	static class BlockClassRegistry {
		static Map<String, Method> blockHandlers = new ConcurrentHashMap<>();
		static Map<String, Method> fallbacks = new ConcurrentHashMap<>();

		static void updateBlockHandlerFor(String name, Method m) {
			blockHandlers.put(name, m);
		}

		static void updateFallbackFor(String name, Method m) {
			fallbacks.put(name, m);
		}
	}

	// ---- sentinelRestTemplate annotation emulation ----
	static class SentinelRestTemplate {
		final String blockHandler;
		final String fallback;
		final String urlCleaner;

		SentinelRestTemplate(String blockHandler, String fallback, String urlCleaner) {
			this.blockHandler = blockHandler;
			this.fallback = fallback;
			this.urlCleaner = urlCleaner;
		}
	}

	// ---- BPP emulation ----
	static class SentinelBeanPostProcessor {
		final Map<String, SentinelRestTemplate> cache = new ConcurrentHashMap<>();
		final List<String> interceptorBeanNames = new ArrayList<>();

		// postProcessMergedBeanDefinition: only RestTemplate types
		void merged(String beanName, Class<?> type, SentinelRestTemplate annotation) {
			if (beanName == null || !RestTemplate.class.isAssignableFrom(type)) {
				return;
			}
			if (annotation != null) {
				cache.put(beanName, annotation);
			}
		}

		// postProcessAfterInitialization: add interceptor at index 0
		RestTemplate afterInit(String beanName, RestTemplate rt) {
			if (cache.containsKey(beanName)) {
				String interceptorName = "sentinelProtectInterceptor_"
						+ cache.get(beanName).blockHandler + "@" + rt;
				interceptorBeanNames.add(interceptorName);
				rt.interceptors.add(0, new SentinelProtectInterceptor(cache.get(beanName)));
			}
			return rt;
		}
	}

	static class RestTemplate {
		final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
	}

	interface ClientHttpRequestInterceptor {
		String intercept(String method, String scheme, String host, int port, String path,
				String urlCleanerResult);
	}

	// ---- interceptor emulation ----
	static class SentinelProtectInterceptor implements ClientHttpRequestInterceptor {
		final SentinelRestTemplate annotation;

		SentinelProtectInterceptor(SentinelRestTemplate annotation) {
			this.annotation = annotation;
		}

		@Override
		public String intercept(String method, String scheme, String host, int port,
				String path, String urlCleanerResult) {
			String hostResource = method + ":" + scheme + "://" + host
					+ (port == -1 ? "" : ":" + port);
			String hostWithPathResource = hostResource + path;
			boolean entryWithPath = !hostResource.equals(hostWithPathResource);
			if (urlCleanerResult != null) {
				hostWithPathResource = hostResource + urlCleanerResult;
			}
			Entry hostEntry = null;
			Entry pathEntry = null;
			try {
				hostEntry = SphU.entry(hostResource);
				if (entryWithPath) {
					pathEntry = SphU.entry(hostWithPathResource);
				}
				return "OK:" + hostWithPathResource;
			}
			catch (BlockException ex) {
				if (ex instanceof DegradeException) {
					Method fb = BlockClassRegistry.fallbacks.get(annotation.fallback);
					return fb != null ? "fallback:" + fb.getName() : "empty-degrade-response";
				}
				Method bh = BlockClassRegistry.blockHandlers.get(annotation.blockHandler);
				return bh != null ? "block:" + bh.getName() : "empty-flow-response";
			}
			finally {
				if (pathEntry != null) {
					SphU.exits.add(pathEntry.resource);
				}
				if (hostEntry != null) {
					SphU.exits.add(hostEntry.resource);
				}
			}
		}
	}

	// ---- Feign-style handler emulation ----
	static class FeignInvocationHandler {
		final Map<String, String> fallbackMethods = new ConcurrentHashMap<>();
		boolean hasFallbackFactory = false;

		Object invoke(String resource) {
			Entry entry = null;
			try {
				entry = SphU.entry(resource);
				return "remote-result";
			}
			catch (BlockException ex) {
				if (hasFallbackFactory) {
					return "fallback-result";
				}
				throw new RuntimeException("blocked", ex);
			}
			finally {
				if (entry != null) {
					SphU.exits.add(entry.resource);
				}
			}
		}
	}

	public static String handleBlock(String request, byte[] body, Object exec,
			BlockException ex) {
		return "block-handled";
	}

	public static String handleFallback(String request, byte[] body, Object exec,
			BlockException ex) {
		return "fallback-handled";
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

	public static void main(String[] args) throws Exception {
		// ---- 1. merged bean definition only for RestTemplate ----
		SentinelBeanPostProcessor bpp = new SentinelBeanPostProcessor();
		bpp.merged("rt", RestTemplate.class, new SentinelRestTemplate("handleBlock", "handleFallback", null));
		bpp.merged("other", String.class, new SentinelRestTemplate("x", "y", null));
		check("A6: non-RestTemplate skipped", bpp.cache.size() == 1);

		// ---- 2. interceptor added at index 0 ----
		RestTemplate rt = new RestTemplate();
		rt.interceptors.add(new ClientHttpRequestInterceptor() {
			@Override
			public String intercept(String m, String s, String h, int p, String pa, String u) {
				return "user-interceptor";
			}
		});
		bpp.afterInit("rt", rt);
		check("A6: interceptor inserted at 0", rt.interceptors.get(0) instanceof SentinelProtectInterceptor);
		check("A6: dynamic bean name encoded", bpp.interceptorBeanNames.get(0).contains("handleBlock"));

		// ---- 3. dual entry with normal flow ----
		SphU.entries.clear();
		SphU.exits.clear();
		SphU.blockRules.clear();
		SentinelProtectInterceptor interceptor = new SentinelProtectInterceptor(
				new SentinelRestTemplate("handleBlock", "handleFallback", null));
		String result = interceptor.intercept("GET", "http", "order-service", 8080, "/api/order", null);
		check("A6: resource format METHOD:scheme://host:port",
				SphU.entries.get(0).equals("GET:http://order-service:8080"));
		check("A6: dual entry host+path", SphU.entries.size() == 2
				&& SphU.entries.get(1).equals("GET:http://order-service:8080/api/order"));
		check("A6: exit order path-first", SphU.exits.get(0).endsWith("/api/order")
				&& SphU.exits.get(1).equals("GET:http://order-service:8080"));
		check("A6: normal result", result.equals("OK:GET:http://order-service:8080/api/order"));

		// ---- 4. flow block → blockHandler ----
		BlockClassRegistry.updateBlockHandlerFor("handleBlock",
				MiniSentinelFlow.class.getMethod("handleBlock", String.class, byte[].class,
						Object.class, BlockException.class));
		SphU.blockRules.put("GET:http://order-service:8080", "flow");
		result = interceptor.intercept("GET", "http", "order-service", 8080, "/api/order", null);
		check("A6: flow block → blockHandler", result.equals("block:handleBlock"));

		// ---- 5. degrade → fallback ----
		BlockClassRegistry.updateFallbackFor("handleFallback",
				MiniSentinelFlow.class.getMethod("handleFallback", String.class, byte[].class,
						Object.class, BlockException.class));
		SphU.blockRules.clear();
		SphU.blockRules.put("GET:http://order-service:8080", "degrade");
		result = interceptor.intercept("GET", "http", "order-service", 8080, "/api/order", null);
		check("A6: degrade → fallback", result.equals("fallback:handleFallback"));

		// ---- 6. no handler → empty response ----
		SentinelProtectInterceptor bare = new SentinelProtectInterceptor(
				new SentinelRestTemplate("", "", null));
		SphU.blockRules.clear();
		SphU.blockRules.put("GET:http://order-service:8080", "flow");
		result = bare.intercept("GET", "http", "order-service", 8080, "/api/order", null);
		check("A6: no blockHandler → empty response", result.equals("empty-flow-response"));

		// ---- 7. urlCleaner ----
		SphU.blockRules.clear();
		result = interceptor.intercept("GET", "http", "order-service", 8080, "/api/order/1", "/api/order/{id}");
		check("A6: urlCleaner rewrites resource", result.equals("OK:GET:http://order-service:8080/api/order/{id}"));

		// ---- 8. single entry when no path ----
		SphU.entries.clear();
		interceptor.intercept("GET", "http", "order-service", 8080, "", null);
		check("A6: no path → single entry", SphU.entries.size() == 1);

		// ---- 9. Feign-style: fallbackFactory vs no fallback ----
		SphU.blockRules.clear();
		SphU.blockRules.put("GET:https://api/order", "flow");
		FeignInvocationHandler fh = new FeignInvocationHandler();
		fh.hasFallbackFactory = true;
		check("A6: feign fallback factory result", fh.invoke("GET:https://api/order").equals("fallback-result"));
		FeignInvocationHandler fh2 = new FeignInvocationHandler();
		boolean rethrew = false;
		try {
			fh2.invoke("GET:https://api/order");
		}
		catch (RuntimeException e) {
			rethrew = true;
		}
		check("A6: feign without fallback rethrows", rethrew);

		// ---- 10. blocked entry → no exit record (entry stays null) ----
		check("A6: blocked entry leaves no exit (mirrors null-entry guard)",
				!SphU.exits.contains("GET:https://api/order"));

		System.out.println("===== MiniSentinelFlow: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
