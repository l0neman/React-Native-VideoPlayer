package com.reactnativevideoplayer.rctmodule;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativevideoplayer.view.VideoPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by runing on 2016/11/13.
 */

public final class VideoPlayerModule {

    public static final class RCTVideoPlaerPackage implements ReactPackage {

        @Override
        public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends JavaScriptModule>> createJSModules() {
            return Collections.emptyList();
        }

        @Override
        public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
            return Collections.<ViewManager>singletonList(new RCTVideoPlayerManager());
        }
    }

    public static final class RCTVideoPlayerManager extends SimpleViewManager<VideoPlayer> {

        private static final String PROPS_URI = "uri";
        private static final String PROPS_RESIZE_MODE = "resizeMode";
        private static final String PROPS_ENABLE_AUTO = "autoPlay";

        private static final int COMMAND_RECYCLE = 0;
        private static final int COMMAND_STOP = 1;

        @Override
        public String getName() {
            return "VideoPlayer";
        }

        @Override
        protected VideoPlayer createViewInstance(ThemedReactContext reactContext) {
            return new VideoPlayer(reactContext);
        }

        @ReactProp(name = PROPS_URI)
        public void setUrl(VideoPlayer player, String uri) {
            player.setUrl(uri);
        }

        @ReactProp(name = PROPS_RESIZE_MODE)
        public void setResizeMode(VideoPlayer player, String resizeMode) {
            if (resizeMode == null) {
                return;
            }
            if (resizeMode.equals("contain")) {
                player.setResizeMode(VideoPlayer.RESIZE_MODE_CONTAIN);
            } else if (resizeMode.equals("stretch")) {
                player.setResizeMode(VideoPlayer.RESIZE_MODE_STRETCH);
            } else if (resizeMode.equals("cover")) {
                player.setResizeMode(VideoPlayer.RESIZE_MODE_COVER);
            }
        }

        @ReactProp(name = PROPS_ENABLE_AUTO)
        public void setAutoPlay(VideoPlayer player, boolean isEnable) {
            if (isEnable) {
                player.enableAuto();
            }
        }

        @Nullable
        @Override
        public Map<String, Integer> getCommandsMap() {
            return MapBuilder.of("recycle", COMMAND_RECYCLE,
                    "stop", COMMAND_STOP);
        }

        @Override
        public void receiveCommand(VideoPlayer player, int commandId,
                                   @Nullable ReadableArray args) {
            switch (commandId) {
                case COMMAND_RECYCLE:
                    player.recycle();
                    break;
                case COMMAND_STOP:
                    player.stop();
                    break;
                default:
                    throw new IllegalArgumentException(String.format(Locale.US,
                            "Unsupported command %d received by %s.",
                            commandId, getClass().getSimpleName()));
            }
        }
    }
}
