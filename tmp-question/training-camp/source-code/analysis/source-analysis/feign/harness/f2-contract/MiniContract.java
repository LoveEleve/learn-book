import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniContract — 微缩版 Feign Contract 解析链 (DefaultContract → DeclarativeContract → BaseContract)
 *
 * 覆盖机制:
 * 1. BaseContract.parseAndValidateMetadata 双遍历 (接口+类) + 四类参数判定 (body/url/options/form)
 * 2. DeclarativeContract 三注册表 (类级/方法级/参数级) + GuardedAnnotationProcessor 双职责
 * 3. DefaultContract 7 处理器: 类级 Headers / 方法级 RequestLine·Body·Headers / 参数级 Param·QueryMap·HeaderMap
 * 4. MethodMetadata 16 字段面 (configKey/indexToName/bodyIndex/urlIndex/queryMapIndex/formParams...)
 * 5. 未消费注解告警 (addWarning) + configKey 冲突 (override 返回类型解析)
 */
public class MiniContract {

    // ===== 注解面 (仿 feign.RequestLine/Param/QueryMap/HeaderMap/Headers) =====

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RequestLine {
        String value();

        boolean decodeSlash() default true;

        CollectionFormat collectionFormat() default CollectionFormat.EXPLODED;
    }

    public enum CollectionFormat {
        EXPLODED(null);
        final String separator;
        CollectionFormat(String separator) {
            this.separator = separator;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Param {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface QueryMap {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface HeaderMap {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Headers {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Body {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface FeignIgnore {
    }

    // ===== MethodMetadata 字段面 (仿 feign.MethodMetadata 16 字段子集) =====

    public static class MethodMetadata {
        private String configKey;
        private Type returnType;
        private Integer urlIndex;
        private Integer bodyIndex;
        private Integer headerMapIndex;
        private Integer queryMapIndex;
        private boolean alwaysEncodeBody;
        private Type bodyType;
        private final StringBuffer template = new StringBuffer();
        private final List<String> formParams = new ArrayList<>();
        private final Map<Integer, Collection<String>> indexToName = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private transient Class<?> targetType;
        private transient Method method;

        public String configKey() { return configKey; }
        public MethodMetadata configKey(String v) { this.configKey = v; return this; }
        public Type returnType() { return returnType; }
        public MethodMetadata returnType(Type v) { this.returnType = v; return this; }
        public Integer urlIndex() { return urlIndex; }
        public MethodMetadata urlIndex(Integer v) { this.urlIndex = v; return this; }
        public Integer bodyIndex() { return bodyIndex; }
        public MethodMetadata bodyIndex(Integer v) { this.bodyIndex = v; return this; }
        public Integer headerMapIndex() { return headerMapIndex; }
        public MethodMetadata headerMapIndex(Integer v) { this.headerMapIndex = v; return this; }
        public Integer queryMapIndex() { return queryMapIndex; }
        public MethodMetadata queryMapIndex(Integer v) { this.queryMapIndex = v; return this; }
        public boolean alwaysEncodeBody() { return alwaysEncodeBody; }
        public MethodMetadata alwaysEncodeBody(boolean v) { this.alwaysEncodeBody = v; return this; }
        public boolean isIgnored() { return ignored; }
        private boolean ignored;
        public Type bodyType() { return bodyType; }
        public MethodMetadata bodyType(Type v) { this.bodyType = v; return this; }
        public StringBuffer template() { return template; }
        public List<String> formParams() { return formParams; }
        public Map<Integer, Collection<String>> indexToName() { return indexToName; }
        public List<String> warnings() { return warnings; }
        public MethodMetadata addWarning(String w) { warnings.add(w); return this; }
        public Class<?> targetType() { return targetType; }
        public MethodMetadata targetType(Class<?> v) { this.targetType = v; return this; }
        public Method method() { return method; }
        public MethodMetadata method(Method v) { this.method = v; return this; }
    }

    // ===== BaseContract: 主循环 (仿 feign.Contract.BaseContract) =====

    public static abstract class BaseContract {
        public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
            Map<String, MethodMetadata> result = new LinkedHashMap<>();
            for (Method method : targetType.getMethods()) {
                if (method.getDeclaringClass() == Object.class
                        || (method.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0
                        || method.isDefault()
                        || method.isAnnotationPresent(FeignIgnore.class)) {
                    continue;
                }
                MethodMetadata metadata = parseAndValidateMetadata(targetType, method);
                if (result.containsKey(metadata.configKey())) {
                    // override: 返回类型解析后保留子类版本
                    result.put(metadata.configKey(), metadata);
                    continue;
                }
                result.put(metadata.configKey(), metadata);
            }
            return new ArrayList<>(result.values());
        }

        protected MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
            MethodMetadata data = new MethodMetadata();
            data.targetType(targetType);
            data.method(method);
            data.returnType(method.getGenericReturnType());
            data.configKey(targetType.getSimpleName() + "#" + method.getName());
            if (targetType.getInterfaces().length == 1) {
                processAnnotationOnClass(data, targetType.getInterfaces()[0]);
            }
            processAnnotationOnClass(data, targetType);
            for (Annotation methodAnnotation : method.getAnnotations()) {
                processAnnotationOnMethod(data, methodAnnotation, method);
            }
            if (data.isIgnored()) {
                return data;
            }
            if (data.template().length() == 0) {
                throw new IllegalStateException(
                        "Method " + data.configKey() + " not annotated with HTTP method type (ex. GET, POST)"
                                + data.warnings());
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            int count = parameterAnnotations.length;
            for (int i = 0; i < count; i++) {
                boolean isHttpAnnotation = false;
                if (parameterAnnotations[i] != null) {
                    isHttpAnnotation = processAnnotationsOnParameter(data, parameterAnnotations[i], i);
                }
                if (parameterTypes[i] == java.net.URI.class) {
                    data.urlIndex(i);
                } else if (!isHttpAnnotation
                        && !java.net.URI.class.isAssignableFrom(parameterTypes[i])) {
                    if (data.formParams().isEmpty() || data.bodyIndex() == null) {
                        if (data.bodyIndex() == null) {
                            data.bodyIndex(i);
                            data.bodyType(genericParameterTypes[i]);
                        }
                    }
                }
            }
            return data;
        }

        protected abstract void processAnnotationOnClass(MethodMetadata data, Class<?> clz);
        protected abstract void processAnnotationOnMethod(MethodMetadata data, Annotation annotation, Method method);
        protected abstract boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex);

        protected void nameParam(MethodMetadata data, String name, int i) {
            Collection<String> names = data.indexToName().containsKey(i)
                    ? data.indexToName().get(i) : new ArrayList<>();
            names.add(name);
            data.indexToName().put(i, names);
        }
    }

    // ===== DeclarativeContract: 三注册表 (仿 feign.DeclarativeContract) =====

    public static abstract class DeclarativeContract extends BaseContract {
        private final List<GuardedAnnotationProcessor> classAnnotationProcessors = new ArrayList<>();
        private final List<GuardedAnnotationProcessor> methodAnnotationProcessors = new ArrayList<>();
        private final Map<Class<Annotation>, ParameterAnnotationProcessor<Annotation>>
                parameterAnnotationProcessors = new HashMap<>();

        @Override
        protected final void processAnnotationOnClass(MethodMetadata data, Class<?> targetType) {
            List<GuardedAnnotationProcessor> processors = new ArrayList<>();
            for (Annotation annotation : targetType.getAnnotations()) {
                for (GuardedAnnotationProcessor p : classAnnotationProcessors) {
                    if (p.test(annotation)) processors.add(p);
                }
            }
            if (!processors.isEmpty()) {
                for (Annotation annotation : targetType.getAnnotations()) {
                    for (GuardedAnnotationProcessor p : processors) {
                        if (p.test(annotation)) p.process(annotation, data);
                    }
                }
            } else {
                if (targetType.getAnnotations().length == 0) {
                    data.addWarning("Class " + targetType.getSimpleName() + " has no annotations");
                } else {
                    data.addWarning("Class " + targetType.getSimpleName() + " has annotations that are not used by contract");
                }
            }
        }

        @Override
        protected final void processAnnotationOnMethod(MethodMetadata data, Annotation annotation, Method method) {
            List<GuardedAnnotationProcessor> processors = new ArrayList<>();
            for (GuardedAnnotationProcessor p : methodAnnotationProcessors) {
                if (p.test(annotation)) processors.add(p);
            }
            if (!processors.isEmpty()) {
                for (GuardedAnnotationProcessor p : processors) {
                    p.process(annotation, data);
                }
            } else {
                data.addWarning("Method " + method.getName() + " has an annotation "
                        + annotation.annotationType().getSimpleName() + " that is not used by contract");
            }
        }

        @Override
        protected final boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
            List<Annotation> matching = new ArrayList<>();
            for (Annotation a : annotations) {
                if (parameterAnnotationProcessors.containsKey(a.annotationType())) {
                    matching.add(a);
                }
            }
            if (!matching.isEmpty()) {
                for (Annotation a : matching) {
                    parameterAnnotationProcessors.getOrDefault(a.annotationType(),
                            (ann, d, i) -> {}).process(a, data, paramIndex);
                }
            } else {
                String parameterName = data.method().getParameters()[paramIndex].isNamePresent()
                        ? data.method().getParameters()[paramIndex].getName()
                        : data.method().getParameters()[paramIndex].getType().getSimpleName();
                if (annotations.length == 0) {
                    data.addWarning("Parameter " + parameterName + " has no annotations");
                } else {
                    data.addWarning("Parameter " + parameterName + " has annotations not used by contract");
                }
            }
            return false;
        }

        protected <E extends Annotation> void registerClassAnnotation(
                Class<E> annotationType, AnnotationProcessor<E> processor) {
            this.classAnnotationProcessors.add(
                    new GuardedAnnotationProcessor(
                            (java.util.function.Predicate<Annotation>) a -> a.annotationType().equals(annotationType),
                            processor));
        }

        protected <E extends Annotation> void registerMethodAnnotation(
                Class<E> annotationType, AnnotationProcessor<E> processor) {
            this.methodAnnotationProcessors.add(
                    new GuardedAnnotationProcessor(
                            (java.util.function.Predicate<Annotation>) a -> a.annotationType().equals(annotationType),
                            processor));
        }

        protected <E extends Annotation> void registerParameterAnnotation(
                Class<E> annotation, ParameterAnnotationProcessor<E> processor) {
            this.parameterAnnotationProcessors.put((Class) annotation, (ParameterAnnotationProcessor) processor);
        }

        @FunctionalInterface
        public interface AnnotationProcessor<E extends Annotation> {
            void process(E annotation, MethodMetadata metadata);
        }

        @FunctionalInterface
        public interface ParameterAnnotationProcessor<E extends Annotation> {
            ParameterAnnotationProcessor<Annotation> DO_NOTHING = (ann, data, i) -> {};
            void process(E annotation, MethodMetadata metadata, int paramIndex);
        }

        private class GuardedAnnotationProcessor
                implements java.util.function.Predicate<Annotation>, AnnotationProcessor<Annotation> {
            private final java.util.function.Predicate<Annotation> predicate;
            private final AnnotationProcessor<Annotation> processor;
            @SuppressWarnings({"rawtypes", "unchecked"})
            GuardedAnnotationProcessor(java.util.function.Predicate predicate, AnnotationProcessor processor) {
                this.predicate = predicate;
                this.processor = processor;
            }
            @Override
            public void process(Annotation annotation, MethodMetadata metadata) {
                processor.process(annotation, metadata);
            }
            @Override
            public boolean test(Annotation t) {
                return predicate.test(t);
            }
        }
    }

    // ===== DefaultContract: 7 处理器 (仿 feign.DefaultContract) =====

    public static class DefaultContract extends DeclarativeContract {
        public DefaultContract() {
            super.registerClassAnnotation(Headers.class, (header, data) -> {
                String[] headersOnType = header.value();
                for (String h : headersOnType) {
                    data.template().append("H:" + h + ";");
                }
            });
            super.registerMethodAnnotation(RequestLine.class, (ann, data) -> {
                String requestLine = ann.value();
                if (requestLine == null || requestLine.isEmpty()) {
                    throw new IllegalStateException("RequestLine annotation was empty");
                }
                String[] parts = requestLine.trim().split("\\s+", 2);
                data.template().append(parts[0]).append(" ").append(parts[1]).append(" ");
            });
            super.registerMethodAnnotation(Body.class, (ann, data) -> {
                String body = ann.value();
                if (body.indexOf('{') == -1) {
                    data.template().append("BODY[" + body + "]");
                } else {
                    data.template().append("BODYTMPL[" + body + "]");
                }
            });
            super.registerMethodAnnotation(Headers.class, (header, data) -> {
                for (String h : header.value()) {
                    data.template().append("H:" + h + ";");
                }
            });
            super.registerParameterAnnotation(Param.class, (paramAnnotation, data, paramIndex) -> {
                String name = paramAnnotation.value();
                if (name.isEmpty()) {
                    Parameter p = data.method().getParameters()[paramIndex];
                    name = p.isNamePresent() ? p.getName() : p.getType().getSimpleName();
                }
                nameParam(data, name, paramIndex);
                if (!data.template().toString().contains("{" + name + "}")) {
                    data.formParams().add(name);
                }
            });
            super.registerParameterAnnotation(QueryMap.class, (queryMap, data, paramIndex) -> {
                if (data.queryMapIndex() != null) {
                    throw new IllegalStateException("QueryMap annotation was present on multiple parameters.");
                }
                data.queryMapIndex(paramIndex);
            });
            super.registerParameterAnnotation(HeaderMap.class, (headerMap, data, paramIndex) -> {
                if (data.headerMapIndex() != null) {
                    throw new IllegalStateException("HeaderMap annotation was present on multiple parameters.");
                }
                data.headerMapIndex(paramIndex);
            });
        }
    }
}
