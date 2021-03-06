package code.name.monkey.retromusic.ui.fragments.mainactivity.home;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import code.name.monkey.appthemehelper.ThemeStore;
import code.name.monkey.appthemehelper.util.ATHUtil;
import code.name.monkey.appthemehelper.util.TintHelper;
import code.name.monkey.retromusic.R;
import code.name.monkey.retromusic.dialogs.HomeOptionDialog;
import code.name.monkey.retromusic.helper.MusicPlayerRemote;
import code.name.monkey.retromusic.interfaces.MainActivityFragmentCallbacks;
import code.name.monkey.retromusic.loaders.SongLoader;
import code.name.monkey.retromusic.model.Album;
import code.name.monkey.retromusic.model.Artist;
import code.name.monkey.retromusic.model.Genre;
import code.name.monkey.retromusic.model.Playlist;
import code.name.monkey.retromusic.model.smartplaylist.HistoryPlaylist;
import code.name.monkey.retromusic.model.smartplaylist.LastAddedPlaylist;
import code.name.monkey.retromusic.model.smartplaylist.MyTopTracksPlaylist;
import code.name.monkey.retromusic.mvp.contract.HomeContract;
import code.name.monkey.retromusic.mvp.presenter.HomePresenter;
import code.name.monkey.retromusic.ui.adapter.GenreAdapter;
import code.name.monkey.retromusic.ui.adapter.album.AlbumFullWithAdapter;
import code.name.monkey.retromusic.ui.adapter.artist.ArtistAdapter;
import code.name.monkey.retromusic.ui.fragments.base.AbsMainActivityFragment;
import code.name.monkey.retromusic.util.Compressor;
import code.name.monkey.retromusic.util.NavigationUtil;
import code.name.monkey.retromusic.util.PreferenceUtil;
import code.name.monkey.retromusic.views.CircularImageView;
import code.name.monkey.retromusic.views.MetalRecyclerViewPager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static code.name.monkey.retromusic.Constants.USER_PROFILE;

public class BannerHomeFragment extends AbsMainActivityFragment implements
        MainActivityFragmentCallbacks,
        HomeContract.HomeView {

    private static final String TAG = "HomeFragment";
    Unbinder unbinder;
    @BindView(R.id.home_toolbar)
    Toolbar toolbar;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.user_image)
    CircularImageView userImage;
    @BindView(R.id.recycler_view)
    RecyclerView recentArtistRV;
    @BindView(R.id.recent_album)
    RecyclerView recentAlbumRV;
    @BindView(R.id.top_artist)
    RecyclerView topArtistRV;
    @BindView(R.id.top_album)
    MetalRecyclerViewPager topAlbumRV;
    @BindView(R.id.recent_artist_container)
    View recentArtistContainer;
    @BindView(R.id.recent_albums_container)
    View recentAlbumsContainer;
    @BindView(R.id.top_artist_container)
    View topArtistContainer;
    @BindView(R.id.top_albums_container)
    View topAlbumContainer;
    @BindView(R.id.genres)
    RecyclerView genresRecyclerView;
    @BindView(R.id.genre_container)
    LinearLayout genreContainer;
    @BindView(R.id.container)
    View container;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.search)
    ImageView search;


    private HomePresenter homePresenter;
    private CompositeDisposable disposable;

    public static BannerHomeFragment newInstance() {
        Bundle args = new Bundle();
        BannerHomeFragment fragment = new BannerHomeFragment();
        fragment.setArguments(args);
        return fragment;
    }


    private void loadImageFromStorage(ImageView imageView) {
        //noinspection ConstantConditions
        disposable.add(new Compressor(getContext())
                .setMaxHeight(300)
                .setMaxWidth(300)
                .setQuality(75)
                .setCompressFormat(Bitmap.CompressFormat.WEBP)
                .compressToBitmapAsFlowable(
                        new File(PreferenceUtil.getInstance(getContext()).getProfileImage(), USER_PROFILE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageView::setImageBitmap,
                        throwable -> imageView.setImageDrawable(ContextCompat
                                .getDrawable(getContext(), R.drawable.ic_person_flat))));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disposable = new CompositeDisposable();
        //noinspection ConstantConditions
        homePresenter = new HomePresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_banner_home, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setStatusbarColorAuto(view);
        getMainActivity().getSlidingUpPanelLayout().setShadowHeight(8);
        getMainActivity().setBottomBarVisibility(View.VISIBLE);

        setupToolbar();
        loadImageFromStorage(userImage);

        homePresenter.subscribe();
        checkPadding();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar() {
        //noinspection ConstantConditions
        int primaryColor = ThemeStore.primaryColor(getContext());
        appbar.setBackgroundColor(primaryColor);
        toolbar.setBackgroundColor(primaryColor);
        appbar.addOnOffsetChangedListener((appBarLayout, verticalOffset) ->
                getMainActivity().setLightStatusbar(!ATHUtil.isWindowBackgroundDark(getContext())));

        getActivity().setTitle(R.string.app_name);
        getMainActivity().setSupportActionBar(toolbar);

        title.setTextColor(ThemeStore.textColorPrimary(getContext()));
        TintHelper.setTintAuto(search, ThemeStore.textColorPrimary(getContext()), false);
        TintHelper.setTintAuto(container, primaryColor, true);
    }

    @Override
    public boolean handleBackPress() {
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        disposable.clear();
        homePresenter.unsubscribe();
    }

    @Override
    public void loading() {

    }

    @Override
    public void showEmptyView() {

    }

    @Override
    public void completed() {

    }

    @Override
    public void showData(ArrayList<Object> homes) {
        //homeAdapter.swapDataSet(homes);
    }

    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
        homePresenter.subscribe();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        checkPadding();
    }

    @Override
    public void onQueueChanged() {
        super.onQueueChanged();
        checkPadding();
    }

    private void checkPadding() {
        int height = getResources().getDimensionPixelSize(R.dimen.mini_player_height);
        container.setPadding(0, 0, 0, MusicPlayerRemote.getPlayingQueue().isEmpty() ? height * 2 : 0);
    }

    @Override
    public void recentArtist(ArrayList<Artist> artists) {
        recentArtistContainer.setVisibility(View.VISIBLE);
        recentArtistRV.setLayoutManager(new GridLayoutManager(getMainActivity(),
                1, GridLayoutManager.HORIZONTAL, false));
        ArtistAdapter artistAdapter = new ArtistAdapter(getMainActivity(), artists,
                R.layout.item_artist, false, null);
        recentArtistRV.setAdapter(artistAdapter);
    }

    @Override
    public void recentAlbum(ArrayList<Album> albums) {
        recentAlbumsContainer.setVisibility(View.VISIBLE);
        AlbumFullWithAdapter artistAdapter = new AlbumFullWithAdapter(getMainActivity(),
                getDisplayMetrics());
        artistAdapter.swapData(albums);
        recentAlbumRV.setAdapter(artistAdapter);
    }

    @Override
    public void topArtists(ArrayList<Artist> artists) {
        topArtistContainer.setVisibility(View.VISIBLE);
        topArtistRV.setLayoutManager(new GridLayoutManager(getMainActivity(),
                1, GridLayoutManager.HORIZONTAL, false));
        ArtistAdapter artistAdapter = new ArtistAdapter(getMainActivity(), artists,
                R.layout.item_artist, false, null);
        topArtistRV.setAdapter(artistAdapter);

    }

    @Override
    public void topAlbums(ArrayList<Album> albums) {
        topAlbumContainer.setVisibility(View.VISIBLE);
        AlbumFullWithAdapter artistAdapter = new AlbumFullWithAdapter(getMainActivity(),
                getDisplayMetrics());
        artistAdapter.swapData(albums);
        topAlbumRV.setAdapter(artistAdapter);
    }

    private DisplayMetrics getDisplayMetrics() {
        Display display = getMainActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        return metrics;
    }

    @Override
    public void suggestions(ArrayList<Playlist> playlists) {

    }


    @Override
    public void geners(ArrayList<Genre> genres) {
        genreContainer.setVisibility(View.VISIBLE);
        genresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //noinspection ConstantConditions
        GenreAdapter genreAdapter = new GenreAdapter(getActivity(), genres, R.layout.item_list);
        genresRecyclerView.setAdapter(genreAdapter);
    }


    @OnClick({R.id.last_added, R.id.top_played, R.id.action_shuffle, R.id.history,
            R.id.user_image, R.id.search})
    void startUserInfo(View view) {
        Activity activity = getActivity();
        if (activity != null) {
            switch (view.getId()) {
                case R.id.action_shuffle:
                    MusicPlayerRemote
                            .openAndShuffleQueue(SongLoader.getAllSongs(activity).blockingFirst(), true);
                    break;
                case R.id.last_added:
                    NavigationUtil.goToPlaylistNew(activity, new LastAddedPlaylist(activity));
                    break;
                case R.id.top_played:
                    NavigationUtil.goToPlaylistNew(activity, new MyTopTracksPlaylist(activity));
                    break;
                case R.id.history:
                    NavigationUtil.goToPlaylistNew(activity, new HistoryPlaylist(activity));
                    break;
                case R.id.search:
                    NavigationUtil.goToSearch(activity);
                    break;
                case R.id.user_image:
                    new HomeOptionDialog().show(getFragmentManager(), TAG);
                    break;
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        homePresenter.loadRecentArtists();
        homePresenter.loadRecentAlbums();
    }
}