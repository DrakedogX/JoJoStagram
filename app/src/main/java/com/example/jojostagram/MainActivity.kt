package com.example.jojostagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jojostagram.navigation.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 하단 네비게이션 셀렉트 세팅
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }

    // 하단 네비게이션 아이콘 선택에 따른 프래그먼트 호출
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId){
            // 메인 화면
            R.id.action_home ->{
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, detailViewFragment).commit()
                return true
            }
            // 검색 화면
            R.id.action_search ->{
                var gridFragment = GridFragment()
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
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, alarmFragment).commit()
                return true
            }
            // 계정 화면
            R.id.action_account ->{
                var userFragment = UserFragment()
                supportFragmentManager.beginTransaction().replace(R.id.frame_main_content, userFragment).commit()
                return true
            }
        }
        return false
    }
}
