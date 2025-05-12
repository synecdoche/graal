/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.nativeimage.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.graalvm.nativeimage.hosted.RegistrationCondition;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeProxyCreation;

public interface RuntimeReflectionSupport extends ReflectionRegistry {
    // needed as reflection-specific ImageSingletons key
    void registerAllMethodsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllDeclaredMethodsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllFieldsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllDeclaredFieldsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllConstructorsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllDeclaredConstructorsQuery(RegistrationCondition condition, boolean queriedOnly, Class<?> clazz);

    void registerAllClassesQuery(RegistrationCondition condition, Class<?> clazz);

    void registerAllDeclaredClassesQuery(RegistrationCondition condition, Class<?> clazz);

    void registerAllFields(RegistrationCondition condition, Class<?> clazz);

    void registerAllDeclaredFields(RegistrationCondition condition, Class<?> clazz);

    void registerAllRecordComponentsQuery(RegistrationCondition condition, Class<?> clazz);

    void registerAllPermittedSubclassesQuery(RegistrationCondition condition, Class<?> clazz);

    void registerAllNestMembersQuery(RegistrationCondition condition, Class<?> clazz);

    void registerAllSignersQuery(RegistrationCondition condition, Class<?> clazz);

    void registerClassLookupException(RegistrationCondition condition, String typeName, Throwable t);

    default void registerClassFully(RegistrationCondition condition, Class<?> clazz) {
        register(condition, false, clazz);

        // GR-62143 Register all fields is very slow.
        // registerAllDeclaredFields(condition, clazz);
        // registerAllFields(condition, clazz);
        registerAllDeclaredMethodsQuery(condition, false, clazz);
        registerAllMethodsQuery(condition, false, clazz);
        registerAllDeclaredConstructorsQuery(condition, false, clazz);
        registerAllConstructorsQuery(condition, false, clazz);
        registerAllClassesQuery(condition, clazz);
        registerAllDeclaredClassesQuery(condition, clazz);
        registerAllNestMembersQuery(condition, clazz);
        registerAllPermittedSubclassesQuery(condition, clazz);
        registerAllRecordComponentsQuery(condition, clazz);
        registerAllSignersQuery(condition, clazz);

        /* Register every single-interface proxy */
        // GR-62293 can't register proxies from jdk modules.
        if (clazz.getModule() == null && clazz.isInterface()) {
            RuntimeProxyCreation.register(clazz);
        }

        RuntimeJNIAccess.register(clazz);
        try {
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                RuntimeJNIAccess.register(declaredMethod);
            }
            for (Constructor<?> declaredConstructor : clazz.getDeclaredConstructors()) {
                RuntimeJNIAccess.register(declaredConstructor);
            }
            // GR-62143 Registering all fields is very slow.
            // for (Field declaredField : clazz.getDeclaredFields()) {
            // RuntimeJNIAccess.register(declaredField);
            // RuntimeReflection.register(declaredField);
            // }
        } catch (LinkageError e) {
            /* If we can't link we can not register for JNI */
        }

        // GR-62143 Registering all fields is very slow.
        // RuntimeSerialization.register(clazz);

        // if we register unsafe allocated earlier there are build-time initialization errors
        register(condition, !(clazz.isArray() || clazz.isInterface() || clazz.isPrimitive() || Modifier.isAbstract(clazz.getModifiers())), clazz);
    }
}
