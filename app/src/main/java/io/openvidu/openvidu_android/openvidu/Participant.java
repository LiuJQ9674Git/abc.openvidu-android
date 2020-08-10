package io.openvidu.openvidu_android.openvidu;

import android.util.Log;

import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public abstract class Participant {

    //连接ID
    protected String connectionId;
    protected String participantName;
    protected Session session;
    protected List<IceCandidate> iceCandidateList = new ArrayList<>();
    protected PeerConnection peerConnection;
    protected AudioTrack audioTrack;
    protected VideoTrack videoTrack;
    protected MediaStream mediaStream;

    /**
     * 本地参与者
     * @param participantName
     * @param session
     */
    public Participant(String participantName, Session session) {
        this.participantName = participantName;
        this.session = session;
    }

    /**
     * 远程参与者实例化
     * @param connectionId
     * @param participantName
     * @param session
     */
    public Participant(String connectionId, String participantName, Session session) {
        this.connectionId = connectionId;
        this.participantName = participantName;
        this.session = session;
    }

    public String getConnectionId() {
        return this.connectionId;
    }

    /**
     * CustomWebSocket的handleServerResponse
     * @param connectionId
     */
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getParticipantName() {
        return this.participantName;
    }

    public List<IceCandidate> getIceCandidateList() {
        return this.iceCandidateList;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    /**
     * CustomWebSocket的handleServerResponse
     * Session的createRemotePeerConnection
     * @param peerConnection
     */
    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public AudioTrack getAudioTrack() {
        return this.audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public VideoTrack getVideoTrack() {
        return this.videoTrack;
    }

    public void setVideoTrack(VideoTrack videoTrack) {
        this.videoTrack = videoTrack;
    }

    public MediaStream getMediaStream() {
        return this.mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    public void dispose() {
        if (this.peerConnection != null) {
            try {
                this.peerConnection.close();
            } catch (IllegalStateException e) {
                Log.e("Dispose PeerConnection", e.getMessage());
            }
        }
    }
}
