package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.MaintenanceLog
import com.example.data.model.ServiceReminder
import com.example.data.model.Vehicle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GarageRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addVehicle_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = GarageRepository(firestore)

    val vehicle = Vehicle(
      familyMember = "My Car",
      nickname = "Commuter",
      make = "Toyota",
      model = "RAV4",
      year = 2023,
      vin = "JTMDFREV123456789",
      licensePlate = "8XYZ999",
      state = "TX",
      currentMileage = 15000,
      insuranceCompany = "Geico",
      insurancePolicyNumber = "POL-112233",
      insuranceDuration = "6 Months"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addVehicle(vehicle) }
    assertTrue(result.isSuccess)
    val vehicleId = result.getOrThrow()
    assertTrue(vehicleId.isNotEmpty())

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getVehicle(vehicleId) }
    assertTrue(fetched.isSuccess)
    val fetchedVehicle = fetched.getOrThrow()
    assertEquals("Toyota", fetchedVehicle.make)
    assertEquals("RAV4", fetchedVehicle.model)
    assertEquals(uid, fetchedVehicle.userId)
  }

  @Test
  fun observeVehicles_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = GarageRepository(firestore)

    val vehicle = Vehicle(
      familyMember = "Wife's Car",
      make = "Honda",
      model = "CR-V",
      year = 2022,
      vin = "7FARW2H86NE111222",
      licensePlate = "6WIFE88",
      insuranceDuration = "1 Year"
    )
    val vehicleId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addVehicle(vehicle).getOrThrow() }

    val vehicles = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeVehicles().first { list -> list.any { it.id == vehicleId } }
    }
    assertTrue(vehicles.any { it.id == vehicleId })
  }

  @Test
  fun getVehicle_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = GarageRepository(firestore)

    val vehicle = Vehicle(
      familyMember = "Kid's Car",
      make = "Mazda",
      model = "3",
      year = 2020,
      vin = "JM1BPACL1L1333444",
      licensePlate = "5KID123",
      insuranceDuration = "6 Months"
    )
    val vehicleId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addVehicle(vehicle).getOrThrow() }

    signInTestUser(BOB_EMAIL)
    val bobRepo = GarageRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getVehicle(vehicleId) }
    assertTrue(result.isFailure)
    val ex = result.exceptionOrNull()
    assertNotNull(ex)
  }

  @Test
  fun observeVehicles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = GarageRepository(firestore)

    var caught = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeVehicles().first() }
    } catch (e: Exception) {
      caught = true
      assertTrue(
        e is IllegalStateException ||
        e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      )
    }
    assertTrue("Expected exception when unauthenticated", caught)
  }

  @Test
  fun addMaintenanceLog_and_observe_worksForOwner() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = GarageRepository(firestore)

    val log = MaintenanceLog(
      vehicleId = "veh_test_1",
      title = "Oil & Filter Change",
      logType = "MAINTENANCE",
      serviceDate = "2024-11-01",
      mileage = 15200,
      cost = 79.99,
      serviceProvider = "Valvoline"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addMaintenanceLog(log) }
    assertTrue(result.isSuccess)
    val logId = result.getOrThrow()
    assertTrue(logId.isNotEmpty())

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeMaintenanceLogs("veh_test_1").first { list -> list.any { it.id == logId } }
    }
    assertTrue(logs.any { it.id == logId })
  }

  @Test
  fun addServiceReminder_and_toggleCompletion_works() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = GarageRepository(firestore)

    val reminder = ServiceReminder(
      vehicleId = "veh_test_1",
      serviceType = "Tire Rotation",
      dueDate = "2025-03-01",
      dueMileage = 20000,
      isCompleted = false,
      priority = "HIGH"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addServiceReminder(reminder) }
    assertTrue(result.isSuccess)
    val reminderId = result.getOrThrow()

    val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.toggleReminderCompletion(reminderId, true)
    }
    assertTrue(toggleResult.isSuccess)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
