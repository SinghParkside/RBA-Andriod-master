package edu.uwp.appfactory.wishope.views.map

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterManager
import edu.uwp.appfactory.wishope.R
import edu.uwp.appfactory.wishope.utils.UserConstants
import edu.uwp.appfactory.wishope.views.calling.IncomingCallActivity
import edu.uwp.appfactory.wishope.views.landing.RecovereeLandingActivity
import kotlinx.android.synthetic.main.fragment_map.*

/**
 * <h1>.</h1>
 * <p>.</p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener, LocationListener,
    ClusterManager.OnClusterItemClickListener<LocationClusterItem> {

    /**
     * Bounding information for the map.
     */
    private var mMap: GoogleMap? = null
    private val northeast = LatLng(46.947762, -86.616211)
    private val southwest = LatLng(42.679743, -92.625732)
    private val boundingBox = LatLngBounds(southwest, northeast)
    val PERMISSION_ID = 42
    private var location: Location? = null
    private var lat = 0.0
    private var lng = 0.0
    private var mLocationManager: LocationManager? = null

    private val db = FirebaseFirestore.getInstance() //Get the Fire Store DB
    private lateinit var mClusterManager: ClusterManager<LocationClusterItem>//Cluster manager
    private var locationItems: ArrayList<LocationClusterItem> =
        arrayListOf()//Master list of all location items

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private val incomingCallBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Here you can refresh your listview or other UI
            // Change activity/fragment
            startActivity(Intent(requireContext(), IncomingCallActivity::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        // Remove the broadcast receiver
        requireContext().unregisterReceiver(incomingCallBroadcastReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            } else {
                if (UserConstants.ROLE == "recoveree") {
                    startActivity(
                        Intent(
                            requireActivity(),
                            RecovereeLandingActivity::class.java
                        )
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mLocationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        lat = location.latitude
        lng = location.longitude
        this.location = location
    }

    override fun onStatusChanged(s: String?, i: Int, bundle: Bundle?) {

    }

    override fun onProviderEnabled(s: String?) {}

    override fun onProviderDisabled(s: String?) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_ID
            )
        }
        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)
        map.onResume()

        //Set up map
        try {
            MapsInitializer.initialize(requireActivity())
            map.getMapAsync(this)

        } catch (e: Exception) {
            Log.e(MapFragment::class.java.name, "${e.message}")
        }

        //Allows different states to be applied to bottom sheet
//        resourceToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
//        residentialToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
//        homelessToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
//        outpatientToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
//        adolescentToggle.setOnClickListener {
//            //Pass args (Strings of "type" field for locations) to addItems
//            //Whenever one of these buttons is clicked, it updates what items
//            //  should be added to the map, by updating filters
//            addItems(getFilters())
//        }
//
//        soberToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
//        noTypeToggle.setOnClickListener {
//            addItems(getFilters())
//        }
//
        filterButton.setOnClickListener {
            filterLayer.visibility = View.VISIBLE
        }

        filterLayoutClose.setOnClickListener {
            filterLayer.visibility = View.GONE
        }

        recoveryLocation.setOnClickListener(View.OnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_ID
                )
            }
            mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
            try {
                val latLng = LatLng(location!!.latitude, location!!.longitude)
                val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                mMap?.animateCamera(cameraUpdate)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        })

        websiteTextView.setOnClickListener { view: View? ->
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(websiteTextView.text.toString())
            )
            startActivity(browserIntent)
        }


        //Map sends current tab info to the pop up window
        reportTextView.setOnClickListener{view: View? ->
            val bund = bundleOf("Address" to addressTextView.text.toString(), "Phone" to phoneTextView.text.toString(), "Web" to websiteTextView.text.toString(),"Type" to markerTypeTextView.text.toString(), "Title" to titleTextView.text.toString())
            val intent = Intent(
                this.context,
                PopUpWindow::class.java
            )
            intent.putExtras(bund)
            startActivity(intent)
        }

        addressTextView.setOnClickListener { view: View? ->
            val gmmIntentUri =
                Uri.parse("geo:0,0?q=" + addressTextView.text.toString().replace("[\n\t ]", ""))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        phoneTextView.setOnClickListener { view: View? ->
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + phoneTextView.text.toString())
            startActivity(intent)
        }

        applyFilters.setOnClickListener(View.OnClickListener {
            addItems(getFilters())
            filterLayer.visibility = View.GONE
        })
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "Map"//Set the fragment title
        requireContext().registerReceiver(
            incomingCallBroadcastReceiver,
            IntentFilter("incomingCall")
        )

    }

    /**
     * Initializes the map and sets up constraints on zoom and bounding box
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            mMap = it
//            it.setLatLngBoundsForCameraTarget(boundingBox)
            it.setMinZoomPreference(6.5f)
            it.setMaxZoomPreference(20.0f)
            it.uiSettings.isMyLocationButtonEnabled = false

            it.moveCamera(CameraUpdateFactory.zoomTo(6.5f))
            it.moveCamera(CameraUpdateFactory.newLatLng(boundingBox.center))
        }
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap?.isMyLocationEnabled = true
        setUpClusterer()
    }


    /**
     * Sets up the map cluster manager
     */
    private fun setUpClusterer() {
        mClusterManager = ClusterManager(this.activity, mMap)
        val customRenderer = CustomClusterRenderer(this.view!!.context, mMap, mClusterManager)
        mClusterManager.renderer = customRenderer
        mMap?.setOnCameraIdleListener(mClusterManager)
        mClusterManager.setOnClusterItemClickListener(this)
        mMap?.setOnMarkerClickListener(mClusterManager)
        addItems()
    }

    /**
     * Adds all map pins in database to the map and clusters them
     */
    private fun addItems() {
        db.collection("map_data")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var lat = document.get("latitude") as String
                    var long = document.get("longitude") as String
                    var title = document!!.get("name")
                    var snippet = document.get("website")
                    var type = document.get("type")
                    var phone = document.get("phone")
                    var address = document.get("address").toString() + " " + document.get("city")
                        .toString() + " " + document.get(
                        "state"
                    ).toString() + " " + document.get("zip").toString()


                    var clustItem = LocationClusterItem(
                        lat.toDouble(),
                        long.toDouble(),
                        title.toString(),
                        snippet.toString(),
                        type.toString(),
                        phone.toString(),
                        address.toString()
                    )

                    locationItems.add(clustItem)
                    mClusterManager.addItem(clustItem)
                }
                mClusterManager.cluster()
            }
    }

    /**
     * This method takes a list of Strings that represent the field "type"
     * for locations and loops through the map locations master list, adding any location with
     * matching "type" to pin cluster
     */
    private fun addItems(type: ArrayList<String>) {
        //clear cluster items
        mClusterManager.clearItems()

        //loop through all locations
        //If location.type is in type array, make clusterItem and add
        for (item in locationItems) {
            if (type.contains(item.getType())) {
                mClusterManager.addItem(item)

            }
        }

        mClusterManager.cluster()
    }

    /**
     * This method gathers all the "type" fields that the filters represent and
     * stores them as a list of Strings
     */
    private fun getFilters(): ArrayList<String> {

        var typeList: ArrayList<String> = arrayListOf()

        if (adolescentToggle.isChecked) typeList.add("ADOLESCENTS")
        if (homelessToggle.isChecked) typeList.add("HOMELESS SHELTER")
        if (outpatientToggle.isChecked) typeList.add("OUTPATIENT")
        if (residentialToggle.isChecked) typeList.add("RESIDENTIAL")
        if (resourceToggle.isChecked) typeList.add("RESOURCE / COMM ORG")
        if (soberToggle.isChecked) typeList.add("SOBER LIVING")
        if (noTypeToggle.isChecked) typeList.add("")

        //None are checked, show all pins
        if (typeList.isEmpty()) {
            typeList.add("ADOLESCENTS")
            typeList.add("HOMELESS SHELTER")
            typeList.add("OUTPATIENT")
            typeList.add("RESIDENTIAL")
            typeList.add("RESOURCE / COMM ORG")
            typeList.add("SOBER LIVING")
            typeList.add("")
        }

        return typeList
    }

    override fun onInfoWindowClick(marker: Marker) {
        Toast.makeText(
            this.context, "Info window clicked",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        print(marker.title + "YEET")
        return true
    }



    override fun onClusterItemClick(p0: LocationClusterItem?): Boolean {
        var informationSheet: ConstraintLayout = view!!.findViewById(R.id.information_pop_up)
        titleTextView.text = p0?.title
        titleTextView.setTextColor(Color.parseColor("#000000"))

        Log.d("Type:", p0?.getType())

        if (p0?.getType().equals("")) {
            markerTypeTextView.text = "No Type"
            ImageViewCompat.setImageTintList(
                markerType,
                ColorStateList.valueOf(resources.getColor(R.color.none_marker))
            )
        } else {
            markerTypeTextView.text = p0?.getType()
            when (p0?.getType()?.toLowerCase()) {
                "outpatient" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.out_marker))
                    )
                }
                "sober living" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.sober_marker))
                    )
                }
                "residential" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.residential_marker))
                    )
                }
                "resource / comm org" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.resource_marker))
                    )
                }
                "homeless shelter" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.homeless_marker))
                    )
                }
                "adolescents" -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.adolescents_marker))
                    )
                }
                else -> {
                    ImageViewCompat.setImageTintList(
                        markerType,
                        ColorStateList.valueOf(resources.getColor(R.color.none_marker))
                    )
                }
            }
        }

        if (p0?.getAddress().equals("")) {
            addressTextView.text = "No Address Available"
            addressTextView.setTextColor(Color.parseColor("#FF0000"))
        } else {
            addressTextView.text = p0?.getAddress()
            addressTextView.setTextColor(Color.parseColor("#000000"))
        }


        if (p0?.getPhone().equals("")) {
            phoneTextView.text = "No Phone Number Available"
            phoneTextView.setTextColor(Color.parseColor("#FF0000"))
        } else {
            phoneTextView.text = p0?.getPhone()
            phoneTextView.setTextColor(Color.parseColor("#000000"))

        }

        if (p0?.snippet.equals("")) {
            websiteTextView.text = "No Website Available"
            websiteTextView.setTextColor(Color.parseColor("#FF0000"))
        } else {
            websiteTextView.text = p0?.snippet
            websiteTextView.setTextColor(Color.parseColor("#000000"))
        }

        informationSheet.visibility = View.VISIBLE

        information_sheet_close.setOnClickListener(View.OnClickListener {
            informationSheet.visibility = View.GONE
        })

        return true
    }

}
