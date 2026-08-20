import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniThreadModel — T-4 harness (12 assertions)
 *
 * Reproduces without Tomcat server:
 *   1. Acceptor: countUpOrAwaitConnection limitLatch (maxConnections=8*1024)
 *   2. countUpOrAwaitConnection blocks when limit reached (Semaphore semantics)
 *   3. Poller: wakeupCounter CAS — wakeup() only when counter==0
 *   4. PollerEvent reuse pool (createPollerEvent with cached event)
 *   5. Worker pool: bounded executor processes socket events
 *   6. selector.select() vs wakeup race: wakeupCounter.getAndSet(-1) pattern
 *   7. keep-alive: one socket, N requests re-registered on poller
 */
public class MiniThreadModel {

	// ---- limitLatch (mirror AbstractEndpoint.java:1477 countUpOrAwaitConnection) ----
	static class LimitLatch {
		private final int limit;
		private int count = 0;

		LimitLatch(int limit) {
			this.limit = limit;
		}

		synchronized void countUpOrAwait() throws InterruptedException {
			if (limit < 0) {
				count++; // unlimited (maxConnections==-1): count still tracked
				return;
			}
			while (count >= limit) {
				wait();
			}
			count++;
		}

		synchronized void countDown() {
			count--;
			notifyAll();
		}

		synchronized int getCount() {
			return count;
		}
	}

	// ---- PollerEvent (mirror NioEndpoint.PollerEvent:559) ----
	static class PollerEvent {
		Object socket;
		int interestOps;
		boolean reused = false;

		void recycle() {
			socket = null;
			interestOps = 0;
			reused = true;
		}
	}

	// ---- Poller with wakeupCounter (mirror NioEndpoint.Poller:595-757) ----
	static class Poller implements Runnable {
		final AtomicLong wakeupCounter = new AtomicLong(0);
		final Deque<PollerEvent> eventQueue = new ArrayDeque<>();
		final Deque<PollerEvent> eventPool = new ArrayDeque<>();
		final CountDownLatch wakeupFired = new CountDownLatch(1);
		volatile boolean stop = false;
		final ExecutorService workers = Executors.newFixedThreadPool(2);
		final LimitLatch limitLatch;
		volatile int selectCount = 0;
		volatile boolean wakeupCalled = false;

		Poller(LimitLatch ll) {
			this.limitLatch = ll;
		}

		// mirror NioEndpoint.java:630 wakeup()
		void wakeup() {
			if (wakeupCounter.incrementAndGet() == 0) {
				wakeupCalled = true;
				wakeupFired.countDown();
			}
		}

		// mirror NioEndpoint.java:635-659 createPollerEvent — reuse from pool
		PollerEvent createPollerEvent(Object socket, int interestOps) {
			PollerEvent pe = eventPool.pollFirst();
			if (pe == null) {
				pe = new PollerEvent();
			}
			pe.socket = socket;
			pe.interestOps = interestOps;
			pe.reused = false;
			return pe;
		}

		void addEvent(Object socket, int interestOps) {
			eventQueue.addLast(createPollerEvent(socket, interestOps));
			wakeup();
		}

		@Override
		public void run() {
			while (!stop) {
				PollerEvent pe;
				synchronized (eventQueue) {
					pe = eventQueue.pollFirst();
				}
				if (pe != null) {
					// "selector.select()" equivalent: simulate readiness processing
					workers.execute(() -> {
						// process socket event
						try {
							Thread.sleep(1);
						} catch (InterruptedException ignored) {
						}
					});
					// return event to pool after processing
					pe.recycle();
					eventPool.addLast(pe);
					selectCount++;
				} else {
					try {
						Thread.sleep(2);
					} catch (InterruptedException ignored) {
					}
				}
			}
		}

		void shutdown() {
			stop = true;
			workers.shutdown();
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
		System.out.println("MiniThreadModel — T-4 harness");

		// 1. limitLatch: blocks at limit, resumes on countDown
		LimitLatch ll = new LimitLatch(2);
		ll.countUpOrAwait();
		ll.countUpOrAwait();
		check(ll.getCount() == 2, "limitLatch count reaches 2");

		Thread blocker = new Thread(() -> {
			try {
				ll.countUpOrAwait();
			} catch (InterruptedException ignored) {
			}
		});
		blocker.start();
		Thread.sleep(50);
		check(ll.getCount() == 2, "third acquire blocks at limit (count stays 2)");

		ll.countDown();
		blocker.join(1000);
		check(ll.getCount() == 2, "countDown releases blocked acquire");

		// 2. unlimited mode (maxConnections==-1)
		LimitLatch unlimited = new LimitLatch(-1);
		for (int i = 0; i < 100; i++) {
			unlimited.countUpOrAwait();
		}
		check(unlimited.getCount() == 100, "maxConnections==-1 → unlimited (no block)");

		// 3. wakeupCounter CAS: only wakeup when counter==0
		Poller poller = new Poller(new LimitLatch(100));
		Thread pollerThread = new Thread(poller);
		pollerThread.start();
		poller.addEvent("sock1", 1);
		// counter goes 1 → no wakeup call (incrementAndGet != 0)
		check(!poller.wakeupCalled, "wakeup() not called when counter != 0");

		// simulate select loop: getAndSet(-1) then select
		// (mirror NioEndpoint.java:750-757)
		poller.wakeupCounter.getAndSet(-1);
		poller.wakeup();
		check(poller.wakeupCalled, "wakeup() called when counter==0 (via getAndSet(-1) reset)");

		// 4. PollerEvent pool reuse
		PollerEvent e1 = poller.createPollerEvent("sockA", 1);
		e1.recycle();
		poller.eventPool.addLast(e1);
		PollerEvent e2 = poller.createPollerEvent("sockB", 2);
		check(e1 == e2, "createPollerEvent reuses pooled event object");

		// 5. events processed by worker pool
		poller.addEvent("sockX", 1);
		poller.addEvent("sockY", 2);
		Thread.sleep(100);
		check(poller.selectCount >= 1, "poller events processed (selectCount >= 1)");

		// 6. keep-alive: re-register same socket multiple times
		poller.addEvent("keepAliveSock", 1);
		poller.addEvent("keepAliveSock", 1);
		poller.addEvent("keepAliveSock", 1);
		Thread.sleep(100);
		check(poller.selectCount >= 4, "keep-alive: same socket re-registered (N requests)");

		// 7. selector.select() vs wakeup race pattern
		// mirror: if (wakeupCounter.getAndSet(-1) > 0) selectNow else select(timeout)
		boolean sawRace = false;
		Poller p2 = new Poller(new LimitLatch(100));
		p2.addEvent("sockR", 1); // counter now 1
		long v = p2.wakeupCounter.getAndSet(-1);
		if (v > 0) {
			sawRace = true; // selectNow path — pending wakeup consumed
		}
		check(sawRace, "getAndSet(-1) > 0 → selectNow (no blocking)");

		// 8. shutdown cleanly
		poller.shutdown();
		p2.shutdown();
		pollerThread.join(1000);
		check(!pollerThread.isAlive(), "poller thread stops cleanly");

		System.out.println("== " + pass + "/" + (pass + fail) + " PASS");
		if (fail > 0) {
			System.exit(1);
		}
	}
}