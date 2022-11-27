package dk.easj.anbo.locationupdates

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import dk.easj.anbo.locationupdates.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // gradle:     implementation 'com.google.android.gms:play-services-location:21.0.1'
    private lateinit var locationCallback: LocationCallback
    private var udpPort = 7000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // https://developer.android.com/training/location/permissions
        // Must be registered early in the process
        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    doIt()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    doIt()
                }
                else -> {
                    Snackbar.make(
                        binding.firstFragment,
                        "Sorry no location for you",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val message =
                        "${location.latitude} ${location.longitude} ${location.bearing} ${location.altitude} ${location.speed}"
                    binding.textviewFirst.append("\n$message")
                    UdpBroadcastHelper().sendUdpBroadcast("Location\n$message", udpPort)
                }
            }
        }

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextUdpPortLocation.setText(udpPort.toString())

        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Before you perform the actual permission request, check whether your app
                // already has the permissions, and whether your app needs to show a permission
                // rationale dialog. For more details, see Request permissions.
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION // TODO no coarse location
                    )
                )
            } else {
                stopLocationUpdates()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun doIt() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        startLocationUpdates()
        //singleLocation()
    }

    @SuppressLint("MissingPermission")
    private fun singleLocation() {
        // https://developer.android.com/training/location/retrieve-current
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    binding.textviewFirst.text = "Location is null"
                } else {
                    // https://developer.android.com/training/location/request-updates
                    val message =
                        "${location.latitude} ${location.longitude} ${location.bearing} ${location.altitude} ${location.speed}"
                    binding.textviewFirst.text = message

                }
            }
            .addOnFailureListener { exception ->
                binding.textviewFirst.text = exception.toString()
            }
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(1000)
                .build(), // https://developer.android.com/training/location/change-location-settings
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}