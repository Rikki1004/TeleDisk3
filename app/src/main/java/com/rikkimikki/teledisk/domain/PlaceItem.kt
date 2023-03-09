package com.rikkimikki.teledisk.domain

data class PlaceItem (
    val name:String,
    val path:String,
    val totalSpase:Long,
    val occupatedSpace:Long,
    val scopeType: ScopeType,
    val isMain:Boolean = false
)