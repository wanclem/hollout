package com.wan.hollout.ui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.parse.ParseObject;
import com.parse.ParseUser;
import com.wan.hollout.R;
import com.wan.hollout.animations.BounceInterpolator;
import com.wan.hollout.components.ApplicationLoader;
import com.wan.hollout.ui.widgets.HolloutTextView;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutUtils;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Wan Clem
 */

public class PeopleToMeetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ParseObject> people;
    private LayoutInflater layoutInflater;
    private Context context;
    private String searchedString;
    private static SparseBooleanArray selections;

    public PeopleToMeetAdapter(Context context, List<ParseObject> people) {
        this.context = context;
        this.people = people;
        this.layoutInflater = LayoutInflater.from(context);
        selections = new SparseBooleanArray();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = layoutInflater.inflate(R.layout.chip, parent, false);
        return new PeopleToMeetViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        PeopleToMeetViewHolder peopleToMeetViewHolder = (PeopleToMeetViewHolder) holder;
        ParseObject careerObject = people.get(position);
        if (careerObject != null) {
            peopleToMeetViewHolder.bindPerson(context, careerObject, getSearchedString());
        }
    }

    @Override
    public int getItemCount() {
        return people != null ? people.size() : 0;
    }

    public void setSearchedString(String searchedString) {
        this.searchedString = searchedString;
    }

    private String getSearchedString() {
        return searchedString;
    }

    @SuppressWarnings("WeakerAccess")
    static class PeopleToMeetViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.chip_item)
        HolloutTextView chipItemView;

        private Animation bounceAnimation;
        private BounceInterpolator bounceInterpolator;

        PeopleToMeetViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            bounceAnimation = AnimationUtils.loadAnimation(ApplicationLoader.getInstance(), R.anim.bounce);
            bounceInterpolator = new BounceInterpolator(0.2, 20);
            bounceAnimation.setInterpolator(bounceInterpolator);
        }

        void bindPerson(final Context context, final ParseObject personObject, String searchedString) {
            if (personObject != null) {
                final String personName = personObject.getString(AppConstants.NAME);
                boolean selected = personObject.getBoolean(AppConstants.SELECTED);
                setCareerName(context, searchedString, WordUtils.capitalize(personName.toLowerCase(Locale.getDefault())));
                selections.put(personName.hashCode(), selected);
                refreshViewState(context, personName);
                handleItemViewClick(context, personObject);
            }
        }

        private void setCareerName(Context context, String searchedString, String careerName) {
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(searchedString)) {
                chipItemView.setText(UiUtils.highlightTextIfNecessary(searchedString, WordUtils.capitalize(careerName + "s"),
                        ContextCompat.getColor(context, R.color.hollout_color_three)));
            } else {
                chipItemView.setText(WordUtils.capitalize(careerName + "s".toLowerCase(Locale.getDefault())));
            }
        }

        private void handleItemViewClick(final Context context, final ParseObject personObject) {
            final ParseUser parseUser = ParseUser.getCurrentUser();
            final String personName = personObject.getString(AppConstants.NAME);
            final List<String> selectedPeopleToMeet = parseUser.getList(AppConstants.INTERESTS);
            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    itemView.startAnimation(bounceAnimation);
                    if (!selectedPeopleToMeet.contains(personName)) {
                        HolloutUtils.bangSound(context, true, R.raw.tipjar_send);
                        selections.put(personName.hashCode(), true);
                        selectedPeopleToMeet.add(personName);
                        parseUser.put(AppConstants.INTERESTS, selectedPeopleToMeet);
                        personObject.put(AppConstants.SELECTED, true);
                    } else {
                        if (selectedPeopleToMeet.contains(personName)) {
                            selections.put(personName.hashCode(), false);
                            selectedPeopleToMeet.remove(personName);
                            parseUser.put(AppConstants.INTERESTS, selectedPeopleToMeet);
                            personObject.put(AppConstants.SELECTED, false);
                        }
                    }
                    refreshViewState(context, personName);
                }

            });

        }

        private void setChipBackground(Drawable background) {
            chipItemView.setBackground(background);
        }

        private void refreshViewState(Context context, String name) {
            boolean isSelected = selections.get(name.hashCode());
            String TAG = "CareersTag";
            HolloutLogger.d(TAG, "Selection State Value = " + isSelected);
            setChipBackground(isSelected ?
                    ContextCompat.getDrawable(context, R.drawable.chip_selected) :
                    ContextCompat.getDrawable(context, R.drawable.chip_unselected));
        }

    }

}
