package com.joel.jojostagram.navigation

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
import com.google.firebase.firestore.FirebaseFirestore
import com.joel.jojostagram.R
import com.joel.jojostagram.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_grid.view.*

// 그리드 이미지뷰 화면
class GridFragment : Fragment() {
    // Firebase 전역 변수
    private var fireStore: FirebaseFirestore? = null

    // 프래그먼트 뷰 전역 변수
    private var fragmentView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_grid, container, false)

        // Firebase 초기화
        fireStore = FirebaseFirestore.getInstance()

        // 리사이클러뷰 어댑터 및 레이아웃 매니저 setting(그리드 레이아웃, 3칸씩 표시)
        fragmentView?.grid_fragment_recyclerview?.adapter = GridFragmentRecyclerViewAdapter()
        fragmentView?.grid_fragment_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)

        return fragmentView
    }
    // 유저 리사이클러뷰 어댑터
    // 로그인한 유저의 업로드한 이미지만 리사이클러뷰에 출력
    inner class GridFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            contentDTOs = ArrayList()

            // 로그인한 유저의 이미지만 Select
            // 비동기 처리 추가 예정
            fireStore?.collection("images")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    // 쿼리스냅샷 널 처리
                    if (querySnapshot == null) return@addSnapshotListener

                    // Firestore 데이터 추출
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
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
        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}