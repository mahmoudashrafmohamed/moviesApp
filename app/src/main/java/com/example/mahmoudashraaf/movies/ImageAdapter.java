package com.example.mahmoudashraaf.movies;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * create an Adapter based on ArrayAdapter to manage the movie information
 */
public class ImageAdapter extends ArrayAdapter<MovieInfo> {

    private Context mContext;
    private int layoutResourceId;
    private List<MovieInfo> mGridData = new ArrayList<>();
    private int imageViewResourceId;

    public ImageAdapter(Context mContext, int layoutResourceId,
                        int imageViewResourceId, List<MovieInfo> mGridData) {
        super(mContext, layoutResourceId, imageViewResourceId, mGridData);
        this.mContext = mContext;
        this.layoutResourceId = layoutResourceId;
        this.imageViewResourceId = imageViewResourceId;
        this.mGridData = mGridData;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageView imageView = (ImageView) convertView;
        if(imageView == null) {
            imageView = new ImageView(mContext);
        }

        // use Picasso to load image to imageView
        String image = mGridData.get(position).imageUrl;
        Picasso.with(mContext).load(image).into(imageView);

        // let imageView adjust its bounds to preserve the aspect ration of the image
        imageView.setAdjustViewBounds(true);

        return imageView;
    }

}