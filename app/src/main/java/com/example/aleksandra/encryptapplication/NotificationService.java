package com.example.aleksandra.encryptapplication;

import static com.example.aleksandra.encryptapplication.handlers.DatabaseHandler.getDatabaseHandler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;

import com.example.aleksandra.encryptapplication.handlers.TableMessagesUtils;
import com.example.aleksandra.encryptapplication.model.message.websocket.MessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NotificationService extends Service {
    private Socket mSocket;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        addGlobalHandlers();
        startForeground(1, foregroundNotification());
    }

    @Override
    public void onDestroy() {
        mSocket.off("pwMessageGlobal");
        mSocket.off("groupMessageGlobal");
        EncryptAppSocket app = (EncryptAppSocket) getApplication();
        mSocket.emit("disconnect user", app.getUsername());
        mSocket.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification foregroundNotification(){
        Context ctx = NotificationService.this;
        Intent notificationIntent = new Intent(ctx, ServerStatsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(ctx);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.encrypt_app_icon)
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.foreground_notification_title))
                .setContentText(getString(R.string.foreground_notification_content));
        return builder.build();
    }

    private void addGlobalHandlers() {
        EncryptAppSocket app = (EncryptAppSocket) this.getApplication();
        mSocket = app.getSocket();
        mSocket.on("pwMessageGlobal", handlePrivateMessage);
        mSocket.on("groupMessageGlobal", handleGroupMessage);
        mSocket.on("userActive", userActive);
    }

    private Emitter.Listener handlePrivateMessage = args -> {
        Thread privateMessageThread = new Thread(() -> {
                try {
                    MessageModel model = new MessageModel((JSONObject) args[0]);
                    TableMessagesUtils.addToDatabase(model, getDatabaseHandler(getApplicationContext()));
                    createNotification(model, "WritePrivateMessageFragment", model.getUsername());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        });
        privateMessageThread.start();
    };

    private Emitter.Listener handleGroupMessage = args -> {
        Thread handleGroupMessageThread = new Thread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    MessageModel model = new MessageModel(data);
                    TableMessagesUtils.addToDatabase(model, getDatabaseHandler(getApplicationContext()));
                    createNotification(model, "GroupChatFragment", data.getString("roomName"));
                } catch (JSONException e) {
                    return;
                }
            });
        handleGroupMessageThread.start();
    };

    private Emitter.Listener userActive = args -> {
        Ack ack = (Ack) args[args.length - 1];
        ack.call();
    };

    private void createNotification(MessageModel model, String fragment, String chatView) {
        Context ctx = NotificationService.this;
        Intent notificationIntent = new Intent(ctx, ServerStatsActivity.class);
        notificationIntent.putExtra("fragment", fragment);
        notificationIntent.putExtra("chatView", chatView);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager) ctx
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(ctx);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.message)
                .setAutoCancel(true)
                .setVibrate(new long[]{250, 250, 250, 250})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentTitle(getString(R.string.message_notification, model.getUsername()))
                .setContentText(model.isImage() ? "image" : model.decryptMessage());
        Notification n = builder.build();

        nm.notify(2, n);
    }
}
