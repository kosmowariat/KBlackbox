package top.niunaijun.blackboxa.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FavouritePlace(val name: String, val latitude: Double, val longitude: Double)

/** Places saved by the user for the fake location map. */
object FakeFavourites {

    private const val PREFS = "fake_favourites"
    private const val KEY = "places"

    fun load(context: Context): List<FavouritePlace> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(json)
        return List(array.length()) {
            val place = array.getJSONObject(it)
            FavouritePlace(place.getString("name"), place.getDouble("lat"), place.getDouble("lon"))
        }
    }

    fun add(context: Context, place: FavouritePlace) = save(context, load(context) + place)

    fun remove(context: Context, place: FavouritePlace) =
            save(context, load(context).filterNot { it.name == place.name && it.latitude == place.latitude && it.longitude == place.longitude })

    private fun save(context: Context, places: List<FavouritePlace>) {
        val array = JSONArray()
        places.forEach {
            array.put(JSONObject().put("name", it.name).put("lat", it.latitude).put("lon", it.longitude))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
