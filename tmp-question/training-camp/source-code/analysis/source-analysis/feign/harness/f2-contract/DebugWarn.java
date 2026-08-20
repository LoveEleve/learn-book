public class DebugWarn {
    public static void main(String[] args) {
        MiniContract.DefaultContract contract = new MiniContract.DefaultContract();
        for (MiniContract.MethodMetadata md : contract.parseAndValidateMetadata(MiniContractTest.UnknownAnnApi.class)) {
            System.out.println("configKey=" + md.configKey());
            System.out.println("warnings=" + md.warnings());
        }
    }
}
