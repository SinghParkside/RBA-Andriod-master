package edu.uwp.appfactory.wishope.views.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.uwp.appfactory.wishope.R;

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
public class CustomClusterRenderer extends DefaultClusterRenderer<LocationClusterItem> implements ClusterManager.OnClusterItemClickListener<LocationClusterItem> {
    private final IconGenerator mIconGenerator;
    private final ImageView mImageView;
    private int mDimension;
    @NotNull
    private Context context;

    public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<LocationClusterItem> clusterManager) {
        super(context, map, clusterManager);
        mIconGenerator = new IconGenerator(context);  // 3
        mImageView = new ImageView(context);
//        mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        mIconGenerator.setBackground(context.getResources().getDrawable(R.drawable.transparent));
        mIconGenerator.setContentView(mImageView);  // 4
    }

    protected void onBeforeClusterItemRendered(@NotNull LocationClusterItem item, @Nullable MarkerOptions markerOptions) {
        int iconRes = 0;

        switch (item.getType()) {
            case "ADOLESCENTS":
                iconRes = R.drawable.ic_adolescents;
                break;
            case "HOMELESS SHELTER":
                iconRes = R.drawable.ic_homeless;
                break;
            case "OUTPATIENT":
                iconRes = R.drawable.ic_outpatient;
                break;
            case "RESIDENTIAL":
                iconRes = R.drawable.ic_residential;
                break;
            case "RESOURCE / COMM ORG":
                iconRes = R.drawable.ic_resource;
                break;
            case "SOBER LIVING":
                iconRes = R.drawable.ic_soberliving;
                break;
            case "":
                iconRes = R.drawable.ic_notypeen;
                break;
        }

        mImageView.setImageResource(iconRes);
        Bitmap icon = mIconGenerator.makeIcon();

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    public boolean onClusterItemClick(LocationClusterItem locationClusterItem) {
        System.out.println("YEEEET");
        return true;
    }
}
