package com.joel.jojostagram.navigation

import android.content.Intent
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.joel.jojostagram.R
import com.joel.jojostagram.navigation.model.AlarmDTO
import com.joel.jojostagram.navigation.model.ContentDTO
import com.squareup.okhttp.OkHttpClient
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

// 디테일 화면
class DetailViewFragment : Fragment() {

    // Firebase 유저 공통 전역 변수
    var user: FirebaseUser? = null

    // Firestore 저장소 전역 변수
    var firestore: FirebaseFirestore? = null

    // Firestore 리스너 등록 전역 변수
    var imagesSnapshot: ListenerRegistration? = null

    // OkHttpClient 전역 변수
    var okHttpClient: OkHttpClient? = null

    // 뷰 참조 전역 변수
    var mainView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)

        // 유저 정보
        user = FirebaseAuth.getInstance().currentUser
        // FirebaseFirestore 정보
        firestore = FirebaseFirestore.getInstance()

        // 리사이클러 뷰 어댑터 연결
        view.detail_fragment_recyclerview.adapter = DetailRecyclerViewAdapter()
        view.detail_fragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 리사이클러뷰 어댑터
    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // 리사이클러 뷰 데이터 변수
        private val contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        private val contentUidList: ArrayList<String> = arrayListOf()

        // 리사이클러뷰 초기값 설정
        init{
            // Firestore 접근하여 DB 정보 받아옴, 시간순으로
            // 쿼리스냅샷 데이터 하나씩 반복문으로 item에 set
            // contentDTOs에 item Add, contentUidList에 snapshot.id Add
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                // contentDTOs, contentUidList 초기화 -> 값 중복 방지
                contentDTOs.clear()
                contentUidList.clear()

                // 쿼리스냅샷 널 처리
                if (querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    val item = snapshot.toObject(ContentDTO::class.java)

                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged() // Adapter에게 DataSet이 변경되었으니 갱신하라고 알림
            }
        }

        // DetailRecyclerViewAdapter 클래스 상속시 구현해야할 함수 3가지 : onCreateViewHolder, onBindViewHolder, getItemCount
        // RecyclerView에 들어갈 View Holder를 할당하는 함수, View Holder는 실제 레이아웃 파일과 매핑되어야하며, extends의 Adater<>에서 <>안에들어가는 타입을 따른다.
        // 앞의 번호는 실행 순서
        // 2.viewType 형태의 아이템 뷰를 위한 뷰홀더 객체 생성
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
             val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        // 1.전체 아이템 갯수 리턴
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        // 3.데이터를 View Holder에 바인딩 (position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시)
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            // 홀더 프로필 이미지
            val profileImageView = viewHolder.item_detail_profile_imageview
            // 홀더 유저 ID
            val contentProfileTextView = viewHolder.item_detail_profile_textview
            // 홀더 게시글 이미지
            val contentImageView = viewHolder.item_detail_content_imageview
            // 홀더 게시글 내용
            val contentExplainTextView = viewHolder.item_detail_explain_textview
            // 좋아요 갯수
            val contentFavoriteCntTextView = viewHolder.item_detail_favorite_count_textview
            // 좋아요 버튼
            val contentFavoriteImageView = viewHolder.item_detail_favorite_imageview
            // 댓글 버튼
            val contentCommentImageView = viewHolder.item_detail_comment_imageview

            // 유저 ID
            contentProfileTextView.text = contentDTOs[position].userId

            // 유저 프로필 이미지
            firestore?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result?.get("image")

                        // 찾아올 유저 프로필 이미지가 null일 경우 기본 프로필 이미지 세팅
                        if (url == null) {
                            profileImageView.setImageResource(R.drawable.ic_account)
                        } else {
                            Glide.with(holder.itemView.context)
                                .load(url)
                                .apply(RequestOptions().circleCrop()).into(profileImageView)
                        }
                    }
                }

            // 콘텐트 이미지
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(contentImageView)

            // 게시글 내용
            contentExplainTextView.text = contentDTOs[position].explain

            //좋아요 갯수 설정
            contentFavoriteCntTextView.text = getString(R.string.favorite_ea, contentDTOs[position].favoriteCount)

            // 좋아요 버튼 클릭 이벤트
            contentFavoriteImageView.setOnClickListener { favoriteEvent(position) }

            // 좋아요 버튼 이미지 클릭시 change 설정
            if (contentDTOs[position].favorites.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)) {
                contentFavoriteImageView.setImageResource(R.drawable.ic_favorite)
            } else {
                contentFavoriteImageView.setImageResource(R.drawable.ic_favorite_border)
            }

            // 프로필 이미지 클릭시 상대방 유저 정보 페이지로 이동
            profileImageView.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()

                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, fragment).commit()
            }

            // 댓글 버튼 클릭시 댓글 화면 이동
            contentCommentImageView.setOnClickListener { v ->
                val intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        // 좋아요 버튼 이벤트
        private fun favoriteEvent(position : Int){
            // Firestore 선택한 이미지 uid
            val tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            // Firestore Transaction Run
            firestore?.runTransaction { transaction ->
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java) // ContentDTO Transaction Get

                if (contentDTO!!.favorites.containsKey(user!!.uid)) {
                    // 좋아요 활성때일때, 좋아요 비활성화
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1 // 좋아요 삭제
                    contentDTO.favorites.remove(user!!.uid) // uid 값 제거

                } else {
                    // 좋아요 비 활성화 일때, 좋아요 활성화
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1 // 좋아요 추가
                    contentDTO.favorites[user!!.uid] = true // uid true
                    favoriteAlarm(contentDTOs[position].uid!!) // 좋아요가 설정 되었기 때문에 알람 uid 세팅
                }
                // transaction set 값 리턴
                transaction.set(tsDoc, contentDTO)
            }
        }

        // 좋아요 알람 이벤트 (전달받은 UID)
        private fun favoriteAlarm(destinationUid: String) {
            // 알람 데이터 클래스 세팅
            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.email
            alarmDTO.uid = user?.uid
            alarmDTO.kind = 0 // 알람종류: 좋아요
            alarmDTO.timestamp = System.currentTimeMillis()

            // FirebaseFirestore 알람 세팅
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        }
    }
}