package com.cdac.rrrlf.Activities

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cdac.rrrlf.Adapter.LibraryAdapter
import com.cdac.rrrlf.Data.Library
import com.cdac.rrrlf.R
import com.cdac.rrrlf.Services.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenLibraries : AppCompatActivity() {

    lateinit var state: TextView
    lateinit var district: TextView
    lateinit var back: ImageView
    lateinit var librariesRecyclerView: RecyclerView
    lateinit var progressBar: LinearLayout
    lateinit var progressBar2: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_libraries)

        val beneficiaryIds = intent.getIntArrayExtra("BENEFICIARY_IDS")

        val stateName = intent.getStringExtra("STATE")
        val districtName = intent.getStringExtra("DISTRICT")
        val stateID = intent.getStringExtra("STATE_ID")
        val districtID = intent.getStringExtra("DISTRICT_ID")


        state = findViewById(R.id.state)
        district = findViewById(R.id.district)
        back = findViewById(R.id.back)
        librariesRecyclerView = findViewById(R.id.librariesRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        progressBar2 = findViewById(R.id.progressBar2)

        state.text = stateName
        district.text = districtName

        back.setOnClickListener {
            super.onBackPressed()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://nodenote.cyclic.cloud/") // Adjust the base URL accordingly
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        val call = service.getLibraries()

        call.enqueue(object : Callback<List<Library>> {
            override fun onResponse(call: Call<List<Library>>, response: Response<List<Library>>) {
                if (response.isSuccessful) {
                    val libraries = response.body()

                    // Filter libraries based on stateID and districtID
                    if (beneficiaryIds != null) {
                        val filteredLibraries = libraries?.filter {
                            it.beneficiary_state == stateID!!.toInt() && it.beneficiary_district == districtID!!.toInt() &&
                                    it.beneficiary_id.toInt() in beneficiaryIds
                        }

                        // Set up RecyclerView and LibraryAdapter
                        val layoutManager = LinearLayoutManager(this@OpenLibraries)
                        librariesRecyclerView.layoutManager = layoutManager

                        filteredLibraries?.let {
                            if (it.isEmpty()) {
                                progressBar2.visibility = View.VISIBLE
                                progressBar.visibility = View.GONE
                            } else {
                                // Data is not empty, proceed with setting up the adapter
                                val libraryAdapter = LibraryAdapter(this@OpenLibraries, it)
                                librariesRecyclerView.adapter = libraryAdapter
                                progressBar.visibility = View.GONE

                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<Library>>, t: Throwable) {
                // Handle failure (e.g., network issues, server errors)
            }
        })
    }
}
