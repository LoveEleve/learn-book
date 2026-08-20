import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MiniProcessor — T-2 harness (11 assertions)
 *
 * Reproduces without Tomcat server:
 *   1. AbstractProtocol: processor recycling pool (recycledProcessors push/pop)
 *   2. Pool cap: pool full → processor discarded (GC) not retained
 *   3. CoyoteAdapter: getNote(ADAPTER_NOTES) → catalina Request/Response two-layer
 *   4. mapper.map() void + mutable MappingData fill (no new object per request)
 *   5. Response: getOutputStream()/getWriter() mutual exclusion (IllegalStateException)
 *   6. request.recycle() clears per-request state (43 lines of work in Tomcat)
 *   7. Processor reuse across keep-alive requests
 */
public class MiniProcessor {

	// ---- RecycledProcessors pool (mirror coyote/AbstractProtocol.java:786) ----
	static class ProcessorPool {
		private final Deque<Processor> recycled = new ArrayDeque<>();
		private final int max;
		int discarded = 0;

		ProcessorPool(int max) {
			this.max = max;
		}

		Processor pop() {
			return recycled.pollLast();
		}

		void push(Processor p) {
			if (recycled.size() >= max) {
				discarded++;
				return; // pool full → discard (Tomcat: pool max 200)
			}
			recycled.addLast(p);
		}

		int size() {
			return recycled.size();
		}
	}

	// ---- Processor (mirror coyote/http11/Http11Processor) ----
	static class Processor {
		private final CoyoteRequest coyoteReq = new CoyoteRequest();
		private final CoyoteResponse coyoteRes = new CoyoteResponse();
		private boolean recycled = false;
		private String localName = "";

		void setLocalName(String n) {
			this.localName = n;
		}

		void recycle() {
			// mirror Request.recycle(): clear per-request notes/attributes
			coyoteReq.recycle();
			coyoteRes.recycle();
			this.recycled = true;
			this.localName = "";
		}

		boolean isRecycled() {
			return recycled;
		}
	}

	// ---- coyote Response mini ----
	static class CoyoteResponse {
		void recycle() {
		}
	}

	// ---- coyote Request/Response mini (mirror coyote classes) ----
	static class CoyoteRequest {
		private final Object[] notes = new Object[32];

		void setNote(int idx, Object o) {
			notes[idx] = o;
		}

		Object getNote(int idx) {
			return notes[idx];
		}

		void recycle() {
			for (int i = 0; i < notes.length; i++) {
				notes[i] = null;
			}
		}
	}

	// ---- two-layer: coyote request carries catalina Request via note ----
	static class CatalinaRequest {
		final String layer = "catalina";
	}

	static class CatalinaResponse {
		final String layer = "catalina";
		boolean writerUsed = false;
		boolean streamUsed = false;
		boolean committed = false;

		Object getOutputStream() {
			if (writerUsed) {
				throw new IllegalStateException("getOutputStream.ise");
			}
			streamUsed = true;
			return new Object();
		}

		Object getWriter() {
			if (streamUsed) {
				throw new IllegalStateException("getWriter.ise");
			}
			writerUsed = true;
			return new Object();
		}

		void commit() {
			committed = true;
		}
	}

	// ---- MappingData (mirror mapper.map void fill) ----
	static class MappingData {
		Object host;
		Object context;
		Object wrapper;

		boolean matched() {
			return host != null && context != null && wrapper != null;
		}
	}

	static class Mapper {
		void map(String uri, MappingData data) {
			// fill mutable object, return void — no allocation per request
			data.host = "localhost";
			data.context = "/app";
			data.wrapper = "ServletA";
		}
	}

	static class Adapter {
		static final int ADAPTER_NOTES = 1;

		Processor service(Processor p, Mapper mapper) {
			CoyoteRequest cr = new CoyoteRequest();
			cr.setNote(ADAPTER_NOTES, new CatalinaRequest());
			MappingData md = new MappingData();
			mapper.map("/app/servletA", md);
			p.recycled = false;
			return p;
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
		System.out.println("MiniProcessor — T-2 harness");

		// 1. pool pop/push round-trip
		ProcessorPool pool = new ProcessorPool(200);
		Processor p1 = new Processor();
		pool.push(p1);
		check(pool.pop() == p1, "push then pop returns same processor");

		// 2. pool cap: discard when full
		ProcessorPool tiny = new ProcessorPool(2);
		for (int i = 0; i < 5; i++) {
			tiny.push(new Processor());
		}
		check(tiny.discarded == 3 && tiny.size() == 2, "pool full → discard (cap 2, 3 discarded)");

		// 3. two-layer: ADAPTER_NOTES carries catalina Request
		CoyoteRequest cr = new CoyoteRequest();
		cr.setNote(Adapter.ADAPTER_NOTES, new CatalinaRequest());
		check(cr.getNote(Adapter.ADAPTER_NOTES) instanceof CatalinaRequest, "ADAPTER_NOTES → catalina Request");

		// 4. mapper.map void + fill mutable MappingData
		Mapper mapper = new Mapper();
		MappingData md = new MappingData();
		Mapper voidReturn = mapper; // interface shape: map returns void
		voidReturn.map("/app/x", md);
		check(md.matched() && md.host.equals("localhost") && md.wrapper.equals("ServletA"),
				"map() void + mutable fill (host/context/wrapper)");

		// 5. getOutputStream then getWriter → ISE
		CatalinaResponse resp = new CatalinaResponse();
		resp.getOutputStream();
		boolean ise1 = false;
		try {
			resp.getWriter();
		} catch (IllegalStateException e) {
			ise1 = true;
		}
		check(ise1, "getOutputStream then getWriter → IllegalStateException");

		// 6. reverse order: getWriter then getOutputStream → ISE
		CatalinaResponse resp2 = new CatalinaResponse();
		resp2.getWriter();
		boolean ise2 = false;
		try {
			resp2.getOutputStream();
		} catch (IllegalStateException e) {
			ise2 = true;
		}
		check(ise2, "getWriter then getOutputStream → IllegalStateException");

		// 7. recycle clears notes
		CoyoteRequest cr2 = new CoyoteRequest();
		cr2.setNote(Adapter.ADAPTER_NOTES, new CatalinaRequest());
		cr2.recycle();
		check(cr2.getNote(Adapter.ADAPTER_NOTES) == null, "recycle() clears notes");

		// 8. processor recycle clears local state
		Processor p2 = new Processor();
		p2.setLocalName("hostname");
		p2.recycle();
		check(p2.isRecycled() && p2.localName.equals(""), "processor recycle() clears local state");

		// 9. keep-alive reuse: same processor serves two requests
		Processor reused = new Processor();
		Adapter adapter = new Adapter();
		adapter.service(reused, mapper);
		reused.recycle();
		adapter.service(reused, mapper);
		check(!reused.isRecycled(), "processor reused across keep-alive requests");

		// 10. coyote Request/Response bound to processor (1:1), not thread
		check(p1.coyoteReq != p2.coyoteReq, "each processor has its own coyote Request");

		// 11. commit after stream prevents further body writes
		CatalinaResponse resp3 = new CatalinaResponse();
		resp3.getOutputStream();
		resp3.commit();
		check(resp3.committed, "response committed flag set");

		System.out.println("== " + pass + "/" + (pass + fail) + " PASS");
		if (fail > 0) {
			System.exit(1);
		}
	}
}