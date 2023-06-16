package com.kelompokcool.bangkitcapstone

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Direction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bangkit.mento.R
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import com.kelompokcool.entity.Notifications
import kotlinx.android.synthetic.main.activity_notification.*
import kotlinx.android.synthetic.main.item_notifications.view.*
import kotlinx.android.synthetic.main.toolbar_main.*
import java.text.SimpleDateFormat
import java.util.*

class Notificat/ionActivity : AppCompatActivity() {

    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val notificationsQuery = rootDB.collection("notifications")
            .orderBy("following")
            .orderBy("time", DownloadManager.Query.Direction.DESCENDING)
            .whereEqualTo("userId", authDb.currentUser!!.uid)
            .whereNotEqualTo("following", true)

        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        val options =
            FirestoreRecyclerOptions.Builder<Notifications>()
                .setQuery(notificationsQuery, Notifications::class.java)
                .build()
        adapter = NotificationsAdapter(options)
        notificationRecyclerView.adapter = adapter

        setSupportActionBar(topToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar_notification, menu)
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
            R.id.action_home -> {
                startActivity(Intent(applicationContext, HomeActivity::class.java))
                return true
            }
            R.id.action_message -> {
                startActivity(Intent(applicationContext, HomeChatActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    private fun logout() {
        authDb.signOut()
        finish()
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
    }

    private inner class NotificationsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class NotificationsAdapter(options: FirestoreRecyclerOptions<Notifications>) :
        FirestoreRecyclerAdapter<Notifications, NotificationsViewHolder>(options) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notifications, parent, false)
            return NotificationsViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: NotificationsViewHolder,
            position: Int,
            model: Notifications
        ) {
            rootDB.collection("users")
                .whereEqualTo("id", model.followerRequestId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userNotification = task.result
                        if (userNotification != null) {
                            task.result?.forEach { doc ->
                                val userName: String = doc.get("userName").toString()
                                holder.itemView.username.text = userName

                                holder.itemView.btnApprove.setOnClickListener {
                                    // Handle approve action
                                    rootDB.collection("users")
                                        .document(authDb.currentUser!!.uid)
                                        .update("followers", FieldValue.arrayUnion(model.followerRequestId))

                                    rootDB.collection("users")
                                        .document(model.followerRequestId.toString())
                                        .update("following", FieldValue.arrayUnion(authDb.currentUser!!.uid))

                                    rootDB.collection("posts")
                                        .whereEqualTo("userId", authDb.currentUser!!.uid)
                                        .get()
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                task.result?.forEach { doc ->
                                                    doc.reference.update(
                                                        "sharedWithUsers",
                                                        FieldValue.arrayUnion(model.followerRequestId.toString())
                                                    )
                                                }
                                            }
                                        }

                                    rootDB.collection("notifications")
                                        .document(model.id.toString())
                                        .delete()
                                        .addOnSuccessListener {
                                            val intent =
                                                Intent(applicationContext, NotificationActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                applicationContext,
                                                "Error. Could not reject",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }

                                holder.itemView.btnReject.setOnClickListener {
                                    // Handle reject action
                                    rootDB.collection("notifications")
                                        .document(model.id.toString())
                                        .delete()
                                        .addOnSuccessListener {
                                            val intent =
                                                Intent(applicationContext, NotificationActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                applicationContext,
                                                "Error. Could not reject",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
        return sdf.format(Date())
    }
}
