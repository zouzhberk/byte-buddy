package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A utility class that contains a human-readable language for creating {@link net.bytebuddy.matcher.ElementMatcher}s.
 */
public final class ElementMatchers {

    /**
     * A readable reference to the bootstrap class loader which is represented by {@code null}.
     */
    private static final ClassLoader BOOTSTRAP_CLASSLOADER = null;

    /**
     * A private constructor that must not be invoked.
     */
    private ElementMatchers() {
        throw new UnsupportedOperationException();
    }

    /**
     * Wraps another matcher to assure that an element is not matched in case that the matching causes an {@link Exception}.
     *
     * @param matcher The element matcher that potentially throws an exception.
     * @param <T>     The type of the matched object.
     * @return A matcher that returns {@code false} in case that the given matcher throws an exception.
     */
    public static <T> ElementMatcher.Junction<T> failSafe(ElementMatcher<? super T> matcher) {
        return new FailSafeMatcher<T>(matcher, false);
    }

    /**
     * Matches the given value which can also be {@code null} by the {@link java.lang.Object#equals(Object)} method or
     * by a null-check.
     *
     * @param value The value that is to be matched.
     * @param <T>   The type of the matched object.
     * @return A matcher that matches an exact value.
     */
    public static <T> ElementMatcher.Junction<T> is(Object value) {
        return value == null
                ? new NullMatcher<T>()
                : new EqualityMatcher<T>(value);
    }

    /**
     * Exactly matches a given field as a {@link FieldDescription}.
     *
     * @param field The field to match by its description
     * @param <T>   The type of the matched object.
     * @return An element matcher that exactly matches the given field.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> is(Field field) {
        return definedField(is(new FieldDescription.ForLoadedField(field)));
    }

    /**
     * Matches a field in its defined shape.
     *
     * @param matcher The matcher to apply to the matched field's defined shape.
     * @param <T>     The matched object's type.
     * @return A matcher that matches a matched field's defined shape.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> definedField(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        return new DefinedShapeMatcher<T, FieldDescription.InDefinedShape>(matcher);
    }

    /**
     * Exactly matches a given method as a {@link MethodDescription}.
     *
     * @param method The method to match by its description
     * @param <T>    The type of the matched object.
     * @return An element matcher that exactly matches the given method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> is(Method method) {
        return definedMethod(is(new MethodDescription.ForLoadedMethod(method)));
    }

    /**
     * Exactly matches a given constructor as a {@link MethodDescription}.
     *
     * @param constructor The constructor to match by its description
     * @param <T>         The type of the matched object.
     * @return An element matcher that exactly matches the given constructor.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> is(Constructor<?> constructor) {
        return definedMethod(is(new MethodDescription.ForLoadedConstructor(constructor)));
    }

    /**
     * Matches a method in its defined shape.
     *
     * @param matcher The matcher to apply to the matched method's defined shape.
     * @param <T>     The matched object's type.
     * @return A matcher that matches a matched method's defined shape.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> definedMethod(ElementMatcher<? super MethodDescription.InDefinedShape> matcher) {
        return new DefinedShapeMatcher<T, MethodDescription.InDefinedShape>(matcher);
    }

    /**
     * Matches a parameter in its defined shape.
     *
     * @param matcher The matcher to apply to the matched parameter's defined shape.
     * @param <T>     The matched object's type.
     * @return A matcher that matches a matched parameter's defined shape.
     */
    public static <T extends ParameterDescription> ElementMatcher.Junction<T> definedParameter(ElementMatcher<? super ParameterDescription.InDefinedShape> matcher) {
        return new DefinedShapeMatcher<T, ParameterDescription.InDefinedShape>(matcher);
    }

    /**
     * Matches a parameter's type by the given matcher.
     *
     * @param matcher The matcher to apply to the parameter's type.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches a parameter's type by the given matcher.
     */
    public static <T extends ParameterDescription> ElementMatcher.Junction<T> hasType(ElementMatcher<? super TypeDescription> matcher) {
        return hasGenericType(rawType(matcher));
    }

    /**
     * Matches a method parameter by its generic type.
     *
     * @param matcher The matcher to apply to a parameter's generic type.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches the matched parameter's generic type.
     */
    public static <T extends ParameterDescription> ElementMatcher.Junction<T> hasGenericType(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return new MethodParameterTypeMatcher<T>(matcher);
    }

    /**
     * Exactly matches a given type as a {@link TypeDescription}.
     *
     * @param type The type to match by its description
     * @param <T>  The type of the matched object.
     * @return An element matcher that exactly matches the given type.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> is(Type type) {
        return is(TypeDefinition.Sort.describe(type));
    }

    /**
     * Exactly matches a given annotation as an {@link AnnotationDescription}.
     *
     * @param annotation The annotation to match by its description.
     * @param <T>        The type of the matched object.
     * @return An element matcher that exactly matches the given annotation.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> is(Annotation annotation) {
        return is(AnnotationDescription.ForLoadedAnnotation.of(annotation));
    }

    /**
     * Inverts another matcher.
     *
     * @param matcher The matcher to invert.
     * @param <T>     The type of the matched object.
     * @return An inverted version of the given {@code matcher}.
     */
    public static <T> ElementMatcher.Junction<T> not(ElementMatcher<? super T> matcher) {
        return new NegatingMatcher<T>(matcher);
    }

    /**
     * Creates a matcher that always returns {@code true}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that matches anything.
     */
    public static <T> ElementMatcher.Junction<T> any() {
        return new BooleanMatcher<T>(true);
    }

    /**
     * Creates a matcher that always returns {@code false}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that matches nothing.
     */
    public static <T> ElementMatcher.Junction<T> none() {
        return new BooleanMatcher<T>(false);
    }

    /**
     * Creates a matcher that matches any of the given objects by the {@link java.lang.Object#equals(Object)} method.
     * None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T> ElementMatcher.Junction<T> anyOf(Object... value) {
        return anyOf(Arrays.asList(value));
    }

    /**
     * Creates a matcher that matches any of the given objects by the {@link java.lang.Object#equals(Object)} method.
     * None of the values must be {@code null}.
     *
     * @param values The input values to be compared against.
     * @param <T>    The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T> ElementMatcher.Junction<T> anyOf(Iterable<?> values) {
        ElementMatcher.Junction<T> matcher = none();
        for (Object value : values) {
            matcher = matcher.or(is(value));
        }
        return matcher;
    }

    /**
     * Creates a matcher that matches any of the given types as {@link TypeDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> anyOf(Type... value) {
        return anyOf(new TypeList.Generic.ForLoadedTypes(value));
    }

    /**
     * Creates a matcher that matches any of the given constructors as {@link MethodDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> anyOf(Constructor<?>... value) {
        return definedMethod(anyOf(new MethodList.ForLoadedType(value, new Method[0])));
    }

    /**
     * Creates a matcher that matches any of the given methods as {@link MethodDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> anyOf(Method... value) {
        return definedMethod(anyOf(new MethodList.ForLoadedType(new Constructor<?>[0], value)));
    }

    /**
     * Creates a matcher that matches any of the given fields as {@link FieldDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> anyOf(Field... value) {
        return definedField(anyOf(new FieldList.ForLoadedFields(value)));
    }

    /**
     * Creates a matcher that matches any of the given annotations as {@link AnnotationDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> anyOf(Annotation... value) {
        return anyOf(new AnnotationList.ForLoadedAnnotations(value));
    }

    /**
     * Creates a matcher that matches none of the given objects by the {@link java.lang.Object#equals(Object)} method.
     * None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T> ElementMatcher.Junction<T> noneOf(Object... value) {
        return noneOf(Arrays.asList(value));
    }

    /**
     * Creates a matcher that matches none of the given objects by the {@link java.lang.Object#equals(Object)} method.
     * None of the values must be {@code null}.
     *
     * @param values The input values to be compared against.
     * @param <T>    The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T> ElementMatcher.Junction<T> noneOf(Iterable<?> values) {
        ElementMatcher.Junction<T> matcher = any();
        for (Object value : values) {
            matcher = matcher.and(not(is(value)));
        }
        return matcher;
    }

    /**
     * Creates a matcher that matches none of the given types as {@link TypeDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> noneOf(Type... value) {
        return noneOf(new TypeList.Generic.ForLoadedTypes(value));
    }

    /**
     * Creates a matcher that matches none of the given constructors as {@link MethodDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> noneOf(Constructor<?>... value) {
        return definedMethod(noneOf(new MethodList.ForLoadedType(value, new Method[0])));
    }

    /**
     * Creates a matcher that matches none of the given methods as {@link MethodDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> noneOf(Method... value) {
        return definedMethod(noneOf(new MethodList.ForLoadedType(new Constructor<?>[0], value)));
    }

    /**
     * Creates a matcher that matches none of the given methods as {@link FieldDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with none of the given objects.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> noneOf(Field... value) {
        return definedField(noneOf(new FieldList.ForLoadedFields(value)));
    }

    /**
     * Creates a matcher that matches none of the given annotations as {@link AnnotationDescription}s
     * by the {@link java.lang.Object#equals(Object)} method. None of the values must be {@code null}.
     *
     * @param value The input values to be compared against.
     * @param <T>   The type of the matched object.
     * @return A matcher that checks for the equality with any of the given objects.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> noneOf(Annotation... value) {
        return noneOf(new AnnotationList.ForLoadedAnnotations(value));
    }

    /**
     * Matches an iterable by assuring that at least one element of the iterable collection matches the
     * provided matcher.
     *
     * @param matcher The matcher to apply to each element.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches an iterable if at least one element matches the provided matcher.
     */
    public static <T> ElementMatcher.Junction<Iterable<? extends T>> whereAny(ElementMatcher<? super T> matcher) {
        return new CollectionItemMatcher<T>(matcher);
    }

    /**
     * Matches an iterable by assuring that no element of the iterable collection matches the provided matcher.
     *
     * @param matcher The matcher to apply to each element.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches an iterable if no element matches the provided matcher.
     */
    public static <T> ElementMatcher.Junction<Iterable<? extends T>> whereNone(ElementMatcher<? super T> matcher) {
        return not(whereAny(matcher));
    }

    /**
     * Matches a generic type's raw type against the provided raw type.
     *
     * @param type The type to match a generic type's erasure against.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches a generic type's raw type against the provided non-generic type.
     */
    public static <T extends TypeDescription.Generic> ElementMatcher.Junction<T> rawType(Class<?> type) {
        return rawType(is(type));
    }

    /**
     * Matches a generic type's raw type against the provided raw type.
     *
     * @param type The type to match a generic type's erasure against.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches a generic type's raw type against the provided non-generic type.
     */
    public static <T extends TypeDescription.Generic> ElementMatcher.Junction<T> rawType(TypeDescription type) {
        return rawType(is(type));
    }

    /**
     * Converts a matcher for a type description into a matcher for a raw type of the matched generic type against the given matcher. A wildcard
     * type which does not define a raw type results in a negative match.
     *
     * @param matcher The matcher to match the matched object's raw type against.
     * @param <T>     The type of the matched object.
     * @return A type matcher for a generic type that matches the matched type's raw type against the given type description matcher.
     */
    public static <T extends TypeDescription.Generic> ElementMatcher.Junction<T> rawType(ElementMatcher<? super TypeDescription> matcher) {
        return new RawTypeMatcher<T>(matcher);
    }

    /**
     * Matches an iteration of generic types' erasures against the provided raw types.
     *
     * @param type The types to match.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches an iteration of generic types' raw types against the provided non-generic types.
     */
    public static <T extends Iterable<? extends TypeDescription.Generic>> ElementMatcher.Junction<T> rawTypes(Class<?>... type) {
        return rawTypes(new TypeList.ForLoadedTypes(type));
    }

    /**
     * Matches an iteration of generic types' erasures against the provided raw types.
     *
     * @param type The types to match.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches an iteration of generic types' raw types against the provided non-generic types.
     */
    public static <T extends Iterable<? extends TypeDescription.Generic>> ElementMatcher.Junction<T> rawTypes(TypeDescription... type) {
        return rawTypes(Arrays.asList(type));
    }

    /**
     * Matches an iteration of generic types' erasures against the provided raw types.
     *
     * @param types The types to match.
     * @param <T>   The type of the matched object.
     * @return A matcher that matches an iteration of generic types' raw types against the provided non-generic types.
     */
    public static <T extends Iterable<? extends TypeDescription.Generic>> ElementMatcher.Junction<T> rawTypes(
            Iterable<? extends TypeDescription> types) {
        List<ElementMatcher<? super TypeDescription>> typeMatchers = new ArrayList<ElementMatcher<? super TypeDescription>>();
        for (TypeDescription type : types) {
            typeMatchers.add(is(type));
        }
        return rawTypes(new CollectionOneToOneMatcher<TypeDescription>(typeMatchers));
    }

    /**
     * Applies the provided matcher to an iteration og generic types' generic types.
     *
     * @param matcher The matcher to apply at the erased types.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches an iteration of generic types' raw types against the provided matcher.
     */
    public static <T extends Iterable<? extends TypeDescription.Generic>> ElementMatcher.Junction<T> rawTypes(
            ElementMatcher<? super Iterable<? extends TypeDescription>> matcher) {
        return new CollectionRawTypeMatcher<T>(matcher);
    }

    /**
     * Matches a type variable with the given name.
     *
     * @param symbol The name of the type variable to be match.
     * @param <T>    The type of the matched object.
     * @return A matcher that matches type variables with the given name.
     */
    public static <T extends TypeDefinition> ElementMatcher<T> isVariable(String symbol) {
        return isVariable(named(symbol));
    }

    /**
     * Matches a type variable with the given name.
     *
     * @param matcher A matcher for the type variable's name.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches type variables with the given name.
     */
    public static <T extends TypeDefinition> ElementMatcher<T> isVariable(ElementMatcher<? super NamedElement> matcher) {
        return new TypeSortMatcher<T>(anyOf(TypeDefinition.Sort.VARIABLE, TypeDefinition.Sort.VARIABLE_SYMBOLIC)).and(matcher);
    }

    /**
     * Matches a {@link NamedElement} for its exact name.
     *
     * @param name The expected name.
     * @param <T>  The type of the matched object.
     * @return An element matcher for a named element's exact name.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> named(String name) {
        return new NameMatcher<T>(new StringMatcher(name, StringMatcher.Mode.EQUALS_FULLY));
    }

    /**
     * Matches a {@link NamedElement} for its name. The name's
     * capitalization is ignored.
     *
     * @param name The expected name.
     * @param <T>  The type of the matched object.
     * @return An element matcher for a named element's name.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> namedIgnoreCase(String name) {
        return new NameMatcher<T>(new StringMatcher(name, StringMatcher.Mode.EQUALS_FULLY_IGNORE_CASE));
    }

    /**
     * Matches a {@link NamedElement} for its name's prefix.
     *
     * @param prefix The expected name's prefix.
     * @param <T>    The type of the matched object.
     * @return An element matcher for a named element's name's prefix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameStartsWith(String prefix) {
        return new NameMatcher<T>(new StringMatcher(prefix, StringMatcher.Mode.STARTS_WITH));
    }

    /**
     * Matches a {@link NamedElement} for its name's prefix. The name's
     * capitalization is ignored.
     *
     * @param prefix The expected name's prefix.
     * @param <T>    The type of the matched object.
     * @return An element matcher for a named element's name's prefix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameStartsWithIgnoreCase(String prefix) {
        return new NameMatcher<T>(new StringMatcher(prefix, StringMatcher.Mode.STARTS_WITH_IGNORE_CASE));
    }

    /**
     * Matches a {@link NamedElement} for its name's suffix.
     *
     * @param suffix The expected name's suffix.
     * @param <T>    The type of the matched object.
     * @return An element matcher for a named element's name's suffix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameEndsWith(String suffix) {
        return new NameMatcher<T>(new StringMatcher(suffix, StringMatcher.Mode.ENDS_WITH));
    }

    /**
     * Matches a {@link NamedElement} for its name's suffix. The name's
     * capitalization is ignored.
     *
     * @param suffix The expected name's suffix.
     * @param <T>    The type of the matched object.
     * @return An element matcher for a named element's name's suffix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameEndsWithIgnoreCase(String suffix) {
        return new NameMatcher<T>(new StringMatcher(suffix, StringMatcher.Mode.ENDS_WITH_IGNORE_CASE));
    }

    /**
     * Matches a {@link NamedElement} for an infix of its name.
     *
     * @param infix The expected infix of the name.
     * @param <T>   The type of the matched object.
     * @return An element matcher for a named element's name's infix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameContains(String infix) {
        return new NameMatcher<T>(new StringMatcher(infix, StringMatcher.Mode.CONTAINS));
    }

    /**
     * Matches a {@link NamedElement} for an infix of its name. The name's
     * capitalization is ignored.
     *
     * @param infix The expected infix of the name.
     * @param <T>   The type of the matched object.
     * @return An element matcher for a named element's name's infix.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameContainsIgnoreCase(String infix) {
        return new NameMatcher<T>(new StringMatcher(infix, StringMatcher.Mode.CONTAINS_IGNORE_CASE));
    }

    /**
     * Matches a {@link NamedElement} name against a regular expression.
     *
     * @param regex The regular expression to match the name against.
     * @param <T>   The type of the matched object.
     * @return An element matcher for a named element's name's against the given regular expression.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameMatches(String regex) {
        return new NameMatcher<T>(new StringMatcher(regex, StringMatcher.Mode.MATCHES));
    }

    /**
     * Matches a {@link ByteCodeElement}'s descriptor against a given value.
     *
     * @param descriptor The expected descriptor.
     * @param <T>        The type of the matched object.
     * @return A matcher for the given {@code descriptor}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> hasDescriptor(String descriptor) {
        return new DescriptorMatcher<T>(new StringMatcher(descriptor, StringMatcher.Mode.EQUALS_FULLY));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a given {@link java.lang.Class}. This matcher matches
     * a declared element's raw declaring type.
     *
     * @param type The type that is expected to declare the matched byte code element.
     * @param <T>  The type of the matched object.
     * @return A matcher for byte code elements being declared by the given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(Class<?> type) {
        return isDeclaredBy(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a given {@link TypeDescription}. This matcher matches
     * a declared element's raw declaring type.
     *
     * @param type The type that is expected to declare the matched byte code element.
     * @param <T>  The type of the matched object.
     * @return A matcher for byte code elements being declared by the given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(TypeDescription type) {
        return isDeclaredBy(is(type));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a {@link TypeDescription} that is matched by the given matcher. This matcher matches
     * a declared element's raw declaring type.
     *
     * @param matcher A matcher for the declaring type of the matched byte code element as long as it
     *                is not {@code null}.
     * @param <T>     The type of the matched object.
     * @return A matcher for byte code elements being declared by a type matched by the given {@code matcher}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredBy(ElementMatcher<? super TypeDescription> matcher) {
        return isDeclaredByGeneric(rawType(matcher));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a given generic {@link Type}.
     *
     * @param type The type that is expected to declare the matched byte code element.
     * @param <T>  The type of the matched object.
     * @return A matcher for byte code elements being declared by the given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredByGeneric(Type type) {
        return isDeclaredByGeneric(TypeDefinition.Sort.describe(type));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a given {@link TypeDescription.Generic}.
     *
     * @param type The type that is expected to declare the matched byte code element.
     * @param <T>  The type of the matched object.
     * @return A matcher for byte code elements being declared by the given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredByGeneric(TypeDescription.Generic type) {
        return isDeclaredByGeneric(is(type));
    }

    /**
     * Matches a {@link ByteCodeElement} for being declared by a {@link TypeDescription.Generic} that is matched by the given matcher.
     *
     * @param matcher A matcher for the declaring type of the matched byte code element as long as it is not {@code null}.
     * @param <T>     The type of the matched object.
     * @return A matcher for byte code elements being declared by a type matched by the given {@code matcher}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isDeclaredByGeneric(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return new DeclaringTypeMatcher<T>(matcher);
    }

    /**
     * Matches a {@link ByteCodeElement} that is visible to a given {@link java.lang.Class}.
     *
     * @param type The type that a matched byte code element is expected to be visible to.
     * @param <T>  The type of the matched object.
     * @return A matcher for a byte code element to be visible to a given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isVisibleTo(Class<?> type) {
        return isVisibleTo(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches a {@link ByteCodeElement} that is visible to a given
     * {@link TypeDescription}.
     *
     * @param type The type that a matched byte code element is expected to be visible to.
     * @param <T>  The type of the matched object.
     * @return A matcher for a byte code element to be visible to a given {@code type}.
     */
    public static <T extends ByteCodeElement> ElementMatcher.Junction<T> isVisibleTo(TypeDescription type) {
        return new VisibilityMatcher<T>(type);
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code abstract}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code abstract} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isAbstract() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.ABSTRACT);
    }

    /**
     * Matches an {@link net.bytebuddy.description.annotation.AnnotatedCodeElement} for declared annotations.
     * This matcher does not match inherited annotations which only exist for classes. Use
     * {@link net.bytebuddy.matcher.ElementMatchers#inheritsAnnotation(Class)} for matching inherited annotations.
     *
     * @param type The annotation type to match against.
     * @param <T>  The type of the matched object.
     * @return A matcher that validates that an annotated element is annotated with an annotation of {@code type}.
     */
    public static <T extends AnnotatedCodeElement> ElementMatcher.Junction<T> isAnnotatedWith(Class<? extends Annotation> type) {
        return isAnnotatedWith(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches an {@link net.bytebuddy.description.annotation.AnnotatedCodeElement} for declared annotations.
     * This matcher does not match inherited annotations which only exist for classes. Use
     * {@link net.bytebuddy.matcher.ElementMatchers#inheritsAnnotation(TypeDescription)}
     * for matching inherited annotations.
     *
     * @param type The annotation type to match against.
     * @param <T>  The type of the matched object.
     * @return A matcher that validates that an annotated element is annotated with an annotation of {@code type}.
     */
    public static <T extends AnnotatedCodeElement> ElementMatcher.Junction<T> isAnnotatedWith(TypeDescription type) {
        return isAnnotatedWith(is(type));
    }

    /**
     * Matches an {@link net.bytebuddy.description.annotation.AnnotatedCodeElement} for declared annotations.
     * This matcher does not match inherited annotations which only exist for classes. Use
     * {@link net.bytebuddy.matcher.ElementMatchers#inheritsAnnotation(net.bytebuddy.matcher.ElementMatcher)}
     * for matching inherited annotations.
     *
     * @param matcher The matcher to apply to any annotation's type found on the matched annotated element.
     * @param <T>     The type of the matched object.
     * @return A matcher that validates that an annotated element is annotated with an annotation of a type
     * that matches the given {@code matcher}.
     */
    public static <T extends AnnotatedCodeElement> ElementMatcher.Junction<T> isAnnotatedWith(ElementMatcher<? super TypeDescription> matcher) {
        return declaresAnnotation(annotationType(matcher));
    }

    /**
     * Matches an {@link net.bytebuddy.description.annotation.AnnotatedCodeElement} to declare any annotation
     * that matches the given matcher. Note that this matcher does not match inherited annotations that only exist
     * for types. Use {@link net.bytebuddy.matcher.ElementMatchers#inheritsAnnotation(net.bytebuddy.matcher.ElementMatcher)}
     * for matching inherited annotations.
     *
     * @param matcher A matcher to apply on any declared annotation of the matched annotated element.
     * @param <T>     The type of the matched object.
     * @return A matcher that validates that an annotated element is annotated with an annotation that matches
     * the given {@code matcher}.
     */
    public static <T extends AnnotatedCodeElement> ElementMatcher.Junction<T> declaresAnnotation(ElementMatcher<? super AnnotationDescription> matcher) {
        return new DeclaringAnnotationMatcher<T>(new CollectionItemMatcher<AnnotationDescription>(matcher));
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code public}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code public} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPublic() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.PUBLIC);
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code protected}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code protected} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isProtected() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.PROTECTED);
    }

    /**
     * Matches a {@link ModifierReviewable} that is package-private.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a package-private modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code private}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code private} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isPrivate() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.PRIVATE);
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code final}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code final} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isFinal() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.FINAL);
    }

    /**
     * Matches a {@link ModifierReviewable} that is {@code static}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code static} modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isStatic() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.STATIC);
    }

    /**
     * Matches a {@link ModifierReviewable} that is synthetic.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a synthetic modifier reviewable.
     */
    public static <T extends ModifierReviewable> ElementMatcher.Junction<T> isSynthetic() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.SYNTHETIC);
    }

    /**
     * Matches a {@link MethodDescription} that is {@code synchronized}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code synchronized} method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSynchronized() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.SYNCHRONIZED);
    }

    /**
     * Matches a {@link MethodDescription} that is {@code native}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code native} method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isNative() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.NATIVE);
    }

    /**
     * Matches a {@link MethodDescription} that is {@code strictfp}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a {@code strictfp} method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isStrict() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.STRICT);
    }

    /**
     * Matches a {@link MethodDescription} that is a var-args.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a var-args method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isVarArgs() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.VAR_ARGS);
    }

    /**
     * Matches a {@link MethodDescription} that is a bridge.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for a bridge method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isBridge() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.BRIDGE);
    }

    /**
     * Matches {@link MethodDescription}s that return a given generic type.
     *
     * @param type The generic type the matched method is expected to return.
     * @param <T>  The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returnsGeneric(Type type) {
        return returnsGeneric(TypeDefinition.Sort.describe(type));
    }

    /**
     * Matches {@link MethodDescription}s that returns a given
     * {@link TypeDescription}.
     *
     * @param type The type the matched method is expected to return.
     * @param <T>  The type of the matched object.
     * @return An element matcher that matches a given return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returnsGeneric(TypeDescription.Generic type) {
        return returnsGeneric(is(type));
    }

    /**
     * Matches {@link MethodDescription}s that return a given erasure type.
     *
     * @param type The raw type the matched method is expected to return.
     * @param <T>  The type of the matched object.
     * @return An element matcher that matches a given return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(Class<?> type) {
        return returnsGeneric(rawType(type));
    }

    /**
     * Matches {@link MethodDescription}s that return a given erasure type.
     *
     * @param type The raw type the matched method is expected to return.
     * @param <T>  The type of the matched object.
     * @return An element matcher that matches a given return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(TypeDescription type) {
        return returns(is(type));
    }

    /**
     * Matches a method's return type's erasure by the given matcher.
     *
     * @param matcher The matcher to apply to a method's return type's erasure.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches the matched method's return type's erasure.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returns(ElementMatcher<? super TypeDescription> matcher) {
        return returnsGeneric(rawType(matcher));
    }

    /**
     * Matches {@link MethodDescription}s that match a matched method's return type.
     *
     * @param matcher A matcher to apply onto a matched method's return type.
     * @param <T>     The type of the matched object.
     * @return An element matcher that matches a given return type against another {@code matcher}.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> returnsGeneric(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return new MethodReturnTypeMatcher<T>(matcher);
    }

    /**
     * Matches {@link MethodDescription}s that define a given generic type as a parameter at the given index.
     *
     * @param index The index of the parameter.
     * @param type  The generic type the matched method is expected to define as a parameter type.
     * @param <T>   The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArgument(int index, Type type) {
        return takesGenericArgument(index, TypeDefinition.Sort.describe(type));
    }

    /**
     * Matches {@link MethodDescription}s that define a given generic type as a parameter at the given index.
     *
     * @param index The index of the parameter.
     * @param type  The generic type the matched method is expected to define as a parameter type.
     * @param <T>   The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArgument(int index, TypeDescription.Generic type) {
        return takesGenericArgument(index, is(type));
    }

    /**
     * Matches {@link MethodDescription}s that define a given generic type as a parameter at the given index.
     *
     * @param index   The index of the parameter.
     * @param matcher A matcher for the generic type the matched method is expected to define as a parameter type.
     * @param <T>     The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArgument(int index, ElementMatcher<? super TypeDescription.Generic> matcher) {
        return takesGenericArguments(new CollectionElementMatcher<TypeDescription.Generic>(index, matcher));
    }

    /**
     * Matches a method description that takes the provided generic arguments.
     *
     * @param type The arguments to match against the matched method.
     * @param <T>  The type of the matched object.
     * @return A method matcher that matches a method's generic parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArguments(Type... type) {
        return takesGenericArguments(new TypeList.Generic.ForLoadedTypes(type));
    }

    /**
     * Matches a method description that takes the provided generic arguments.
     *
     * @param type The arguments to match against the matched method.
     * @param <T>  The type of the matched object.
     * @return A method matcher that matches a method's generic parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArguments(TypeDefinition... type) {
        return takesGenericArguments((Arrays.asList(type)));
    }

    /**
     * Matches a method description that takes the provided generic arguments.
     *
     * @param types The arguments to match against the matched method.
     * @param <T>   The type of the matched object.
     * @return A method matcher that matches a method's generic parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArguments(List<? extends TypeDefinition> types) {
        List<ElementMatcher<? super TypeDescription.Generic>> typeMatchers = new ArrayList<ElementMatcher<? super TypeDescription.Generic>>();
        for (TypeDefinition type : types) {
            typeMatchers.add(is(type));
        }
        return takesGenericArguments(new CollectionOneToOneMatcher<TypeDescription.Generic>(typeMatchers));
    }

    /**
     * Matches a {@link MethodDescription} by applying an iterable collection of element matcher on any parameter's {@link TypeDescription}.
     *
     * @param matchers The matcher that are applied onto the parameter types of the matched method description.
     * @param <T>      The type of the matched object.
     * @return A matcher that matches a method description by applying another element matcher onto each
     * parameter's type.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesGenericArguments(ElementMatcher<? super Iterable<? extends TypeDescription.Generic>> matchers) {
        return new MethodParametersMatcher<T>(new MethodParameterTypesMatcher<ParameterList<?>>(matchers));
    }

    /**
     * Matches {@link MethodDescription}s that define a given generic type as a parameter at the given index.
     *
     * @param index The index of the parameter.
     * @param type  The erasure of the type the matched method is expected to define as a parameter type.
     * @param <T>   The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArgument(int index, Class<?> type) {
        return takesGenericArgument(index, rawType(type));
    }

    /**
     * Matches {@link MethodDescription}s that define a given generic type as a parameter at the given index.
     *
     * @param index The index of the parameter.
     * @param type  The erasure of the type the matched method is expected to define as a parameter type.
     * @param <T>   The type of the matched object.
     * @return An element matcher that matches a given generic return type for a method description.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArgument(int index, TypeDescription type) {
        return takesGenericArgument(index, rawType(type));
    }

    /**
     * Matches a method description that takes the provided raw arguments.
     *
     * @param type The arguments to match against the matched method.
     * @param <T>  The type of the matched object.
     * @return A method matcher that matches a method's raw parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(Class<?>... type) {
        return takesGenericArguments(rawTypes(type));
    }

    /**
     * Matches a method description that takes the provided raw arguments.
     *
     * @param type The arguments to match against the matched method.
     * @param <T>  The type of the matched object.
     * @return A method matcher that matches a method's raw parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(TypeDescription... type) {
        return takesGenericArguments(rawTypes(type));
    }

    /**
     * Matches a method description that takes the provided raw arguments.
     *
     * @param types The arguments to match against the matched method.
     * @param <T>   The type of the matched object.
     * @return A method matcher that matches a method's raw parameter types against the supplied arguments.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(Iterable<? extends TypeDescription> types) {
        List<ElementMatcher<? super TypeDescription.Generic>> typeMatchers = new ArrayList<ElementMatcher<? super TypeDescription.Generic>>();
        for (TypeDescription type : types) {
            typeMatchers.add(rawType(type));
        }
        return takesGenericArguments(new CollectionOneToOneMatcher<TypeDescription.Generic>(typeMatchers));
    }

    /**
     * Matches a {@link MethodDescription} by the number of its parameters.
     *
     * @param length The expected length.
     * @param <T>    The type of the matched object.
     * @return A matcher that matches a method description by the number of its parameters.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(int length) {
        return new MethodParametersMatcher<T>(new CollectionSizeMatcher<ParameterList<?>>(length));
    }

    /**
     * Matches a {@link MethodDescription} by validating that its parameters
     * fulfill a given constraint.
     *
     * @param matcher The matcher to apply for validating the parameters.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches a method description's parameters against the given constraint.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> hasParameters(
            ElementMatcher<? super Iterable<? extends ParameterDescription>> matcher) {
        return new MethodParametersMatcher<T>(matcher);
    }

    /**
     * Matches a {@link MethodDescription} by its capability to throw a given
     * checked exception. For specifying a non-checked exception, any method is matched.
     *
     * @param exceptionType The type of the exception that should be declared by the method to be matched.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches a method description by its declaration of throwing a checked exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> canThrow(Class<? extends Throwable> exceptionType) {
        return canThrow(new TypeDescription.ForLoadedType(exceptionType));
    }

    /**
     * Matches a {@link MethodDescription} by its capability to throw a given
     * checked exception. For specifying a non-checked exception, any method is matched.
     *
     * @param exceptionType The type of the exception that should be declared by the method to be matched.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches a method description by its declaration of throwing a checked exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> canThrow(TypeDescription exceptionType) {
        return exceptionType.isAssignableTo(RuntimeException.class) || exceptionType.isAssignableTo(Error.class)
                ? new BooleanMatcher<T>(true)
                : ElementMatchers.<T>declaresGenericException(new CollectionItemMatcher<TypeDescription.Generic>(rawType(isSuperTypeOf(exceptionType))));
    }

    /**
     * Matches a method that declares the given generic exception type. For non-generic type, this matcher behaves identically to
     * {@link ElementMatchers#declaresException(Class)}. For exceptions that are expressed as type variables, only exceptions
     * that are represented as this type variable are matched.
     *
     * @param exceptionType The generic exception type that is matched exactly.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches any method that exactly matches the provided generic exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> declaresGenericException(Type exceptionType) {
        return declaresGenericException(TypeDefinition.Sort.describe(exceptionType));
    }

    /**
     * Matches a method that declares the given generic exception type. For non-generic type, this matcher behaves identically to
     * {@link ElementMatchers#declaresException(TypeDescription)}. For exceptions that are expressed as type variables, only exceptions
     * that are represented as this type variable are matched.
     *
     * @param exceptionType The generic exception type that is matched exactly.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches any method that exactly matches the provided generic exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> declaresGenericException(TypeDescription.Generic exceptionType) {
        return !exceptionType.getSort().isWildcard() && exceptionType.asErasure().isAssignableTo(Throwable.class)
                ? ElementMatchers.<T>declaresGenericException(new CollectionItemMatcher<TypeDescription.Generic>(is(exceptionType)))
                : new BooleanMatcher<T>(false);
    }

    /**
     * Matches a method that declares the given generic exception type as a (erased) exception type.
     *
     * @param exceptionType The exception type that is matched.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches any method that exactly matches the provided exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> declaresException(Class<? extends Throwable> exceptionType) {
        return declaresException(new TypeDescription.ForLoadedType(exceptionType));
    }

    /**
     * Matches a method that declares the given generic exception type as a (erased) exception type.
     *
     * @param exceptionType The exception type that is matched.
     * @param <T>           The type of the matched object.
     * @return A matcher that matches any method that exactly matches the provided exception.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> declaresException(TypeDescription exceptionType) {
        return exceptionType.isAssignableTo(Throwable.class)
                ? ElementMatchers.<T>declaresGenericException(new CollectionItemMatcher<TypeDescription.Generic>(rawType(exceptionType)))
                : new BooleanMatcher<T>(false);
    }

    /**
     * Matches a method's generic exception types against the provided matcher.
     *
     * @param matcher The exception matcher to apply onto the matched method's generic exceptions.
     * @param <T>     The type of the matched object.
     * @return A matcher that applies the provided matcher to a method's generic exception types.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> declaresGenericException(
            ElementMatcher<? super Iterable<? extends TypeDescription.Generic>> matcher) {
        return new MethodExceptionTypeMatcher<T>(matcher);
    }

    /**
     * Matches a {@link TypeDescription} that is an interface.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for an interface.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> isInterface() {
        return new ModifierMatcher<T>(ModifierMatcher.Mode.INTERFACE);
    }

    /**
     * Only matches method descriptions that represent a {@link java.lang.reflect.Method}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches method descriptions that represent a Java method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isMethod() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.METHOD);
    }

    /**
     * Only matches method descriptions that represent a {@link java.lang.reflect.Constructor}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches method descriptions that represent a Java constructor.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isConstructor() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.CONSTRUCTOR);
    }

    /**
     * Only matches method descriptions that represent a {@link java.lang.Class} type initializer.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches method descriptions that represent the type initializer.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isTypeInitializer() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.TYPE_INITIALIZER);
    }

    /**
     * Matches any method that is virtual, i.e. non-constructors that are non-static and non-private.
     *
     * @param <T> The type of the matched object.
     * @return A matcher for virtual methods.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isVirtual() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.VIRTUAL);
    }

    /**
     * Only matches Java 8 default methods.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches Java 8 default methods.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultMethod() {
        return new MethodSortMatcher<T>(MethodSortMatcher.Sort.DEFAULT_METHOD);
    }

    /**
     * Matches a default constructor, i.e. a constructor without arguments.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that matches a default constructor.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultConstructor() {
        return isConstructor().and(takesArguments(0));
    }

    /**
     * Only matches the {@link Object#finalize()} method if it was not overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches a non-overridden {@link Object#finalize()} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultFinalizer() {
        return isFinalizer().and(isDeclaredBy(TypeDescription.OBJECT));
    }

    /**
     * Only matches the {@link Object#finalize()} method, even if it was overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the {@link Object#finalize()} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isFinalizer() {
        return named("finalize").and(takesArguments(0)).and(returns(TypeDescription.VOID));
    }

    /**
     * Only matches the {@link Object#toString()} method, also if it was overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the {@link Object#toString()} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isHashCode() {
        return named("hashCode").and(takesArguments(0)).and(returns(int.class));
    }

    /**
     * Only matches the {@link Object#equals(Object)} method, also if it was overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the {@link Object#equals(Object)} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isEquals() {
        return named("equals").and(takesArguments(TypeDescription.OBJECT)).and(returns(boolean.class));
    }

    /**
     * Only matches the {@link Object#clone()} method, also if it was overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the {@link Object#clone()} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isClone() {
        return named("clone").and(takesArguments(0)).and(returns(TypeDescription.OBJECT));
    }

    /**
     * Only matches the {@link Object#toString()} method, also if it was overridden.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the {@link Object#toString()} method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isToString() {
        return named("toString").and(takesArguments(0)).and(returns(TypeDescription.STRING));
    }

    /**
     * Matches any Java bean setter method.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that matches any setter method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter() {
        return nameStartsWith("set").and(takesArguments(1)).and(returns(TypeDescription.VOID));
    }

    /**
     * Matches any Java bean setter method which takes an argument the given type.
     *
     * @param type The required setter type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any setter method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter(Type type) {
        return isSetter(TypeDefinition.Sort.describe(type));
    }

    /**
     * Matches any Java bean setter method which takes an argument the given type.
     *
     * @param type The required setter type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches a setter method with the specified argument type.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter(TypeDescription.Generic type) {
        return isSetter(is(type));
    }

    /**
     * Matches any Java bean setter method which takes an argument that matches the supplied matcher.
     *
     * @param matcher A matcher to be allied to a setter method's argument type.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches a setter method with an argument type that matches the supplied matcher.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isSetter(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return isSetter().and(takesGenericArguments(new CollectionOneToOneMatcher<TypeDescription.Generic>(Collections.singletonList(matcher))));
    }

    /**
     * Matches any Java bean getter method.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that matches any getter method.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter() {
        return takesArguments(0).and(not(returns(TypeDescription.VOID))).and(nameStartsWith("get")
                .or(nameStartsWith("is").and(returnsGeneric(anyOf(boolean.class, Boolean.class)))));
    }

    /**
     * Matches any Java bean getter method which returns the given type.
     *
     * @param type The required getter type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches a getter method with the given type.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter(Type type) {
        return isGetter(TypeDefinition.Sort.describe(type));
    }

    /**
     * Matches any Java bean getter method which returns the given type.
     *
     * @param type The required getter type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches a getter method with the given type.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter(TypeDescription.Generic type) {
        return isGetter(is(type));
    }

    /**
     * Matches any Java bean getter method which returns an value with a type matches the supplied matcher.
     *
     * @param matcher A matcher to be allied to a getter method's argument type.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches a getter method with a return type that matches the supplied matcher.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> isGetter(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return isGetter().and(returnsGeneric(matcher));
    }

    /**
     * Matches a method against its internal name such that constructors and type initializers are matched appropriately.
     *
     * @param internalName The internal name of the method.
     * @param <T>          The type of the matched object.
     * @return A matcher for a method with the provided internal name.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> hasMethodName(String internalName) {
        if (MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(internalName)) {
            return isConstructor();
        } else if (MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME.equals(internalName)) {
            return isTypeInitializer();
        } else {
            return named(internalName);
        }
    }

    /**
     * Matches any type description that is a subtype of the given type.
     *
     * @param type The type to be checked being a super type of the matched type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any type description that represents a sub type of the given type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSubTypeOf(Class<?> type) {
        return isSubTypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches any type description that is a subtype of the given type.
     *
     * @param type The type to be checked being a super type of the matched type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any type description that represents a sub type of the given type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSubTypeOf(TypeDescription type) {
        return new SubTypeMatcher<T>(type);
    }

    /**
     * Matches any type description that is a super type of the given type.
     *
     * @param type The type to be checked being a subtype of the matched type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any type description that represents a super type of the given type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSuperTypeOf(Class<?> type) {
        return isSuperTypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches any type description that is a super type of the given type.
     *
     * @param type The type to be checked being a subtype of the matched type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any type description that represents a super type of the given type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> isSuperTypeOf(TypeDescription type) {
        return new SuperTypeMatcher<T>(type);
    }

    /**
     * Matches any annotations by their type on a type that declared these annotations or inherited them from its
     * super classes.
     *
     * @param type The annotation type to be matched.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any inherited annotation by their type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> inheritsAnnotation(Class<?> type) {
        return inheritsAnnotation(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches any annotations by their type on a type that declared these annotations or inherited them from its
     * super classes.
     *
     * @param type The annotation type to be matched.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches any inherited annotation by their type.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> inheritsAnnotation(TypeDescription type) {
        return inheritsAnnotation(is(type));
    }

    /**
     * Matches any annotations by a given matcher on a type that declared these annotations or inherited them from its
     * super classes.
     *
     * @param matcher A matcher to apply onto the inherited annotations.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches any inherited annotation by a given matcher.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> inheritsAnnotation(ElementMatcher<? super TypeDescription> matcher) {
        return hasAnnotation(annotationType(matcher));
    }

    /**
     * Matches a list of annotations by a given matcher on a type that declared these annotations or inherited them
     * from its super classes.
     *
     * @param matcher A matcher to apply onto a list of inherited annotations.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches a list of inherited annotation by a given matcher.
     */
    public static <T extends TypeDescription> ElementMatcher.Junction<T> hasAnnotation(ElementMatcher<? super AnnotationDescription> matcher) {
        return new InheritedAnnotationMatcher<T>(new CollectionItemMatcher<AnnotationDescription>(matcher));
    }

    /**
     * Matches a type by a another matcher that is applied on any of its declared fields.
     *
     * @param matcher The matcher that is applied onto each declared field.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches any type where another matcher is matched positively on at least on declared field.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> declaresField(ElementMatcher<? super FieldDescription> matcher) {
        return new DeclaringFieldMatcher<T>(new CollectionItemMatcher<FieldDescription>(matcher));
    }

    /**
     * Matches a type by a another matcher that is applied on any of its declared methods.
     *
     * @param matcher The matcher that is applied onto each declared method.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches any type where another matcher is matched positively on at least on declared methods.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> declaresMethod(ElementMatcher<? super MethodDescription> matcher) {
        return new DeclaringMethodMatcher<T>(new CollectionItemMatcher<MethodDescription>(matcher));
    }

    /**
     * Matches generic type descriptions of the given sort.
     *
     * @param sort The generic type sort to match.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches generic types of the given sort.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> ofSort(TypeDefinition.Sort sort) {
        return ofSort(is(sort));
    }

    /**
     * Matches generic type descriptions of the given sort.
     *
     * @param matcher A matcher for a generic type's sort.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches generic types of the given sort.
     */
    public static <T extends TypeDefinition> ElementMatcher.Junction<T> ofSort(ElementMatcher<? super TypeDefinition.Sort> matcher) {
        return new TypeSortMatcher<T>(matcher);
    }

    /**
     * Matches a field's generic type against the provided matcher.
     *
     * @param fieldType The field type to match.
     * @param <T>       The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> genericFieldType(Type fieldType) {
        return genericFieldType(TypeDefinition.Sort.describe(fieldType));
    }

    /**
     * Matches a field's generic type against the provided matcher.
     *
     * @param fieldType The field type to match.
     * @param <T>       The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> genericFieldType(TypeDescription.Generic fieldType) {
        return genericFieldType(is(fieldType));
    }

    /**
     * Matches a field's generic type against the provided matcher.
     *
     * @param matcher The matcher to apply to the field's type.
     * @param <T>     The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> genericFieldType(ElementMatcher<? super TypeDescription.Generic> matcher) {
        return new FieldTypeMatcher<T>(matcher);
    }

    /**
     * Matches a field's raw type against the provided matcher.
     *
     * @param fieldType The field type to match.
     * @param <T>       The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> fieldType(Class<?> fieldType) {
        return fieldType(new TypeDescription.ForLoadedType(fieldType));
    }

    /**
     * Matches a field's raw type against the provided matcher.
     *
     * @param fieldType The field type to match.
     * @param <T>       The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> fieldType(TypeDescription fieldType) {
        return fieldType(is(fieldType));
    }

    /**
     * Matches a field's raw type against the provided matcher.
     *
     * @param matcher The matcher to apply to the field's type.
     * @param <T>     The type of the matched object.
     * @return A matcher matching the provided field type.
     */
    public static <T extends FieldDescription> ElementMatcher.Junction<T> fieldType(ElementMatcher<? super TypeDescription> matcher) {
        return genericFieldType(rawType(matcher));
    }

    /**
     * Matches if an annotation is of a given type.
     *
     * @param type The expected annotation type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches the annotation's type for being equal to the given type.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> annotationType(Class<? extends Annotation> type) {
        return annotationType(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches if an annotation is of a given type.
     *
     * @param type The expected annotation type.
     * @param <T>  The type of the matched object.
     * @return A matcher that matches the annotation's type for being equal to the given type.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> annotationType(TypeDescription type) {
        return annotationType(is(type));
    }

    /**
     * Matches if an annotation's type matches the supplied matcher.
     *
     * @param matcher The matcher to match the annotation's type against.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches the annotation's type.
     */
    public static <T extends AnnotationDescription> ElementMatcher.Junction<T> annotationType(ElementMatcher<? super TypeDescription> matcher) {
        return new AnnotationTypeMatcher<T>(matcher);
    }

    /**
     * Matches exactly the bootstrap {@link java.lang.ClassLoader} . The returned matcher is a synonym to
     * a matcher matching {@code null}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the bootstrap class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> isBootstrapClassLoader() {
        return new NullMatcher<T>();
    }

    /**
     * Matches exactly the system {@link java.lang.ClassLoader}. The returned matcher is a synonym to
     * a matcher matching {@code ClassLoader.gerSystemClassLoader()}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the system class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> isSystemClassLoader() {
        return new EqualityMatcher<T>(ClassLoader.getSystemClassLoader());
    }

    /**
     * Matches exactly the extension {@link java.lang.ClassLoader}. The returned matcher is a synonym to
     * a matcher matching {@code ClassLoader.gerSystemClassLoader().getParent()}.
     *
     * @param <T> The type of the matched object.
     * @return A matcher that only matches the extension class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> isExtensionClassLoader() {
        return new EqualityMatcher<T>(ClassLoader.getSystemClassLoader().getParent());
    }

    /**
     * Matches any class loader that is either the given class loader or a child of the given class loader.
     *
     * @param classLoader The class loader of which child class loaders are matched.
     * @param <T>         The type of the matched object.
     * @return A matcher that matches the given class loader and any class loader that is a child of the given
     * class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> isChildOf(ClassLoader classLoader) {
        return classLoader == BOOTSTRAP_CLASSLOADER
                ? new BooleanMatcher<T>(true)
                : ElementMatchers.<T>hasChild(is(classLoader));
    }

    /**
     * Matches all class loaders in the hierarchy of the matched class loader against a given matcher.
     *
     * @param matcher The matcher to apply to all class loaders in the hierarchy of the matched class loader.
     * @param <T>     The type of the matched object.
     * @return A matcher that matches all class loaders in the hierarchy of the matched class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> hasChild(ElementMatcher<? super ClassLoader> matcher) {
        return new ClassLoaderHierarchyMatcher<T>(matcher);
    }

    /**
     * Matches any class loader that is either the given class loader or a parent of the given class loader.
     *
     * @param classLoader The class loader of which parent class loaders are matched.
     * @param <T>         The type of the matched object.
     * @return A matcher that matches the given class loader and any class loader that is a parent of the given
     * class loader.
     */
    public static <T extends ClassLoader> ElementMatcher<T> isParentOf(ClassLoader classLoader) {
        return classLoader == BOOTSTRAP_CLASSLOADER
                ? ElementMatchers.<T>isBootstrapClassLoader()
                : new ClassLoaderParentMatcher<T>(classLoader);
    }
}
