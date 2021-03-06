package io.enuma.app.keystoretest;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import io.enuma.app.keystoretest.R;

import static io.enuma.app.keystoretest.Constants.MESSAGE_INREPLYTO;
import static io.enuma.app.keystoretest.Constants.MESSAGE_RECIPIENT_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_EMAIL;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SENDER_NAME;
import static io.enuma.app.keystoretest.Constants.MESSAGE_SUBJECT;
import static io.enuma.app.keystoretest.Constants.MESSAGE_TEXT;

public class CardListAdapter extends RecyclerView.Adapter<CardListAdapter.CardListViewHolder> {

    private List<ChatMessage> list;

    public CardListAdapter(List<ChatMessage> list) {
        this.list = list;
    }

    @Override
    public CardListViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(viewType, viewGroup,false);
        return new CardListViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage s = list.get(position);
        if (s.messageId == null) {
            return R.layout.card_datetime;
        }
        return (s.senderName != null) ? R.layout.card_layout : R.layout.card_me_layout;
    }

    @Override
    public void onBindViewHolder(CardListViewHolder cardListViewHolder, int i) {
        final ChatMessage s = list.get(i);
        cardListViewHolder.title.setText(s.message);
        if (s.status == ChatMessage.Status.Failed)
            cardListViewHolder.button.setVisibility(View.VISIBLE);
        else if (s.status == ChatMessage.Status.Delivered)
            cardListViewHolder.button.setVisibility(View.INVISIBLE);
//        cardListViewHolder.button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int addMessage(ChatMessage chatMsg){
        chatMsg.position = this.list.size();
        this.list.add(chatMsg);
        notifyDataSetChanged();
        return chatMsg.position;
    }

    public void updateMessageStatus(ChatMessage chatMessage) {
        notifyItemChanged(chatMessage.position);
    }

    public static class CardListViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        Button button;
        public CardListViewHolder(View itemView) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.msg_text);
            button = (Button)itemView.findViewById(R.id.msg_status);
        }
    }

}