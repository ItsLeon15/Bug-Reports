/*
package com.leon.bugreport;

import java.sql.Connection;

import static com.leon.bugreport.BugReportDatabase.dataSource;
import static com.leon.bugreport.BugReportManager.plugin;

public class DatabaseConnectionChecker implements Runnable {
	private static BugReportDatabase bugReportDatabase;

	static {
		try {
			bugReportDatabase = new BugReportDatabase();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final long checkIntervalMillis;

	public DatabaseConnectionChecker(BugReportDatabase bugReportDatabase, long checkIntervalMillis) {
		DatabaseConnectionChecker.bugReportDatabase = bugReportDatabase;
		this.checkIntervalMillis = checkIntervalMillis;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(checkIntervalMillis);
				checkConnection();
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	public static void checkConnection() throws Exception {
		Connection connection = dataSource.getConnection();

		if (connection != null) {
			try {
				if (connection.isClosed()) {
					bugReportDatabase.connectLocalOrRemote(true);
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to check database connection");
			}
		}
	}
}
*/
