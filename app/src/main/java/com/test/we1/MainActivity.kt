package com.test.we1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class MainActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var apiKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("jinxd_prefs", Context.MODE_PRIVATE)
        apiKey = prefs.getString("api_key", null)

        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        val inputField = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<ImageButton>(R.id.sendButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                if (apiKey.isNullOrBlank()) {
                    showApiKeyDialog()
                } else {
                    sendMessage(text)
                    inputField.text.clear()
                }
            }
        }

        settingsButton.setOnClickListener {
            showApiKeyDialog()
        }

        if (apiKey.isNullOrBlank()) {
            showApiKeyDialog()
        }
    }

    private fun showApiKeyDialog() {
        val input = EditText(this)
        input.hint = "sk-or-..."
        input.setText(apiKey)
        AlertDialog.Builder(this)
            .setTitle("Enter OpenRouter API Key")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newKey = input.text.toString().trim()
                if (newKey.isNotEmpty()) {
                    apiKey = newKey
                    getSharedPreferences("jinxd_prefs", Context.MODE_PRIVATE).edit()
                        .putString("api_key", newKey).apply()
                    Toast.makeText(this, "API Key Saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendMessage(text: String) {
        val userMsg = ChatMessage(text, true)
        messages.add(userMsg)
        chatAdapter.notifyItemInserted(messages.size - 1)
        findViewById<RecyclerView>(R.id.chatRecyclerView).scrollToPosition(messages.size - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = callOpenRouter(text)
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(response, false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    findViewById<RecyclerView>(R.id.chatRecyclerView).scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun callOpenRouter(userPrompt: String): String {
        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true

        val jsonBody = JSONObject().apply {
            put("model", "google/gemini-flash-1.5")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        conn.outputStream.use { it.write(jsonBody.toString().toByteArray()) }

        return if (conn.responseCode == 200) {
            val responseString = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else {
            val errorString = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown Error"
            "API Error: $errorString"
        }
    }

    data class ChatMessage(val content: String, val isUser: Boolean)

    inner class ChatAdapter(private val data: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.messageText)
        }

        override fun getItemViewType(position: Int) = if (data[position].isUser) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = if (viewType == 1) R.layout.item_message_user else R.layout.item_message_bot
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = data[position].content
        }

        override fun getItemCount() = data.size
    }
}