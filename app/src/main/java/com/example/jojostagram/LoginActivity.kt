package com.example.jojostagram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    // Firebase Authentication 관리 클래스 전역 변
    var auth: FirebaseAuth? = null

    // Google Login 관리 클래스 전역 변수
    var googleSignInClient: GoogleSignInClient? = null

    // GoogleLogin 코드 전역 변수
    val GOOGLE_LOGIN_CODE = 9001 // Intent Request ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance() // 파이어베이스 인증 인스턴스 가져오기

        email_login_btn.setOnClickListener{ createAndLoginEmail() } // 이메일 로그인 버튼 set

        google_sign_in_btn.setOnClickListener { googleLogin() } //구글 로그인 버튼 set

        // 구글 로그인 옵션 및 토큰키 설정
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                  .requestIdToken(getString(R.string.default_web_client_id))
                  .requestEmail().build()

        // 구글 로그인 클래스
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // 구글 로그인 액티비티 인텐트
    fun googleLogin (){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 구글 로그인에서 승인된 정보를 가지고 오며 성공시 파이어베이스로 전달
        if (requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess){
                var account = result.signInAccount
                firebaseAuthWithGoogle(account)
            }
        }
    }

    // 구글 인증 정보 파이어베이스로 전달
    fun firebaseAuthWithGoogle (account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                //progress_bar.visibility = View.GONE
                if (task.isSuccessful) {
                    // 구글 로그인 성공 및 메인 액티비티 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    // 구글 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 이메일 회원가입 및 로그인 메서드 (Firebase)
    fun createAndLoginEmail(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {
            task ->
                if(task.isSuccessful){
                    // 계정 생성이 성공 했을 경우 메인 액티비티 호출
                    moveMainPage(auth?.currentUser)
                } else if (task.exception?.message.isNullOrEmpty()){
                    // 계정 생성이 실패 했을 경우
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    // 계정 생성도 및 에러도 발생되지 않았을 경우 이메일 로그인
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

    // 메인 액티비티로 이동
    fun moveMainPage (user: FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
