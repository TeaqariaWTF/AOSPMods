package sh.siava.pixelxpert.ui.preferences;

import static sh.siava.pixelxpert.utils.MiscUtils.dpToPx;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceViewHolder;

import sh.siava.pixelxpert.R;

public class MaterialMultiSelectListPreference extends MultiSelectListPreference {

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context) {
        super(context);
        initResource();
    }

    private void initResource() {
        setLayoutResource(R.layout.custom_preference_list);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (holder.getBindingAdapterPosition() == 0) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            layoutParams.topMargin = dpToPx(12);
            holder.itemView.setLayoutParams(layoutParams);
        } else {
            if (holder.getBindingAdapter() != null) {
                if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
                    layoutParams.bottomMargin = dpToPx(0);
                    holder.itemView.setLayoutParams(layoutParams);
                }
            }
        }
    }

}
