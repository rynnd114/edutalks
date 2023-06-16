package com.kelompokcool.bangkitcapstone

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import kotlinx.android.synthetic.main.activity_students.*
import kotlinx.android.synthetic.main.item_reviews.view.*
import kotlinx.android.synthetic.main.toolbar_main.*
import java.text.SimpleDateFormat
import java.util.*

class StudentsActivity : AppCompatActivity() {

    private lateinit var adapter: FollowingAdapter
    private var pageBack: String = ""
    private var button: String = ""
    private var following: ArrayList<String> = ArrayList()
    private var followers: ArrayList<String> = ArrayList()
    private var users: ArrayList<String> = ArrayList()
    private var isFollowing: Boolean = false
    private lateinit var layout: RelativeLayout
    private var userID: String = ""

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)

        pageBack = intent.getStringExtra("pageBack").toString()
        button = intent.getStringExtra("button").toString()
        userID = intent.getStringExtra("userID").toString()

        adapter = FollowingAdapter(users)
        followingsRecyclerView.layoutManager = LinearLayoutManager(this)
        followingsRecyclerView.adapter = adapter

        if (userID == "null") {
            getUserData(authDb.currentUser!!.uid)
        } else {
            getUserData(userID)
        }

        layout = findViewById(R.id.following_linearLayout)
        layout.setOnTouchListener(object : OnSwipeTouchListener(this@StudentsActivity) {
            @SuppressLint("ClickableViewAccessibility")
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchUserList()
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchUserList()
            }
        })

        // Set toolbar
        setSupportActionBar(topToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar_back, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_back -> {
                when (pageBack) {
                    "home" -> startActivity(Intent(applicationContext, HomeActivity::class.java))
                    "personal" -> startActivity(Intent(applicationContext, PersonalActivity::class.java))
                    "personalUserSide" -> {
                        val intent = Intent(this@StudentsActivity, PersonalUserSideActivity::class.java)
                        intent.putExtra("userID", userID)
                        startActivity(intent)
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUserData(userId: String) {
        rootDB.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    following = document.get("following") as ArrayList<String>
                    followers = document.get("followers") as ArrayList<String>

                    if (button == "following") {
                        TextView_followingList.text = "FOLLOWING's"
                        users = following
                    } else {
                        TextView_followingList.text = "FOLLOWERS's"
                        users = followers
                        isFollowing = true
                    }

                    adapter.setUsers(users)
                }
            }
    }

    private fun switchUserList() {
        if (!isFollowing) {
            TextView_followingList.text = "FOLLOWER's"
            users = followers
            isFollowing = true
        } else {
            TextView_followingList.text = "FOLLOWING's"
            users = following
            isFollowing = false
        }
        adapter.setUsers(users)
    }

    private inner class FollowingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class FollowingAdapter(private var users: ArrayList<String>) :
        RecyclerView.Adapter<FollowingViewHolder>() {

        fun setUsers(users: ArrayList<String>) {
            this.users = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
            return FollowingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_reviews, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return users.size
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
            val user = users[position]

            rootDB.collection("users").document(user).get().addOnSuccessListener { document ->
                if (document != null) {
                    val userName = document.get("userName") as String
                    holder.itemView.TextView_user.text = userName

                    if (userID == "null") {
                        if (!isFollowing) {
                            holder.itemView.Button_follow.text = "Unfollow"
                            holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_unfollow)
                            holder.itemView.Button_follow.setTextColor(resources.getColor(R.color.red))
                        } else {
                            holder.itemView.Button_follow.text = "Block"
                            holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_block)
                        }
                    } else {
                        rootDB.collection("users").document(authDb.currentUser!!.uid).get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument != null) {
                                    val followingList = userDocument.get("following") as ArrayList<String>
                                    if (followingList.contains(user)) {
                                        holder.itemView.Button_follow.text = "Unfollow"
                                        holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_unfollow)
                                        holder.itemView.Button_follow.setTextColor(resources.getColor(R.color.red))
                                    } else {
                                        holder.itemView.Button_follow.text = "Follow"
                                        holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_follow)
                                    }
                                }
                            }
                    }
                }
            }

            holder.itemView.Button_follow.setOnClickListener {
                if (holder.itemView.Button_follow.text == "Unfollow") {
                    rootDB.collection("users").document(user).get().addOnSuccessListener { document ->
                        if (document != null) {
                            val userId = document.id
                            rootDB.collection("users").document(authDb.currentUser!!.uid)
                                .update("following", FieldValue.arrayRemove(userId))
                            rootDB.collection("users").document(user)
                                .update("followers", FieldValue.arrayRemove(authDb.currentUser!!.uid))
                            rootDB.collection("posts").whereEqualTo("userId", user)
                                .get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        task.result?.forEach { doc ->
                                            doc.reference.update("sharedWithUsers", FieldValue.arrayRemove(authDb.currentUser!!.uid))
                                        }

                                        val intent = Intent(applicationContext, StudentsActivity::class.java)
                                        intent.putExtra("button", button)
                                        intent.putExtra("pageBack", pageBack)
                                        if (userID != "null") {
                                            intent.putExtra("userID", userID)
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                        }
                    }
                } else if (holder.itemView.Button_follow.text == "Follow") {
                    rootDB.collection("users").document(user).get().addOnSuccessListener { document ->
                        if (document != null) {
                            val userId = document.id
                            rootDB.collection("users").document(authDb.currentUser!!.uid)
                                .update("following", FieldValue.arrayUnion(userId))
                            rootDB.collection("users").document(user)
                                .update("followers", FieldValue.arrayUnion(authDb.currentUser!!.uid))
                            rootDB.collection("posts").whereEqualTo("userId", user)
                                .get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        task.result?.forEach { doc ->
                                            doc.reference.update("sharedWithUsers", FieldValue.arrayUnion(authDb.currentUser!!.uid))
                                        }

                                        val intent = Intent(applicationContext, StudentsActivity::class.java)
                                        intent.putExtra("button", button)
                                        intent.putExtra("pageBack", pageBack)
                                        if (userID != "null") {
                                            intent.putExtra("userID", userID)
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                        }
                    }
                }
            }

            holder.itemView.TextView_user.setOnClickListener {
                if (user == authDb.currentUser!!.uid) {
                    startActivity(Intent(applicationContext, PersonalActivity::class.java))
                    finish()
                } else {
                    val intent = Intent(applicationContext, PersonalUserSideActivity::class.java)
                    intent.putExtra("userID", user)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTime(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
        return sdf.format(Date())
    }
}
