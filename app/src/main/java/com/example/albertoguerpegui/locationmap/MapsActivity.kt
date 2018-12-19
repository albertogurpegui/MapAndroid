package com.example.albertoguerpegui.locationmap

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions




class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

    companion object {
        private const val PLACE_PICKER_REQUEST = 1
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mLocationPermissionGranted = false
    private var sydney = LatLng(-34.0, 151.0)
    private var mLastKnownLocation: Location? = null
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        spinner = findViewById<Spinner>(R.id.types_spinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.types_map, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        fab.setOnClickListener {
            loadPlacePicker()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapFragment.getMapAsync(this)
    }

    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()
        try {
            startActivityForResult(builder.build(this@MapsActivity), PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PLACE_PICKER_REQUEST){
            data?.let {
                val place = PlacePicker.getPlace(this,data)
                addMapMarker(place.latLng, place.name.toString())
            }
        }
    }

    private fun addMapMarker(posicion: LatLng, name: String, zoom: Float = 12f) {
        map.addMarker(MarkerOptions().position(posicion).title(name))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom((posicion), zoom))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.let {
            map.uiSettings.isZoomControlsEnabled = true
            addMapMarker(sydney, "Sydney")
            updateLocationUI()
        }
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                map.mapType = when(position){
                    0->
                        GoogleMap.MAP_TYPE_SATELLITE
                    1->
                        GoogleMap.MAP_TYPE_TERRAIN
                    2->
                        GoogleMap.MAP_TYPE_HYBRID
                    else ->
                        GoogleMap.MAP_TYPE_NORMAL
                }
            }
        }
    }



    private fun updateLocationUI() {
        try {
            if (mLocationPermissionGranted) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
                getDeviceLocation()
            }
            else {
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                //sydney = null
                getLocationPermission()
            }
        }
        catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }


    private fun getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Log.d("MapLocation", "Se pide ultima localizacion")
                fusedLocationClient.lastLocation.addOnCompleteListener {
                    if (it.isSuccessful) {
                        mLastKnownLocation = it.result
                        Log.d("MapLocation", "Ultima localizacion cargada" + mLastKnownLocation?.toString())

                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                mLastKnownLocation?.let { location ->
                                    LatLng(location.latitude,
                                        location.longitude)
                                } ?: kotlin.run {
                                    sydney
                                }, 10f)
                        )
                    } else {
                        Log.d("error", "Current location is null. Using defaults.")
                        Log.e("exception", "Exception: %s", it.exception)
                        map.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        }
        catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }



    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
            updateLocationUI()
        }
        else {
            mLocationPermissionGranted = false
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
                    updateLocationUI()
                }else {
                    Toast.makeText(this, "DEBES ACEPTAR LOS PERMISOS", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
