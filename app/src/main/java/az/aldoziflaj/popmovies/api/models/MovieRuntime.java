package az.aldoziflaj.popmovies.api.models;

import com.google.gson.annotations.SerializedName;

public class MovieRuntime {

    @SerializedName("runtime")
    private int mRuntime;

    public int getRuntime() {
        return mRuntime;
    }
}
