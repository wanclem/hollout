package com.wan.hollout.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseObject;
import com.wan.hollout.R;
import com.wan.hollout.ui.utils.DateUtils;
import com.wan.hollout.ui.widgets.ConversationMessageView;
import com.wan.hollout.utils.HolloutUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Wan Clem
 */

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        StickyRecyclerHeadersAdapter<MessagesAdapter.MessagedDatesHeaderHolder> {

    private List<ParseObject> messages;
    private Context context;
    private LayoutInflater layoutInflater;
    private Calendar calendar;

    public MessagesAdapter(Context context, List<ParseObject> messages) {
        this.context = context;
        this.messages = messages;
        this.layoutInflater = LayoutInflater.from(context);
        calendar = Calendar.getInstance();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = layoutInflater.inflate(R.layout.chat_messages_recycler_item_list, parent, false);
        return new MessageItemsHolder(convertView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageItemsHolder messageItemsHolder = (MessageItemsHolder) holder;
        ParseObject messageObject = messages.get(position);
        if (messageObject != null) {
            messageItemsHolder.bindMessage(context, messageObject);
        }
    }

    @Override
    public String getHeaderId(int position) {
        ParseObject parseObject = messages.get(position);
        calendar.setTime(parseObject.getCreatedAt());
        return String.valueOf(HolloutUtils.hashCode(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR)));
    }

    @Override
    public MessagedDatesHeaderHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View messageDatesHeaderView = layoutInflater.inflate(R.layout.scroll_date_header, parent, false);
        return new MessagedDatesHeaderHolder(messageDatesHeaderView);
    }

    @Override
    public void onBindHeaderViewHolder(MessagedDatesHeaderHolder holder, int position) {
        ParseObject message = messages.get(position);
        if (message != null) {
            holder.bindHeader(DateUtils.getRelativeDate(context, Locale.getDefault(), message.getCreatedAt().getTime()));
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    @SuppressWarnings("WeakerAccess")
    static class MessageItemsHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.conversation_message_view)
        ConversationMessageView conversationMessageView;

        public MessageItemsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bindMessage(Context context, ParseObject parseObject) {
            conversationMessageView.bindData(context, parseObject);
        }

    }

    @SuppressWarnings("WeakerAccess")
    static class MessagedDatesHeaderHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.scroll_date_header)
        TextView scrollDateHeaderView;

        public MessagedDatesHeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bindHeader(String relativeDate) {
            scrollDateHeaderView.setText(relativeDate);
        }

    }

}
