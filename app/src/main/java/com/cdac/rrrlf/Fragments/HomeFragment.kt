package com.cdac.rrrlf.Fragments

import android.Manifest
import android.app.AlertDialog
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
import com.airbnb.lottie.LottieAnimationView
import com.cdac.rrrlf.Data.District
import com.cdac.rrrlf.Activities.OpenLibraries
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

    private var matchingBeneficiaryIds: List<Int> = emptyList()

    private lateinit var sharedPreferences: SharedPreferences

    val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val REQUEST_CODE = 100

    private lateinit var apiService: ApiService
    private lateinit var dialog: Dialog
    private var districtList: List<District> = emptyList()

    private var selectedStateId: Int? = 0
    private var selectedDistrictId: Int? = 0
    private var selectedStateName: String? = ""
    private var selectedDistrict: String? = ""
    private var selectedFinancialYear: String? = "2023-2024"


    lateinit var address: TextView
    lateinit var city: TextView
    lateinit var state: TextView
    lateinit var district: TextView
    lateinit var open: TextView
    lateinit var layout: LinearLayout
    lateinit var mainLayout: LinearLayout
    lateinit var autoCompleteFY: AutoCompleteTextView
    lateinit var autocompleteState: AutoCompleteTextView
    lateinit var shimmerLayout: ShimmerFrameLayout
    lateinit var progressBar: ProgressBar

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val window: Window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.orange)


        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        address = view.findViewById(R.id.address)
        city = view.findViewById(R.id.city)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        getLastLocation()

        autocompleteState = view.findViewById(R.id.autoCompleteTextView)
        autoCompleteFY = view.findViewById(R.id.autoCompleteFY)
        layout = view.findViewById(R.id.layout)
        mainLayout = view.findViewById(R.id.mainLayout)
        shimmerLayout = view.findViewById(R.id.shimmer_view_container)
        progressBar = view.findViewById(R.id.progressBar)

        state = view.findViewById(R.id.state)
        district = view.findViewById(R.id.district)
        open = view.findViewById(R.id.view)

        state.text = selectedStateName
        district.text = selectedDistrict

        shimmerLayout.startShimmer();

//        if (state.text.isNullOrBlank()) {
//            layout.alpha = 0.3f
//        }

//        if (state.text.isNullOrBlank()) {
//            open.alpha = 0.5f
//        }

//        layout.setOnClickListener {
//            if (state.text.isNullOrBlank()) {
//                Toast.makeText(activity, "Please Select State", Toast.LENGTH_SHORT).show()
//            }
//        }

        showState()


        open.setOnClickListener {

            if (open.text == "Continue") {
                if (state.text.isNullOrBlank()) {
                    Toast.makeText(activity, "Please select state", Toast.LENGTH_SHORT).show()
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
            } else if (open.text == "View Libraries") {
                if (district.text.isNullOrBlank()) {
                    Toast.makeText(activity, "Please select district", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(activity, OpenLibraries::class.java)
                    intent.putExtra("DISTRICT_ID", selectedDistrictId.toString())
                    intent.putExtra("STATE_ID", selectedStateId.toString())
                    intent.putExtra("STATE", selectedStateName)
                    intent.putExtra("DISTRICT", selectedDistrict)
                    intent.putExtra("FY", selectedDistrict)

                    // Pass the matchingBeneficiaryIds array to the next screen
                    intent.putExtra("BENEFICIARY_IDS", matchingBeneficiaryIds.toIntArray())

                    startActivity(intent)
                }
            }
        }


        val financial_year = resources.getStringArray(R.array.financial_year)
        val arrayAdapter1 = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            financial_year
        )

        // Set the adapter to the AutoCompleteTextView
        autoCompleteFY.setAdapter(arrayAdapter1)

        autoCompleteFY.setOnClickListener {

            val financial_year = resources.getStringArray(R.array.financial_year)
            val arrayAdapter1 = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                financial_year
            )

            // Set the adapter to the AutoCompleteTextView
            autoCompleteFY.setAdapter(arrayAdapter1)
            // Request focus and show the dropdown
            autoCompleteFY.requestFocus()
            autoCompleteFY.showDropDown()

        }

        autoCompleteFY.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Update the selected financial year based on user input
                selectedFinancialYear = s.toString().trim()
                // You can use selectedFinancialYear as needed

                storeSelectedFinancialYear(selectedFinancialYear)

                checkState()

            }
        })


        return view
    }

    override fun onResume() {
        super.onResume()
        getLastLocation()
    }

    private fun storeSelectedFinancialYear(financialYear: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("SELECTED_FINANCIAL_YEAR", financialYear)
        editor.apply()
    }

    private fun showState() {

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nodenote.cyclic.cloud/") // Replace with your API base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create ApiService instance
        apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stateList = apiService.getStates()

                // Extract state names from the list
                val stateNames = stateList.map { it.state_name }
                val stateIds = stateList.map { it.state_id }

                // Create and set the ArrayAdapter with the fetched state names
                val arrayAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    stateNames
                )
                autocompleteState.setAdapter(arrayAdapter)

                shimmerLayout.stopShimmer();
                shimmerLayout.visibility = View.GONE
                mainLayout.visibility = View.VISIBLE

                autocompleteState.setOnItemClickListener { _, _, position, _ ->
                    // Retrieve the selected state_id based on the selected position
                    selectedStateId = stateIds[position]
                    selectedStateName = stateNames[position]
                    state.text = stateNames[position]

                    selectedDistrict = ""
                    district.text = selectedDistrict

                    checkState()


                }

            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
                // Handle error
//                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch data from the new API
                val allData = apiService.getAllData()

                // Extract financial year and state id from the fetched data
                val financialYear =
                    selectedFinancialYear // Replace with the actual selected financial year
                val stateId = selectedStateId // Replace with the actual selected state id

                // Find all matching entries in the data
                val matchingEntries = allData.filter { entry ->
                    entry.financial_year_of_selection == financialYear && entry.state_id == stateId
                }

                // Collect beneficiary_id values for matching entries into an array
                matchingBeneficiaryIds = matchingEntries.map { it.beneficiary_id }

                layout.alpha = 1.0f
                showDistrictDialog()

                // If at least one matching entry is found, set the alpha of the layout to 1
//                if (matchingBeneficiaryIds.isNotEmpty()) {
//                    layout.alpha = 1.0f
//                    showDistrictDialog()
//
//                    // Pass matchingBeneficiaryIds to the next screen using Intent or ViewModel
//                    // Example: startActivity(intent.putExtra("beneficiaryIds", matchingBeneficiaryIds.toIntArray()))
//
//                } else {
//                    layout.alpha = 0.3f
//                    layout.setOnClickListener {
//                        showNoBeneficiaryDialog()
//                    }
//                }

            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
                // Handle error
//                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showNoBeneficiaryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("No Beneficiary Found")
        builder.setMessage("No beneficiary found in $selectedStateName for $selectedFinancialYear")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun showDistrictDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                districtList = withContext(Dispatchers.IO) {
                    apiService.getDistricts()
                }

                // Filter districts based on the selected stateId
                val filteredDistricts = districtList.filter { it.state_id == selectedStateId }

                // Extract district names from the filtered list
                val districtNames = filteredDistricts.map { it.district_name }

                // Initialize array adapter with the filtered district names
                val adapter2 = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    districtNames
                )

                layout.setOnClickListener {
                    // Check if districtList is not empty before showing the dialog
                    if (districtList.isNotEmpty()) {
                        // Initialize dialog
                        dialog = Dialog(requireContext())

                        // Set custom dialog
                        dialog.setContentView(R.layout.dialog_searchable_spinner)

                        // Set custom height and width
                        dialog.window?.setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Set transparent background
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                        // Show dialog
                        dialog.show()

                        // Initialize and assign variables
                        val editText = dialog.findViewById<EditText>(R.id.edit_text)
                        val listView = dialog.findViewById<ListView>(R.id.list_view)

                        // Set adapter
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
                            // When item selected from list
                            // Set selected district information
                            selectedDistrictId = filteredDistricts[position].district_id
                            selectedDistrict = filteredDistricts[position].district_name
                            district.text = selectedDistrict

//                            if (!district.text.isNullOrBlank()) {
//                                open.alpha = 1.0f
//                            }
                            // Handle the selected district as needed

                            // Dismiss dialog
                            dialog.dismiss()
                        }
                    } else {
                        // Notify the user that district data is not available
                        Toast.makeText(
                            requireContext(),
                            "District data not available for the selected state. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("APIError", "Error fetching data", e)
                // Handle error
//                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

                            address.text =
                                addresses!![0].locality + ", " + addresses[0].adminArea

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