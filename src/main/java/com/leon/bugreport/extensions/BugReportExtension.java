package com.leon.bugreport.extensions;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;

import java.util.UUID;

import static com.leon.bugreport.BugReportDatabase.*;

@PluginInfo(
	name = "BugReportPlugin",
	iconName = "bug",
	iconFamily = Family.REGULAR,
	color = Color.YELLOW
)
public class BugReportExtension implements DataExtension {
	@Override
	public CallEvents[] callExtensionMethodsOn() {
		return new CallEvents[]{
				CallEvents.PLAYER_JOIN,
				CallEvents.PLAYER_LEAVE,
				CallEvents.SERVER_EXTENSION_REGISTER,
				CallEvents.SERVER_PERIODICAL,
				CallEvents.MANUAL
		};
	}

	//	User Table only!
	@NumberProvider(
			text = "Bug Reports",
			description = "How many bug reports the player has submitted",
			iconName = "bookmark",
			iconColor = Color.YELLOW,
			priority = 100,
			showInPlayerTable = true
	)
	public long bugReportCount(UUID playerUUID) {
		return loadBugReportCountForPlayer(playerUUID);
	}

	@NumberProvider(
			text = "Archived Bug Reports",
			description = "How many archived bug reports the player has submitted",
			iconName = "box-archive",
			iconColor = Color.ORANGE,
			priority = 90,
			showInPlayerTable = true
	)
	public long archivedBugReportCount(UUID playerUUID) {
		return loadArchivedBugReportCountForPlayer(playerUUID);
	}

	@NumberProvider(
			text = "Non-Archived Bug Reports",
			description = "How many non-archived bug reports the player has submitted",
			iconName = "bookmark",
			iconColor = Color.LIGHT_GREEN,
			priority = 80,
			showInPlayerTable = true
	)
	public long nonArchivedBugReportCount(UUID playerUUID) {
		return loadNonArchivedBugReportCountForPlayer(playerUUID);
	}
}
