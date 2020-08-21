/* Copyright Airship and Contributors */

package com.urbanairship.automation.tags;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Result returned in the method {@link AudienceManager#getTags(Map)}}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TagGroupResult {

    /**
     * {@code true} if the result is successful, otherwise {@code false}.
     */
    public final boolean success;

    /**
     * The tag groups.
     */
    @NonNull
    public final Map<String, Set<String>> tagGroups;

    public TagGroupResult(boolean success, @Nullable Map<String, Set<String>> tagGroups) {
        this.success = success;
        this.tagGroups = tagGroups == null ? Collections.<String, Set<String>>emptyMap() : tagGroups;
    }

}
