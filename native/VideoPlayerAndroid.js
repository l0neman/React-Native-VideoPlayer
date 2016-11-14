import React, { PropTypes, Component } from 'react';
import {
    UIManager,
    requireNativeComponent,
    View,
    findNodeHandle
} from 'react-native';

const VideoPlayerAndroid = requireNativeComponent('VideoPlayer', VideoPlayer);

export default class VideoPlayer extends Component {

    videoPlayer;

    constructor(props) {
        super(props);
    }

    static propTypes = {
        uri: PropTypes.string,
        resizeMode: PropTypes.oneOf(['cover', 'contain', 'stretch']),
        autoPlay: PropTypes.bool,
        ...View.propTypes
    }

    recycle() {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.VideoPlayer.Commands.recycle,
            [],
        );
    }

    stop(){
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.VideoPlayer.Commands.stop,
            []
        );
    }

    render() {
        return <VideoPlayerAndroid {...this.props } />;
    }
}