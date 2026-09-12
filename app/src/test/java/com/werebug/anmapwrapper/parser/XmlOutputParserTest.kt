package com.werebug.anmapwrapper.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.InputStream

class XmlOutputParserTest {

  private fun fixture(name: String): InputStream =
    checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }

  private fun parse(name: String): List<Host> = XMLOutputParser().parse(fixture(name))

  @Test
  fun `a plain host scan yields one host with its ports`() {
    val hosts = parse("plain_host_scan.xml")

    assertEquals(1, hosts.size)
    val host = hosts[0]
    assertEquals("45.33.32.156", host.ipAddress)
    assertEquals(IpVersion.V4, host.ipVersion)
    assertNull(host.macAddress)
    assertEquals(setOf("scanme.nmap.org"), host.hostnames)
    assertEquals(
      listOf(
        Service(22, "open", "ssh", "", ""),
        Service(80, "open", "http", "", ""),
        Service(9929, "closed", "nping-echo", "", "")
      ),
      host.services
    )
  }

  @Test
  fun `a version scan carries the product and version of each service`() {
    val hosts = parse("version_scan.xml")

    assertEquals(1, hosts.size)
    assertEquals(
      listOf(
        Service(22, "open", "ssh", "OpenSSH", "6.6.1p1 Ubuntu 2ubuntu2.13"),
        Service(80, "open", "http", "Apache httpd", "2.4.7"),
        Service(443, "filtered", "https", "", "")
      ),
      hosts[0].services
    )
  }

  @Test
  fun `a LAN sweep keeps the MAC address of every host`() {
    val hosts = parse("lan_sweep.xml")

    assertEquals(2, hosts.size)

    val router = hosts[0]
    assertEquals("192.168.1.1", router.ipAddress)
    assertEquals("AA:BB:CC:DD:EE:01", router.macAddress)
    assertEquals(setOf("fritz.box"), router.hostnames)
    assertEquals(emptyList<Service>(), router.services)

    val pi = hosts[1]
    assertEquals("192.168.1.42", pi.ipAddress)
    assertEquals("AA:BB:CC:DD:EE:42", pi.macAddress)
    assertEquals(listOf(Service(22, "open", "ssh", "", "")), pi.services)
  }

  @Test
  fun `a host without a reverse name falls back to N slash A`() {
    val hosts = parse("lan_sweep.xml")

    assertEquals(setOf("N/A"), hosts[1].hostnames)
  }

  @Test
  fun `an IPv6 host is parsed and flagged as v6`() {
    val hosts = parse("ipv6_scan.xml")

    // The fixture opens with the <hosthint> block nmap -6 emits: it repeats the address
    // and hostname, and must not turn into a second host.
    assertEquals(1, hosts.size)
    val host = hosts[0]
    assertEquals("2600:3c01::f03c:91ff:fe18:bb2f", host.ipAddress)
    assertEquals(IpVersion.V6, host.ipVersion)
    assertEquals(setOf("scanme.nmap.org"), host.hostnames)
    assertEquals(
      listOf(Service(80, "open", "http", "", ""), Service(443, "closed", "https", "", "")),
      host.services
    )
  }

  @Test
  fun `a truncated document fails instead of returning a partial result`() {
    assertThrows(XmlOutputParseException::class.java) { parse("truncated_scan.xml") }
  }

  @Test
  fun `missing attributes do not abort the parse`() {
    val hosts = parse("missing_attributes.xml")

    assertEquals(1, hosts.size)
    val host = hosts[0]
    assertEquals("192.168.1.7", host.ipAddress)
    // The only <hostname> has no name attribute, so it is skipped and the fallback applies.
    assertEquals(setOf("N/A"), host.hostnames)
    // The port with no portid is dropped; the one with no state/name keeps empty strings.
    assertEquals(listOf(Service(80, "", "", "", "")), host.services)
  }

  @Test
  fun `an empty document yields no hosts`() {
    val hosts = XMLOutputParser().parse("<?xml version=\"1.0\"?><nmaprun></nmaprun>".byteInputStream())

    assertEquals(emptyList<Host>(), hosts)
  }
}
