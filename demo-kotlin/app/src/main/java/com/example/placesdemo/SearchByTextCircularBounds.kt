package com.example.placesdemo

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.placesdemo.databinding.CurrentPlaceActivityBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest

/**
 * modified from CurrentPlaceActivity.kt
 */
class SearchByTextCircularBounds : AppCompatActivity() {
    private lateinit var placesClient: PlacesClient
    private lateinit var fieldSelector: FieldSelector

    private lateinit var binding: CurrentPlaceActivityBinding

    @SuppressLint("MissingPermission")
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[permission.ACCESS_FINE_LOCATION] == true || permissions[permission.ACCESS_COARSE_LOCATION] == true -> {
                    // Only approximate location access granted.
                    findCurrentPlaceWithPermissions()
                }
                else -> {
                    Toast.makeText(
                        this,
                        "Either ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permissions are required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CurrentPlaceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve a PlacesClient (previously initialized - see MainActivity)
        placesClient = Places.createClient(this)

        // Set view objects
        // Exclude fields that are not supported by search endpoints
        val placeFields = FieldSelector.allExcept(
            Place.Field.ADDRESS_COMPONENTS,
            Place.Field.CURBSIDE_PICKUP,
            Place.Field.CURRENT_OPENING_HOURS,
            Place.Field.DELIVERY,
            Place.Field.DINE_IN,
            Place.Field.EDITORIAL_SUMMARY,
            Place.Field.OPENING_HOURS,
            Place.Field.PHONE_NUMBER,
            Place.Field.RESERVABLE,
            Place.Field.SECONDARY_OPENING_HOURS,
            Place.Field.SERVES_BEER,
            Place.Field.SERVES_BREAKFAST,
            Place.Field.SERVES_BRUNCH,
            Place.Field.SERVES_DINNER,
            Place.Field.SERVES_LUNCH,
            Place.Field.SERVES_VEGETARIAN_FOOD,
            Place.Field.SERVES_WINE,
            Place.Field.TAKEOUT,
            Place.Field.UTC_OFFSET,
            Place.Field.WEBSITE_URI,
            Place.Field.WHEELCHAIR_ACCESSIBLE_ENTRANCE
        )
        fieldSelector = FieldSelector(
            binding.useCustomFields,
            binding.customFieldsList,
            savedInstanceState,
            placeFields
        )
        setLoading(false)

        // Set listeners for programmatic Find Current Place
        binding.findCurrentPlaceButton.setOnClickListener {
            //findCurrentPlace()
            findrestaurant()
        }
    }


    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        fieldSelector.onSaveInstanceState(bundle)
    }

    /**
     * Check whether permissions have been granted or not, and ultimately proceeds to either
     * request them or runs {@link #findCurrentPlaceWithPermissions() findCurrentPlaceWithPermissions}
     */
    @SuppressLint("MissingPermission")
    private fun findCurrentPlace() {
        if (hasOnePermissionGranted(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION)) {
            findCurrentPlaceWithPermissions()
            return
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    permission.ACCESS_FINE_LOCATION,
                    permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Fetches a list of [com.google.android.libraries.places.api.model.PlaceLikelihood] instances that represent the Places the user is
     * most likely to be at currently.
     */
    @RequiresPermission(allOf = [permission.ACCESS_FINE_LOCATION, permission.ACCESS_WIFI_STATE])
    private fun findCurrentPlaceWithPermissions() {
        setLoading(true)
        val currentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        val currentPlaceTask = placesClient.findCurrentPlace(currentPlaceRequest)
        currentPlaceTask.addOnSuccessListener { response: FindCurrentPlaceResponse? ->
            response?.let {
                binding.response.text = StringUtil.stringify(it, isDisplayRawResultsChecked)
            }
        }
        currentPlaceTask.addOnFailureListener { exception: Exception ->
            exception.printStackTrace()
            binding.response.text = exception.message
        }
        currentPlaceTask.addOnCompleteListener { setLoading(false) }
    }

    private fun findrestaurant() {
        // issue a search request around downtown vancouver

        val placesClient = Places.createClient(this)

        val searchCenter = LatLng(49.2820 , -123.1171)
        val searchByTextRequest = SearchByTextRequest.builder("Spicy Vegetarian Food", placeFields)
            .setMaxResultCount(5)
            .setLocationRestriction(CircularBounds.newInstance(searchCenter, 500.0)).build()

        placesClient.searchByText(searchByTextRequest)
            .addOnSuccessListener { response ->
                val places: List<Place> = response.places
                var restaurant_list = ""
                for (place in places) {
                    restaurant_list += place.name +", "
                }
                binding.response.text = restaurant_list

            }
    }

    //////////////////////////
    // Helper methods below //
    //////////////////////////
    private val placeFields: List<Place.Field>
        get() = if (binding.useCustomFields.isChecked) {
            fieldSelector.selectedFields
        } else {
            fieldSelector.allFields
        }

    private val isDisplayRawResultsChecked: Boolean
        get() = binding.displayRawResults.isChecked

    private fun setLoading(loading: Boolean) {
        binding.loading.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun hasOnePermissionGranted(vararg permissions: String): Boolean =
        permissions.any {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
}