package sh.siava.pixelxpert.ui.preferences;

import static sh.siava.pixelxpert.utils.MiscUtils.dpToPx;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.List;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.utils.PreferenceHelper;

public class Utils {

    public static void setFirstAndLastItemMargin(@NonNull PreferenceViewHolder holder) {
        // Set margin for the first item
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();

        if (holder.getBindingAdapterPosition() == 0) {
            layoutParams.topMargin = dpToPx(12);
        } else {
            if (holder.getBindingAdapter() != null) {
                // Set margin for the last item
                if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
                    layoutParams.bottomMargin = dpToPx(18);
                } else {
                    layoutParams.topMargin = 0;
                    layoutParams.bottomMargin = dpToPx(2);
                }
            }
        }

        holder.itemView.setLayoutParams(layoutParams);
    }

    public static void setBackgroundResource(Preference preference, PreferenceViewHolder holder) {
        PreferenceGroup parent = preference.getParent();

        if (parent != null) {
            List<Preference> visiblePreferences = new ArrayList<>();

            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref.getKey() != null && PreferenceHelper.isVisible(pref.getKey()) && !(pref instanceof IllustrationPreference || pref instanceof MaterialMainSwitchPreference)) {
                    visiblePreferences.add(pref);
                }
            }

            int itemCount = visiblePreferences.size();
            int position = visiblePreferences.indexOf(preference);

            if (itemCount == 1) {
                holder.itemView.setBackgroundResource(R.drawable.container_single);
            } else if (itemCount > 1) {
                if (position == 0) {
                    holder.itemView.setBackgroundResource(R.drawable.container_top);
                } else if (position == itemCount - 1) {
                    holder.itemView.setBackgroundResource(R.drawable.container_bottom);
                } else {
                    holder.itemView.setBackgroundResource(R.drawable.container_mid);
                }
            }

            holder.itemView.setClipToOutline(true);
            holder.setDividerAllowedAbove(false);
            holder.setDividerAllowedBelow(false);
        }
    }
}
