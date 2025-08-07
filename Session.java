import java.util.*;

public class Session{

    private String player;
    private List<char[]> game;
    private long startTime;
    private long duration;
    private boolean ended;

    public Session(){
        this.player = "Invité";
        this.game = null;
        this.startTime = System.currentTimeMillis();
        this.duration = 0;
        this.ended = false;
    }
    
    public void setPlayer(String player){
        this.player = player;
    }
    
    public void setGame(List<char[]> game){
        this.game = game;
    }
    
    public String getPlayer(){
        return player;
    }
    
    public List<char[]> getGame(){
        return game;
    }

    public void endGame(){
        duration = System.currentTimeMillis() - startTime;
        ended = true;
    }

    public long getDuration(){
        return this.duration;
    }
}