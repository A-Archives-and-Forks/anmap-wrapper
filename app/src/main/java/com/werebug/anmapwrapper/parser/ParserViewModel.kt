package com.werebug.anmapwrapper.parser

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.werebug.anmapwrapper.MainActivity
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors

class ParserViewModel(app: Application) : AndroidViewModel(app) {

  /** The three terminal states of a parse attempt, as rendered by [ParserActivity]. */
  sealed interface ParseResult {
    data class Parsed(val hosts: List<Host>) : ParseResult
    data object OutputUnavailable : ParseResult
    data object ParseFailed : ParseResult
  }

  private val executor = Executors.newSingleThreadExecutor()

  private val _parseResult = MutableLiveData<ParseResult>()
  val parseResult: LiveData<ParseResult> = _parseResult

  init {
    executor.execute { _parseResult.postValue(parse()) }
  }

  private fun parse(): ParseResult {
    val outputFile = File(getApplication<Application>().filesDir, MainActivity.XML_OUTPUT_FILE)
    return try {
      FileInputStream(outputFile).use { ParseResult.Parsed(XMLOutputParser().parse(it)) }
    } catch (e: IOException) {
      // The output is deleted on Clear and is absent altogether when the scan ran
      // without -oX, so a stale back-stack entry can land here with no file.
      Log.e(MainActivity.LOG_TAG, "Cannot read the XML scan output.", e)
      ParseResult.OutputUnavailable
    } catch (e: XmlOutputParseException) {
      Log.e(MainActivity.LOG_TAG, "Cannot parse the XML scan output.", e)
      ParseResult.ParseFailed
    }
  }

  override fun onCleared() {
    super.onCleared()
    executor.shutdown()
  }
}
