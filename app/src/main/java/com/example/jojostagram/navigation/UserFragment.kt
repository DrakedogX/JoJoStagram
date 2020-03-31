package com.example.jojostagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jojostagram.LoginActivity
import kotlinx.android.synthetic.main.fragment_user.view.*
import com.example.jojostagram.R
import com.example.jojostagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// 유저 화면
class UserFragment : Fragment() {

    // 프래그먼트 뷰 전역 변수
    private var fragmentView: View? = null

    // Firebase 전역 변수
    private var fireStore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    // 유저 UID 정보 전역 변수
    private var uid: String? = null
    private var currentUserUid: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)

        // 이전 화면에서 UID 넘겨 받기
        uid = arguments?.getString("destinationUid")

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

        // UID 비교를 위해 uid 초기화
        currentUserUid = auth?.currentUser?.uid

        // UID 비교 [ 현재 유저 정보 페이지 기준, 해당 유저 페이지가 앱에 로그인된 계정인지 다른 사람의 계정인지 비교하여 페이지 호출 ]
        if (uid == currentUserUid) {
            // 현재 앱에 로그인된 나의 페이지
            fragmentView?.account_follow_sign_btn?.text = getString(R.string.signout)

            // 로그아웃
            fragmentView?.account_follow_sign_btn?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }

        } else {
            // 다른 유저의 페이지
            fragmentView?.account_follow_sign_btn?.text = getString(R.string.follow)

        }

        // 리사이클러뷰 어댑터 및 레이아웃 매니저 setting(그리드 레이아웃, 3칸씩 표시)
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)

        return fragmentView
    }

    // 유저 리사이클러뷰 어댑터
    // 로그인한 유저의 업로드한 이미지만 리사이클러뷰에 출력
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            contentDTOs = ArrayList()

            // 로그인한 유저의 이미지만 Select
            fireStore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()

                // 쿼리스냅샷 널 처리
                if (querySnapshot == null) return@addSnapshotListener

                // Firestore 데이터 추출
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }

                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString() // 로그인한 유저가 업로드한 포스트의 갯수
                notifyDataSetChanged() // 데이터 새로고침 알림
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // 화면 가로 크기 1/3
            val width = resources.displayMetrics.widthPixels / 3

            // 가로 크기의 1/3 정사각형 이미지
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            // 뷰 홀더로 리턴
            return CustomViewHolder(imageView)
        }

        // contentDTOs 사이즈 리턴
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        // 리사이클러 뷰 홀더 이미지 바인딩
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as CustomViewHolder).imageView

            // Glide 이미지 로드 (이미지 경로), 적용(이미지 타입, 이미지 Center Crop)
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageView)
        }

        // 이미지 레이아웃 리턴 받은 뷰 홀더
        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) { }
    }
}