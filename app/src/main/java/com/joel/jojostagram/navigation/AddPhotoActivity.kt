package com.joel.jojostagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.joel.jojostagram.R
import com.joel.jojostagram.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

// 이미지 업로드 화면
class AddPhotoActivity : AppCompatActivity() {
    // PhotoPicker startActivityForResult 상수 값
    companion object {
        const val PICK_PROFILE_FROM_ALBUM = 0 // Intent Request ID
    }

    // 이미지 Uri 전역 변
    private var photoUri: Uri? = null

    // Firebase 저장소 전역 변수
    private var storage: FirebaseStorage? = null

    // Firebase Firestore 전역 변수
    private var firestore: FirebaseFirestore? = null

    // Firebase Authentication 전역 변수 (인증 정보 관련 private 정의)
    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // 인스턴스 초기화
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 화면 실행시 디바이스 앨범 오픈
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)

        // 게시글 내용 텍스트 필드 인풋타입 세팅
        add_photo_edit_explain.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // 이미지 파일 업로드 이벤트
        add_photo_btn.setOnClickListener { contentUpload() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 리퀘스트 코드가 PICK_PROFILE_FROM_ALBUM 일때
       if(requestCode == PICK_PROFILE_FROM_ALBUM){
           // 이미지 선택을 했을때 액티비티 결과 값 OK
           if(resultCode == Activity.RESULT_OK){
               // 이미지 뷰에 이미지 경로 세팅하여 이미지 표시
               println(data?.data)
               photoUri = data?.data
               add_photo_image.setImageURI(photoUri)
           }
        } else{
           // 이미지 선택을 하지 않았을때 액티비티 종료
           finish()
       }
    }

    // 이미지 파일 업로드
    private fun contentUpload(){
        add_photo_progress_bar.visibility = View.VISIBLE

        // 파일 이름 생성
        val currentDateTime = Calendar.getInstance().time
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(currentDateTime)
        val imageFileName = "JOJO_IMAGE{$timeStamp}.png"

        // Firebase 저장소상의 폴더명, 파일명 reference set
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // 이미지 Firebase 업로드 promise 메서드 (구글 권장 방식)
        storageRef?.putFile(photoUri!!)?.continueWithTask {
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            add_photo_progress_bar.visibility = View.GONE

            // 업로드 성공 토스트
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()

            // 시간 생성
            val contentDTO = ContentDTO()

            // 이미지 주소
            contentDTO.imageUrl = uri.toString()
            // 유저의 UID
            contentDTO.uid = auth?.currentUser?.uid
            // 게시물의 설명
            contentDTO.explain = add_photo_edit_explain.text.toString()
            // 유저의 이메일
            contentDTO.userId = auth?.currentUser?.email
            // 게시물 업로드 시간
            contentDTO.timestamp = System.currentTimeMillis()

            // 게시물 데이터를 DB에 생성
            firestore?.collection("images")?.document()?.set(contentDTO)

            // 액티비티 결과 값 OK set
            setResult(Activity.RESULT_OK)

            //엑티비티 종료
            finish()
        }?.addOnFailureListener {
            add_photo_progress_bar.visibility = View.GONE
            Toast.makeText(this, getString(R.string.upload_fail), Toast.LENGTH_SHORT).show()
        }

        // 이미지 Firebase 업로드 callback 메서드 (구글 권장 X)
        /*storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                //progress_bar.visibility = View.GONE

                // 업로드 성공 토스트
                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()

                // 시간 생성
                val contentDTO = ContentDTO()

                // 이미지 주소
                contentDTO.imageUrl = uri.toString()
                // 유저의 UID
                contentDTO.uid = auth?.currentUser?.uid
                // 게시물의 설명
                contentDTO.explain = add_photo_edit_explain.text.toString()
                // 유저의 이메일
                contentDTO.userId = auth?.currentUser?.email
                // 게시물 업로드 시간
                contentDTO.timestamp = System.currentTimeMillis()

                // 게시물 데이터를 DB에 생성
                firestore?.collection("images")?.document()?.set(contentDTO)

                // 액티비티 결과 값 OK set
                setResult(Activity.RESULT_OK)

                //엑티비티 종료
                finish()
            }
        }*/

    }
}

