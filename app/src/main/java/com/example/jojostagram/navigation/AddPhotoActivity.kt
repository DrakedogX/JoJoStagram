package com.example.jojostagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.jojostagram.R
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    // 리퀘스트 코드
    var PICK_IMAGE_FROM_ALBUM = 0

    // Firebase 저장소 초기화 전역 변수 선언
    var storage: FirebaseStorage? = null

    // 이미지 Uri
    var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // Firebase 저장소 초기화
        storage = FirebaseStorage.getInstance()

        // 화면 실행시 디바이스 앨범 오픈
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // 이미지드 파일 업로드 이벤트
        add_photo_btn.setOnClickListener { contentUpload() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 리퀘스트 코드가 PICK_IMAGE_FROM_ALBUM 일때
       if(requestCode == PICK_IMAGE_FROM_ALBUM){
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
    fun contentUpload(){
        //progress_bar.visibility = View.VISIBLE

        // 파일 이름 생성
        var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JOJO_IMAGE" + timeStamp + ".png"

        // Firebase 저장소상의 폴더명, 파일명 reference set
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // 이미지 Firebase 업로드
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener { taskSnapshot ->
            //progress_bar.visibility = View.GONE

            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
        }

    }
}

