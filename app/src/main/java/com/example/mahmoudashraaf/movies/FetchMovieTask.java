package com.example.mahmoudashraaf.movies;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchMovieTask extends AsyncTask<String, Void, MovieInfo[]> {

    private final String LOG_TAG = FetchMovieTask.class.getSimpleName();

    /**
     * Take the String representing the complete movie data in JSON format and pull out the
     * data we need to construct the Strings needed for the wireframes
     */
    private MovieInfo[] getMovieDataFromJson(String movieJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String RESULTS = "results";
        final String PATH = "poster_path";
        final String ID = "id";
        final String TITLE = "original_title";
        final String PLOT = "overview";
        final String VOTE = "vote_average";
        final String DATE = "release_date";

        JSONObject movieJson = new JSONObject(movieJsonStr);

        // get all movie information from the JSON
        JSONArray movieArray = movieJson.getJSONArray(RESULTS);

        // create an array to store parsed MovieInfo objects
        MovieInfo[] resultInfo = new MovieInfo[movieArray.length()];

        final String IMAGE_BASE_URL = "http://image.tmdb.org/t/p/";
        final String SIZE = "w185";

        for (int i = 0; i < movieArray.length(); i++) {
            String url, title, plot, date, id;
            double vote;

            JSONObject MovieInfoObject = movieArray.getJSONObject(i);

            url = IMAGE_BASE_URL + SIZE + MovieInfoObject.getString(PATH);
            id =  MovieInfoObject.getString(ID);
            title = MovieInfoObject.getString(TITLE);
            plot = MovieInfoObject.getString(PLOT);
            vote = MovieInfoObject.getDouble(VOTE);
            date = MovieInfoObject.getString(DATE);

            resultInfo[i] = new MovieInfo(url, id, title, plot, vote, date);
        }
        return resultInfo;
    }

    @Override
    protected MovieInfo[] doInBackground(String... params) {

        // If there's no input params, there's nothing to look up. Verify size of params.
        if (params.length == 0) {
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String movieJsonStr = null;

        try {
            // Construct the URL for the movieDB query
            final String MOVIE_BASE_URL = "https://api.themoviedb.org/3/discover/movie?";
            final String QUERY_PARAM = "sort_by";
            final String API = "api_key";
            final String API_KEY = "254054661bab0aeec2c07cf3ac0d2ea2";

            Uri buildUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
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
            movieJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e("MovieGridFragment", "Error ", e);
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
                    Log.e("MovierFragment", "Error closing stream", e);
                }
            }
        }
        try {
            return getMovieDataFromJson(movieJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    // get data from the sever
    @Override
    protected void onPostExecute(MovieInfo[] result) {
        if (result != null) {
            MovieGridFragment.mMovieAdapter.clear();
            for (MovieInfo info : result) {
                MovieGridFragment.mMovieAdapter.add(info);
            }
        }
    }
}