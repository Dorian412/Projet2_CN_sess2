import java.util.*;

public class GameManager{
    private List<char[]> game;//[0] = contenu (B ou chiffre) et [1] = flagged revealed ou unrevealed (F, R ou U) 

    public GameManager(List<char[]> game){
        this.game = game;
    }

    public List<char[]> getGame(){
        return game;
    }

    //cree une nouvelle grille de demineur avec le premier click safe
    public List<char[]> createNew(int firstX, int firstY){
        int firstTry = firstX * 7 + firstY;
        List<char[]> grid = new ArrayList<>();

        for(int k = 0; k < 49; k++){
            grid.add(new char[]{'0', 'U'});
        }

        char[] cell = new char[2];
        Random random = new Random();
        int i = 0;
        int randomNumber;
        while(i < 7){//set les bombs random
            randomNumber = random.nextInt(49);
            if(grid.get(randomNumber)[0] != 'B' && randomNumber != firstTry){//premier essai tjr gagnant
                grid.set(randomNumber, new char[]{'B', 'U'});
                i++;
            }
        }
    
        int numberOfBombs;
        for(int j = 0; j < 49; j++){
            numberOfBombs = 0;
            cell = grid.get(j);
            if(cell[0] != 'B'){
               //check au dessus
                if(j > 6 && grid.get(j - 7)[0] == 'B')
                    numberOfBombs++;
                //check au dessus a droite
                if((j%7) + 1 != 7 && j > 6 && grid.get(j - 6)[0] =='B')
                    numberOfBombs++;
                //check au dessus a gauche
                if((j%7) != 0 && j > 7 && grid.get(j - 8)[0] == 'B')
                    numberOfBombs++;
                //check en bas
                if(j < 42 && grid.get(j + 7)[0] == 'B')
                    numberOfBombs++;
                //check en bas a droite
                if((j%7) + 1 != 7 && j < 42 && grid.get(j + 8)[0] == 'B')
                    numberOfBombs++;
                //check en bas a gauche
                if((j%7) != 0 && j < 42 && grid.get(j + 6)[0] == 'B')
                    numberOfBombs++;
                //check a gauche
                if((j%7) != 0 && grid.get(j - 1)[0] == 'B')
                    numberOfBombs++;
                //check a droite
                if((j%7) + 1 != 7 && grid.get(j + 1)[0] == 'B')
                    numberOfBombs++;

                // + '0' pour avoir numberofbombs correct en ASCII
                grid.set(j, new char[]{(char)(numberOfBombs + '0'), 'U'});
            }
        }
        return grid;
    }

    //si return 0, = tombé sur une bombe donc perdu. return 1, = pas bombe donc OK on continue
    public int Try(int x, int y){
        int choice = (x * 7) + y;

        if(game == null){
            game = createNew(x, y);
        } 

        if(game.get(choice)[0] == 'B'){
            for(int i = 0; i < 49; i++){
                game.get(i)[1] = 'R';
            }
            return 0;
        }
        else{
            if(game.get(choice)[0] == '0'){
                revealZeroNeighbor(x, y);
                return 1;
            }
            else{
                game.get(choice)[1] = 'R';
            }
        }
        return 1;
    }

    //cette fonction va flag/unflag la cellule x,y
    public void Flag(int x, int y){
        int choice = (x * 7) + y;

        if(game.get(choice)[1] == 'R'){
            return;
        }
        
        if(game.get(choice)[1] == 'U'){//si case cache on flag
            game.get(choice)[1] = 'F';
            return;
        }

        if(game.get(choice)[1] =='F'){//si case flag on la cache
            game.get(choice)[1] = 'U';
            return;
        }
    }

    public int getGameState(){
        //si loose return -1
        //si win return 1
        //si en cours return 0
        int unrevealedCell = 0;
        for(int i = 0; i < 49; i++){
            if(game.get(i)[0] == 'B' && game.get(i)[1] =='R')
                return -1;
            if(game.get(i)[1] == 'U' || game.get(i)[1] =='F')
                unrevealedCell++;
        }

        if(unrevealedCell == 7){
            return 1;
        }
        else return 0;
    }

    //cette fonction est utilisée pour reveller les voisins d une case = a '0'
    public void revealZeroNeighbor(int x, int y){
        int cell = (x * 7) + y;

        if(game.get(cell)[1] == 'R'){//si deja revealed on return
            return;
        }

        game.get(cell)[1] = 'R';

        if(game.get(cell)[0] != '0'){
            return;
        }

        //au dessus
        if(x > 0)
        revealZeroNeighbor(x - 1, y);
        //a droite
        if(y < 6)
            revealZeroNeighbor(x, y + 1);
        //a gauche
        if(y > 0)
            revealZeroNeighbor(x, y - 1);
        //en bas
        if(x < 6)
            revealZeroNeighbor(x + 1, y);        
    }

    //revelle entierement la grille
    public void cheat(){
        for(int i = 0; i < game.size() && game!= null; i++){
            game.get(i)[1] = 'R';
        }
        return;
    }
}