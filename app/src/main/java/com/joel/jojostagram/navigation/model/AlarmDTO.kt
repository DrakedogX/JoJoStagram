package com.joel.jojostagram.navigation.model

data class AlarmDTO (
    // kind 0 : 좋아요, 1: 댓글, 2: 팔로우
    var destinationUid: String? = null,
    var userId: String? = null,
    var uid: String? = null,
    var kind: Int = 0,
    var message: String? = null,
    var timestamp: Long? = null
)