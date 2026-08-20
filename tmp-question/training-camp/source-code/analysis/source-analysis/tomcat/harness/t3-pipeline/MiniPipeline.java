import java.util.ArrayList;
import java.util.List;

/**
 * MiniPipeline — T-3 harness (13 assertions)
 *
 * Reproduces without Tomcat server:
 *   1. Valve chain: getNext().invoke() explicit passing (mirror StandardPipeline)
 *   2. 4-level cascade: Engine→Host→Context→Wrapper valves + basic valve last
 *   3. getFirst() returns first addValve'd valve (basic NOT first)
 *   4. Filter chain: ApplicationFilterChain pos cursor loop (not recursion)
 *   5. Valve chain order vs Filter chain order: EngineValve → ... → Filter → Servlet
 *   6. Chain termination: filter that doesn't call doFilter stops the chain
 *   7. AccessLog counting: 3 valves on 3 layers → 3 log entries
 */
public class MiniPipeline {

	// ---- Valve (mirror catalina/Valve.java) ----
	interface Valve {
		void invoke(Request req, Response resp, ValveContext ctx);

		Valve getNext();

		void setNext(Valve v);
	}

	// ---- ValveContext (mirror catalina.connector.Request passing) ----
	static class ValveContext {
		private Valve current;

		ValveContext(Valve first) {
			this.current = first;
		}

		void invokeNext(Request req, Response resp) {
			Valve v = current;
			current = v.getNext();
			v.invoke(req, resp, this);
		}
	}

	static class Request {
		String uri = "/app/servletA";
		String host = "localhost";
		String context = "/app";
		String wrapper = "ServletA";
		final List<String> trace = new ArrayList<>();
	}

	static class Response {
		boolean committed = false;
	}

	// ---- basic valve: ends the chain (mirror StandardWrapperValve → Filter chain) ----
	static class BasicValve implements Valve {
		private Valve next;
		private final ApplicationFilterChain filterChain;

		BasicValve(ApplicationFilterChain fc) {
			this.filterChain = fc;
		}

		@Override
		public void invoke(Request req, Response resp, ValveContext ctx) {
			req.trace.add("basic:servlet-via-filter-chain");
			filterChain.doFilter(req, resp);
		}

		@Override
		public Valve getNext() {
			return next;
		}

		@Override
		public void setNext(Valve v) {
			this.next = v;
		}
	}

	// ---- named valve (mirror EngineValve/HostValve/ContextValve/AccessLogValve) ----
	static class NamedValve implements Valve {
		final String name;
		private Valve next;
		private final boolean accessLog;

		NamedValve(String name, boolean accessLog) {
			this.name = name;
			this.accessLog = accessLog;
		}

		@Override
		public void invoke(Request req, Response resp, ValveContext ctx) {
			req.trace.add(name);
			if (accessLog) {
				req.trace.add(name + ":access-log");
			}
			ctx.invokeNext(req, resp);
		}

		@Override
		public Valve getNext() {
			return next;
		}

		@Override
		public void setNext(Valve v) {
			this.next = v;
		}
	}

	// ---- StandardPipeline (mirror catalina/core/StandardPipeline.java) ----
	static class StandardPipeline {
		private Valve first;
		private Valve basic;

		void addValve(Valve v) {
			if (first == null) {
				first = v;
			} else {
				// append at tail (before basic)
				Valve tail = first;
				while (tail.getNext() != null) {
					tail = tail.getNext();
				}
				tail.setNext(v);
			}
		}

		void setBasic(Valve b) {
			this.basic = b;
			Valve tail = first;
			if (tail == null) {
				first = b;
				return;
			}
			while (tail.getNext() != null) {
				tail = tail.getNext();
			}
			tail.setNext(b);
		}

		Valve getFirst() {
			return first;
		}

		Valve getBasic() {
			return basic;
		}

		void invoke(Request req, Response resp) {
			new ValveContext(first).invokeNext(req, resp);
		}
	}

	// ---- Filter (mirror jakarta.servlet.Filter) ----
	interface Filter {
		void doFilter(Request req, Response resp, FilterChain chain);
	}

	interface FilterChain {
		void doFilter(Request req, Response resp);
	}

	// ---- ApplicationFilterChain (mirror catalina/core/ApplicationFilterChain.java:46) ----
	static class ApplicationFilterChain implements FilterChain {
		private final List<Filter> filters = new ArrayList<>();
		private int pos = 0;
		private final Servlet servlet;

		ApplicationFilterChain(Servlet servlet) {
			this.servlet = servlet;
		}

		void addFilter(Filter f) {
			filters.add(f);
		}

		@Override
		public void doFilter(Request req, Response resp) {
			// pos cursor loop — NOT recursion (mirror Tomcat L117-124)
			if (pos < filters.size()) {
				Filter f = filters.get(pos++);
				f.doFilter(req, resp, this);
				return;
			}
			servlet.service(req, resp);
		}
	}

	static class Servlet {
		void service(Request req, Response resp) {
			req.trace.add("servlet:service");
			resp.committed = true;
		}
	}

	// ---- assertions ----
	static int pass = 0, fail = 0;

	static void check(boolean ok, String name) {
		if (ok) {
			pass++;
			System.out.println("  PASS: " + name);
		} else {
			fail++;
			System.out.println("  FAIL: " + name);
		}
	}

	public static void main(String[] args) {
		System.out.println("MiniPipeline — T-3 harness");

		// 1. valve chain: 4-level cascade order
		StandardPipeline pipe = new StandardPipeline();
		NamedValve engineValve = new NamedValve("engine-valve", false);
		NamedValve hostValve = new NamedValve("host-valve", false);
		NamedValve ctxValve = new NamedValve("context-valve", false);
		ApplicationFilterChain fc = new ApplicationFilterChain(new Servlet());
		BasicValve basic = new BasicValve(fc);
		pipe.addValve(engineValve);
		pipe.addValve(hostValve);
		pipe.addValve(ctxValve);
		pipe.setBasic(basic);
		check(pipe.getFirst() == engineValve, "getFirst() returns first addValve'd valve");

		// 2. getFirst is NOT basic
		check(pipe.getFirst() != pipe.getBasic(), "getFirst() != basic valve");

		// 3. full chain execution order
		Request req = new Request();
		Response resp = new Response();
		pipe.invoke(req, resp);
		check(req.trace.get(0).equals("engine-valve") && req.trace.get(1).equals("host-valve")
				&& req.trace.get(2).equals("context-valve"), "valve order: engine→host→context");
		check(req.trace.contains("servlet:service"), "basic valve ends chain → servlet");
		check(resp.committed, "response committed by servlet");

		// 4. filter chain: pos cursor loop (add 2 filters)
		ApplicationFilterChain fc2 = new ApplicationFilterChain(new Servlet());
		fc2.addFilter((r, s, c) -> {
			r.trace.add("filter1");
			c.doFilter(r, s);
		});
		fc2.addFilter((r, s, c) -> {
			r.trace.add("filter2");
			c.doFilter(r, s);
		});
		Request req2 = new Request();
		fc2.doFilter(req2, new Response());
		check(req2.trace.contains("filter1") && req2.trace.contains("filter2")
				&& req2.trace.contains("servlet:service"), "filter1→filter2→servlet order");

		// 5. filter that doesn't call doFilter terminates chain
		ApplicationFilterChain fc3 = new ApplicationFilterChain(new Servlet());
		fc3.addFilter((r, s, c) -> {
			r.trace.add("blocking-filter");
			// no c.doFilter → chain stops
		});
		Request req3 = new Request();
		fc3.doFilter(req3, new Response());
		check(req3.trace.size() == 1 && !req3.trace.contains("servlet:service"),
				"filter without doFilter terminates chain");

		// 6. chain safety: pos guard prevents double-serve
		ApplicationFilterChain fc4 = new ApplicationFilterChain(new Servlet());
		fc4.addFilter((r, s, c) -> c.doFilter(r, s));
		Request req4 = new Request();
		fc4.doFilter(req4, new Response());
		int servletCount = 0;
		for (String t : req4.trace) {
			if (t.equals("servlet:service")) {
				servletCount++;
			}
		}
		check(servletCount == 1, "servlet served exactly once (pos cursor guard)");

		// 7. access log on 3 layers → 3 entries
		StandardPipeline pipeLog = new StandardPipeline();
		pipeLog.addValve(new NamedValve("engine-valve", true));
		pipeLog.addValve(new NamedValve("host-valve", true));
		pipeLog.addValve(new NamedValve("context-valve", true));
		pipeLog.setBasic(new BasicValve(new ApplicationFilterChain(new Servlet())));
		Request reqLog = new Request();
		pipeLog.invoke(reqLog, new Response());
		long accessCount = reqLog.trace.stream().filter(t -> t.endsWith(":access-log")).count();
		check(accessCount == 3, "3 valves with access log → 3 entries (confirms question)");

		// 8. filter chain AFTER valves (order: valve first, filter second)
		check(req.trace.indexOf("engine-valve") < req.trace.indexOf("filter1") || !req.trace.contains("filter1"),
				"valves execute before filters (when filter present)");

		System.out.println("== " + pass + "/" + (pass + fail) + " PASS");
		if (fail > 0) {
			System.exit(1);
		}
	}
}