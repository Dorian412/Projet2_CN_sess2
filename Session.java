import java.util.*;

public class Session{

    private String player;
    private List<char[]> game;

    public Session(){
        this.player = "Invité";
        this.game = null;
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
}