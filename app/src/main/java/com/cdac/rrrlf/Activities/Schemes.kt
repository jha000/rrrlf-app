package com.cdac.rrrlf.Activities

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.cdac.rrrlf.Adapter.SchemeAdapter
import com.cdac.rrrlf.Data.AllData
import com.cdac.rrrlf.R
import com.cdac.rrrlf.Services.ApiService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Schemes : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var schemeAdapter: SchemeAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loading: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schemes)

        val libID = intent.getStringExtra("LIBRARY_ID")
        val libTitle = intent.getStringExtra("LIBRARY_TITLE")

        val libraryName = findViewById<TextView>(R.id.LibraryName)
        val back = findViewById<ImageView>(R.id.back)
        recyclerView = findViewById(R.id.SchemeRecyclerView)
        loading = findViewById(R.id.loading)

        back.setOnClickListener{
            super.onBackPressed()
        }

        libraryName.text = libTitle

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nodenote.cyclic.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create the API service
        val apiService = retrofit.create(ApiService::class.java)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        // Retrieve the selected financial year from SharedPreferences
        val selectedFinancialYear = retrieveSelectedFinancialYear()

        // Use viewLifecycleOwner.lifecycleScope for Fragments
        lifecycleScope.launch {
            try {
                // Fetch data from the new API and filter based on libID and financial year
                val allData = apiService.getAllData()
                val filteredData = allData.filter {
                    it.beneficiary_id == libID!!.toInt() && it.financial_year_of_selection == selectedFinancialYear
                }

                // Update RecyclerView
                updateRecyclerView(filteredData)

            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
                // Handle error
                Toast.makeText(this@Schemes, "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRecyclerView(dataList: List<AllData>) {
        schemeAdapter = SchemeAdapter(dataList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = schemeAdapter

        loading.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

    }

    private fun retrieveSelectedFinancialYear(): String {
        return sharedPreferences.getString("SELECTED_FINANCIAL_YEAR", "") ?: ""
    }
}

