package com.kelompokcool.bangkitcapstone

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.kelompokcool.entity.BaseFirebaseProperties
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.authDb
import com.kelompokcool.entity.BaseFirebaseProperties.Companion.rootDB
import com.kelompokcool.entity.User
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.item_search_user.view.*
import kotlinx.android.synthetic.main.toolbar_main.*

class SearchActivity : AppCompatActivity() {

    private var adapter: UserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(topToolbar)

        searchResultRecyclerView.layoutManager = LinearLayoutManager(this)

        val query = getInitialQuery()
        val options = getFirestoreRecyclerOptions(query)
        adapter = UserAdapter(options)
        searchResultRecyclerView.adapter = adapter

        searchUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val newQuery = getSearchQuery(query)
                val newOptions = getFirestoreRecyclerOptions(newQuery)
                adapter?.updateOptions(newOptions)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val newQuery = getSearchQuery(newText)
                val newOptions = getFirestoreRecyclerOptions(newQuery)
                adapter?.updateOptions(newOptions)
                return true
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                startActivity(Intent(applicationContext, PostActivity::class.java))
                return true
            }
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
            R.id.action_serach -> {
                startActivity(Intent(applicationContext, SearchActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getInitialQuery(): Query {
        return rootDB.collection("users")
            .whereNotEqualTo("userName", null)
            .orderBy("userName")
    }

    private fun getSearchQuery(searchString: String): Query {
        return rootDB.collection("users")
            .whereNotEqualTo("userName", null)
            .orderBy("userName")
            .whereGreaterThanOrEqualTo("userName", searchString)
    }

    private fun getFirestoreRecyclerOptions(query: Query): FirestoreRecyclerOptions<User> {
        return FirestoreRecyclerOptions.Builder<User>()
            .setQuery(query, User::class.java)
            .build()
    }

    private inner class UserViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view)

    private inner class UserAdapter internal constructor(options: FirestoreRecyclerOptions<User>) :
        FirestoreRecyclerAdapter<User, UserViewHolder>(options) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_user, parent, false)
            return UserViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
            if (model.photoURI != null) {
                val tempPhotoUri: String = model.photoURI!!
                val photoTemp = tempPhotoUri.split(".")
                val imageUrl = if (photoTemp[0] == authDb.currentUser?.uid) {
                    BaseFirebaseProperties.imageRef.child("profilePictures/$tempPhotoUri")
                } else {
                    model.photoURI
                }
                Glide.with(this@SearchActivity)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.itemView.imageView_profile_picture)
            }

            holder.itemView.user_full_name.text = "${model.firstName} ${model.lastName}"
            holder.itemView.username.text = model.userName

            holder.itemView.setOnClickListener {
                if (model.id == authDb.currentUser?.uid) {
                    startActivity(Intent(applicationContext, PersonalActivity::class.java))
                } else {
                    val intent = Intent(applicationContext, PersonalUserSideActivity::class.java)
                    intent.putExtra("userID", model.id)
                    intent.putExtra("isPrivate", model.private)
                    if (model.followers?.contains(authDb.currentUser?.uid) == true) {
                        intent.putExtra("isFollowing", true)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun logout() {
        authDb.signOut()
        finish()
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
    }
}
