package com.example.mahmoudashraaf.movies;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 *  display the movie details.
 */

public class DetailFragment extends Fragment {
    @Bind(R.id.detail_title) TextView title;
    @Bind(R.id.detail_image) ImageView image;
    @Bind(R.id.detail_date) TextView releaseYear;
    @Bind(R.id.detail_plot) TextView plot;
    @Bind(R.id.detail_vote) TextView vote;
    @Bind(R.id.detail_reviews) LinearLayout reviewLayout;
    @Bind(R.id.detail_trailers) LinearLayout trailerLayout;
    @Bind(R.id.detail_favorite) TextView favoriteMarker;
    @Bind(R.id.detail_trailer_item) TextView trailer;
    @Bind(R.id.detail_review_item) TextView review;
    @Bind(R.id.detail_trailer_title) TextView trailerTitle;
    @Bind(R.id.detail_review_title) TextView reviewTitle;


    public DetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);

        final MovieInfo movieInfo;
        Bundle arguments = getArguments();
        if (arguments != null) {
            // The detail fragment called via callback.
            movieInfo = arguments.getParcelable("MovieInfo");
            addDetails(movieInfo);
        }
        else {
            // The detail Activity called via intent. Inspect the intent for movie data.
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra("MovieInfo")) {
                movieInfo = (MovieInfo) intent.getParcelableExtra("MovieInfo");
                addDetails(movieInfo);
            }
        }
        return rootView;
    }

    private void addDetails(final MovieInfo movieInfo) {
        Context mContext = getActivity();
        final String GREEN = "#009688";

        // display title
        title.setText(movieInfo.title);
        // set background to green
        title.setBackgroundColor(Color.parseColor(GREEN));

        // display poster
        Picasso.with(mContext).load(movieInfo.imageUrl).into(image);

        // display year of release
        String string = movieInfo.releaseDate;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(string);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            releaseYear.setText("" + year);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // display plot
        plot.setText(movieInfo.plot);

        // display vote
        vote.setText("" + movieInfo.vote + "/10");

        // display favoriteMarker
        favoriteMarker.setText("MARK AS FAVORITE");
        favoriteMarker.setBackgroundColor(Color.parseColor(GREEN));

        // display reviews
        reviewTitle.setText("Reviews:");
        FetchReviewTask reviewTask = new FetchReviewTask();
        reviewTask.execute(movieInfo.id);

        // show trailers
        trailerTitle.setText("Trailers:");
        FetchTrailerTask trailerTask = new FetchTrailerTask();
        trailerTask.execute(movieInfo.id);

        // deal with favorites
        favoriteMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (updateFavorites(movieInfo)) {
                    Toast.makeText(
                            getActivity(),
                            getString(R.string.add_new_favorite) + " " + movieInfo.title,
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            getActivity(),
                            movieInfo.title + " " + getString(R.string.item_marked_already),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private boolean updateFavorites(MovieInfo favoriteMovie) {
        Gson gson = new Gson();
        String movieString = gson.toJson(favoriteMovie);
        Set<String> prevFavorites = MovieGridFragment.favoritesPref.getStringSet(
                getString(R.string.pref_favorites_key),
                null);
        // first check whether the movie is already marked or not
        Set<String>favorites;
        if(prevFavorites != null) {
            if  (prevFavorites.contains(movieString)) {
                return false;
            }
            else {
                favorites = new HashSet<>(prevFavorites);
                prevFavorites.clear();
            }
        }
        else {
            favorites = new HashSet<>();
        }
        favorites.add(movieString);

        // store updated favorites set in favoritePref
        MovieGridFragment.favoritesPrefEditor.putStringSet(
                getString(R.string.pref_favorites_key),
                favorites
        );
        MovieGridFragment.favoritesPrefEditor.apply();
        return true;
    }

    private void watchTrailer(String videoKey) {
        try {
            String videoUrl = "vnd.youtube" + videoKey;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            String videoUrl = "http://www.youtube.com/watch?v=" + videoKey;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            startActivity(intent);
        }
    }

    /**
     * fetch reviews from server and attach to adapter
     */
    public class FetchReviewTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchReviewTask.class.getSimpleName();

        /**
         * Take the String representing the complete movie data in JSON format and pull out the
         * data we need to construct the Strings needed for the wireframes
         */
        private String[] getReviewDataFromJson(String reviewJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String RESULTS = "results";
            final String CONTENT = "content";
            final String TOTAL_RESULTS = "total_results";

            JSONObject reviewJson = new JSONObject(reviewJsonStr);
            int totalReviews = reviewJson.getInt(TOTAL_RESULTS);
            if(totalReviews == 0) {
                return null;
            }

            // get all reviews from the JSON
            JSONArray reviewArray = reviewJson.getJSONArray(RESULTS);

            // create an array to store parsed review objects
            String[] resultInfo = new String[reviewArray.length()];

            for (int i = 0; i < reviewArray.length(); i++) {
                String content;
                JSONObject reviewObject = reviewArray.getJSONObject(i);
                content = reviewObject.getString(CONTENT);
                resultInfo[i] = content;
            }
            return resultInfo;
        }

        @Override
        protected String[] doInBackground(String... params) {

            // If there's no input params, there's nothing to look up. Verify size of params.
            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String reviewJsonStr = null;

            try {
                // Construct the URL for the reviews query
                final String MOVIE_BASE_URL1 = "https://api.themoviedb.org/3/movie/";
                final String MOVIE_BASE_URL2 = "/reviews?";
                final String API = "api_key";
                final String API_KEY = "254054661bab0aeec2c07cf3ac0d2ea2";

                Uri buildUri = Uri.parse(MOVIE_BASE_URL1 + params[0] + MOVIE_BASE_URL2).buildUpon()
                        .appendQueryParameter(API, API_KEY).build();

                URL url = new URL(buildUri.toString());

                // Create the request to movieDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;

                }
                reviewJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("DetailFragment", "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("DetailFragment", "Error closing stream", e);
                    }
                }
            }
            try {
                return getReviewDataFromJson(reviewJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        // get data from the sever
        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                for (final String reviewStr : result) {
                    review = new TextView(getActivity());
                    review.setText("\"" + reviewStr + "\"");
                    review.setTypeface(Typeface.SERIF, 2);
                    review.setPadding(24, 12, 12, 24);
                    reviewLayout.addView(review);
                }
            }
            // show a warning if no reviews available yet
            else {
                review = new TextView(getActivity());
                review.setText("No reviews yet!");
                review.setTypeface(Typeface.SERIF, 2);
                review.setPadding(24, 12, 12, 24);
                reviewLayout.addView(review);
                }
            }
        }

    /**
     * fetch trailers from server and attach to adapter
     */
    public class FetchTrailerTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchTrailerTask.class.getSimpleName();

        /**
         * Take the String representing the complete movie data in JSON format and pull out the
         * data we need to construct the Strings needed for the wireframes
         */
        private String[] getTrailerDataFromJson(String trailerJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String RESULTS = "results";
            final String YOUTUBE_KEY = "key";  // youtube key

            JSONObject trailerJson = new JSONObject(trailerJsonStr);

            // get all trailers from the JSON
            JSONArray trailerArray = trailerJson.getJSONArray(RESULTS);

            // no trailer, do nothing
            if(trailerArray.length() == 0) {
                return null;
            }

            // create an array to store parsed review objects
            String[] resultInfo = new String[trailerArray.length()];

            for (int i = 0; i < trailerArray.length(); i++) {
                String youtubeKey;
                JSONObject reviewObject = trailerArray.getJSONObject(i);
                youtubeKey = reviewObject.getString(YOUTUBE_KEY);
                resultInfo[i] = youtubeKey;
            }
            return resultInfo;
        }

        @Override
        protected String[] doInBackground(String... params) {

            // If there's no input params, there's nothing to look up. Verify size of params.
            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String trailerJsonStr = null;

            try {
                // Construct the URL for the trailers query
                final String MOVIE_BASE_URL1 = "https://api.themoviedb.org/3/movie/";
                final String MOVIE_BASE_URL2 = "/videos?";
                final String API = "api_key";
                final String API_KEY = "254054661bab0aeec2c07cf3ac0d2ea2";

                Uri buildUri = Uri.parse(MOVIE_BASE_URL1 + params[0] + MOVIE_BASE_URL2).buildUpon()
                        .appendQueryParameter(API, API_KEY).build();

                URL url = new URL(buildUri.toString());

                // Create the request to movieDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                trailerJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("DetailFragment", "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("DetailFragment", "Error closing stream", e);
                    }
                }
            }
            try {
                return getTrailerDataFromJson(trailerJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        // get data from the sever
        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                int trailerIndex = 0;
                char playSymbol = '\u25b6';
                for(final String youtube_key : result) {
                    trailer = new TextView(getActivity());
                    trailer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            watchTrailer(youtube_key);
                        }
                    });
                    trailer.setText(playSymbol + "     Trailer " + ++trailerIndex);
                    trailer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                    trailer.setPadding(24, 12, 12, 12);
                    trailerLayout.addView(trailer);
                }
            }
            // show a warning if no trailers available yet
            else {
                trailer = new TextView(getActivity());
                trailer.setText("No trailers yet, please come back later and try again!");
                trailer.setTypeface(Typeface.SERIF, 2);
                trailer.setPadding(24, 12, 12, 12);
                trailerLayout.addView(trailer);
            }
        }
    }
}
