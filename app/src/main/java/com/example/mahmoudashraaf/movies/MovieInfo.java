package com.example.mahmoudashraaf.movies;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * implement a parcelable MovieInfo class, allow read and write instances from a Parcel
 */
public class MovieInfo implements Parcelable {
    String imageUrl, title, plot, releaseDate, id;
    //int id;
    double vote;


    public MovieInfo(String imageUrl, String id, String title, String plot, double vote, String releaseDate) {
        this.imageUrl = imageUrl;
        this.id = id;
        this.title = title;
        this.plot = plot;
        this.vote = vote;
        this.releaseDate = releaseDate;
    }

    private MovieInfo(Parcel in) {
        imageUrl = in.readString();
        id = in.readString();
        title = in.readString();
        plot = in.readString();
        vote = in.readDouble();
        releaseDate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return imageUrl + "--" + id + "--" + title
            + "--" + plot + "--" + vote + "--" + releaseDate;
    }

   @Override
    public void writeToParcel(Parcel parcel, int i) {
       parcel.writeString(imageUrl);
       parcel.writeString(id);
       parcel.writeString(title);
       parcel.writeString(plot);
       parcel.writeDouble(vote);
       parcel.writeString(releaseDate);
   }

    public static final Parcelable.Creator<MovieInfo>
            CREATOR = new Parcelable.Creator<MovieInfo>() {

        @Override
        public MovieInfo createFromParcel(Parcel parcel) {
            return new MovieInfo(parcel);
        }

        @Override
        public MovieInfo[] newArray(int i) {
            return new MovieInfo[i];
        }
    };

}