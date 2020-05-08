package com.joel.jojostagram.navigation

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.joel.jojostagram.LoginActivity
import com.joel.jojostagram.MainActivity
import com.joel.jojostagram.R
import com.joel.jojostagram.data.AlarmDTO
import com.joel.jojostagram.data.ContentDTO
import com.joel.jojostagram.data.FollowDTO
import com.joel.jojostagram.api.FcmPush
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*


// 유저 화면
class UserFragment : Fragment() {

    // PhotoPicker startActivityForResult 상수 값
    companion object {
        const val PICK_PROFILE_FROM_ALBUM = 10 // Intent Request ID
    }

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
        if (uid != null && uid == currentUserUid) {
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

            val mainActivity = (activity as MainActivity)

            mainActivity.main_toolbar_username.text = arguments!!.getString("userId")

            mainActivity.main_toolbar_back_btn.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }

            // 툴바 요소들 안보이게 처리
            mainActivity.main_toolbar_title_img.visibility = View.GONE
            mainActivity.main_toolbar_username.visibility = View.VISIBLE
            mainActivity.main_toolbar_back_btn.visibility = View.VISIBLE

            // 팔로우 버튼 클릭시 팔로우 이벤트 호출
            fragmentView?.account_follow_sign_btn?.setOnClickListener {
                requestFollow()
            }
        }

        // 리사이클러뷰 어댑터 및 레이아웃 매니저 setting(그리드 레이아웃, 3칸씩 표시)
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            // startActivityForResult Deprecated 예정 - registerForActivityResult()로 대체
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowing()
        getFollower()

        return fragmentView
    }

    // 팔로우 기능 세팅
    private fun requestFollow() {
        // 팔로잉 세팅
        val tsDocFollowing = fireStore?.collection("users")?.document(currentUserUid!!)
        // 트랜잭션 시작
        fireStore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            // followDTO가 값이 null일때 해당 followDTO 팔로잉 데이터 생성
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true // 나의 계정에 상대방의 uid 세팅

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // 팔로잉 상태 설정
            if (followDTO.followings.containsKey(uid)) {
                // 팔로우 한 상태
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)
            } else {
                // 팔로우 안한 상태
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // 팔로워 세팅
        val tsDocFollower = fireStore!!.collection("users").document(uid!!)
        // 트랜잭션 시작
        fireStore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)

            // followDTO가 값이 null일때 해당 followDTO 팔로워 데이터 생성
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true // 상대방의 계정에 나의 uid 세팅
                followerAlarm(uid!!) // 최초 팔로우 알람 세팅
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            // 팔로워 상태 설정
            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {
                // 상대방의 계정을 팔로우 중인 경우
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                // 상대방의 계정을 팔로우 중이 아닌 경우
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    // 실시간 팔로잉 가져오기
    private fun getFollowing() {
        fireStore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener

            fragmentView!!.account_tv_following_count.text = followDTO.followingCount.toString()
        }
    }

    // 실시간 팔로워 가져오기
    private fun getFollower() {
        fireStore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener

            fragmentView?.account_tv_follower_count?.text = followDTO.followerCount.toString()

            // 팔로우 상태에 따른 버튼 상태 변경
            if (followDTO.followers.containsKey(currentUserUid)) {
                fragmentView?.account_follow_sign_btn?.text = getString(R.string.follow_cancel)
                mySetColorFilter(fragmentView?.account_follow_sign_btn?.background, ContextCompat.getColor(activity!!, R.color.colorLightGray))
            } else {
                if (uid != currentUserUid) {
                    fragmentView?.account_follow_sign_btn?.text = getString(R.string.follow)
                    fragmentView?.account_follow_sign_btn?.background?.colorFilter = null
                }
            }
        }
    }

    // Firebase 유저 프로필 이미지 세팅
    private fun getProfileImage() {
        fireStore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener

            if(documentSnapshot.data != null) {
                val url = documentSnapshot.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    // 안드로이드 버전에 따른 setColorFilter 적용
    private fun mySetColorFilter(drawable: Drawable?, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable?.colorFilter = BlendModeColorFilter(color, BlendMode.MULTIPLY)
        } else {
            drawable?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    // 팔로우 알람 이벤트 (전달받은 UID)
    private fun followerAlarm(destinationUid: String) {
        // 알람 데이터 클래스 세팅
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2 // 알람종류: 팔로우
        alarmDTO.timestamp = System.currentTimeMillis()

        // FirebaseFirestore 알람 세팅
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        // FCM 푸시
        val message = auth?.currentUser?.email + " " +getString(R.string.alarm_follow)
        FcmPush.FcmPushInstance.sendMessage(destinationUid, "JoJoStagram", message)
    }

    // 유저 리사이클러뷰 어댑터
    // 로그인한 유저의 업로드한 이미지만 리사이클러뷰에 출력
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

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

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}