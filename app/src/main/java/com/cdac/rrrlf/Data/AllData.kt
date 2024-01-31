package com.cdac.rrrlf.Data

data class AllData (
    val scheme_code: String,
    val beneficiary_id: Int,
    val application_id: String,
    val financial_year_of_selection: String,
    val state_id: Int,
    val total_applied_amount: Double,
    val applied_gst: Double,
    val total_recommended_amount: Double,
    val recommended_gst: Double,
    val total_sanctioned_amount: Double,
    val sanctioned_gst: Double,
    val sanctioned_remarks: String
)
