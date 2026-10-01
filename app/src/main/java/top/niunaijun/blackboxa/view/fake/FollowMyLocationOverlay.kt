package top.niunaijun.blackboxa.view.fake


import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.preference.PreferenceManager
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.databinding.ActivityOsmdroidBinding
import top.niunaijun.blackboxa.databinding.DialogTextInputBinding
import top.niunaijun.blackboxa.util.FakeFavourites
import top.niunaijun.blackboxa.util.FavouritePlace
import top.niunaijun.blackboxa.util.GeoCoordinates
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.showConfirmDialog
import top.niunaijun.blackboxa.util.toast



class FollowMyLocationOverlay : AppCompatActivity() {
    val TAG: String = "FollowMyLocationOverlay"

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    private val binding: ActivityOsmdroidBinding by inflate()

    lateinit var startPoint: GeoPoint

    private lateinit var marker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        

        
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        
        
        
        
        

        
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this) { finishWithResult(startPoint) }

        val location: BLocation? = intent.getParcelableExtra("location")

        startPoint = if (location == null) {
            GeoPoint(30.2736, 120.1563)
        } else {
            GeoPoint(location.latitude, location.longitude)
        }


        val startMarker = Marker(binding.map).also { marker = it }
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        binding.map.overlays.add(startMarker)
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                startPoint = p
                startMarker.position = p
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                binding.map.overlays.add(startMarker)
                toast(p.latitude.toString() + " - " + p.longitude)
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        binding.map.overlays.add(MapEventsOverlay(mReceive))
        val mapController = binding.map.controller
        mapController.setZoom(12.5)

        mapController.setCenter(startPoint)
        binding.map.setTileSource(TileSourceFactory.MAPNIK)

        binding.coordinatesButton.setOnClickListener { showCoordinatesDialog() }
        binding.favouritesButton.setOnClickListener { showFavouritesDialog() }
    }

    private fun selectPoint(point: GeoPoint) {
        startPoint = point
        marker.position = point
        binding.map.controller.animateTo(point)
        binding.map.invalidate()
    }

    private fun showCoordinatesDialog() {
        val dialogBinding = DialogTextInputBinding.inflate(layoutInflater)
        dialogBinding.inputLayout.hint = getString(R.string.fake_coordinates_hint)
        dialogBinding.input.inputType = InputType.TYPE_CLASS_TEXT
        dialogBinding.input.setText(formatPoint(startPoint))
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fake_coordinates)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.done) { _, _ ->
                    val parsed = GeoCoordinates.parse(dialogBinding.input.text.toString())
                    if (parsed == null) {
                        toast(R.string.fake_coordinates_invalid)
                    } else {
                        selectPoint(GeoPoint(parsed.first, parsed.second))
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun showFavouritesDialog() {
        val places = FakeFavourites.load(this)
        val builder = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fake_favourites)
                .setNeutralButton(R.string.fake_save_place) { _, _ -> showSavePlaceDialog() }
                .setNegativeButton(R.string.cancel, null)
        if (places.isEmpty()) {
            builder.setMessage(R.string.fake_favourites_empty)
        } else {
            builder.setItems(places.map { it.name }.toTypedArray()) { _, index ->
                selectPoint(GeoPoint(places[index].latitude, places[index].longitude))
            }
        }
        val dialog = builder.show()
        dialog.listView?.setOnItemLongClickListener { _, _, index, _ ->
            dialog.dismiss()
            confirmRemove(places[index])
            true
        }
    }

    private fun showSavePlaceDialog() {
        val dialogBinding = DialogTextInputBinding.inflate(layoutInflater)
        dialogBinding.inputLayout.hint = getString(R.string.fake_place_name)
        dialogBinding.input.setText(formatPoint(startPoint))
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fake_save_place)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.done) { _, _ ->
                    val name = dialogBinding.input.text.toString().trim().ifEmpty { formatPoint(startPoint) }
                    FakeFavourites.add(this, FavouritePlace(name, startPoint.latitude, startPoint.longitude))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun confirmRemove(place: FavouritePlace) {
        showConfirmDialog(R.string.fake_favourites, getString(R.string.fake_place_delete, place.name)) {
            FakeFavourites.remove(this, place)
        }
    }

    private fun formatPoint(point: GeoPoint) = String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)

    override fun onResume() {
        super.onResume()
        
        
        
        
        binding.map.onResume() 
    }

    override fun onPause() {
        super.onPause()
        
        
        
        
        binding.map.onPause()  
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun finishWithResult(geoPoint: GeoPoint) {
        intent.putExtra("latitude", geoPoint.latitude)
        intent.putExtra("longitude", geoPoint.longitude)
        setResult(Activity.RESULT_OK, intent)
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        window.peekDecorView()?.run {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
        finish()
    }

}