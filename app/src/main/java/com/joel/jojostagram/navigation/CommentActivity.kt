package com.joel.jojostagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.joel.jojostagram.R
import com.joel.jojostagram.model.AlarmDTO
import com.joel.jojostagram.model.ContentDTO
import com.joel.jojostagram.api.FcmPush
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {

    // Firebase 및 계정 관련 전역 변수
    private var contentUid: String? = null
    private var destinationUid: String? = null
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        // 유저 정보
        user = FirebaseAuth.getInstance().currentUser

        // 인텐트로 받은 정보
        destinationUid = intent.getStringExtra("destinationUid")
        contentUid = intent.getStringExtra("contentUid")

        // 리사이클러 뷰 어댑터 연결
        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        // 댓글 전송 버튼 이벤트
        comment_send_btn?.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser!!.email
            comment.comment = comment_message_btn.text.toString()
            comment.uid = FirebaseAuth.getInstance().currentUser!!.uid
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, comment_item_comment_textview.text.toString())
            comment_message_btn.setText("")
        }
    }

    // 댓글 알람 이벤트 (전달받은  UID, 댓글 내용)
    private fun commentAlarm(destinationUid: String, message: String) {
        // 알람 데이터 클래스 세팅
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = user?.email
        alarmDTO.uid = user?.uid
        alarmDTO.kind = 1 // 알람종류: 댓글
        alarmDTO.message = message // 댓글 내용
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        // FCM 푸시
        val msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        FcmPush.FcmPushInstance.sendMessage(destinationUid, "JoJoStagram", msg)
    }

    // 리사이클러뷰 어댑터
    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // 리사이클러 뷰 데이터 변수
        private var comments: ArrayList<ContentDTO.Comment> = arrayListOf()

        // 리사이클러 뷰 초기값 설정
        init {
            comments = ArrayList()
            FirebaseFirestore
                .getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    // comments 초기화 -> 값 중복 방지
                    comments.clear()

                    // 쿼리스냅샷 널 처리
                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged() // Adapter에게 DataSet이 변경되었으니 갱신하라고 알림
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = holder.itemView

            // 홀더 프로필 이미지
            val profileImage = viewHolder.comment_item_profile_imageview
            // 홀더 댓글 텍스트
            val commentTextView = viewHolder.comment_item_comment_textview
            // 홀더 유저 ID
            val commentProfileTextView = viewHolder.comment_item_profile_textview

            // 댓글 텍스트 세팅
            commentTextView.text = comments[position].comment
            // 유저 ID 세팅
            commentProfileTextView.text = comments[position].userId

            // 유저 프로필 이미지
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result?.get("image")

                        // 찾아올 유저 프로필 이미지가 null일 경우 기본 프로필 이미지 세팅
                        if (url == null) {
                            profileImage.setImageResource(R.drawable.ic_account)
                        } else {
                            Glide.with(holder.itemView.context)
                                .load(url)
                                .apply(RequestOptions().circleCrop()).into(profileImage)
                        }
                    }
                }
        }
    }
}
