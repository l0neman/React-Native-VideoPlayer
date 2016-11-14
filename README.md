# React-Native-VideoPlayer

(Android平台)一个小小视频播放器，封装了系统的MediaPlayer类，支持在原生平台和React-Native中使用，提供三种显示模式

- `resizeMode='contain'` (原始视频比例，控件边缘可能会出现空白)

 ![contain_mode0](https://github.com/wangruning/MyFlowLayout/blob/master/image/contain_mode0.png)

 ![contain_mode1](https://github.com/wangruning/MyFlowLayout/blob/master/image/contain_mode1.png)

- `resizeMode='cover'` (剪切画面，控件不会产生空白，但视频画面会被裁剪)

 ![cover_mode](https://github.com/wangruning/MyFlowLayout/blob/master/image/cover_mode.png)

- `resizeMode='stretch'` (视频填充屏幕，画面将可能出现拉伸)

 ![stretch_mode](https://github.com/wangruning/MyFlowLayout/blob/master/image/stretch_mode.png)

## 导入

1. 原生平台直接把app项目下的VideoPlayer.java和资源文件放入项目即可

2. React-Native平台，需要把app中代码导入，然后注册RCTVideoPlaerPackage到Application

   ```java
   @Override
   protected List<ReactPackage> getPackages() {
   	return Arrays.<ReactPackage>asList(
       	new MainReactPackage(),
           	new VideoPlayerModule.RCTVideoPlaerPackage()
           );
      }
   ```

   需要把 `native/VideoPlayerAndroid.js` 导入后即可编译运行

## 使用

1. 原生平台直接在布局中引入

   ```xml
   <com.reactnativevideoplayer.view.VideoPlayer
   	android:id="@+id/vPlayer"
       android:layout_width="match_parent"
       android:layout_height="300dp" />
   ```

   然后在Activity中使用

   ```java
   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

     	VideoPlayer mVideoPlayer = (VideoPlayer) findViewById(R.id.vPlayer);

     	mVideoPlayer.setUrl("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
     	mVideoPlayer.enableAuto();
     	mVideoPlayer.setResizeMode(VideoPlayer.RESIZE_MODE_CONTAIN);
   }
   ```

   ​

2. react-native中，在引入VideoPlayer.js，并在Component中使用

```js
//...
import VideoPlayer from './native/VideoPlayerAndroid';

export default class App extends Component {

    render() {
        return <VideoPlayer
            style={{ flex: 2, flexDirection: 'column',backgroundColor: '#292929' }}
            ref='videoPlayer'
            resizeMode='contain'
            uri='http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4'
            autoPlay={false} />
    }

	_enterNextPage() {
        videoPlayer.stop(); //停止videoPlayer播放
        //...
	}
}
```

