package com.joel.jojostagram.navigation.model

/* 데이터 관리 클래스
explain: 컨텐츠 설명
imageUrl: 이미지 경로
uid: 컨텐츠 업로드 유저 UID
userId: 컨텐츠 업로드 유저 이메일
timestamp: 컨텐츠 올린 시각
favoriteCount: 좋아요 갯수

favorites: 좋아요 중복
{Comment: 댓글
  uid: 댓글 유저 UID
  userId: 댓글 유저 이메일
  comment: 댓글
  timestamp: 댓글 시각
}*/
data class ContentDTO(var explain: String? = null,
                      var imageUrl: String? = null,
                      var uid: String? = null,
                      var userId: String? = null,
                      var timestamp: Long? = null,
                      var favoriteCount: Int = 0,
                      var favorites: MutableMap<String, Boolean> = HashMap()) {

    data class Comment(var uid: String? = null,
                       var userId: String? = null,
                       var comment: String? = null,
                       var timestamp: Long? = null)
}