package io.openvidu.openvidu_android.observers;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * SDP观察者
 *
 * Session的createLocalOffer实例化本端的SDP
 * CustomerWebSocket的subscribeAux创建远端的SDP
 * 端连接方法
 * createOffer(SdpObserver observer, MediaConstraints constraints)
 * createAnswer(SdpObserver observer, MediaConstraints constraints)
 * setLocalDescription(SdpObserver observer, SessionDescription sdp)
 * setRemoteDescription(SdpObserver observer, SessionDescription sdp)
 *
 * localParticipant.getPeerConnection().createOffer(new CustomSdpObserver("local offer sdp")
 * remoteParticipant.getPeerConnection().createOffer(new CustomSdpObserver("remote offer sdp")
 */
public class CustomSdpObserver implements SdpObserver {

    private String tag;

    public CustomSdpObserver(String tag) {
        this.tag = "SdpObserver-" + tag;
    }

    private void log(String s) {
        Log.d(tag, s);
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        log("onCreateSuccess " + sessionDescription);
    }

    @Override
    public void onSetSuccess() {
        log("onSetSuccess ");
    }

    @Override
    public void onCreateFailure(String s) {
        log("onCreateFailure " + s);
    }

    @Override
    public void onSetFailure(String s) {
        log("onSetFailure " + s);
    }
}
