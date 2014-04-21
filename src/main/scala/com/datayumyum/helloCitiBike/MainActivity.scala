package com.datayumyum.helloCitiBike

import android.os.Bundle
import com.google.android.gms.maps.{CameraUpdate, CameraUpdateFactory, GoogleMap, MapFragment}
import com.google.android.gms.common.{GooglePlayServicesClient, ConnectionResult, GooglePlayServicesUtil}
import android.util.Log
import android.support.v4.app.FragmentActivity
import android.location.Location
import com.google.android.gms.maps.model.{BitmapDescriptorFactory, MarkerOptions, LatLng}
import com.google.android.gms.location.LocationClient

class MainActivity extends FragmentActivity {
  val TAG = "com.datayumyum.helloCitiBike.MainActivity"
  var locationClient: LocationClient = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)

    def initializeLocationClient() {
      locationClient = new LocationClient(MainActivity.this, GooglePlayCallbacks, GooglePlayCallbacks)
      locationClient.connect()
    }

    initializeLocationClient()
  }

  def zoomTo(location: Location): Unit = {
    val latLng: LatLng = new LatLng(location.getLatitude(), location.getLongitude())
    val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15)
    val map = getMap()
    map.animateCamera(cameraUpdate);
  }

  def addMarker() {
    val location = locationClient.getLastLocation()
    val xy = new LatLng(location.getLatitude(), location.getLongitude())
    val map = getMap()
    val bikeIcon = BitmapDescriptorFactory.fromResource(R.drawable.bike)

    map.addMarker(new MarkerOptions().position(xy).title("hello world").icon(bikeIcon))
  }

  def getMap(): GoogleMap = {
    val status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
    if (status == ConnectionResult.SUCCESS) {
      val mapFragment = getFragmentManager().findFragmentById(R.id.map).asInstanceOf[MapFragment]
      val map = mapFragment.getMap()
      map.setMyLocationEnabled(true)
      return map
    } else {
      throw new RuntimeException(f"isGooglePlayServicesAvailable=${status}")
    }
  }


  object GooglePlayCallbacks extends GooglePlayServicesClient.ConnectionCallbacks with GooglePlayServicesClient.OnConnectionFailedListener {
    val TAG = "com.datayumyum.helloCitiBike.GooglePlayCallbacks"

    override def onConnectionFailed(result: ConnectionResult): Unit = {
      Log.w(TAG, f"onConnectionFailed")
    }

    override def onDisconnected(): Unit = {
      Log.w(TAG, f"onDisconnected")
    }

    override def onConnected(bundle: Bundle): Unit = {
      Log.d(TAG, f"onConnected")
      zoomTo(locationClient.getLastLocation())
      addMarker()
    }
  }

}