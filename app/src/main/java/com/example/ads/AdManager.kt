package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob sample test IDs
    const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAppOpenAd = false
    private var loadTime: Long = 0

    private var interstitialAd: InterstitialAd? = null
    private var isShowingInterstitialAd = false
    private var isLoadingInterstitial = false

    private val _isAdsReady = MutableStateFlow(false)
    val isAdsReady = _isAdsReady.asStateFlow()

    fun initializeMobileAds(context: Context, onInitComplete: () -> Unit = {}) {
        if (isMobileAdsInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "Google Mobile Ads initialized: $status")
                    _isAdsReady.value = true
                    onInitComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mobile Ads init failed safely", e)
                onInitComplete()
            }
        } else {
            onInitComplete()
        }
    }

    /**
     * Loads App Open ad safely without ever affecting or resetting book states
     */
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null) return

        try {
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context.applicationContext,
                TEST_APP_OPEN_AD_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        loadTime = System.currentTimeMillis()
                        Log.d(TAG, "App Open Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Log.w(TAG, "App Open Ad failed to load: ${error.message}")
                        appOpenAd = null
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "AppOpenAd load error", e)
        }
    }

    /**
     * Shows App Open ad safely. Crucially, onDismissed or failure does NOT recreate Activity
     * or clear ViewModel/Room data.
     */
    fun showAppOpenAdIfAvailable(activity: Activity, onAdFinished: () -> Unit = {}) {
        val ad = appOpenAd
        val isAdStillValid = (System.currentTimeMillis() - loadTime) < 4 * 3600000

        if (ad != null && isAdStillValid && !isShowingAppOpenAd) {
            isShowingAppOpenAd = true
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                    onAdFinished()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                    onAdFinished()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad showing")
                }
            }
            ad.show(activity)
        } else {
            onAdFinished()
            loadAppOpenAd(activity)
        }
    }

    /**
     * Preloads an Interstitial ad safely
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true

        try {
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                TEST_INTERSTITIAL_AD_ID,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                        Log.d(TAG, "Interstitial Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Log.w(TAG, "Interstitial Ad failed to load: ${error.message}")
                        interstitialAd = null
                        isLoadingInterstitial = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "InterstitialAd load error", e)
            isLoadingInterstitial = false
        }
    }

    /**
     * Shows Interstitial ad when opening a novel/chapter if available.
     * Guaranteed never to block the user or book access if ad is not loaded or fails.
     */
    fun showInterstitialAdIfAvailable(activity: Activity, onAdFinished: () -> Unit) {
        val ad = interstitialAd
        if (ad != null && !isShowingInterstitialAd) {
            isShowingInterstitialAd = true
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isShowingInterstitialAd = false
                    loadInterstitialAd(activity)
                    onAdFinished()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    isShowingInterstitialAd = false
                    loadInterstitialAd(activity)
                    onAdFinished()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad showing")
                }
            }
            ad.show(activity)
        } else {
            // Not ready or already showing -> immediately proceed to read the book without blocking
            onAdFinished()
            loadInterstitialAd(activity)
        }
    }
}
