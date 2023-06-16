package com.kelompokcool.bangkitcapstone

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import com.kelompokcool.entity.Notifications
import kotlinx.android.synthetic.main.activity_review.*
import kotlinx.android.synthetic.main.item_reviews.view.*
import kotlinx.android.synthetic.main.toolbar_main.*
import java.text.SimpleDateFormat
import java.util.*

class ReviewActivity : AppCompatActivity() {

    private var pageBack: String = ""
    private var userId: String = ""
    private var review: Array<String> = arrayOf("Yummy", "Sweet", "Sour", "Salty", "Bitter", "Like")
    private var position: Int = 0
    private var users: ArrayList<String> = ArrayList()
    private lateinit var layout: RelativeLayout
    private val requestMessageSuccess: String = "Request sent successfully"
    private val requestMessageFail: String = "Error, Request not sent!"

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val postId = intent.getStringExtra("postId")
        pageBack = intent.getStringExtra("pageBack").toString()
        userId = intent.getStringExtra("userId").toString()

        TextView_review.text = review[position].toUpperCase(Locale.ROOT) + "\'s"
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        postId?.let {
            rootDB.collection("posts").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val reviewed = document.get("review") as HashMap<String, ArrayList<String>>
                        reviewed[review[position]]?.let { value ->
                            users = value
                            reviewsRecyclerView.layoutManager = LinearLayoutManager(this@ReviewActivity)
                            reviewsRecyclerView.adapter = ReviewAdapter(users, this@ReviewActivity)
                        }
                    }
                }
        }

        layout = findViewById(R.id.review_linearLayout)
        layout.setOnTouchListener(object : OnSwipeTouchListener(this@ReviewActivity) {
            @SuppressLint("ClickableViewAccessibility")
            override fun onSwipeLeft() {
                super.onSwipeLeft()

                position = (position + 1) % review.size
                TextView_review.text = review[position].toUpperCase(Locale.ROOT) + "\'s"
                postId?.let {
                    rootDB.collection("posts").document(it).get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                val reviewed = document.get("review") as HashMap<String, ArrayList<String>>
                                reviewed[review[position]]?.let { value ->
                                    users = value
                                    reviewsRecyclerView.layoutManager = LinearLayoutManager(this@ReviewActivity)
                                    reviewsRecyclerView.adapter = ReviewAdapter(users, this@ReviewActivity)
                                }
                            }
                        }
                }

            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onSwipeRight() {
                super.onSwipeRight()
                position = (position - 1 + review.size) % review.size
                TextView_review.text = review[position].toUpperCase(Locale.ROOT) + "\'s"
                postId?.let {
                    TextView_review.text = review[position].toUpperCase(Locale.ROOT) + "\'s"
                    rootDB.collection("posts").document(it).get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                val reviewed = document.get("review") as HashMap<String, ArrayList<String>>
                                reviewed[review[position]]?.let { value ->
                                    users = value
                                    reviewsRecyclerView.layoutManager = LinearLayoutManager(this@ReviewActivity)
                                    reviewsRecyclerView.adapter = ReviewAdapter(users, this@ReviewActivity)
                                }
                            }
                        }
                }
            }
        })

        // instantiate toolbar
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
                    "home" -> {
                        startActivity(Intent(applicationContext, HomeActivity::class.java))
                        return true
                    }
                    "personal" -> {
                        startActivity(Intent(applicationContext, PersonalActivity::class.java))
                        return true
                    }
                    "personalUserSide" -> {
                        val intent = Intent(this@ReviewActivity, PersonalUserSideActivity::class.java)
                        intent.putExtra("userID", userId)
                        startActivity(intent)
                        return true
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // create inner classes needed to bind the data to the recyclerview
    private inner class ReviewViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view)

    private inner class ReviewAdapter internal constructor(private val users: List<String>, private val reviewActivity: ReviewActivity) :
        RecyclerView.Adapter<ReviewViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            return ReviewViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_reviews, parent, false))
        }

        override fun getItemCount(): Int {
            return users.size
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat", "ResourceAsColor")
        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            val currentUser = authDb.currentUser

            rootDB.collection("users").document(users[position]).get().addOnSuccessListener { document ->
                if (document != null) {
                    holder.itemView.TextView_user.text = document.getString("userName")
                    val followers = document.get("followers") as? ArrayList<String>
                    if (followers?.contains(currentUser?.uid) == true) {
                        holder.itemView.Button_follow.text = "UnFollow"
                        holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_unfollow)
                        holder.itemView.Button_follow.setTextColor(resources.getColor(R.color.red))
                    } else {
                        holder.itemView.Button_follow.text = "Follow"
                        holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_follow)
                    }
                }
            }
            holder.itemView.Button_follow.setOnClickListener {
                when (holder.itemView.Button_follow.text) {
                    "Follow" -> {
                        rootDB.collection("users").document(users[position]).get().addOnSuccessListener { document ->
                            if (document != null) {
                                val isPrivate = document.getBoolean("private") ?: false
                                if (!isPrivate) {
                                    currentUser?.uid?.let { currentUserId ->
                                        rootDB.collection("users").document(currentUserId).update(
                                            "following", FieldValue.arrayUnion(document.id)
                                        )
                                        rootDB.collection("users").document(document.id).update(
                                            "followers", FieldValue.arrayUnion(currentUserId)
                                        )
                                        rootDB.collection("posts").whereEqualTo("userId", document.id).get()
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    task.result?.forEach { doc ->
                                                        doc.reference.update(
                                                            "sharedWithUsers", FieldValue.arrayUnion(currentUserId)
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                    holder.itemView.Button_follow.text = "UnFollow"
                                    holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_unfollow)
                                    holder.itemView.Button_follow.setTextColor(resources.getColor(R.color.red))
                                } else {
                                    rootDB.collection("notifications").document(currentUser?.uid ?: "").get()
                                        .addOnCompleteListener { user ->
                                            if (user.isSuccessful) {
                                                val userInfo = user.result
                                                if (userInfo != null) {
                                                    try {
                                                        val notification = Notifications().apply {
                                                            userId = document.id
                                                            followerRequestId = currentUser?.uid
                                                            time = getTime()
                                                            isFollowing = false
                                                            id = rootDB.collection("notifications").document().id
                                                        }
                                                        rootDB.collection("notifications").document(notification.id!!)
                                                            .set(notification)
                                                        Toast.makeText(
                                                            this@ReviewActivity,
                                                            requestMessageSuccess,
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } catch (e: Exception) {
                                                        Log.e("TAG", e.message!!)
                                                        Toast.makeText(
                                                            this@ReviewActivity,
                                                            requestMessageFail,
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    holder.itemView.Button_follow.text = "Requested"
                                    holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_follow)
                                }
                            }
                        }
                    }
                    "UnFollow" -> {
                        rootDB.collection("users").document(users[position]).get().addOnSuccessListener { document ->
                            if (document != null) {
                                currentUser?.uid?.let { currentUserId ->
                                    rootDB.collection("users").document(currentUserId)
                                        .update("following", FieldValue.arrayRemove(document.id))
                                    rootDB.collection("users").document(users[position]).update(
                                        "followers",
                                        FieldValue.arrayRemove(currentUserId)
                                    )
                                    rootDB.collection("posts").whereEqualTo("userId", users[position]).get()
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                task.result?.forEach { doc ->
                                                    doc.reference.update(
                                                        "sharedWithUsers",
                                                        FieldValue.arrayRemove(currentUserId)
                                                    )
                                                }
                                            }
                                        }
                                }
                                holder.itemView.Button_follow.text = "Follow"
                                holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_follow)
                            }
                        }
                    }
                    "Requested" -> {
                        rootDB.collection("notifications")
                            .whereEqualTo("userId", users[position])
                            .whereEqualTo("followerRequestId", currentUser?.uid)
                            .get().addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) {
                                    if (task2.result != null) {
                                        task2.result?.forEach { doc ->
                                            doc.reference.delete()
                                        }
                                    }
                                }
                            }
                        holder.itemView.Button_follow.text = "Follow"
                        holder.itemView.Button_follow.setBackgroundResource(R.drawable.button_follow)
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getTime(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
        val currentDateandTime: String = sdf.format(Date())
        return currentDateandTime
    }
}
