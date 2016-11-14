import React, { Component } from 'react';
import VideoPlayer from './native/VideoPlayerAndroid';

export default class App extends Component {

    render() {
        return <VideoPlayer
            style={{ flex: 2, flexDirection: 'column',backgroundColor: '#292929' }}
            ref='videoPlayer'
            resizeMode='contain' //显示模式['cover', 'contain', 'stretch']
            uri='http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4' //指定视频uri
            autoPlay={false} //开启自动播放 />
    }
}