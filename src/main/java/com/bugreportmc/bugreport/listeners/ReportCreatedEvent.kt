package com.bugreportmc.bugreport.listeners

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ReportCreatedEvent(val report: String) : Event() {
	override fun getHandlers(): HandlerList {
		return handlerList
	}

	companion object {
		private val handlerList = HandlerList()

		@JvmStatic
		fun getHandlerList(): HandlerList {
			return handlerList
		}
	}
}
