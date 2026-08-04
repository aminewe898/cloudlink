package com.cloudlink.app.data.model

data class RemoteFile(
    val name: String,
    val isDirectory: Boolean,
    val size: String,
    val permissions: String,
    val modifiedDate: String,
    val isSymbolicLink: Boolean = false,
    val rawSize: Long = 0L
)

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
}
