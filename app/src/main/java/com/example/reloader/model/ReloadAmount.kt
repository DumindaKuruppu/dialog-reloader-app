package com.example.reloader.model

import java.util.UUID

data class ReloadAmount(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val phoneNumber: String,
    val amount: String
)
