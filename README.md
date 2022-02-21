# RBA-Android
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

RBA (Recovery Business Association) is a project with the goal of providing resources to individuals struggling with mental or behavioral health.
The app will accomplish this in a few ways: Daily wellness questionnaires, an interactive map of resources, and a way to reach out to life coaches through chat, video, or voice calls.

# Purpose of the App
The app is used as a form of communication between RBA's coaches and recoverees as well as having a map showing the coach/recoveree a map of helpful locations. Coaches and recoverees are able to sign up within the app or through a RBA admin creating an account for them. It allows a coach and recoveree to communicate via text chat, video calling, or voice calling. The video and voice calling length is recorded when both users are present in the room to accurately report to the RBA admins the amount of time a coach has spent assisting a recoveree. A recoveree that is logged-in ('online') can see a list of coaches that are logged-in ('online') as well. If the coach is currently not in a call they will be available for the recoveree to see on their home-screen, but if the coach is not logged-in ('offline') or the coach is currently in a call with another recoveree ('busy') they will not show up in the recoveree's home-screen. The map contains all the locations associated with RBA that offer a form of help. It includes adoloscent outreaches, homeless shelters, outpatient centers, residential centers, resource/community organizations, sober living associations, and other similar outreaches. Any of these associations are able to report issues with their locations, telephone, address, and website to have an RBA admin review it to correct any possible misinformation. Overall, the app is meant to help anyone with mental or behavorial health by providing useful information and a way to reach out to life coaches.

# Important note
This app does not use the one activity multiple fragment design approach. This was because on Android, a notification can only open an Activity not a Fragment. So you use Fragment transactions to change the current Activity's Fragment and startActivity with an Intent containing an Activity to change the current Activity. Also be sure to add the Firebase Service Account (from the Firebase project settings) to the app/ directory. Do NOT commit the Service Account to the VCS.

## Changing an Activity within an Activity (Java)
```java
startActivity(new Intent(this, ActivityToStart.class))
```

## Changing an Activity within an Activity (Kotlin)
```kotlin
startActivity(Intent(this, ActivityToStart::class.java))
```

## Changing an Fragment within an Activity (Java)
```java
FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
fragmentTransaction.replace(
    R.id.nav_host_fragment,
    new FragmentToReplaceTheCurrentFragment(),
    "FragmentToReplaceTheCurrentFragment"
    )
.addToBackStack(null)
.commit();
```

## Changing an Fragment within an Activity (Kotlin)
```kotlin
val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
fragmentTransaction.replace(
    R.id.nav_host_fragment,
    FragmentToReplaceTheCurrentFragment(),
    "FragmentToReplaceTheCurrentFragment"
    )
.addToBackStack(null)
.commit()
```

## Changing an Activity within an Fragment (Java)
```java
startActivity(new Intent(requireActivity(), ActivityToStart.class));
```

## Changing an Activity within an Fragment (Kotlin)
```kotlin
startActivity(Intent(requireActivity(), ActivityToStart::class.java))
```

# How does it work?

RBA-Android takes advantage of Google Firebase for storing information about users and resource locations, as well as for user authentication. Communications between life coaches and recoverees will be done using Twilio, as the platform appears to address the requirement of HIPAA-compliant communication.

# What does it still need?
## UI
- App launcher icon
- Determine to keep/remove the 'message' button on the Incoming Call UI

## SECURITY
- Basic security standards have been implemented for log-ins and account creations. Such as, character limits, invalid character checks, and a minimun 8-alphanumeric with at least 1 special character password. A deeper inspection is needed to ensure the RBA-app complies with HIPAA.

# Twilio access token generation.
The back-end is able to generate access tokens that work to connect users to a video room, but they contain invalid signatures. This needs to be inspected to find out why they contain invalid signatures.
Another high-level need for this app is a "social wrapper." The bones and framework of the app are in place, but the current state may be improved to determine when coaches are busy with current calls, so multiple recoverees do not call the same coach believing they are available when they may already be in a call.
Finally, the app is subject to a branding and scope change. The client intends to scale this beyond Wisconsin-only resources and into an app for all of the US. Once more is known, a graphic designer will be needed to revamp app backgrounds, logos, color scheme, etc.

# Some helpful links and resources
Wishope official site: https://www.wishope.org/

Twiliio HIPAA: https://www.twilio.com/hipaa

Twilio programmable video: https://www.twilio.com/docs/video

Firebase resources: https://firebase.google.com/docs

For project specific questions, contact rocha021@rangers.uwp.edu
