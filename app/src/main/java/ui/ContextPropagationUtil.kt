@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ui

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context

/**
 * 1, We provide the `context` source.
 * 2, We provide `ContextPropagationUtil` to attach certain baggage such as uuid, cold launch id.
 * 3, We revise the codegen to support receiving the  `context` as the parameter.
 * 4, We are responsible to inject the Context into the network request as http header.
 * 5, The main responsibility from feature developers
 *  is to pass along the Context across functions and features.
 *
 */
object ContextPropagationUtil {

    fun attachedCheckInStarted(): Baggage {
        return Baggage.builder()
                .put("check_in_started", System.currentTimeMillis().toString())
                .build()
    }


    fun attachedLocationFetched(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("location_fetched", System.currentTimeMillis().toString())
                .build()
    }


    fun attachedSendingNetwork(context: Context): Baggage {
        return Baggage.fromContext(context).toBuilder()
                .put("sending_network", System.currentTimeMillis().toString())
                .build()
    }


}

