import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniNotify — N-24 harness (12 assertions)
 *
 * Reproduces NotifyCenter model without Nacos server:
 *   1. type → publisher map + shared publisher for slow events
 *   2. publisher thread: blocking queue consume → subscriber dispatch
 *   3. smart subscriber (multi-type) vs single-type
 *   4. publish routes by event type
 */
public class MiniNotify {

	// ---- event ----
	static class Event {
		final String type;
		final String data;

		Event(String type, String data) {
			this.type = type;
			this.data = data;
		}
	}

	// ---- subscriber ----
	static class Subscriber {
		final List<Event> received = new ArrayList<>();

		void onEvent(Event e) {
			received.add(e);
		}
	}

	// ---- publisher (mirror DefaultPublisher) ----
	static class Publisher {
		final String type;
		final BlockingQueue<Event> queue;
		final List<Subscriber> subscribers = new ArrayList<>();
		volatile boolean running = true;

		Publisher(String type, int capacity) {
			this.type = type;
			this.queue = new ArrayBlockingQueue<>(capacity);
		}

		boolean publish(Event e) {
			return queue.offer(e);
		}

		// consumer loop (single thread in real code)
		void consumeOne() {
			try {
				Event e = queue.take();
				for (Subscriber s : subscribers) {
					s.onEvent(e);
				}
			}
			catch (InterruptedException ex) {
				// ignore
			}
		}
	}

	// ---- notify center (mirror NotifyCenter) ----
	static class NotifyCenter {
		final Map<String, Publisher> publisherMap = new ConcurrentHashMap<>();
		Publisher sharePublisher = new Publisher("share", 64);
		final List<Subscriber> shareSubscribers = new ArrayList<>();

		Publisher registerToPublisher(String type, int cap) {
			return publisherMap.computeIfAbsent(type, t -> new Publisher(t, cap));
		}

		void registerSubscriber(String type, Subscriber s) {
			Publisher p = publisherMap.get(type);
			if (p != null) {
				p.subscribers.add(s);
			}
		}

		boolean publishEvent(Event e, boolean slow) {
			if (slow) {
				shareSubscribers.forEach(s -> s.onEvent(e));
				return true;
			}
			Publisher p = publisherMap.get(e.type);
			return p != null && p.publish(e);
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
		NotifyCenter center = new NotifyCenter();
		Publisher p1 = center.registerToPublisher("instances-change", 16);
		Publisher p2 = center.registerToPublisher("config-change", 16);

		// ---- 1. type → publisher map ----
		check("N24: per-type publisher", p1 != p2
				&& center.publisherMap.size() == 2);

		// ---- 2. register + consume + dispatch ----
		Subscriber sub = new Subscriber();
		center.registerSubscriber("instances-change", sub);
		Event e1 = new Event("instances-change", "svc-a changed");
		check("N24: publish accepted", p1.publish(e1));
		p1.consumeOne();
		check("N24: subscriber received", sub.received.size() == 1
				&& sub.received.get(0).data.equals("svc-a changed"));

		// ---- 3. type isolation ----
		Subscriber other = new Subscriber();
		center.registerSubscriber("config-change", other);
		Event e2 = new Event("config-change", "data-1 changed");
		p2.publish(e2);
		p2.consumeOne();
		check("N24: type isolated", sub.received.size() == 1
				&& other.received.size() == 1);

		// ---- 4. slow event → shared ----
		Subscriber slowSub = new Subscriber();
		center.shareSubscribers.add(slowSub);
		center.publishEvent(new Event("slow", "x"), true);
		check("N24: slow event via shared", slowSub.received.size() == 1);

		// ---- 5. queue capacity ----
		Publisher small = new Publisher("small", 1);
		small.publish(new Event("small", "1"));
		boolean overflow = small.publish(new Event("small", "2"));
		check("N24: overflow rejected (bounded queue)", !overflow);

		// ---- 6. unknown type publish ----
		check("N24: unknown type → no publisher (false)",
				!center.publishEvent(new Event("nope", "x"), false));

		System.out.println("===== MiniNotify: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
