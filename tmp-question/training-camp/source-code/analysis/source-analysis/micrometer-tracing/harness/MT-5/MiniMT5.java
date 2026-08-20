import io.micrometer.tracing.Span;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.exporter.SpanIgnoringSpanExportingPredicate;
import io.micrometer.tracing.exporter.TestSpanReporter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class MiniMT5 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) { if (cond) { pass++; System.out.println("  PASS " + name); } else { fail++; System.out.println("  FAIL " + name); } }
    public static void main(String[] args) throws Exception {
        System.out.println("== MiniMT5: Exporter / filters ==");
        MutableFinishedSpan s = new MutableFinishedSpan("span.name");
        s.start = Instant.ofEpochMilli(1000); s.end = Instant.ofEpochMilli(2500);
        check("duration derived from timestamps", s.getDuration().equals(Duration.ofMillis(1500)));
        Map<String,Object> typed = new HashMap<>(); typed.put("number", 1); typed.put("flag", true); typed.put("list", List.of("a", "b")); typed.put("null", null); s.setTypedTags(typed);
        check("typed tag number stringified", "1".equals(s.tags.get("number")));
        check("typed tag list joined", "a,b".equals(s.tags.get("list")));
        check("typed tag null stringified", "null".equals(s.tags.get("null")));
        check("typed tags default getter returns object map", s.getTypedTags().get("flag").equals("true"));
        check("full regex matches", !new SpanIgnoringSpanExportingPredicate(List.of("span\\.name"), List.of()).isExportable(s));
        check("partial regex does not match", new SpanIgnoringSpanExportingPredicate(List.of("span"), List.of()).isExportable(s));
        s.name = "";
        check("empty span name exportable", new SpanIgnoringSpanExportingPredicate(List.of(".*"), List.of()).isExportable(s));
        s.name = "other";
        check("additional skip list works", !new SpanIgnoringSpanExportingPredicate(List.of(), List.of("other")).isExportable(s));
        TestSpanReporter reporter = new TestSpanReporter();
        reporter.report(s); reporter.report(new MutableFinishedSpan("two"));
        check("reporter stores spans", reporter.spans().size() == 2);
        check("reporter poll removes first", reporter.poll() == s && reporter.spans().size() == 1);
        reporter.close();
        check("reporter close clears queue", reporter.spans().isEmpty());
        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
    static class MutableFinishedSpan implements FinishedSpan {
        String name; Instant start=Instant.EPOCH, end=Instant.EPOCH; Map<String,String> tags=new HashMap<>();
        MutableFinishedSpan(String name){this.name=name;}
        public FinishedSpan setName(String n){name=n;return this;} public String getName(){return name;}
        public Instant getStartTimestamp(){return start;} public Instant getEndTimestamp(){return end;}
        public FinishedSpan setTags(Map<String,String> t){tags=new HashMap<>(t);return this;} public Map<String,String> getTags(){return tags;}
        public FinishedSpan setEvents(Collection<Map.Entry<Long,String>> e){return this;} public Collection<Map.Entry<Long,String>> getEvents(){return List.of();}
        public String getSpanId(){return "s";} public String getParentId(){return null;} public String getRemoteIp(){return null;}
        public FinishedSpan setLocalIp(String ip){return this;} public int getRemotePort(){return 0;} public FinishedSpan setRemotePort(int p){return this;}
        public String getTraceId(){return "t";} public Throwable getError(){return null;} public FinishedSpan setError(Throwable e){return this;}
        public Span.Kind getKind(){return null;} public String getRemoteServiceName(){return null;} public FinishedSpan setRemoteServiceName(String s){return this;}
    }
}
