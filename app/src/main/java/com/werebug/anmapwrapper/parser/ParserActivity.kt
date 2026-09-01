package com.werebug.anmapwrapper.parser

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.werebug.anmapwrapper.MainActivity
import com.werebug.anmapwrapper.R
import com.werebug.anmapwrapper.databinding.ActivityParserBinding
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ParserActivity : AppCompatActivity() {

  private lateinit var binding: ActivityParserBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityParserBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.hostListRecyclerView.layoutManager = LinearLayoutManager(this)

    val hosts = try {
      FileInputStream(File(filesDir, MainActivity.XML_OUTPUT_FILE)).use {
        XMLOutputParser().parse(it)
      }
    } catch (e: IOException) {
      // The output is deleted on Clear and is absent altogether when the scan ran
      // without -oX, so a stale back-stack entry can land here with no file.
      Log.e(MainActivity.LOG_TAG, "Cannot read the XML scan output.", e)
      showMessage(R.string.parser_output_unavailable)
      null
    } catch (e: XmlOutputParseException) {
      Log.e(MainActivity.LOG_TAG, "Cannot parse the XML scan output.", e)
      showMessage(R.string.parser_parse_failed)
      null
    }

    if (hosts != null) {
      if (hosts.isEmpty()) {
        showMessage(R.string.parser_no_hosts)
      } else {
        binding.hostListRecyclerView.adapter = HostAdapter(hosts)
      }
    }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  private fun showMessage(@StringRes messageRes: Int) {
    binding.hostListRecyclerView.visibility = View.GONE
    binding.parserMessageTextView.setText(messageRes)
    binding.parserMessageTextView.visibility = View.VISIBLE
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        onBackPressedDispatcher.onBackPressed()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}
