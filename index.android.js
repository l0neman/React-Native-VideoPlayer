/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import { View, AppRegistry } from 'react-native';
import App from './app';

export default class ReactNativeVideoPlayer extends Component {
  render() {
    return (
      <View
        style={{ flex: 7, flexDirection: 'column' }}  >
        <App/>
        <View style={{ flex: 5 }} />
      </View>);
  }
}

AppRegistry.registerComponent('ReactNativeVideoPlayer', () => ReactNativeVideoPlayer);
