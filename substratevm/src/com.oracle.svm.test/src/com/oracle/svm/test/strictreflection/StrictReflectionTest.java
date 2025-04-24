/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.test.strictreflection;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Unit tests for the -H:StrictReflection=Enforce mode.
 */
public class StrictReflectionTest {

    /*
     * References to these static final String fields will get folded into
     * an LDC(_W) instruction by javac, thus using them is equivalent to
     * using a String literal.
     */
    private static final String TEST_CLASS_A_NAME = "com.oracle.svm.test.strictreflection.TestClassA";
    private static final String TEST_CLASS_B_NAME = "com.oracle.svm.test.strictreflection.TestClassB";

    @Test
    public void testForNameLiteral() {
        try {
            Class<?> clazz = Class.forName(TEST_CLASS_A_NAME);
            Assert.assertEquals(clazz, TestClassA.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testForNameSimpleVariable() {
        try {
            String className = TEST_CLASS_A_NAME;
            Class<?> clazz = Class.forName(className);
            Assert.assertEquals(clazz, TestClassA.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testForNameDivergentVariable() {
        Assert.assertThrows(Throwable.class, () -> {
            String className = isEven(2) ? TEST_CLASS_A_NAME : TEST_CLASS_B_NAME;
            Class.forName(className);
        });

        Assert.assertThrows(Throwable.class, () -> {
            String className;
            if (isEven(4)) {
                className = TEST_CLASS_A_NAME;
            } else {
                className = TEST_CLASS_B_NAME;
            }
            Class.forName(className);
        });
    }

    @Test
    public void testForNameSimpleLoopVariable() {
        try {
            String className = TEST_CLASS_A_NAME;
            for (int i = 0; i < 1; i++) {
                Class<?> clazz = Class.forName(className);
                Assert.assertEquals(clazz, TestClassA.class);
            }
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testForNameDivergentLoopVariable() {
        Assert.assertThrows(Throwable.class, () -> {
            String className = TEST_CLASS_A_NAME;
            for (int i = 0; i < 1; i++) {
                Class.forName(className);
                className = TEST_CLASS_B_NAME;
            }
        });
    }

    @Test
    public void testForNameTrivialLoopVariable() {
        try {
            String className = TEST_CLASS_A_NAME;
            for (int i = 0; i < 1; i++) {
                Class<?> clazz = Class.forName(className);
                Assert.assertEquals(clazz, TestClassA.class);
                className = TEST_CLASS_B_NAME;
                break;
            }
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testForNameNested() {
        Assert.assertThrows(Throwable.class, () -> nestedForName(TEST_CLASS_A_NAME));
    }

    /**
     * Javac duplicates code when generating bytecode for try-catch-finally constructs
     * by embedding the code from the finally block into the try and catch blocks. Because
     * of this, className is actually dominated by a store to a compile time constant
     * in the following example.
     */
    @Test
    public void testForNameTryCatchFinally() {
        String className = "";
        try {
            className = TEST_CLASS_A_NAME;
            throw new Throwable();
        } catch (Throwable t) {
            className = TEST_CLASS_B_NAME;
        } finally {
            try {
                Class<?> clazz = Class.forName(className);
                Assert.assertEquals(clazz, TestClassB.class);
            } catch (Throwable t) {
                Assert.fail(t.getMessage());
            }
        }
    }

    @Test
    public void testForNameNull() {
        Assert.assertThrows(NullPointerException.class, () -> Class.forName(null));
    }

    @Test
    public void testGetDeclaredFieldLiterals() {
        try {
            Field field = TestClassA.class.getDeclaredField("someField");
            assertFieldSignatureMatches(field, TestClassA.class, int.class, "someField");
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testForNamePropagates() {
        try {
            Class<?> clazz = Class.forName(TEST_CLASS_A_NAME);
            Field field = clazz.getDeclaredField("someField");
            assertFieldSignatureMatches(field, TestClassA.class, int.class, "someField");
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetDeclaredMethodLiterals() {
        try {
            Method method = TestClassA.class.getDeclaredMethod("someMethod", String.class, int.class);
            assertMethodSignatureMatches(method, TestClassA.class, String.class, "someMethod", String.class, int.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetDeclaredMethodDirectArrayInitialization() {
        try {
            Method method = TestClassA.class.getDeclaredMethod("someMethod", new Class<?>[]{String.class, int.class});
            assertMethodSignatureMatches(method, TestClassA.class, String.class, "someMethod", String.class, int.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetDeclaredMethodDivergentParameterType() {
        Assert.assertThrows(Throwable.class, () -> TestClassA.class.getDeclaredMethod("someMethod", String.class, isEven(4) ? int.class : double.class));
    }

    @Test
    public void testGetDeclaredMethodParameterTypesVariable() {
        Assert.assertThrows(Throwable.class, () -> {
            Class<?>[] parameterTypes = new Class<?>[]{String.class, int.class};
            TestClassA.class.getDeclaredMethod("someMethod", parameterTypes);
        });
    }

    @Test
    public void testGetDeclaredConstructorLiterals() {
        try {
            Constructor<?> constructor = TestClassA.class.getDeclaredConstructor();
            assertConstructorSignatureMatches(constructor, TestClassA.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetDeclaredConstructorEmptyArray() {
        try {
            Constructor<?> constructor = TestClassA.class.getDeclaredConstructor(new Class<?>[0]);
            assertConstructorSignatureMatches(constructor, TestClassA.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetDeclaredConstructorNullAsParameterTypes() {
        try {
            Constructor<?> constructor = TestClassA.class.getDeclaredConstructor((Class<?>[]) null);
            assertConstructorSignatureMatches(constructor, TestClassA.class);
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    private static void assertFieldSignatureMatches(Field field, Class<?> declaringClass, Class<?> type, String name) {
        Assert.assertEquals(field.getDeclaringClass(), declaringClass);
        Assert.assertEquals(field.getType(), type);
        Assert.assertEquals(field.getName(), name);
    }

    private static void assertMethodSignatureMatches(Method method, Class<?> declaringClass, Class<?> returnType, String name, Class<?>... parameterTypes) {
        Assert.assertEquals(method.getDeclaringClass(), declaringClass);
        Assert.assertEquals(method.getReturnType(), returnType);
        Assert.assertEquals(method.getName(), name);
        Assert.assertArrayEquals(method.getParameterTypes(), parameterTypes);
    }

    private static void assertConstructorSignatureMatches(Constructor<?> constructor, Class<?> declaringClass, Class<?>... parameterTypes) {
        Assert.assertEquals(constructor.getDeclaringClass(), declaringClass);
        Assert.assertArrayEquals(constructor.getParameterTypes(), parameterTypes);
    }

    private static boolean isEven(int n) {
        return n % 2 == 0;
    }

    private static Class<?> nestedForName(String className) throws Throwable {
        return Class.forName(className);
    }
}

class TestClassA {
    private static int someField = 42;

    TestClassA() {

    }

    private static String someMethod(String s, int n) {
        return s.repeat(n);
    }
}

class TestClassB {

}
