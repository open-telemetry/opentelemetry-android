/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.log;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

import android.util.Log;
import io.opentelemetry.instrumentation.library.log.AndroidLogSubstitutions;
import java.io.IOException;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.build.AndroidDescriptor;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

public class AndroidLogPlugin implements Plugin {
    private final AndroidDescriptor androidDescriptor;

    public AndroidLogPlugin(AndroidDescriptor androidDescriptor) {
        this.androidDescriptor = androidDescriptor;
    }

    @Override
    public DynamicType.Builder<?> apply(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassFileLocator classFileLocator) {
        try {
            return builder.visit(
                    MemberSubstitution.relaxed()
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "v", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForVerbose", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "v",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForVerbose2",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "d", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForDebug", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "d",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForDebug2",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "i", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForInfo", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "i",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForInfo2",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "w", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWarn", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "w", String.class, Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWarn2", String.class, Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "w",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWarn3",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "e", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForError", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "e",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForError2",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "wtf", String.class, String.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWtf", String.class, String.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "wtf", String.class, Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWtf2", String.class, Throwable.class))
                            .method(
                                    is(
                                            Log.class.getDeclaredMethod(
                                                    "wtf",
                                                    String.class,
                                                    String.class,
                                                    Throwable.class)))
                            .replaceWith(
                                    AndroidLogSubstitutions.class.getDeclaredMethod(
                                            "substitutionForWtf3",
                                            String.class,
                                            String.class,
                                            Throwable.class))
                            .on(isMethod()));

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // No operation.
    }

    @Override
    public boolean matches(TypeDescription target) {
        return androidDescriptor.getTypeScope(target) == AndroidDescriptor.TypeScope.LOCAL;
    }
}
