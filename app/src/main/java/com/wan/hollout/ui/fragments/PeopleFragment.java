package com.wan.hollout.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.liucanwen.app.headerfooterrecyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.liucanwen.app.headerfooterrecyclerview.RecyclerViewUtils;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.wan.hollout.R;
import com.wan.hollout.callbacks.EndlessRecyclerViewScrollListener;
import com.wan.hollout.eventbuses.ConnectivityChangedAction;
import com.wan.hollout.eventbuses.SearchPeopleEvent;
import com.wan.hollout.ui.activities.MeetPeopleActivity;
import com.wan.hollout.ui.adapters.PeopleAdapter;
import com.wan.hollout.ui.helpers.DividerItemDecoration;
import com.wan.hollout.ui.widgets.HolloutTextView;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutUtils;
import com.wan.hollout.utils.SafeLayoutManager;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeopleFragment extends Fragment {

    @BindView(R.id.people_content_flipper)
    public ViewFlipper peopleContentFlipper;

    @BindView(R.id.swipe_refresh_layout)
    public SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.people_recycler_view)
    public RecyclerView peopleRecyclerView;

    @BindView(R.id.no_hollout_users_text_view)
    public HolloutTextView noHolloutTextView;

    @BindView(R.id.card_meet_people)
    CardView cardMeetPeople;

    @BindView(R.id.meet_people_textview)
    HolloutTextView meetPeopleTextView;

    private PeopleAdapter peopleAdapter;
    private List<ParseUser> people = new ArrayList<>();
    private ParseObject signedInUser;

    private View footerView;

    public String searchString;

    public PeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSignedInUser();
    }

    private void initSignedInUser() {
        if (signedInUser == null) {
            signedInUser = ParseUser.getCurrentUser();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAndRegEventBus();
    }

    @Override
    public void onResume() {
        super.onResume();
        initSignedInUser();
        checkAndRegEventBus();
    }

    private void checkAndRegEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void checkAnUnRegEventBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        checkAnUnRegEventBus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        checkAnUnRegEventBus();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_people, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initSignedInUser();
        if (getActivity() != null) {
            initBasicViews();
        }
        fetchPeopleOfCommonInterestFromCache();
    }

    private void fetchPeople() {
        swipeRefreshLayout.setRefreshing(true);
        fetchPeopleOfCommonInterestFromCache();
    }

    private void fetchPeopleOfCommonInterestFromCache() {
        ParseQuery<ParseUser> localUsersQuery = ParseUser.getQuery();
        localUsersQuery.fromLocalDatastore();
        localUsersQuery.whereNotEqualTo(AppConstants.APP_USER_ID, signedInUser.getString(AppConstants.APP_USER_ID));
        localUsersQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {
                if (objects != null && !objects.isEmpty()) {
                    loadAdapter(objects);
                    UiUtils.toggleFlipperState(peopleContentFlipper, 2);
                }
                fetchPeopleOfCommonInterestsFromNetwork(0);
            }
        });
    }

    @SuppressLint("InflateParams")
    private void initBasicViews() {
        footerView = getActivity().getLayoutInflater().inflate(R.layout.loading_footer, null);
        UiUtils.setUpRefreshColorSchemes(getActivity(), swipeRefreshLayout);
        peopleAdapter = new PeopleAdapter(getActivity(), people);

        HeaderAndFooterRecyclerViewAdapter headerAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(peopleAdapter);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                people.clear();
                fetchPeopleOfCommonInterestsFromNetwork(0);
            }

        });

        SafeLayoutManager linearLayoutManager = new SafeLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        peopleRecyclerView.setLayoutManager(linearLayoutManager);
        peopleRecyclerView.setItemAnimator(new DefaultItemAnimator());
        peopleRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        peopleRecyclerView.setAdapter(headerAndFooterRecyclerViewAdapter);
        RecyclerViewUtils.setFooterView(peopleRecyclerView,footerView);
        UiUtils.showView(footerView, false);

        peopleRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(linearLayoutManager) {

            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (!people.isEmpty() && people.size() >= 100) {
                    UiUtils.showView(footerView, true);
                    if (StringUtils.isNotEmpty(searchString)) {
                        searchPeople(people.size(), searchString);
                    } else {
                        fetchPeopleOfCommonInterestsFromNetwork(people.size());
                    }
                }
            }

        });

        cardMeetPeople.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                UiUtils.blinkView(view);
                if (meetPeopleTextView.getText().toString().equals(getString(R.string.screwed_data_error_message))) {
                    Intent dataSourceIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(dataSourceIntent);
                } else {
                    Intent interestsIntent = new Intent(getActivity(), MeetPeopleActivity.class);
                    startActivity(interestsIntent);
                }

            }

        });

    }

    public void fetchPeopleOfCommonInterestsFromNetwork(final int skip) {
        if (getActivity() != null) {
            if (HolloutUtils.isNetWorkConnected(getActivity())) {
                if (signedInUser != null) {
                    String signedInUserId = signedInUser.getString(AppConstants.APP_USER_ID);
                    List<String> savedUserChats = signedInUser.getList(AppConstants.APP_USER_CHATS);
                    String signedInUserCountry = signedInUser.getString(AppConstants.APP_USER_COUNTRY);
                    List<String> signedInUserInterests = signedInUser.getList(AppConstants.INTERESTS);

                    String startAgeValue = signedInUser.getString(AppConstants.START_AGE_FILTER_VALUE);
                    String endAgeValue = signedInUser.getString(AppConstants.END_AGE_FILTER_VALUE);

                    ArrayList<String> newUserChats = new ArrayList<>();
                    ParseQuery<ParseUser> peopleQuery = ParseUser.getQuery();

                    if (startAgeValue != null && endAgeValue != null) {
                        List<String> ageRanges = HolloutUtils.computeAgeRanges(startAgeValue, endAgeValue);
                        HolloutLogger.d("AgeRanges", TextUtils.join(",", ageRanges));
                        peopleQuery.whereContainedIn(AppConstants.APP_USER_AGE, ageRanges);
                    }

                    String genderFilter= signedInUser.getString(AppConstants.GENDER_FILTER);

                    if (genderFilter!=null && !genderFilter.equals(AppConstants.Both)){
                        peopleQuery.whereEqualTo(AppConstants.APP_USER_GENDER,genderFilter);
                    }

                    if (savedUserChats != null) {
                        if (!savedUserChats.contains(signedInUserId.toLowerCase())) {
                            savedUserChats.add(signedInUserId.toLowerCase());
                        }
                        peopleQuery.whereNotContainedIn(AppConstants.APP_USER_ID, savedUserChats);
                    } else {
                        if (!newUserChats.contains(signedInUserId)) {
                            newUserChats.add(signedInUserId);
                        }
                        peopleQuery.whereNotContainedIn(AppConstants.APP_USER_ID, newUserChats);
                    }
                    if (signedInUserCountry != null) {
                        peopleQuery.whereEqualTo(AppConstants.APP_USER_COUNTRY, signedInUserCountry);
                    }
                    if (signedInUserInterests != null) {
                        peopleQuery.whereContainedIn(AppConstants.ABOUT_USER, signedInUserInterests);
                    }
                    ParseGeoPoint signedInUserGeoPoint = signedInUser.getParseGeoPoint(AppConstants.APP_USER_GEO_POINT);
                    if (signedInUserGeoPoint != null) {
                        peopleQuery.whereWithinKilometers(AppConstants.APP_USER_GEO_POINT, signedInUserGeoPoint, 1000.0);
                    }
                    peopleQuery.setLimit(100);
                    if (skip != 0) {
                        peopleQuery.setSkip(skip);
                    }
                    peopleQuery.findInBackground(new FindCallback<ParseUser>() {

                        @Override
                        public void done(final List<ParseUser> users, final ParseException e) {
                            if (swipeRefreshLayout.isRefreshing()) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            if (e == null) {
                                if (skip == 0) {
                                    people.clear();
                                }
                                if (users != null) {
                                    loadAdapter(users);
                                }
                            } else {
                                if (e.getCode() == ParseException.CONNECTION_FAILED) {
                                    displayFetchErrorMessage(true);
                                } else {
                                    displayFetchErrorMessage(false);
                                }
                            }
                            if (!people.isEmpty()) {
                                UiUtils.toggleFlipperState(peopleContentFlipper, 2);
                                cacheListOfPeople();
                            } else {
                                displayFetchErrorMessage(false);
                            }
                            UiUtils.showView(footerView, false);
                        }
                    });
                }
            } else {
                displayFetchErrorMessage(true);
            }
        }
    }

    private void cacheListOfPeople() {
        ParseObject.unpinAllInBackground(AppConstants.APP_USERS, new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                ParseObject.pinAllInBackground(AppConstants.APP_USERS, people);
            }
        });
    }

    private void loadAdapter(List<ParseUser> users) {
        if (!users.isEmpty()) {
            for (ParseUser parseUser : users) {
                if (!people.contains(parseUser)) {
                    people.add(parseUser);
                }
            }
        }
        peopleAdapter.notifyDataSetChanged();
    }

    private void displayFetchErrorMessage(boolean networkError) {
        if (people.isEmpty() && getActivity() != null && signedInUser != null) {
            UiUtils.toggleFlipperState(peopleContentFlipper, 1);
            if (networkError) {
                noHolloutTextView.setText(getString(R.string.screwed_data_error_message));
                meetPeopleTextView.setText(getString(R.string.review_network));
            } else {
                noHolloutTextView.setText(getString(R.string.people_unavailable));
                meetPeopleTextView.setText(getString(R.string.meet_more_people));
            }
        }
    }

    private void searchPeople(final int skip, String searchString) {
        peopleAdapter.setSearchString(searchString);
        ParseQuery<ParseUser> parseUserParseQuery = ParseUser.getQuery();
        parseUserParseQuery.whereContains(AppConstants.APP_USER_DISPLAY_NAME, searchString.toLowerCase());
        if (signedInUser != null) {
            parseUserParseQuery.whereNotEqualTo(AppConstants.APP_USER_ID, signedInUser.getString(AppConstants.APP_USER_ID));
        }
        ParseQuery<ParseUser> categoryQuery = ParseUser.getQuery();
        if (signedInUser != null) {
            categoryQuery.whereNotEqualTo(AppConstants.APP_USER_ID, signedInUser.getString(AppConstants.APP_USER_ID));
        }
        List<String> elements = new ArrayList<>();
        elements.add(StringUtils.stripEnd(searchString.toLowerCase(), "s"));
        categoryQuery.whereContainsAll(AppConstants.ABOUT_USER, elements);
        List<ParseQuery<ParseUser>> queries = new ArrayList<>();
        queries.add(parseUserParseQuery);
        queries.add(categoryQuery);
        ParseQuery<ParseUser> joinedQuery = ParseQuery.or(queries);
        joinedQuery.setLimit(100);
        if (skip != 0) {
            joinedQuery.setSkip(skip);
        }
        joinedQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {
                if (e == null) {
                    if (objects != null && !objects.isEmpty()) {
                        if (skip == 0) {
                            people.clear();
                        }
                        for (ParseUser parseUser : objects) {
                            if (!people.contains(parseUser)) {
                                people.add(parseUser);
                            }
                        }
                        peopleAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public void onEventAsync(final Object o) {
        UiUtils.runOnMain(new Runnable() {
            @Override
            public void run() {
                if (o instanceof ConnectivityChangedAction) {
                    ConnectivityChangedAction connectivityChangedAction = (ConnectivityChangedAction) o;
                    if (connectivityChangedAction.isConnectivityChanged()) {
                        if (people.isEmpty()) {
                            UiUtils.toggleFlipperState(peopleContentFlipper, 0);
                            fetchPeopleOfCommonInterestsFromNetwork(0);
                        }
                    }
                } else if (o instanceof String) {
                    String action = (String) o;
                    switch (action) {
                        case AppConstants.DISABLE_NESTED_SCROLLING:
                            peopleRecyclerView.setNestedScrollingEnabled(false);
                            break;
                        case AppConstants.ENABLE_NESTED_SCROLLING:
                            peopleRecyclerView.setNestedScrollingEnabled(true);
                            break;
                        case AppConstants.REFRESH_PEOPLE:
                            fetchPeople();
                            break;
                        case AppConstants.SEARCH_VIEW_CLOSED:
                            peopleAdapter.setSearchString(null);
                            searchString = null;
                            fetchPeople();
                            break;
                    }
                } else if (o instanceof SearchPeopleEvent) {
                    SearchPeopleEvent searchPeopleEvent = (SearchPeopleEvent) o;
                    String queryString = searchPeopleEvent.getQueryString();
                    searchString = queryString;
                    searchPeople(0, queryString);
                }
            }
        });
    }

}
