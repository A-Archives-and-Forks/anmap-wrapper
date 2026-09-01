package com.werebug.anmapwrapper.parser

enum class IpVersion { V4, V6 }

data class Host(
  val hostnames: Set<String>,
  val ipAddress: String,
  val ipVersion: IpVersion,
  val macAddress: String?,
  val services: List<Service>
)
