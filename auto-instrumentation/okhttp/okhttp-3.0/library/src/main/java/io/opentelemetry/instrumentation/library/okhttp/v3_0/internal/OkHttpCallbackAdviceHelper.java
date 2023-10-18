/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import okhttp3.Call;
import okhttp3.Request;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttpCallbackAdviceHelper {

    public static boolean propagateContext(Call call) {
        Context context = Context.current();
        if (shouldPropagateContext(context)) {
            VirtualField<Request, Context> virtualField =
                    VirtualField.find(Request.class, Context.class);
            virtualField.set(call.request(), context);
            return true;
        }

        return false;
    }

    public static Context tryRecoverPropagatedContextFromCallback(Request request) {
        VirtualField<Request, Context> virtualField =
                VirtualField.find(Request.class, Context.class);
        return virtualField.get(request);
    }

    private static boolean shouldPropagateContext(Context context) {
        return context != Context.root();
    }
}
