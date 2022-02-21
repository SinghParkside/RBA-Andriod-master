package edu.uwp.appfactory.wishope.views.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * <h1>This class is for cluster item objects.</h1>
 * <p>Lets map pins carry more information and importantly lets them be clustered.</p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
class LocationClusterItem : ClusterItem {

    private val mPosition: LatLng
    private val mTitle: String
    private val mSnippet: String
    private val mType: String
    private val mPhone: String
    private val mAddress: String

    constructor(lat: Double, long: Double) {
        mPosition = LatLng(lat, long)
        mTitle = ""
        mSnippet = ""
        mType = ""
        mPhone = ""
        mAddress = ""
    }

    constructor(
        lat: Double,
        long: Double,
        title: String,
        snippet: String,
        type: String,
        phone: String,
        address: String
    ) {
        mPosition = LatLng(lat, long)
        mTitle = title
        mSnippet = snippet
        mType = type
        mPhone = phone
        mAddress = address
    }

    override fun getSnippet(): String {
        return mSnippet
    }

    override fun getTitle(): String {
        return mTitle
    }

    override fun getPosition(): LatLng {
        return mPosition
    }

    fun getType(): String {
        return mType
    }

    fun getPhone(): String {
        return mPhone
    }

    fun getAddress(): String {
        return mAddress
    }
}