package com.example.azamar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutUser: LinearLayout = view.findViewById(R.id.layout_user)
        val textUser: TextView = view.findViewById(R.id.text_user)
        val layoutBot: LinearLayout = view.findViewById(R.id.layout_bot)
        val textBot: TextView = view.findViewById(R.id.text_bot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutBot.visibility = View.GONE
            holder.textUser.text = message.text
        } else {
            holder.layoutUser.visibility = View.GONE
            holder.layoutBot.visibility = View.VISIBLE
            holder.textBot.text = message.text
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}