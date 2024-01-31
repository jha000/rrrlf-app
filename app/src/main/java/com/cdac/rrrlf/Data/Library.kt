package com.cdac.rrrlf.Data

data class Library(
    val beneficiary_id: String,
    val beneficiary_name: String,
    val beneficiary_contact_person: String,
    val beneficiary_contact_person_mobile_no: String,
    val beneficiary_contact_person_email_id: String,
    val beneficiary_pin_code: Int,
    val beneficiary_state: Int,
    val beneficiary_district: Int
)
