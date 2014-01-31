package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodNameEqualityResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AnnotationDrivenBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.This;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

public class MethodDelegation implements ByteCodeAppender.Factory {

    public static ByteCodeAppender.Factory to(Class<?> type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Cannot delegate to interface " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot delegate to array " + type);
        } else if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot delegate to primitive " + type);
        }
        return new MethodDelegation(
                new AnnotationDrivenBinder(
                        Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(
                                Argument.Binder.INSTANCE,
                                This.Binder.INSTANCE,
                                AllArguments.Binder.INSTANCE),
                        Argument.NextUnboundAsDefaultProvider.INSTANCE,
                        new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false)),
                new MethodDelegationBinder.AmbiguityResolver.Chain(
                        Arrays.<MethodDelegationBinder.AmbiguityResolver>asList(
                                MethodNameEqualityResolver.INSTANCE,
                                MostSpecificTypeResolver.INSTANCE)),
                new TypeDescription.ForLoadedType(type).getReachableMethods().filter(isStatic().and(not(isPrivate()))));
    }

    private final MethodDelegationBinder.Processor processor;
    private final MethodList methods;

    protected MethodDelegation(MethodDelegationBinder methodDelegationBinder,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                               MethodList methods) {
        processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        this.methods = methods;
    }

    public MethodDelegation withAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver ambiguityResolver) {
        return new MethodDelegation(processor.getMethodDelegationBinder(), ambiguityResolver, methods);
    }

    public MethodDelegation withMethodDelegationBinder(MethodDelegationBinder methodDelegationBinder) {
        return new MethodDelegation(methodDelegationBinder, processor.getAmbiguityResolver(), methods);
    }

    public MethodDelegation withMethodsMatching(MethodMatcher methodMatcher) {
        return new MethodDelegation(processor.getMethodDelegationBinder(), processor.getAmbiguityResolver(), methods.filter(methodMatcher));
    }

    private class AppenderDelegate implements ByteCodeAppender {

        private final TypeDescription typeDescription;

        private AppenderDelegate(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            Assignment.Size size = processor.process(typeDescription, methodDescription, methods).apply(methodVisitor);
            size = size.aggregate(MethodReturn.returning(methodDescription.getReturnType()).apply(methodVisitor));
            return new Size(size.getMaximalSize(), methodDescription.getStackSize());
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return new AppenderDelegate(typeDescription);
    }
}