package com.bugreportmc.bugreport.extensions

import com.djrapitops.plan.capability.CapabilityService
import com.djrapitops.plan.extension.Caller
import com.djrapitops.plan.extension.ExtensionService
import java.util.*

class PlanHook private constructor() {
	private var caller: Optional<Caller>

	init {
		caller = Optional.empty()
	}

	fun updateHook(playerId: UUID?, playerName: String?) {
		caller.ifPresent { c: Caller ->
			c.updateServerData()
			c.updatePlayerData(playerId, playerName)
		}
	}

	fun hookIntoPlan() {
		if (!areAllCapabilitiesAvailable()) return
		registerDataExtension()
		listenForPlanReloads()
	}

	private fun areAllCapabilitiesAvailable(): Boolean {
		val capabilities = CapabilityService.getInstance()
		return capabilities.hasCapability("DATA_EXTENSION_VALUES")
	}

	private fun registerDataExtension() {
		try {
			caller = ExtensionService.getInstance().register(BugReportExtension())
		} catch (_: IllegalStateException) {

		} catch (_: IllegalArgumentException) {
		}
	}

	private fun listenForPlanReloads() {
		CapabilityService.getInstance().registerEnableListener { isPlanEnabled: Boolean ->
			if (isPlanEnabled) registerDataExtension()
		}
	}

	companion object {
		@get:Synchronized
		var instance: PlanHook? = null
			get() {
				if (field == null) {
					field = PlanHook()
				}
				return field
			}
			private set
	}
}
