package com.michaelhsieh.writingimprov

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.michaelhsieh.writingimprov.home.HomeFragment
import com.michaelhsieh.writingimprov.home.HomeFragment.Companion.COLLECTION_CHALLENGES
import com.michaelhsieh.writingimprov.httprequest.JsonUnsplashApi
import com.michaelhsieh.writingimprov.httprequest.UnsplashImage
import com.michaelhsieh.writingimprov.settings.SettingsFragment
import com.michaelhsieh.writingimprov.testresource.CountingIdlingResourceSingleton
import es.dmoral.toasty.Toasty
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.*
import java.util.zip.ZipException
import javax.net.ssl.SSLContext
import kotlin.collections.ArrayList


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

    companion object {
        // Global boolean to
        // -- Listen for notifications in initial sign up by calling in HomeFragment instead.
        // -- Listen for notifications after signing out, which
        // creates snapshot error, through calling in HomeFragment.
        // -- Prevent displaying notifications multiple times when
        // tap a notification and re-open app.
        var isListeningForChallenges = false
        // Global boolean to prevent listenForChallengesChange function from executing twice
        // in MainActivity, which results in the same received or completed challenge showing
        // a duplicated notification to the user.
        var isListenFunctionCalledAlready = false
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val BASE_URL:String = "https://api.unsplash.com/"
    // Email of account linked to writing bot
    val botId = "michaelhsieh1997@gmail.com"
    val botName = "writingbot"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        Timber.d("ONCREATE called")
//        Timber.d("isListeningForChallenges? %s", isListeningForChallenges)
//        Timber.d("isListenFunctionCalledAlready? %s", isListenFunctionCalledAlready)

        Timber.plant(Timber.DebugTree())

        // vector Drawables on older devices, example API 17 tablet
        // to avoid crashes
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        // SignInFragment and HomeFragment should not have an up button.
        // Create an app bar configuration with a specific set of top level destinations.
        val appBarConfiguration = AppBarConfiguration
            .Builder(
                R.id.signInFragment,
                R.id.homeFragment
            )
            .build()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        updateAndroidSecurityProvider(this)

        // create notification channel
        createNotificationChannel()

        // notify user if received a challenge
        val email = getEmail()
        if (email != null) {
            listenForChallengesChange(email)
        }

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, "settings_1")
            param(FirebaseAnalytics.Param.ITEM_NAME, "settings")
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "settings_button")
        }
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
        } catch (e: ZipException) {
            Timber.e("Zip exception was found")
            Timber.e(e)
        }
    }

    /**
     * Notify user whenever he or she receives a new challenge,
     * that is when his or her Firestore challenges collection has a document added
     *
     * Notify user whenever another user completes his or her
     * sent challenge
     * @param userId The current user's ID, which is his or her email
     */
     fun listenForChallengesChange(userId: String) {
        // If already listening for challenges, e.g. opened app then tapped notification
        // which re-opens app and goes to challenges screen,
        // don't display notifications again
        if (isListeningForChallenges) {
            Timber.d("Already listening, exit")
            return
        }

        if (isListenFunctionCalledAlready) {
            Timber.d("Already called function to create a listener, exit")
            return
        } else {
            isListenFunctionCalledAlready = true
        }

        Timber.d("Listening for challenges")

        db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(HomeFragment.COLLECTION_CHALLENGES)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)

                    // Set to false in order to trigger HomeFragment call to this function
                    // when use sign out
                    isListeningForChallenges = false
                    // Set to false to allow function be called again
                    isListenFunctionCalledAlready = false

                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            if (dc.document.data["completed"] == false) {

                                // Toasty.normal(this, "You received " + dc.document.data.get("name") + " with prompt: " + dc.document.data.get("prompt"), Toast.LENGTH_LONG).show()
                                // Toasty.normal(this, "wait", Toast.LENGTH_LONG).show()

                                Timber.d("Received challenge notification with id " + dc.document.id)

                                // notify user about new challenge
                                val notificationTitle = getString(R.string.notification_title, dc.document.data["name"] as String)
                                val notificationText = getString(R.string.notification_text, dc.document.data["prompt"] as String)
                                displayNotification(notificationTitle, notificationText, R.id.challengesFragment)

                                isListeningForChallenges = true
                            }
                        }
                        DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified challenge: ${dc.document.data}")
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed challenge: ${dc.document.data}")
                    }
                }
            }

        // Listen for challenges current user has sent which have been completed
        // These challenges should be in other users' collections and
        // have current user email as senderId and completed set to true
        val otherUserIds = arrayListOf<String>()
        db.collection(HomeFragment.COLLECTION_USERS)
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    if (doc.id != userId) {
                        otherUserIds.add(doc.id)
                    }
                }

                listenForCompletedChallenges(userId, otherUserIds)
            }
    }

    /**
     * Display notification when writing submitted by other
     * user about one of current user's sent challenges
     * @param userEmail: Current user email
     * @param otherEmails: All other user's emails
     */
    private fun listenForCompletedChallenges(userEmail:String, otherEmails:ArrayList<String>) {
        for (otherEmail in otherEmails) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(otherEmail)
                .collection(COLLECTION_CHALLENGES)
                .whereEqualTo("senderId", userEmail)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }

                    for (dc in snapshots!!.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                // Log.d(TAG, "Added challenge: ${dc.document.data}")
                            }
                            DocumentChange.Type.MODIFIED -> {
                                if (dc.document.data["completed"] == true) {
                                    // Toasty.normal(this, "Your challenge was completed by " + dc.document.data["receiverUsername"] + " with prompt: " + dc.document.data["prompt"], Toast.LENGTH_LONG).show()
                                    Timber.d("Completed challenge notification")

                                    // notify user about submitted challenge
                                    val notificationTitle = getString(R.string.notification_title_submitted_challenge, dc.document.data["receiverUsername"] as String)
                                    val notificationText = getString(R.string.notification_text, dc.document.data["prompt"] as String)
                                    displayNotification(notificationTitle, notificationText, R.id.sentChallengesFragment)

                                    // displayNotification(
                                        // dc.document.data["name"] as String,
                                        // dc.document.data["prompt"] as String
                                    // )
                                }
                                // Log.d(TAG, "Modified challenge: ${dc.document.data}")

                                isListeningForChallenges = true

                            }
                            DocumentChange.Type.REMOVED -> {
                                //Log.d(TAG, "Removed writing: ${dc.document.data}")
                            }
                        }
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
     * @param title: notification title, e.g. challenge title
     * @param text: notification text, e.g. challenge prompt
     * @param navDest: Resource ID of fragment to navigate to, e.g. ChallengesFragment or SentChallengesFragment
     */
    private fun displayNotification(title: String, text: String, navDest:Int) {
        // navigate to challenges fragment
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            // .setDestination(R.id.challengesFragment)
            .setDestination(navDest)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            // .setContentTitle(getString(R.string.notification_title, name))
            // .setContentText(getString(R.string.notification_text, prompt))
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // set defaults in order to play notification sound
            .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Want each notification to be unique in order to show them all in status bar
        // Create a one time ID by using current time
        val notificationId = SystemClock.uptimeMillis().toInt()

        // Timber.d("Display notification with ID " + notificationId)

        // show notification
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }

    /**
     * Add challenge from writing bot to current user's collection.
     * This should trigger the listener for challenge notifications
     * @param myEmail Current user email, to use as Firestore ID
     * @param myUsername Current user's username
     */
    public fun generateBotChallenge() {
        // Get random time
        // Check if saved times already exist
        val sp: SharedPreferences = this.getSharedPreferences(SettingsFragment.KEY_PREFS, Activity.MODE_PRIVATE)
        var savedMin = sp.getInt(SettingsFragment.KEY_MIN_MINUTES, -1)
        var savedMax = sp.getInt(SettingsFragment.KEY_MAX_MINUTES, -1)
        // Default 1 and 3
        if (savedMin == -1) {
            savedMin = 1
        }
        if (savedMax == -1) {
            savedMax = 3
        }
        val minutes = getRandomTime(savedMin, savedMax).toString()
        // Now get prompt
        getUserPrompts(minutes)
        // If succeed, challenge item prompt text is set
        // and will get image URL
        // If that also succeeds, then challenge item thumb and full urls are set
        // and will then create the challenge item to add in current user's collection
    }

    private fun createBotChallenge(writingName:String, prompt:String, minutes:String, url: String, thumbUrl:String, botId:String, myEmail: String, myUsername: String) {
        val challengeItem = ChallengeItem(
            UUID.randomUUID().toString(),
            writingName,
            prompt = prompt,
            time = minutes,
            url = url,
            thumbUrl = thumbUrl,
            completed = false,
            senderId = botId,
            receiverId = myEmail,
            receiverUsername = myUsername,
            timestamp = System.currentTimeMillis() / 1000
        )
        db.collection("users")
            .document(myEmail)
            .collection("challenges")
            .add(challengeItem)
            // Show success or error Toasty
            .addOnSuccessListener {
                Toasty.info(
                    this,
                    getString(R.string.success_bot),
                    Toast.LENGTH_LONG,
                    true
                ).show()
            }
            .addOnFailureListener() {
                Toasty.error(
                    this,
                    getString(R.string.error_bot),
                    Toast.LENGTH_LONG,
                    true
                ).show()
            }
    }

    /**
     * Return the user's display name if signed in.
     * Otherwise, return null.
     */
    private fun getUsername():String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            return user.displayName
        }
        return null
    }

    /** Generates a random integer time in minutes.
     * @param min The minimum time limit
     * @param max The maximum time limit
     * @return A number from min to max, included */
    private fun getRandomTime(min:Int, max:Int):Int {
        return (min..max).random()
    }

    /** Generates a random prompt from user's custom prompts collection, or
     * if not available, generates from a newly created set of default prompts. */
    private fun getUserPrompts(minutes:String) {
        var isGotPrompts:Boolean = false

        val email = getEmail()
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .collection(HomeFragment.COLLECTION_PROMPTS)
                .get()
                .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                    if (task.isSuccessful) {
                        if (task.result?.size()!! > 0) {
                            for (document in task.result!!) {
                                Timber.d("Prompts already exist, get from Firestore")
                                // Only run once
                                if (!isGotPrompts) {
                                    getPromptsFromFirestore(email, minutes)
                                    isGotPrompts = true
                                }
                            }
                        }
//                        else {
//                            Timber.d("No prompts exist, create a new prompts collection")
//                            createDefaultPrompts(email)
//                        }
                    } else {
                        Timber.d("Error getting practice prompts: %s", task.exception)
                        Toasty.error(this, getString(R.string.error_loading_prompts), Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    /**
     * Creates a new collection in Firestore containing default prompts from String resources.
     * Then generates a random prompt from this collection.
     */
//    private fun createDefaultPrompts(userId:String, botId:String, botName:String, writingName:String, minutes:String) {
//        val promptArray = arrayOf(
//            getString(R.string.prompt_feel),
//            getString(R.string.prompt_story),
//            getString(R.string.prompt_mystery),
//            getString(R.string.prompt_action),
//            getString(R.string.prompt_thriller),
//            getString(R.string.prompt_comedy)
//        )
//
//        val collection = db.collection(HomeFragment.COLLECTION_USERS)
//            .document(userId)
//            .collection(HomeFragment.COLLECTION_PROMPTS)
//
//        var counter = 0
//        for (text in promptArray) {
//            val promptItem = PromptItem(
//                id = UUID.randomUUID().toString(),
//                prompt = text,
//                timestamp = System.currentTimeMillis() / 1000
//            )
//            collection.add(promptItem).addOnCompleteListener {
//                if (counter == promptArray.size) {
//                    Timber.d("Done adding default prompts")
//                    getPromptsFromFirestore(userId)
//                }
//                counter += 1
//            }
//        }
//
//    }

    private fun getPromptsFromFirestore(userId: String, minutes:String) {
        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(HomeFragment.COLLECTION_PROMPTS)

        collection
            .get()
            .addOnSuccessListener {
                // Convert the whole Query Snapshot to a list
                // of objects directly
                val items: List<PromptItem> =
                    it.toObjects(PromptItem::class.java)

                val prompt = getRandomPrompt(items)
                getRandomImageUrl(userId, minutes, prompt)

            }.addOnFailureListener {
                Timber.e(it)
                Toasty.error(this, R.string.error_loading_prompts, Toast.LENGTH_LONG).show()
            }
    }

    /** Generates a random prompt from array.
     * @param prompts The List of possible PromptItems */
    private fun getRandomPrompt(prompts: List<PromptItem>):String {
        val promptTextList = prompts.map { it.prompt }
        val promptArray = promptTextList.toTypedArray()
        Timber.d("Final prompt array to pick from: %s", promptArray.joinToString())
        // generated random number from 0 to last index included
        val randNum = (promptArray.indices).random()
        return promptArray[randNum]
    }

    /**
     *  Gets a random image URL and sets variable if successful.
     *  Otherwise, shows an error Toast.
     *
     */
    private fun getRandomImageUrl(email:String, minutes:String, prompt:String) {
        Timber.d("starting get url")

        // Create Retrofit to get random image
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonUnsplashApi: JsonUnsplashApi = retrofit.create(JsonUnsplashApi::class.java)

        // pass in access key
        val call: Call<UnsplashImage> = jsonUnsplashApi.getRandomImage(getString(R.string.access_key))

        call.enqueue(object : retrofit2.Callback<UnsplashImage> {
            override fun onFailure(call: Call<UnsplashImage>, t: Throwable) {
                Timber.e(t.message)
                Toasty.error(this@MainActivity, R.string.error_loading_url, Toast.LENGTH_LONG, true).show()
            }

            override fun onResponse(call: Call<UnsplashImage>, response: Response<UnsplashImage>) {
                if (!response.isSuccessful) {
                    Timber.d("Code: %s", response.code())
                    // Show error Toasty
                    Toasty.error(this@MainActivity,
                        R.string.error_loading_url, Toast.LENGTH_LONG, true).show()
                    return
                }

                val image: UnsplashImage? = response.body()

                if (image != null) {

                    val regularUrl = image.urls.asJsonObject.get("regular")
                    val thumbnailUrl = image.urls.asJsonObject.get("thumb")

                    // Set var url to the new image URL
                    val url = regularUrl.asString
                    Timber.d("finished getting url: %s", url)

                    // Set var thumb url to the new image thumbnail URL
                    val thumbUrl = thumbnailUrl.asString
                    Timber.d("finished getting thumbnail url: %s", thumbUrl)
                    val writingName = getString(R.string.challenge_from, botName)
                    val username = getUsername()
                    if (username != null) {
                        createBotChallenge(writingName, prompt, minutes, url, thumbUrl, botId, email, username)
                    }
                }
            }

        })
    }
}