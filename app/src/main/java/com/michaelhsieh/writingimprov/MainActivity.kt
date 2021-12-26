package com.michaelhsieh.writingimprov

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import javax.net.ssl.SSLContext


/**
 * Fragment host.
 *
 * References:
 * https://source.unsplash.com/
 * https://stackoverflow.com/questions/29916962/javax-net-ssl-sslhandshakeexception-javax-net-ssl-sslprotocolexception-ssl-han
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    var db = FirebaseFirestore.getInstance()
    private val TAG = "MainActivity"

    // notification channel ID
    private val CHANNEL_ID = "writing_improv_channel"

    // Global boolean to prevent displaying notifications
    // multiple times
    companion object {
        var isListeningForChallenges = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // vector Drawables on older devices, example API 17 tablet
        // to avoid crashes
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        Timber.plant(Timber.DebugTree())

        updateAndroidSecurityProvider(this)

        // create notification channel
        createNotificationChannel()

        // notify user if received a challenge
        val email = getEmail()
        if (email != null) {
            listenForChallengesChange(email)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController)
    }

    /**
     * Update Provider to fix Picasso 504 timeout error on older device,
     * example API 17 tablet.
     *
     * Force TLSv1.2 on Android 4.0 devices, ex. API 17 tablet, in order to use Retrofit.
     */
    private fun updateAndroidSecurityProvider(callingActivity: Activity) {
        try {
            ProviderInstaller.installIfNeeded(this)
            Timber.d("Installed provider if needed")

            // force TLS v1.2 for Android 4.0 devices that don't have it enabled by default
            val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, null, null)
            sslContext.createSSLEngine()
            Timber.d("SSLContext with protocol TLSv1.2")
        } catch (e: GooglePlayServicesRepairableException) {
            // Thrown when Google Play Services is not installed, up-to-date, or enabled
            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
            GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Timber.e("Google Play Services not available.")
        }
    }

    /**
     * Notify user whenever he or she receives a new challenge,
     * that is when his or her Firestore challenges collection has a document added
     * @param userId The current user's ID, which is his or her email
     */
     fun listenForChallengesChange(userId: String) {
        // If already listening for challenges, e.g. opened app then tapped notification
        // which re-opens app and goes to challenges screen,
        // don't display again
        if (isListeningForChallenges) {
            return
        }
        db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(HomeFragment.COLLECTION_CHALLENGES)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            if (dc.document.data["completed"] == false) {
                                // Toasty.normal(this, "You received " + dc.document.data.get("name") + " with prompt: " + dc.document.data.get("prompt"), Toast.LENGTH_LONG).show()

                                // notify user about new challenge
                                displayNotification(
                                    dc.document.data["name"] as String,
                                    dc.document.data["prompt"] as String
                                )

                                isListeningForChallenges = true
                            }
                        }
                        DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified challenge: ${dc.document.data}")
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed challenge: ${dc.document.data}")
                    }
                }
            }
    }

    /**
     * Return the user's email if signed in.
     * Otherwise, return null.
     */
    private fun getEmail():String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            return user.email
        }
        return null
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Display notification to let user know
     * he or she has received a challenge
     * title: Challenge name
     * text: Challenge prompt
     */
    private fun displayNotification(name: String, prompt: String) {
        // navigate to challenges fragment
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.challengesFragment)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.notification_title, name))
            .setContentText(getString(R.string.notification_text, prompt))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // set defaults in order to play notification sound
            .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Want each notification to be unique in order to show them all in status bar
        // Create a one time ID by using current time
        val notificationId = SystemClock.uptimeMillis().toInt()
        // show notification
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }
}