package com.cdac.rrrlf.Data

data class State(
    val state_id: Int,
    val state_name: String,
    val state_address1: String,
    val state_address2: String,
    val state_pincode: Int,
    val state_authority: String,
    val convener_name: String,
    val convener_address: String,
)
