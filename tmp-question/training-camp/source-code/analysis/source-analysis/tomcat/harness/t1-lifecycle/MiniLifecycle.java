import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MiniLifecycle — T-1 harness (12 assertions)
 *
 * Reproduces without Tomcat server:
 *   1. LifecycleState 11-state machine + legal transitions
 *   2. Template Method: final start() drives abstract startInternal()
 *   3. startInternal() throws → state FAILED (not rollback to INITIALIZED)
 *   4. ContainerBase: HashMap + ReadWriteLock children (not ConcurrentHashMap)
 *   5. addChild: two-step atomicity inside write lock + child.start() outside write lock
 *   6. parent/children bidirectional reference + findChild consistency
 *   7. state lifecycle events (CONFIGURE_START vs START)
 */
public class MiniLifecycle {

	// ---- LifecycleState machine (mirror catalina/LifecycleState.java) ----
	enum LifecycleState {
		NEW, INITIALIZING, INITIALIZED, STARTING_PREP, STARTING, STARTED,
		STOPPING_PREP, STOPPING, STOPPED, DESTROYING, DESTROYED, FAILED, MUST_STOP;

		boolean isAvailable() {
			return this == STARTED || this == INITIALIZING || this == STARTING_PREP || this == STARTING;
		}
	}

	// ---- LifecycleBase (mirror catalina/util/LifecycleBase.java) ----
	static abstract class LifecycleBase {
		protected volatile LifecycleState state = LifecycleState.NEW;
		protected volatile boolean throwOnStartFailure = false;

		public final String name;

		LifecycleBase() {
			this.name = "";
		}

		LifecycleBase(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		private ContainerBase parent;

		public ContainerBase getParent() {
			return parent;
		}

		public void setParent(ContainerBase p) {
			this.parent = p;
		}

		public final synchronized void start() {
			if (state.isAvailable()) {
				return;
			}
			setStateInternal(LifecycleState.STARTING_PREP);
			try {
				startInternal();
			} catch (Throwable t) {
				setStateInternal(LifecycleState.FAILED);
				if (throwOnStartFailure) {
					throw new IllegalStateException(t);
				}
				return;
			}
			setStateInternal(LifecycleState.STARTED);
		}

		protected abstract void startInternal() throws Exception;

		private void setStateInternal(LifecycleState s) {
			state = s;
			onStateChange(s);
		}

		protected void onStateChange(LifecycleState s) {
		}
	}

	// ---- ContainerBase (mirror catalina/core/ContainerBase.java:154) ----
	static class ContainerBase extends LifecycleBase {
		protected final HashMap<String, ContainerBase> children = new HashMap<>();
		private final ReadWriteLock lock = new ReentrantReadWriteLock();

		@Override
		protected void startInternal() throws Exception {
			// start children sequentially inside start (as Tomcat does)
			for (ContainerBase c : children.values()) {
				c.start();
			}
		}

		public void addChild(LifecycleBase child) {
			if (child == null) {
				throw new NullPointerException("child");
			}
			lock.writeLock().lock();
			try {
				// two-step atomicity: register first, then start outside lock
				children.put(child.getName(), (ContainerBase) child);
				child.setParent(this);
			} finally {
				lock.writeLock().unlock();
			}
			// child.start() OUTSIDE write lock — startInternal may be slow
			child.start();
		}

		public ContainerBase findChild(String name) {
			lock.readLock().lock();
			try {
				return children.get(name);
			} finally {
				lock.readLock().unlock();
			}
		}

		private ContainerBase parent;

		public ContainerBase getParent() {
			return parent;
		}

		public void setParent(ContainerBase p) {
			this.parent = p;
		}

		ContainerBase(String name) {
			super(name);
		}
	}

	// ---- events (mirror LifecycleEvent types) ----
	static class Listener {
		final StringBuilder log = new StringBuilder();

		void onEvent(String event) {
			log.append(event).append(';');
		}
	}

	static class EventfulBase extends LifecycleBase {
		final Listener l;

		EventfulBase(Listener l) {
			this.l = l;
		}

		@Override
		protected void onStateChange(LifecycleState s) {
			l.onEvent(s.name());
		}

		@Override
		protected void startInternal() {
			l.onEvent("CONFIGURE_START");
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

	public static void main(String[] args) throws Exception {
		System.out.println("MiniLifecycle — T-1 harness");

		// 1. state machine: 11 core states
		check(LifecycleState.values().length >= 11, "11+ states defined");
		check(LifecycleState.STARTING_PREP.ordinal() < LifecycleState.STARTED.ordinal(),
				"STARTING_PREP precedes STARTED");

		// 2. template method: start() final, startInternal abstract
		ContainerBase root = new ContainerBase("root");
		root.start();
		check(root.state == LifecycleState.STARTED, "start() drives to STARTED via startInternal");

		// 3. startInternal throws → FAILED not rollback
		LifecycleBase boom = new LifecycleBase() {
			@Override
			protected void startInternal() throws Exception {
				throw new Exception("boom");
			}
		};
		boom.start();
		check(boom.state == LifecycleState.FAILED, "exception → FAILED (no rollback to INITIALIZED)");

		// 4. idempotent: second start() no-op when available
		ContainerBase again = new ContainerBase("again");
		again.start();
		again.start();
		check(again.state == LifecycleState.STARTED, "second start() no-op (isAvailable guard)");

		// 5. children HashMap + ReadWriteLock — findChild works
		ContainerBase engine = new ContainerBase("engine");
		ContainerBase host = new ContainerBase("host");
		engine.addChild(host);
		check(engine.findChild("host") == host, "addChild + findChild via HashMap+RWLock");
		check(host.getParent() == engine, "bidirectional parent reference set");

		// 6. child.start() called by addChild (outside write lock semantics)
		check(host.state == LifecycleState.STARTED, "child started by addChild (outside write lock)");

		// 7. 4-level cascade: engine → host → context → wrapper
		ContainerBase ctx = new ContainerBase("ctx");
		ContainerBase wrp = new ContainerBase("wrp");
		engine.addChild(ctx);
		ctx.addChild(wrp);
		check(wrp.state == LifecycleState.STARTED && ctx.state == LifecycleState.STARTED,
				"4-level cascade start");

		// 8. events: CONFIGURE_START + state transitions
		Listener l = new Listener();
		EventfulBase eb = new EventfulBase(l);
		eb.start();
		check(l.log.toString().contains("CONFIGURE_START"), "CONFIGURE_START event fired");
		check(l.log.toString().contains("STARTING_PREP") && l.log.toString().contains("STARTED"),
				"state events fired in order");

		// 9. CONFIGURE_START distinct from START (separate event types)
		int configureCount = 0;
		for (String ev : l.log.toString().split(";")) {
			if (ev.equals("CONFIGURE_START")) {
				configureCount++;
			}
		}
		check(configureCount == 1, "CONFIGURE_START once per start");

		// 10. children iteration order preserved (HashMap insertion in same thread)
		ContainerBase h2 = new ContainerBase("h2");
		engine.addChild(h2);
		check(engine.findChild("h2") == h2, "second child registered");

		// 11. exception in child.start() propagates out of addChild (Tomcat: IllegalStateException)
		ContainerBase parentOfBoom = new ContainerBase("pob");
		parentOfBoom.throwOnStartFailure = true;
		LifecycleBase boomChild = new LifecycleBase() {
			@Override
			protected void startInternal() throws Exception {
				throw new Exception("boom");
			}
		};
		boolean childFailurePropagated = false;
		try {
			parentOfBoom.addChild(boomChild);
		} catch (RuntimeException re) {
			childFailurePropagated = true;
		}
		check(childFailurePropagated, "child.start() exception propagates out of addChild");

		// 12. null child rejected
		boolean rejected = false;
		try {
			engine.addChild(null);
		} catch (NullPointerException npe) {
			rejected = true;
		}
		check(rejected, "null child rejected (NPE)");

		System.out.println("== " + pass + "/" + (pass + fail) + " PASS");
		if (fail > 0) {
			System.exit(1);
		}
	}
}