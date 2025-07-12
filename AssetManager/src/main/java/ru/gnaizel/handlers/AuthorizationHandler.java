package ru.gnaizel.handlers;

package com.telegramfiledownloader.handlers;

import ca.denisab85.tdlight.Client;
import ca.denisab85.tdlight.jni.TdApi;
import ca.denisab85.tdlight.Client.ResultHandler;

public class AuthorizationHandler implements ResultHandler {
    private final Client client;
    private boolean authComplete = false;

    public AuthorizationHandler(Client client) {
        this.client = client;
    }

    public boolean isAuthComplete() {
        return authComplete;
    }

    @Override
    public void onResult(TdApi.Object object) {
        if (object.getConstructor() != TdApi.UpdateAuthorizationState.CONSTRUCTOR) return;

        TdApi.AuthorizationState state = ((TdApi.UpdateAuthorizationState) object).authorizationState;

        switch (state.getConstructor()) {
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR:
                handlePhoneNumber();
                break;
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR:
                handleAuthCode();
                break;
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                authComplete = true;
                System.out.println("✅ Authorization complete!");
                break;
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR:
                System.err.println("⚠️ Two-step verification is not supported");
                System.exit(1);
            default:
                System.out.println("Unhandled state: " + state);
        }
    }

    private void handlePhoneNumber() {
        String phone = System.getenv("TELEGRAM_PHONE");
        if (phone == null || phone.isEmpty()) {
            System.err.println("TELEGRAM_PHONE environment variable not set");
            System.exit(1);
        }
        client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), null);
    }

    private void handleAuthCode() {
        String code = System.getenv("TELEGRAM_CODE");
        if (code == null || code.isEmpty()) {
            System.err.println("TELEGRAM_CODE environment variable not set");
            System.exit(1);
        }
        client.send(new TdApi.CheckAuthenticationCode(code), null);
    }
}
