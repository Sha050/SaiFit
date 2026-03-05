package com.saifit.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName(value = "_id", alternate = ["id"])
    val id: String = "",
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val age: Int,
    val gender: Gender,
    val role: UserRole,
    val email: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String? = null,
    val region: String? = null,
    val sport: String? = null,
    @SerializedName("profile_image_uri")
    val profileImageUri: String? = null
) {

    val name: String get() = "$firstName $lastName".trim()
}

enum class UserRole {
    @SerializedName("athlete")
    ATHLETE,
    @SerializedName("admin")
    ADMIN
}

enum class Gender {
    @SerializedName("Male")
    MALE,
    @SerializedName("Female")
    FEMALE,
    @SerializedName("Other")
    OTHER
}
