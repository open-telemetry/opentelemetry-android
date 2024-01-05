/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

/**
 * Sets up a scheduling mechanism to read and export previously stored signals in disk.
 */
interface ExportScheduleHandler {
    /**
     * Start/Set up the exporting schedule. Called when the disk buffering feature gets enabled.
     */
    fun enable()

    /**
     * Don't start (or stop if something was previously started) the exporting schedule, no look up
     * for data stored in the disk to export will be carried over if this function is called.
     * This will be called if the disk buffering feature gets disabled.
     */
    fun disable() {}
}
