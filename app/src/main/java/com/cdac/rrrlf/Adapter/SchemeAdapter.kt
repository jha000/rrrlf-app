package com.cdac.rrrlf.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cdac.rrrlf.Data.AllData
import com.cdac.rrrlf.Data.Scheme
import com.cdac.rrrlf.R
import com.cdac.rrrlf.Services.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SchemeAdapter(private val dataList: List<AllData>) : RecyclerView.Adapter<SchemeAdapter.SchemeViewHolder>() {

    class SchemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val schemeCode: TextView = itemView.findViewById(R.id.code)
        val scheme: TextView = itemView.findViewById(R.id.scheme)
        val applied_Amount: TextView = itemView.findViewById(R.id.applied_Amount)
        val applied_gst: TextView = itemView.findViewById(R.id.applied_gst)
        val recommended_GST: TextView = itemView.findViewById(R.id.recommended_GST)
        val recommended_Amount: TextView = itemView.findViewById(R.id.recommended_Amount)
        val total_GST: TextView = itemView.findViewById(R.id.total_GST)
        val total_Amount: TextView = itemView.findViewById(R.id.total_Amount)
        val status: TextView = itemView.findViewById(R.id.status)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_scheme, parent, false)
        return SchemeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SchemeViewHolder, position: Int) {
        val currentItem = dataList[position]

        holder.schemeCode.text = currentItem.scheme_code
        holder.applied_gst.text = currentItem.applied_gst.toString()
        holder.applied_Amount.text = currentItem.total_applied_amount.toString()
        holder.recommended_GST.text = currentItem.recommended_gst.toString()
        holder.recommended_Amount.text = currentItem.total_recommended_amount.toString()
        holder.total_GST.text = currentItem.sanctioned_gst.toString()
        holder.total_Amount.text = currentItem.total_sanctioned_amount.toString()
        holder.status.text = currentItem.sanctioned_remarks

        if (currentItem.sanctioned_remarks.equals("Approved", ignoreCase = true)) {
            // Status is Approved, set background color to green
            holder.status.setBackgroundColor(Color.parseColor("#039487"))
        } else {
            // Status is not Approved, set background color to red
            holder.status.setBackgroundColor(Color.parseColor("#f7cb73"))

            // Update status text to "Pending"
            holder.status.text = "Pending"
        }

        // Fetch scheme name by scheme code from the API
        fetchSchemeNameByCode(currentItem.scheme_code) { schemeName ->
            // Update the scheme name in the TextView
            holder.scheme.text = schemeName
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    // Implement this function to fetch scheme name by scheme code
    private fun fetchSchemeNameByCode(schemeCode: String, callback: (String) -> Unit) {
        // Replace "http://10.240.13.110:3000/" with your actual base URL
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nodenote.cyclic.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Make the network request using Retrofit's enqueue
        apiService.getSchemes().enqueue(object : Callback<List<Scheme>> {
            override fun onResponse(call: Call<List<Scheme>>, response: Response<List<Scheme>>) {
                if (response.isSuccessful) {
                    // Filter the schemeList by schemeCode
                    val filteredScheme = response.body()?.firstOrNull { it.scheme_code == schemeCode }

                    // Invoke the callback with the scheme name
                    callback(filteredScheme?.scheme_name ?: "Scheme Not Found")
                } else {
                    // Handle unsuccessful response
                    callback("Error Fetching Scheme")
                }
            }

            override fun onFailure(call: Call<List<Scheme>>, t: Throwable) {
                // Handle network request failure
                callback("Error Fetching Scheme")
            }
        })
    }
}
