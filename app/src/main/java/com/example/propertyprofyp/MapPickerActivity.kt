package com.example.propertyprofyp
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.fragment.app.FragmentActivity
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.OnMapReadyCallback
//import com.google.android.gms.maps.SupportMapFragment
//import com.google.android.gms.maps.model.LatLng
//import com.example.propertyprofyp.databinding.ActivityMapPickerBinding
//
class MapPickerActivity {
//
//    private lateinit var mMap: GoogleMap
//    private lateinit var binding: ActivityMapPickerBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMapPickerBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        val mapFragment = supportFragmentManager
//            .findFragmentById(R.id.map) as SupportMapFragment
//        mapFragment.getMapAsync(this)
//    }
//
//    override fun onMapReady(googleMap: GoogleMap) {
//        mMap = googleMap
//
//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//
//        // Set a map click listener
//        mMap.setOnMapClickListener { latLng ->
//            val returnIntent = Intent()
//            returnIntent.putExtra("location", "${latLng.latitude}, ${latLng.longitude}")
//            setResult(RESULT_OK, returnIntent)
//            finish()
//        }
//    }
}
