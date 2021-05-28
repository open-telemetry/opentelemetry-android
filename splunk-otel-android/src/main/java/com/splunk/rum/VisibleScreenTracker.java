package com.splunk.rum;

import android.app.Activity;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Wherein we do our best to figure out what "screen" is visible and what was the previously visible "screen".
 * <p>
 * In general, we favor using the last fragment that was resumed, but fall back to the last resumed activity
 * in case we don't have a fragment.
 * <p>
 * We always ignore NavHostFragment instances since they aren't ever visible to the user.
 * <p>
 * We have to treat DialogFragments slightly differently since they don't replace the launching screen, and
 * the launching screen never leaves visibility.
 */
class VisibleScreenTracker {
    private final AtomicReference<String> lastResumedActivity = new AtomicReference<>();
    private final AtomicReference<String> previouslyLastResumedActivity = new AtomicReference<>();
    private final AtomicReference<String> lastResumedFragment = new AtomicReference<>();
    private final AtomicReference<String> previouslyLastResumedFragment = new AtomicReference<>();

    String getPreviouslyVisibleScreen() {
        String previouslyLastFragment = previouslyLastResumedFragment.get();
        if (previouslyLastFragment != null) {
            return previouslyLastFragment;
        }
        return previouslyLastResumedActivity.get();
    }

    String getCurrentlyVisibleScreen() {
        String lastFragment = lastResumedFragment.get();
        if (lastFragment != null) {
            return lastFragment;
        }
        String lastActivity = lastResumedActivity.get();
        if (lastActivity != null) {
            return lastActivity;
        }
        return "unknown";
    }

    void activityResumed(Activity activity) {
        lastResumedActivity.set(activity.getClass().getSimpleName());
    }

    void activityPaused(Activity activity) {
        previouslyLastResumedActivity.set(activity.getClass().getSimpleName());
        lastResumedActivity.compareAndSet(activity.getClass().getSimpleName(), null);
    }

    void fragmentResumed(Fragment fragment) {
        //skip the NavHostFragment since it's never really "visible" by itself.
        if (fragment instanceof NavHostFragment) {
            return;
        }

        if (fragment instanceof DialogFragment) {
            previouslyLastResumedFragment.set(lastResumedFragment.get());
        }
        lastResumedFragment.set(fragment.getClass().getSimpleName());
    }

    void fragmentPaused(Fragment fragment) {
        //skip the NavHostFragment since it's never really "visible" by itself.
        if (fragment instanceof NavHostFragment) {
            return;
        }
        if (fragment instanceof DialogFragment) {
            lastResumedFragment.set(previouslyLastResumedFragment.get());
        } else {
            lastResumedFragment.compareAndSet(fragment.getClass().getSimpleName(), null);
        }
        previouslyLastResumedFragment.set(fragment.getClass().getSimpleName());
    }
}
