package org.cryptomator.presentation.licensing

import android.app.Activity
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class LicenseEnforcerTest {

	private val sharedPreferencesHandler: SharedPreferencesHandler = mock()
	private lateinit var licenseEnforcer: LicenseEnforcer

	@BeforeEach
	fun setUp() {
		licenseEnforcer = LicenseEnforcer(sharedPreferencesHandler)
	}

	// -- hasWriteAccess --

	@Test
	fun `hasWriteAccess returns true when license token is present`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true when subscription is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(true)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true when trial is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true even when trial is expired`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true even when no license and no trial and no subscription`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	// -- hasWriteAccessForVault --

	@Test
	fun `hasWriteAccessForVault returns true for non-hub vault with write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `hasWriteAccessForVault returns true for non-hub vault even without write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `hasWriteAccessForVault returns true for hub vault with paid license`() {
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(true)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `hasWriteAccessForVault returns true for hub vault without paid license and no local license`() {
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `hasWriteAccessForVault returns true for hub vault without paid license but with local license`() {
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `hasWriteAccessForVault returns true for hub vault without paid license but with active trial`() {
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
	}

	@Test
	fun `ensureWriteAccessForVault returns true for hub vault without paid license but with local license`() {
		val activity: Activity = mock()
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.ensureWriteAccessForVault(activity, vault, LicenseEnforcer.LockedAction.UPLOAD_FILES))
	}

	@Test
	fun `hasWriteAccessForVault returns true when vault is null and has write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(null))
	}

	@Test
	fun `hasWriteAccessForVault returns true when vault is null even without explicit write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(null))
	}

	// -- ensureWriteAccessForVault --

	@Test
	fun `ensureWriteAccessForVault returns true for hub vault with paid license`() {
		val activity: Activity = mock()
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(true)

		assertTrue(licenseEnforcer.ensureWriteAccessForVault(activity, vault, LicenseEnforcer.LockedAction.UPLOAD_FILES))
	}

	@Test
	fun `ensureWriteAccessForVault returns true for hub vault without paid license and no local license`() {
		val activity: Activity = mock()
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.ensureWriteAccessForVault(activity, vault, LicenseEnforcer.LockedAction.UPLOAD_FILES))
	}

	// -- hasPaidLicense --

	@Test
	fun `hasPaidLicense returns true when license token is present`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns true when subscription is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(true)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns true always (app is free)`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns true even when no license and no subscription`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	// -- hasActiveTrial --

	@Test
	fun `hasActiveTrial returns true when trial expiration is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertTrue(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when trial expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when no trial was started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	// -- startTrial --

	@Test
	fun `startTrial sets trial expiration date 30 days in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		val before = System.currentTimeMillis()
		licenseEnforcer.startTrial()
		val after = System.currentTimeMillis()

		val captor = ArgumentCaptor.forClass(Long::class.java)
		verify(sharedPreferencesHandler).setTrialExpirationDate(captor.capture())

		val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
		assertTrue(captor.value >= before + thirtyDaysMs)
		assertTrue(captor.value <= after + thirtyDaysMs)
	}

	@Test
	fun `startTrial does not overwrite existing active trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpirationDate(anyLong())
	}

	@Test
	fun `startTrial does not overwrite expired trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpirationDate(anyLong())
	}

	// -- evaluateTrialState --

	@Test
	fun `evaluateTrialState returns active with formatted date when trial is active`() {
		val futureDate = System.currentTimeMillis() + 86400000L
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(futureDate)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertTrue(state.isActive)
		assertFalse(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns expired with formatted date when trial is expired`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns inactive and not expired when no trial started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertFalse(state.isExpired)
		assertNull(state.formattedExpirationDate)
	}

	// -- sticky trial expiry --

	@Test
	fun `hasActiveTrial latches isTrialExpired when expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
		verify(sharedPreferencesHandler).setTrialExpired(true)
	}

	@Test
	fun `hasActiveTrial returns false when isTrialExpired is already true even if date is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		assertFalse(licenseEnforcer.hasActiveTrial())
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `evaluateTrialState latches isTrialExpired and returns expired when date is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		verify(sharedPreferencesHandler).setTrialExpired(true)
	}

	@Test
	fun `evaluateTrialState returns expired when isTrialExpired is already true even if date is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `evaluateTrialState is idempotent once sticky flag is set`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		licenseEnforcer.hasActiveTrial()
		licenseEnforcer.evaluateTrialState()

		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `startTrial does not reset sticky trialExpired flag`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `fresh install with no trial and no flag behaves unchanged`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
		val state = licenseEnforcer.evaluateTrialState()
		assertFalse(state.isActive)
		assertFalse(state.isExpired)
		assertNull(state.formattedExpirationDate)
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	// -- evaluateUiState --

	@Test
	fun `evaluateUiState returns hasPaidLicense true when trial is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertTrue(uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isActive)
		assertNotNull(uiState.trialState.formattedExpirationDate)
	}

	@Test
	fun `evaluateUiState returns hasPaidLicense true when trial is expired`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertTrue(uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isExpired)
		assertNotNull(uiState.trialState.formattedExpirationDate)
	}

	@Test
	fun `evaluateUiState returns hasPaidLicense true when no trial started`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertTrue(uiState.hasPaidLicense)
		assertFalse(uiState.trialState.isActive)
		assertFalse(uiState.trialState.isExpired)
		assertNull(uiState.trialState.formattedExpirationDate)
	}
}
