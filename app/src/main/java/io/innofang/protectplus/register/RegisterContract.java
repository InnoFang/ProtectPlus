package io.innofang.protectplus.register;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;

import io.innofang.base.base.BasePresenter;
import io.innofang.base.base.BaseView;

/**
 * Author: Inno Fang
 * Time: 2017/9/13 16:56
 * Description:
 */


public class RegisterContract {

    interface View extends BaseView<Presenter> {
        void showInfo(String text);

        void beforeRegister();

        void registerSuccessful();

        void registerFailed(String text);
    }

    interface Presenter extends BasePresenter {
        void showEnterAnimation(CardView cardView, FloatingActionButton fab);

        void animateRevealShow(CardView cardView, FloatingActionButton fab);

        void animateRevealClose(CardView cardView, FloatingActionButton fab);

        void register(String username, String password, String repeatPassword, String client);
    }
}
