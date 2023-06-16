package com.kelompokcool.bangkitcapstone

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.realtimeDB
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import com.kelompokcool.entity.ChatUsers
import kotlinx.android.synthetic.main.activity_home_chat.*
import kotlinx.android.synthetic.main.item_chat_message.view.*
import kotlinx.android.synthetic.main.toolbar_main.*


class HomeChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_chat)
        val chatUsersList = ArrayList<ChatUsers>()

        realtimeDB.reference.child(authDb.currentUser?.uid!!).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result
                if (result != null) {
                    for (snapshot in result.children) {
                        snapshot.key?.let {
                            rootDB.collection("users").document(it).get().addOnCompleteListener { userTask ->
                                if (userTask.isSuccessful) {
                                    val userInfo = userTask.result
                                    if (userInfo != null) {
                                        val lastMessage = snapshot.child("messages").children.last().child("chatValue").value.toString()
                                        val sendAt = snapshot.child("messages").children.last().child("sendAt").value.toString()
                                        chatUsersList.add(ChatUsers(snapshot.key, userInfo.get("userName").toString(), userInfo.get("photoURI").toString(), lastMessage, sendAt))
                                    }
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            }
        }
        adapter = ChatUsersAdapter(chatUsersList)
        allChatRecycler.layoutManager = LinearLayoutManager(this)
        allChatRecycler.adapter = adapter

        setSupportActionBar(topToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar_message, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_logout -> {
                logout()
                return true
            }
            R.id.action_personal -> {
                startActivity(Intent(applicationContext, PersonalActivity::class.java))
                return true
            }
            R.id.action_notification -> {
                startActivity(Intent(applicationContext, NotificationActivity::class.java))
                return true
            }
            R.id.action_home -> {
                startActivity(Intent(applicationContext, HomeActivity::class.java))
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        authDb.signOut()
        finish()
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
    }

    private inner class ChatUsersViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {}

    private inner class ChatUsersAdapter(private var chatUserList: ArrayList<ChatUsers>) : RecyclerView.Adapter<ChatUsersViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUsersViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatUsersViewHolder(view)
        }

        override fun getItemCount(): Int {
            return chatUserList.size
        }

        override fun onBindViewHolder(holder: ChatUsersViewHolder, position: Int) {
            val chatUser = chatUserList[position]
            holder.itemView.chat_user_name.text = chatUser.username
            holder.itemView.latest_chat_message.text = chatUser.lastMessage
            Glide.with(this@HomeChatActivity).load(chatUser.photoURI).into(holder.itemView.chat_user_image)
            holder.itemView.setOnClickListener {
                val intent = Intent(applicationContext, ChatActivity::class.java)
                intent.putExtra("chatReceiverID", chatUser.id)
                startActivity(intent)
            }
        }
    }
}
