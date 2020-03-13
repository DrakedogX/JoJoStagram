package com.example.jojostagram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    // Firebase Authentication 관리 클래스
    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance() // 인스턴스 가져오기

        email_login_btn.setOnClickListener{ createAndLoginEmail() }
    }

    // 이메일 회원가입 및 로그인 메드 (Firebase)
    fun createAndLoginEmail(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {
            task ->
                if(task.isSuccessful){
                    // 계정 생성이 성공 했을 경우
                    moveMainPage(auth?.currentUser)
                } else if (task.exception?.message.isNullOrEmpty()){
                    // 계정 생성이 실패 했을 경우
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    // 계정 생성도 및 에러도 발생되지 않았을 경우 로그인 화면으로 이동
                    signInEmail()
                }
        }
    }

    // 이메일 로그인 메서드
    fun signInEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                //progress_bar.visibility = View.GONE

                if (task.isSuccessful) {
                    // 이메일 로그인 성공 및 다음페이지 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    // 이메일 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun moveMainPage (user: FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
