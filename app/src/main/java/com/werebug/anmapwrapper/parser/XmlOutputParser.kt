package com.werebug.anmapwrapper.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/** Raised when the document could not be read to the end. */
class XmlOutputParseException(cause: Throwable) : Exception(cause)

class XMLOutputParser {

  fun parse(inputStream: InputStream): List<Host> {
    val hosts = mutableListOf<Host>()
    var hostnames = mutableSetOf<String>()
    var ipAddress: String? = null
    var ipVersion = IpVersion.V4
    var macAddress: String? = null
    var services = mutableListOf<Service>()

    fun parsePortTag(parser: XmlPullParser) {
      val portId = parser.getAttributeValue(null, "portid")?.toIntOrNull()
      var portState = ""
      var serviceName = ""
      var serviceProduct = ""
      var serviceVersion = ""
      while (parser.next() != XmlPullParser.END_TAG || parser.name != "port") {
        if (parser.eventType == XmlPullParser.START_TAG) {
          when (parser.name) {
            "service" -> {
              serviceName = parser.getAttributeValue(null, "name") ?: ""
              serviceProduct = parser.getAttributeValue(null, "product") ?: ""
              serviceVersion = parser.getAttributeValue(null, "version") ?: ""
            }

            "state" -> {
              portState = parser.getAttributeValue(null, "state") ?: ""
            }
          }
        }
      }
      portId?.let {
        services.add(
          Service(
            port = it,
            state = portState,
            name = serviceName,
            product = serviceProduct,
            version = serviceVersion
          )
        )
      }
    }

    fun parseAddressTag(parser: XmlPullParser) {
      val type = parser.getAttributeValue(null, "addrtype")
      when (type) {
        "ipv4" -> {
          ipAddress = parser.getAttributeValue(null, "addr")
          ipVersion = IpVersion.V4
        }

        "ipv6" -> {
          ipAddress = parser.getAttributeValue(null, "addr")
          ipVersion = IpVersion.V6
        }

        "mac" -> {
          macAddress = parser.getAttributeValue(null, "addr")
        }
      }
    }

    fun parseHostTag(parser: XmlPullParser) {
      while (parser.next() != XmlPullParser.END_TAG || parser.name != "host") {
        if (parser.eventType == XmlPullParser.START_TAG) {
          when (parser.name) {
            "hostname" -> {
              parser.getAttributeValue(null, "name")?.let { hostnames.add(it) }
            }

            "address" -> {
              parseAddressTag(parser)
            }

            "port" -> {
              parsePortTag(parser)
            }
          }
        }
      }
      ipAddress?.let { ip ->
        if (hostnames.isEmpty()) {
          hostnames.add("N/A")
        }
        hosts.add(
          Host(
            hostnames = hostnames,
            ipAddress = ip,
            ipVersion = ipVersion,
            macAddress = macAddress,
            services = services
          )
        )
      }
      services = mutableListOf()
      hostnames = mutableSetOf()
      ipAddress = null
      ipVersion = IpVersion.V4
      macAddress = null
    }

    try {
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(inputStream, null)

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "host") {
          parseHostTag(parser)
        }
        eventType = parser.next()
      }
    } catch (e: Exception) {
      // Whatever was accumulated so far is deliberately discarded: a document that
      // stops mid-way (the truncated output a stopped scan leaves behind) would
      // otherwise be indistinguishable from a complete one.
      throw XmlOutputParseException(e)
    }
    return hosts
  }
}