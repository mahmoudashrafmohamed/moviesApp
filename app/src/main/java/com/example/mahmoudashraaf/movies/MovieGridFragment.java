package com.example.mahmoudashraaf.movies;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Set;

/**
 * Encapsulate fetching the movie information and displaying it as a GridView layout.
 */

public  class MovieGridFragment extends Fragment {

    public static ImageAdapter mMovieAdapter;
    private ArrayList<MovieInfo> movieList;
    private final String MOVIEKEY = "movies";
    private final String POSITIONKEY = "position";
    public Callback mCallback;
    private int mPosition = GridView.INVALID_POSITION;
    private GridView mGridView;

    public static SharedPreferences favoritesPref;
    public static SharedPreferences.Editor favoritesPrefEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey(MOVIEKEY)) {
            movieList = new ArrayList<>();
        }
        else {
            movieList = savedInstanceState.getParcelableArrayList(MOVIEKEY);
        }
        favoritesPref = getActivity().getSharedPreferences(
                getString(R.string.pref_favorites_key),
                Context.MODE_PRIVATE);
        favoritesPrefEditor = favoritesPref.edit();
    }

    public MovieGridFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected item needs to be saved.
        // When no item is selected, mPosition will be set to GridView.INVALID_POSITION,
        // so check for that before storing.
        outState.putParcelableArrayList(MOVIEKEY, movieList);
        if (mPosition != GridView.INVALID_POSITION) {
            outState.putInt(POSITIONKEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and use it to populate the GridView
        // it's attached to.
        mMovieAdapter =
                new ImageAdapter(
                        getActivity(),
                        R.layout.grid_item_movies,
                        R.id.grid_item_moview_imageview,
                        movieList);

        // create a root view for the Fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the GridView and attach this adapter to it.
        mGridView = (GridView) rootView.findViewById(R.id.gridview_movies);
        mGridView.setAdapter(mMovieAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                MovieInfo movieInfo = mMovieAdapter.getItem(position);
                ((Callback) getActivity()).onItemSelected(movieInfo);
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(MOVIEKEY)) {
            // The GridView probably hasn't even been populated yet.
            mPosition = savedInstanceState.getInt(POSITIONKEY);
            if (mPosition != GridView.INVALID_POSITION) {
                // If we don't need to restart the loader, and there's a desired position to restore
                // to, do so now.
                mGridView.smoothScrollToPosition(mPosition);
            }
        }
        return rootView;
    }

    private void updateMovie() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String rank = prefs.getString(getString(R.string.pref_rank_key),
                getString(R.string.pref_rank_default));

        if(!rank.equals("favorite")) {
            FetchMovieTask MovieTask = new FetchMovieTask();
            MovieTask.execute(rank);
        }
        else {
            mMovieAdapter.clear();
            Set<String> favoritesSet = favoritesPref.getStringSet(
                    getString(R.string.pref_favorites_key),
                    null
            );
            if (favoritesSet != null) {
                for (String s : favoritesSet) {
                    Gson gson = new Gson();
                    MovieInfo favorite = gson.fromJson(s, MovieInfo.class);
                    mMovieAdapter.add(favorite);
                }
            }
            else {
                Toast.makeText(
                        getActivity(),
                        getString(R.string.empty_favorite_list),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovie();
    }


    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(MovieInfo movieInfo);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onItemSelected");
        }

    }
}

