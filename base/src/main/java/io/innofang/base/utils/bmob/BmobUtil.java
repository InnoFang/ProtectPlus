package io.innofang.base.utils.bmob;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import cn.bmob.newim.BmobIM;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.bean.BmobIMMessage;
import cn.bmob.newim.bean.BmobIMUserInfo;
import cn.bmob.newim.core.ConnectionStatus;
import cn.bmob.newim.event.MessageEvent;
import cn.bmob.newim.listener.ConnectListener;
import cn.bmob.newim.listener.ConnectStatusChangeListener;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import io.innofang.base.bean.User;
import io.innofang.base.utils.common.L;

/**
 * Author: Inno Fang
 * Time: 2017/9/12 21:21
 * Description:
 */


public class BmobUtil {

    public static void login(String username, String password, final BmobEvent.onLoginListener listener) {
        if (listener.beforeLogin()) {
            BmobUser.loginByAccount(username, password, new LogInListener<User>() {
                @Override
                public void done(User user, BmobException e) {
                    if (null == e) {
                        listener.loginSuccessful(user);
                    } else {
                        listener.loginFailed(e);
                    }
                }
            });
        }
    }

    public static void register(User user, final BmobEvent.onRegisterListener listener) {
        if (listener.beforeRegister()) {
            user.signUp(new SaveListener<User>() {
                @Override
                public void done(User user, BmobException e) {
                    if (null == e) {
                        listener.registerSuccessful(user);
                    } else {
                        listener.registerFailed(e);
                    }
                }
            });
        }
    }

    public static void update(User user, final BmobEvent.onUpdateListener listener) {
        if (listener.beforeUpdate()) {
            user.update(new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (null == e) {
                        listener.updateSuccessful();
                    } else {
                        listener.updateFailed(e);
                    }
                }
            });
        }
    }

    public static void query(String username, final BmobEvent.onQueryListener listener) {
        if (listener.beforeQuery()) {
            BmobQuery<User> query = new BmobQuery<>();
            //去掉当前用户
            try {
                BmobUser user = BmobUser.getCurrentUser();
                query.addWhereNotEqualTo("username", user.getUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            query.addWhereEqualTo("username", username);
            query.setLimit(1);
            query.order("-createdAt");
            query.findObjects(new FindListener<User>() {
                @Override
                public void done(List<User> list, BmobException e) {
                    if (null == e) {
                        if (list != null && list.size() > 0) {
                            listener.querySuccessful(list);
                        } else {
                            listener.queryFailed(new BmobException("查无此人"));
                        }
                    } else {
                        listener.queryFailed(e);
                    }
                }
            });
        }
    }

    public static void connect(final User user, final BmobEvent.onConnectListener listener) {
        User curr = BmobUser.getCurrentUser(User.class);
        if (!TextUtils.isEmpty(curr.getObjectId())) {
            BmobIM.connect(user.getObjectId(), new ConnectListener() {
                @Override
                public void done(String s, BmobException e) {
                    if (null == e) {
                        if (BmobIM.getInstance().getCurrentStatus().getCode() != ConnectionStatus.CONNECTED.getCode()) {
                            listener.connectFailed("haven't connect server");
                            return;
                        }
                        listener.connectSuccessful(user);
                    } else {
                        listener.connectFailed(e.getMessage());
                    }
                }
            });
        }
    }

    public static void checkConnect(final Context context) {

        BmobUtil.connect(BmobUser.getCurrentUser(User.class), new BmobEvent.onConnectListener() {
            @Override
            public void connectSuccessful(User user) {
                //服务器连接成功就发送一个更新事件，同步更新会话及主页的小红点
                EventBus.getDefault().post(new BmobIMMessage());
                //会话： 更新用户资料，用于在会话页面、聊天页面以及个人信息页面显示
                BmobIM.getInstance().
                        updateUserInfo(new BmobIMUserInfo(user.getObjectId(),
                                user.getUsername(), null));
            }

            @Override
            public void connectFailed(String error) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        });
        // 连接： 监听连接状态，可通过BmobIM.getInstance().getCurrentStatus()来获取当前的长连接状态
        BmobIM.getInstance().setOnConnectStatusChangeListener(new ConnectStatusChangeListener() {
            @Override
            public void onChange(ConnectionStatus status) {
                Toast.makeText(context, status.getMsg(), Toast.LENGTH_SHORT).show();
                L.i(BmobIM.getInstance().getCurrentStatus().getMsg());
            }
        });

    }

    /**
     * 更新用户资料和会话资料
     *
     * @param event
     * @param listener
     */
    public static void updateUserInfo(MessageEvent event, final BmobEvent.UpdateCacheListener listener) {
        final BmobIMConversation conversation = event.getConversation();
        final BmobIMUserInfo info = event.getFromUserInfo();
        final BmobIMMessage msg = event.getMessage();
        String username = info.getName();
        String title = conversation.getConversationTitle();
        //SDK内部将新会话的会话标题用objectId表示，因此需要比对用户名和私聊会话标题，后续会根据会话类型进行判断
        if (!username.equals(title)) {
            queryUserInfo(info.getUserId(), new BmobEvent.QueryUserListener() {
                @Override
                public void done(User s, BmobException e) {
                    if (e == null) {
                        String name = s.getUsername();
                        conversation.setConversationTitle(name);
                        info.setName(name);
                        // 更新用户资料，用于在会话页面、聊天页面以及个人信息页面显示
                        BmobIM.getInstance().updateUserInfo(info);
                        // 更新会话资料-如果消息是暂态消息，则不更新会话资料
                        if (!msg.isTransient()) {
                            BmobIM.getInstance().updateConversation(conversation);
                        }
                    } else {
                        L.e(e.getMessage());
                    }
                    listener.done(null);
                }
            });
        } else {
            listener.done(null);
        }
    }

    /**
     * 查询指定用户信息
     *
     * @param objectId
     * @param listener
     */
    public static void queryUserInfo(String objectId, final BmobEvent.QueryUserListener listener) {
        BmobQuery<User> query = new BmobQuery<>();
        query.addWhereEqualTo("objectId", objectId);
        query.findObjects(
                new FindListener<User>() {
                    @Override
                    public void done(List<User> list, BmobException e) {
                        if (e == null) {

                            if (list != null && list.size() > 0) {
                                listener.done(list.get(0), null);
                            } else {
                                listener.done(null, new BmobException(000, "查无此人"));
                            }
                        } else {
                            listener.done(null, e);
                        }
                    }
                });
    }

}
