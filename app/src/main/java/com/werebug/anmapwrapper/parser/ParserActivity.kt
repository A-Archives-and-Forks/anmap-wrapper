package com.werebug.anmapwrapper.parser

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.werebug.anmapwrapper.R
import com.werebug.anmapwrapper.databinding.ActivityParserBinding

class ParserActivity : AppCompatActivity() {

  private lateinit var binding: ActivityParserBinding
  private val viewModel: ParserViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityParserBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.hostListRecyclerView.layoutManager = LinearLayoutManager(this)

    viewModel.parseResult.observe(this) { result ->
      binding.parserProgressBar.visibility = View.GONE
      when (result) {
        is ParserViewModel.ParseResult.Parsed ->
          if (result.hosts.isEmpty()) {
            showMessage(R.string.parser_no_hosts)
          } else {
            binding.hostListRecyclerView.adapter = HostAdapter(result.hosts)
          }

        ParserViewModel.ParseResult.OutputUnavailable ->
          showMessage(R.string.parser_output_unavailable)

        ParserViewModel.ParseResult.ParseFailed -> showMessage(R.string.parser_parse_failed)
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
