import java.io.*;
import java.util.List;

public class playHTML{
    /*cette fonction prends en arguments une session et deux String et va generer une page html sous forme de string 
     * qu elle va ensuite renvoyer. cette page differe en fonction des arguments recus
     */
    public String generatePlayHTML(Session session, String bomb, String flag, boolean errorFlag){
        StringBuilder html = new StringBuilder();
        String player ="Invité";
        String game;
        String usableGameString = null;
        if(session != null){
            player = session.getPlayer();
        
            if(session.getGame() != null){
                game = transformListToString(session.getGame());
                //il faut modifier la string de notre game sinon ca ira pas lorsqu on va lire le fichier .js
                usableGameString = game.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
            }
        }

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"fr\">\n");
        html.append("<head>\n");
        html.append("   <meta charset=\"UTF-8\">\n");
        html.append("   <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("   <title>Minesweeper</title>\n");
        html.append("   <style>\n");
        html.append("       body { font-family: Arial, sans-serif; text-align: center; }\n");
        html.append("       .grid { display: grid; grid-template-columns: repeat(7, 30px); margin-top: 20px; grid-gap: 2px; justify-content: center; }\n");//modifier le 7 pour modifier le nombre de collones
        html.append("       .cell { width: 30px; height: 30px; border: 1px solid #aaa; background-color: lightgray; cursor: pointer; }\n");
        html.append("       .revealed { background-color: white; }\n");
        html.append("       .flag { background-image: url('data:image/png;base64,").append(flag).append("'); background-size: cover; }\n");
        html.append("       .bomb { background-image: url('data:image/png;base64,").append(bomb).append("'); background-size: cover; }\n");
        html.append("   </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("   <h1>Minesweeper</h1>\n");
        html.append("   <p id=\"current-player\">Player : ").append(player).append("</p>");
        html.append("   <form method=\"POST\" action=\"/set_username\">\n");
        html.append("       <input type=\"text\" name=\"player_name\" placeholder=\"Entrez votre nom\" required>\n");
        html.append("       <button type=\"submit\">Mettre à jour</button>\n");
        html.append("   </form>\n");
        html.append("   <div class=\"grid\" id=\"minesweeper-grid\"></div>\n");
        html.append("   <script>\n");

        html.append("       let game = \"").append(usableGameString).append("\";\n");
        
        html.append(convertFileToString("PlayJS.js"));

        html.append("   </script>\n");
        html.append("   <noscript>\n");

        if(session != null){
            html.append(generateNoScriptGrid(transformListToString(session.getGame()), flag, bomb));
        }
        else{
            html.append(generateNoScriptGrid(transformListToString(null), flag, bomb));
        }

        html.append("       <form action=\"request\" method=\"POST\">");
        html.append("           <label for=\"action\">Action :</label>");
        html.append("           <select name=\"action\" id=\"action\">");
        html.append("               <option value=\"TRY\"> try </option>");
        html.append("               <option value=\"FLAG\"> flag </option>");
        html.append("           </select>");
        html.append("           <br><br>");
        html.append("           <label for=\"row\"> Row : </label>");
        html.append("           <select name=\"row\" id =\"row\">");
        html.append("               <option value=\"0\"> 0 </option>");
        html.append("               <option value=\"1\"> 1 </option>");
        html.append("               <option value=\"2\"> 2 </option>");
        html.append("               <option value=\"3\"> 3 </option>");
        html.append("               <option value=\"4\"> 4 </option>");
        html.append("               <option value=\"5\"> 5 </option>");
        html.append("               <option value=\"6\"> 6 </option>");
        html.append("           </select>");
        html.append("           <br><br>");
        html.append("           <label for=\"col\"> Col : </label>");
        html.append("           <select name=\"col\" id =\"col\">");
        html.append("               <option value=\"0\"> 0 </option>");
        html.append("               <option value=\"1\"> 1 </option>");
        html.append("               <option value=\"2\"> 2 </option>");
        html.append("               <option value=\"3\"> 3 </option>");
        html.append("               <option value=\"4\"> 4 </option>");
        html.append("               <option value=\"5\"> 5 </option>");
        html.append("               <option value=\"6\"> 6 </option>");
        html.append("           </select>");
        html.append("           <br><br>"); 
        html.append("           <button type=\"submit\">Send</button>");
        html.append("       </form>");      
        html.append("   </noscript>\n");
        html.append("   <h2>Si la partie est terminée ( gagnée ou perdue), il suffit de refresh la page pour commencer une nouvelle partie</h2>");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    
    }

    /*cette fonction prend en argument un string(path vers un fichier) et va lire 
     * se fichier pour le transformer en une seule String
     */
    private String convertFileToString(String file){
        StringBuilder string = new StringBuilder();

        try{
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                
                while((line = reader.readLine()) != null){
                    string.append(line).append("\r\n");
                }
            }
        }
        catch(IOException e){
            System.err.println(e);
        }
        return string.toString();
    }

    private String transformListToString(List<char[]> game){
        if(game == null){
            return "#######\r\n#######\r\n#######\r\n#######\r\n#######\r\n#######\r\n#######\r\n\r\n";
        }
        else {
            StringBuilder gridString = new StringBuilder();
            for(int i = 0; i < 49; i++){
                switch (game.get(i)[1]) {
                    case 'R':
                        gridString.append(game.get(i)[0]);
                        break;
                    case 'F':
                        gridString.append("F");
                        break;
                    default:
                        gridString.append("#");
                        break;
                }
                if((i + 1) % 7 == 0){
                    gridString.append("\r\n");
                }
            }
            return gridString.toString();
        }
    }

    private String generateNoScriptGrid(String game, String flag, String bomb){
        if(game == null){
            StringBuilder emptyGame = new StringBuilder();

            for(int i = 0;i < 49; i++){
                emptyGame.append("#");
                if((i+1) % 7 == 0){
                    emptyGame.append("\n");
                }
            }

            game = emptyGame.toString();
        }
        //System.out.println(game);
        game = game.replace("\r\n", "");

        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" style=\"margin:auto; border-collapse: collapse;\">\n");

        for(int i = 0; i < 7; i++){
            html.append("<tr>");

            for(int j=0; j < 7; j++){
                html.append("<td style=\"width:30px; height:30px; text-align:center;");

                if(game.charAt((i * 7) + j) == '#' || game.charAt((i * 7) + j) == 'F'){
                    html.append("background-color : lightgray;");
                }
                else{
                    html.append("background-color : white;");
                }

                html.append("\">");

                switch(game.charAt((i * 7) + j)){
                    case '#':
                        html.append(" ");
                        break;
                    case 'F':
                        System.out.println("case F");
                        html.append("<img src = \"data:image/png;base64, ").append(flag).append("\" style=\"width: 100%; height: 100%;\">");
                        break;
                    case 'B':
                        html.append("<img src = \"data:image/png;base64, ").append(bomb).append("\" style=\"width: 100%; height: 100%;\">");
                        break;
                    case '0':
                        html.append(" ");
                        break;
                    default :
                        html.append(game.charAt((i * 7) + j));
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
        return html.toString();
    }
}