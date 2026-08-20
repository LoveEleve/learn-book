import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.oap.server.network.trace.component.command.*;
import java.util.Collections;

public class MiniSW1 {
    static int pass, fail;
    static void check(String name, boolean ok) { if (ok) { pass++; System.out.println("PASS " + name); } else { fail++; System.out.println("FAIL " + name); } }
    public static void main(String[] args) {
        ProfileTaskCommand profile = new ProfileTaskCommand("sn1", "task1", "/endpoint", 10, 2, 3, 4, 5L, 6L);
        BaseCommand profileBack = CommandDeserializer.deserialize(profile.serialize().build());
        check("Profile command round trip", profileBack instanceof ProfileTaskCommand && profileBack.getSerialNumber().equals("sn1"));
        ConfigurationDiscoveryCommand config = new ConfigurationDiscoveryCommand("sn2", "uuid", Collections.emptyList());
        BaseCommand configBack = CommandDeserializer.deserialize(config.serialize().build());
        check("Configuration command round trip", configBack instanceof ConfigurationDiscoveryCommand && configBack.getSerialNumber().equals("sn2"));
        AsyncProfilerTaskCommand async = new AsyncProfilerTaskCommand("sn3", "task3", 10, "cpu", 12L);
        BaseCommand asyncBack = CommandDeserializer.deserialize(async.serialize().build());
        check("Async profiler command round trip", asyncBack instanceof AsyncProfilerTaskCommand && asyncBack.getSerialNumber().equals("sn3"));
        PprofTaskCommand pprof = new PprofTaskCommand("sn4", "task4", "cpu", 10L, 11L, 12);
        BaseCommand pprofBack = CommandDeserializer.deserialize(pprof.serialize().build());
        check("Pprof command round trip", pprofBack instanceof PprofTaskCommand && pprofBack.getSerialNumber().equals("sn4"));
        check("EBPF command serializes", new EBPFProfilingTaskCommand("sn5", "task5", Collections.emptyList(), 1L, 2L, "", null, "", null).serialize().build().getCommand().equals(EBPFProfilingTaskCommand.NAME));
        check("Continuous policy command serializes", new ContinuousProfilingPolicyCommand("sn6", Collections.emptyList()).serialize().build().getCommand().equals(ContinuousProfilingPolicyCommand.NAME));
        check("Continuous report command serializes", new ContinuousProfilingReportCommand("sn7", "task7").serialize().build().getCommand().equals(ContinuousProfilingReportCommand.NAME));
        check("Trace ignore command serializes", new TraceIgnoreCommand("sn8").serialize().build().getCommand().equals("TraceIgnore"));
        boolean unsupported = false;
        try { CommandDeserializer.deserialize(Command.newBuilder().setCommand("unknown").build()); }
        catch (UnsupportedCommandException e) { unsupported = true; }
        check("Unknown command rejected", unsupported);
        System.out.println("RESULT " + pass + " PASS, " + fail + " FAIL");
        if (fail > 0) System.exit(1);
    }
}