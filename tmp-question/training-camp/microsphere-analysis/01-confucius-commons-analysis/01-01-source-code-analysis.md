# 01-01：01-confucius-commons 源码文件清单

> **核心命题**：01-confucius-commons 模块的全部源码文件列表，共 60 个 Java 文件。

---

## 项目结构

### confucius-commons-lang（52 文件）

- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/ClassLoaderUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/ClassPathUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/ClassUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/constants/Constants.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/constants/FileSuffixConstants.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/constants/PathConstants.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/constants/ProtocolConstants.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/constants/SeparatorConstants.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/ClassFileJarEntryFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/ClassFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/Filter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/FilterOperator.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/FilterUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/JarEntryFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/PackageNameClassFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/PackageNameClassNameFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/filter/TrueClassFilter.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/io/FileUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/io/scanner/Scanner.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/io/scanner/SimpleClassScanner.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/io/scanner/SimpleFileScanner.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/io/scanner/SimpleJarEntryScanner.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/management/ManagementUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/misc/UnsafeUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/net/URLUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/process/ProcessExecutor.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/process/ProcessManager.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/reflect/ReflectionUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/util/PropertyResourceBundleControl.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/util/PropertyResourceBundleUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/util/ServiceLoaderUtils.java`
- `confucius-commons-lang/src/main/java/org/confucius/commons/lang/util/jar/JarUtils.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/AbstractTestCase.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/ClassLoaderUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/ClassPathUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/ClassUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/filter/FilterUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/io/scanner/SimpleClassScannerTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/io/scanner/SimpleFileScannerTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/io/scanner/SimpleJarEntryScannerTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/management/ManagementUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/misc/UnsafeUtilTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/net/URLUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/process/ProcessExecutorTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/reflect/ReflectionUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/util/PropertyResourceBundleControlTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/util/PropertyResourceBundleUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/util/ServiceLoaderUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/lang/util/jar/JarUtilsTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/performance/AbstractPerformanceTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/performance/ClassLoaderUtilsPerformanceTest.java`
- `confucius-commons-lang/src/test/java/org/confucius/commons/performance/PerformanceAction.java`

### confucius-commons-tools/confucius-commons-tools-attach（5 文件）

- `confucius-commons-tools/confucius-commons-tools-attach/src/main/java/org/confucius/commons/tools/attach/HotSpotVirtualMachineCallback.java`
- `confucius-commons-tools/confucius-commons-tools-attach/src/main/java/org/confucius/commons/tools/attach/LocalVirtualMachineTemplate.java`
- `confucius-commons-tools/confucius-commons-tools-attach/src/main/java/org/confucius/commons/tools/attach/VirtualMachineCallback.java`
- `confucius-commons-tools/confucius-commons-tools-attach/src/main/java/org/confucius/commons/tools/attach/VirtualMachineTemplate.java`
- `confucius-commons-tools/confucius-commons-tools-attach/src/test/java/org/confucius/commons/tools/attach/LocalVirtualMachineTemplateTest.java`

### confucius-commons-util（3 文件）

- `confucius-commons-util/src/main/java/org/confucius/commons/util/os/windows/Base64.java`
- `confucius-commons-util/src/main/java/org/confucius/commons/util/os/windows/WindowsRegistry.java`
- `confucius-commons-util/src/test/java/org/confucius/commons/util/os/windows/WindowsRegistryTest.java`

---

**总计**：60 个 Java 文件
