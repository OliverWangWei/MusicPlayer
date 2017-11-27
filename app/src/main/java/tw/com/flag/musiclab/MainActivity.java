package tw.com.flag.musiclab;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;

/*
* 【检查内容】
1. 布局显示是否正常
2. 播放，暂停，停止功能是否可用，界面显示是否正常
3. 本次实验要求使用Ibinder方式实现服务与Activity之间通信
4. 是否可以后台播放
5. 播放时是否显示当前播放时间，位置，以及图片是否旋转(可用ObjectAnimator类)
6. 是否可以拖动滑动条进行音乐进度调节
7. 加分项：通过申请动态权限读取内置sd卡中音乐文件
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MusicService.MyMusicBinder musicBinder;
    private Button play, pause, stop, quit;
    private TextView current_state, running_time, total_time;
    private SeekBar seekBar;
    //进度条下面的当前进度文字，将毫秒化为m:ss格式
    private SimpleDateFormat time = new SimpleDateFormat("m:ss");
    private ImageView img;
    private ObjectAnimator animator;

    private Thread timeRunningThread;
    
    public static final int TEXT_NOT_TEXT_RUN_THE_THREAD = -1;
    public static final int TEXT_RUN_THE_THREAD = 1;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TEXT_NOT_TEXT_RUN_THE_THREAD:
                    handler.removeCallbacks(timeRunningThread);
                    break;
                case TEXT_RUN_THE_THREAD:
                    running_time.setText(time.format(msg.arg1));
                    seekBar.setProgress(msg.arg1);
                    handler.postDelayed(timeRunningThread, 100);
                    break;
                default:
                    break;
            }
        }
    };


    private ServiceConnection connection = new ServiceConnection() {

        MusicService musicService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicService.MyMusicBinder) service;

            musicService = musicBinder.getService();
            total_time.setText(time.format(musicBinder.getTotalTime()));
            running_time.setText(time.format(0));
            seekBar.setMax(musicBinder.getTotalTime());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();

        animator = animator.ofFloat(img, "rotation", 0, 360);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);

        Intent startService = new Intent(this, MusicService.class);
        bindService(startService, connection, BIND_AUTO_CREATE);

    }

    public void findView() {
        current_state = findViewById(R.id.current_state);
        running_time = findViewById(R.id.running_time);
        total_time = findViewById(R.id.total_time);

        play = findViewById(R.id.play);
        pause = findViewById(R.id.pause);
        stop = findViewById(R.id.stop);
        quit = findViewById(R.id.quit);

        img = findViewById(R.id.img);

        seekBar = findViewById(R.id.timeSeekBar);

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        quit.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicBinder.setTime(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.play:
                play.setVisibility(View.INVISIBLE);
                pause.setVisibility(View.VISIBLE);

                musicBinder.playMusic();

                current_state.setText("Started");

                animator.start();

                timeRunningThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = handler.obtainMessage();
                        msg.what = TEXT_RUN_THE_THREAD;
                        msg.arg1 = musicBinder.getCurrentTime();
                        handler.sendMessage(msg);

                        if (msg.arg1 >= musicBinder.getTotalTime()) {
                            handler.removeCallbacks(this);
                        }
                    }
                });

                timeRunningThread.start();

                break;
            case R.id.pause:
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);

                musicBinder.pauseMusic();
                animator.cancel();

                current_state.setText("Paused");

                handler.obtainMessage(TEXT_NOT_TEXT_RUN_THE_THREAD).sendToTarget();
                break;
            case R.id.stop:
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);

                musicBinder.stopMusic();
                animator.cancel();

                current_state.setText("Stopped");

                handler.obtainMessage(TEXT_NOT_TEXT_RUN_THE_THREAD).sendToTarget();

                break;
            case R.id.quit:

                handler.obtainMessage(TEXT_NOT_TEXT_RUN_THE_THREAD).sendToTarget();
                animator.cancel();

                unbindService(connection);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
