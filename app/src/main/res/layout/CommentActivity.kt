package com.kelompokcool.bangkitcapstone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.kelompokcool.bangkitcapstone.HomeActivity
import com.kelompokcool.bangkitcapstone.PersonalActivity
import com.kelompokcool.bangkitcapstone.PersonalUserSideActivity
import com.kelompokcool.bangkitcapstone.R
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import com.kelompokcool.entity.Comment
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.toolbar_main.*
import java.text.SimpleDateFormat
import java.util.*

class CommentActivity : AppCompatActivity() {

    private var adapter: CommentAdapter? = null
    private var pageBack: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        val postId = intent.getStringExtra("postId")
        pageBack = intent.getStringExtra("pageBack").toString()
        userId = intent.getStringExtra("userId").toString()

        button_comment.setOnClickListener {
            rootDB.collection("users").document(authDb.currentUser!!.uid).get()
                .addOnCompleteListener { user ->
                    if (user.isSuccessful) {
                        val userInfo = user.result
                        if (userInfo != null) {
                            try {
                                val comment = Comment().apply {
                                    userId = authDb.currentUser?.uid
                                    userFullName = userInfo.get("userName") as String?
                                    this.comment = EditText_comment.text.toString().trim()
                                    time = getTime()
                                    this.postId = postId
                                    id = rootDB.collection("comments").document().id
                                }

                                if (comment.comment?.isNotEmpty() == true) {
                                    rootDB.collection("comments").document(comment.id!!).set(comment)
                                }

                                // Show confirmation & clear inputs
                                EditText_comment.setText("")

                            } catch (e: Exception) {
                                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        }

        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        val commentsQuery = rootDB.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("time", Query.Direction.DESCENDING)
        val options = FirestoreRecyclerOptions.Builder<Comment>()
            .setQuery(commentsQuery, Comment::class.java)
            .build()
        adapter = CommentAdapter(options)
        commentsRecyclerView.adapter = adapter

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
                        val intent = Intent(this@CommentActivity, PersonalUserSideActivity::class.java)
                        intent.putExtra("userID", userId)
                        startActivity(intent)
                        return true
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private inner class CommentViewHolder(private val view: View) :
        RecyclerView.ViewHolder(view)

    private inner class CommentAdapter(options: FirestoreRecyclerOptions<Comment>) :
        FirestoreRecyclerAdapter<Comment, CommentViewHolder>(options) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CommentViewHolder(view)
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        override fun onBindViewHolder(holder: CommentViewHolder, position: Int, model: Comment) {
            with(holder.itemView) {
                TextView_user.text = "${model.userFullName}:"
                TextView_comment.text = model.comment!!

                ImageView_like.setBackgroundResource(
                    if (model.whoLiked?.contains(authDb.currentUser!!.uid) == true) {
                        R.drawable.liked
                    } else {
                        R.drawable.heart
                    }
                )

                ImageView_like.setOnClickListener {
                    val docRef = rootDB.collection("comments").document(model.id!!)
                    val likedByCurrentUser = model.whoLiked?.contains(authDb.currentUser!!.uid) == true

                    val updateMap = if (likedByCurrentUser) {
                        mapOf("whoLiked" to FieldValue.arrayRemove(authDb.currentUser!!.uid))
                    } else {
                        mapOf("whoLiked" to FieldValue.arrayUnion(authDb.currentUser!!.uid))
                    }

                    docRef.update(updateMap)
                }

                TextView_comment.setOnClickListener {
                    val alert = AlertDialog.Builder(this@CommentActivity)
                    val mView: View = layoutInflater.inflate(R.layout.bio_dialogue, null)
                    val button_bio_edit: Button = mView.findViewById(R.id.Button_bio_edit)
                    val editText_bio_edit: EditText = mView.findViewById(R.id.EditText_bio_edit)
                    alert.setView(mView)
                    val alertDialog: AlertDialog = alert.create()

                    editText_bio_edit.setText(model.comment.toString())
                    editText_bio_edit.setSelection(editText_bio_edit.length())
                    alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    alertDialog.show()

                    button_bio_edit.setOnClickListener {
                        val newComment = editText_bio_edit.text.toString()
                        if (newComment.isNotEmpty()) {
                            rootDB.collection("comments").document(model.id!!)
                                .update(
                                    mapOf(
                                        "comment" to newComment,
                                        "time" to getTime()
                                    )
                                )
                                .addOnSuccessListener {
                                    val intent = Intent(applicationContext, CommentActivity::class.java)
                                    intent.putExtra("postId", model.postId)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            rootDB.collection("comments").document(model.id!!).delete()
                                .addOnSuccessListener {
                                    val intent = Intent(applicationContext, CommentActivity::class.java)
                                    intent.putExtra("postId", model.postId)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
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
