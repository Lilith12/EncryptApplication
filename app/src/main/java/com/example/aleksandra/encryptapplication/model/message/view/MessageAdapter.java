package com.example.aleksandra.encryptapplication.model.message.view;

/**
 * Created by Aleksandra on 2016-11-19.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.aleksandra.encryptapplication.R;

import java.util.List;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> mMessages;
    private int[] mUsernameColors;
    Resources resources;

    public MessageAdapter(Context context, List<Message> messages) {
        mMessages = messages;
        resources = context.getResources();
        mUsernameColors = resources.getIntArray(R.array.username_colors);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Message.TYPE_MESSAGE:
                layout = R.layout.item_message;
                break;
            case Message.TYPE_LOG:
                layout = R.layout.item_log;
                break;
            case Message.TYPE_ACTION:
                layout = R.layout.item_action;
                break;
            default:
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Message message = mMessages.get(position);
        viewHolder.setImage(message.getImage());
        viewHolder.setMessage(message.getMessage());
        viewHolder.setUsername(message.getUsername());
        viewHolder.setEdited(message.isEdited());
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mUsernameView;
        private TextView mMessageView;
        private TextView mEditedView;
        private ImageView mImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mUsernameView = itemView.findViewById(R.id.username);
            mMessageView = itemView.findViewById(R.id.message);
            mEditedView = itemView.findViewById(R.id.edited);
        }

        public void setUsername(String username) {
            if (null == mUsernameView) return;
            mUsernameView.setText(username);
            mUsernameView.setTextColor(getUsernameColor(username));
        }

        public void setMessage(String message) {
            if (null == mMessageView) return;
            int fontColor = ResourcesCompat.getColor(resources, R.color.fontColor, null);
            mMessageView.setTextColor(fontColor);
            mMessageView.setText(message);
        }

        public void setImage(Bitmap bmp){
            if(null == mImageView) return;
            if(null == bmp) return;
            mImageView.setImageBitmap(bmp);
        }

        public void setEdited(Boolean wasEdited){
            if(wasEdited != null && mEditedView != null){
                mEditedView.setText(wasEdited ? "Edited" : "");
            }
        }

        private int getUsernameColor(String username) {
            int hash = 7;
            for (int i = 0, len = username.length(); i < len; i++) {
                hash = username.codePointAt(i) + (hash << 5) - hash;
            }
            int index = Math.abs(hash % mUsernameColors.length);
            return mUsernameColors[index];
        }
    }
}
