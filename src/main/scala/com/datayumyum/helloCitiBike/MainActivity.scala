package com.datayumyum.helloCitiBike

import android.os.Bundle
import com.google.android.gms.maps.{CameraUpdate, CameraUpdateFactory, GoogleMap, MapFragment}
import com.google.android.gms.common.{GooglePlayServicesClient, ConnectionResult, GooglePlayServicesUtil}
import android.util.Log
import android.support.v4.app.FragmentActivity
import android.location.Location
import com.google.android.gms.maps.model.{BitmapDescriptor, BitmapDescriptorFactory, MarkerOptions, LatLng}
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

  def zoomTo(latLng: LatLng): Unit = {
    val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15)
    val map = getMap()
    map.animateCamera(cameraUpdate)
  }

  def addMarker(xy: LatLng, icon: BitmapDescriptor, title: String) {
    val map = getMap()
    map.addMarker(new MarkerOptions().position(xy).title(title).icon(icon))
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
      val lastLocation: Location = locationClient.getLastLocation()
      val xy = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())
      zoomTo(xy)
      val helloKittyBike = BitmapDescriptorFactory.fromResource(R.drawable.hellobike_me)
      addMarker(xy, helloKittyBike, "You are Here")
      addStations()
    }

    def addStations() {
      thread {
        import com.cedarsoftware.util.io.{JsonObject, JsonReader}
        import scala.collection.JavaConversions._
        val jsonStr = scala.io.Source.fromURL("http://appservices.citibikenyc.com/data2/stations.php").getLines.mkString("\n")
        val map = JsonReader.jsonToMaps(jsonStr)
        val results = map.get("results").asInstanceOf[JsonObject[String, Any]]
        val items = results.get("@items").asInstanceOf[Array[Any]]
        val stations: List[Station] = items.map {
          i =>
            val jMap: java.util.Map[String, Any] = i.asInstanceOf[java.util.Map[String, Any]]
            val map = jMap.toMap
            Station(map("id").asInstanceOf[Long], map("label").asInstanceOf[String], map("latitude").asInstanceOf[Double], map("longitude").asInstanceOf[Double],
              map("availableBikes").asInstanceOf[Long], map("availableDocks").asInstanceOf[Long])
        }.toList

        stations.foreach {
          station => val xy = new LatLng(station.latitude, station.longitude)
            uiThread {
              val bikeIcon = BitmapDescriptorFactory.fromResource(R.drawable.bike)
              addMarker(xy, bikeIcon, f"${station.label}\nbikes:${station.availableBikes}\ndocks:${station.availableDocks}")
            }
        }
      }
    }
  }

  def thread[F](f: => F) = (new Thread(new Runnable() {
    def run() {
      f
    }
  })).start

  def uiThread[F](f: => F) = runOnUiThread(new Runnable() {
    def run() {
      f
    }
  })
}

case class Station(id: Long, label: String, latitude: Double, longitude: Double, availableBikes: Long,
                   availableDocks: Long)