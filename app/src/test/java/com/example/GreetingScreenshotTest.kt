package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Quote
import com.example.ui.components.QuoteCard
import com.example.ui.theme.CharlesDickensTheme
import com.example.ui.viewmodel.AppLanguage
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quote_card_screenshot() {
        val sampleQuote = Quote(
            id = "test_q",
            quoteAr = "كان أفضل الأوقات، وكان أسوأ الأوقات.",
            quoteEn = "It was the best of times, it was the worst of times.",
            sourceNovelAr = "قصة مدينتين",
            sourceNovelEn = "A Tale of Two Cities"
        )

        composeTestRule.setContent {
            CharlesDickensTheme {
                QuoteCard(
                    quote = sampleQuote,
                    currentLanguage = AppLanguage.ARABIC,
                    onRefreshQuote = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
