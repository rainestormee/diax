package me.diax.diax.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;
import java.util.Map;

public class GuildMusicManager {

    private static Map<String, GuildMusicManager> MANAGERS;
    private static DefaultAudioPlayerManager MANAGER;

    static  {
        MANAGERS = new HashMap<>();
        MANAGER = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(MANAGER);
    }

    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final Guild guild;

    public GuildMusicManager(Guild guild) {
        player = MANAGER.createPlayer();
        scheduler = new TrackScheduler(this);
        this.guild = guild;
        player.addListener(scheduler);
        guild.getAudioManager().setSendingHandler(this.getSendHandler());
    }

    public static GuildMusicManager getManagerFor(Guild guild) {
        return MANAGERS.computeIfAbsent(guild.getId(), (id) -> new GuildMusicManager(guild));
    }

    public AudioPlayerManager getPlayerManager() {
        return MANAGER;
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public TrackScheduler getScheduler() {
        return scheduler;
    }

    public Guild getGuild() {
        return guild;
    }
}
