package com.joel.jojostagram.data

data class FollowDTO(

    // 팔로워 카운트, 팔로워 중복 방지
    var followerCount: Int = 0,
    var followers: MutableMap<String, Boolean> = HashMap(),

    // 팔로잉 카운트, 팔로잉 중복 방지
    var followingCount: Int = 0,
    var followings: MutableMap<String, Boolean> = HashMap()
)