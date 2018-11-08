package location.detailed.com.mydetailedlocation

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<LocationSettingsResult> {

    private lateinit var mMap: GoogleMap
    private val REQUEST_CODE: Int = 101
    private val REQUEST_CHECK_SETTINGS: Int = 102
    private var locationListener: LocationListener? = null
    private var googleApiClient: GoogleApiClient? = null
    private var slidingUpPanelLayout: SlidingUpPanelLayout?=null
    private var mainContent: TextView? = null
    private var clipToBoard: Button? = null
    private var shareLocation: Button? = null
    private var lat : Double? = null
    private var lon: Double? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        slidingUpPanelLayout = findViewById(R.id.sliding_layout) as SlidingUpPanelLayout?
        mainContent = findViewById(R.id.main_content) as TextView?
        clipToBoard = findViewById(R.id.clipboard) as Button?
        shareLocation = findViewById(R.id.shareLoc) as Button?

        displayLocationSettingsRequest()
        getCurrentLocation()
        performSlidingOperation()
        buttonsLogic()
    }

    private fun buttonsLogic() {

        clipToBoard!!.setOnClickListener {
            var clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var clip: ClipData = ClipData.newPlainText("Your location", mainContent!!.text)
            clipboard.primaryClip = clip
            Toast.makeText(this@MapsActivity,"Copied to clipboard.",Toast.LENGTH_SHORT).show()
        }

        shareLocation!!.setOnClickListener {
            var uri: String = "geo:" + lat!! + "," +lon!! + "?q=" + lat!! +"," + lon!!
            startActivity(Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse(uri)))
        }

    }

    private fun performSlidingOperation() {

        slidingUpPanelLayout!!.setFadeOnClickListener {
            slidingUpPanelLayout!!.panelState= SlidingUpPanelLayout.PanelState.COLLAPSED }

    }

    private fun displayLocationSettingsRequest() {
        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API).build()
        googleApiClient!!.connect()

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = (10000 / 2).toLong()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                }


                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {

                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        status.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                    } catch (e: IntentSender.SendIntentException) {
                    }
                }

                else -> {
                }
            }
        }
    }

    private fun getCurrentLocation() {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            requestLocationUpdate(locationManager,LocationManager.NETWORK_PROVIDER)

        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            requestLocationUpdate(locationManager,LocationManager.GPS_PROVIDER)

        }
    }

    private fun requestLocationUpdate(locationManager:LocationManager,provider:String){

        locationListener = object : LocationListener {
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {
            }

            override fun onProviderDisabled(provider: String?) {
            }

            override fun onLocationChanged(location: Location?) {

                var latitude = location!!.latitude
                var longitude = location!!.longitude

                lat=latitude
                lon=longitude

                val latLng = LatLng(latitude, longitude)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13F))

                var cameraPosition = CameraPosition.Builder()
                        .target(latLng)      // Sets the center of the map to location user
                        .zoom(17F)                   // Sets the zoom
                        .bearing(90F)                // Sets the orientation of the camera to east
                        .tilt(40F)                   // Sets the tilt of the camera to 30 degrees
                        .build()                // Creates a CameraPosition from the builder

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                var geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
                var addresses: MutableList<Address>? = geocoder.getFromLocation(latitude,longitude,1)

                var address = addresses!![0].getAddressLine(0)
                var city = addresses!![0].locality
                var state = addresses!![0].adminArea
                var country = addresses!![0].countryName
                var postalCode = addresses!![0].postalCode
                var knownName = addresses!![0].featureName

                mMap.clear()
                mMap.addMarker(MarkerOptions().position(latLng).title(address))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))

                mainContent!!.setText("Address: "+address+"\n\n"+
                "City: "+city+"\n\n"+
                "State: "+state+"\n\n"+
                "Country: "+country+"\n\n"+
                "Postal code: "+postalCode+"\n\n"+
                "Known name: "+knownName)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(provider, 0,
                        0f, locationListener)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE)
            }
        }
        else{
            locationManager.requestLocationUpdates(provider, 0,
                    0f, locationListener)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {

                if (requestCode == REQUEST_CODE) {
                    if (grantResults.size == 1
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        mMap.isMyLocationEnabled = true
                        getCurrentLocation()
                    } else {
                        // Permission was denied or request was cancelled
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        /*mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
        mMap.uiSettings.isMapToolbarEnabled = false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
            }
            else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val states: LocationSettingsStates = LocationSettingsStates.fromIntent(data);
        when (requestCode) {
            REQUEST_CHECK_SETTINGS ->
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {

                                getCurrentLocation()

                            } else {
                                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                                        REQUEST_CODE)
                            }
                        }
                        else{
                            getCurrentLocation()
                        }
                    }

                    Activity.RESULT_CANCELED ->{
                        finish()
                    }
                }
        }

    }


    override fun onConnected(p0: Bundle?) {
    }


    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onLocationChanged(location: Location?) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    override fun onResult(p0: LocationSettingsResult) {
    }

}
