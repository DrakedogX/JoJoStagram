package com.joel.jojostagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.joel.jojostagram.R
import com.joel.jojostagram.navigation.model.AlarmDTO
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*

// 알람 화면
class AlarmFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm, container, false)

        // 리사이클러 뷰 어댑터 연결
        view.alarm_fragment_recyclerview.adapter = AlarmRecyclerViewAdapter()
        view.alarm_fragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 리사이클러뷰 어댑터
    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        // 리사이클러 뷰 데이터 변수
        private val alarmDTOList: ArrayList<AlarmDTO> = arrayListOf()

        init {
            // 나에게 도착한 알림 필터링
            val uid = FirebaseAuth.getInstance().currentUser!!.uid

            FirebaseFirestore.getInstance()
                .collection("alarms")
                .whereEqualTo("destinationUid", uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    // alarmDTOList 초기화 -> 값 중복 방지
                    alarmDTOList.clear()

                    // 쿼리스냅샷 널 처리
                    if(querySnapshot == null)return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }

                    alarmDTOList.sortByDescending { it.timestamp }

                    notifyDataSetChanged() // Adapter에게 DataSet이 변경되었으니 갱신하라고 알림
                }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = holder.itemView

            // 홀더 프로필 이미지
            val profileImage = viewHolder.comment_item_profile_imageview
            // 홀더 유저 ID
            val commentProfileTextView = viewHolder.comment_item_profile_textview
            // 홀더 댓글 텍스트
            val commentTextView = viewHolder.comment_item_comment_textview

            // 유저 프로필 이미지
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(alarmDTOList[position].uid!!)
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

            // 알람 종류에 따른 텍스트 표시
            when (alarmDTOList[position].kind){
                // 좋아요
                0 -> {
                    val favoriteTxt = alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite)
                    commentProfileTextView.text = favoriteTxt
                }
                // 댓글
                1 -> {
                    val commentTxt = alarmDTOList[position].userId + " " +alarmDTOList[position].message + getString(R.string.alarm_comment)
                    commentProfileTextView.text = commentTxt
                }
                // 팔로우
                2 -> {
                    val followTxt = alarmDTOList[position].userId + " " +getString(R.string.alarm_follow)
                    commentProfileTextView.text = followTxt
                }
            }
            // 댓글 내용은 숨김 처리 (USER ID를 보여주는 텍스트뷰에 내용 전부 출력)
            commentTextView.visibility = View.INVISIBLE
        }

    }
}