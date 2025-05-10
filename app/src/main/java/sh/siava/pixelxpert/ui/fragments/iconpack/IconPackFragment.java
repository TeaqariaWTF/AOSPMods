package sh.siava.pixelxpert.ui.fragments.iconpack;

import static sh.siava.pixelxpert.utils.MiscUtils.dpToPx;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.FragmentIconPackBinding;
import sh.siava.pixelxpert.ui.fragments.BaseFragment;
import sh.siava.pixelxpert.utils.IconPackUtil;

public class IconPackFragment extends BaseFragment implements IconPackUtil.IconPackQueryListener {

	private IconPackUtil mIconPackUtil;
	private FragmentIconPackBinding binding;

	// Search
    public String mSearchQuery = "";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		binding = FragmentIconPackBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mIconPackUtil = IconPackUtil.getInstance(requireContext());
		mIconPackUtil.addListener(this);

		IconPackCollectionAdapter fragmentCollectionAdapter = new IconPackCollectionAdapter(this);
		binding.pager.setAdapter(fragmentCollectionAdapter);
		String[] mTabTitles = {getString(R.string.icon_pack_selection_title), getString(R.string.icon_pack_customization_title)};
		new TabLayoutMediator(binding.tabLayout, binding.pager,
				(tab, position) -> tab.setText(mTabTitles[position])
		).attach();
		binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				binding.resetButton.setVisibility(isFabVisible() ? View.VISIBLE : View.GONE);
				submitQuery();
			}
		});
		binding.resetButton.setOnClickListener(v -> resetIconPacks());

		ViewGroup tabs = (ViewGroup) binding.tabLayout.getChildAt(0);
		int tabCount = tabs.getChildCount();
		Log.i("IconPackFragment", "tabCount: " + tabCount);
		for (int i = 0; i < tabCount; i++) {
			View tab = tabs.getChildAt(i);
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) tab.getLayoutParams();
			if (i != 0) layoutParams.setMarginStart(dpToPx(6));
			if (i != tabCount - 1) layoutParams.setMarginEnd(dpToPx(6));
			tab.setLayoutParams(layoutParams);
			tab.requestLayout();
		}
		binding.tabLayout.requestLayout();
	}

	@Override
	public void onIconPacksLoaded(IconPackUtil.ResourceMapping mapping, IconPackUtil.IconPackMapping packMapping) {
		new Handler(Looper.getMainLooper()).post(() -> {
			binding.loadingIndicator.setVisibility(View.GONE);

			boolean iconPackAvailable = packMapping != null && !packMapping.isEmpty();
			binding.tabLayout.setVisibility(iconPackAvailable ? View.VISIBLE : View.GONE);
			binding.pager.setVisibility(iconPackAvailable ? View.VISIBLE : View.GONE);
			binding.noPacksLayout.setVisibility(iconPackAvailable ? View.GONE : View.VISIBLE);
		});
	}

	public void submitQuery() {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof IconPackListFragment iconPackListFragment) {
			iconPackListFragment.query(mSearchQuery);
		} else if (currentFragment instanceof IconPackCustomizationFragment iconPackCustomizationFragment) {
			iconPackCustomizationFragment.query(mSearchQuery);
		}
	}

	private boolean isFabVisible() {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof IconPackListFragment iconPackListFragment) {
			return iconPackListFragment.isFabVisible();
		} else if (currentFragment instanceof IconPackCustomizationFragment iconPackCustomizationFragment) {
			return iconPackCustomizationFragment.isFabVisible();
		}
		return false;
	}

	public void requestFabVisibility() {
		binding.resetButton.setVisibility(isFabVisible() ? View.VISIBLE : View.GONE);
	}

	private Fragment getCurrentFragment() {
		int currentItem = binding.pager.getCurrentItem();
		String fragmentTag = "f" + currentItem;
		return getChildFragmentManager().findFragmentByTag(fragmentTag);
	}

	private void resetIconPacks() {
		IconPackUtil iconPackUtil = IconPackUtil.getInstance(requireContext());
		for (IconPackUtil.IconPack iconPack : iconPackUtil.mIconPackMapping.getIconPacks()) {
			iconPackUtil.disable(iconPack);
		}
		iconPackUtil.queryIconPacks(false);
	}

	@Override
	public String getTitle() {
		return getString(R.string.icon_packs_title);
	}

	private static class IconPackCollectionAdapter extends FragmentStateAdapter {

		public IconPackCollectionAdapter(Fragment fragment) {
			super(fragment);
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			Fragment fragment;
			if (position == 0) {
				fragment = new IconPackListFragment();
			} else {
				fragment = new IconPackCustomizationFragment();
			}
			return fragment;
		}

		@Override
		public int getItemCount() {
			return 2;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mIconPackUtil.queryIconPacks(true);
	}

}
