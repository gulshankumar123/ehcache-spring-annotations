/**
 * Copyright 2010 Nicholas Blair, Eric Dalquist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.ehcache.annotations.key;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public abstract class AbstractDeepCacheKeyGeneratorTest<T extends Serializable> {
    
    public enum TestEnum {
        TEST1,
        TEST2;
    }
    
    protected abstract AbstractDeepCacheKeyGenerator<?, T> getCacheKeyGenerator();
    
    /**
     * Test circular reference handling
     */
    @Test
    public final void testCircularReferenceNoCheck() {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(false);
        generator.setUseReflection(false);
        
        final Object[] arg = new Object[2];
        final Object[] childArg = new Object[2];
        arg[0] = childArg;
        arg[1] = "argString";
        childArg[0] = arg;
        childArg[1] = "childArgString";
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { arg });
        
        replay(invocation);

        try {
            generator.generateKey(invocation);
            Assert.fail("Should have thrown a StackOverflowError");
        }
        catch (StackOverflowError e) {
            //expected
        }
        
        verify(invocation);
    }
    
    /**
     * Test circular reference handling
     */
    @Test
    public final void testCircularReference() {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Object[] arg = new Object[2];
        final Object[] childArg = new Object[2];
        arg[0] = childArg;
        arg[1] = "argString";
        childArg[0] = arg;
        childArg[1] = "childArgString";
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { arg });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        
        this.verifyTestCircularReference(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestCircularReference(MethodInvocation invocation, T key);
    
    /**
     * Test a circular reference hidden by a field with reflection
     */
    @Test
    public final void testCircularReferenceWithReflection() {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(true);
        generator.setUseReflection(true);
        
        final Object[] arg = new Object[2];
        final Object[] childArg = new Object[2];
        arg[0] = childArg;
        arg[1] = "argString";
        childArg[0] = new RequiresReflectionKey(arg);
        childArg[1] = "childArgString";
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { arg });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        
        this.verifyTestCircularReferenceWithReflection(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestCircularReferenceWithReflection(MethodInvocation invocation, T key);
    

    /**
     * Similar to the example used in the documentation so it is handy to be able to look at
     * what the generated key looks like
     */
    @Test
    public final void testForDocs() throws SecurityException, NoSuchMethodException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(true);
        generator.setIncludeParameterTypes(true);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Method testMethod = MethodInvocationHelper.class.getMethod("testMethod1", Object.class);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getMethod()).andReturn(testMethod);
        expect(invocation.getArguments()).andReturn(new Object[] { "49931" });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestForDocs(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestForDocs(MethodInvocation invocation, T key);
    
    /**
     * Enum doesn't implement hashCode and likely has special handling
     */
    @Test
    public final void testEnumHashCode() {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { TimeUnit.SECONDS, TestEnum.TEST1 });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestEnumHashCode(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestEnumHashCode(MethodInvocation invocation, T key);
    
    /**
     * Class doesn't implement hashCode and likely has special handling
     */
    @Test
    public final void testClassHashCode() {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { Integer.class });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyClassHashCode(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyClassHashCode(MethodInvocation invocation, T key);

    @Test
    public final void testComplexHashCode() throws SecurityException, NoSuchMethodException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(true);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Method testMethod = MethodInvocationHelper.class.getMethod("testMethod2", int[].class, String.class, boolean[].class, Object.class);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getMethod()).andReturn(testMethod);
        expect(invocation.getArguments()).andReturn(new Object[] { 
                new int[] {1, 2, 3, 4}, 
                "foo", 
                new boolean[] {false, true},
                new Object[] { null, new Date(0) }
                });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestComplexHashCode(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestComplexHashCode(MethodInvocation invocation, T key);

    @Test
    public final void testPrimitiveArrayHandling() throws SecurityException, NoSuchMethodException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(true);
        generator.setIncludeParameterTypes(true);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Method testMethod = MethodInvocationHelper.class.getMethod("testMethod1", Object.class);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getMethod()).andReturn(testMethod);
        expect(invocation.getArguments()).andReturn(new Object[] { 
                    new Object[] { 
                        new byte[] {},
                        new short[] {},
                        new int[] {},
                        new long[] {},
                        new char[] {},
                        new float[] {},
                        new double[] {},
                        new boolean[] {},

                        new byte[] {1},
                        new short[] {2},
                        new int[] {3},
                        new long[] {4l},
                        new char[] {'a'},
                        new float[] {6.8f},
                        new double[] {7.9d},
                        new boolean[] {true},
                        
                        new byte[] {1, 2, 3},
                        new short[] {4, 5, 6},
                        new int[] {7, 8, 9},
                        new long[] {10l, 11l, 12l},
                        new char[] {'a', 'b', 'c'},
                        new float[] {16.1f, 17.2f, 18.3f},
                        new double[] {19.4, 20.5, 21.6},
                        new boolean[] {true, false, false}
                    }
                });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestPrimitiveArrayHandling(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestPrimitiveArrayHandling(MethodInvocation invocation, T key);
    
    @Test
    public final void testPrimitiveHandling() throws SecurityException, NoSuchMethodException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(true);
        generator.setIncludeParameterTypes(true);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Method testMethod = MethodInvocationHelper.class.getMethod("testMethod1", Object.class);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getMethod()).andReturn(testMethod);
        expect(invocation.getArguments()).andReturn(new Object[] { 
                    new Object[] { 
                        (byte)1,
                        (short)2,
                        (int)3,
                        (long)4l,
                        (char)'a',
                        (float)6.8f,
                        (double)7.9d,
                        (boolean)true
                    }
                });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestPrimitiveHandling(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestPrimitiveHandling(MethodInvocation invocation, T key);
    
    @Test
    public final void testCollectionHandling() throws SecurityException, NoSuchMethodException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(true);
        generator.setIncludeParameterTypes(true);
        generator.setCheckforCycles(true);
        generator.setUseReflection(false);
        
        final Method testMethod = MethodInvocationHelper.class.getMethod("testMethod1", Object.class);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getMethod()).andReturn(testMethod);
        
        final Map<Object, Object> testMap = new HashMap<Object, Object>();
        testMap.put("A", 123);
        testMap.put("B", new String[] {"hello", "world"});
        
        expect(invocation.getArguments()).andReturn(new Object[] { 
                    new Object[] { 
                        new LinkedHashSet<Object>(Arrays.asList("foo", "bar", "bop")),
                        testMap
                    }
                });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestCollectionHandling(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestCollectionHandling(MethodInvocation invocation, T key);
    
    @Test
    public final void testNoArguments() throws SecurityException {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(false);
        generator.setUseReflection(false);
        
        final MethodInvocation invocation = createMock(MethodInvocation.class);
        expect(invocation.getArguments()).andReturn(new Object[] { });
        
        replay(invocation);
        
        final T key = generator.generateKey(invocation);
        this.verifyTestNoArguments(invocation, key);
        
        verify(invocation);
    }
    protected abstract void verifyTestNoArguments(MethodInvocation invocation, T key);
    
    @Test
    public final void testGeneratesDifferentKeysWithDifferentNonIntegerPartsOfFloatParameter() throws Exception {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(false);
        generator.setUseReflection(false);
        
        final MethodInvocation firstInvocation = createMock(MethodInvocation.class);
        expect(firstInvocation.getArguments()).andReturn(new Object[] { 1.5f });
        
        final MethodInvocation secondInvocation = createMock(MethodInvocation.class);
        expect(secondInvocation.getArguments()).andReturn(new Object[] { 1.7f });
        
        
        replay(firstInvocation, secondInvocation);
        
        final T firstKey = generator.generateKey(firstInvocation);
        final T secondKey = generator.generateKey(secondInvocation);

        assertFalse(firstKey.equals(secondKey));
        assertFalse(secondKey.equals(firstKey));

        this.verifyTestGeneratesDifferentKeysWithDifferentNonIntegerPartsOfFloatParameter(firstInvocation, firstKey, secondKey);
        
        verify(firstInvocation, secondInvocation);
    }
    protected abstract void verifyTestGeneratesDifferentKeysWithDifferentNonIntegerPartsOfFloatParameter(MethodInvocation invocation, T firstKey, T secondKey);
    
    @Test
    public final void testGeneratesDifferentKeysWithDifferentNonIntegerPartsOfDoubleParameter() throws Exception {
        final AbstractDeepCacheKeyGenerator<?, T> generator = this.getCacheKeyGenerator();
        
        generator.setIncludeMethod(false);
        generator.setIncludeParameterTypes(false);
        generator.setCheckforCycles(false);
        generator.setUseReflection(false);
        
        final MethodInvocation firstInvocation = createMock(MethodInvocation.class);
        expect(firstInvocation.getArguments()).andReturn(new Object[] { 1.5d });
        
        final MethodInvocation secondInvocation = createMock(MethodInvocation.class);
        expect(secondInvocation.getArguments()).andReturn(new Object[] { 1.7d });
        
        
        replay(firstInvocation, secondInvocation);
        
        final T firstKey = generator.generateKey(firstInvocation);
        final T secondKey = generator.generateKey(secondInvocation);

        assertFalse(firstKey.equals(secondKey));
        assertFalse(secondKey.equals(firstKey));

        this.verifyTestGeneratesDifferentKeysWithDifferentNonIntegerPartsOfDoubleParameter(firstInvocation, firstKey, secondKey);
        
        verify(firstInvocation, secondInvocation);
    }
    protected abstract void verifyTestGeneratesDifferentKeysWithDifferentNonIntegerPartsOfDoubleParameter(MethodInvocation invocation, T firstKey, T secondKey);
    
}
