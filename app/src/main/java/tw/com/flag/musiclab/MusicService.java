package tw.com.flag.musiclab;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {

    MyMusicBinder myMusicBinder = new MyMusicBinder();
    MediaPlayer mp = new MediaPlayer();

    class MyMusicBinder extends Binder {

        public void playMusic() {
            if (!mp.isPlaying()) {
                mp.start();
            }
        }

        public void pauseMusic() {
            if (mp.isPlaying()) {
                mp.pause();
            }
        }

        public void stopMusic() {
            mp.reset();
            initMusic();
        }

        public int getTotalTime() {
            return mp.getDuration();
        }

        public int getCurrentTime() {
            return mp.getCurrentPosition();
        }

        public void setTime(int time) {
            mp.seekTo(time);
        }

        public MusicService getService() {
            return MusicService.this;
        }
    }

    public MusicService() {
        initMusic();
    }

    private void initMusic() {
        try {
            String file_path = Environment.getExternalStorageDirectory()
                    + "/music/melt.mp3";
            mp.setDataSource(file_path);
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {}
            });
            mp.prepareAsync();
            mp.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myMusicBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.stop();
            mp.release();
        }
        Log.d("MainActivity", "release the resource");
    }
}
