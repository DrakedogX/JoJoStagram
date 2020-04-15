package com.joel.jojostagram

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.joel.jojostagram.navigation.*
import kotlinx.android.synthetic.main.activity_main.*

// 메인 화면
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 메인 프로그레스 바 표시
        main_progress_bar.visibility = View.VISIBLE

        // 하단 네비게이션 셀렉트 세팅
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        // 디바이스 스토리지 리드 권한 체크
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        // 메인 액티비티 defalt 화면 설정
        bottom_navigation.selectedItemId = R.id.action_home

        // FCM 토큰 생성
        registerPushToken()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 유저 프로필 사진 변경이 정상적으로 되었을 경우
        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            val imageUri = data?.data
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

             storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                 return@continueWithTask storageRef.downloadUrl
             }.addOnSuccessListener { uri ->
                 val map = HashMap<String, Any>()
                 map["image"] = uri.toString()
                 FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
             }
        }
    }

    // 하단 네비게이션 아이콘 선택에 따른 프래그먼트 호출
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        setToolbarDefalt()
        when (p0.itemId){
            // 메인 화면
            R.id.action_home ->{
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, detailViewFragment).commit()
                return true
            }
            // 검색 화면
            R.id.action_search ->{
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, gridFragment).commit()
                return true
            }
            // 이미지 업로드 화면
            R.id.action_add_photo ->{
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 디바이스 내부 저장소 접근 권한을 얻었을 경우 이미지 업로드 화면으로 이동
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                } else {
                    // 디바이스 내부 저장소 접근 권한을 얻지 못했을 경우 토스트 출력
                    Toast.makeText(this, "디바이스 스토리지 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }
                return true
            }
            // 알람 화면
            R.id.action_favorite_alarm ->{
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, alarmFragment).commit()
                return true
            }
            // 계정 화면
            R.id.action_account ->{
                val userFragment = UserFragment()

                // UID 전달
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle

                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, userFragment).commit()
                return true
            }
        }
        return false
    }

    fun registerPushToken(){
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {task ->
            val pushToken = task.result?.token
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String,Any>()
            map["pushToken"] = pushToken!!
            FirebaseFirestore.getInstance().collection("pushToken").document(uid!!).set(map)
        }
    }

    // 툴바 상태 변경 메서드
    fun setToolbarDefalt() {
         main_toolbar_username.visibility = View.GONE
        main_toolbar_back_btn.visibility = View.GONE
        main_toolbar_title_img.visibility = View.GONE
    }
}
