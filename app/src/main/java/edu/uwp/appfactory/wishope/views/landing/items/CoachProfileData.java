package edu.uwp.appfactory.wishope.views.landing.items;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 23-12-2020
 */
public class CoachProfileData {
    private String firstName = "";
    private String lastName = "";
    private String uid = "";
    private String profilePic = "";
    private String bio = "";
    private String location = "";
    private String email = "";
    private String status = "";
    private Bitmap imageBitmap = null;

    public CoachProfileData(
            final String firstName,
            final String lastName,
            final String uid,
            final String profilePic,
            final String bio,
            final String location,
            final String email,
            final String status) {
        this.profilePic = profilePic;
        this.firstName = firstName;
        this.lastName = lastName;
        this.uid = uid;
        this.bio = bio;
        this.location = location;
        this.email = email;
        this.status = status;
    }

    public CoachProfileData(String uid) {
        this.uid = uid;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("{%n\t\"uid\": \"%s\"%n\t\"firstName\": \"%s\"%n\t\"lastName\": \"%s\"%n\t\"status\": \"%s\"%n}", this.uid, this.firstName, this.lastName, this.status);
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public String getBio() {
        return bio;
    }

    public String getLocation() {
        return location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return this.uid.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof CoachProfileData) {
            CoachProfileData that = (CoachProfileData) obj;
            return this.uid.equals(that.uid);
        }
        return false;
    }
}
