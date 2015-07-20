package az.aldoziflaj.popmovies.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import az.aldoziflaj.popmovies.Constants;
import az.aldoziflaj.popmovies.R;
import az.aldoziflaj.popmovies.Utility;
import az.aldoziflaj.popmovies.activities.MovieDetailsActivity;
import az.aldoziflaj.popmovies.adapters.MovieAdapter;

public class AllMoviesFragment extends Fragment {
    public static final String LOG_TAG = AllMoviesFragment.class.getSimpleName();
    ArrayList<HashMap<String, String>> movieList;
    MovieAdapter movieAdapter;
    GridView moviesGridView;

    public AllMoviesFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        moviesGridView = (GridView) rootView.findViewById(R.id.movies_gridview);

        movieList = new ArrayList<>();
        /*
        // For testing only!
        String[] moviesTitleList = {
                "Interstellar",
                "Jurasic World",
                "Mad Max",
                "Kingsman: The Secret Service"
        };

        String[] moviesPosterList = {
                "/nBNZadXqJSdt05SHLqgT0HuC5Gm.jpg", //interstellar
                "/uXZYawqUsChGSj54wcuBtEdUJbh.jpg", //jurasic world
                "/kqjL17yufvn9OVLyXYpvtyrFfak.jpg", //mad max
                "/oAISjx6DvR2yUn9dxj00vP8OcJJ.jpg" //kingsman
        };

        for (int i=0; i<4; i++) {
            HashMap<String, String> tmpMovie = new HashMap<>();
            tmpMovie.put(Constants.Movie.MOVIE_TITLE, moviesTitleList[i]);
            tmpMovie.put(Constants.Movie.MOVIE_POSTER, moviesPosterList[i]);
            movieList.add(tmpMovie);
        }
        //*/

        movieAdapter = new MovieAdapter(
                getActivity(),
                movieList, // list of data to show
                R.layout.movie_poster, // the layout of a single item
                new String[]{Constants.Movie.MOVIE_POSTER},
                new int[]{R.id.movie_poster});

        moviesGridView.setAdapter(movieAdapter);

        moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> itemClicked = movieList.get(position);
                String movieTitle = itemClicked.get(Constants.Movie.MOVIE_TITLE);
                String moviePoster = itemClicked.get(Constants.Movie.MOVIE_POSTER);
                String movieReleaseDate = itemClicked.get(Constants.Movie.MOVIE_RELEASE_DATE);
                String movieRating = itemClicked.get(Constants.Movie.MOVIE_RATING);
                String movieTotalVotes = itemClicked.get(Constants.Movie.MOVIE_TOTAL_VOTES);
                String movieOverview = itemClicked.get(Constants.Movie.MOVIE_OVERVIEW);

                Intent detailsIntent = new Intent(getActivity(), MovieDetailsActivity.class);
                detailsIntent.putExtra(Constants.Movie.MOVIE_TITLE, movieTitle);
                detailsIntent.putExtra(Constants.Movie.MOVIE_POSTER, moviePoster);
                detailsIntent.putExtra(Constants.Movie.MOVIE_RELEASE_DATE, movieReleaseDate);
                detailsIntent.putExtra(Constants.Movie.MOVIE_RATING, movieRating);
                detailsIntent.putExtra(Constants.Movie.MOVIE_TOTAL_VOTES, movieTotalVotes);
                detailsIntent.putExtra(Constants.Movie.MOVIE_OVERVIEW, movieOverview);

                startActivity(detailsIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_allmovies, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            updateMovies();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    private void updateMovies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = prefs.getString(
                getString(R.string.movie_sort_key),
                getString(R.string.movie_sort_default));

        Log.d(LOG_TAG, sortOrder);

        FetchMoviesTask task = new FetchMoviesTask();
        task.execute(sortOrder);
    }

    class FetchMoviesTask extends AsyncTask<String, Void, String> {
        public final String LOG_TAG = FetchMoviesTask.class.getSimpleName();
        Toast errorInConnection;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            errorInConnection = Toast.makeText(getActivity(),
                    "Can't connect to the server",
                    Toast.LENGTH_LONG);
            dialog = ProgressDialog.show(getActivity(), "Please wait", "Updating the movies");
        }

        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                Log.e(LOG_TAG, "Why is this called without params?!");
                return null;
            }

            String sortOrder;
            if (params[0].equals(getString(R.string.movie_sort_default))) {
                sortOrder = Constants.Api.SORT_BY_POPULARITY;
            } else {
                sortOrder = Constants.Api.SORT_BY_VOTES;
            }

            HttpURLConnection urlConnection = null;
            String moviesJSONString = null;
            BufferedReader reader = null;

            try {
                Uri moviesUri = Uri.parse(Constants.Api.API_BASE_URL).buildUpon()
                        .appendQueryParameter(Constants.Api.SORT_KEY, sortOrder)
                        .appendQueryParameter(Constants.Api.API_KEY_QUERY, Constants.Api.API_KEY)
                        .build();

                Log.d(LOG_TAG, moviesUri.toString());

                URL url = new URL(moviesUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                if (sb.length() == 0) {
                    Log.d(LOG_TAG, "No response from the server");
                    return null;
                }

                moviesJSONString = sb.toString();

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error: " + e.getMessage());
                return null;

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error: " + e.getMessage());
                return null;

            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error: " + e.getMessage());

                    }
                }
            }

            return moviesJSONString;
        }

        @Override
        protected void onPostExecute(String moviesJsonString) {
            if (moviesJsonString == null) {
                errorInConnection.show();
                return;
            }

            movieList = fetchMovieListFromJSON(moviesJsonString);
            Log.d(LOG_TAG, "movieList updated");

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            movieAdapter = new MovieAdapter(
                    getActivity(),
                    movieList, // list of data to show
                    R.layout.movie_poster, // the layout of a single item
                    new String[]{Constants.Movie.MOVIE_POSTER},
                    new int[]{R.id.movie_poster}
            );

            moviesGridView.setAdapter(movieAdapter);
        }

        private ArrayList<HashMap<String, String>> fetchMovieListFromJSON(String jsonString) {
            ArrayList<HashMap<String, String>> kvPair = new ArrayList<>();

            try {
                JSONArray jsonMovieList = (new JSONObject(jsonString)).getJSONArray("results");
                int movieListLength = jsonMovieList.length();
                Log.d(LOG_TAG, movieListLength + " items fetched");

                for (int i = 0; i < movieListLength; i++) {
                    JSONObject currentMovie = jsonMovieList.getJSONObject(i);
                    HashMap<String, String> item = new HashMap<>();

                    //get the movie data from the JSON response
                    item.put(Constants.Movie.MOVIE_ID,
                            currentMovie.getString(Constants.Api.ID_KEY));

                    item.put(Constants.Movie.MOVIE_TITLE,
                            currentMovie.getString(Constants.Api.ORIGINAL_TITLE_KEY));

                    item.put(Constants.Movie.MOVIE_POSTER,
                            currentMovie.getString(Constants.Api.POSTER_PATH_KEY));

                    item.put(Constants.Movie.MOVIE_RATING,
                            currentMovie.getString(Constants.Api.VOTE_AVERAGE_KEY));

                    item.put(Constants.Movie.MOVIE_TOTAL_VOTES,
                            currentMovie.getString(Constants.Api.TOTAL_VOTES_KEY));

                    item.put(Constants.Movie.MOVIE_RELEASE_DATE,
                            //releaseDateFormatter(currentMovie.getString(Constants.Api.RELEASE_DATE_KEY)));
                            Utility.releaseDateFormatter(currentMovie.getString(Constants.Api.RELEASE_DATE_KEY)));

                    item.put(Constants.Movie.MOVIE_OVERVIEW,
                            currentMovie.getString(Constants.Api.OVERVIEW_KEY));

                    kvPair.add(item);
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error: " + e.getMessage());
                e.printStackTrace();
            }

            return kvPair;
        }

        private String releaseDateFormatter(String unformattedDate) {
            StringBuilder sb = new StringBuilder();
            String[] explodedDate = unformattedDate.split("-");

            sb.append(explodedDate[2])                  //day of month
                    .append("/").append(explodedDate[1])    //month
                    .append("/").append(explodedDate[0]);   //year

            //Log.d(LOG_TAG, sb.toString());
            return sb.toString();
        }

    }
}