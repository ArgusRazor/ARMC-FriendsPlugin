package com.argusrazor.friends.model;

public class PlayerSettings {

    private boolean allowRequests = true;
    private boolean allowMessages = true;
    private boolean notifyJoinQuit = true;

    public boolean isAllowRequests() {
        return allowRequests;
    }

    public void setAllowRequests(boolean allowRequests) {
        this.allowRequests = allowRequests;
    }

    public boolean isAllowMessages() {
        return allowMessages;
    }

    public void setAllowMessages(boolean allowMessages) {
        this.allowMessages = allowMessages;
    }

    public boolean isNotifyJoinQuit() {
        return notifyJoinQuit;
    }

    public void setNotifyJoinQuit(boolean notifyJoinQuit) {
        this.notifyJoinQuit = notifyJoinQuit;
    }
}
