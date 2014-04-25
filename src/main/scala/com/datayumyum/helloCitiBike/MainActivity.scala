package com.datayumyum.helloCitiBike

import android.os.Bundle
import com.google.android.gms.maps.{CameraUpdate, CameraUpdateFactory, GoogleMap, MapFragment}
import com.google.android.gms.common.{GooglePlayServicesClient, ConnectionResult, GooglePlayServicesUtil}
import android.util.Log
import android.support.v4.app.FragmentActivity
import android.location.Location
import com.google.android.gms.maps.model.{BitmapDescriptorFactory, MarkerOptions, LatLng}
import com.google.android.gms.location.LocationClient
import scala.util.parsing.json.JSON

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

  def addMarker(xy: LatLng) {
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
      val lastLocation: Location = locationClient.getLastLocation()
      val xy = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())
      zoomTo(xy)
      addMarker(xy)
      addStations()
    }

    def addStations() {
      thread {
        val jsonStr = scala.io.Source.fromURL("http://appservices.citibikenyc.com/data2/stations.php").getLines.mkString("\n")
        val jsonAST = JSON.parseFull(jsonStr)
        val parsedMap: Map[String, List[Map[String, Any]]] = jsonAST.get.asInstanceOf[Map[String, List[Map[String, Any]]]]
        val results: List[Map[String, Any]] = parsedMap("results")

        val stations: List[Station] = results.map {
          stationMap =>
            Station(stationMap("id").asInstanceOf[Double].toInt,
              stationMap("label").asInstanceOf[String],
              stationMap("latitude").asInstanceOf[Double],
              stationMap("longitude").asInstanceOf[Double],
              stationMap("availableBikes").asInstanceOf[Double].toInt,
              stationMap("availableDocks").asInstanceOf[Double].toInt)
        }
        stations.foreach {
          station => val xy = new LatLng(station.latitude, station.longitude)
            uiThread {
              addMarker(xy)
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


case class Station(id: Int, label: String, latitude: Double, longitude: Double, availableBikes: Int, availableDocks: Int)