package com.wan.hollout.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMLocationMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMVideoMessageBody;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.hyphenate.exceptions.HyphenateException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SubscriptionHandling;
import com.wan.hollout.R;
import com.wan.hollout.chat.ChatUtils;
import com.wan.hollout.components.ApplicationLoader;
import com.wan.hollout.ui.activities.ChatActivity;
import com.wan.hollout.ui.helpers.CircleTransform;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.AuthUtil;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutUtils;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONObject;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Wan Clem
 */
@SuppressWarnings("FieldCanBeLocal")
public class ConversationItemView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    @BindView(R.id.user_online_status)
    ImageView userOnlineStatusView;

    @BindView(R.id.from)
    HolloutTextView usernameEntryView;

    @BindView(R.id.txt_secondary)
    ChatMessageTextView userStatusOrLastMessageView;

    @BindView(R.id.unread_message_indicator)
    TextView unreadMessagesCountView;

    @BindView(R.id.icon_text)
    TextView iconText;

    @BindView(R.id.timestamp)
    TextView msgTimeStampView;

    @BindView(R.id.icon_back)
    RelativeLayout iconBack;

    @BindView(R.id.icon_front)
    RelativeLayout iconFront;

    @BindView(R.id.icon_profile)
    ImageView userPhotoView;

    @BindView(R.id.message_container)
    LinearLayout messageContainer;

    @BindView(R.id.icon_container)
    RelativeLayout iconContainer;

    @BindView(R.id.delivery_status_view)
    ImageView deliveryStatusView;

    @BindView(R.id.parent_layout)
    View parentView;

    protected EMCallBack messageSendCallback;
    protected EMCallBack messageReceiveCallback;

    public ParseObject parseObject;
    public Activity activity;

    private ParseQuery<ParseObject> objectStateQuery;

    private String searchString;

    private EMConversation emConversation;
    private EMMessage lastMessage;

    private ParseObject signedInUserObject;

    public ConversationItemView(Context context) {
        this(context, null);
    }

    public ConversationItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.chat_item, this);
    }

    private void init() {
        setOnClickListener(this);
        setOnLongClickListener(this);
        parentView.setOnClickListener(this);
    }

    public void bindData(Activity activity, String searchString, ParseObject parseObject) {
        this.searchString = searchString;
        this.activity = activity;
        this.signedInUserObject = AuthUtil.getCurrentUser();
        this.parseObject = parseObject;
        this.emConversation = EMClient.getInstance()
                .chatManager().getConversation(parseObject.getString(AppConstants.REAL_OBJECT_ID),
                        ChatUtils.getConversationType(parseObject.get(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)
                                ? AppConstants.CHAT_TYPE_SINGLE
                                : (parseObject.getInt(AppConstants.ROOM_TYPE) == AppConstants.CHAT_TYPE_GROUP)
                                ? AppConstants.CHAT_TYPE_GROUP : AppConstants.CHAT_TYPE_ROOM), true);

        init();
        loadParseObject(searchString);
        invalidateViewOnScroll();
    }

    private void applyProfilePicture(String profileUrl) {
        if (!TextUtils.isEmpty(profileUrl)) {
            Glide.with(activity).load(profileUrl)
                    .thumbnail(0.5f)
                    .crossFade()
                    .transform(new CircleTransform(activity))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(userPhotoView);
            userPhotoView.setColorFilter(null);
            iconText.setVisibility(View.GONE);
        } else {
            userPhotoView.setImageResource(R.drawable.bg_circle);
            userPhotoView.setColorFilter(getRandomMaterialColor("400"));
            iconText.setVisibility(View.VISIBLE);
        }
    }

    private void applyIconAnimation() {
        iconBack.setVisibility(View.GONE);
        resetIconYAxis(iconFront);
        iconFront.setVisibility(View.VISIBLE);
        iconFront.setAlpha(1);
    }

    private void resetIconYAxis(View view) {
        if (view.getRotationY() != 0) {
            view.setRotationY(0);
        }
    }

    /**
     * chooses a random color from array.xml
     */
    private int getRandomMaterialColor(String typeColor) {
        int returnColor = Color.GRAY;
        int arrayId = getResources().getIdentifier("mdcolor_" + typeColor, "array", activity.getPackageName());
        if (arrayId != 0) {
            TypedArray colors = getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getColor(index, Color.GRAY);
            colors.recycle();
        }
        return returnColor;
    }

    public int getMessageId() {
        return parseObject.getObjectId().hashCode();
    }

    public void loadParseObject(String searchString) {
        if (parseObject != null) {
            String userName = parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL) ? parseObject.getString(AppConstants.APP_USER_DISPLAY_NAME) : parseObject.getString(AppConstants.GROUP_OR_CHAT_ROOM_NAME);
            String userProfilePhoto = parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL) ? parseObject.getString(AppConstants.APP_USER_PROFILE_PHOTO_URL) : parseObject.getString(AppConstants.GROUP_OR_CHAT_ROOM_PHOTO_URL);
            if (StringUtils.isNotEmpty(userName)) {
                if (StringUtils.isNotEmpty(searchString)) {
                    usernameEntryView.setText(UiUtils.highlightTextIfNecessary(searchString, WordUtils.capitalize(userName),
                            ContextCompat.getColor(activity, R.color.hollout_color_three)));
                } else {
                    usernameEntryView.setText(WordUtils.capitalize(userName));
                }
                // displaying the first letter of From in icon text
                iconText.setText(WordUtils.capitalize(userName.substring(0, 1)));
            }
            // display profile image
            applyProfilePicture(userProfilePhoto);
            applyIconAnimation();
            if (emConversation != null) {
                int unreadMessagesCount = emConversation.getUnreadMsgCount();

                if (unreadMessagesCount > 0) {
                    UiUtils.showView(unreadMessagesCountView, true);
                    unreadMessagesCountView.setText(String.valueOf(unreadMessagesCount));
                    AppConstants.unreadMessagesPositions.put(getMessageId(), true);
                    userStatusOrLastMessageView.setTextColor(Color.BLACK);
                    userStatusOrLastMessageView.setTypeface(null, Typeface.BOLD);
                } else {
                    UiUtils.showView(unreadMessagesCountView, false);
                    AppConstants.unreadMessagesPositions.put(getMessageId(), false);
                    userStatusOrLastMessageView.setTypeface(null, Typeface.NORMAL);
                    userStatusOrLastMessageView.setTextColor(ContextCompat.getColor(activity, R.color.message));
                }

                lastMessage = emConversation.getLastMessage();
                if (parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)) {
                    JSONObject chatStates = parseObject.getJSONObject(AppConstants.APP_USER_CHAT_STATES);
                    if (chatStates != null) {
                        String chatStateToSignedInUser = chatStates.optString(signedInUserObject.getString(AppConstants.REAL_OBJECT_ID));
                        if (chatStateToSignedInUser.contains(activity.getString(R.string.typing))
                                && parseObject.getString(AppConstants.APP_USER_ONLINE_STATUS)
                                .equals(AppConstants.ONLINE)) {
                            userStatusOrLastMessageView.setTypeface(null, Typeface.BOLD);
                            userStatusOrLastMessageView.setText(activity.getString(R.string.typing));
                            userStatusOrLastMessageView.setTextColor(ContextCompat.getColor(getContext(), R.color.hollout_color_one));
                            UiUtils.showView(deliveryStatusView, false);
                            AppConstants.lastMessageAvailablePositions.put(getMessageId(), false);
                        } else {
                            doTheOtherThings();
                        }
                    } else {
                        doTheOtherThings();
                    }
                } else {
                    doTheOtherThings();
                }
            } else {
                HolloutLogger.d("LastMessageTracker", "Sorry, conversation does not even exist. Lolz.");
            }

            if (parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)) {
                UiUtils.showView(userOnlineStatusView, true);
                String userOnlineStatus = parseObject.getString(AppConstants.APP_USER_ONLINE_STATUS);
                if (userOnlineStatus != null && UiUtils.canShowPresence(parseObject, AppConstants.ENTITY_TYPE_CHATS, null)) {
                    if (parseObject.getString(AppConstants.APP_USER_ONLINE_STATUS).equals(AppConstants.ONLINE)
                            && HolloutUtils.isNetWorkConnected(activity)) {
                        userOnlineStatusView.setImageResource(R.drawable.ic_online);
                        AppConstants.onlinePositions.put(getMessageId(), true);
                    } else {
                        userOnlineStatusView.setImageResource(R.drawable.ic_offline_grey);
                        AppConstants.onlinePositions.put(getMessageId(), false);
                    }
                } else {
                    userOnlineStatusView.setImageResource(R.drawable.ic_offline_grey);
                    AppConstants.onlinePositions.put(getMessageId(), false);
                }
                AppConstants.parseUserAvailableOnlineStatusPositions.put(getMessageId(), true);
            } else {
                UiUtils.showView(userOnlineStatusView, false);
                AppConstants.parseUserAvailableOnlineStatusPositions.put(getMessageId(), false);
            }

            userPhotoView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    UiUtils.blinkView(view);
                    if (parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)) {
                        UiUtils.loadUserData(activity, parseObject);
                    }
                }

            });

            messageContainer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    ConversationItemView.this.performClick();
                }

            });

            iconContainer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    ConversationItemView.this.performClick();
                }

            });

        }

    }

    private void doTheOtherThings() {
        if (lastMessage != null) {
            HolloutLogger.d("LastMessageTracker", "Last Message in conversation is not null");
            UiUtils.showView(msgTimeStampView, true);
            long lastMessageTime = lastMessage.getMsgTime();
            parseObject.put(AppConstants.LAST_CONVERSATION_TIME_WITH, lastMessageTime);
            Date msgDate = new Date(lastMessageTime);
            if (msgDate.equals(new Date())) {
                //Msg received date = today
                String msgTime = AppConstants.DATE_FORMATTER_IN_12HRS.format(msgDate);
                msgTimeStampView.setText(msgTime);
            } else {
                msgTimeStampView.setText(UiUtils.getDaysAgo(AppConstants.DATE_FORMATTER_IN_BIRTHDAY_FORMAT.format(msgDate)) + ", " + AppConstants.DATE_FORMATTER_IN_12HRS.format(msgDate));
            }
            AppConstants.lastMessageAvailablePositions.put(getMessageId(), true);
            setupLastMessage(lastMessage);
        } else {
            HolloutLogger.d("LastMessageTracker", "Last Message in conversation is null");
            parseObject.put(AppConstants.LAST_CONVERSATION_TIME_WITH, 0);
            UiUtils.showView(msgTimeStampView, false);
            AppConstants.lastMessageAvailablePositions.put(getMessageId(), false);
            if (parseObject.getString(AppConstants.OBJECT_TYPE).equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)) {
                String userStatusString = parseObject.getString(AppConstants.APP_USER_STATUS);
                if (StringUtils.isNotEmpty(userStatusString) && UiUtils.canShowStatus(parseObject, AppConstants.ENTITY_TYPE_CHATS, null)) {
                    userStatusOrLastMessageView.setText(userStatusString);
                } else {
                    userStatusOrLastMessageView.setText(activity.getString(R.string.hey_there_holla_me_on_hollout));
                }
            } else {
                String groupDescription = parseObject.getString(AppConstants.ROOM_DESCRIPTION);
                if (StringUtils.isNotEmpty(groupDescription)) {
                    userStatusOrLastMessageView.setText(groupDescription);
                } else {
                    userStatusOrLastMessageView.setText(activity.getString(R.string.conferencing_happens_here));
                }
            }
        }
    }

    private void invalidateViewOnScroll() {
        UiUtils.showView(unreadMessagesCountView, AppConstants.unreadMessagesPositions.get(getMessageId()));
        UiUtils.showView(msgTimeStampView, AppConstants.lastMessageAvailablePositions.get(getMessageId()));
        UiUtils.showView(userOnlineStatusView, AppConstants.parseUserAvailableOnlineStatusPositions.get(getMessageId()));
        userOnlineStatusView.setImageResource(AppConstants.onlinePositions.get(getMessageId()) ? R.drawable.ic_online : R.drawable.ic_offline_grey);
        if (AppConstants.lastMessageAvailablePositions.get(getMessageId()) && lastMessage != null && lastMessage.direct() == EMMessage.Direct.SEND) {
            setupMessageReadStatus(lastMessage);
        } else {
            UiUtils.showView(deliveryStatusView, false);
        }
    }

    private void subscribeToUserChanges() {
        if (parseObject != null) {
            objectStateQuery = ParseQuery.getQuery(AppConstants.PEOPLE_GROUPS_AND_ROOMS);
            objectStateQuery.whereEqualTo(AppConstants.REAL_OBJECT_ID, parseObject.getString(AppConstants.REAL_OBJECT_ID));
            try {
                SubscriptionHandling<ParseObject> subscriptionHandling = ApplicationLoader.getParseLiveQueryClient().subscribe(objectStateQuery);
                subscriptionHandling.handleEvent(SubscriptionHandling.Event.UPDATE, new SubscriptionHandling.HandleEventCallback<ParseObject>() {
                    @Override
                    public void onEvent(ParseQuery<ParseObject> query, final ParseObject object) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                parseObject = object;
                                loadParseObject(searchString);
                            }
                        });
                    }
                });
            }catch (NullPointerException ignored){

            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscribeToUserChanges();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unSubscribeFromUserChanges();
    }

    private void unSubscribeFromUserChanges() {
        try {
            if (objectStateQuery != null) {
                ApplicationLoader.getParseLiveQueryClient().unsubscribe(objectStateQuery);
            }
        } catch (NullPointerException ignored) {

        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    @Override
    public void onClick(View v) {
        Intent viewProfileIntent = new Intent(activity, ChatActivity.class);
        parseObject.put(AppConstants.CHAT_TYPE, (getObjectType().equals(AppConstants.OBJECT_TYPE_INDIVIDUAL)
                ? AppConstants.CHAT_TYPE_SINGLE : getObjectType().equals(AppConstants.OBJECT_TYPE_GROUP)
                ? AppConstants.CHAT_TYPE_GROUP : AppConstants.CHAT_TYPE_ROOM));
        viewProfileIntent.putExtra(AppConstants.USER_PROPERTIES, parseObject);
        activity.startActivity(viewProfileIntent);
    }

    private String getObjectType() {
        return parseObject.getString(AppConstants.OBJECT_TYPE);
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    public void setupLastMessage(EMMessage message) {

        EMMessage.Type messageType = message.getType();

        if (message.direct() == EMMessage.Direct.SEND) {
            UiUtils.showView(deliveryStatusView, true);
            setMessageSendCallback();
            setupMessageReadStatus(message);
        } else {
            setMessageReceiveCallback();
            UiUtils.showView(deliveryStatusView, false);
        }

        if (messageType == EMMessage.Type.TXT) {
            EMTextMessageBody emTextMessageBody = (EMTextMessageBody) message.getBody();
            userStatusOrLastMessageView.setText(emTextMessageBody.getMessage());
            UiUtils.removeAllDrawablesFromTextView(userStatusOrLastMessageView);
        }

        if (messageType == EMMessage.Type.IMAGE) {
            UiUtils.attachDrawableToTextView(activity, userStatusOrLastMessageView, R.drawable.msg_status_cam, UiUtils.DrawableDirection.LEFT);
            EMImageMessageBody emImageMessageBody = (EMImageMessageBody) message.getBody();
            String messageBody = emImageMessageBody.getFileName();
            if (StringUtils.isNotEmpty(messageBody)) {
                userStatusOrLastMessageView.setText(messageBody);
            } else {
                userStatusOrLastMessageView.setText(activity.getString(R.string.photo));
            }
        }

        if (messageType == EMMessage.Type.VIDEO) {
            UiUtils.attachDrawableToTextView(activity, userStatusOrLastMessageView, R.drawable.msg_status_video, UiUtils.DrawableDirection.LEFT);
            EMVideoMessageBody emVideoMessageBody = (EMVideoMessageBody) message.getBody();
            String messageBody = emVideoMessageBody.getFileName();
            if (StringUtils.isNotEmpty(messageBody)) {
                userStatusOrLastMessageView.setText(messageBody);
            } else {
                userStatusOrLastMessageView.setText(activity.getString(R.string.video));
            }
        }

        if (messageType == EMMessage.Type.LOCATION) {
            EMLocationMessageBody emLocationMessageBody = (EMLocationMessageBody) message.getBody();
            String messageBody = emLocationMessageBody.getAddress();
            if (StringUtils.isNotEmpty(messageBody)) {
                userStatusOrLastMessageView.setText(messageBody);
            } else {
                userStatusOrLastMessageView.setText(activity.getString(R.string.location));
            }
        }

        if (messageType == EMMessage.Type.VOICE) {
            EMVoiceMessageBody emVoiceMessageBody = (EMVoiceMessageBody) message.getBody();
            String messageBody = emVoiceMessageBody.getFileName();
            if (StringUtils.isNotEmpty(messageBody)) {
                userStatusOrLastMessageView.setText(messageBody);
            } else {
                userStatusOrLastMessageView.setText(activity.getString(R.string.voice));
            }
        }

        if (messageType == EMMessage.Type.FILE) {
            String messageBody = null;
            try {
                String fileType = message.getStringAttribute(AppConstants.FILE_TYPE);
                switch (fileType) {
                    case AppConstants.FILE_TYPE_CONTACT:
                        messageBody = "Contact";
                        break;
                    case AppConstants.FILE_TYPE_AUDIO:
                        messageBody = "Music";
                        break;
                    case AppConstants.FILE_TYPE_DOCUMENT:
                        messageBody = "Document";
                        break;
                }
                userStatusOrLastMessageView.setText(messageBody);
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setMessageSendCallback() {

        if (messageSendCallback == null) {

            messageSendCallback = new EMCallBack() {

                @Override
                public void onSuccess() {
                    loadParseObject(searchString);
                }

                @Override
                public void onProgress(final int progress, String status) {

                }

                @Override
                public void onError(int code, String error) {

                }

            };

        }

        lastMessage.setMessageStatusCallback(messageSendCallback);

    }

    /**
     * set callback for receiving message
     */
    protected void setMessageReceiveCallback() {

        if (messageReceiveCallback == null) {

            messageReceiveCallback = new EMCallBack() {

                @Override
                public void onSuccess() {
                    loadParseObject(searchString);
                }

                @Override
                public void onProgress(final int progress, String status) {
                }

                @Override
                public void onError(int code, String error) {

                }

            };

        }

        lastMessage.setMessageStatusCallback(messageReceiveCallback);

    }

    private void setupMessageReadStatus(EMMessage message) {
        if (message.isAcked()) {
            deliveryStatusView.setImageResource(R.drawable.msg_status_client_read);
        } else if (message.isDelivered()) {
            deliveryStatusView.setImageResource(R.drawable.msg_status_client_received_white);
        } else if (message.isListened()) {
            deliveryStatusView.setImageResource(R.drawable.msg_status_client_read);
        } else {
            deliveryStatusView.setImageResource(R.drawable.msg_status_server_receive);
        }
    }

}
