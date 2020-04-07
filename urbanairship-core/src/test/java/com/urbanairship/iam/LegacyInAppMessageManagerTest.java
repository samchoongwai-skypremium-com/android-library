/* Copyright Airship and Contributors */

package com.urbanairship.iam;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.urbanairship.BaseTestCase;
import com.urbanairship.PendingResult;
import com.urbanairship.PreferenceDataStore;
import com.urbanairship.TestApplication;
import com.urbanairship.analytics.Analytics;
import com.urbanairship.iam.banner.BannerDisplayContent;
import com.urbanairship.push.NotificationInfo;
import com.urbanairship.push.PushManager;
import com.urbanairship.push.PushMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LegacyInAppMessageManager}
 */
public class LegacyInAppMessageManagerTest extends BaseTestCase {

    LegacyInAppMessageManager legacyInAppMessageManager;
    PreferenceDataStore preferenceDataStore;
    InAppMessageManager inAppMessageManager;
    Analytics analytics;
    PushMessage pushMessage;
    PushManager pushManager;

    @Before
    public void setup() {
        preferenceDataStore = TestApplication.getApplication().preferenceDataStore;
        inAppMessageManager = mock(InAppMessageManager.class);
        analytics = mock(Analytics.class);
        pushManager = mock(PushManager.class);
        legacyInAppMessageManager = new LegacyInAppMessageManager(TestApplication.getApplication(), preferenceDataStore, inAppMessageManager, analytics, pushManager);

        String inAppJson = "{\"display\": {\"primary_color\": \"#FF0000\"," +
                "\"duration\": 10, \"secondary_color\": \"#00FF00\", \"type\": \"banner\"," +
                "\"alert\": \"Oh hi!\"}, \"actions\": {\"button_group\": \"ua_yes_no\"," +
                "\"button_actions\": {\"yes\": {\"^+t\": \"yes_tag\"}, \"no\": {\"^+t\": \"no_tag\"}}," +
                "\"on_click\": {\"^d\": \"someurl\"}}, \"expiry\": \"2015-12-12T12:00:00\", \"extra\":" +
                "{\"wat\": 123, \"Tom\": \"Selleck\"}}";

        Bundle extras = new Bundle();
        extras.putString(PushMessage.EXTRA_IN_APP_MESSAGE, inAppJson);
        extras.putString(PushMessage.EXTRA_SEND_ID, "send id");

        pushMessage = new PushMessage(extras);
    }

    @Test
    public void testInit() {
        legacyInAppMessageManager.init();
        verify(pushManager).addPushListener(legacyInAppMessageManager);
        verify(pushManager).addInternalNotificationListener(legacyInAppMessageManager);
    }

    @Test
    public void testPushReceived() {
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        verify(inAppMessageManager).scheduleMessage(argThat(new ArgumentMatcher<InAppMessageScheduleInfo>() {
            @Override
            public boolean matches(InAppMessageScheduleInfo argument) {
                if (!argument.getInAppMessage().getId().equals("send id")) {
                    return false;
                }

                BannerDisplayContent displayContent = argument.getInAppMessage().getDisplayContent();
                if (!displayContent.getBody().getText().equals("Oh hi!")) {
                    return false;
                }

                if (displayContent.getDuration() != 10000) {
                    return false;
                }

                return true;
            }
        }));
    }

    @Test
    public void testPushReceivedCancelsPreviousIam() {
        // Receive the first push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Create a new push
        Bundle extras = pushMessage.getPushBundle();
        extras.putString(PushMessage.EXTRA_SEND_ID, "some other id");
        PushMessage otherPush = new PushMessage(extras);

        // Set up a pending result for a cancelled message
        PendingResult<Boolean> pendingResult = new PendingResult<>();
        pendingResult.setResult(true);
        when(inAppMessageManager.cancelMessage("send id")).thenReturn(pendingResult);

        // Receive the other push
        legacyInAppMessageManager.onPushReceived(otherPush, true);

        // Verify it added a resolution event for the previous message
        verify(analytics).addEvent(any(ResolutionEvent.class));
    }

    @Test
    public void testPushReceivedPreviousAlreadyDisplayed() {
        // Receive the first push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Create a new push
        Bundle extras = pushMessage.getPushBundle();
        extras.putString(PushMessage.EXTRA_SEND_ID, "some other id");
        PushMessage otherPush = new PushMessage(extras);

        // Set up a pending result for a cancelled message
        PendingResult<Boolean> pendingResult = new PendingResult<>();
        pendingResult.setResult(false);
        when(inAppMessageManager.cancelMessage("send id")).thenReturn(pendingResult);

        // Receive the other push
        legacyInAppMessageManager.onPushReceived(otherPush, true);

        // Verify it did not add a resolution event for the previous message
        verify(analytics, never()).addEvent(any(ResolutionEvent.class));
    }

    @Test
    public void testPushResponse() {
        // Receive the push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Set up a pending result for a cancelled message
        PendingResult<Boolean> pendingResult = new PendingResult<>();
        pendingResult.setResult(true);
        when(inAppMessageManager.cancelMessage("send id")).thenReturn(pendingResult);

        // Receive the response
        NotificationInfo info = new NotificationInfo(pushMessage, 1, "cool");
        legacyInAppMessageManager.onNotificationResponse(info, null);

        // Verify it added a resolution event for the message
        verify(analytics).addEvent(any(ResolutionEvent.class));
    }

    @Test
    public void testPushResponseAlreadyDisplayed() {
        // Receive the push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Set up a pending result for a cancelled message
        PendingResult<Boolean> pendingResult = new PendingResult<>();
        pendingResult.setResult(false);
        when(inAppMessageManager.cancelMessage("send id")).thenReturn(pendingResult);

        // Receive the response
        NotificationInfo info = new NotificationInfo(pushMessage, 1, "cool");
        legacyInAppMessageManager.onNotificationResponse(info, null);

        // Verify it did not add a resolution event for the message
        verify(analytics, never()).addEvent(any(ResolutionEvent.class));
    }

    @Test
    public void testMessageExtenderException() {
        legacyInAppMessageManager.setMessageBuilderExtender(new LegacyInAppMessageManager.MessageBuilderExtender() {
            @NonNull
            @Override
            public InAppMessage.Builder extend(@NonNull Context context, @NonNull InAppMessage.Builder builder, @NonNull LegacyInAppMessage legacyMessage) {
                throw new RuntimeException("exception!");
            }
        });

        // Receive the push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Verify we did not try to schedule an in-app message
        verifyZeroInteractions(inAppMessageManager);
    }

    @Test
    public void testScheduleExtenderException() {
        legacyInAppMessageManager.setScheduleBuilderExtender(new LegacyInAppMessageManager.ScheduleInfoBuilderExtender() {
            @NonNull
            @Override
            public InAppMessageScheduleInfo.Builder extend(@NonNull Context context, @NonNull InAppMessageScheduleInfo.Builder builder, @NonNull LegacyInAppMessage legacyMessage) {
                throw new RuntimeException("exception!");
            }
        });

        // Receive the push
        legacyInAppMessageManager.onPushReceived(pushMessage, true);

        // Verify we did not try to schedule an in-app message
        verifyZeroInteractions(inAppMessageManager);
    }

}
