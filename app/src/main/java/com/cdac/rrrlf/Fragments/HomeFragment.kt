package com.cdac.rrrlf.Fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cdac.rrrlf.Activities.OpenLibraries
import com.cdac.rrrlf.Data.District
import com.cdac.rrrlf.R
import com.cdac.rrrlf.Services.ApiService
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var apiService: ApiService
    private lateinit var dialog: Dialog
    private var districtList: List<District> = emptyList()
    private var selectedStateId: Int? = 0
    private var selectedDistrictId: Int? = 0
    private var selectedStateName: String? = ""
    private var selectedDistrict: String? = ""
    private var selectedFinancialYear: String? = "2023-2024"
    private var matchingBeneficiaryIds: List<Int> = emptyList()

    private lateinit var address: TextView
    private lateinit var city: TextView
    private lateinit var autoCompleteFY: AutoCompleteTextView
    private lateinit var autocompleteState: AutoCompleteTextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var state: TextView
    private lateinit var district: TextView
    private lateinit var open: TextView
    private lateinit var layout: LinearLayout
    private lateinit var mainLayout: LinearLayout

    val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
    private val REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val window: Window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.orange)

        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        setupUIReferences(view)
        getLastLocation()
        showState()

        return view
    }

    override fun onResume() {
        super.onResume()
        getLastLocation()
    }

    private fun setupUIReferences(view: View) {
        address = view.findViewById(R.id.address)
        city = view.findViewById(R.id.city)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        autoCompleteFY = view.findViewById(R.id.autoCompleteFY)
        autocompleteState = view.findViewById(R.id.autoCompleteTextView)
        layout = view.findViewById(R.id.layout)
        mainLayout = view.findViewById(R.id.mainLayout)
        shimmerLayout = view.findViewById(R.id.shimmer_view_container)
        progressBar = view.findViewById(R.id.progressBar)
        state = view.findViewById(R.id.state)
        district = view.findViewById(R.id.district)
        open = view.findViewById(R.id.view)

        state.text = selectedStateName
        district.text = selectedDistrict

        shimmerLayout.startShimmer()

        openButton()

        val financial_year = resources.getStringArray(R.array.financial_year)
        val arrayAdapter1 = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            financial_year
        )

        autoCompleteFY.setAdapter(arrayAdapter1)

        autoCompleteFY.setOnClickListener {
            val financial_year = resources.getStringArray(R.array.financial_year)
            val arrayAdapter1 = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                financial_year
            )
            autoCompleteFY.setAdapter(arrayAdapter1)
            autoCompleteFY.requestFocus()
            autoCompleteFY.showDropDown()
        }

        autoCompleteFY.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                selectedFinancialYear = s.toString().trim()
                storeSelectedFinancialYear(selectedFinancialYear)
                checkState()
            }
        })
    }

    private fun openButton() {
        open.setOnClickListener {
            if (open.text == "Continue") {
                handleContinueButtonClick()
            } else if (open.text == "View Libraries") {
                handleViewLibrariesButtonClick()
            }
        }

    }

    private fun handleContinueButtonClick() {
        if (state.text.isNullOrBlank()) {
            showToast("Please select state")
        } else {
            progressBar.visibility = View.VISIBLE
            open.visibility = View.GONE
            Handler().postDelayed({
                progressBar.visibility = View.GONE
                open.visibility = View.VISIBLE
                layout.visibility = View.VISIBLE
                open.text = "View Libraries"
            }, 1000)
        }
    }

    private fun handleViewLibrariesButtonClick() {
        if (district.text.isNullOrBlank()) {
            showToast("Please select district")
        } else {
            val intent = Intent(activity, OpenLibraries::class.java)
            intent.putExtra("DISTRICT_ID", selectedDistrictId.toString())
            intent.putExtra("STATE_ID", selectedStateId.toString())
            intent.putExtra("STATE", selectedStateName)
            intent.putExtra("DISTRICT", selectedDistrict)
            intent.putExtra("FY", selectedDistrict)

            intent.putExtra("BENEFICIARY_IDS", matchingBeneficiaryIds.toIntArray())

            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun storeSelectedFinancialYear(financialYear: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("SELECTED_FINANCIAL_YEAR", financialYear)
        editor.apply()
    }

    private fun showState() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nodenote.cyclic.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stateList = apiService.getStates()

                val stateNames = stateList.map { it.state_name }
                val stateIds = stateList.map { it.state_id }

                val arrayAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    stateNames
                )
                autocompleteState.setAdapter(arrayAdapter)

                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                mainLayout.visibility = View.VISIBLE

                autocompleteState.setOnItemClickListener { _, _, position, _ ->
                    selectedStateId = stateIds[position]
                    selectedStateName = stateNames[position]
                    state.text = stateNames[position]

                    selectedDistrict = ""
                    district.text = selectedDistrict

                    checkState()
                }
            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
            }
        }
    }

    private fun checkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val allData = apiService.getAllData()

                val financialYear = selectedFinancialYear
                val stateId = selectedStateId

                val matchingEntries = allData.filter { entry ->
                    entry.financial_year_of_selection == financialYear && entry.state_id == stateId
                }

                matchingBeneficiaryIds = matchingEntries.map { it.beneficiary_id }

                showDistrictDialog()
            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
            }
        }
    }

    private fun showDistrictDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                districtList = withContext(Dispatchers.IO) {
                    apiService.getDistricts()
                }

                val filteredDistricts = districtList.filter { it.state_id == selectedStateId }
                val districtNames = filteredDistricts.map { it.district_name }

                val adapter2 = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    districtNames
                )

                layout.setOnClickListener {
                    if (districtList.isNotEmpty()) {
                        dialog = Dialog(requireContext())
                        dialog.setContentView(R.layout.dialog_searchable_spinner)
                        dialog.window?.setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()

                        val editText = dialog.findViewById<EditText>(R.id.edit_text)
                        val listView = dialog.findViewById<ListView>(R.id.list_view)

                        listView.adapter = adapter2

                        editText.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                adapter2.filter.filter(s)
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        })

                        listView.setOnItemClickListener { _, _, position, _ ->
                            selectedDistrictId = filteredDistricts[position].district_id
                            selectedDistrict = filteredDistricts[position].district_name
                            district.text = selectedDistrict

                            dialog.dismiss()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "District data not available for the selected state. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Please provide the required permission",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {

                }
            }
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        try {
                            val geocoder = Geocoder(requireActivity(), Locale.getDefault())
                            val addresses: List<Address>? =
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)

                            address.text = addresses!![0].locality + ", " + addresses[0].adminArea
                            city.text = addresses[0].postalCode

                            val value = addresses[0].latitude.toString().trim { it <= ' ' }
                            val value2 = addresses[0].longitude.toString().trim { it <= ' ' }
                            if (isAdded) {
                                val sharedPref = requireActivity().getSharedPreferences(
                                    "myKey",
                                    Context.MODE_PRIVATE
                                )
                                val editor = sharedPref.edit()
                                editor.putFloat("lat", value.toFloat())
                                editor.putFloat("lon", value2.toFloat())
                                editor.apply()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
        } else {
            askPermission()
        }
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE
        )
    }
}
