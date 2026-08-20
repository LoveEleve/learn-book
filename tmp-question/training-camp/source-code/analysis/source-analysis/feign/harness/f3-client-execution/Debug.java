public class Debug {
    public static void main(String[] args) throws Throwable {
        java.util.List<MiniExecute.RequestInterceptor> ris = new java.util.ArrayList<>();
        ris.add(inv -> inv.requestLine = inv.requestLine.replace("/", "/auth"));
        java.util.List<MiniExecute.MethodInterceptor> mis = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        MiniExecute.Client c = req -> { attempts.incrementAndGet(); if (attempts.get() <= 2) throw new MiniExecute.RetryableException("f", null); return "OK"; };
        MiniExecute.Executor e = new MiniExecute.Executor(new MiniExecute.DefaultRetryer(), ris, mis, c, false);
        MiniExecute.Invocation inv = new MiniExecute.Invocation();
        e.invoke(inv);
        System.out.println("requestLine after = [" + inv.requestLine + "]");
        System.out.println("log = " + inv.log);
    }
}
