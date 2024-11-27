package sh.siava.pixelxpert.ui.fragments.iconpack;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.FragmentIconPackCustomizationBinding;
import sh.siava.pixelxpert.ui.adapter.IconPackCustomizationAdapter;
import sh.siava.pixelxpert.ui.adapter.ItemChangedListener;
import sh.siava.pixelxpert.ui.fragments.BaseFragment;
import sh.siava.pixelxpert.utils.IconPackUtil;

public class IconPackCustomizationFragment extends BaseFragment implements IconPackUtil.IconPackQueryListener {

	private FragmentIconPackCustomizationBinding binding;
	private IconPackUtil mIconPackUtil;
	private IconPackCustomizationAdapter mAdapter;

	@Override
	public String getTitle() {
		return getString(R.string.icon_pack_customization_title);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentIconPackCustomizationBinding.inflate(inflater, container, false);

		mIconPackUtil = IconPackUtil.getInstance(requireContext());
		mIconPackUtil.addListener(this);
		mIconPackUtil.queryIconPacks(false);

		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
	}

	@Override
	public void onIconPacksLoaded(IconPackUtil.ResourceMapping mapping, IconPackUtil.IconPackMapping packMapping) {
		List<IconPackUtil.IconPack> iconPacks = new ArrayList<>(packMapping.keySet());
		iconPacks.sort((o1, o2) -> {
			int nameComparison = o1.mName.compareTo(o2.mName);
			if (nameComparison == 0) {
				return o1.mPackageName.compareTo(o2.mPackageName);
			}
			return nameComparison;
		});
		new Handler(Looper.getMainLooper()).post(this::requestFabVisibility);
		mAdapter = new IconPackCustomizationAdapter(mIconPackUtil, mapping, mItemChangedListener);
		binding.recyclerView.post(() -> binding.recyclerView.setAdapter(mAdapter));
	}

	private final ItemChangedListener mItemChangedListener = () -> new Handler(Looper.getMainLooper()).post(this::requestFabVisibility);

	public boolean isFabVisible() {
		if (mIconPackUtil.mResourceMapping == null) {
			return false;
		}
        return mIconPackUtil.mResourceMapping.values().stream()
                .flatMap(List::stream)
                .anyMatch(IconPackUtil.ReplacementIcon::isEnabled);
	}

	private void requestFabVisibility() {
		if (getParentFragment() instanceof IconPackFragment iconPackFragment) {
			iconPackFragment.requestFabVisibility();
		}
	}

	public void query(String mSearchQuery) {
		mAdapter.filter(mSearchQuery);
	}

}