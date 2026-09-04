package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class UmpConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w("UmpConsentManager", "Consent form error: ${formError.message}")
                        }
                        onComplete()
                    }
                },
                { requestConsentError ->
                    Log.w("UmpConsentManager", "Consent update failed: ${requestConsentError.message}")
                    onComplete()
                }
            )
        } catch (e: Exception) {
            Log.e("UmpConsentManager", "UMP gatherConsent error", e)
            onComplete()
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.w("UmpConsentManager", "Privacy options form error: ${formError.message}")
                }
                onDismiss()
            }
        } catch (e: Exception) {
            Log.e("UmpConsentManager", "Error showing privacy options", e)
            onDismiss()
        }
    }
}
