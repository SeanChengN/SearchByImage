package io.github.seancheng.searchbyimage.ui

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalLaunchTest {
    @Test
    fun googleLensIntentsCarryShareAndBothImageContractsWithReadGrant() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val imageUri = Uri.parse("content://io.github.seancheng.searchbyimage.debug.fileprovider/cache/search.jpg")
        val intents = buildGoogleLensIntents(
            context,
            imageUri,
            shareActivityName = "com.google.android.apps.search.lens.LensShareEntryPointActivity",
            timestampNanos = 12345L,
        )

        val share = requireNotNull(intents.share)
        assertEquals(Intent.ACTION_SEND, share.action)
        assertEquals("com.google.android.googlequicksearchbox", share.component?.packageName)
        assertEquals(
            "com.google.android.apps.search.lens.LensShareEntryPointActivity",
            share.component?.className,
        )
        assertEquals(imageUri, share.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue(share.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertLensImageContract(intents.direct, "google", imageUri)
        assertLensImageContract(intents.session, "googleapp", imageUri)
        assertEquals("googleapp", intents.picker.data?.scheme)
        assertEquals("lens", intents.picker.data?.authority)
        assertNull(intents.picker.data?.getQueryParameter("LensBitmapUriKey"))
    }

    @Test
    fun googleLensLaunchFallsBackFromShareToDirectToSessionThenPicker() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val imageUri = Uri.parse("content://io.github.seancheng.searchbyimage.debug.fileprovider/cache/search.jpg")
        val intents = buildGoogleLensIntents(
            context,
            imageUri,
            shareActivityName = "LensShareEntryPointActivity",
            timestampNanos = 12345L,
        )
        val attempts = mutableListOf<String>()

        val result = launchGoogleLens(
            intents = intents,
            launchWithImage = { intent ->
                attempts += intent.data?.scheme ?: "share"
                error("not handled")
            },
            launchPicker = { attempts += "picker" },
        )

        assertEquals(GoogleLensLaunchResult.OPENED_FOR_SELECTION, result)
        assertEquals(listOf("share", "google", "googleapp", "picker"), attempts)
    }

    @Test
    fun googleLensLaunchStopsAfterFirstSuccessfulImageContract() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val imageUri = Uri.parse("content://io.github.seancheng.searchbyimage.debug.fileprovider/cache/search.jpg")
        val intents = buildGoogleLensIntents(
            context,
            imageUri,
            shareActivityName = "LensShareEntryPointActivity",
            timestampNanos = 12345L,
        )
        val attempts = mutableListOf<String>()

        val result = launchGoogleLens(
            intents = intents,
            launchWithImage = { attempts += it.data?.scheme ?: "share" },
            launchPicker = { fail("picker must not open after a successful image launch") },
        )

        assertEquals(GoogleLensLaunchResult.OPENED_WITH_IMAGE, result)
        assertEquals(listOf("share"), attempts)
    }

    @Test
    fun lensShareResolverSelectsOnlyLensShareEntryPoint() {
        assertEquals(
            "com.google.android.apps.search.lens.LensShareEntryPointActivity",
            selectGoogleLensShareActivity(
                listOf(
                    "com.google.android.apps.search.ShareActivity",
                    "com.google.android.apps.search.lens.LensShareEntryPointActivity",
                ),
            ),
        )
        assertNull(selectGoogleLensShareActivity(listOf("com.google.android.apps.search.ShareActivity")))
    }

    private fun assertLensImageContract(intent: Intent, scheme: String, imageUri: Uri) {
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.google.android.googlequicksearchbox", intent.`package`)
        assertEquals(scheme, intent.data?.scheme)
        assertEquals("lens", intent.data?.authority)
        assertEquals(imageUri.toString(), intent.data?.getQueryParameter("LensBitmapUriKey"))
        assertEquals("true", intent.data?.getQueryParameter("IncognitoUriKey"))
        assertEquals("12345", intent.data?.getQueryParameter("ActivityLaunchTimestampNanos"))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(imageUri, intent.clipData?.getItemAt(0)?.uri)
    }
}
