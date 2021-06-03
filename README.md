# Writing Improv
Improve your creative writing skills.

## Installation
Clone this repository:
`git clone https://github.com/mdhsieh/writing-improv.git`
and open the cloned folder in Android Studio.

This app uses the Unsplash API,
so you need to [register as a developer](https://unsplash.com/join).
Login to Unsplash, create a new application, and copy the access key.
Then, create a new file `app/src/main/res/values/secrets.xml` with the access key.
For example:
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="access_key">YOUR_ACCESS_KEY</string>
</resources>
```

Run the app.

## Download

## Features

## Libraries
- [FirebaseUI Authentication](https://firebase.google.com/docs/auth/android/firebaseui)
- [Cloud Firestore](https://firebase.google.com/docs/firestore)
- [Retrofit](https://square.github.io/retrofit)
- [Unsplash API](https://unsplash.com/developers)
- [Toasty](https://github.com/GrenderG/Toasty)
- [Timber](https://github.com/JakeWharton/timber)

## AndroidX
- Fragment Navigation Component
- Espresso UI Testing

## Screenshots
![Sign In](Screenshot_1.png)
![Home](Screenshot_2.png)
![Prompt](Screenshot_3.png)
![Writing](Screenshot_4.png)
![My Writing](Screenshot_5.PNG)
![My Writing Details](Screenshot_6.PNG)