package com.werebug.anmapwrapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NmapCommandBuilderTest {

  private val nmapPath = "/data/data/com.werebug.anmapwrapper/files/nmap"
  private val dataDir = "/data/data/com.werebug.anmapwrapper/files"
  private val xmlOutput = "/data/data/com.werebug.anmapwrapper/files/output.xml"

  private fun builder(
    xmlOutputPath: String? = null,
    defaultDnsServers: List<String> = emptyList(),
  ) = NmapCommandBuilder(
    nmapExecutablePath = nmapPath,
    dataDirPath = dataDir,
    xmlOutputPath = xmlOutputPath,
    defaultDnsServers = defaultDnsServers,
  )

  private fun argvOf(result: NmapCommandBuilder.Result): List<String> {
    assertTrue("expected a success, got $result", result is NmapCommandBuilder.Result.Success)
    return (result as NmapCommandBuilder.Result.Success).argv
  }

  private fun errorOf(result: NmapCommandBuilder.Result): NmapCommandBuilder.Result.Error {
    assertTrue("expected an error, got $result", result is NmapCommandBuilder.Result.Error)
    return result as NmapCommandBuilder.Result.Error
  }

  @Test
  fun `nmap is replaced by the executable path and datadir is appended`() {
    val argv = argvOf(builder().build("nmap -sS 192.168.1.1"))

    assertEquals(listOf(nmapPath, "-sS", "192.168.1.1", "--datadir", dataDir), argv)
  }

  @Test
  fun `surrounding whitespace is ignored`() {
    val argv = argvOf(builder().build("   nmap 192.168.1.1  \n"))

    assertEquals(listOf(nmapPath, "192.168.1.1", "--datadir", dataDir), argv)
  }

  @Test
  fun `runs of whitespace between arguments collapse to a single separator`() {
    val argv = argvOf(builder().build("nmap   -sV\t-p  22,80    scanme.nmap.org"))

    assertEquals(
      listOf(nmapPath, "-sV", "-p", "22,80", "scanme.nmap.org", "--datadir", dataDir),
      argv
    )
  }

  @Test
  fun `sudo in first position becomes su -c`() {
    val argv = argvOf(builder().build("sudo nmap -sS 192.168.1.1"))

    assertEquals(listOf("su", "-c", nmapPath, "-sS", "192.168.1.1", "--datadir", dataDir), argv)
  }

  @Test
  fun `sudo anywhere but the first position is rejected`() {
    val error = errorOf(builder().build("nmap sudo -sS 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.INVALID_SUDO_SYNTAX, error.kind)
  }

  @Test
  fun `a command that does not start with nmap is rejected`() {
    val error = errorOf(builder().build("ls -la"))

    assertEquals(NmapCommandBuilder.ErrorKind.INVALID_NMAP_SYNTAX, error.kind)
  }

  @Test
  fun `nmap after the first argument is rejected`() {
    val error = errorOf(builder().build("-sS nmap 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.INVALID_NMAP_SYNTAX, error.kind)
  }

  @Test
  fun `sudo must be followed immediately by nmap`() {
    val error = errorOf(builder().build("sudo -sS nmap 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.INVALID_NMAP_SYNTAX, error.kind)
  }

  @Test
  fun `an empty command is rejected`() {
    val error = errorOf(builder().build("   "))

    assertEquals(NmapCommandBuilder.ErrorKind.INVALID_NMAP_SYNTAX, error.kind)
  }

  @Test
  fun `datadir is reserved`() {
    val error = errorOf(builder().build("nmap --datadir /sdcard 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.RESERVED_FLAG, error.kind)
    assertEquals("--datadir", error.flag)
  }

  @Test
  fun `datadir is reserved in its equals form too`() {
    val error = errorOf(builder().build("nmap --datadir=/sdcard 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.RESERVED_FLAG, error.kind)
    assertEquals("--datadir", error.flag)
  }

  @Test
  fun `xml output is appended when the parser is enabled`() {
    val argv = argvOf(builder(xmlOutputPath = xmlOutput).build("nmap 192.168.1.1"))

    assertEquals(
      listOf(nmapPath, "192.168.1.1", "--datadir", dataDir, "-oX", xmlOutput),
      argv
    )
  }

  @Test
  fun `oX is reserved when the parser is enabled`() {
    val error = errorOf(builder(xmlOutputPath = xmlOutput).build("nmap -oX out.xml 192.168.1.1"))

    assertEquals(NmapCommandBuilder.ErrorKind.RESERVED_FLAG, error.kind)
    assertEquals("-oX", error.flag)
  }

  @Test
  fun `oX is allowed when the parser is disabled`() {
    val argv = argvOf(builder().build("nmap -oX /sdcard/out.xml 192.168.1.1"))

    assertEquals(
      listOf(nmapPath, "-oX", "/sdcard/out.xml", "192.168.1.1", "--datadir", dataDir),
      argv
    )
  }

  @Test
  fun `the default dns servers are appended when the user did not set any`() {
    val argv = argvOf(
      builder(defaultDnsServers = listOf("1.1.1.1", "8.8.8.8")).build("nmap 192.168.1.1")
    )

    assertEquals(
      listOf(nmapPath, "192.168.1.1", "--datadir", dataDir, "--dns-servers", "1.1.1.1,8.8.8.8"),
      argv
    )
  }

  @Test
  fun `a user supplied dns-servers flag wins over the default`() {
    val argv = argvOf(
      builder(defaultDnsServers = listOf("1.1.1.1")).build("nmap --dns-servers 9.9.9.9 host")
    )

    assertEquals(
      listOf(nmapPath, "--dns-servers", "9.9.9.9", "host", "--datadir", dataDir),
      argv
    )
  }

  @Test
  fun `a user supplied dns-servers flag wins in its equals form too`() {
    val argv = argvOf(
      builder(defaultDnsServers = listOf("1.1.1.1")).build("nmap --dns-servers=9.9.9.9 host")
    )

    assertEquals(
      listOf(nmapPath, "--dns-servers=9.9.9.9", "host", "--datadir", dataDir),
      argv
    )
  }

  @Test
  fun `no dns-servers flag is added when there is no default`() {
    val argv = argvOf(builder(defaultDnsServers = emptyList()).build("nmap 192.168.1.1"))

    assertEquals(listOf(nmapPath, "192.168.1.1", "--datadir", dataDir), argv)
  }

  @Test
  fun `every patch applies to a sudo command as well`() {
    val argv = argvOf(
      builder(xmlOutputPath = xmlOutput, defaultDnsServers = listOf("1.1.1.1"))
        .build("sudo nmap -sS 10.0.0.0/24")
    )

    assertEquals(
      listOf(
        "su", "-c", nmapPath, "-sS", "10.0.0.0/24",
        "--datadir", dataDir,
        "--dns-servers", "1.1.1.1",
        "-oX", xmlOutput
      ),
      argv
    )
  }
}
