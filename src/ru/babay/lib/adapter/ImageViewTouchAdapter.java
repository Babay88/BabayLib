package ru.babay.lib.adapter;

import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import ru.babay.lib.model.Image;
import ru.babay.lib.model.PhotosetPhoto;
import ru.babay.lib.view.LoadableImageView;
import ru.babay.lib.view.TouchImageView;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 22.08.13
 * Time: 20:32
 */
public class ImageViewTouchAdapter extends PagerAdapter {
    Image[] images;
    int defDrawableId = 0;

    public ImageViewTouchAdapter(Image[] images) {
        this.images = images;
    }

    public ImageViewTouchAdapter(PhotosetPhoto[] photos){
        if (photos == null){
            images = null;
            return;
        }

        images = new Image[photos.length];
        for (int i=0; i< photos.length; i++){
            images[i] = photos[i].getLargeImage();
        }
    }

    public void setDefDrawableId(int defDrawableId) {
        this.defDrawableId = defDrawableId;
    }

    @Override
    public int getCount() {
        return images == null ? 0 : images.length;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (object instanceof RelativeLayout){
            View v= ((RelativeLayout)object).getChildAt(0);
            ((LoadableImageView) v).loadImageifNotLoaded(false);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        RelativeLayout rl = new RelativeLayout(container.getContext());
        container.addView(rl);
        final LoadableImageView view = new TouchImageView(container.getContext());
        view.setImage(null, images[position]);
        view.setLoadParams(0, defDrawableId, true);
        rl.addView(view, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rl.setTag(images[position]);
        DisplayMetrics dm = container.getContext().getResources().getDisplayMetrics();
        int width = (int)(dm.widthPixels / dm.density / 2);

        view.loadPreview(width, 0);
        //view.loadImage();
        rl.setTag(position);
        return rl;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }



}
