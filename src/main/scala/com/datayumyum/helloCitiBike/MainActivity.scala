package com.datayumyum.helloCitiBike

import android.os.Bundle
import com.google.android.gms.maps.{CameraUpdate, CameraUpdateFactory, GoogleMap, MapFragment}
import com.google.android.gms.common.{GooglePlayServicesClient, ConnectionResult, GooglePlayServicesUtil}
import android.util.Log
import android.support.v4.app.FragmentActivity
import android.location.Location
import com.google.android.gms.maps.model.{BitmapDescriptor, BitmapDescriptorFactory, MarkerOptions, LatLng}
import com.google.android.gms.location.{LocationRequest, LocationListener, LocationClient}
import android.widget.Toast
import java.net.HttpURLConnection
import scala.io.Source

class MainActivity extends FragmentActivity {
  val TAG = "com.datayumyum.helloCitiBike.MainActivity"
  var locationClient: LocationClient = null
  var mLocationRequest: LocationRequest = null
  var stop = false

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)

    def connectLocationClient() {
      mLocationRequest = LocationRequest.create();
      mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
      val MILLISECONDS_PER_SECOND = 1000
      val UPDATE_INTERVAL_IN_SECONDS = 5
      val FASTEST_INTERVAL_IN_SECONDS = 1

      val UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS
      val FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

      mLocationRequest.setInterval(UPDATE_INTERVAL)
      mLocationRequest.setFastestInterval(FASTEST_INTERVAL)

      locationClient = new LocationClient(MainActivity.this, GooglePlayCallbacks, GooglePlayCallbacks)
      locationClient.connect()
    }

    connectLocationClient()
  }

  override def onBackPressed(): Unit = {
    locationClient.unregisterConnectionCallbacks(GooglePlayCallbacks)
    locationClient.unregisterConnectionFailedListener(GooglePlayCallbacks)
    locationClient.removeLocationUpdates(LocationUpdater)
    super.onBackPressed()
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
      Log.i(TAG, f"onConnectionFailed")
      Toast.makeText(MainActivity.this, "Connection failed. Please re-connect.", Toast.LENGTH_SHORT).show()
    }

    override def onDisconnected(): Unit = {
      Log.i(TAG, f"onDisconnected")
      Toast.makeText(MainActivity.this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show()
    }

    override def onConnected(bundle: Bundle): Unit = {
      Log.d(TAG, f"onConnected")
      Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();

      val lastLocation: Location = locationClient.getLastLocation()
      val xy = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())
      zoomTo(xy)
      addStations()
      locationClient.requestLocationUpdates(mLocationRequest, LocationUpdater)
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
          station =>
            val xy = new LatLng(station.latitude, station.longitude)
            uiThread {
              val bikeIcon = BitmapDescriptorFactory.fromResource(R.drawable.bike)
              addMarker(xy, bikeIcon, f"${station.label}\nbikes:${station.availableBikes}\ndocks:${station.availableDocks}")
            }

        }
      }
    }
  }

  object LocationUpdater extends LocationListener {
    val TAG = "com.datayumyum.helloCitiBike.LocationUpdater"

    override def onLocationChanged(location: Location): Unit = {
      thread {
        Log.i(TAG, f"location=${location.getLatitude},${location.getLongitude}")
        val x = location.getLatitude()
        val y = location.getLongitude()
        val trackURL: String = f"http://hive.kaicode.com:3000/setLocation?x=${x}&y=${y}"
        val url = new java.net.URL(trackURL)
        val httpConnection = url.openConnection().asInstanceOf[HttpURLConnection]
        val result = Source.fromInputStream(httpConnection.getInputStream).mkString
        Log.i(TAG, f"${trackURL} result=${result}}")
        httpConnection.disconnect()
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