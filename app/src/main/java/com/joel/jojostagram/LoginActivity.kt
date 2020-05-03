package com.joel.jojostagram

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

// 로그인 화면
// 최초 메인 액티비티 추가하여 변경 예정
class LoginActivity : AppCompatActivity()  {

    // GoogleLogin startActivityForResult 상수 값
    companion object {
        private const val GOOGLE_LOGIN_CODE = 9001 // Intent Request ID
    }

    // Firebase Authentication 전역 변수 (인증 정보 관련 private 정의)
    private var auth: FirebaseAuth? = null

    // Google Login 관리 클래스 전역 변수
    private var googleSignInClient: GoogleSignInClient? = null

    // Facebook 로그인 처리 결과 관리 클래스 (콜백)
    private var facebookCallbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance() // 파이어베이스 인증 인스턴스 가져오기

        email_login_btn.setOnClickListener{ createAndLoginEmail() } // 이메일 로그인 버튼 set

        google_login_in_btn.setOnClickListener { googleLogin() } // 구글 로그인 버튼 set

        facebook_login_btn.setOnClickListener { facebookLogin() } // 페이스북 로그인 버튼 set

        // 구글 로그인 옵션 및 토큰키 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                  .requestIdToken(getString(R.string.default_web_client_id))
                  .requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(this, gso) // 구글 로그인 클래스 변수 할당

        //printHashKey() // 페이스북 로그인 API에 필요한 해쉬 값 추출

        facebookCallbackManager = CallbackManager.Factory.create() // 페이스북 콜백 매니저 변수 create
    }

    override fun onStart() {
        super.onStart()

        moveMainPage(auth?.currentUser)
    }

    // 멀티덱스 세팅
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    // 액티비티 결과 값 수신
    // onActivityResult Deprecated 예정 - ActivityResult로 대체
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 페이스북에서 승인된 정보를 가지고 옴
        facebookCallbackManager?.onActivityResult(requestCode, resultCode, data)

        // 구글 로그인에서 승인된 정보를 가지고 오며 성공시 파이어베이스로 전달
        if (requestCode == GOOGLE_LOGIN_CODE){
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result != null) {
                if (result.isSuccess){
                    val account = result.signInAccount
                    firebaseAuthWithGoogle(account!!)
                } else {
                    // 구글 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    login_progress_bar.visibility = View.GONE
                    Toast.makeText(this, resultCode, Toast.LENGTH_SHORT).show()
                }
            } else {
                // 구글 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                login_progress_bar.visibility = View.GONE
                Toast.makeText(this, resultCode, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 이메일 로그인 function
    private fun signInEmail(){
        login_progress_bar.visibility = View.VISIBLE
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                login_progress_bar.visibility = View.GONE
                if (task.isSuccessful) {
                    // 이메일 로그인 성공 및 다음페이지 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    // 이메일 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 구글 로그인 액티비티 인텐트
    private fun googleLogin (){
        login_progress_bar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient?.signInIntent
        // startActivityForResult Deprecated 예정 - registerForActivityResult()로 대체
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    // 페이스북 로그인
    private fun facebookLogin(){
        login_progress_bar.visibility = View.VISIBLE
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        LoginManager.getInstance().registerCallback(facebookCallbackManager, object: FacebookCallback<LoginResult>{
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                login_progress_bar.visibility = View.GONE
            }

            override fun onError(error: FacebookException?) {
                login_progress_bar.visibility = View.GONE
            }

        })
    }

    // 이메일 회원가입 및 로그인 function (Firebase)
    private fun createAndLoginEmail(){
        if (email_edittext.text.toString().isEmpty() || password_edittext.text.toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        } else {
            login_progress_bar.visibility = View.VISIBLE
            auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                ?.addOnCompleteListener { task ->
                    login_progress_bar.visibility = View.GONE
                    when {
                        task.isSuccessful -> {
                            // 계정 생성이 성공 했을 경우 메인 액티비티 호출
                            moveMainPage(auth?.currentUser)
                        }
                        task.exception?.message.isNullOrEmpty() -> {
                            // 계정 생성이 실패 했을 경우
                            Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // 계정 생성도 및 에러도 발생되지 않았을 경우 이메일 로그인
                            signInEmail()
                        }
                    }
                }
        }
    }

    // Facebook 인증 토큰을 Firebase로 넘겨주는 메서드
    fun handleFacebookAccessToken(token : AccessToken){
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                login_progress_bar.visibility = View.GONE
                if (task.isSuccessful) {
                    // 페이스 로그인 성공 및 메인 액티비티 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    // 페이스북 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 구글 인증 정보 파이어베이스로 전달
    private fun firebaseAuthWithGoogle (account: GoogleSignInAccount?){
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                login_progress_bar.visibility = View.GONE
                if (task.isSuccessful) {
                    // 구글 로그인 성공 및 메인 액티비티 호출
                    moveMainPage(auth?.currentUser)
                } else {
                    // 구글 로그인 실패 (계정 정보가 맞지 않거나 없는 계정일때) - 토스트 출력
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 메인 액티비티로 이동
    private fun moveMainPage (user: FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // 페이스북 로그인 해쉬 값 로그 추출
    /* fun printHashKey() {
         try {
             val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
             for (signature in info.signatures) {
                 val md: MessageDigest = MessageDigest.getInstance("SHA")
                 md.update(signature.toByteArray())
                 val hashKey = String(Base64.encode(md.digest(), 0))
                 Log.i("TAG", "printHashKey() Hash Key: $hashKey")
             }
         } catch (e: NoSuchAlgorithmException) {
             Log.e("TAG", "printHashKey()", e)
         } catch (e: Exception) {
             Log.e("TAG", "printHashKey()", e)
         }
     }*/
}
