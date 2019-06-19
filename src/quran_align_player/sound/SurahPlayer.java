package quran_align_player.sound;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import quran_align_player.AyahSoundTicker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SurahPlayer {
    private final int surahNumber;
    private final int ayahCount;
    private final String surahFilePath;
    private final AyahSoundTicker tickCallback;
    private final Semaphore ayahTrackLock = new Semaphore(1);
    @SuppressWarnings("FieldCanBeLocal")
    private MediaPlayer player;

    public SurahPlayer(int surahNumber, int ayahCount, String surahFilePath, AyahSoundTicker tickCallback) {
        this.surahNumber = surahNumber;
        this.ayahCount = ayahCount;
        this.surahFilePath = surahFilePath;
        this.tickCallback = tickCallback;
    }

    public void play() {
        List<Integer> ayahs = IntStream.range(1, ayahCount).boxed().collect(Collectors.toList());
        playAyahs(ayahs);
    }

    public void playAyahs(List<Integer> ayahs) {
        try {
            ayahTrackLock.acquire();
            for (Integer i : ayahs) {
                playAyah(i);
                ayahTrackLock.acquire();
            }
            ayahTrackLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void playAyah(int ayahNumber) {
        String surahFileName = String.format("%03d%03d.mp3", surahNumber, ayahNumber);
        Path fullFileName = Paths.get(surahFilePath, surahFileName);
        System.err.println("[Playing] " + surahFileName);

        player = new MediaPlayer(new Media(fullFileName.toUri().toString()));
        player.setOnEndOfMedia(ayahTrackLock::release);
        player.currentTimeProperty().addListener((o, oldVal, newVal) -> {
            Duration d = Duration.ofMillis((int) newVal.toMillis());
            this.tickCallback.accept(ayahNumber, d);
        });
        player.play();
    }
}
