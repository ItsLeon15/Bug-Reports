package com.leon.bugreport.extensions;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.extension.ExtensionService;

import java.util.Optional;
import java.util.UUID;

public class PlanHook {
	private static PlanHook instance; // Singleton instance
	private Optional<Caller> caller;

	// Private constructor to prevent instantiation
	private PlanHook() {
		caller = Optional.empty();
	}

	// Public method to get the instance
	public static synchronized PlanHook getInstance() {
		if (instance == null) {
			instance = new PlanHook();
		}
		return instance;
	}

	public void updateHook(UUID PlayerID, String PlayerName) {
		caller.ifPresent(c -> {
			c.updateServerData();
			c.updatePlayerData(PlayerID, PlayerName);
		});
	}

	public void hookIntoPlan() {
		if (!areAllCapabilitiesAvailable()) return;
		registerDataExtension();
		listenForPlanReloads();
	}

	private boolean areAllCapabilitiesAvailable() {
		CapabilityService capabilities = CapabilityService.getInstance();
		return capabilities.hasCapability("DATA_EXTENSION_VALUES");
	}

	private void registerDataExtension() {
		try {
			caller = ExtensionService.getInstance().register(new BugReportExtension());
		} catch (IllegalStateException | IllegalArgumentException e) {
			// Plan is not enabled or the DataExtension implementation has an error, handle exception
		}
	}

	private void listenForPlanReloads() {
		CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
			if (isPlanEnabled) registerDataExtension();
		});
	}
}