package io.openvidu.openvidu_android.openvidu;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import io.openvidu.openvidu_android.activities.SessionActivity;
import io.openvidu.openvidu_android.observers.CustomPeerConnectionObserver;
import io.openvidu.openvidu_android.observers.CustomSdpObserver;
import io.openvidu.openvidu_android.websocket.CustomWebSocket;

import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 会话由SessionActivity负责创建与销毁
 * 当获取到Tocken之后就实例化
 */
public class Session {

    /**
     * 本端参与者
     */
    private LocalParticipant localParticipant;

    /**
     * 远端参与者
     */
    private Map<String, RemoteParticipant> remoteParticipants = new HashMap<>();

    //Session标识
    private String id;

    //Token
    private String token;

    //远端容器
    private LinearLayout views_container;

    //端链接工厂
    private PeerConnectionFactory peerConnectionFactory;

    //websocket
    private CustomWebSocket websocket;

    //使用Session的活动
    private SessionActivity activity;

    /**
     * SessionActivity的getTokenSuccess方法调用
     * @param id
     * @param token
     * @param views_container
     * @param activity
     */
    public Session(String id, String token, LinearLayout views_container, SessionActivity activity) {
        this.id = id;
        this.token = token;
        this.views_container = views_container;
        this.activity = activity;

        PeerConnectionFactory.InitializationOptions.Builder optionsBuilder =
                PeerConnectionFactory.InitializationOptions.builder(activity.getApplicationContext());
        optionsBuilder.setEnableInternalTracer(true);
        PeerConnectionFactory.InitializationOptions opt = optionsBuilder.createInitializationOptions();
        PeerConnectionFactory.initialize(opt);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;
        encoderFactory = new SoftwareVideoEncoderFactory();
        decoderFactory = new SoftwareVideoDecoderFactory();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    /**
     * SessionActivity的startWebSocket
     * @param websocket
     */
    public void setWebSocket(CustomWebSocket websocket) {
        this.websocket = websocket;
    }

    /**
     * 创建本端链接，由CustomWebSocket的handleServerResponse调用
     * @return
     */
    public PeerConnection createLocalPeerConnection() {
        //RTC配置
        PeerConnection.RTCConfiguration rtcConfig = configRtcConfig();
        //端链接
        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig,
                new CustomPeerConnectionObserver("local") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                //把交互候选者发送到服务器
                websocket.onIceCandidate(iceCandidate, localParticipant.getConnectionId());
            }
        });
        return peerConnection;
    }

    /**
     * 由CustomWebSocket的newRemoteParticipantAux调用
     * @param connectionId
     */
    public void createRemotePeerConnection(final String connectionId) {
        PeerConnection.RTCConfiguration rtcConfig =configRtcConfig();
        //对端链接peerConnection
        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig,
                new CustomPeerConnectionObserver("remotePeerCreation") {
                    /**
                     * 候选者在PeerConnection处理之后的回调
                     * @param iceCandidate
                     */
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        super.onIceCandidate(iceCandidate);
                        //远端候选者发送到服务端
                        websocket.onIceCandidate(iceCandidate, connectionId);
                    }

                    /**
                     *
                     * @param rtpReceiver Rtp接收器
                     * @param mediaStreams 媒体流
                     */
                    @Override
                    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                        super.onAddTrack(rtpReceiver, mediaStreams);
                        //端链接建立之后的回调，activity设置流实现界面显示
                        //同一个连接获取第一个媒体流
                        //setRemoteMediaStream(MediaStream stream,
                        //              final RemoteParticipant remoteParticipant)
                        activity.setRemoteMediaStream(mediaStreams[0],
                                remoteParticipants.get(connectionId));
                    }

                    /**
                     * 信令处理状态变化,当信令状态到达稳定状态时，从远端参与者获取候选者，
                     * 把每个参与者加入到远端参与者的端链接
                     * @param signalingState
                     */
                    @Override
                    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                        if (PeerConnection.SignalingState.STABLE.equals(signalingState)) {
                            final RemoteParticipant remoteParticipant = remoteParticipants.get(connectionId);
                            Iterator<IceCandidate> it = remoteParticipant.getIceCandidateList().iterator();
                            while (it.hasNext()) {
                                IceCandidate candidate = it.next();
                                remoteParticipant.getPeerConnection().addIceCandidate(candidate);
                                it.remove();
                            }
                        }
                    }
        });
        //对端链接peerConnection定义匿名定义结束
        //Add audio track to create transReceiver 本端音频加入到传输接收器
        peerConnection.addTrack(localParticipant.getAudioTrack());
        //Add video track to create transReceiver 本端视频加入到传输接收器
        peerConnection.addTrack(localParticipant.getVideoTrack());
        //对于每个Rtp传输器设置媒体的方法为仅仅接收
        for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
            //We set both audio and video in receive only mode
            transceiver.setDirection(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);
        }

        //连接的每个远端设置
        this.remoteParticipants.get(connectionId).setPeerConnection(peerConnection);
    }

    /**
     * CustomWebSocket的handleServerResponse调用调用，产生SDP的提交动作
     * @param constraints
     */
    public void createLocalOffer(MediaConstraints constraints) {
        //本地参与者，端链接创建 在端链接上发起SDP 提交交互请求
        localParticipant.getPeerConnection().createOffer(new CustomSdpObserver("local offer sdp") {
            /**
             * 本地参与者创建offer成功
             * @param sessionDescription
             */
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                Log.i("createOffer SUCCESS", sessionDescription.toString());
                //本端参与者的连接设置本端的描述，WebRTC创建本地会话
                //在SDP向WebRTC发起成功之后，回调设置端链接的本地描述
                localParticipant.getPeerConnection().setLocalDescription(
                        new CustomSdpObserver("local set local"),
                        sessionDescription);
                //websocket发布本地创建SDP的offer，发布视频
                websocket.publishVideo(sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e("createOffer ERROR", s);
            }

        }, constraints);
    }

    public String getId() {
        return this.id;
    }

    public String getToken() {
        return this.token;
    }

    public LocalParticipant getLocalParticipant() {
        return this.localParticipant;
    }

    public void setLocalParticipant(LocalParticipant localParticipant) {
        this.localParticipant = localParticipant;
    }

    public RemoteParticipant getRemoteParticipant(String id) {
        return this.remoteParticipants.get(id);
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return this.peerConnectionFactory;
    }

    public void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        this.remoteParticipants.put(remoteParticipant.getConnectionId(), remoteParticipant);
    }

    public RemoteParticipant removeRemoteParticipant(String id) {
        return this.remoteParticipants.remove(id);
    }

    public void leaveSession() {
        //离开
        websocket.setWebsocketCancelled(true);
        if (websocket != null) {
            websocket.leaveRoom();
            websocket.disconnect();
        }
        this.localParticipant.dispose();
        for (RemoteParticipant remoteParticipant : remoteParticipants.values()) {
            if (remoteParticipant.getPeerConnection() != null) {
                remoteParticipant.getPeerConnection().close();
            }
            //
            views_container.removeView(remoteParticipant.getView());
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }

    public void removeView(View view) {
        this.views_container.removeView(view);
    }

    private PeerConnection.RTCConfiguration configRtcConfig(){
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        //
        PeerConnection.IceServer iceServer =
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        return rtcConfig;
    }

}
