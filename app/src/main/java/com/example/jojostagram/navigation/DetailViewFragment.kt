package com.example.jojostagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jojostagram.R
import com.example.jojostagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.okhttp.OkHttpClient
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {

    // Firebase 유저 전역 변수
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
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)

        firestore = FirebaseFirestore.getInstance()

        view.detail_fragment_recyclerview.adapter = DetailRecyclerViewAdapter()
        view.detail_fragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 리사이클러뷰 어댑터
    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        val contentUidList: ArrayList<String> = arrayListOf()

        // 생성자
        init{

            // Firestore 접근하여 DB 정보 받아옴, 시간순으로
            // 쿼리스냅샷시 contentDTOs, contentUidList 초기화
            // 쿼리스냅샷 데이터 하나씩 반복문으로 item에 set
            // contentDTOs에 item Add, contentUidList에 snapshot.id Add
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)

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
             var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        // 1.전체 아이템 갯수 리턴
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        // 3.데이터를 View Holder에 바인딩 (position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시)
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView

            // 유저 ID 바인딩
            viewHolder.item_detail_profile_textview.text = contentDTOs!![position].userId

            // 유저 프로필 이미지
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewHolder.item_detail_profile_imageview)

            // 콘텐트 이미지
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewHolder.item_detail_content_imageview)

            // 설명 텍스트
            viewHolder.item_detail_explain_textview.text = contentDTOs[position].explain

            //좋아요 갯수 설정
            viewHolder.item_detail_favorite_count_textview.text = "좋아요 " + contentDTOs[position].favoriteCount + "개"

        }

    }
}